package com.carlosv.dolaraldia

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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
    fun setUserAsPremium(plan: String?) {
        val planName = plan ?: "Desconocido"
        val editor = preferences.edit()

        val calendar = Calendar.getInstance()
        val startDate = calendar.timeInMillis // Fecha de inicio: ahora mismo

        // --- ¡AQUÍ ESTÁ LA LÓGICA DE CÁLCULO Y LOGGING! ---

        // 1. Creamos un formateador para mostrar las fechas de forma legible.
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

        // 2. Imprimimos la fecha de inicio.
        Log.d("PremiumStatus", "Plan seleccionado: $planName")
        Log.d("PremiumStatus", "Fecha de Inicio (ms): $startDate")
        Log.d("PremiumStatus", "Fecha de Inicio (formateada): ${dateFormat.format(Date(startDate))}")

        // 3. Calculamos la fecha de vencimiento basándonos en el nombre del plan.
        when (planName) {
            "Mensual" -> {
                calendar.add(Calendar.MONTH, 1) // Añade 1 mes
            }
            "Anual" -> {
                calendar.add(Calendar.YEAR, 1) // Añade 1 año
            }
            "Vitalicio" -> {
                // Usamos -1L como convención para "nunca expira".
                calendar.timeInMillis = -1L
            }
        }
        val expirationDate = calendar.timeInMillis

        // 4. Imprimimos la fecha de vencimiento.
        Log.d("PremiumStatus", "Fecha de Vencimiento (ms): $expirationDate")
        if (expirationDate == -1L) {
            Log.d("PremiumStatus", "Fecha de Vencimiento (formateada): NUNCA (Vitalicio)")
        } else {
            Log.d("PremiumStatus", "Fecha de Vencimiento (formateada): ${dateFormat.format(Date(expirationDate))}")
        }

        // --- FIN DE LA LÓGICA DE LOGGING ---

        // Guardamos todos los datos
        editor.putBoolean(IS_USER_PREMIUM, true)
        editor.putString(PREMIUM_PLAN_NAME, planName)
        editor.putLong(PREMIUM_START_DATE, startDate)
        editor.putLong(PREMIUM_EXPIRATION_DATE, expirationDate)

        editor.apply()
    }


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

   /* @param hours La cantidad de horas de premium que se otorgarán.
    */
    fun grantTemporaryPremium(hours: Int) {
        val calendar = Calendar.getInstance()
        // Establecemos la nueva fecha de vencimiento sumando las horas.
        calendar.add(Calendar.HOUR, hours)

        val newExpirationDate = calendar.timeInMillis

        // Guardamos la nueva fecha de vencimiento.
        // También nos aseguramos de que la bandera is_premium esté en true.
        preferences.edit().apply {
            putBoolean(IS_USER_PREMIUM, true) // Activa el estado premium
            putLong(PREMIUM_EXPIRATION_DATE, newExpirationDate)
            apply()
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        Log.d("AppPreferences", "Premium temporal otorgado. Nueva fecha de vencimiento: ${dateFormat.format(Date(newExpirationDate))}")
    }

    // --- ¡NUEVAS CLAVES Y LÓGICA PARA EL CACHE DE TIEMPO! ---
    private const val LAST_API_REFRESH_TIMESTAMP = "last_api_refresh_timestamp"
    // Tiempo mínimo en milisegundos entre actualizaciones (15 minutos)
    private const val REFRESH_INTERVAL_MS = 15 * 60 * 1000

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


}