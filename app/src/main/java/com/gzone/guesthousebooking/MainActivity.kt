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
import com.gzone.guesthousebooking.data.DatabaseInitializer
import com.gzone.guesthousebooking.ui.booking.BookingScreen
import com.gzone.guesthousebooking.viewmodel.BookingViewModel
import com.gzone.guesthousebooking.ui.theme.GuestHouseBookingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize database with rooms
        DatabaseInitializer.initialize(applicationContext)
        enableEdgeToEdge()

        setContent {
            GuestHouseBookingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // ✅ Now using AndroidViewModel which takes Application context
                    val bookingViewModel: BookingViewModel = viewModel()
                    BookingScreen(
                        bookingViewModel = bookingViewModel,
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
        // Simple preview without database dependencies
        BookingScreen(
            bookingViewModel = BookingViewModel(getApplication()), // This won't work in preview
            modifier = Modifier.fillMaxSize()
        )
    }
}

// Helper function for preview (won't actually work but prevents compile errors)
@Composable
fun getApplication(): android.app.Application {
    return android.app.Application()
}