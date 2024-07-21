package com.carlosv.dolaraldia.model.datosPMovil

import com.beust.klaxon.Klaxon

private val klaxon = Klaxon()
class DatosPMovilModel(
    var seleccionado: Boolean= false,
    val tipo:String?= null,
    val nombre:String?= null,
    val tlf: String? = null,
    val cedula: String? = null,
    val banco: String? = null,
    val fecha: String? = null,

    ) {

    fun toJson(): String = klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String): DatosPMovilModel? = klaxon.parse<DatosPMovilModel>(json)
    }


}