package com.carlosv.dolaraldia.model.apiPlatforms

import com.google.gson.annotations.SerializedName

// Estructura principal de la respuesta
data class PlatformResponse(
    val datetime: PlatformDateTime,
    val platforms: Map<String, PlatformDetail> // Usamos un Map para las claves dinámicas (binance, bybit, etc.)
)

// Estructura para la fecha y hora
data class PlatformDateTime(
    val date: String,
    val time: String
)

// Estructura para el detalle de cada plataforma
data class PlatformDetail(
    val change: Double,
    @SerializedName("change_old")
    val changeOld: Double,
    val color: String,
    val image: String?, // La imagen puede ser nula
    @SerializedName("last_update")
    val lastUpdate: String,
    @SerializedName("last_update_old")
    val lastUpdateOld: String,
    val percent: Double,
    @SerializedName("percent_old")
    val percentOld: Double,
    val price: Double,
    @SerializedName("price_old")
    val priceOld: Double,
    @SerializedName("price_older")
    val priceOlder: Double,
    val symbol: String,
    val title: String
)