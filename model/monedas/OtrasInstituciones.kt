package com.carlosv.dolaraldia.model.monedas

data class OtrasInstituciones(
    var last_update: String,
    var price: Double,
    var price_old: Int,
    var title: String,
    var type: String
)