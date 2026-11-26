package com.gzone.guesthousebooking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gzone.guesthousebooking.data.repository.BookingFailure
import com.gzone.guesthousebooking.data.repository.BookingRepository
import com.gzone.guesthousebooking.data.repository.BookingWriteResult
import com.gzone.guesthousebooking.data.model.Booking
import com.gzone.guesthousebooking.data.model.GuestRoom
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

class BookingViewModel(private val repository: BookingRepository) : ViewModel() {

    private val _visibleMonth = MutableStateFlow(YearMonth.now())
    val visibleMonth: StateFlow<YearMonth> = _visibleMonth.asStateFlow()

    private val minMonth = YearMonth.now().minusMonths(3)
    private val maxMonth = YearMonth.now().plusMonths(9)

    val calendarUiState: StateFlow<BookingCalendarUiState> = combine(
        repository.observeRooms(),
        repository.observeBookings(),
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

    // Re-evaluate when either dates or stored data change; this is advisory only.
    val availableRooms: StateFlow<List<GuestRoom>> = combine(
        _checkInDate, _checkOutDate, repository.observeRooms(), repository.observeBookings()
    ) { checkIn, checkOut, rooms, bookings ->
        if (checkIn == null || checkOut == null || !checkOut.after(checkIn)) {
            emptyList()
        } else {
            val occupied = bookings.filter {
                it.paymentStatus != com.gzone.guesthousebooking.data.model.PaymentStatus.CANCELLED &&
                    it.checkInDate.before(checkOut) && it.checkOutDate.after(checkIn)
            }.map { it.roomNumber }.toSet()
            rooms.filter { it.isActive && it.number !in occupied }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _bookingError = MutableStateFlow<String?>(null)
    val bookingError: StateFlow<String?> = _bookingError.asStateFlow()

    private val _guestName = MutableStateFlow("")
    val guestName: StateFlow<String> = _guestName.asStateFlow()
    private val _numberOfGuests = MutableStateFlow("")
    val numberOfGuests: StateFlow<String> = _numberOfGuests.asStateFlow()
    private val _contactNumber = MutableStateFlow("")
    val contactNumber: StateFlow<String> = _contactNumber.asStateFlow()
    private val _selectedRoomNumbers = MutableStateFlow<List<Int>>(emptyList())
    val selectedRoomNumbers: StateFlow<List<Int>> = _selectedRoomNumbers.asStateFlow()

    fun setCheckInDate(date: Date?) {
        _checkInDate.value = date
        if (date == null || _checkOutDate.value?.after(date) == false) {
            _checkOutDate.value = null
        }
        _selectedRoomNumbers.value = emptyList()
        _bookingError.value = null
    }

    fun setCheckOutDate(date: Date?) {
        _checkOutDate.value = date
        _selectedRoomNumbers.value = emptyList()
        _bookingError.value = null
    }

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

        if (_isSaving.value) return
        if (checkIn == null || checkOut == null || !checkOut.after(checkIn) ||
            name.isBlank() || rooms.isEmpty() || (guests.toIntOrNull() ?: 1) <= 0
        ) {
            _bookingError.value = "Enter valid booking details and a check-out after check-in."
            return
        }

        val bookings = rooms.map { roomNumber ->
            Booking(
                guestName = name,
                roomNumber = roomNumber,
                checkInDate = checkIn,
                checkOutDate = checkOut,
                numberOfGuests = guests.toIntOrNull() ?: 1,
                contactNumber = contact
            )
        }
        _isSaving.value = true
        _bookingError.value = null
        viewModelScope.launch {
            try {
                when (val result = repository.createBookings(bookings)) {
                    BookingWriteResult.Success -> clearFormState()
                    else -> _bookingError.value = result.errorMessage()
                }
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deleteBooking(booking: Booking) {
        viewModelScope.launch {
            _bookingError.value = repository.deleteBooking(booking.id).errorMessage()
        }
    }

    private fun clearFormState() {
        _guestName.value = ""
        _numberOfGuests.value = ""
        _contactNumber.value = ""
        _selectedRoomNumbers.value = emptyList()
        _checkInDate.value = null
        _checkOutDate.value = null
        _bookingError.value = null
    }

    private fun BookingWriteResult.errorMessage(): String? = when (this) {
        BookingWriteResult.Success -> null
        is BookingWriteResult.Unavailable ->
            "Booking failed: rooms ${roomNumbers.sorted().joinToString()} are unavailable. No bookings were saved."
        is BookingWriteResult.Failed -> when (reason) {
            BookingFailure.INVALID_REQUEST -> "Check the booking details and selected rooms."
            BookingFailure.ACCESS_DENIED -> "You do not have permission to make this change."
            BookingFailure.CONNECTION_REQUIRED -> "Connect to the internet to confirm this change."
            BookingFailure.STORAGE_ERROR -> "Could not save the change. Please try again."
        }
    }
}
