package com.carlosv.dolaraldia

import com.carlosv.dolaraldia.model.apicontoken.ApiConTokenResponse
import com.carlosv.dolaraldia.model.apicontoken2.ApiModelResponseCripto
import com.carlosv.dolaraldia.model.apicontoken2.ApiModelResponseBCV

import com.carlosv.dolaraldia.ui.bancos.BancosModel
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Url

interface ApiService {
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

//    @GET("dollar")
//    suspend fun getDollarcriptodolar(
//        @Query("page") page: String
//    ): Response<ApiConToken2>  // Usa el tipo de respuesta adecuado

    @GET("dollar")
    suspend fun getDollarcriptodolar(@Query("page") page: String): ApiModelResponseCripto

    @GET("dollar")
    suspend fun getDollarBancosBcv(@Query("page") page: String): ApiModelResponseBCV

    @GET("euro")
    suspend fun getEuro(@Query("page") page: String): ApiConTokenResponse


    @GET("http://pydolarve.org/api/v1/")
    suspend fun getDollarInfo(@Header("Authorization") authorization: String): ApiConTokenResponse


}