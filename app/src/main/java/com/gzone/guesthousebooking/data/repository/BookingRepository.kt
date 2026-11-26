package com.gzone.guesthousebooking.data.repository

import com.gzone.guesthousebooking.data.model.Booking
import com.gzone.guesthousebooking.data.model.GuestRoom
import kotlinx.coroutines.flow.Flow

/**
 * Storage strategy consumed by the booking UI.
 *
 * Implementations own threading, persistence and final availability validation.
 * Availability is advisory; only a successful createBookings result confirms a reservation.
 * A remote implementation must enforce permissions and conflicts on the server.
 */
interface BookingRepository {
    fun observeBookings(): Flow<List<Booking>>
    fun observeRooms(): Flow<List<GuestRoom>>

    /** Confirms every room in the request atomically, or confirms none of them. */
    suspend fun createBookings(bookings: List<Booking>): BookingWriteResult

    suspend fun deleteBooking(bookingId: String): BookingWriteResult
}

sealed interface BookingWriteResult {
    object Success : BookingWriteResult
    data class Unavailable(val roomNumbers: Set<Int>) : BookingWriteResult
    data class Failed(val reason: BookingFailure) : BookingWriteResult
}

/** Provider-independent failures; never expose SQL, sheet rows or HTTP details to the UI. */
enum class BookingFailure {
    INVALID_REQUEST,
    ACCESS_DENIED,
    CONNECTION_REQUIRED,
    STORAGE_ERROR
}
