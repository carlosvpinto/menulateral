package com.carlosv.dolaraldia.provider

import android.util.Log
import com.carlosv.dolaraldia.model.controlPublicidad.ConfigImagenModel
import com.carlosv.dolaraldia.model.controlPublicidad.ImprecionesArtiModel
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

import com.google.firebase.firestore.DocumentSnapshot
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class RegistroPubliProvider {

    val db = Firebase.firestore.collection("RegistroPublidaBD")
    val authProvider = AuthProvider()

    fun create(imagen: ImprecionesArtiModel): Task<DocumentReference> {
        return db.add(imagen).addOnFailureListener {
            Log.d("FIRESTORE", "ERROR: ${it.message}")
        }
    }

    suspend fun createWithCoroutines(imagen: ImprecionesArtiModel): DocumentReference {
        return suspendCoroutine { continuation ->
            db.add(imagen)
                .addOnSuccessListener { documentReference ->
                    continuation.resume(documentReference)
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }
    }

    fun getLastHistory(): Query { // CONSULTA COMPUESTA - INDICE
        return db.whereEqualTo("idClient", authProvider.getId())
            .orderBy("timestamp", Query.Direction.DESCENDING).limit(1)
    }

    fun getImageConfig(): Query { // CONSULTA COMPUESTA - INDICE
        return db.whereEqualTo("idClient", authProvider.getId())
            .orderBy("timestamp", Query.Direction.DESCENDING)
    }

    fun getAllImagenConfig(): Query {
        // Retorna una consulta que recupera todos los documentos de la colección
        return db
    }
}