package com.gzone.guesthousebooking.ui.booking

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    var roomNumber by remember { mutableStateOf("") }
    var numberOfGuests by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }

    // Observe bookings from ViewModel
    val bookings by bookingViewModel.bookings.collectAsState()

    // Observe dates from ViewModel
    val checkInDate by bookingViewModel.checkInDate.collectAsState()
    val checkOutDate by bookingViewModel.checkOutDate.collectAsState()

    // Date pickers with validation
    val checkInDatePicker = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val selectedDate = calendar.time
                bookingViewModel.setCheckInDate(selectedDate)

                // If check-out is before new check-in, reset it
                checkOutDate?.let {
                    if (it.before(selectedDate)) {
                        bookingViewModel.setCheckOutDate(null)
                    }
                }
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

                // Validate: Check-out cannot be before check-in
                checkInDate?.let { checkIn ->
                    if (selectedDate.after(checkIn)) {
                        bookingViewModel.setCheckOutDate(selectedDate)
                    }
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            // Set min date for check-out to check-in date or today
            checkInDate?.let {
                datePicker.minDate = it.time
            } ?: run {
                datePicker.minDate = System.currentTimeMillis() - 1000 // Today
            }
        }
    }

    // FIXED SCROLLING: Use verticalScroll for the entire content
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // ENABLE SCROLLING FOR WHOLE SCREEN
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
            modifier = Modifier.fillMaxWidth()
        )

        // Room Number
        OutlinedTextField(
            value = roomNumber,
            onValueChange = { roomNumber = it },
            label = { Text("Room Number") },
            modifier = Modifier.fillMaxWidth()
        )

        // Date Selection Row - FIXED DATE VISIBILITY
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Check-in Date - FIXED: Larger box for better date visibility
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp) // Increased height
            ) {
                OutlinedTextField(
                    value = checkInDate?.let { dateFormatter.format(it) } ?: "",
                    onValueChange = {},
                    label = { Text("Check-in") },
                    readOnly = true,
                    modifier = Modifier.fillMaxSize(),
                    // FIXED: Single line and better text visibility
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { checkInDatePicker.show() }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick Check-in Date")
                        }
                    }
                )
            }

            // Check-out Date - FIXED: Larger box for better date visibility
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp) // Increased height
            ) {
                OutlinedTextField(
                    value = checkOutDate?.let { dateFormatter.format(it) } ?: "",
                    onValueChange = {},
                    label = { Text("Check-out") },
                    readOnly = true,
                    modifier = Modifier.fillMaxSize(),
                    // FIXED: Single line and better text visibility
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (checkInDate != null) {
                                    checkOutDatePicker.show()
                                }
                            },
                            enabled = checkInDate != null
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = "Pick Check-out Date"
                            )
                        }
                    },
                    // Visual indication when disabled
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
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
            )

            OutlinedTextField(
                value = contactNumber,
                onValueChange = { contactNumber = it },
                label = { Text("Contact") },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
            )
        }

        // Add Booking Button with validation
        Button(
            onClick = {
                // SAFE: We already checked these are not null in enabled condition
                val safeCheckInDate = checkInDate!!
                val safeCheckOutDate = checkOutDate!!

                bookingViewModel.addBooking(
                    guestName = guestName,
                    roomNumber = roomNumber.toIntOrNull() ?: 0,
                    checkInDate = safeCheckInDate,
                    checkOutDate = safeCheckOutDate,
                    numberOfGuests = numberOfGuests.toIntOrNull() ?: 1,
                    contactNumber = contactNumber
                )
                // Clear form
                guestName = ""
                roomNumber = ""
                numberOfGuests = ""
                contactNumber = ""
                bookingViewModel.setCheckInDate(null)
                bookingViewModel.setCheckOutDate(null)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = guestName.isNotBlank() &&
                    roomNumber.isNotBlank() &&
                    numberOfGuests.isNotBlank() &&
                    contactNumber.isNotBlank() &&
                    checkInDate != null &&
                    checkOutDate != null
        ) {
            Text("Add Booking")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bookings List - This will now scroll with the entire screen
        if (bookings.isNotEmpty()) {
            Text(
                text = "Current Bookings:",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Show bookings in a column with SERIAL NUMBERS
            Column {
                bookings.forEachIndexed { index, booking ->
                    BookingItem(booking = booking, serialNumber = index + 1)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else {
            // Empty state
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

        // Add some extra space at the bottom for better scrolling
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
            // FIXED: Added both Serial Number AND Short ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Booking #$serialNumber",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "ID: ${booking.id.take(6)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

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