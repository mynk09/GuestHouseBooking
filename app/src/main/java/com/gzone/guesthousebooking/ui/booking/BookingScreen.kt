package com.gzone.guesthousebooking.ui.booking

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gzone.guesthousebooking.data.model.GuestRoom
import com.gzone.guesthousebooking.viewmodel.BookingViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BookingScreen(
    bookingViewModel: BookingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val calendar = Calendar.getInstance()

    // State for form fields
    var guestName by remember { mutableStateOf("") }
    var numberOfGuests by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var selectedRoomNumbers by remember { mutableStateOf<List<Int>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) } // Dropdown expanded state

    // Observe bookings from ViewModel
    val bookings by bookingViewModel.bookings.collectAsState()

    // Observe dates from ViewModel
    val checkInDate by bookingViewModel.checkInDate.collectAsState()
    val checkOutDate by bookingViewModel.checkOutDate.collectAsState()

    // Available rooms
    val availableRooms = remember {
        listOf(
            GuestRoom(101, "Single", 1, "AC,TV", 1500.0),
            GuestRoom(102, "Double", 2, "AC,TV", 2500.0),
            GuestRoom(103, "Double", 2, "AC,TV", 2500.0),
            GuestRoom(104, "Double", 2, "AC,TV", 2500.0),
            GuestRoom(105, "Suite", 4, "AC,TV,Kitchen", 5000.0),
            GuestRoom(106, "Single", 1, "AC,TV", 1500.0),
            GuestRoom(107, "Single", 1, "AC,TV", 1500.0),
            GuestRoom(108, "Double", 2, "AC,TV", 2500.0),
            GuestRoom(109, "Double", 2, "AC,TV", 2500.0),
            GuestRoom(110, "Suite", 4, "AC,TV,Kitchen", 5000.0),
            GuestRoom(201, "Single", 1, "AC,TV", 1600.0),
            GuestRoom(202, "Double", 2, "AC,TV", 2600.0),
            GuestRoom(511, "Premium Suite", 4, "AC,TV,Kitchen,Jacuzzi", 6000.0)
        )
    }

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
            text = "TRACK YOUR BOOKINGS",
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

        // ✅ DROPDOWN ROOM SELECTION (ORIGINAL FUNCTIONALITY)
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

            // Dropdown menu with checkboxes
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                availableRooms.forEach { room ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
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
            Box(modifier = Modifier.weight(1f).height(64.dp)) {
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

            Box(modifier = Modifier.weight(1f).height(64.dp)) {
                OutlinedTextField(
                    value = checkOutDate?.let { dateFormatter.format(it) } ?: "",
                    onValueChange = {},
                    label = { Text("Check-out") },
                    readOnly = true,
                    modifier = Modifier.fillMaxSize(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (checkInDate != null) checkOutDatePicker.show()
                            },
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
                val safeCheckInDate = checkInDate ?: Date()
                val safeCheckOutDate = checkOutDate ?: Date().apply {
                    time += 24 * 60 * 60 * 1000
                }

                // Create separate booking for each selected room
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

                // Clear form but keep dates for convenience
                guestName = ""
                selectedRoomNumbers = emptyList()
                numberOfGuests = ""
                contactNumber = ""
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = guestName.isNotBlank() &&
                    selectedRoomNumbers.isNotEmpty() &&
                    numberOfGuests.isNotBlank() &&
                    contactNumber.isNotBlank() &&
                    checkInDate != null &&
                    checkOutDate != null
        ) {
            Text("Add ${selectedRoomNumbers.size} Booking(s)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bookings List
        if (bookings.isNotEmpty()) {
            Text(
                text = "Current Bookings:",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column {
                bookings.forEachIndexed { index, booking ->
                    BookingItem(booking = booking, serialNumber = index + 1)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
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

@Composable
fun BookingItem(booking: com.gzone.guesthousebooking.data.model.Booking, serialNumber: Int) {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Booking #$serialNumber",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Guest: ${booking.guestName}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Room: ${booking.roomNumber}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Guests: ${booking.numberOfGuests}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Contact: ${booking.contactNumber}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Dates: ${formatter.format(booking.checkInDate)} to ${formatter.format(booking.checkOutDate)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = "Status: ${booking.paymentStatus}",
                style = MaterialTheme.typography.bodyMedium,
                color = when (booking.paymentStatus.toString().uppercase()) {
                    "PAID" -> MaterialTheme.colorScheme.primary
                    "PENDING" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}