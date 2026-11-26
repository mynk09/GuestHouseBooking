package com.gzone.guesthousebooking.data.repository

import androidx.room.withTransaction
import com.gzone.guesthousebooking.data.AppDatabase
import com.gzone.guesthousebooking.data.model.Booking
import com.gzone.guesthousebooking.data.model.PaymentStatus
import kotlinx.coroutines.CancellationException

/** Local-only strategy. Cross-device confirmation belongs in a future remote strategy. */
class RoomBookingRepository(private val database: AppDatabase) : BookingRepository {
    private val bookingDao = database.bookingDao()
    private val roomDao = database.roomDao()

    override fun observeBookings() = bookingDao.getAllBookings()

    override fun observeRooms() = roomDao.getAllRooms()

    override suspend fun createBookings(bookings: List<Booking>): BookingWriteResult = write {
        if (bookings.isEmpty() ||
            bookings.map { it.id }.distinct().size != bookings.size ||
            bookings.any {
                it.guestName.isBlank() || it.numberOfGuests <= 0 ||
                    !it.checkOutDate.after(it.checkInDate) ||
                    it.paymentStatus == PaymentStatus.CANCELLED
            }
        ) {
            return@write BookingWriteResult.Failed(BookingFailure.INVALID_REQUEST)
        }

        database.withTransaction {
            if (bookings.any { roomDao.getRoom(it.roomNumber)?.isActive != true }) {
                return@withTransaction BookingWriteResult.Failed(BookingFailure.INVALID_REQUEST)
            }

            val unavailable = bookings.filter { candidate ->
                bookingDao.getConflictingBookings(
                    candidate.roomNumber, candidate.checkInDate, candidate.checkOutDate
                ).isNotEmpty() || bookings.any { other ->
                    other.id != candidate.id && other.roomNumber == candidate.roomNumber &&
                        other.checkInDate.before(candidate.checkOutDate) &&
                        other.checkOutDate.after(candidate.checkInDate)
                }
            }.map { it.roomNumber }.toSet()

            if (unavailable.isNotEmpty()) {
                return@withTransaction BookingWriteResult.Unavailable(unavailable)
            }

            bookings.forEach { bookingDao.insert(it) }
            BookingWriteResult.Success
        }
    }

    override suspend fun deleteBooking(bookingId: String): BookingWriteResult = write {
        database.withTransaction {
            // Deleting an already removed booking is safe to retry.
            bookingDao.getBookingById(bookingId)?.let { bookingDao.delete(it) }
            BookingWriteResult.Success
        }
    }

    private suspend fun write(block: suspend () -> BookingWriteResult): BookingWriteResult {
        return try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            BookingWriteResult.Failed(BookingFailure.STORAGE_ERROR)
        }
    }
}
