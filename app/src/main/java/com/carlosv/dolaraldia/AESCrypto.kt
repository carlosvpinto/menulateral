package com.carlosv.dolaraldia

import java.nio.charset.Charset
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

class AESCrypto {

//    companion object {
//        private const val ALGORITHM = "AES/ECB/PKCS5Padding"
//        private const val AES = "AES"
//
//        // Función para establecer la clave de cifrado
//        private fun setKey(key: String): SecretKeySpec {
//            val sha = MessageDigest.getInstance("SHA-256")
//            var keyBytes = key.toByteArray(Charset.forName("UTF-8"))
//            keyBytes = sha.digest(keyBytes)
//            keyBytes = keyBytes.copyOf(16)  // Solo usamos los primeros 16 bytes para AES
//            return SecretKeySpec(keyBytes, AES)
//        }
//
//        // Función para cifrar
//        fun encrypt(data: String, key: String): String {
//            val secretKey = setKey(key)
//            val cipher = Cipher.getInstance(ALGORITHM)
//            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
//            val encrypted = cipher.doFinal(data.toByteArray(Charset.forName("UTF-8")))
//            return Base64.encodeToString(encrypted, Base64.NO_WRAP) // Codificar en Base64 para enviar por API
//        }
//
//        // Función para descifrar
//        fun decrypt(encryptedData: String, key: String): String {
//            val secretKey = setKey(key)
//            val cipher = Cipher.getInstance(ALGORITHM)
//            cipher.init(Cipher.DECRYPT_MODE, secretKey)
//            val decodedEncryptedData = Base64.decode(encryptedData, Base64.NO_WRAP)
//            val decrypted = cipher.doFinal(decodedEncryptedData)
//            return String(decrypted, Charset.forName("UTF-8"))
//        }
//    }
}
