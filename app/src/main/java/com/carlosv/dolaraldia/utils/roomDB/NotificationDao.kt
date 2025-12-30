package com.carlosv.dolaraldia.utils.roomDB

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete // <--- NO OLVIDES IMPORTAR ESTO
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): LiveData<List<NotificationEntity>>

    // --- NUEVO: FUNCIÓN PARA BORRAR ---
    @Delete
    suspend fun delete(notification: NotificationEntity)

    // NUEVO: Borrar toda la tabla
    @Query("DELETE FROM notifications")
    suspend fun deleteAll()
}