package com.gzone.guesthousebooking.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gzone.guesthousebooking.data.model.Booking
import com.gzone.guesthousebooking.data.model.GuestRoom

@Database(
    entities = [Booking::class, GuestRoom::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookingDao(): BookingDao
    abstract fun roomDao(): RoomDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,  // ✅ Use applicationContext
                    AppDatabase::class.java,
                    "guesthouse.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                Instance = instance
                instance
            }
        }
    }
}