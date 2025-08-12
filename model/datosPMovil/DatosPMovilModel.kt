package com.carlosv.dolaraldia.model.datosPMovil

import androidx.annotation.Keep
import com.beust.klaxon.Klaxon


@Keep
class DatosPMovilModel(
    var seleccionado: Boolean= false,
    val tipo:String?= null,
    val nombre:String?= null,
    val tlf: String? = null,
    val cedula: String? = null,
    val banco: String? = null,
    val fecha: String? = null,
    var imagen: String? = null // <-- campo para la ruta de la imagen

    )