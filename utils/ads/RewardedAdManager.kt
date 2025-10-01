package com.carlosv.dolaraldia.utils.ads // O el paquete que prefieras

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

private const val TAG = "RewardedAdManager"
// ID de anuncio de prueba. ¡Siempre usa este para desarrollo!
private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
// TODO: Reemplázalo por tu ID de producción antes de publicar.

class RewardedAdManager(private val context: Context) {

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    // Interfaz para notificar a la Activity/Fragment cuando la recompensa se ha ganado.
    interface RewardListener {
        fun onRewardEarned()
        fun onAdFailedToLoad()
        fun onAdNotReady()
    }

    init {
        loadAd() // Precargamos un anuncio al inicializar el gestor.
    }

    private fun loadAd() {
        if (isLoading) return
        isLoading = true
        Log.d(TAG, "Iniciando carga de anuncio bonificado...")

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
                isLoading = false
                Log.d(TAG, "Anuncio bonificado cargado exitosamente.")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                rewardedAd = null
                isLoading = false

                Log.e(TAG, "Fallo al cargar el anuncio bonificado: ${loadAdError.message}")
            }
        })
    }

    fun showAd(activity: Activity, listener: RewardListener) {
        if (rewardedAd == null) {
            Log.d(TAG, "Intento de mostrar abortado: el anuncio no está listo.")
            listener.onAdNotReady()
            loadAd() // Intentamos cargar uno nuevo para la próxima vez.
            return
        }

        Log.d(TAG, "Mostrando anuncio bonificado...")

        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                // El anuncio se cerró. Limpiamos la referencia y precargamos el siguiente.
                rewardedAd = null
                loadAd()
            }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Fallo al mostrar el anuncio: ${adError.message}")
                rewardedAd = null
                loadAd()
            }
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Anuncio mostrado en pantalla completa.")
            }
        }

        rewardedAd?.show(activity, OnUserEarnedRewardListener {
            // ¡El usuario vio el video completo! Aquí se entrega la recompensa.
            Log.d(TAG, "¡Recompensa ganada por el usuario!")
            listener.onRewardEarned()
        })
    }
}