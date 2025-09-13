package com.carlosv.dolaraldia.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.os.Build
import androidx.core.app.NotificationCompat
import com.carlosv.dolaraldia.MainActivity
import com.carlosv.dolaraldia.MyApplication
import com.carlosv.dolaraldia.utils.roomDB.NotificationEntity
import com.carlosv.menulateral.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random




class MyFirebaseMessagingClient: FirebaseMessagingService() {

    private val NOTIFICATION_CODE = 100
    private val random = Random

    // Es "lazy", por lo que solo se inicializará la primera vez que se use.
    private val repository by lazy { (application as MyApplication).repository }


    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Verificamos si la notificación viene en el payload "notification"
        message.notification?.let { notification ->
            val title = notification.title ?: "Dólar al Día"
            val body = notification.body ?: "Nueva información disponible."

            // 1. Mostrar la notificación en la barra de estado (tu lógica existente)
            sendNotification(notification)

            // 2. Crear la entidad para la base de datos
            val notificationEntity = NotificationEntity(title = title, body = body)

            // 3. Guardar la notificación en la base de datos usando una Coroutine
            //    Usamos CoroutineScope(Dispatchers.IO) para realizar la operación
            //    en un hilo de fondo, ideal para operaciones de base de datos.
            CoroutineScope(Dispatchers.IO).launch {
                repository.insert(notificationEntity)
            }
        }
    }

    private fun sendNotification(message: RemoteMessage.Notification) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        val channelId = this.getString(R.string.default_notification_channel_id)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSmallIcon(R.drawable.logoredondo) // CAMBIO: Usar un ícono adecuado para notificaciones
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        manager.notify(Random.nextInt(), notificationBuilder.build())
    }

    companion object {
        const val CHANNEL_NAME = "FCM notification channel"
    }

}