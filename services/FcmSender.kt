package com.carlosv.dolaraldia.services
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

// --- Modelos de Datos para la solicitud a la API de FCM ---

data class FcmNotificationPayload(
    @SerializedName("to") val to: String, // El token del dispositivo de destino
    @SerializedName("priority") val priority: String = "high",
    @SerializedName("data") val data: Map<String, String>
)

// --- Interfaz de Retrofit ---

interface FcmApiService {
    @Headers(
        "Content-Type: application/json"
    )
    // --- ¡AQUÍ ESTÁ LA CORRECCIÓN! ---
    // El endpoint es simplemente "fcm/send". Retrofit lo añadirá a la URL base.
    @POST("fcm/send")
    suspend fun sendNotification(
        @retrofit2.http.Header("Authorization") serverKey: String,
        @Body payload: FcmNotificationPayload
    ): Response<Unit>
}

// --- Objeto Singleton para crear la instancia de Retrofit ---

object FcmApiClient {
    val api: FcmApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://fcm.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FcmApiService::class.java)
    }
}