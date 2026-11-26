package com.gzone.guesthousebooking.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gzone.guesthousebooking.data.AppDatabase
import com.gzone.guesthousebooking.data.model.Booking
import com.gzone.guesthousebooking.data.model.GuestRoom
import com.gzone.guesthousebooking.data.model.PaymentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date

@RunWith(AndroidJUnit4::class)
class RoomBookingRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: BookingRepository

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).build()
        repository = RoomBookingRepository(database)
        listOf(101, 102).forEach { number ->
            database.roomDao().insert(GuestRoom(number, "Double", 2, ratePerNight = 1000.0))
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun conflictRejectsEntireMultiRoomRequest() = runBlocking {
        assertEquals(BookingWriteResult.Success, repository.createBookings(listOf(booking(101))))

        val result = repository.createBookings(listOf(booking(102), booking(101)))

        assertEquals(BookingWriteResult.Unavailable(setOf(101)), result)
        assertEquals(1, repository.observeBookings().first().size)
    }

    @Test
    fun adjacentStaysAndDifferentRoomsAreAccepted() = runBlocking {
        val result = repository.createBookings(
            listOf(booking(101), booking(101, "2026-11-10", "2026-11-12"), booking(102))
        )

        assertEquals(BookingWriteResult.Success, result)
        assertEquals(3, repository.observeBookings().first().size)
    }

    @Test
    fun overlappingRoomsWithinSameRequestAreRejected() = runBlocking {
        val result = repository.createBookings(listOf(booking(101), booking(101)))

        assertEquals(BookingWriteResult.Unavailable(setOf(101)), result)
        assertTrue(repository.observeBookings().first().isEmpty())
    }

    @Test
    fun cancelledBookingDoesNotBlockRoom() = runBlocking {
        database.bookingDao().insert(booking(101).copy(paymentStatus = PaymentStatus.CANCELLED))

        assertEquals(BookingWriteResult.Success, repository.createBookings(listOf(booking(101))))
    }

    @Test
    fun concurrentRequestsConfirmOnlyOneBooking() = runBlocking {
        val results = coroutineScope {
            List(2) {
                async(Dispatchers.IO) { repository.createBookings(listOf(booking(101))) }
            }.awaitAll()
        }

        assertEquals(1, results.count { it == BookingWriteResult.Success })
        assertEquals(1, results.count { it is BookingWriteResult.Unavailable })
        assertEquals(1, repository.observeBookings().first().size)
    }

    @Test
    fun failedInsertRollsBackOtherRooms() = runBlocking {
        val existing = booking(101)
        database.bookingDao().insert(existing)
        // The second insert reuses an existing primary key but has non-conflicting dates.
        val result = repository.createBookings(
            listOf(booking(102), booking(101, "2026-11-12", "2026-11-14").copy(id = existing.id))
        )

        assertEquals(BookingWriteResult.Failed(BookingFailure.STORAGE_ERROR), result)
        assertEquals(listOf(existing), repository.observeBookings().first())
    }

    @Test
    fun deleteCanBeRetried() = runBlocking {
        val booking = booking(101)
        repository.createBookings(listOf(booking))

        assertEquals(BookingWriteResult.Success, repository.deleteBooking(booking.id))
        assertEquals(BookingWriteResult.Success, repository.deleteBooking(booking.id))
        assertTrue(repository.observeBookings().first().isEmpty())
    }

    private fun booking(
        room: Int,
        checkIn: String = "2026-11-08",
        checkOut: String = "2026-11-10"
    ) = Booking(
        guestName = "Test guest",
        roomNumber = room,
        checkInDate = date(checkIn),
        checkOutDate = date(checkOut),
        numberOfGuests = 1,
        contactNumber = ""
    )

    private fun date(value: String): Date =
        Date.from(LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant())
}
