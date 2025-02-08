package com.carlosv.dolaraldia

import com.carlosv.dolaraldia.model.apicontoken.ApiConTokenResponse
import com.carlosv.dolaraldia.model.apicontoken2.ApiModelResponseCripto
import com.carlosv.dolaraldia.model.apicontoken2.ApiModelResponseBCV
import com.carlosv.dolaraldia.model.apimercantil.ApiResponse
import com.carlosv.dolaraldia.model.apimercantil.PaymentRequest
import com.carlosv.dolaraldia.model.apimercantil.busqueda.MobilePaymentSearchRequest
import com.carlosv.dolaraldia.model.history.HistoryModelResponse

import com.carlosv.dolaraldia.ui.bancos.BancosModel
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface ApiService {
//    @GET
//    suspend fun getBancos(@Url url: String): BancosModel


    @GET("dollar")
    suspend fun getDollar(): ApiConTokenResponse

    @GET("dollar")
    suspend fun getDollarcriptodolar(@Query("page") page: String): ApiModelResponseCripto
    @GET("dollar")
    suspend fun getDollarAlCambio(@Query("page") page: String): ApiConTokenResponse

    @GET("dollar")
    suspend fun getDollarBancosBcv(@Query("page") page: String): ApiModelResponseBCV

    @GET("euro")
    suspend fun getEuro(@Query("page") page: String): ApiConTokenResponse


    @GET("http://pydolarve.org/api/v1/")
    suspend fun getDollarInfo(@Header("Authorization") authorization: String): ApiConTokenResponse

    @GET("dollar/history")
    suspend fun getDollarHistory(
        @Query("page") page: String,
        @Query("monitor") monitor: String,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): HistoryModelResponse

    //** Para api Mercantil***********************
   /*
    @Headers(
        "Content-Type: application/json",
        "X-IBM-Client-Id: 81188330-c768-46fe-a378-ff3ac9e88824"
    )

    @POST("/mercantil-banco/sandbox/v1/mobile-payment/search")
    fun sendPaymentData(@Body paymentRequest: PaymentRequest): Call<ApiResponse>


*/

    @Headers("Content-Type: application/json")
    @POST("/mercantil-banco/sandbox/v1/payment/c2p")
    fun sendPaymentData(@Body paymentRequest: PaymentRequest): Call<ApiResponse>

    @POST("mobile-payment/search")
    fun buscarPagosMoviles(
        @Header("X-IBM-Client-Id") clientId: String,
        @Body request: MobilePaymentSearchRequest
    ): Call<ApiResponse>

    @Headers("X-IBM-Client-Id: 81188330-c768-46fe-a378-ff3ac9e88824", "Content-Type: application/json")
    @POST("mobile-payment/search")
    fun searchMobilePayment(@Body request: MobilePaymentSearchRequest): Call<ApiResponse>



}