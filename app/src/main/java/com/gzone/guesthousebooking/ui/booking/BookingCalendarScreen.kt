package com.gzone.guesthousebooking.ui.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gzone.guesthousebooking.data.model.Booking
import com.gzone.guesthousebooking.viewmodel.BookingViewModel
import com.gzone.guesthousebooking.viewmodel.toLocalDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// --- UI Constants ---
private val dayCellWidth: Dp = 65.dp
private val roomCellWidth: Dp = 120.dp
private val cellHeight: Dp = 60.dp
private val gridBorderColor: Color = Color.LightGray

@Composable
fun BookingCalendarScreen(viewModel: BookingViewModel) {
    // Observe the correct state from the ViewModel
    val uiState by viewModel.calendarUiState.collectAsState()
    val horizontalScrollState = rememberScrollState()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- 1. Header Row (Dates) ---
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(roomCellWidth)) // Top-left empty cell
            Row(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                (0 until uiState.dateRange).forEach { dayIndex ->
                    val date = uiState.timelineStart.plusDays(dayIndex.toLong())
                    DateHeaderCell(date)
                }
            }
        }

        // --- 2. Main Content (Rooms and Bookings Grid) ---
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(uiState.rooms, key = { room -> room.number }) { room ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoomNameCell("Room ${room.number}")

                    // Booking Grid Row (Scrollable)
                    Box(
                        modifier = Modifier
                            .height(cellHeight)
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        // Background cells
                        Row {
                            (0 until uiState.dateRange).forEach { DayBackgroundCell() }
                        }

                        // Overlay bookings
                        val bookingsForRoom = uiState.bookings.filter { it.roomNumber == room.number }
                        bookingsForRoom.forEach { booking ->
                            BookingItem(
                                booking = booking,
                                timelineStartDate = uiState.timelineStart,
                                dateRange = uiState.dateRange
                            )
                        }
                    }
                }
            }
        }
    }
}

// region Sub-Composables
@Composable
private fun DateHeaderCell(date: LocalDate) {
    Column(
        modifier = Modifier
            .width(dayCellWidth)
            .height(cellHeight)
            .border(0.5.dp, gridBorderColor),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = date.format(DateTimeFormatter.ofPattern("E")), fontSize = 12.sp)
        Text(text = date.dayOfMonth.toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun RoomNameCell(name: String) {
    Box(
        modifier = Modifier
            .width(roomCellWidth)
            .height(cellHeight)
            .border(0.5.dp, gridBorderColor)
            .padding(8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = name, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DayBackgroundCell() {
    Spacer(
        modifier = Modifier
            .width(dayCellWidth)
            .height(cellHeight)
            .border(width = 0.5.dp, color = gridBorderColor)
    )
}

@Composable
private fun BookingItem(booking: Booking, timelineStartDate: LocalDate, dateRange: Int) {
    val checkIn = booking.checkInDate.toLocalDate()
    val checkOut = booking.checkOutDate.toLocalDate()

    val durationInDays = ChronoUnit.DAYS.between(checkIn, checkOut).coerceAtLeast(0)
    val offsetInDays = ChronoUnit.DAYS.between(timelineStartDate, checkIn)

    if (durationInDays > 0 && offsetInDays < dateRange && offsetInDays + durationInDays >= 0) {
        val bookingWidth = (durationInDays * dayCellWidth.value).dp
        val bookingOffset = (offsetInDays * dayCellWidth.value).dp

        Box(
            modifier = Modifier
                .offset(x = bookingOffset)
                .width(bookingWidth)
                .fillMaxHeight()
                .padding(2.dp)
                .background(Color(0xFF3F51B5), shape = RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFF303F9F), shape = RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = booking.guestName,
                color = Color.White,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
// endregion
