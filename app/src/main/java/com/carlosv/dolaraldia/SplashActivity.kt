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
import com.carlosv.menulateral.R
import java.util.concurrent.atomic.AtomicBoolean

// Reducimos el tiempo de espera a 2.5 segundos (Equilibrio entre cargar anuncio y UX)
private const val MAX_WAIT_TIME = 6000L
private val TAG: String = "AppOpenAdManager"

class SplashActivity : AppCompatActivity() {

    // Bandera atómica para evitar que se abra el MainActivity dos veces
    private var isNavigating = AtomicBoolean(false)
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        startAnimations()

        // 1. Optimización: ¿El anuncio ya está listo desde el inicio?
        val googleApp = application as? MyApplication
        if (googleApp != null && googleApp.isAdAvailable()) {
            Log.d(TAG, "Anuncio listo en caché, mostrando de inmediato.")
            showAdAndStartApp()
        } else {
            // 2. Si no, iniciamos el contador de seguridad
            createTimer(MAX_WAIT_TIME)
        }
    }

    private fun startAnimations() {
        val logo = findViewById<ImageView>(R.id.splash_activity_image)
        val title = findViewById<TextView>(R.id.splash_activity_title)

        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.15f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.15f)

        val animatorLogo = ObjectAnimator.ofPropertyValuesHolder(logo, scaleX, scaleY).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }
        animatorLogo.start()

        title.translationY = 100f
        title.alpha = 0f // Inicia invisible
        title.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(1200)
            .setStartDelay(300)
            .setInterpolator(OvershootInterpolator())
            .start()
    }

    private fun createTimer(milliseconds: Long) {
        // Intervalo de 500ms para revisar más frecuentemente, no cada segundo
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
                // Se acabó el tiempo. Si el usuario sigue aquí, avanzamos sin anuncio.
                Log.d(TAG, "Tiempo agotado. Pasando a MainActivity.")
                startMainActivity()
            }
        }
        countDownTimer?.start()
    }

    private fun showAdAndStartApp() {
        // Si ya estamos navegando, no hacemos nada
        if (isNavigating.get()) return

        val googleApp = application as? MyApplication

        googleApp?.showAdIfAvailable(this, object : MyApplication.OnShowAdCompleteListener {
            override fun onShowAdComplete() {
                // Ya sea que el anuncio se mostró y cerró, o falló al mostrarse:
                startMainActivity()
            }

            override fun onAdLoaded() {
                // No requerido aquí
            }
        })
    }

    private fun startMainActivity() {
        // Seteamos la bandera en true. Si ya era true, significa que ya nos fuimos.
        if (isNavigating.getAndSet(true)) return

        // Cancelamos el timer por seguridad si llegamos aquí por otro medio
        countDownTimer?.cancel()

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)

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

    override fun onDestroy() {
        super.onDestroy()
        // Limpieza para evitar memory leaks
        countDownTimer?.cancel()
    }
}