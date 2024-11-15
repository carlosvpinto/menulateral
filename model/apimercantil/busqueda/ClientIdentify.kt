package com.carlosv.dolaraldia.model.apimercantil.busqueda

data class ClientIdentify(
    val ipaddress: String,
    val browser_agent: String,
    val mobile: MobileInfo,
    val os_version: String,
    val location: Location
)