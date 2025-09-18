package com.gzone.guesthousebooking.data.model

import java.util.Date

data class Booking(
    val id: String,              // Unique booking ID
    val guestName: String,       // Name of the guest
    val roomNumber: Int,         // Assigned room number
    val checkInDate: Date,       // Check-in date
    val checkOutDate: Date,      // Check-out date
    val numberOfGuests: Int,     // Total guests staying
    val contactNumber: String,   // Guest contact info
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING
)

// Enum to track booking payment state
enum class PaymentStatus {
    PENDING,
    PAID,
    CANCELLED
}
