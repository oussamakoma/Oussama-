package com.example.data.local

import androidx.room.*
import com.example.data.model.RefurbishedDevice
import kotlinx.coroutines.flow.Flow

@Dao
interface RefurbishedDeviceDao {
    @Query("SELECT * FROM refurbished_devices WHERE id = :deviceId")
    suspend fun getDeviceById(deviceId: Int): RefurbishedDevice?

    @Query("SELECT * FROM refurbished_devices ORDER BY createdAt DESC")
    fun getAllDevices(): Flow<List<RefurbishedDevice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: RefurbishedDevice): Long

    @Update
    suspend fun updateDevice(device: RefurbishedDevice)

    @Delete
    suspend fun deleteDevice(device: RefurbishedDevice)

    @Query("DELETE FROM refurbished_devices")
    suspend fun deleteAllDevices()
}
