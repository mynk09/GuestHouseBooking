package com.gzone.guesthousebooking.viewmodel

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.size
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gzone.guesthousebooking.data.AppDatabase
import com.gzone.guesthousebooking.data.model.Booking
import com.gzone.guesthousebooking.data.model.GuestRoom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.*


fun Date.toLocalDate(): LocalDate {
    return this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}


data class BookingCalendarUiState(
    val rooms: List<GuestRoom> = emptyList(),
    val bookings: List<Booking> = emptyList(),
    val timelineStart: LocalDate = LocalDate.now(),
    val dateRange: Int = 30, // Default number of days to show
    val isLoading: Boolean = true
)
class BookingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val bookingDao = database.bookingDao()

    private val roomDao = database.roomDao()

    private val _calendarUiState = MutableStateFlow(BookingCalendarUiState())
    val calendarUiState: StateFlow<BookingCalendarUiState> = _calendarUiState.asStateFlow()


    private val _checkInDate = MutableStateFlow<Date?>(null)
    val checkInDate = _checkInDate.asStateFlow()

    private val _checkOutDate = MutableStateFlow<Date?>(null)
    val checkOutDate = _checkOutDate.asStateFlow()

    init {
        loadCalendarData()
    }

    private fun loadCalendarData() {
        viewModelScope.launch {
            _calendarUiState.value = _calendarUiState.value.copy(isLoading = true)
            println("🔄 Loading calendar data...")

            // Combine data streams from both DAOs
            roomDao.getAllRooms() // Ensure this exists in your RoomDao
                .combine(bookingDao.getAllBookings()) { rooms, bookings ->
                    BookingCalendarUiState(
                        rooms = rooms,
                        bookings = bookings,
                        timelineStart = LocalDate.now(),
                        isLoading = false
                    )
                }.collect { combinedState ->
                    _calendarUiState.value = combinedState
                    println("✅ Calendar data loaded: ${combinedState.rooms.size} rooms, ${combinedState.bookings.size} bookings.")
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