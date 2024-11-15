package com.carlosv.dolaraldia.model.apimercantil

data class TransactionC2P(
    val amount: Double,
    val currency: String,
    val destination_bank_id: String,
    val destination_id: String, // Encriptado
    val destination_mobile_number: String, // Encriptado
    val origin_mobile_number: String,
    val payment_reference: String,
    val trx_type: String,
    val payment_method: String,
    val invoice_number: String,
    val twofactor_auth: String // Encriptado
)