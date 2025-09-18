package com.gzone.guesthousebooking.ui.booking

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BookingScreen(viewModel: BookingViewModel) {
    val bookings = remember { mutableStateOf(viewModel.getBookings()) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Bookings", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(12.dp))

        bookings.value.forEach { booking ->
            Text(text = "${booking.guestName} - Room ${booking.roomNumber}")
        }
    }
}
