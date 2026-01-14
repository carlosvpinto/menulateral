package com.carlosv.dolaraldia.utils.ads

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.carlosv.menulateral.BuildConfig
import com.carlosv.menulateral.R
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

class BannerAdManager(private val context: Context) {

    /**
     * Crea un AdView programáticamente y lo añade al contenedor.
     * @param adContainer El FrameLayout o LinearLayout donde vivirá el anuncio.
     * @param delayMillis Tiempo de espera.
     */
    fun showBanner(adContainer: ViewGroup, delayMillis: Long = 0) {

        // 1. Crear el AdView por código (Esto evita el error del XML)
        val adView = AdView(context)

        // 2. Configurar el tamaño (OBLIGATORIO cuando se hace por código)
        adView.setAdSize(AdSize.BANNER)

        // 3. Selección automática del ID (Seguridad)
        val adUnitId = if (BuildConfig.DEBUG) {
            Log.d("BannerAdManager", "Modo DEBUG: Usando ID de prueba")
            context.getString(R.string.banner_ad_prueba)
        } else {
            Log.d("BannerAdManager", "Modo RELEASE: Usando ID de producción")
            context.getString(R.string.banner_ad_ejecucion)
        }
        adView.adUnitId = adUnitId

        // 4. Limpiar contenedor y añadir el anuncio
        adContainer.removeAllViews()
        adContainer.addView(adView)

        // 5. Crear la petición
        val adRequest = AdRequest.Builder().build()

        // 6. Listeners
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() { Log.d("BannerAdManager", "Banner cargado.") }
            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e("BannerAdManager", "Fallo banner: ${error.message}")
            }
        }

        // 7. Cargar con retraso
        if (delayMillis > 0) {
            adContainer.postDelayed({
                adView.loadAd(adRequest)
            }, delayMillis)
        } else {
            adView.loadAd(adRequest)
        }
    }
}