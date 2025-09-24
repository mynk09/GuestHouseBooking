package com.gzone.guesthousebooking.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rooms")
data class GuestRoom(
    @PrimaryKey val number: Int,
    val type: String,           // "Single", "Double", "Suite"
    val capacity: Int,
    val amenities: String = "", // "AC,TV,WiFi"
    val ratePerNight: Double,
    val isActive: Boolean = true
)