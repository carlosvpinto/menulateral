package com.carlosv.dolaraldia.utils.roomDB

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class NotificationsViewModelFactory(private val repository: NotificationsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}