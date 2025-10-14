package com.gzone.guesthousebooking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.gzone.guesthousebooking.data.DatabaseInitializer
import com.gzone.guesthousebooking.ui.booking.BookingCalendarScreen
import com.gzone.guesthousebooking.ui.booking.BookingFormScreen // <-- Make sure to import your renamed form screen
import com.gzone.guesthousebooking.ui.theme.GuestHouseBookingTheme
import com.gzone.guesthousebooking.viewmodel.BookingViewModel

// Data class to represent each item in our navigation bar
private data class NavItem(val label: String, val icon: ImageVector, val screenRoute: String)

class MainActivity : ComponentActivity() {
    // Initialize the BookingViewModel once for the entire activity lifecycle.
    // Both screens will share this same instance.
    private val bookingViewModel: BookingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Your database initializer - keep this as it is.
        DatabaseInitializer.initialize(applicationContext)
        enableEdgeToEdge()

        setContent {
            GuestHouseBookingTheme {
                // MainApp composable that contains all the navigation logic
                MainApp(bookingViewModel = bookingViewModel)
            }
        }
    }
}

@Composable
private fun MainApp(bookingViewModel: BookingViewModel) {
    // State to keep track of the currently selected screen. Default to the form.
    var currentScreen by remember { mutableStateOf("form") }

    // List of screens for the navigation bar
    val navItems = listOf(
        NavItem("Booking Form", Icons.Default.Edit, "form"),
        NavItem("Calendar View", Icons.Default.DateRange, "calendar")
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                navItems.forEach { navItem ->
                    NavigationBarItem(
                        icon = { Icon(navItem.icon, contentDescription = navItem.label) },
                        label = { Text(navItem.label) },
                        selected = currentScreen == navItem.screenRoute,
                        onClick = { currentScreen = navItem.screenRoute }
                    )
                }
            }
        }
    ) { innerPadding ->
        // This `when` block acts as a simple navigator.
        // It displays the correct screen based on the `currentScreen` state.

        Surface(
            modifier = Modifier
                .fillMaxSize()) {
            when (currentScreen) {
                "form" -> {
                    // Show your original booking form screen
                    BookingFormScreen(
                        bookingViewModel = bookingViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                "calendar" -> {
                    // Show the new calendar view screen
                    BookingCalendarScreen(
                        viewModel = bookingViewModel
                        // The padding is applied automatically by the Scaffold's content lambda
                    )
                }
            }
        }
    }
}
