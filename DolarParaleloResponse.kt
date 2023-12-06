package com.carlosv.dolaraldia

import com.google.gson.annotations.SerializedName

data class DolarParaleloResponse (
    @SerializedName("last_update") val fecha: String,
    @SerializedName("price")val preciodolarParalelo: Double,
    @SerializedName("price_old")val priceparalelo_old: Long,
    @SerializedName("title")val title: String,
    @SerializedName("type")val origen: String
)