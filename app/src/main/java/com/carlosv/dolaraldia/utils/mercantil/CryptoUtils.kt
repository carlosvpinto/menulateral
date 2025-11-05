package com.carlosv.dolaraldia.utils.mercantil

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/ECB/PKCS5Padding" // PKCS5Padding es el estándar para ECB en Java

    /**
     * Cifra un mensaje usando AES-128-ECB con una clave derivada de SHA-256.
     * @param message El mensaje a cifrar (ej. el número de teléfono).
     * @param key La clave secreta (SECRETKEY de tu config).
     * @return El mensaje cifrado en formato Base64.
     */
    fun encrypt(message: String, key: String): String {
        try {
            // Genera la clave de 16 bytes a partir del hash SHA-256 de la secret key
            val secretKeySpec = generateKey(key)

            // Inicia el cifrador
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)

            // Cifra el mensaje
            val encryptedBytes = cipher.doFinal(message.toByteArray(Charsets.UTF_8))

            // Devuelve el resultado en Base64
            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            // En una app real, maneja este error apropiadamente
            return ""
        }
    }

    /**
     * Genera la clave AES-128 a partir de la llave secreta, replicando la lógica de NodeJS.
     */
    private fun generateKey(key: String): SecretKeySpec {
        // 1. Convertir la llave secreta en un hash SHA256
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(key.toByteArray(Charsets.UTF_8))

        // 2. Obtener los primeros 16 bytes del hash (128 bits)
        val keyBytes = hashBytes.copyOfRange(0, 16)

        // 3. Crear la especificación de la clave para AES
        return SecretKeySpec(keyBytes, ALGORITHM)
    }
}