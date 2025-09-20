package com.gzone.guesthousebooking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gzone.guesthousebooking.ui.booking.BookingScreen
import com.gzone.guesthousebooking.viewmodel.BookingViewModel
import com.gzone.guesthousebooking.ui.theme.GuestHouseBookingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GuestHouseBookingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val bookingViewModel: BookingViewModel = viewModel()
                    BookingScreen(
                        bookingViewModel = bookingViewModel,   // ✅ fixed parameter name
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BookingPreview() {
    GuestHouseBookingTheme {
        val previewViewModel = BookingViewModel()
        BookingScreen(bookingViewModel = previewViewModel)  // ✅ fixed parameter name
    }
}
