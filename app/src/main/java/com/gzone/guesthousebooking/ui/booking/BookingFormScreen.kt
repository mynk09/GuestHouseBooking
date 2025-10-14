package com.gzone.guesthousebooking.ui.booking

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gzone.guesthousebooking.viewmodel.BookingViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BookingFormScreen(
    bookingViewModel: BookingViewModel,
    modifier: Modifier = Modifier
) {
    // Observe all state directly from the ViewModel
    val guestName by bookingViewModel.guestName.collectAsState()
    val numberOfGuests by bookingViewModel.numberOfGuests.collectAsState()
    val contactNumber by bookingViewModel.contactNumber.collectAsState()
    val selectedRoomNumbers by bookingViewModel.selectedRoomNumbers.collectAsState()
    val availableRooms by bookingViewModel.availableRooms.collectAsState()
    val checkInDate by bookingViewModel.checkInDate.collectAsState()
    val checkOutDate by bookingViewModel.checkOutDate.collectAsState()
    val allBookings = bookingViewModel.calendarUiState.collectAsState().value.bookings

    // ✅ NEW LAYOUT: Use LazyColumn for the entire screen.
    // This allows the form and the list to scroll together seamlessly.
    LazyColumn(
        modifier = modifier.fillMaxSize(), // Use the modifier passed from Scaffold
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        // --- Header ---
        item {
            Text(
                " TRACK GUEST HOUSE BOOKINGS",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- Form Section ---
        item {
            BookingFormFields(
                bookingViewModel = bookingViewModel,
                guestName = guestName,
                numberOfGuests = numberOfGuests,
                contactNumber = contactNumber,
                selectedRoomNumbers = selectedRoomNumbers,
                availableRooms = availableRooms,
                checkInDate = checkInDate,
                checkOutDate = checkOutDate
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- Submit Button ---
        item {
            Button(
                onClick = { bookingViewModel.addBooking() },
                modifier = Modifier.fillMaxWidth(),
                enabled = guestName.isNotBlank() && selectedRoomNumbers.isNotEmpty() && checkInDate != null && checkOutDate != null
            ) {
                Text("Add ${if (selectedRoomNumbers.size > 1) "${selectedRoomNumbers.size} Bookings" else "Booking"}")
            }
        }

        // --- Bookings List Section ---
        item {
            Text("Current Bookings:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (allBookings.isNotEmpty()) {
            items(allBookings.sortedByDescending { it.checkInDate }, key = { it.id }) { booking ->
                BookingListItem(
                    booking = booking,
                    onDelete = { bookingViewModel.deleteBooking(booking) }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No bookings yet.\nAdd your first booking above!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Add final spacer for bottom padding
        item {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun BookingFormFields(
    bookingViewModel: BookingViewModel,
    guestName: String,
    numberOfGuests: String,
    contactNumber: String,
    selectedRoomNumbers: List<Int>,
    availableRooms: List<com.gzone.guesthousebooking.data.model.GuestRoom>,
    checkInDate: Date?,
    checkOutDate: Date?
) {
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("dd-MMM-yy", Locale.US).apply { timeZone = TimeZone.getDefault() } }
    var expanded by remember { mutableStateOf(false) }

    fun showDatePicker(isCheckIn: Boolean) {
        val initialDate = if (isCheckIn) checkInDate else checkOutDate
        val calendar = Calendar.getInstance().apply { initialDate?.let { time = it } }
        DatePickerDialog(context, { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            if (isCheckIn) bookingViewModel.setCheckInDate(calendar.time) else bookingViewModel.setCheckOutDate(calendar.time)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            if (!isCheckIn) checkInDate?.let { datePicker.minDate = it.time + (24 * 60 * 60 * 1000) }
        }.show()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dates Section
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DatePickerField(
                label = "Check-in",
                date = checkInDate,
                dateFormatter = dateFormatter,
                onIconClick = { showDatePicker(isCheckIn = true) }
            )
            DatePickerField(
                label = "Check-out",
                date = checkOutDate,
                dateFormatter = dateFormatter,
                onIconClick = { if (checkInDate != null) showDatePicker(isCheckIn = false) },
                enabled = checkInDate != null
            )
        }

        // Guest Name
        OutlinedTextField(
            value = guestName,
            onValueChange = { bookingViewModel.onGuestNameChange(it) },
            label = { Text("Guest Name") },
            modifier = Modifier.fillMaxWidth()
        )

        // Room Selection
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = if (selectedRoomNumbers.isNotEmpty()) "Selected: ${selectedRoomNumbers.sorted().joinToString(", ")}" else "Select Available Rooms",
                onValueChange = {},
                label = { Text("Select Rooms") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                enabled = checkInDate != null && checkOutDate != null,
                trailingIcon = {
                    IconButton(
                        onClick = { expanded = true },
                        enabled = checkInDate != null && checkOutDate != null
                    ) { Icon(Icons.Default.ArrowDropDown, "Select Rooms") }
                }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                if (availableRooms.isEmpty() && checkInDate != null && checkOutDate != null) {
                    DropdownMenuItem(
                        text = { Text("No rooms available for these dates", color = MaterialTheme.colorScheme.error) },
                        onClick = { expanded = false }
                    )
                } else {
                    availableRooms.forEach { room ->
                        DropdownMenuItem(
                            text = {
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Room ${room.number} - ${room.type}", style = MaterialTheme.typography.bodyMedium)
                                        Text("₹${room.ratePerNight}/night • Capacity: ${room.capacity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Checkbox(
                                        checked = selectedRoomNumbers.contains(room.number),
                                        onCheckedChange = { bookingViewModel.onRoomSelectionChange(room.number) }
                                    )
                                }
                            },
                            onClick = { bookingViewModel.onRoomSelectionChange(room.number) }
                        )
                        Divider()
                    }
                }
            }
        }

        // Guest Details
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = numberOfGuests,
                onValueChange = { bookingViewModel.onNumberOfGuestsChange(it) },
                label = { Text("Guests") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = contactNumber,
                onValueChange = { bookingViewModel.onContactNumberChange(it) },
                label = { Text("Contact") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}


@Composable
fun RowScope.DatePickerField(
    label: String,
    date: Date?,
    dateFormatter: SimpleDateFormat,
    onIconClick: () -> Unit,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = date?.let { dateFormatter.format(it) } ?: "",
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        modifier = Modifier.weight(1f),
        singleLine = true,
        trailingIcon = { IconButton(onClick = onIconClick, enabled = enabled) { Icon(Icons.Default.DateRange, "Pick Date") } },
        enabled = enabled
    )
}

@Composable
fun BookingListItem(
    booking: com.gzone.guesthousebooking.data.model.Booking,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd-MMM-yy", Locale.US).apply { timeZone = TimeZone.getDefault() } }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Room ${booking.roomNumber} - ${booking.guestName}",
                    style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary)
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete Booking", tint = MaterialTheme.colorScheme.error)
                }
            }
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Guests: ${booking.numberOfGuests} • Contact: ${booking.contactNumber}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Dates: ${formatter.format(booking.checkInDate)} to ${formatter.format(booking.checkOutDate)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
