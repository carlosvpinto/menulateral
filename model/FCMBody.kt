package com.carlosv.dolaraldia.model

class FCMBody(
    val to: String,
    val priority: String,
    val ttl: String,
    val data: MutableMap<String, String>,
) {
}