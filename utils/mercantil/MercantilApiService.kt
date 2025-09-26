package com.carlosv.dolaraldia.utils.mercantil


import com.carlosv.dolaraldia.model.searchc2p.MercantilSearchRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface MercantilApiService {

    @POST("mercantil-banco/prod/v1/mobile-payment/search")
    suspend fun searchPayment(
        @Header("X-IBM-Client-ID") clientId: String,
        @Body requestBody: MercantilSearchRequest
        // ==========> CAMBIO CLAVE AQUÍ <==========
        // Cambia de Response<MercantilSearchResponse> a Response<ResponseBody>
    ): Response<ResponseBody>
}

// En algún otro archivo, crea la instancia de Retrofit (ej. un objeto Singleton)
object RetrofitClient {
    val instance: MercantilApiService by lazy {
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("https://apimbu.mercantilbanco.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(MercantilApiService::class.java)
    }
}