package com.carlosv.dolaraldia

import com.carlosv.dolaraldia.model.monedas.DolarInstituciones
import com.carlosv.dolaraldia.ui.bancos.BancosModel
import retrofit2.http.GET
import retrofit2.http.Url

interface ApiService {
    @GET
    suspend fun getPrecioParalelo(@Url url: String): DolarParaleloResponse

    @GET
    suspend fun getBcv(@Url url: String):DolarBCVResponse

    @GET
    suspend fun getHistory(@Url url: String):HttpHistory

    @GET
    suspend fun getInstituciones(@Url url: String):DolarInstituciones

    @GET
    suspend fun getBancos(@Url url: String):BancosModel
}