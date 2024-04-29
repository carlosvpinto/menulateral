package com.carlosv.dolaraldia

import com.carlosv.dolaraldia.model.bancos.DolarNew
import com.carlosv.dolaraldia.model.bcv.BcvNew
import com.carlosv.dolaraldia.model.monedas.DolarInstituciones
import com.carlosv.dolaraldia.model.paralelo.ParaleloVzla
import com.carlosv.dolaraldia.ui.bancos.BancosModel
import retrofit2.http.GET
import retrofit2.http.Url

interface ApiService {
    @GET
    suspend fun getParalelovzla(@Url url: String): ParaleloVzla

    @GET
    suspend fun getBcv(@Url url: String):BcvNew

 @GET
    suspend fun getDolarNew(@Url url: String): DolarNew



    @GET
    suspend fun getInstituciones(@Url url: String):DolarInstituciones

    @GET
    suspend fun getBancos(@Url url: String):BancosModel
}