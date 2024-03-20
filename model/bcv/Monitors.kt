package com.carlosv.dolaraldia.model.bcv

data class Monitors(
    var cny: Cny,
    var eur: Eur,
    var last_update: String,
    var rub: Rub,
    var `try`: Try,
    var usd: Usd
)