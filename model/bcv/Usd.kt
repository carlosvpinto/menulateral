package com.carlosv.dolaraldia.model.bcv

data class Usd(
    var price: String,
    var price_old: Double,
    var last_update: String,
    var title: String,
    var color: String,
    var percent: String
)