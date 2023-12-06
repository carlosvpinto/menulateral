package com.carlosv.dolaraldia

import com.carlosv.preciodolarvzla.Datetime
import com.carlosv.preciodolarvzla.Monitors

data class DolarBCVResponse(
    var datetime: Datetime,
    var monitors: Monitors
)