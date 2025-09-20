package com.gzone.guesthousebooking.ui.booking

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
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

    // Date pickers
    val checkInDatePicker = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                bookingViewModel.setCheckInDate(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val checkOutDatePicker = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                bookingViewModel.setCheckOutDate(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    // Observe dates from ViewModel
    val checkInDate by bookingViewModel.checkInDate.collectAsState()
    val checkOutDate by bookingViewModel.checkOutDate.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Changed title to "TRACK YOUR BOOKINGS"
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
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
        )

        // Room Number
        OutlinedTextField(
            value = roomNumber,
            onValueChange = { roomNumber = it },
            label = { Text("Room Number") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
        )

        // Date Selection Row - LARGER fields for better visibility
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Check-in Date - LARGER field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp) // Increased height for better visibility
            ) {
                OutlinedTextField(
                    value = checkInDate?.let { dateFormatter.format(it) } ?: "",
                    onValueChange = {},
                    label = { Text("Check-in") },
                    readOnly = true,
                    modifier = Modifier.fillMaxSize(),
                    trailingIcon = {
                        IconButton(onClick = { checkInDatePicker.show() }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick Check-in Date")
                        }
                    },
                    singleLine = true // Ensure text doesn't get cut off
                )
            }

            // Check-out Date - LARGER field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp) // Increased height for better visibility
            ) {
                OutlinedTextField(
                    value = checkOutDate?.let { dateFormatter.format(it) } ?: "",
                    onValueChange = {},
                    label = { Text("Check-out") },
                    readOnly = true,
                    modifier = Modifier.fillMaxSize(),
                    trailingIcon = {
                        IconButton(onClick = { checkOutDatePicker.show() }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick Check-out Date")
                        }
                    },
                    singleLine = true // Ensure text doesn't get cut off
                )
            }
        }

        // Number of Guests and Contact - FIXED ALIGNMENT
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = numberOfGuests,
                onValueChange = { numberOfGuests = it },
                label = { Text("Number of Guests") },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { /* Focus moves to next field */ })
            )

            OutlinedTextField(
                value = contactNumber,
                onValueChange = { contactNumber = it },
                label = { Text("Contact Number") },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { /* Keyboard dismissed */ })
            )
        }

        // Add Booking Button
        Button(
            onClick = {
                bookingViewModel.addBooking(
                    guestName = guestName,
                    roomNumber = roomNumber.toIntOrNull() ?: 0,
                    checkInDate = checkInDate ?: Date(),
                    checkOutDate = checkOutDate ?: Date(),
                    numberOfGuests = numberOfGuests.toIntOrNull() ?: 1,
                    contactNumber = contactNumber
                )
                // Clear form
                guestName = ""
                roomNumber = ""
                numberOfGuests = ""
                contactNumber = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Booking")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bookings List
        if (bookings.isNotEmpty()) {
            Text(
                text = "Current Bookings:",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                itemsIndexed(bookings) { index, booking ->
                    BookingItem(booking = booking, bookingNumber = index + 1)
                }
            }
        }
    }
}

@Composable
fun BookingItem(booking: com.gzone.guesthousebooking.data.model.Booking, bookingNumber: Int) {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Added Booking Number and shortened ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Booking #$bookingNumber",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "ID: ${booking.id.take(8)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Guest: ${booking.guestName}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Room: ${booking.roomNumber}")
            Text(text = "Guests: ${booking.numberOfGuests}")
            Text(text = "Contact: ${booking.contactNumber}")

            // Improved date display
            Text(
                text = "Dates: ${formatter.format(booking.checkInDate)} to ${formatter.format(booking.checkOutDate)}",
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Color-coded payment status
            Text(
                text = "Payment Status: ${booking.paymentStatus}",
                color = when (booking.paymentStatus.toString().uppercase()) {
                    "PAID" -> MaterialTheme.colorScheme.primary
                    "PENDING" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}