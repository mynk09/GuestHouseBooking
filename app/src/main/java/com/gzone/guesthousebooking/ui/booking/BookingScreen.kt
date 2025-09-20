package com.gzone.guesthousebooking.ui.booking

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gzone.guesthousebooking.data.model.Booking
import com.gzone.guesthousebooking.data.model.PaymentStatus
import com.gzone.guesthousebooking.viewmodel.BookingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    bookingViewModel: BookingViewModel,             // matches your MainActivity call
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // --- form fields ---
    var guestName by remember { mutableStateOf("") }
    var numberOfGuests by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }

    // --- room multi-select (checkbox dropdown) ---
    val availableRooms = listOf(
        // Floor 1
        "101", "102", "103", "104", "105", "106", "107", "108",
        // Floor 2
        "201", "202", "203", "204", "205", "206", "207", "208", "209", "210", "211",
        // Floor 3
        "301", "302", "303", "304", "305", "306", "307", "308", "309", "310", "311",
        // Floor 4
        "401", "402", "403", "404", "405", "406", "407", "408", "409", "410", "411",
        // Floor 5
        "501" // Or "511" if you specifically need that one instead/as well
    )
    var roomsExpanded by remember { mutableStateOf(false) }
    var selectedRooms by remember { mutableStateOf(setOf<String>()) }

    // --- date state comes from ViewModel (keep centralized logic) ---
    val checkInDate by bookingViewModel.checkInDate.collectAsState()
    val checkOutDate by bookingViewModel.checkOutDate.collectAsState()

    // DatePicker dialogs
    val nowCal = Calendar.getInstance()

    // Check-in DatePicker (created once)
    val checkInPicker = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val c = Calendar.getInstance()
                c.set(year, month, dayOfMonth, 0, 0, 0)
                c.set(Calendar.MILLISECOND, 0)
                bookingViewModel.setCheckInDate(c.time)
                // if checkout earlier than new checkin, clear checkout (viewModel)
                val co = bookingViewModel.checkOutDate.value
                if (co != null && co.before(c.time)) bookingViewModel.setCheckOutDate(null)
            },
            nowCal.get(Calendar.YEAR),
            nowCal.get(Calendar.MONTH),
            nowCal.get(Calendar.DAY_OF_MONTH)
        )
    }

    // Check-out DatePicker (recreated when checkInDate changes so minDate updates)
    val checkOutPicker = remember(checkInDate) {
        val initCal = Calendar.getInstance().apply {
            if (checkInDate != null) time = checkInDate
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val c = Calendar.getInstance()
                c.set(year, month, dayOfMonth, 0, 0, 0)
                c.set(Calendar.MILLISECOND, 0)
                // only allow if after checkInDate -- but we also set minDate below
                if (checkInDate == null || c.time.after(checkInDate)) {
                    bookingViewModel.setCheckOutDate(c.time)
                }
            },
            initCal.get(Calendar.YEAR),
            initCal.get(Calendar.MONTH),
            initCal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            // set minimum selectable date
            datePicker.minDate = checkInDate?.time ?: System.currentTimeMillis() - 1000L
        }
    }

    // Observe bookings
    val bookings by bookingViewModel.bookings.collectAsState()

    // whole screen scrolls (keeps layout stable on small screens)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "TRACK YOUR BOOKINGS",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth()
        )

        // Guest name
        OutlinedTextField(
            value = guestName,
            onValueChange = { guestName = it },
            label = { Text("Guest Name") },
            modifier = Modifier.fillMaxWidth()
        )

        // ---- ROOM MULTI-SELECT: placed where your room field was (not moved) ----
        ExposedDropdownMenuBox(
            expanded = roomsExpanded,
            onExpandedChange = { roomsExpanded = !roomsExpanded }
        ) {
            OutlinedTextField(
                value = selectedRooms.joinToString(", "),
                onValueChange = {},
                label = { Text("Select Rooms") },
                readOnly = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roomsExpanded) }
            )

            DropdownMenu(
                expanded = roomsExpanded,
                onDismissRequest = { roomsExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                availableRooms.forEach { room ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedRooms.contains(room),
                                    onCheckedChange = { checked ->
                                        selectedRooms = if (checked) selectedRooms + room else selectedRooms - room
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = room)
                            }
                        },
                        onClick = { /* keep menu open, checkbox handles selection */ }
                    )
                }
            }
        }

        // Date row (larger boxes so text doesn't reflow)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Check-in field (always enabled)
            Box(modifier = Modifier
                .weight(1f)
                .height(64.dp)) {
                OutlinedTextField(
                    value = checkInDate?.let { dateFormatter.format(it) } ?: "",
                    onValueChange = {},
                    label = { Text("Check-in") },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxSize(),
                    trailingIcon = {
                        IconButton(onClick = { checkInPicker.show() }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick check-in")
                        }
                    }
                )
            }

            // Check-out field (disabled until check-in selected)
            Box(modifier = Modifier
                .weight(1f)
                .height(64.dp)) {
                OutlinedTextField(
                    value = checkOutDate?.let { dateFormatter.format(it) } ?: "",
                    onValueChange = {},
                    label = { Text("Check-out") },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxSize(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                // only open if check-in chosen
                                if (checkInDate != null) checkOutPicker.show()
                            },
                            enabled = checkInDate != null
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick check-out")
                        }
                    },
                    enabled = checkInDate != null
                )
            }
        }

        // Number of Guests / Contact (kept where they were)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = numberOfGuests,
                onValueChange = { numberOfGuests = it },
                label = { Text("Guests") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f).heightIn(min = 50.dp)
            )

            OutlinedTextField(
                value = contactNumber,
                onValueChange = { contactNumber = it },
                label = { Text("Contact") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(1f).heightIn(min = 50.dp)
            )
        }

        // Add Booking button (splits into multiple bookings if multiple rooms selected)
        Button(
            onClick = {
                // validation: require guest, rooms, dates
                val ci = checkInDate
                val co = checkOutDate
                if (guestName.isNotBlank() && selectedRooms.isNotEmpty() && ci != null && co != null) {
                    selectedRooms.forEach { room ->
                        bookingViewModel.addBooking(
                            guestName = guestName,
                            roomNumber = room.toIntOrNull() ?: 0,
                            checkInDate = ci,
                            checkOutDate = co,
                            numberOfGuests = numberOfGuests.toIntOrNull() ?: 1,
                            contactNumber = contactNumber
                        )
                    }
                    // clear form and central date state
                    guestName = ""
                    numberOfGuests = ""
                    contactNumber = ""
                    selectedRooms = emptySet()
                    bookingViewModel.setCheckInDate(null)
                    bookingViewModel.setCheckOutDate(null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = guestName.isNotBlank() && selectedRooms.isNotEmpty() && checkInDate != null && checkOutDate != null
        ) {
            Text("Add Booking")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Bookings list (unchanged, scrolls with content) ---
        if (bookings.isNotEmpty()) {
            Text(text = "Current Bookings:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Column {
                bookings.forEachIndexed { index, booking ->
                    BookingItem(booking = booking, serialNumber = index + 1)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
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
fun BookingItem(booking: Booking, serialNumber: Int) {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Booking #$serialNumber", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary))
                Text(text = "ID: ${booking.id.take(6)}...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Guest: ${booking.guestName}", style = MaterialTheme.typography.bodyMedium)
            Text("Room: ${booking.roomNumber}", style = MaterialTheme.typography.bodyMedium)
            Text("Guests: ${booking.numberOfGuests}", style = MaterialTheme.typography.bodyMedium)
            Text("Contact: ${booking.contactNumber}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Dates: ${formatter.format(booking.checkInDate)} to ${formatter.format(booking.checkOutDate)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
            Text(text = "Status: ${booking.paymentStatus}", style = MaterialTheme.typography.bodyMedium, color = when (booking.paymentStatus) {
                PaymentStatus.PAID -> MaterialTheme.colorScheme.primary
                PaymentStatus.PENDING -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            })
        }
    }
}
