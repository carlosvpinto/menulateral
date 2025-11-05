package com.carlosv.dolaraldia.utils

import android.app.Activity
import android.content.Context
import android.util.Log // <-- 1. IMPORTAMOS LA CLASE Log
import com.google.android.play.core.review.ReviewManagerFactory

object ReviewManager {

    // 2. DEFINIMOS UN TAG PARA NUESTROS MENSAJES DE LOG
    private const val TAG = "ReviewManager"

    private const val MINIMUM_SESSIONS = 4
    private const val MAX_REVIEW_REQUESTS = 2

    private const val SESSION_COUNT_KEY = "reviewSessionCount"
    private const val REVIEW_REQUEST_COUNT_KEY = "reviewRequestCount"
    private const val PREFS_NAME = "ReviewPrefs"

    /**
     * Llama a esta función cada vez que la app se inicia.
     * Incrementa el contador de sesiones y decide si debe solicitar una reseña.
     * @param context El contexto de la Actividad actual.
     */
    fun trackSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val requestCount = prefs.getInt(REVIEW_REQUEST_COUNT_KEY, 0)

        if (requestCount >= MAX_REVIEW_REQUESTS) {
            // 3. REEMPLAZAMOS println CON Log.d
            Log.d(TAG, "Se ha alcanzado el número máximo de solicitudes de reseña ($MAX_REVIEW_REQUESTS). No se volverá a solicitar.")
            return
        }

        var sessionCount = prefs.getInt(SESSION_COUNT_KEY, 0)
        sessionCount++
        prefs.edit().putInt(SESSION_COUNT_KEY, sessionCount).apply()

        // 3. REEMPLAZAMOS println CON Log.d
        Log.d(TAG, "Sesión número $sessionCount. Solicitudes realizadas: $requestCount de $MAX_REVIEW_REQUESTS.")

        if (sessionCount >= MINIMUM_SESSIONS) {
            if (context is Activity) {
                requestReview(context)
            }
        }
    }

    private fun requestReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()

        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)

                flow.addOnCompleteListener { _ ->
                    // 3. REEMPLAZAMOS println CON Log.d
                    Log.d(TAG, "Solicitud de reseña enviada al sistema.")
                    updateCountersAfterRequest(activity)
                }
            } else {
                // 3. REEMPLAZAMOS println CON Log.d
                Log.d(TAG, "No se pudo obtener el objeto reviewInfo.")
            }
        }
    }

    private fun updateCountersAfterRequest(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var requestCount = prefs.getInt(REVIEW_REQUEST_COUNT_KEY, 0)
        requestCount++
        prefs.edit().putInt(REVIEW_REQUEST_COUNT_KEY, requestCount).apply()
        prefs.edit().putInt(SESSION_COUNT_KEY, 0).apply()

        // 3. REEMPLAZAMOS println CON Log.d
        Log.d(TAG, "Contador de sesiones reseteado. Próxima solicitud en $MINIMUM_SESSIONS sesiones.")
    }
}