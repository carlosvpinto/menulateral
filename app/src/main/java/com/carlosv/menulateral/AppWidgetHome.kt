package com.carlosv.menulateral

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import com.carlosv.dolaraldia.AppPreferences.leerResponse
import com.carlosv.dolaraldia.MainActivity
import com.carlosv.dolaraldia.model.apiAlcambioEuro.ApiOficialTipoCambio
import com.carlosv.dolaraldia.model.apiAlcambioEuro.Eur
import com.carlosv.dolaraldia.model.apiAlcambioEuro.Usd
import com.carlosv.dolaraldia.model.apiAlcambioEuro.Usdt
import com.carlosv.dolaraldia.utils.Constants
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import okhttp3.Request
class AppWidgetHome : AppWidgetProvider() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, widgetId, scope)
        }
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        scope.cancel()
    }

    companion object {
        private const val TAG = "AppWidgetHome"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            scope: CoroutineScope
        ) {
            Log.d(TAG, "Iniciando actualización para widget ID: $appWidgetId")
            val views = RemoteViews(context.packageName, R.layout.app_widget_home)

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            scope.launch {
                try {
                    // --- PASO 1: Intentar obtener datos frescos de la API ---
                    val apiResponse = fetchFromApi()

                    if (apiResponse != null) {
                        Log.d(TAG, "Éxito en la API. Guardando en caché y actualizando widget.")
                        // Guardamos la respuesta completa en caché usando TU función.
                        guardarResponse(context, apiResponse)
                        // Actualizamos la UI del widget con los datos de la API.
                        updateWidgetViews(appWidgetManager, appWidgetId, views, apiResponse.monitors.usd, apiResponse.monitors.eur, apiResponse.monitors.usdt)
                    } else {
                        throw Exception("La llamada a la API falló o devolvió datos nulos.")
                    }

                } catch (e: Exception) {
                    // --- PASO 2: Si la API falló, intentar cargar desde caché ---
                    Log.e(TAG, "Error al contactar la API: ${e.message}. Intentando cargar desde caché.")

                    // Leemos la última respuesta guardada usando TU función.
                    val cachedResponse = leerResponse(context)

                    if (cachedResponse != null) {
                        Log.d(TAG, "Éxito al cargar desde caché. Actualizando widget con datos antiguos.")
                        // Usamos los datos del caché para actualizar el widget.
                        updateWidgetViews(appWidgetManager, appWidgetId, views, cachedResponse.monitors.usd, cachedResponse.monitors.eur, cachedResponse.monitors.usdt, isFromCache = true)
                    } else {
                        // --- PASO 3: Si la API y el caché fallan ---
                        Log.e(TAG, "Fallo de API y sin datos en caché. Mostrando estado de error.")
                        showErrorInWidget(appWidgetManager, appWidgetId, views)
                    }
                }
            }
        }
        //Guarda en SharePreference los Respose de cada solicitud al API
        private fun guardarResponse(context: Context, responseBCV: ApiOficialTipoCambio) {
            val gson = Gson()
            val responseJson = gson.toJson(responseBCV)

            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("dolarBCVResponse", responseJson)
            editor.apply()
        }

        /**
         * Llama a la API y devuelve el objeto `ApiOficialTipoCambio` si tiene éxito.
         */
        private suspend fun fetchFromApi(): ApiOficialTipoCambio? {
            return withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url(Constants.URL_BASE + Constants.ENDPOINT)
                        .addHeader("Authorization", Constants.BEARER_TOKEN)
                        .build()

                    val response = client.newCall(request).execute()

                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        // Usamos Gson para convertir el JSON directamente a tu modelo de datos.
                        Gson().fromJson(responseBody, ApiOficialTipoCambio::class.java)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }

        /**
         * Actualiza la UI del widget con los datos proporcionados.
         */
        private suspend fun updateWidgetViews(
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            views: RemoteViews,
            usdData: Usd, // Recibimos el objeto Usd directamente
            eurData: Eur, // Recibimos el objeto Eur directamente
            usdtData: Usdt,
            isFromCache: Boolean = false
        ) {
            withContext(Dispatchers.Main) {
                val dateOnly = usdData.last_update.split(",").firstOrNull()?.trim() ?: usdData.last_update

                var finalDateText = " $dateOnly"
                // Si los datos vienen del caché, añadimos un indicador visual.
                if (isFromCache) {
                    finalDateText = "⚠ $dateOnly"
                }

                views.setTextViewText(R.id.text_view_dolar_rate, "%.2f".format(usdData.price))
                views.setTextViewText(R.id.text_view_euro_rate, "%.2f".format(eurData.price))
                views.setTextViewText(R.id.text_view_USDT_rate, "%.2f".format(usdtData.price))
                views.setTextViewText(R.id.text_view_updated, finalDateText)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private suspend fun showErrorInWidget(
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            views: RemoteViews
        ) {
            withContext(Dispatchers.Main) {
                views.setTextViewText(R.id.text_view_dolar_rate, "Error")
                views.setTextViewText(R.id.text_view_euro_rate, "N/A")
                views.setTextViewText(R.id.text_view_USDT_rate, "N/A")
                views.setTextViewText(R.id.text_view_updated, "Sin conexión")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}