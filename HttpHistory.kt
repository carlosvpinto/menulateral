package com.carlosv.dolaraldia

data class HttpHistory(
    var description: String,
    var metadata: Metadata,
    var results: List<Result>
)