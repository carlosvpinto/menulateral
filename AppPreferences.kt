package com.carlosv.dolaraldia

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.carlosv.dolaraldia.model.apiAlcambioEuro.ApiOficialTipoCambio
import com.carlosv.dolaraldia.utils.Constants.CONTEO_INICIOS_TRAS_DENEGAR_PERMISO
import com.carlosv.dolaraldia.utils.Constants.UMBRAL_RECORDATORIO_PERMISO
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AppPreferences {
    private const val NAME = "DolarAlDiaPrefs"
    private const val MODE = Context.MODE_PRIVATE

    private const val PREFS_NAME = "DolarAlDiaPrefs"
    private const val VIO_NOVEDAD_PLATAFORMAS = "vioNovedadPlataformas"
    private lateinit var preferences: SharedPreferences

    // Clave para guardar si el usuario vio el diálogo de Pago Móvil
    private val VIO_DIALOGO_PAGO_MOVIL = "VIO_DIALOGO_PAGO_MOVIL"

    // --- ¡NUEVAS CLAVES PARA EL ESTADO PREMIUM! ---
    private const val IS_USER_PREMIUM = "is_user_premium"
    private const val PREMIUM_PLAN_NAME = "premium_plan_name"

    // --- ¡NUEVAS CLAVES PARA LAS FECHAS! ---
    // Guardaremos las fechas como 'Long' (milisegundos desde 1970)
    private const val PREMIUM_START_DATE = "premium_start_date"
    private const val PREMIUM_EXPIRATION_DATE = "premium_expiration_date"

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

    fun marcarDialogoPlatformasComoVisto() {
        preferences.edit().putBoolean(VIO_NOVEDAD_PLATAFORMAS, true).apply()
    }

    /**
     * Verifica si el usuario ya vio el diálogo de la nueva función.
     * @return `true` si ya lo vio, `false` si no.
     */
    fun haVistoDialogoPlatformas(): Boolean {
        return preferences.getBoolean(VIO_NOVEDAD_PLATAFORMAS, false)
    }

    /** [SOLO PARA DEPURACIÓN] Borra el estado de "visto" del diálogo de Pago Móvil,
     * forzando a que aparezca de nuevo en el próximo inicio.
     */
    fun borrarEstadoDialogoPlatformas() {
        preferences.edit().remove(VIO_NOVEDAD_PLATAFORMAS).apply()
    }



    /**
     * Guarda el estado premium, el plan y calcula las fechas de inicio y vencimiento.
     * @param plan El nombre del plan ("Mensual", "Anual", "Vitalicio").
     */



    /**
     * Verifica si el usuario tiene una suscripción premium ACTIVA.
     * Comprueba si es premium y si la fecha de vencimiento no ha pasado.
     * @return `true` si el usuario es premium y la suscripción es válida, `false` en caso contrario.
     */
    fun isUserPremiumActive(): Boolean {
        val isPremium = preferences.getBoolean(IS_USER_PREMIUM, false)
        if (!isPremium) {
            return false // Si nunca fue premium, no está activo.
        }

        val expirationDate = preferences.getLong(PREMIUM_EXPIRATION_DATE, 0L)

        // Si la fecha de vencimiento es -1, es vitalicio y siempre está activo.
        if (expirationDate == -1L) {
            return true
        }

        // Comprueba si la fecha de vencimiento es en el futuro.
        return expirationDate > System.currentTimeMillis()
    }

    // --- FUNCIONES DE LECTURA (GETTERS) OPCIONALES PERO RECOMENDADAS ---

    fun getPremiumPlan(): String? {
        if (!isUserPremiumActive()) return null
        return preferences.getString(PREMIUM_PLAN_NAME, "Desconocido")
    }

    fun getPremiumExpirationDate(): Long {
        return preferences.getLong(PREMIUM_EXPIRATION_DATE, 0L)
    }

    /**
     * [SOLO PARA DEPURACIÓN] Borra el estado premium para volver a mostrar anuncios.
     */
    fun clearPremiumStatus() {
        preferences.edit().apply {
            remove(IS_USER_PREMIUM)
            remove(PREMIUM_PLAN_NAME)
            remove(PREMIUM_START_DATE)
            remove(PREMIUM_EXPIRATION_DATE)
            apply()
        }
    }


    //GUARDA EL TIPO DE PLAN Y LA FECHA DE INICIO Y VENCIMIENTO********************
    fun setUserAsPremium(plan: String?, durationInHours: Int? = null) {
        val planName = plan ?: "Desconocido"
        val editor = preferences.edit()

        val calendar = Calendar.getInstance()
        val startDate = calendar.timeInMillis
        var expirationDate: Long

        // --- LÓGICA DE CÁLCULO DE VENCIMIENTO ---

        if (durationInHours != null && durationInHours > 0) {
            // Caso 1: Es un plan temporal definido en horas.
            calendar.add(Calendar.HOUR, durationInHours)
            expirationDate = calendar.timeInMillis
        } else {
            // Caso 2: Es un plan fijo basado en el nombre.
            when (planName) {
                "Mensual" -> {
                    calendar.add(Calendar.MONTH, 1)
                    expirationDate = calendar.timeInMillis
                }
                "Anual" -> {
                    calendar.add(Calendar.YEAR, 1)
                    expirationDate = calendar.timeInMillis
                }
                "Vitalicio" -> {
                    expirationDate = -1L // Convención para "nunca expira"
                }
                else -> {
                    // Si el plan no es reconocido y no se dieron horas, no hacemos nada.
                    Log.w("PremiumStatus", "Plan '$planName' no reconocido y sin duración especificada. No se aplicó el estado premium.")
                    return
                }
            }
        }

        // --- LÓGICA DE LOGGING ---
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        Log.d("PremiumStatus", "==================== ESTADO PREMIUM ACTUALIZADO ====================")
        Log.d("PremiumStatus", "Plan Aplicado: $planName")
        Log.d("PremiumStatus", "Fecha de Inicio: ${dateFormat.format(Date(startDate))}")

        val expirationFormatted = if (expirationDate == -1L) {
            "NUNCA (Vitalicio)"
        } else {
            dateFormat.format(Date(expirationDate))
        }
        Log.d("PremiumStatus", "Fecha de Vencimiento: $expirationFormatted")
        Log.d("PremiumStatus", "==================================================================")

        // --- GUARDADO DE DATOS ---
        editor.putBoolean(IS_USER_PREMIUM, true)
        editor.putString(PREMIUM_PLAN_NAME, planName)
        editor.putLong(PREMIUM_START_DATE, startDate)
        editor.putLong(PREMIUM_EXPIRATION_DATE, expirationDate)
        editor.apply()
    }


    // --- ¡NUEVAS CLAVES Y LÓGICA PARA EL CACHE DE TIEMPO! ---
    private const val LAST_API_REFRESH_TIMESTAMP = "last_api_refresh_timestamp"
    // Tiempo mínimo en milisegundos entre actualizaciones (15 minutos)
    private const val REFRESH_INTERVAL_MS =  30 * 1000

    /**
     * Guarda la marca de tiempo actual como la última vez que la API se actualizó.
     */
    fun updateLastRefreshTimestamp() {
        preferences.edit().putLong(LAST_API_REFRESH_TIMESTAMP, System.currentTimeMillis()).apply()
    }

    /**
     * Comprueba si ha pasado el tiempo suficiente para permitir un nuevo refresco.
     * @return `true` si se debe refrescar, `false` si no.
     */
    fun shouldRefreshApi(): Boolean {
        val lastRefresh = preferences.getLong(LAST_API_REFRESH_TIMESTAMP, 0L)
        val timeSinceLastRefresh = System.currentTimeMillis() - lastRefresh

        // Si es la primera vez (lastRefresh es 0) o si ha pasado el intervalo,
        // entonces sí debemos refrescar.
        return lastRefresh == 0L || timeSinceLastRefresh >= REFRESH_INTERVAL_MS
    }



    /**
     * Incrementa el contador de inicios de la app y devuelve el nuevo valor.
     * Este contador se usa para saber cuándo volver a mostrar el diálogo de permiso.
     * @return El número de inicios desde la última vez que se denegó el permiso.
     */
    fun incrementarYObtenerConteoInicios(): Int {
        var conteoActual = preferences.getInt(CONTEO_INICIOS_TRAS_DENEGAR_PERMISO, 0)
        conteoActual++
        preferences.edit().putInt(CONTEO_INICIOS_TRAS_DENEGAR_PERMISO, conteoActual).apply()
        return conteoActual
    }

    /**
     * Resetea el contador de inicios de la app a 0.
     * Se debe llamar cuando el permiso es concedido o cuando se muestra el recordatorio.
     */
    fun resetearConteoInicios() {
        preferences.edit().putInt(CONTEO_INICIOS_TRAS_DENEGAR_PERMISO, 0).apply()
    }

    /**
     * Comprueba si se ha alcanzado el umbral para mostrar el recordatorio de permiso.
     * @param conteoActual El número actual de inicios.
     * @return `true` si se debe mostrar el recordatorio, `false` en caso contrario.
     */
    fun debeMostrarRecordatorioPermiso(conteoActual: Int): Boolean {
        return conteoActual >= UMBRAL_RECORDATORIO_PERMISO
    }

    fun leerResponse(context: Context): ApiOficialTipoCambio? {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)

        // Obtenemos el JSON guardado. Si no existe, devolvemos null.
        val responseJson = sharedPreferences.getString("dolarBCVResponse", null)

        // Si encontramos un JSON, lo convertimos de nuevo a nuestro objeto de datos.
        return if (responseJson != null) {
            val gson = Gson()
            try {
                gson.fromJson(responseJson, ApiOficialTipoCambio::class.java)
            } catch (e: Exception) {
                // En caso de que el JSON guardado esté corrupto, devolvemos null.
                null
            }
        } else {
            null
        }
    }

}