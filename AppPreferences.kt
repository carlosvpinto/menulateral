package com.carlosv.dolaraldia

import android.content.Context
import android.content.SharedPreferences

object AppPreferences {
    private const val NAME = "DolarAlDiaPrefs"
    private const val MODE = Context.MODE_PRIVATE
    private lateinit var preferences: SharedPreferences

    // Clave para guardar si el usuario vio el diálogo de Pago Móvil
    private val VIO_DIALOGO_PAGO_MOVIL = "VIO_DIALOGO_PAGO_MOVIL"

    fun init(context: Context) {
        preferences = context.getSharedPreferences(NAME, MODE)
    }

    /**
     * Marca que el usuario ya ha visto el diálogo de la nueva función.
     */
    fun marcarDialogoPagoMovilComoVisto() {
        preferences.edit().putBoolean(VIO_DIALOGO_PAGO_MOVIL, true).apply()
    }

    /**
     * Verifica si el usuario ya vio el diálogo de la nueva función.
     * @return `true` si ya lo vio, `false` si no.
     */
    fun haVistoDialogoPagoMovil(): Boolean {
        return preferences.getBoolean(VIO_DIALOGO_PAGO_MOVIL, false)
    }

    /** [SOLO PARA DEPURACIÓN] Borra el estado de "visto" del diálogo de Pago Móvil,
    * forzando a que aparezca de nuevo en el próximo inicio.
    */
    fun borrarEstadoDialogoPagoMovil() {
        preferences.edit().remove(VIO_DIALOGO_PAGO_MOVIL).apply()
    }
}