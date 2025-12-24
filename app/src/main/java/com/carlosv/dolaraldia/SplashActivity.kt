package com.carlosv.dolaraldia

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
//import com.carlosv.dolaraldia.utils.AppPreferences // Asegúrate de importar esto
import com.carlosv.dolaraldia.utils.Constants.MAX_WAIT_TIME
import com.carlosv.menulateral.R
import java.util.concurrent.atomic.AtomicBoolean

private val TAG: String = "AppOpenAdManager"

class SplashActivity : AppCompatActivity() {

    // Bandera atómica para evitar que se abra el MainActivity dos veces
    private var isNavigating = AtomicBoolean(false)
    private var countDownTimer: CountDownTimer? = null
    // 1. Declarar el animator como variable global de la clase
    private var animatorLogo: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        //*************************
        startAnimations()

        // --- 1. VERIFICACIÓN PREMIUM (PRIORIDAD MÁXIMA) ---
        if (AppPreferences.isUserPremiumActive()) {
            Log.d(TAG, "👑 Usuario Premium detectado. Iniciando modo rápido.")
            // No buscamos anuncios. No iniciamos el timer largo.
            // Iniciamos un timer cortito exclusivo para Premium (solo por estética).
            startPremiumFastTrack()
            return
        }

        // --- 2. LÓGICA ESTÁNDAR (USUARIOS GRATIS) ---

        // Optimización: ¿El anuncio ya está listo desde el inicio?
        val googleApp = application as? MyApplication
        if (googleApp != null && googleApp.isAdAvailable()) {
            Log.d(TAG, "Anuncio listo en caché, mostrando de inmediato.")
            showAdAndStartApp()
        } else {
            // Si no, iniciamos el contador de seguridad largo (ej. 6 segundos)
            createTimer(MAX_WAIT_TIME)
        }
    }

    private fun startAnimations() {
        val logo = findViewById<ImageView>(R.id.splash_activity_image)
        val title = findViewById<TextView>(R.id.splash_activity_title)

        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.15f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.15f)

        // 2. Asignarlo a la variable global
        animatorLogo = ObjectAnimator.ofPropertyValuesHolder(logo, scaleX, scaleY).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }
        animatorLogo?.start() // Usar el ?

        // ... (La animación del título está bien, esa termina sola) ...
    }

    // --- NUEVO MÉTODO PARA PREMIUM ---
    private fun startPremiumFastTrack() {
        // Solo esperamos 1.2 segundos para que la animación del logo se vea bonita.
        // Esto se siente "instantáneo" pero profesional.
        countDownTimer = object : CountDownTimer(1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // No hacemos nada, solo esperamos.
            }

            override fun onFinish() {
                Log.d(TAG, "👑 Tiempo Premium completado. Bienvenido.")
                showAdAndStartApp()
            }
        }
        countDownTimer?.start()
    }

    private fun createTimer(milliseconds: Long) {
        // Intervalo de 500ms para revisar más frecuentemente
        countDownTimer = object : CountDownTimer(milliseconds, 500) {
            override fun onTick(millisUntilFinished: Long) {
                val googleApp = application as? MyApplication

                // Si el anuncio carga durante la espera, cancelamos y mostramos
                if (googleApp != null && googleApp.isAdAvailable()) {
                    Log.d(TAG, "Anuncio cargó durante el timer.")
                    cancel()
                    showAdAndStartApp()
                }
            }

            override fun onFinish() {
                Log.d(TAG, "Tiempo agotado. Pasando a MainActivity.")
                showAdAndStartApp()
            }
        }
        countDownTimer?.start()
    }

    private fun showAdAndStartApp() {
        if (isNavigating.get()) return

        val googleApp = application as? MyApplication

        googleApp?.showAdIfAvailable(this, object : MyApplication.OnShowAdCompleteListener {
            override fun onShowAdComplete() {
                startMainActivity()
            }

            override fun onAdLoaded() {}
        })
    }
    private fun startMainActivity() {
        // Seteamos la bandera en true. Si ya era true, significa que ya nos fuimos.
        if (isNavigating.getAndSet(true)) return

        // Cancelamos el timer
        countDownTimer?.cancel()

        val intentMain = Intent(this, MainActivity::class.java)

        // --- CÓDIGO NUEVO (EL PUENTE) ---
        // Si el Splash recibió datos (de la notificación), se los pasamos al Main
        if (intent.extras != null) {
            intentMain.putExtras(intent.extras!!)
            Log.d(TAG, "Pasando datos de notificación al MainActivity")
        }
        // --------------------------------

        intentMain.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intentMain)

        // Transición suave
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        finish()
    }

    // 3. AGREGAR ESTO ES VITAL:
    override fun onDestroy() {
        super.onDestroy()
        // Detenemos la animación para liberar al procesador
        animatorLogo?.cancel()
        animatorLogo = null

        // También cancelamos el timer (que ya lo tenías, bien hecho)
        countDownTimer?.cancel()
    }
}