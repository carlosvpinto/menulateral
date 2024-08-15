package com.carlosv.dolaraldia

import android.telecom.Call
import com.carlosv.dolaraldia.model.apicontoken.ApiConTokenResponse
import com.carlosv.dolaraldia.model.bancos.DolarNew
import com.carlosv.dolaraldia.model.bcv.BcvNew
import com.carlosv.dolaraldia.model.monedas.DolarInstituciones
import com.carlosv.dolaraldia.model.paralelo.ParaleloVzla
import com.carlosv.dolaraldia.ui.bancos.BancosModel
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Url

interface ApiService {
    @GET
    suspend fun getParalelovzla(@Url url: String): ParaleloVzla

    @GET
    suspend fun getBcv(@Url url: String): BcvNew

    @GET
    suspend fun getDolarNew(@Url url: String): DolarNew



    @GET
    suspend fun getInstituciones(@Url url: String): DolarInstituciones

    @GET
    suspend fun getBancos(@Url url: String): BancosModel

    // Nuevo método getDollar
//    @GET("dollar")
//    suspend fun getDollar(@Query("page") page: String): ApiConTokenResponse

//    @GET("dollar")
//    suspend fun getDollar(
//        @Header("Authorization") token: String,
//    ): ApiConTokenResponse

    @GET("dollar")
    suspend fun getDollar(): ApiConTokenResponse

    @GET("dollar")
    suspend fun getDollarcriptodolar(
        @Query("page") page: String
    ): Response<ApiConTokenResponse>  // Usa el tipo de respuesta adecuado

    @GET("euro")
    suspend fun getEuro(@Query("page") page: String): ApiConTokenResponse


    @GET("http://pydolarve.org/api/v1/")
    suspend fun getDollarInfo(@Header("Authorization") authorization: String): ApiConTokenResponse


}