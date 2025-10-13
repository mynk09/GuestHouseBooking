package com.gzone.guesthousebooking.viewmodel

import android.app.Application
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

    // --- State for Calendar View ---
    private val _calendarUiState = MutableStateFlow(BookingCalendarUiState())
    val calendarUiState: StateFlow<BookingCalendarUiState> = _calendarUiState.asStateFlow()

    // --- State for Form Screen ---
    private val _checkInDate = MutableStateFlow<Date?>(null)
    val checkInDate: StateFlow<Date?> = _checkInDate.asStateFlow()

    private val _checkOutDate = MutableStateFlow<Date?>(null)
    val checkOutDate: StateFlow<Date?> = _checkOutDate.asStateFlow()

    private val _availableRooms = MutableStateFlow<List<GuestRoom>>(emptyList())
    val availableRooms: StateFlow<List<GuestRoom>> = _availableRooms.asStateFlow()

    private val _guestName = MutableStateFlow("")
    val guestName: StateFlow<String> = _guestName.asStateFlow()

    private val _numberOfGuests = MutableStateFlow("")
    val numberOfGuests: StateFlow<String> = _numberOfGuests.asStateFlow()

    private val _contactNumber = MutableStateFlow("")
    val contactNumber: StateFlow<String> = _contactNumber.asStateFlow()

    private val _selectedRoomNumbers = MutableStateFlow<List<Int>>(emptyList())
    val selectedRoomNumbers: StateFlow<List<Int>> = _selectedRoomNumbers.asStateFlow()

    init {
        loadCalendarData()
    }

    private fun loadCalendarData() {
        viewModelScope.launch {
            _calendarUiState.value = _calendarUiState.value.copy(isLoading = true)
            roomDao.getAllRooms()
                .combine(bookingDao.getAllBookings()) { rooms, bookings ->
                    BookingCalendarUiState(
                        rooms = rooms,
                        bookings = bookings,
                        timelineStart = LocalDate.now(),
                        isLoading = false
                    )
                }.collect { combinedState ->
                    _calendarUiState.value = combinedState
                    // When data loads, update available rooms. If dates are picked, this will be filtered later.
                    updateAvailableRooms()
                }
        }
    }

    // --- Functions to handle UI events from the BookingFormScreen ---

    fun onGuestNameChange(newName: String) { _guestName.value = newName }
    fun onNumberOfGuestsChange(newCount: String) { _numberOfGuests.value = newCount }
    fun onContactNumberChange(newNumber: String) { _contactNumber.value = newNumber }

    fun onRoomSelectionChange(roomNumber: Int) {
        val currentSelection = _selectedRoomNumbers.value.toMutableList()
        if (currentSelection.contains(roomNumber)) {
            currentSelection.remove(roomNumber)
        } else {
            currentSelection.add(roomNumber)
        }
        _selectedRoomNumbers.value = currentSelection
    }

    fun setCheckInDate(date: Date?) {
        _checkInDate.value = date
        // If checkout is before new checkin, clear it
        if (_checkOutDate.value?.before(date) == true) {
            _checkOutDate.value = null
        }
        _selectedRoomNumbers.value = emptyList() // Reset room selection when dates change
        updateAvailableRooms()
    }

    fun setCheckOutDate(date: Date?) {
        _checkOutDate.value = date
        _selectedRoomNumbers.value = emptyList() // Reset room selection when dates change
        updateAvailableRooms()
    }

    fun updateAvailableRooms() {
        val checkIn = _checkInDate.value
        val checkOut = _checkOutDate.value

        viewModelScope.launch {
            val allRooms = _calendarUiState.value.rooms
            if (checkIn != null && checkOut != null) {
                val conflictingBookings = bookingDao.getAllConflictingBookings(checkIn, checkOut)
                val conflictingRoomNumbers = conflictingBookings.map { it.roomNumber }.toSet()
                _availableRooms.value = allRooms.filter { room -> !conflictingRoomNumbers.contains(room.number) }
            } else {
                // If dates are not set, show all rooms
                _availableRooms.value = allRooms
            }
        }
    }

    /**
     * Final logic to add bookings. Takes no parameters as it reads all data
     * from its own state. This makes it robust and testable.
     */
    fun addBooking() {
        val checkIn = _checkInDate.value
        val checkOut = _checkOutDate.value
        val name = _guestName.value
        val guests = _numberOfGuests.value
        val contact = _contactNumber.value
        val rooms = _selectedRoomNumbers.value

        // Guard clause to ensure all data is valid
        if (checkIn == null || checkOut == null || name.isBlank() || rooms.isEmpty()) return

        viewModelScope.launch {
            rooms.forEach { roomNumber ->
                // Final check for each room before inserting
                if (isRoomAvailable(roomNumber, checkIn, checkOut)) {
                    val newBooking = Booking(
                        guestName = name,
                        roomNumber = roomNumber,
                        checkInDate = checkIn,
                        checkOutDate = checkOut,
                        numberOfGuests = guests.toIntOrNull() ?: 1,
                        contactNumber = contact
                    )
                    bookingDao.insert(newBooking)
                } else {
                    // This could be exposed as a Toast/Snackbar message to the UI
                    println("SKIPPED booking for Room $roomNumber as it was already booked.")
                }
            }
            // Clear the form for the next entry
            clearFormState()
        }
    }

    fun deleteBooking(booking: Booking) {
        viewModelScope.launch {
            bookingDao.delete(booking)
        }
    }

    /**
     * Clears all temporary form state. Can be called after a successful booking.
     */
    private fun clearFormState() {
        _guestName.value = ""
        _numberOfGuests.value = ""
        _contactNumber.value = ""
        _selectedRoomNumbers.value = emptyList()
        // Decide if you want to clear dates or not. Keeping them is often convenient.
        // _checkInDate.value = null
        // _checkOutDate.value = null
    }

    private suspend fun isRoomAvailable(roomNumber: Int, checkIn: Date, checkOut: Date): Boolean {
        return bookingDao.getConflictingBookings(roomNumber, checkIn, checkOut).isEmpty()
    }
}
