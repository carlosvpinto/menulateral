package com.carlosv.dolaraldia.model.searchc2p

import com.google.gson.annotations.SerializedName

// Estructuras para la Petición (REQUEST)
data class MercantilSearchRequest(
    @SerializedName("merchant_identify") val merchantIdentify: MerchantIdentify,
    @SerializedName("client_identify") val clientIdentify: ClientIdentify,
    @SerializedName("search_by") val searchBy: SearchBy
)

data class MerchantIdentify(
    @SerializedName("integratorId") val integratorId: String,
    @SerializedName("merchantId") val merchantId: String,
    @SerializedName("terminalId") val terminalId: String
)

data class ClientIdentify(
    @SerializedName("ipaddress") val ipAddress: String,
    @SerializedName("browser_agent") val browserAgent: String,
    @SerializedName("mobile") val mobile: MobileInfo
)

data class MobileInfo(
    @SerializedName("manufacturer") val manufacturer: String
)

data class SearchBy(
    @SerializedName("amount") val amount: String,
    @SerializedName("currency") val currency: String = "ves",
    @SerializedName("destination_mobile_number") val destinationMobileNumber: String,
    @SerializedName("origin_mobile_number") val originMobileNumber: String,
    @SerializedName("payment_reference") val paymentReference: String,
    @SerializedName("trx_date") val transactionDate: String
)

// ========================================================================
// Estructuras para la Respuesta (RESPONSE) - VERSIÓN FINAL CORREGIDA
// ========================================================================

data class MercantilSearchResponse(
    @SerializedName("merchant_identify") val merchantIdentify: MerchantIdentify?,
    @SerializedName("transaction_list") val transactionList: List<TransactionDetails>?
)

data class TransactionDetails(
    @SerializedName("trx_date") val transactionDate: String?,
    @SerializedName("trx_type") val transactionType: String?,
    @SerializedName("authorization_code") val authorizationCode: String?,
    @SerializedName("payment_reference") val paymentReference: Long?,
    @SerializedName("invoice_number") val invoiceNumber: String?,
    @SerializedName("payment_method") val paymentMethod: String?,
    @SerializedName("origin_mobile_number") val originMobileNumber: String?,
    @SerializedName("destination_mobile_number") val destinationMobileNumber: String?,
    @SerializedName("destination_id") val destinationId: String?,
    @SerializedName("currency") val currency: String?,

    // Double puede manejar tanto enteros (5.0) como decimales (11.12)
    @SerializedName("amount") val amount: Double?,

    @SerializedName("destination_bank_id") val destinationBankId: Int?
)