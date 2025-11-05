package com.carlosv.dolaraldia.utils.premiun

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.carlosv.dolaraldia.utils.Constants.MOSTRAR_DESPUES_NRO_VECES
import com.carlosv.dolaraldia.utils.Constants.MOSTRAR_PRIMER_DIALOGO

class PremiumDialogManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "ad_dialog_prefs"
        private const val AD_VIEW_COUNT_KEY = "ad_view_count"


        val  TAG = "PremiumDialogManager"


    }

    // Obtenemos una instancia de SharedPreferences.
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Esta es la función principal. Incrementa el contador y decide si se debe mostrar el diálogo.
     * @return `true` si el diálogo debe mostrarse, `false` en caso contrario.
     */
    fun shouldShowPremiumDialog(): Boolean {
        // 1. Obtenemos el conteo actual de publicidades vistas.
        var currentCount = prefs.getInt(AD_VIEW_COUNT_KEY, 0)


        currentCount++
        Log.d(TAG, "Conteo de anuncios para diálogo Premium: $currentCount")

        // 3. Guardamos el nuevo conteo inmediatamente.
        // Esto simplifica la lógica, ya que no necesitamos guardarlo en cada 'if/else'.
        prefs.edit().putInt(AD_VIEW_COUNT_KEY, currentCount).apply()

        // 4. Verificamos la lógica principal:
        // ¿El conteo es EXACTAMENTE 5? (Para la primera vez)
        // O
        // ¿El conteo ha alcanzado o superado el umbral de 20? (Para las veces siguientes)
        if (currentCount == MOSTRAR_PRIMER_DIALOGO || currentCount >= MOSTRAR_DESPUES_NRO_VECES) {

            // Si se va a mostrar el diálogo, reiniciamos el contador a 0.
            // Esto funciona para ambos casos (cuando es 5 y cuando es >= 20).
            Log.d(TAG, "¡Umbral alcanzado! Mostrando diálogo Premium y reseteando contador.")
            prefs.edit().putInt(AD_VIEW_COUNT_KEY, 0).apply()

            // Devolvemos 'true' para indicar que se debe mostrar el diálogo.
            return true
        }

        // 5. Si no se cumplió ninguna de las condiciones, no mostramos el diálogo.
        return false
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