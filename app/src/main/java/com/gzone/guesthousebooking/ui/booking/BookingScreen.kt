package com.gzone.guesthousebooking.ui.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gzone.guesthousebooking.viewmodel.BookingViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BookingScreen(
    viewModel: BookingViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var guestName by remember { mutableStateOf("") }
    var roomNumber by remember { mutableStateOf("") }
    var checkInDate by remember { mutableStateOf(Date()) }
    var checkOutDate by remember { mutableStateOf(Date()) }
    var numberOfGuests by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var showCheckInDatePicker by remember { mutableStateOf(false) }
    var showCheckOutDatePicker by remember { mutableStateOf(false) }

    val bookings by viewModel.bookings.collectAsState()

    Column(modifier = modifier.padding(16.dp)) {
        // --- Input Form ---
        OutlinedTextField(
            value = guestName,
            onValueChange = { guestName = it },
            label = { Text("Guest Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = roomNumber,
            onValueChange = { roomNumber = it },
            label = { Text("Room Number") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Check-in and Check-out dates in the same row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Check-in Date Picker
            Button(
                onClick = { showCheckInDatePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Check-in: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(checkInDate)}")
            }

            // Check-out Date Picker
            Button(
                onClick = { showCheckOutDatePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Check-out: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(checkOutDate)}")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Number of Guests
            OutlinedTextField(
                value = numberOfGuests,
                onValueChange = { numberOfGuests = it },
                label = { Text("Guests") },
                modifier = Modifier.weight(1f)
            )

            // Contact Number
            OutlinedTextField(
                value = contactNumber,
                onValueChange = { contactNumber = it },
                label = { Text("Contact") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.addBooking(
                    guestName = guestName,
                    roomNumber = roomNumber.toIntOrNull() ?: 0,
                    checkInDate = checkInDate,
                    checkOutDate = checkOutDate,
                    numberOfGuests = numberOfGuests.toIntOrNull() ?: 1,
                    contactNumber = contactNumber
                )
                // clear input after saving
                guestName = ""
                roomNumber = ""
                numberOfGuests = ""
                contactNumber = ""
                checkInDate = Date()
                checkOutDate = Date()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Booking")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Bookings List ---
        Text(
            text = "Current Bookings:",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(bookings) { booking ->
                BookingItem(booking)
            }
        }
    }

    // Date Picker Dialogs
    if (showCheckInDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showCheckInDatePicker = false },
            onDateSelected = { date ->
                checkInDate = date
                showCheckInDatePicker = false
            }
        )
    }

    if (showCheckOutDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showCheckOutDatePicker = false },
            onDateSelected = { date ->
                checkOutDate = date
                showCheckOutDatePicker = false
            }
        )
    }
}

// Date Picker Dialog Composable
@Composable
fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (Date) -> Unit
) {
    var selectedDate by remember { mutableStateOf(Date()) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Date") },
        text = {
            Column {
                Text("Simple date selection - consider using a proper date picker component")
                Button(
                    onClick = { onDateSelected(selectedDate) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use current date: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(selectedDate)}")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onDateSelected(selectedDate) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            Button(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BookingItem(booking: com.gzone.guesthousebooking.data.model.Booking) {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Guest: ${booking.guestName}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Room: ${booking.roomNumber}")
            Text(text = "Guests: ${booking.numberOfGuests}")
            Text(text = "Contact: ${booking.contactNumber}")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "Check-In: ${formatter.format(booking.checkInDate)}")
                Text(text = "Check-Out: ${formatter.format(booking.checkOutDate)}")
            }
            Text(text = "Payment Status: ${booking.paymentStatus}") // Changed from "Status" to "Payment Status"
        }
    }
}