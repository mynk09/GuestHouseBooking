package com.gzone.guesthousebooking.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "bookings",
    indices = [Index("roomNumber")],  // ✅ Improves query performance
    foreignKeys = [ForeignKey(
        entity = GuestRoom::class,
        parentColumns = ["number"],
        childColumns = ["roomNumber"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Booking(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val guestName: String,
    val roomNumber: Int,
    val checkInDate: Date,  // ✅ Will be converted via TypeConverters
    val checkOutDate: Date, // ✅ Will be converted via TypeConverters
    val numberOfGuests: Int,
    val contactNumber: String,
    val advanceAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val notes: String = "",
    val createdAt: Date = Date()  // ✅ Will be converted via TypeConverters
) {
    init {
        require(guestName.isNotBlank()) { "Guest name cannot be blank" }
        require(roomNumber > 0) { "Room number must be positive" }
        require(numberOfGuests > 0) { "Number of guests must be positive" }
        require(checkOutDate.after(checkInDate)) { "Check-out must be after check-in" }
    }
}

enum class PaymentStatus {
    PENDING, PAID, CANCELLED, REFUNDED
}