package com.carlosv.dolaraldia.model.monedas
import com.beust.klaxon.*

private val klaxon = Klaxon()
data class DolarInstituciones(
    var datetime: Datetime,
    var monitors: Monitors
){
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<DolarInstituciones>(json)
    }
}