package com.carlosv.dolaraldia.utils.roomDB


import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope // Necesario para lanzar corrutinas
import kotlinx.coroutines.launch

class NotificationsViewModel(private val repository: NotificationsRepository) : ViewModel() {

    // Lista observable para el RecyclerView
    val notifications: LiveData<List<NotificationEntity>> = repository.allNotifications

    // --- FUNCIÓN NUEVA: BORRAR UNO ---
    // Esta es la que llama el Fragmento cuando tocas el ícono de basura
    fun delete(notification: NotificationEntity) = viewModelScope.launch {
        repository.delete(notification)
    }

    fun deleteAll() = viewModelScope.launch {
        repository.deleteAll()
    }


}