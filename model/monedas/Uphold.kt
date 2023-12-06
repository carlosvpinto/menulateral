package com.carlosv.dolaraldia.model.monedas

data class Uphold(
    var last_update: String,
    var price: Int,
    var price_old: Double,
    var title: String,
    var type: String
)