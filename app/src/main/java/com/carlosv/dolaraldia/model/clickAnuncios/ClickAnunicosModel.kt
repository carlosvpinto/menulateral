package com.carlosv.dolaraldia.model.clickAnuncios

import com.beust.klaxon.Klaxon
import com.carlosv.menulateral.databinding.CardviewOtrasPaginasBinding
import java.sql.Timestamp
import java.util.Date

private val klaxon = Klaxon()
data class ClickAnunicosModel (

    var id: String? = null,
    val timestamp: Long? = null,
    val articulo: String? = null,
    val date: Date?= null,
    val verificado: Boolean? = false,
    val pagina:String? = null
) {
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<ClickAnunicosModel>(json)
    }
}