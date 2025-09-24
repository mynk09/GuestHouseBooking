package com.gzone.guesthousebooking.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gzone.guesthousebooking.data.model.GuestRoom
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {
    @Insert
    suspend fun insert(guestRoom: GuestRoom)

    @Update
    suspend fun update(guestRoom: GuestRoom)

    @Query("SELECT * FROM rooms ORDER BY number")
    fun getAllRooms(): Flow<List<GuestRoom>>

    @Query("SELECT * FROM rooms WHERE number = :roomNumber")
    suspend fun getRoom(roomNumber: Int): GuestRoom?

    @Query("SELECT * FROM rooms WHERE isActive = 1 ORDER BY number")
    fun getActiveRooms(): Flow<List<GuestRoom>>

    @Query("SELECT COUNT(*) FROM rooms")
    suspend fun getRoomCount(): Int
}