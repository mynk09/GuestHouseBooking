package com.gzone.guesthousebooking.data

import android.content.Context
import com.gzone.guesthousebooking.data.model.GuestRoom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DatabaseInitializer {

    private var isInitialized = false

    fun initialize(context: Context) {
        // This ensures the initialization runs only once per app launch
        if (isInitialized) return
        isInitialized = true

        val database = AppDatabase.getDatabase(context)
        val roomDao = database.roomDao()

        // We run this in a background thread so it doesn't block the UI
        CoroutineScope(Dispatchers.IO).launch {
            // Check if the rooms table is empty
            if (roomDao.getRoomCount() == 0) {
                println("Database is empty. Initializing rooms...")
                insertAllRooms(roomDao)
            } else {
                println("Database already contains rooms.")
            }
        }
    }

    private suspend fun insertAllRooms(roomDao: RoomDao) {
        val roomsToInsert = mutableListOf<GuestRoom>()

        // Function to easily add a range of standard rooms
        fun addRoomRange(start: Int, end: Int, type: String, capacity: Int, rate: Double) {
            for (i in start..end) {
                roomsToInsert.add(
                    GuestRoom(
                        number = i,
                        type = type,
                        capacity = capacity,
                        ratePerNight = rate,
                        amenities = "AC,TV,WiFi" // Standard amenities
                    )
                )
            }
        }

        // Add rooms from 101-108 (Assuming they are 'Double' rooms)
        addRoomRange(101, 108, type = "Double", capacity = 2, rate = 2500.0)

        // Add rooms from 201-210 (Assuming they are 'Double' as well)
        addRoomRange(201, 210, type = "Double", capacity = 2, rate = 2600.0)

        // Add rooms from 301-310
        addRoomRange(301, 310, type = "Double", capacity = 2, rate = 2700.0)

        // Add rooms from 401-410
        addRoomRange(401, 410, type = "Double", capacity = 2, rate = 2800.0)

        // --- Add the special 3-bed bigger rooms ---
        roomsToInsert.add(
            GuestRoom(
                number = 211,
                type = "Family Suite",
                capacity = 3,
                ratePerNight = 4500.0,
                amenities = "AC,TV,WiFi,Mini-Fridge"
            )
        )
        roomsToInsert.add(
            GuestRoom(
                number = 311,
                type = "Family Suite",
                capacity = 3,
                ratePerNight = 4600.0,
                amenities = "AC,TV,WiFi,Mini-Fridge"
            )
        )
        roomsToInsert.add(
            GuestRoom(
                number = 411,
                type = "Family Suite",
                capacity = 3,
                ratePerNight = 4700.0,
                amenities = "AC,TV,WiFi,Mini-Fridge"
            )
        )
        roomsToInsert.add(
            GuestRoom(
                number = 511,
                type = "Premium Suite",
                capacity = 3, // Or more, adjust as needed
                ratePerNight = 6000.0,
                amenities = "AC,TV,WiFi,Jacuzzi,Kitchenette"
            )
        )

        // Insert all the created rooms into the database
        roomsToInsert.forEach { room ->
            roomDao.insert(room)
        }

        println("Successfully inserted ${roomsToInsert.size} rooms into the database.")
    }
}
