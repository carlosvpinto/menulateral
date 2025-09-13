package com.carlosv.dolaraldia.utils.roomDB

import androidx.lifecycle.LiveData

class NotificationsRepository(private val notificationDao: NotificationDao) {
    val allNotifications: LiveData<List<NotificationEntity>> = notificationDao.getAllNotifications()

    suspend fun insert(notification: NotificationEntity) {
        notificationDao.insert(notification)
    }
}