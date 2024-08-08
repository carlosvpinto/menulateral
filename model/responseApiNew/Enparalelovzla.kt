package com.carlosv.dolaraldia.model.responseApiNew

data class Enparalelovzla(
    var change: Double,
    var color: String,
    var image: String,
    var last_update: String,
    var percent: Double,
    var price: Double,
    var price_old: Double,
    var symbol: String,
    var title: String
)