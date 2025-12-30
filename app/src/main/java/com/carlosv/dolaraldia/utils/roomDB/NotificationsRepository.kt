package com.carlosv.dolaraldia.utils.roomDB

import androidx.lifecycle.LiveData

class NotificationsRepository(private val notificationDao: NotificationDao) {

    // Lista de notificaciones (Ya lo tenías)
    val allNotifications: LiveData<List<NotificationEntity>> = notificationDao.getAllNotifications()

    // Insertar (Ya lo tenías)
    suspend fun insert(notification: NotificationEntity) {
        notificationDao.insert(notification)
    }

    // --- AGREGAR ESTA FUNCIÓN NUEVA ---
    // Esta es la que conecta el ViewModel con la Base de Datos para borrar
    suspend fun delete(notification: NotificationEntity) {
        notificationDao.delete(notification)
    }

    suspend fun deleteAll() {
        notificationDao.deleteAll()
    }
}