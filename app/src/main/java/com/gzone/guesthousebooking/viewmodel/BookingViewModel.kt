package com.gzone.guesthousebooking.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gzone.guesthousebooking.data.AppDatabase
import com.gzone.guesthousebooking.data.model.Booking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class BookingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val bookingDao = database.bookingDao()

    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings: StateFlow<List<Booking>> = _bookings.asStateFlow()

    private val _checkInDate = MutableStateFlow<Date?>(null)
    val checkInDate = _checkInDate.asStateFlow()

    private val _checkOutDate = MutableStateFlow<Date?>(null)
    val checkOutDate = _checkOutDate.asStateFlow()

    init {
        loadBookingsFromDatabase()
    }

    private fun loadBookingsFromDatabase() {
        viewModelScope.launch {
            bookingDao.getAllBookings().collect { bookingsList ->
                _bookings.value = bookingsList
                println("📱 Loaded ${bookingsList.size} bookings from database")
            }
        }
    }

    fun setCheckInDate(date: Date?) {
        _checkInDate.value = date
    }

    fun setCheckOutDate(date: Date?) {
        _checkOutDate.value = date
    }

    fun addBooking(
        guestName: String,
        roomNumber: Int,
        checkInDate: Date,
        checkOutDate: Date,
        numberOfGuests: Int,
        contactNumber: String
    ) {
        viewModelScope.launch {
            try {
                val newBooking = Booking(
                    guestName = guestName,
                    roomNumber = roomNumber,
                    checkInDate = checkInDate,
                    checkOutDate = checkOutDate,
                    numberOfGuests = numberOfGuests,
                    contactNumber = contactNumber
                )

                // ✅ SAVE TO DATABASE
                bookingDao.insert(newBooking)
                println("💾 Booking saved to database: $guestName - Room $roomNumber")

                // The Flow will automatically update _bookings
            } catch (e: Exception) {
                println("❌ Error saving booking: ${e.message}")
            }
        }
    }

    fun deleteBooking(booking: Booking) {
        viewModelScope.launch {
            bookingDao.delete(booking)
            println("❌ Booking deleted from database: ${booking.guestName} - Room ${booking.roomNumber}")
        }
    }

    // Optional: Add method to get room availability
    suspend fun isRoomAvailable(roomNumber: Int, checkIn: Date, checkOut: Date): Boolean {
        return bookingDao.getConflictingBookings(roomNumber, checkIn, checkOut).isEmpty()
    }
}