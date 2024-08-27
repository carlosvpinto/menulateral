package com.carlosv.dolaraldia.model.history

data class HistoryModelResponse(
    var datetime: Datetime,
    var history: List<History>
)