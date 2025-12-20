package com.gzone.guesthousebooking.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gzone.guesthousebooking.data.AppDatabase
import com.gzone.guesthousebooking.data.model.Booking
import com.gzone.guesthousebooking.data.model.GuestRoom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.*

fun Date.toLocalDate(): LocalDate {
    return this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}

data class BookingCalendarUiState(
    val rooms: List<GuestRoom> = emptyList(),
    val bookings: List<Booking> = emptyList(),
    val timelineStart: LocalDate = LocalDate.now(),
    val dateRange: Int = YearMonth.now().lengthOfMonth(),
    val isLoading: Boolean = true,
    val canNavigateBackward: Boolean = false,
    val canNavigateForward: Boolean = true
)

class BookingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val bookingDao = database.bookingDao()
    private val roomDao = database.roomDao()

    private val _visibleMonth = MutableStateFlow(YearMonth.now())
    val visibleMonth: StateFlow<YearMonth> = _visibleMonth.asStateFlow()

    private val minMonth = YearMonth.now().minusMonths(3)
    private val maxMonth = YearMonth.now().plusMonths(9)

    val calendarUiState: StateFlow<BookingCalendarUiState> = combine(
        roomDao.getAllRooms(),
        bookingDao.getAllBookings(),
        _visibleMonth
    ) { rooms, bookings, month ->
        BookingCalendarUiState(
            rooms = rooms,
            bookings = bookings,
            timelineStart = month.atDay(1),
            dateRange = month.lengthOfMonth(),
            isLoading = false,
            canNavigateBackward = month.isAfter(minMonth),
            canNavigateForward = month.isBefore(maxMonth)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BookingCalendarUiState()
    )

    // --- Form State ---
    private val _checkInDate = MutableStateFlow<Date?>(null)
    val checkInDate: StateFlow<Date?> = _checkInDate.asStateFlow()

    private val _checkOutDate = MutableStateFlow<Date?>(null)
    val checkOutDate: StateFlow<Date?> = _checkOutDate.asStateFlow()

    // --- BACK TO BASICS: A simple state flow for the available rooms list ---
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

    // --- THIS IS THE CRITICAL CHANGE ---
// In BookingViewModel.kt

    private fun updateAvailableRooms(checkIn: Date?, checkOut: Date?) {
        // If dates are not set, do nothing.
        if (checkIn == null || checkOut == null) {
            _availableRooms.value = emptyList()
            return
        }

        // Launch a coroutine to do the work in the background.
        viewModelScope.launch {
            // --- THIS IS THE ONE-LINE FIX ---
            // Get the room list from the already-loaded calendar state.
            // This avoids any new database calls or blocking operations.
            val allRooms = calendarUiState.value.rooms

            // Get conflicting bookings from the database ON A BACKGROUND THREAD.
            val conflictingBookings = withContext(Dispatchers.IO) {
                bookingDao.getAllConflictingBookings(checkIn, checkOut)
            }

            val conflictingRoomNumbers = conflictingBookings.map { it.roomNumber }.toSet()

            // Update the state with the final list. This is safe to do.
            _availableRooms.value = allRooms.filter { room -> !conflictingRoomNumbers.contains(room.number) }
        }
    }


    // --- `setCheckInDate` and `setCheckOutDate` now call the manual update function ---
    fun setCheckInDate(date: Date?) {
        _checkInDate.value = date
        if (_checkOutDate.value?.before(date) == true) {
            _checkOutDate.value = null
        }
        _selectedRoomNumbers.value = emptyList()
        // Manually trigger the update.
        updateAvailableRooms(date, _checkOutDate.value)
    }

    fun setCheckOutDate(date: Date?) {
        _checkOutDate.value = date
        _selectedRoomNumbers.value = emptyList()
        // Manually trigger the update.
        updateAvailableRooms(_checkInDate.value, date)
    }

    // --- Rest of the file is unchanged and known to be correct ---
    fun navigateToPreviousMonth() {
        val currentMonth = _visibleMonth.value
        if (currentMonth.isAfter(minMonth)) {
            _visibleMonth.value = currentMonth.minusMonths(1)
        }
    }

    fun navigateToNextMonth() {
        val currentMonth = _visibleMonth.value
        if (currentMonth.isBefore(maxMonth)) {
            _visibleMonth.value = currentMonth.plusMonths(1)
        }
    }

    fun returnToCurrentMonth() {
        _visibleMonth.value = YearMonth.now()
    }

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

    fun addBooking() {
        val checkIn = _checkInDate.value
        val checkOut = _checkOutDate.value
        val name = _guestName.value
        val guests = _numberOfGuests.value
        val contact = _contactNumber.value
        val rooms = _selectedRoomNumbers.value

        if (checkIn == null || checkOut == null || name.isBlank() || rooms.isEmpty()) return

        viewModelScope.launch {
            rooms.forEach { roomNumber ->
                val isAvailable = withContext(Dispatchers.IO) {
                    isRoomAvailable(roomNumber, checkIn, checkOut)
                }
                if (isAvailable) {
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
                    println("SKIPPED booking for Room $roomNumber as it was already booked.")
                }
            }
            clearFormState()
        }
    }

    fun deleteBooking(booking: Booking) {
        viewModelScope.launch {
            bookingDao.delete(booking)
        }
    }

    private fun clearFormState() {
        _guestName.value = ""
        _numberOfGuests.value = ""
        _contactNumber.value = ""
        _selectedRoomNumbers.value = emptyList()
    }

    private suspend fun isRoomAvailable(roomNumber: Int, checkIn: Date, checkOut: Date): Boolean {
        return bookingDao.getConflictingBookings(roomNumber, checkIn, checkOut).isEmpty()
    }
}
