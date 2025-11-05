package com.carlosv.dolaraldia.model.apimercantil

import com.carlosv.dolaraldia.model.apimercantil.busqueda.ClientIdentify
import com.carlosv.dolaraldia.model.apimercantil.busqueda.MerchantIdentify

data class PaymentRequest(
    val merchant_identify: MerchantIdentify,
    val client_identify: ClientIdentify,
    val transaction_c2p: TransactionC2P
)
