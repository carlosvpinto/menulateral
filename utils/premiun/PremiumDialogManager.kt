package com.carlosv.dolaraldia.utils.premiun

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class PremiumDialogManager(context: Context) {

    companion object {
        // Nombre del archivo de preferencias y de la clave para el contador.
        private const val PREFS_NAME = "ad_dialog_prefs"
        private const val AD_VIEW_COUNT_KEY = "ad_view_count"
        val  TAG = "PremiumDialogManager"

        // ¡Puedes ajustar este número! El diálogo se mostrará cada 3 anuncios.
        private const val AD_SHOW_THRESHOLD = 2
    }

    // Obtenemos una instancia de SharedPreferences.
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Esta es la función principal. Incrementa el contador y decide si se debe mostrar el diálogo.
     * @return `true` si el diálogo debe mostrarse, `false` en caso contrario.
     */
    fun shouldShowPremiumDialog(): Boolean {
        // 1. Obtenemos el conteo actual. Si no existe, empezamos en 0.
        var currentCount = prefs.getInt(AD_VIEW_COUNT_KEY, 0)

        // 2. Incrementamos el contador.
        currentCount++
        Log.d(TAG, "shouldShowPremiumDialog:currentCount $currentCount ")
        // 3. Verificamos si hemos alcanzado el umbral.
        if (currentCount >= AD_SHOW_THRESHOLD) {
            // Si lo alcanzamos, reiniciamos el contador a 0 y devolvemos 'true'.
            prefs.edit().putInt(AD_VIEW_COUNT_KEY, 0).apply()
            return true
        } else {
            // Si no, solo guardamos el nuevo conteo y devolvemos 'false'.
            prefs.edit().putInt(AD_VIEW_COUNT_KEY, currentCount).apply()
            return false
        }
    }
}