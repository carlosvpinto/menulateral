package com.carlosv.dolaraldia.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
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




class MyFirebaseMessagingClient: FirebaseMessagingService() {

    private val NOTIFICATION_CODE = 100
    private val random = Random
    private val TAG = "NotificacionesRecibidas"

    // Es "lazy", por lo que solo se inicializará la primera vez que se use.
    private val repository by lazy { (application as MyApplication).repository }


    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "¡MENSAJE RECIBIDO! Payload de datos: ${message.data}")


        val title = message.data["title"]
        val body = message.data["body"]
        val deepLink = message.data["deep_link"]


        if (title.isNullOrBlank() || body.isNullOrBlank()) {
            Log.e(TAG, "El mensaje recibido no contenía título o cuerpo en el payload 'data'.")
            return
        }

        Log.d(TAG, "Procesando notificación: Título='$title', Cuerpo='$body'")

        val notificationEntity = NotificationEntity(title = title, body = body)
        CoroutineScope(Dispatchers.IO).launch {
            repository.insert(notificationEntity)
            Log.d(TAG, "Notificación guardada en la base de datos.")
        }

        sendNotification(title, body, deepLink)
    }



    /**

     * @param deepLink Una URI opcional (ej: "dolaraldia://fragment/bancos") que se pasará
     *                 a la MainActivity para navegar a un destino específico.
     */
    private fun sendNotification(title: String, body: String, deepLink: String?) {
        // 1. Crear el Intent que se ejecutará cuando el usuario toque la notificación.
        // Siempre apunta a nuestra actividad principal (MainActivity), que actuará como
        // el punto de entrada y controlador de la navegación.
        val intent = Intent(this, MainActivity::class.java).apply {
            // Estas banderas aseguran que al abrir la app desde la notificación, se cree una
            // nueva tarea o se limpie la existente, evitando comportamientos extraños de navegación.
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            // ¡LA PARTE CLAVE PARA EL DEEP LINK!
            // Si el mensaje de Firebase contenía una URI en el campo "deep_link",
            // la añadimos como un "extra" al Intent. MainActivity leerá este extra.
            if (deepLink != null) {
                putExtra("deep_link", deepLink)
                Log.d(TAG, "Notificación creada con el deep link: $deepLink")
            }
        }

        // 2. Envolver el Intent en un PendingIntent.
        // El sistema operativo Android necesita un PendingIntent para ejecutar nuestro Intent
        // en nombre de nuestra aplicación.
        val pendingIntent = PendingIntent.getActivity(
            this,
            Random.nextInt(), // Usamos un ID de solicitud aleatorio para asegurar que cada PendingIntent sea único.
            intent,
            // FLAG_ONE_SHOT: El PendingIntent solo se puede usar una vez.
            // FLAG_IMMUTABLE: Requerido para seguridad en Android 12 (API 31) y superior.
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Definir el ID del canal de notificación.
        // Este debe coincidir con el ID del canal que creas para Android 8.0+.
        val channelId = this.getString(R.string.default_notification_channel_id)

        // 4. Construir la notificación usando NotificationCompat para máxima compatibilidad.
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logoredondo) // El ícono pequeño que aparece en la barra de estado.
            .setContentTitle(title)              // El título de la notificación.
            .setContentText(body)                // El cuerpo del mensaje.
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Prioridad alta para que aparezca como "heads-up".
            .setAutoCancel(true)                 // La notificación se elimina automáticamente cuando el usuario la toca.
            .setContentIntent(pendingIntent)       // ¡Asociamos el toque de la notificación a nuestro PendingIntent!

        // 5. Obtener el servicio de notificaciones del sistema.
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // 6. Crear el canal de notificación (solo para Android 8.0 Oreo y superior).
        // Si la app se ejecuta en una versión anterior, el sistema ignora este bloque.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                CHANNEL_NAME, // Un nombre legible para el usuario en los ajustes de la app.
                NotificationManager.IMPORTANCE_HIGH // Importancia alta para que el sonido/vibración funcione.
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 7. Mostrar la notificación.
        // Usamos un ID de notificación aleatorio para que cada nueva notificación se muestre
        // por separado, en lugar de reemplazar la anterior.
        notificationManager.notify(Random.nextInt(), notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Nuevo token generado: $token")

        // Aquí es donde deberías enviar el nuevo token a tu propio servidor si
        // necesitaras enviar notificaciones a usuarios específicos.
        // Si solo usas tópicos (como "general"), suscribirse de nuevo puede ser una buena idea.
        FirebaseMessaging.getInstance().subscribeToTopic("general")
    }

    companion object {
        const val CHANNEL_NAME = "FCM notification channel"
    }

}