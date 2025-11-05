package com.carlosv.dolaraldia.model.apimercantil.busqueda

// Modelo para la estructura JSON de la solicitud
data class MobilePaymentSearchRequest(
    val merchant_identify: MerchantIdentify,
    val client_identify: ClientIdentify,
    val search_by: SearchBy
)