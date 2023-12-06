package com.carlosv.dolaraldia.model.monedas

data class Paypal(
    var last_update: String,
    var price: Double,
    var price_old: Double,
    var title: String,
    var type: String
)