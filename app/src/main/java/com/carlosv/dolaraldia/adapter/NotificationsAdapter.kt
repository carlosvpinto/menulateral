package com.carlosv.dolaraldia.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.carlosv.dolaraldia.utils.roomDB.NotificationEntity
import com.carlosv.menulateral.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.*

// CAMBIO: Añadimos dos parámetros al constructor (onDeleteClick y onItemClick)
class NotificationsAdapter(
    private val notifications: List<NotificationEntity>,
    private val onDeleteClick: (NotificationEntity) -> Unit, // Callback para borrar
    private val onItemClick: (NotificationEntity) -> Unit    // Callback para ver detalle
) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]

        holder.binding.notificationTitle.text = notification.title
        holder.binding.notificationBody.text = notification.body
        holder.binding.notificationTimestamp.text = formatTimestamp(notification.timestamp)

        // 1. CLIC EN EL ÍCONO DE BORRAR
        holder.binding.btnDelete.setOnClickListener {
            onDeleteClick(notification)
        }

        // 2. CLIC EN TODA LA FILA (Para ver detalle)
        holder.itemView.setOnClickListener {
            onItemClick(notification)
        }
    }

    override fun getItemCount() = notifications.size

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy 'a las' hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}