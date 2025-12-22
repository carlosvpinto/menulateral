package com.carlosv.dolaraldia.utils

import android.util.Log

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Un objeto de utilidad para manejar la vibración del dispositivo de forma centralizada.
 */
object VibrationHelper {

    /**
     * Realiza una vibración de ÉXITO: un doble pulso corto y rápido.
     * Ideal para confirmar que una operación se ha completado correctamente.
     *
     * @param context El Context necesario para acceder al servicio de vibración.
     */
    fun vibrateOnSuccess(context: Context) {
        val vibrator = getVibrator(context) ?: return // Obtiene el vibrador o sale si no existe

        vibrator.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Patrón: [pausa_inicial, vibración_1, pausa_1, vibración_2]
                val timings = longArrayOf(0, 100, 50, 100) // Vibra 100ms, pausa 50ms, vibra 100ms
                val effect = VibrationEffect.createWaveform(timings, -1) // -1 significa no repetir
                it.vibrate(effect)
            } else {
                // Fallback para APIs antiguas: una única vibración corta
                @Suppress("DEPRECATION")
                it.vibrate(100L)
            }
        }
    }

    /**
     * Realiza una vibración de ERROR: un único pulso.
     * Ideal para notificar al usuario de un fallo en una operación.
     *
     * @param context El Context necesario para acceder al servicio de vibración.
     */
    fun vibrateOnError(context: Context) {
        val vibrator = getVibrator(context) ?: return // Obtiene el vibrador o sale si no existe

        vibrator.let {
            val duration = 200L // Duración más larga para sentirla como una alerta

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
                it.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(duration)
            }
        }
    }


    fun vibrateShake(context: Context) {
        val vibrator = getVibrator(context) ?: return

        vibrator.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Patrón: [espera, vibra, espera, vibra, espera, vibra]
                // 15ms es muy corto (sentimiento "crujiente" o mecánico)
                val timings = longArrayOf(0, 25, 40, 25, 40, 25)

                // -1 significa que no se repite (lo hace una sola vez)
                val effect = VibrationEffect.createWaveform(timings, -1)
                it.vibrate(effect)
            } else {
                // Fallback para celulares viejos: una vibración cortita
                @Suppress("DEPRECATION")
                it.vibrate(100L)
            }
        }
    }

    /**
     * Función privada para obtener el servicio de Vibrator de forma compatible
     * y comprobar si el dispositivo realmente puede vibrar.
     */
    private fun getVibrator(context: Context): Vibrator? {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        // Comprueba si el dispositivo tiene un vibrador antes de devolverlo
        if (vibrator == null || !vibrator.hasVibrator()) {
            Log.e("VibrationHelper", "El dispositivo no tiene vibrador o no se pudo obtener el servicio.")
            return null
        }
        return vibrator
    }

}