package com.carlosv.dolaraldia

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LifecycleObserver
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd

import android.widget.Toast
import androidx.lifecycle.Lifecycle

import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.crashlytics.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging

import java.util.Date

private const val LOG_TAG: String = "AppOpenAdManager"
//private const val AD_UNIT_ID: String ="ca-app-pub-3940256099942544/9257395921" //Para desarrollo y Pruebas
private const val AD_UNIT_ID: String  = "ca-app-pub-5303101028880067/8981364608"
/** Variable para asegurar que el anuncio se muestra solo una vez */
private var hasAdBeenShown = false


/** Application class that initializes, loads and show ads when activities change states. */

class MyApplication :
    Application(), Application.ActivityLifecycleCallbacks, LifecycleObserver {

    private lateinit var appOpenAdManager: AppOpenAdManager
    private var currentActivity: Activity? = null


    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appOpenAdManager = AppOpenAdManager()

        if (BuildConfig.DEBUG) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
        } else {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        }


    }

    //Crea el Token de del dispoitivo para Firebase, Una sola vez se crea

    private fun creaTokenFirebase(): String {
        var token = ""
        Firebase.messaging.token.addOnCompleteListener {
            if (!it.isSuccessful) {
                Log.d(LOG_TAG, "onCreate: el token no fue generado")
                return@addOnCompleteListener
            }
            token = it.result
            Log.d(LOG_TAG, "Token Creado $token")
        }
        return token
    }

    //Muestra la ID de la apliacion
    private fun optenerID(): String {
        var claveId = ""
        FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                claveId = task.result
                Log.d("FirebaseInstallations", "FID: $claveId")

            } else {
                Log.e("FirebaseInstallations", "Error al obtener el FID", task.exception)
                return@addOnCompleteListener
            }
        }
        return claveId
    }


    //    /**Método LifecycleObserver que muestra el anuncio de apertura de la aplicación cuando la aplicación pasa al primer plano. */
//    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
//    fun onMoveToForeground() {
//        // Muestra el anuncio (si está disponible) cuando la aplicación pasa al primer plano.
//        currentActivity?.let { appOpenAdManager.showAdIfAvailable(it) }
//    }
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreateLifecycle() {
        // Muestra el anuncio (si está disponible) cuando la aplicación pasa al primer plano.
        currentActivity?.let { appOpenAdManager.showAdIfAvailable(it) }
        // Acciones que deseas realizar cuando se crea el LifecycleOwner
    }

//    @OnLifecycleEvent(Lifecycle.Event.ON_START)
//    fun onStart() {
//        currentActivity?.let { appOpenAdManager.showAdIfAvailable(it) }
//        // Acciones que deseas realizar cuando el LifecycleOwner pasa a estado STARTED
//    }


    /** ActivityLifecycleCallback methods. */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        // Se inicia una actividad publicitaria cuando se muestra un anuncio, que podría ser la clase AdActivity de Google
        // SDK u otra clase de actividad implementada por un socio de mediación externo. Actualizando el
        // currentActivity solo cuando un anuncio no se muestra garantizará que no se trate de una actividad publicitaria, sino del
        // uno que muestra el anuncio.
        if (!appOpenAdManager.isShowingAd) {
            currentActivity = activity
        }

    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    /**
     * Shows an app open ad.
     *
     * @param activity the activity that shows the app open ad
     * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
     */
    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
        // Ajustamos el anuncio del programa si está disponible para exigir que otras clases solo interactúen con MiAplicación
        // clase.
        appOpenAdManager.showAdIfAvailable(activity, onShowAdCompleteListener)
    }

    /**
     * Load an app open ad.
     *
     * @param activity the activity that shows the app open ad
     */
    fun loadAd(activity: Context) {
        // We wrap the loadAd to enforce that other classes only interact with MyApplication
        // class.
        appOpenAdManager.loadAd(activity)
    }

    /**
     * Interface definition for a callback to be invoked when an app open ad is complete (i.e.
     * dismissed or fails to show).
     */
    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }

    /** Inner class that loads and shows app open ads. */
    private inner class AppOpenAdManager {

        private var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager =
            GoogleMobileAdsConsentManager.getInstance(applicationContext)
        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        var isShowingAd = false

        /** Keep track of the time an app open ad is loaded to ensure you don't show an expired ad. */
        private var loadTime: Long = 0

        /**
         * Load an ad.
         *
         * @param context the context of the activity that loads the ad
         */
        fun loadAd(context: Context) {
            // Do not load ad if there is an unused ad or one is already loading.
            if (isLoadingAd || isAdAvailable()) {
                return
            }

            isLoadingAd = true
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                context,
                AD_UNIT_ID,
                request,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    /**
                     * Se llama cuando se ha cargado un anuncio abierto de aplicación.
                     *
                     * @param anuncia el anuncio abierto de la aplicación cargada.
                     */
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        isLoadingAd = false
                        loadTime = Date().time
                        Log.d(LOG_TAG, "Cargado!!")
                        // Toast.makeText(context, "Cargado!!", Toast.LENGTH_SHORT).show()
                        // Mostrar el anuncio automáticamente si no se ha mostrado antes
                        if (!hasAdBeenShown) {
                            showAdIfAvailable(context as Activity)
                        }
                    }

                    /**
                     * Called when an app open ad has failed to load.
                     *
                     * @param loadAdError the error.
                     */
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isLoadingAd = false
                        Log.d(LOG_TAG, "No se pudo Cargar el Anuncio: " + loadAdError.message)
                        //  Toast.makeText(context, "onAdFai ledToLoad", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        /** ** Verifique si el anuncio se cargó hace más de n horas. * */
        private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
            val dateDifference: Long = Date().time - loadTime
            val numMilliSecondsPerHour: Long = 3600000
            return dateDifference < numMilliSecondsPerHour * numHours
        }

        /**Compruebe si el anuncio existe y se puede mostrar. */
        private fun isAdAvailable(): Boolean {
            // Las referencias a anuncios en la versión beta abierta de la aplicación expirarán después de cuatro horas, pero este límite de tiempo
            // puede cambiar en futuras versiones beta. Para más detalles, consulte:
            // https://support.google.com/admob/answer/9341964?hl=en
            return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
        }

        /**
         * Show the ad if one isn't already showing.
         *
         * @param activity the activity that shows the app open ad
         */
        fun showAdIfAvailable(activity: Activity) {
            showAdIfAvailable(
                activity,
                object : OnShowAdCompleteListener {
                    override fun onShowAdComplete() {
                        // Vacío porque el usuario volverá a la actividad que muestra el anuncio.
                    }
                }
            )
        }

        /**
         * Show the ad if one isn't already showing.
         *
         * @param activity the activity that shows the app open ad
         * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
         */
        fun showAdIfAvailable(
            activity: Activity,
            onShowAdCompleteListener: OnShowAdCompleteListener,
        ) {
            // Si el anuncio de aplicación abierta ya se muestra, no vuelva a mostrarlo.
            if (isShowingAd) {
                Log.d(LOG_TAG, "El anuncio de apertura de la aplicación ya se muestra.")
                return
            }

            // Si el anuncio de apertura de la aplicación aún no está disponible, invoque la devolución de llamada.
            if (!isAdAvailable()) {
                Log.d(LOG_TAG, "El anuncio de apertura de la aplicación aún no está listo.")
                onShowAdCompleteListener.onShowAdComplete()
                if (googleMobileAdsConsentManager.canRequestAds) {
                    loadAd(activity)
                }
                return
            }

            Log.d(LOG_TAG, "Mostrará anuncio.")

            appOpenAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    /**Se llama cuando se descarta el contenido de pantalla completa.*/
                    override fun onAdDismissedFullScreenContent() {
                        // Establezca la referencia en nulo para que isAdAvailable() devuelva falso.
                        appOpenAd = null
                        isShowingAd = false
                        Log.d(LOG_TAG, "en Anuncio descartado en Contenido de pantalla completa.")
                        //  Toast.makeText(activity, "en Anuncio descartado ed Contenido de pantalla completa", Toast.LENGTH_SHORT).show()

                        onShowAdCompleteListener.onShowAdComplete()
                        // if (googleMobileAdsConsentManager.canRequestAds) {
                        loadAd(activity)
                        //   }
                    }

                    /** Called when fullscreen content failed to show. */
                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        appOpenAd = null
                        isShowingAd = false
                        Log.d(
                            LOG_TAG,
                            "el anuncio no se puede mostrar cuando la aplicación no está en primer plano: " + adError.message
                        )
                        //Toast.makeText(activity, " el anuncio no se puede mostrar cuando la aplicación no está en primer plano", Toast.LENGTH_SHORT).show()

                        onShowAdCompleteListener.onShowAdComplete()
                        if (googleMobileAdsConsentManager.canRequestAds) {
                            loadAd(activity)
                        }
                    }

                    /** Se llama cuando se muestra contenido en pantalla completa.*/
                    override fun onAdShowedFullScreenContent() {
                        Log.d(
                            LOG_TAG,
                            "** Se llama cuando se muestra contenido en pantalla completa."
                        )
                        //  Toast.makeText(activity, "contenido en pantalla completa.", Toast.LENGTH_SHORT).show()
                    }
                }
            isShowingAd = true
            appOpenAd?.show(activity)
            hasAdBeenShown = true  // Marcar que el anuncio ya se mostró
        }
    }
}