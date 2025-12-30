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
import com.carlosv.dolaraldia.MainActivity
import com.carlosv.dolaraldia.model.apiAlcambioEuro.ApiOficialTipoCambio
import com.carlosv.dolaraldia.model.apiAlcambioEuro.Eur
import com.carlosv.dolaraldia.model.apiAlcambioEuro.Usd
import com.carlosv.dolaraldia.model.apiAlcambioEuro.Usdt
import com.carlosv.dolaraldia.utils.Constants
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class AppWidgetHome : AppWidgetProvider() {

    // Nota: Ya no usamos un scope global aquí para evitar fugas con el BroadcastReceiver.
    // Usaremos un scope local dentro de onUpdate junto con goAsync.

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            // 1. SOLUCIÓN CRÍTICA: Pedimos tiempo extra al sistema con goAsync
            val pendingResult = goAsync()

            // 2. Lanzamos la corrutina en el hilo IO
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 3. PROTECCIÓN CONTRA CONGELAMIENTO (TIMEOUT)
                    // Android mata el receiver a los ~30s. Nosotros cortamos a los 25s por seguridad.
                    withTimeout(25000L) {
                        updateAppWidgetLogic(context, appWidgetManager, widgetId)
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.e(TAG, "⚠️ Tiempo agotado en Widget (25s). Cancelando para evitar Crash.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error general en Widget: ${e.message}")
                } finally {
                    // 4. VITAL: Avisamos al sistema que terminamos.
                    // Si esto no se ejecuta, ocurre el error CannotDeliverBroadcastException.
                    try {
                        pendingResult.finish()
                    } catch (e: Exception) {
                        // Ignoramos si ya estaba cerrado
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "AppWidgetHome"

        private suspend fun updateAppWidgetLogic(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            Log.d(TAG, "Iniciando actualización para widget ID: $appWidgetId")

            // Preparamos las vistas
            val views = RemoteViews(context.packageName, R.layout.app_widget_home)
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            try {
                // --- PASO 1: Intentar obtener datos frescos de la API ---
                val apiResponse = fetchFromApi()

                if (apiResponse != null) {
                    Log.d(TAG, "Éxito en la API. Guardando en caché y actualizando widget.")
                    guardarResponse(context, apiResponse)

                    updateWidgetViews(
                        context,
                        appWidgetManager,
                        appWidgetId,
                        views,
                        apiResponse.monitors.usd,
                        apiResponse.monitors.eur,
                        apiResponse.monitors.usdt, // Agregado USDT
                        isFromCache = false
                    )
                } else {
                    throw Exception("La llamada a la API falló o devolvió null.")
                }

            } catch (e: Exception) {
                // --- PASO 2: Si la API falló, intentar cargar desde caché ---
                Log.e(TAG, "Error API: ${e.message}. Usando caché.")

                val cachedResponse = leerResponseLocal(context)

                if (cachedResponse != null) {
                    Log.d(TAG, "Éxito al cargar desde caché.")
                    updateWidgetViews(
                        context,
                        appWidgetManager,
                        appWidgetId,
                        views,
                        cachedResponse.monitors.usd,
                        cachedResponse.monitors.eur,
                        cachedResponse.monitors.usdt, // Agregado USDT
                        isFromCache = true
                    )
                } else {
                    // --- PASO 3: Fallo total ---
                    Log.e(TAG, "Sin datos en caché. Mostrando error.")
                    showErrorInWidget(appWidgetManager, appWidgetId, views)
                }
            }
        }

        // --- FUNCIONES DE SOPORTE ---

        private fun guardarResponse(context: Context, responseBCV: ApiOficialTipoCambio) {
            try {
                val gson = Gson()
                val responseJson = gson.toJson(responseBCV)
                val sharedPreferences = context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
                sharedPreferences.edit().putString("dolarBCVResponse", responseJson).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Error guardando caché: ${e.message}")
            }
        }

        // Función local para leer caché (Más segura que importarla de AppPreferences si hay cambios)
        private fun leerResponseLocal(context: Context): ApiOficialTipoCambio? {
            return try {
                val sharedPreferences = context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
                val json = sharedPreferences.getString("dolarBCVResponse", null)
                if (json != null) {
                    Gson().fromJson(json, ApiOficialTipoCambio::class.java)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

        private fun fetchFromApi(): ApiOficialTipoCambio? {
            return try {
                // Configuración de Timeouts para evitar cuelgues de red
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS) // Máximo 10s conectando
                    .readTimeout(10, TimeUnit.SECONDS)    // Máximo 10s leyendo
                    .build()

                val request = Request.Builder()
                    .url(Constants.URL_BASE + Constants.ENDPOINT)
                    .addHeader("Authorization", Constants.BEARER_TOKEN)
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Gson().fromJson(responseBody, ApiOficialTipoCambio::class.java)
                } else {
                    response.close()
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error de red: ${e.message}")
                null
            }
        }

        private fun updateWidgetViews(
            context: Context, // Pasamos contexto por si acaso, aunque no se usa en este bloque
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            views: RemoteViews,
            usdData: Usd,
            eurData: Eur,
            usdtData: Usdt,
            isFromCache: Boolean
        ) {
            // No necesitamos withContext(Main) para RemoteViews, es seguro hacerlo aquí.

            val dateOnly = usdData.last_update.split(",").firstOrNull()?.trim() ?: usdData.last_update
            var finalDateText = " $dateOnly"

            if (isFromCache) {
                finalDateText = "⚠ $dateOnly"
            }

            views.setTextViewText(R.id.text_view_dolar_rate, "%.2f".format(usdData.price))
            views.setTextViewText(R.id.text_view_euro_rate, "%.2f".format(eurData.price))
            // Agregamos USDT
            views.setTextViewText(R.id.text_view_USDT_rate, "%.2f".format(usdtData.price))

            views.setTextViewText(R.id.text_view_updated, finalDateText)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun showErrorInWidget(
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            views: RemoteViews
        ) {
            views.setTextViewText(R.id.text_view_dolar_rate, "Error")
            views.setTextViewText(R.id.text_view_euro_rate, "N/A")
            views.setTextViewText(R.id.text_view_USDT_rate, "N/A")
            views.setTextViewText(R.id.text_view_updated, "Sin conexión")
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}