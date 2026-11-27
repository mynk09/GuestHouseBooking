package com.gzone.guesthousebooking.data

import androidx.room.TypeConverter
import com.gzone.guesthousebooking.data.model.PaymentStatus
import java.util.Date

class Converters {
    /**
     * Converts a Long timestamp into a java.util.Date object.
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    /**
     * Converts a java.util.Date object into a Long timestamp for database storage.
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    /**
     * Converts a String from the database into a PaymentStatus enum.
     */
    @TypeConverter
    fun fromPaymentStatus(value: String?): PaymentStatus? {
        return value?.let { PaymentStatus.valueOf(it) }
    }

    /**
     * Converts a PaymentStatus enum into a String for database storage.
     */
    @TypeConverter
    fun toPaymentStatus(paymentStatus: PaymentStatus?): String? {
        return paymentStatus?.name
    }
}
