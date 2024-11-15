package com.carlosv.dolaraldia.model.apimercantil

data class ApiResponse(
    val status: String,
    val data: List<MobilePayment> // Asegúrate de ajustar esto a la estructura real de tu respuesta
)
