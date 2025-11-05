package com.carlosv.dolaraldia.utils.roomDB

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel

class NotificationsViewModel(private val repository: NotificationsRepository) : ViewModel() {
    val notifications: LiveData<List<NotificationEntity>> = repository.allNotifications
}