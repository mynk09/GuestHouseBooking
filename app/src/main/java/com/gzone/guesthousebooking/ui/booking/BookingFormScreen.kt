package com.gzone.guesthousebooking.ui.booking

import android.app.DatePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gzone.guesthousebooking.R
import com.gzone.guesthousebooking.data.model.Booking
import com.gzone.guesthousebooking.data.model.GuestRoom
import com.gzone.guesthousebooking.viewmodel.BookingViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun BookingFormScreen(
    bookingViewModel: BookingViewModel,
    modifier: Modifier = Modifier
) {
    val guestName by bookingViewModel.guestName.collectAsState()
    val numberOfGuests by bookingViewModel.numberOfGuests.collectAsState()
    val contactNumber by bookingViewModel.contactNumber.collectAsState()
    val selectedRoomNumbers by bookingViewModel.selectedRoomNumbers.collectAsState()
    val availableRooms by bookingViewModel.availableRooms.collectAsState()
    val checkInDate by bookingViewModel.checkInDate.collectAsState()
    val checkOutDate by bookingViewModel.checkOutDate.collectAsState()
    val allBookings = bookingViewModel.calendarUiState.collectAsState().value.bookings

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = "Guesthouse booking",
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "TRACK ROOM BOOKINGS",
                    style = MaterialTheme.typography.titleLarge.copy(   // bolder, larger
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

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

        item {
            Button(
                onClick = { bookingViewModel.addBooking() },
                modifier = Modifier.fillMaxWidth(),
                enabled = guestName.isNotBlank() &&
                        selectedRoomNumbers.isNotEmpty() &&
                        checkInDate != null &&
                        checkOutDate != null
            ) {
                Text(
                    "Add ${
                        if (selectedRoomNumbers.size > 1)
                            "${selectedRoomNumbers.size} Bookings"
                        else
                            "Booking"
                    }"
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Current Bookings:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (allBookings.isNotEmpty()) {
            items(
                allBookings.sortedByDescending { it.checkInDate },
                key = { it.id }
            ) { booking ->
                BookingListItem(
                    booking = booking,
                    onDelete = { bookingViewModel.deleteBooking(booking) }
                )
                Spacer(modifier = Modifier.height(8.dp))
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingFormFields(
    bookingViewModel: BookingViewModel,
    guestName: String,
    numberOfGuests: String,
    contactNumber: String,
    selectedRoomNumbers: List<Int>,
    availableRooms: List<GuestRoom>,
    checkInDate: Date?,
    checkOutDate: Date?
) {
    val context = LocalContext.current
    val dateFormatter = remember {
        SimpleDateFormat("dd-MMM-yy", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    // Bottom sheet state
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()
    var isSheetOpen by remember { mutableStateOf(false) }

    fun showDatePicker(isCheckIn: Boolean) {
        val initialDate = if (isCheckIn) checkInDate else checkOutDate
        val calendar = Calendar.getInstance().apply { initialDate?.let { time = it } }

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                if (isCheckIn) {
                    bookingViewModel.setCheckInDate(calendar.time)
                } else {
                    bookingViewModel.setCheckOutDate(calendar.time)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            if (!isCheckIn) {
                checkInDate?.let { datePicker.minDate = it.time + (24L * 60 * 60 * 1000) }
            }
        }.show()
    }

    // Modal Bottom Sheet for room selection
    if (isSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    isSheetOpen = false
                }
            },
            sheetState = sheetState,
            modifier = Modifier.fillMaxHeight()
        ) {
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                item {
                    Text(
                        "Select Available Rooms",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                if (availableRooms.isEmpty()) {
                    item {
                        Text(
                            "No rooms available for the selected dates.",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    items(availableRooms, key = { it.number }) { room ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    bookingViewModel.onRoomSelectionChange(room.number)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Room ${room.number} - ${room.type}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    "₹${room.ratePerNight}/night • Capacity: ${room.capacity}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Checkbox(
                                checked = selectedRoomNumbers.contains(room.number),
                                onCheckedChange = null
                            )
                        }
                        Divider()
                    }
                }
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                sheetState.hide()
                            }.invokeOnCompletion {
                                isSheetOpen = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                onIconClick = {
                    if (checkInDate != null) {
                        showDatePicker(isCheckIn = false)
                    }
                },
                enabled = checkInDate != null
            )
        }

        OutlinedTextField(
            value = guestName,
            onValueChange = { bookingViewModel.onGuestNameChange(it) },
            label = { Text("Guest Name") },
            modifier = Modifier.fillMaxWidth()
        )

        // CLICKABLE TEXT FIELD TO OPEN THE BOTTOM SHEET (via interactionSource)
        val roomsFieldEnabled = checkInDate != null && checkOutDate != null
        val roomsFieldText =
            if (selectedRoomNumbers.isNotEmpty()) {
                "Selected: ${selectedRoomNumbers.sorted().joinToString(", ")}"
            } else {
                "Select Available Rooms"
            }

        val roomsInteractionSource = remember { MutableInteractionSource() }

        OutlinedTextField(
            value = roomsFieldText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Select Rooms") },
            trailingIcon = {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Open room selection"
                )
            },
            enabled = roomsFieldEnabled,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(),
            interactionSource = roomsInteractionSource
        )

        // Open sheet when the field is pressed or focused
        LaunchedEffect(roomsInteractionSource, roomsFieldEnabled) {
            roomsInteractionSource.interactions.collect { interaction ->
                if (!roomsFieldEnabled) return@collect
                when (interaction) {
                    is PressInteraction.Release,
                    is FocusInteraction.Focus -> {
                        if (!isSheetOpen) {
                            isSheetOpen = true
                            scope.launch { sheetState.show() }
                        }
                    }
                    else -> Unit
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = numberOfGuests,
                onValueChange = { bookingViewModel.onNumberOfGuestsChange(it) },
                label = { Text("Guests") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = contactNumber,
                onValueChange = { newText ->
                    if (newText.length <= 10 && newText.all { it.isDigit() }) {
                        bookingViewModel.onContactNumberChange(newText)
                    }
                },
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
        modifier = Modifier.weight(1f),
        value = date?.let { dateFormatter.format(it) } ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        enabled = enabled,
        trailingIcon = {
            IconButton(onClick = onIconClick, enabled = enabled) {
                Icon(Icons.Default.DateRange, contentDescription = "Select Date")
            }
        }
    )
}

@Composable
private fun BookingListItem(
    booking: Booking,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yy", Locale.US) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "ID: #${booking.id.take(8)}...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Room ${booking.roomNumber}: ${booking.guestName}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Booking",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Divider(modifier = Modifier.padding(horizontal = 12.dp))

            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Check-in: ${dateFormatter.format(booking.checkInDate)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Check-out: ${dateFormatter.format(booking.checkOutDate)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Guests: ${booking.numberOfGuests}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Contact: ${booking.contactNumber}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
