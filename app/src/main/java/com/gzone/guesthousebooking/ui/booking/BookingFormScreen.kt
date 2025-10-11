package com.gzone.guesthousebooking.ui.booking

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gzone.guesthousebooking.viewmodel.BookingViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ✅ RENAMED THE FUNCTION TO MATCH THE FILE AND ITS PURPOSE
@Composable
fun BookingFormScreen(
    bookingViewModel: BookingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFormatter = remember {
        SimpleDateFormat("dd-MMM-yy", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
    }
    val calendar = Calendar.getInstance()

    // State for form fields
    var guestName by remember { mutableStateOf("") }
    var numberOfGuests by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var selectedRoomNumbers by remember { mutableStateOf<List<Int>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) } // Dropdown expanded state

    // ✅ GET DATA FROM THE CORRECT, UNIFIED STATE
    val calendarState by bookingViewModel.calendarUiState.collectAsState()
    val rooms = calendarState.rooms
    val bookings = calendarState.bookings

    // Observe dates from ViewModel (this part is correct)
    val checkInDate by bookingViewModel.checkInDate.collectAsState()
    val checkOutDate by bookingViewModel.checkOutDate.collectAsState()

    // Date pickers
    val checkInDatePicker = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val selectedDate = calendar.time
                bookingViewModel.setCheckInDate(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val checkOutDatePicker = remember(checkInDate) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val selectedDate = calendar.time
                bookingViewModel.setCheckOutDate(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            checkInDate?.let { datePicker.minDate = it.time }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "GUEST HOUSE BOOKINGS",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth()
        )

        // Guest Name
        OutlinedTextField(
            value = guestName,
            onValueChange = { guestName = it },
            label = { Text("Guest Name") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter guest or group name") }
        )

        // Dropdown Room Selection
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = if (selectedRoomNumbers.isNotEmpty())
                    "Selected: ${selectedRoomNumbers.sorted().joinToString(", ")}"
                else
                    "Select Rooms",
                onValueChange = {},
                label = { Text("Select Rooms") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Rooms")
                    }
                }
            )

            // ✅ POPULATE DROPDOWN FROM VIEWMODEL, NOT A HARDCODED LIST
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                rooms.forEach { room ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Room ${room.number} - ${room.type}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "₹${room.ratePerNight}/night • Capacity: ${room.capacity}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Checkbox(
                                    checked = selectedRoomNumbers.contains(room.number),
                                    onCheckedChange = { checked ->
                                        selectedRoomNumbers = if (checked) {
                                            selectedRoomNumbers + room.number
                                        } else {
                                            selectedRoomNumbers - room.number
                                        }
                                    }
                                )
                            }
                        },
                        onClick = {
                            // Toggle selection on click
                            selectedRoomNumbers = if (selectedRoomNumbers.contains(room.number)) {
                                selectedRoomNumbers - room.number
                            } else {
                                selectedRoomNumbers + room.number
                            }
                        }
                    )
                    Divider()
                }
            }
        }

        // Date Selection Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier
                .weight(1f)
                .height(64.dp)) {
                OutlinedTextField(
                    value = checkInDate?.let { dateFormatter.format(it) } ?: "",
                    onValueChange = {},
                    label = { Text("Check-in") },
                    readOnly = true,
                    modifier = Modifier.fillMaxSize(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { checkInDatePicker.show() }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick Check-in Date")
                        }
                    }
                )
            }
            Box(modifier = Modifier
                .weight(1f)
                .height(64.dp)) {
                OutlinedTextField(
                    value = checkOutDate?.let { dateFormatter.format(it) } ?: "",
                    onValueChange = {},
                    label = { Text("Check-out") },
                    readOnly = true,
                    modifier = Modifier.fillMaxSize(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = { if (checkInDate != null) checkOutDatePicker.show() },
                            enabled = checkInDate != null
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick Check-out Date")
                        }
                    },
                    enabled = checkInDate != null
                )
            }
        }

        // Number of Guests and Contact
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = numberOfGuests,
                onValueChange = { numberOfGuests = it },
                label = { Text("Guests") },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Total guests") }
            )
            OutlinedTextField(
                value = contactNumber,
                onValueChange = { contactNumber = it },
                label = { Text("Contact") },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Phone number") }
            )
        }

        // Add Booking Button
        Button(
            onClick = {
                val safeCheckInDate = checkInDate
                val safeCheckOutDate = checkOutDate

                if (safeCheckInDate != null && safeCheckOutDate != null) {
                    selectedRoomNumbers.forEach { roomNumber ->
                        bookingViewModel.addBooking(
                            guestName = guestName,
                            roomNumber = roomNumber,
                            checkInDate = safeCheckInDate,
                            checkOutDate = safeCheckOutDate,
                            numberOfGuests = numberOfGuests.toIntOrNull() ?: 1,
                            contactNumber = contactNumber
                        )
                    }
                    // Clear form after submission
                    guestName = ""
                    selectedRoomNumbers = emptyList()
                    numberOfGuests = ""
                    contactNumber = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = guestName.isNotBlank() &&
                    selectedRoomNumbers.isNotEmpty() &&
                    numberOfGuests.isNotBlank() &&
                    contactNumber.isNotBlank() &&
                    checkInDate != null &&
                    checkOutDate != null
        ) {
            Text("Add ${if (selectedRoomNumbers.size > 1) "${selectedRoomNumbers.size} Bookings" else "Booking"}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bookings List
        if (bookings.isNotEmpty()) {
            Text(text = "Current Bookings:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Column {
                bookings.sortedBy { it.checkInDate }.forEach { booking ->
                    BookingListItem(
                        booking = booking,
                        onDelete = { bookingViewModel.deleteBooking(booking) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No bookings yet.\nAdd your first booking above!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ✅ RENAMED THIS COMPOSABLE TO BE MORE SPECIFIC
@Composable
fun BookingListItem(
    booking: com.gzone.guesthousebooking.data.model.Booking,
    onDelete: () -> Unit
) {
    val formatter = remember {
        SimpleDateFormat("dd-MMM-yy", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Room ${booking.roomNumber} - ${booking.guestName}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete Booking", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Guests: ${booking.numberOfGuests} • Contact: ${booking.contactNumber}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Dates: ${formatter.format(booking.checkInDate)} to ${formatter.format(booking.checkOutDate)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Status: ${booking.paymentStatus}",
                style = MaterialTheme.typography.bodyMedium,
                color = when (booking.paymentStatus) {
                    com.gzone.guesthousebooking.data.model.PaymentStatus.PAID -> Color(0xFF008000) // Green
                    com.gzone.guesthousebooking.data.model.PaymentStatus.PENDING -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}
