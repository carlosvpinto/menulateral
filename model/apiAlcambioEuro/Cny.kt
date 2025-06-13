package com.carlosv.dolaraldia.model.apiAlcambioEuro

data class Cny(
    var change: Double,
    var change_old: Double,
    var color: String,
    var image: String,
    var last_update: String,
    var last_update_old: String,
    var percent: Double,
    var percent_old: Double,
    var price: Double,
    var price_old: Double,
    var price_older: Double,
    var symbol: String,
    var title: String
)