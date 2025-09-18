package com.gzone.guesthousebooking.ui.booking

import androidx.lifecycle.ViewModel
import com.gzone.guesthousebooking.data.model.Booking
import com.gzone.guesthousebooking.data.repository.BookingRepository

class BookingViewModel : ViewModel() {

    private val repository = BookingRepository()

    fun getBookings(): List<Booking> = repository.getAllBookings()

    fun addBooking(booking: Booking) {
        repository.addBooking(booking)
    }

    fun removeBooking(id: String) {
        repository.removeBooking(id)
    }
}
