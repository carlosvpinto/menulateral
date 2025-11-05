package com.carlosv.dolaraldia.ui.bancos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class InstitucionesViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Por Ahora No esta disponible"
    }
    val text: LiveData<String> = _text
}