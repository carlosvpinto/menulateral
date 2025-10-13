package com.carlosv.menulateral

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.carlosv.dolaraldia.MainActivity
import com.carlosv.dolaraldia.utils.Constants
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import okhttp3.Request

class AppWidgetHome : AppWidgetProvider() {

    // Se crea un CoroutineScope que se puede cancelar cuando el widget ya no lo necesite.
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, widgetId, scope)
        }
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        // Se cancela el scope cuando el último widget es eliminado para evitar fugas de memoria.
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
            views.setTextViewText(R.id.text_view_dolar_rate, "Cargando...")
            views.setTextViewText(R.id.text_view_euro_rate, "Cargando...")

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent =
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            scope.launch {
                // --- INICIO DE LA LÓGICA DE RED MEJORADA ---
                val client = OkHttpClient()
                val fullUrl = Constants.URL_BASE + Constants.ENDPOINT

                // Creamos la solicitud (Request) con la cabecera de autorización
                val request = Request.Builder()
                    .url(fullUrl)
                    .addHeader("Authorization", Constants.BEARER_TOKEN)
                    .build()

                Log.d(TAG, "Realizando llamada a la API: $fullUrl")

                try {
                    // Ejecutamos la llamada
                    val response = client.newCall(request).execute()

                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            val json = JSONObject(responseBody)
                            val monitorsObject = json.getJSONObject("monitors")

                            // 1. Obtenemos el dólar
                            val usdObject = monitorsObject.getJSONObject("usd")
                            val dolarPrice = usdObject.getDouble("price")


                            // 2. Obtenemos el euro
                            val euroObject = monitorsObject.getJSONObject("eur")
                            val euroPrice = euroObject.getDouble("price")


                            // --- INICIO DE LA MODIFICACIÓN DE LA FECHA ---

                            // 3. Extraemos la fecha de actualización desde la API (ej: "13/10/2025, 12:00 AM")
                            val lastUpdateFromApi = usdObject.getString("last_update")
                            Log.d(TAG, "Fecha extraída de la API: $lastUpdateFromApi")

                            // 4.  Nos quedamos solo con la parte de la fecha, antes de la coma.
                            val dateOnly = lastUpdateFromApi.split(",").firstOrNull()?.trim()
                                ?: lastUpdateFromApi
                            val finalDateText = " $dateOnly"


                            withContext(Dispatchers.Main) {
                                // Actualizamos los precios
                                views.setTextViewText(
                                    R.id.text_view_dolar_rate,
                                    "%.2f".format(dolarPrice)
                                )
                                views.setTextViewText(
                                    R.id.text_view_euro_rate,
                                    "%.2f".format(euroPrice)
                                )

                                // 5.Usamos la fecha formateada desde la API
                                views.setTextViewText(R.id.text_view_updated, finalDateText)

                                appWidgetManager.updateAppWidget(appWidgetId, views)
                            }
                        } else {
                            Log.e(TAG, "El cuerpo de la respuesta es nulo.")
                             showErrorInWidget(context, appWidgetManager, appWidgetId, views) // Asumo que tienes esta función
                        }
                    } else {
                        Log.e(TAG, "Llamada a la API no fue exitosa. Código: ${response.code}")
                         showErrorInWidget(context, appWidgetManager, appWidgetId, views)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al actualizar el widget ID: $appWidgetId", e)
                     showErrorInWidget(context, appWidgetManager, appWidgetId, views)
                }
            }
        }

        // Función de ayuda para no repetir código de error
        private suspend fun showErrorInWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            views: RemoteViews
        ) {
            withContext(Dispatchers.Main) {
                views.setTextViewText(R.id.text_view_dolar_rate, "Error al cargar")
                views.setTextViewText(R.id.text_view_euro_rate, "Revisa Logcat")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}