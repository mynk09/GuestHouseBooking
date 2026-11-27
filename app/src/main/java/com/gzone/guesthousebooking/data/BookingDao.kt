package com.gzone.guesthousebooking.data

import androidx.room.*
import com.gzone.guesthousebooking.data.model.Booking
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface BookingDao {
    @Insert
    suspend fun insert(booking: Booking)

    @Update
    suspend fun update(booking: Booking)

    @Delete
    suspend fun delete(booking: Booking)

    @Query("SELECT * FROM bookings ORDER BY checkInDate DESC")
    fun getAllBookings(): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE roomNumber = :roomNumber ORDER BY checkInDate DESC")
    fun getBookingsForRoom(roomNumber: Int): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE id = :bookingId")
    suspend fun getBookingById(bookingId: String): Booking?

    // In BookingDao.kt

    @Query("SELECT * FROM bookings WHERE checkInDate < :checkOutDate AND checkOutDate > :checkInDate")
    suspend fun getAllConflictingBookings(checkInDate: Date, checkOutDate: Date): List<Booking>


    @Query("""
        SELECT * FROM bookings 
        WHERE roomNumber = :roomNumber 
        AND paymentStatus != 'CANCELLED'
        AND checkInDate < :checkOut 
        AND checkOutDate > :checkIn
    """)
    suspend fun getConflictingBookings(
        roomNumber: Int,
        checkIn: Date,
        checkOut: Date
    ): List<Booking>

    @Query("SELECT COUNT(*) FROM bookings")
    suspend fun getBookingCount(): Int
}