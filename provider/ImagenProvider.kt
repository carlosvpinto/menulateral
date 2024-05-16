package com.carlosv.dolaraldia.provider

import android.util.Log
import com.carlosv.dolaraldia.model.configImagen.ConfigImagenModel
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

import com.google.firebase.firestore.DocumentSnapshot

class ImagenProvider {

    val db = Firebase.firestore.collection("ConfiImagenBD")
    val authProvider = AuthProvider()

    fun create(imagen: ConfigImagenModel): Task<DocumentReference> {
        return db.add(imagen).addOnFailureListener {
            Log.d("FIRESTORE", "ERROR: ${it.message}")
        }
    }

    fun getLastHistory(): Query { // CONSULTA COMPUESTA - INDICE
        return db.whereEqualTo("idClient", authProvider.getId()).orderBy("timestamp", Query.Direction.DESCENDING).limit(1)
    }

    fun getImageConfig(): Query { // CONSULTA COMPUESTA - INDICE
        return db.whereEqualTo("idClient", authProvider.getId()).orderBy("timestamp", Query.Direction.DESCENDING)
    }

    fun getAllImagenConfig(): Query {
        // Retorna una consulta que recupera todos los documentos de la colección
        return db
    }


    fun getHistoryById(id: String): Task<DocumentSnapshot> {
        return db.document(id).get()
    }


    fun getBooking(): Query {
        return db.whereEqualTo("idDriver", authProvider.getId())
    }



    }



