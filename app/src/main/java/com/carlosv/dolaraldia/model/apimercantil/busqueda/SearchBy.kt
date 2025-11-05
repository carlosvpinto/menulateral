package com.carlosv.dolaraldia.model.apimercantil.busqueda

data class SearchBy(
    val amount: Double,
    val currency: String,
    val origin_mobile_number: String,  // Cifrado
    val destination_mobile_number: String,  // Cifrado
    val payment_reference: String,
    val trx_date: String
)