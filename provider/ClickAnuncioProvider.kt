package com.carlosv.dolaraldia.provider

import android.util.Log
import com.carlosv.dolaraldia.model.clickAnuncios.ClickAnunicosModel
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ClickAnuncioProvider {
    val db = Firebase.firestore.collection("ClickAnuncioBD")
    val authProvider = AuthProvider()

    fun create(anuncios: ClickAnunicosModel): Task<DocumentReference> {
        return db.add(anuncios).addOnFailureListener {
            Log.d("FIRESTORE", "ERROR: ${it.message}")
        }
    }

    fun getLastHistory(): Query { // CONSULTA COMPUESTA - INDICE
        return db.whereEqualTo("idClient", authProvider.getId()).orderBy("timestamp", Query.Direction.DESCENDING).limit(1)
    }

    fun getImageConfig(): Query { // CONSULTA COMPUESTA - INDICE
        return db.whereEqualTo("idClient", authProvider.getId()).orderBy("timestamp", Query.Direction.DESCENDING)
    }
}