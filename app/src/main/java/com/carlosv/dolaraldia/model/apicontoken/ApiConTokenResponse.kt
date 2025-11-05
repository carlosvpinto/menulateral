package com.carlosv.dolaraldia.model.apicontoken

import com.carlosv.dolaraldia.model.apiAlcambioEuro.Monitors
import com.carlosv.dolaraldia.ui.bancos.Datetime

data class ApiConTokenResponse(
    var datetime: Datetime,
    var monitors: Monitors
)