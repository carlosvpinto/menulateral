package com.carlosv.dolaraldia.utils.roomDB

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis() // Guarda la fecha y hora como un número largo
)