package com.carlosv.dolaraldia.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.carlosv.menulateral.R
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.*


class WebAppInterface(private val context: Context,private val price:Float, private val Listener: WebViewListener) {
    private val TAG = "PAYPALPRICE"
    
    @JavascriptInterface
    fun paymentfinished(status: String) {
        if (status==Constants.STATUS_SUCCESS) {// eL nUMERO DEBE SER 21
            // AQUI COLOCAR EL CODIGO A PAGO REALIZADO
            if (price== 1.5f){
                saveSubscriptionExpiration(context, 1)
            }
            if (price== 9.0f){
                saveSubscriptionExpiration(context, 12)
            }
            if (price== 12.0f){
                saveLifetimeSubscription(context)
            }

            Toast.makeText(context, R.string.frgm_payment_msg_thanks, Toast.LENGTH_SHORT).show()
            Listener.onSuccess()
        } else{

            Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
        }

    }

    // Función para guardar la fecha de expiración de la suscripción
    fun saveSubscriptionExpiration(context: Context, months: Int) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("UserSubscription", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Obtener la fecha actual
        val calendar = Calendar.getInstance()

        // Agregar los meses de la suscripción (1 mes para mensual, 12 para anual)
        calendar.add(Calendar.MONTH, months)
        Log.d(TAG, "ENTRO A SUSCRIPCION POR MESES: $calendar y calendar.timeInMillis: ${calendar.timeInMillis}")
        // Guardar la fecha de expiración
        editor.putLong("subscription_expiration", calendar.timeInMillis)
        editor.apply()
    }

    // Función para guardar una suscripción de por vida
    fun saveLifetimeSubscription(context: Context) {

        Log.d(TAG, "saveLifetimeSubscription: Entro a sucrupcion de por vida")
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("UserSubscription", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Guardar un valor booleano indicando que es una suscripción de por vida
        editor.putBoolean("lifetime_subscription", true)
        editor.apply()
    }

}