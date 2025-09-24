package com.gzone.guesthousebooking.data

import android.content.Context
import com.gzone.guesthousebooking.data.model.GuestRoom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DatabaseInitializer {

    fun initialize(context: Context) {
        val database = AppDatabase.getDatabase(context)
        val roomDao = database.roomDao()

        CoroutineScope(Dispatchers.IO).launch {
            // Check if rooms already exist
            val existingRoomsCount = roomDao.getRoomCount()
            if (existingRoomsCount == 0) {
                // Add your 40 rooms here
                val rooms = listOf(
                    GuestRoom(101, "Single", 1, "AC,TV", 1500.0),
                    GuestRoom(102, "Double", 2, "AC,TV", 2500.0),
                    GuestRoom(103, "Double", 2, "AC,TV", 2500.0),
                    GuestRoom(104, "Double", 2, "AC,TV", 2500.0),
                    GuestRoom(105, "Suite", 4, "AC,TV,Kitchen", 5000.0),
                    GuestRoom(106, "Single", 1, "AC,TV", 1500.0),
                    GuestRoom(107, "Single", 1, "AC,TV", 1500.0),
                    GuestRoom(108, "Double", 2, "AC,TV", 2500.0),
                    GuestRoom(109, "Double", 2, "AC,TV", 2500.0),
                    GuestRoom(110, "Suite", 4, "AC,TV,Kitchen", 5000.0),
                    // ... continue with all 40 rooms up to 511
                    GuestRoom(511, "Premium Suite", 4, "AC,TV,Kitchen,Jacuzzi", 6000.0)
                )
                rooms.forEach { roomDao.insert(it) }
                println("Database initialized with ${rooms.size} rooms")
            }
        }
    }
}