package com.carlosv.dolaraldia.model.controlPublicidad

import com.beust.klaxon.Klaxon

private val klaxon = Klaxon()
data class ImprecionesArtiModel (

    var id: String? = null,
    var nombre: String? = null,
    var numeroImpresiones: Long? = 0,
    var numeroClic: Long? = 0,
    val verificado: Boolean? = false,
    val fecha: String?= null
) {
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<ImprecionesArtiModel>(json)
    }
}