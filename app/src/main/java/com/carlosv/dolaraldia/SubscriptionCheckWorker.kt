package com.carlosv.dolaraldia

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SubscriptionCheckWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private val TAG = "PAYPALPRICE"

    override fun doWork(): Result {
        val sharedPreferences: SharedPreferences = applicationContext.getSharedPreferences("UserSubscription", Context.MODE_PRIVATE)

        // Verificar si el usuario tiene una suscripción de por vida
        val hasLifetimeSubscription = sharedPreferences.getBoolean("lifetime_subscription", false)

        if (hasLifetimeSubscription) {
            // No hacer nada si tiene una suscripción de por vida
            Log.d(TAG, "doWork: Tiene Suscripcion de por Vida ")
            return Result.success()
        }

        // Obtener la fecha actual
        val currentDate = Calendar.getInstance().timeInMillis

        // Obtener la fecha de expiración de la suscripción
        val subscriptionExpiration = sharedPreferences.getLong("subscription_expiration", 0)
        Log.d(TAG, "doWork: Fecha de Expedicion Covertida: ${convertMillisToDate(subscriptionExpiration)} ")
        Log.d(TAG, "doWork: Fecha del Actual Covertida: ${convertMillisToDate(currentDate)} ")
        Log.d(TAG, "doWork: Comparar Fechas>= currentDate $currentDate subscriptionExpiration $subscriptionExpiration")
        if (currentDate >= subscriptionExpiration) {
            // Enviar notificación al usuario informando que la suscripción ha expirado

            sendSubscriptionExpiredNotification(currentDate)
        }

        return Result.success()
    }

    private fun sendSubscriptionExpiredNotification(timeInMillis:Long) {
        // Código para enviar una notificación
        // Puedes usar NotificationManager para enviar la notificación al usuario
        Log.d(TAG, "doWork: Envia notificacion al usuario de fecha vencida!!!!!!")
    }
    fun convertMillisToDate(timeInMillis: Long): String {
        // Crear un objeto Date a partir de timeInMillis
        val date = Date(timeInMillis)

        // Formato de fecha: dd/MM/yyyy
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Retornar la fecha en el formato especificado
        return dateFormat.format(date)
    }
}
