package com.gzone.guesthousebooking.data.repository

import android.content.Context
import com.gzone.guesthousebooking.data.AppDatabase

/**
 * Composition point for the active storage strategy.
 * Introduce a remote/cache implementation here without changing screens or ViewModels.
 */
object BookingRepositoryProvider {
    fun create(context: Context): BookingRepository =
        RoomBookingRepository(AppDatabase.getDatabase(context.applicationContext))
}
