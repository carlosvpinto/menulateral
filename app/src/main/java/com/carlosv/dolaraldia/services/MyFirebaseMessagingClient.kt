package com.carlosv.dolaraldia.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.carlosv.dolaraldia.MainActivity
import com.carlosv.dolaraldia.MyApplication
import com.carlosv.dolaraldia.utils.roomDB.NotificationEntity
import com.carlosv.menulateral.R
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

class MyFirebaseMessagingClient : FirebaseMessagingService() {

    private val TAG = "NotificacionesFCM"

    // Acceso seguro al repositorio
    private val repository by lazy { (application as MyApplication).repository }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "¡MENSAJE RECIBIDO! Data Payload: ${message.data}")

        // 1. EXTRACCIÓN DE DATOS
        // Prioridad: Datos Personalizados (Data) > Notificación Estándar > Valores por defecto
        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: "Dolar al Día"
        val body = data["body"] ?: message.notification?.body ?: "Nueva actualización disponible"

        // Obtenemos el destino para la navegación
        val fragmentDestino = data["ir_a"]

        // 2. GUARDAR EN BASE DE DATOS (ROOM)
        // Solo guardamos si hay un cuerpo de mensaje válido
        if (body.isNotEmpty()) {
            guardarEnBaseDeDatos(title, body)
        }

        // 3. MOSTRAR NOTIFICACIÓN VISUAL
        sendNotification(title, body, fragmentDestino)
    }

    private fun guardarEnBaseDeDatos(title: String, body: String) {
        // Usamos IO para operaciones de base de datos
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notificationEntity = NotificationEntity(
                    title = title,
                    body = body,
                    timestamp = System.currentTimeMillis()
                )
                repository.insert(notificationEntity)
                Log.d(TAG, "✅ Notificación guardada en Room exitosamente.")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al guardar en base de datos: ${e.message}")
            }
        }
    }

    private fun sendNotification(title: String, body: String, fragmentDestino: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

            // Pasamos el destino al MainActivity
            if (!fragmentDestino.isNullOrEmpty()) {
                putExtra("FRAGMENT_DESTINO", fragmentDestino)
                Log.d(TAG, "Destino agendado en Intent: $fragmentDestino")
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            Random.nextInt(), // ID único para que no se sobrescriban los extras
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val channelId = getString(R.string.default_notification_channel_id)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logoredondo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal para Android 8+ (Oreo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Notificaciones Dolar al Dia"
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(Random.nextInt(), notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo token generado: $token")
        FirebaseMessaging.getInstance().subscribeToTopic("general")
    }
}