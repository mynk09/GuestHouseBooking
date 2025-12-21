package com.gzone.guesthousebooking.ui.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
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

// --- UI Constants ---
private val dayCellWidth: Dp = 65.dp
private val roomCellWidth: Dp = 120.dp
private val cellHeight: Dp = 60.dp
private val bookingBarVerticalPadding: Dp = 2.dp
private val gridBorderColor: Color = Color.LightGray
private val headerCellBackground: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

@Composable
fun BookingCalendarScreen(
    viewModel: BookingViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.calendarUiState.collectAsState()
    val visibleMonth by viewModel.visibleMonth.collectAsState()

    val horizontalScrollState = rememberScrollState()

    if (uiState.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        CalendarControlHeader(
            visibleMonth = visibleMonth,
            onPreviousMonth = { viewModel.navigateToPreviousMonth() },
            onNextMonth = { viewModel.navigateToNextMonth() },
            onToday = { viewModel.returnToCurrentMonth() },
            canNavigateBackward = uiState.canNavigateBackward,
            canNavigateForward = uiState.canNavigateForward
        )

        // Header row (top-left cell + dates)
        Row(modifier = Modifier.fillMaxWidth()) {
            TopLeftLegendCell()
            Row(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                (0 until uiState.dateRange).forEach { dayIndex ->
                    val date = uiState.timelineStart.plusDays(dayIndex.toLong())
                    DateHeaderCell(date)
                }
            }
        }

        // Main content: rooms + booking grid
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(uiState.rooms, key = { room -> room.number }) { room ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoomNameCell("Room ${room.number}")

                    Box(
                        modifier = Modifier
                            .height(cellHeight)
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        // Background cells
                        Row {
                            (0 until uiState.dateRange).forEach { _ ->
                                DayBackgroundCell()
                            }
                        }

                        // Booking bars
                        val bookingsForRoom =
                            uiState.bookings.filter { it.roomNumber == room.number }
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPreviousMonth, enabled = canNavigateBackward) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
        }

        Text(
            text = visibleMonth.format(
                DateTimeFormatter.ofPattern(
                    "MMMM yyyy",
                    Locale.getDefault()
                )
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onToday,
                modifier = Modifier.padding(end = 8.dp),
                shape = CircleShape
            ) {
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
private fun TopLeftLegendCell() {
    Box(
        modifier = Modifier
            .width(roomCellWidth)
            .height(cellHeight)
            .border(0.5.dp, gridBorderColor)
            .background(headerCellBackground),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.List,
            contentDescription = "Bookings legend",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DateHeaderCell(date: LocalDate) {
    Column(
        modifier = Modifier
            .width(dayCellWidth)
            .height(cellHeight)
            .border(0.5.dp, gridBorderColor)
            .background(headerCellBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("E")),
            fontSize = 12.sp
        )
        Text(
            text = date.dayOfMonth.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun RoomNameCell(name: String) {
    Box(
        modifier = Modifier
            .width(roomCellWidth)
            .height(cellHeight)
            .border(0.5.dp, gridBorderColor)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = name,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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

    val effectiveStart = if (bookingStart.isBefore(monthStart)) monthStart else bookingStart
    val effectiveEnd = if (bookingEnd.isAfter(monthEnd)) monthEnd.plusDays(1) else bookingEnd

    val durationInDays =
        ChronoUnit.DAYS.between(effectiveStart, effectiveEnd).coerceAtLeast(0)
    val offsetInDays =
        ChronoUnit.DAYS.between(monthStart, effectiveStart).coerceAtLeast(0)

    if (durationInDays > 0) {
        val bookingWidth = (durationInDays * dayCellWidth.value).dp
        val bookingOffset = (offsetInDays * dayCellWidth.value).dp

        // Booking bar height slightly less than cellHeight and centered
        val barHeight = cellHeight - bookingBarVerticalPadding * 2

        Box(
            modifier = Modifier
                .padding(
                    start = bookingOffset,
                    top = bookingBarVerticalPadding,
                    bottom = bookingBarVerticalPadding
                )
                .width(bookingWidth)
                .height(barHeight)
                .background(
                    MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(4.dp)
                ),
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
