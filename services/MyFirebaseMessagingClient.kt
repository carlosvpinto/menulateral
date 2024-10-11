package com.carlosv.dolaraldia.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.carlosv.dolaraldia.MainActivity
import com.carlosv.menulateral.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random




class MyFirebaseMessagingClient: FirebaseMessagingService() {

    private val NOTIFICATION_CODE = 100
    private val random = Random


    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val title = data.values
        val body = data["body"]
        println()
        val idBooking = data["idBooking"]
      Log.d("NOTIFICACION", "title: $title body: $body  message: $message  message.data: ${message.data}" )

//        if (!title.isNullOrBlank() && !body.isNullOrBlank()) {
//            if (idBooking != null) {
//                showNotificationActions(title, body, idBooking)
//            }
//            else {
//                showNotification(title, body)
//            }
        }
    private fun sendNotification(message: RemoteMessage.Notification) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, FLAG_IMMUTABLE
        )
        val channelId = this.getString(R.string.default_notification_channel_id)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSmallIcon(R.drawable.ic_buscar_24)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        manager.notify(random.nextInt(), notificationBuilder.build())
    }
    companion object {
        const val CHANNEL_NAME = "FCM notification channel"
    }


    //   }

//    private fun showNotification(title: String, body: String) {
//        val helper = NotificationHelper(baseContext)
//        val builder = helper.getNotification(title, body)
//        helper.getManager().notify(1, builder.build())
//    }

//    private fun showNotificationActions(title: String, body: String, idBooking: String) {
//        val helper = NotificationHelper(baseContext)
//
//        // ACEPTAR VIAJE
//        val acceptIntent = Intent(this, AcceptReceiver::class.java)
//        acceptIntent.putExtra("idBooking", idBooking)
//        var acceptPendingIntent: PendingIntent? = null
//
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
//            acceptPendingIntent = PendingIntent.getBroadcast(this, NOTIFICATION_CODE, acceptIntent, PendingIntent.FLAG_MUTABLE)
//        }
//        else {
//            acceptPendingIntent = PendingIntent.getBroadcast(this, NOTIFICATION_CODE, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT)
//        }
//
//        val actionAccept = NotificationCompat.Action.Builder(
//            R.mipmap.ic_launcher,
//            "Aceptar",
//            acceptPendingIntent
//        ).build()
//
//        // CANCELAR VIAJE
//        val cancelIntent = Intent(this, CancelReceiver::class.java)
//        cancelIntent.putExtra("idBooking", idBooking)
//        var cancelPendingIntent: PendingIntent? = null
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
//            cancelPendingIntent = PendingIntent.getBroadcast(this, NOTIFICATION_CODE, cancelIntent, PendingIntent.FLAG_MUTABLE)
//        }
//        else {
//            cancelPendingIntent = PendingIntent.getBroadcast(this, NOTIFICATION_CODE, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT)
//        }
//
//        val actionCancel = NotificationCompat.Action.Builder(
//            R.mipmap.ic_launcher,
//            "Cancelar",
//            cancelPendingIntent
//        ).build()
//
//
//        val builder = helper.getNotificationActions(title, body, actionAccept, actionCancel)
//        helper.getManager().notify(2, builder.build())
//    }
//
}