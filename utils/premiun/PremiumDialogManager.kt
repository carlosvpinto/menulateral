package com.carlosv.dolaraldia.utils.premiun

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class PremiumDialogManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "ad_dialog_prefs"
        private const val AD_VIEW_COUNT_KEY = "ad_view_count"
        val  TAG = "PremiumDialogManager"

        // El diálogo se mostrará la primera vez (conteo 1) y en el conteo máximo (20).
        // Se reiniciará después de llegar a este número.
        private const val AD_SHOW_THRESHOLD = 20
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

        // 3. Verificamos la lógica: Mostrar si es 1 O si alcanza el umbral (20).
        val shouldShow = (currentCount == 1) || (currentCount >= AD_SHOW_THRESHOLD)

        if (shouldShow) {
            // Si el conteo es 20 (el umbral), lo reiniciamos a 0 para empezar de nuevo.
            if (currentCount == AD_SHOW_THRESHOLD) {
                prefs.edit().putInt(AD_VIEW_COUNT_KEY, 0).apply()
            } else {
                // Si el conteo fue 1, solo guardamos el nuevo valor (1).
                prefs.edit().putInt(AD_VIEW_COUNT_KEY, currentCount).apply()
            }
            return true
        } else {
            // Si no se muestra, solo guardamos el nuevo conteo.
            prefs.edit().putInt(AD_VIEW_COUNT_KEY, currentCount).apply()
            return false
        }
    }

    /**
     * [SOLO PARA DEPURACIÓN] Borra el contador de anuncios vistos del archivo de preferencias.
     * Esto forzará a que el diálogo de oferta premium aparezca la próxima vez
     * que se cierre un anuncio, ya que el contador volverá a ser 1.
     */
    fun clearAdCountForDevelopment() {
        prefs.edit().remove(AD_VIEW_COUNT_KEY).apply()
        Log.d(TAG, "[DEBUG] Contador de anuncios borrado.")
    }
}