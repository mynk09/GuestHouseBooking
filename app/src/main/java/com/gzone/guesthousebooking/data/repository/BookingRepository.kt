package com.gzone.guesthousebooking.data.repository

import com.gzone.guesthousebooking.data.model.Booking
import java.util.UUID

class BookingRepository {

    private val bookings = mutableListOf<Booking>()

    fun getAllBookings(): List<Booking> = bookings

    fun addBooking(booking: Booking) {
        bookings.add(booking)
    }

    fun removeBooking(bookingId: String) {
        bookings.removeAll { it.id == bookingId }
    }

    fun createBooking(
        guestName: String,
        roomNumber: Int,
        checkInDate: java.util.Date,
        checkOutDate: java.util.Date,
        numberOfGuests: Int,
        contactNumber: String
    ): Booking {
        val booking = Booking(
            id = UUID.randomUUID().toString(),
            guestName = guestName,
            roomNumber = roomNumber,
            checkInDate = checkInDate,
            checkOutDate = checkOutDate,
            numberOfGuests = numberOfGuests,
            contactNumber = contactNumber
        )
        addBooking(booking)
        return booking
    }
}
