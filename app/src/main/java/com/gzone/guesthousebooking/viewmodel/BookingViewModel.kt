package com.gzone.guesthousebooking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gzone.guesthousebooking.data.model.Booking
import com.gzone.guesthousebooking.data.model.PaymentStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

class BookingViewModel : ViewModel() {

    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings: StateFlow<List<Booking>> = _bookings

    fun addBooking(
        guestName: String,
        roomNumber: Int,
        checkInDate: Date,
        checkOutDate: Date,
        numberOfGuests: Int,
        contactNumber: String
    ) {
        viewModelScope.launch {
            val newBooking = Booking(
                id = UUID.randomUUID().toString(),
                guestName = guestName,
                roomNumber = roomNumber,
                checkInDate = checkInDate,
                checkOutDate = checkOutDate,
                numberOfGuests = numberOfGuests,
                contactNumber = contactNumber,
                paymentStatus = PaymentStatus.PENDING
            )
            _bookings.value = _bookings.value + newBooking
        }
    }
}
