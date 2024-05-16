package com.carlosv.dolaraldia.model.configImagen

import com.beust.klaxon.Klaxon
import java.util.Date

private val klaxon = Klaxon()
data class ConfigImagenModel (

    var id: String? = null,
    var url: String? = null,
    val linkAfiliado: String? = null,
    val date: Date?= null,
    val nombre:String?=null,
    val verificado: Boolean? = false,
    val pagina: String?= null
) {
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<ConfigImagenModel>(json)
    }
}