package com.gzone.guesthousebooking.ui.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward

// --- UI Constants ---
private val dayCellWidth: Dp = 65.dp
private val roomCellWidth: Dp = 120.dp
private val cellHeight: Dp = 60.dp
private val gridBorderColor: Color = Color.LightGray

@Composable
fun BookingCalendarScreen(viewModel: BookingViewModel) {
    // Observe the correct state from the ViewModel
    val uiState by viewModel.calendarUiState.collectAsState()
    val visibleMonth by viewModel.visibleMonth.collectAsState()

    val horizontalScrollState = rememberScrollState()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.systemBars)) {

        CalendarControlHeader(
            visibleMonth = visibleMonth,
            onPreviousMonth = { viewModel.navigateToPreviousMonth() },
            onNextMonth = { viewModel.navigateToNextMonth() },
            onToday = { viewModel.returnToCurrentMonth() },
            canNavigateBackward = uiState.canNavigateBackward,
            canNavigateForward = uiState.canNavigateForward
        )

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
                            (0 until uiState.dateRange).forEach { _ -> DayBackgroundCell() }
                        }

                        // Overlay bookings
                        val bookingsForRoom = uiState.bookings.filter { it.roomNumber == room.number }
                        bookingsForRoom.forEach { booking ->
                            BookingItem(
                                booking = booking,
                                visibleMonth = visibleMonth
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarControlHeader(
    visibleMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    canNavigateBackward: Boolean,
    canNavigateForward: Boolean
){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPreviousMonth, enabled = canNavigateBackward) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
        }

        Text(
            text = visibleMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy",
                Locale.getDefault())),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row {
            // "Today" button is useful for quickly returning to the current month
            Button(onClick = onToday, modifier = Modifier.padding(end = 8.dp)) {
                Text("Today")
            }
            IconButton(onClick = onNextMonth, enabled = canNavigateForward) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
            }
        }
    }
}

// region Sub-Composable
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
private fun BookingItem(booking: Booking, visibleMonth: YearMonth) {
    val bookingStart = booking.checkInDate.toLocalDate()
    val bookingEnd = booking.checkOutDate.toLocalDate()
    val monthStart = visibleMonth.atDay(1)
    val monthEnd = visibleMonth.atEndOfMonth()

    val effectiveStart =  if (bookingStart.isBefore(monthStart)) monthStart else bookingStart
    // Effective end is the earlier of booking end or month end
    val effectiveEnd = if (bookingEnd.isAfter(monthEnd)) monthEnd.plusDays(1) else bookingEnd

    val durationInDays = ChronoUnit.DAYS.between(effectiveStart, effectiveEnd).coerceAtLeast(0)
    // Offset is from the start of the visible month
    val offsetInDays = ChronoUnit.DAYS.between(monthStart, effectiveStart).coerceAtLeast(0)

    if (durationInDays > 0) {
        val bookingWidth = (durationInDays * dayCellWidth.value).dp
        val bookingOffset = (offsetInDays * dayCellWidth.value).dp

        Box(
            modifier = Modifier
                .offset(x = bookingOffset)
                .width(bookingWidth)
                .fillMaxHeight()
                .padding(2.dp)
                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp))
                .border(1.dp, MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(4.dp)),
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
