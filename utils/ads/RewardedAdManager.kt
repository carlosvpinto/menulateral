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



class RewardedAdManager(private val context: Context) {

    private var mRewardedAd: RewardedAd? = null
    // ¡IMPORTANTE! Reemplaza este ID de prueba con tu ID de bloque de anuncios real para producción.
    private val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"


    interface AdLoadListener {
        fun onAdLoaded()
        fun onAdLoadFailed()
    }


    interface RewardListener {
        fun onRewardEarned()
        fun onAdFailedToLoad()
        fun onAdNotReady()
        fun onAdDismissed() // ¡MÉTODO AÑADIDO!
    }



    fun loadAd(listener: AdLoadListener) {
        if (mRewardedAd != null) {
            // Ya hay un anuncio cargado y listo.
            listener.onAdLoaded()
            return
        }

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(rewardedAd: RewardedAd) {
                mRewardedAd = rewardedAd
                listener.onAdLoaded()
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                mRewardedAd = null
                listener.onAdLoadFailed()
            }
        })
    }

    fun showAd(activity: Activity, listener: RewardListener) {
        if (mRewardedAd != null) {
            mRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {

                // --- ¡CORRECCIÓN 2! LLAMAMOS AL NUEVO MÉTODO CUANDO EL ANUNCIO SE CIERRA ---
                override fun onAdDismissedFullScreenContent() {
                    mRewardedAd = null
                    listener.onAdDismissed() // ¡Llamada al nuevo método!
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    mRewardedAd = null
                    listener.onAdFailedToLoad()
                }
            }
            mRewardedAd?.show(activity) {
                listener.onRewardEarned()
            }
        } else {
            listener.onAdNotReady()
        }
    }
}