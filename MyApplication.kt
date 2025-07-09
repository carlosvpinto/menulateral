package com.carlosv.dolaraldia

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.tasks.Task
import com.google.firebase.crashlytics.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date


private const val LOG_TAG: String = "AppOpenAdManager"
//private const val AD_UNIT_ID: String ="ca-app-pub-3940256099942544/9257395921" //Para desarrollo y Pruebas
//private const val AD_UNIT_ID: String  = "ca-app-pub-5303101028880067/8981364608"
private const val AD_UNIT_ID: String  = "ca-app-pub-3265312813580307/7449206999" // Admob Dolarmexico 2
/** Variable para asegurar que el anuncio se muestra solo una vez */
private var hasAdBeenShown = false


/** Application class that initializes, loads and show ads when activities change states. */

class MyApplication :
    Application(), Application.ActivityLifecycleCallbacks, LifecycleObserver {

    private lateinit var appOpenAdManager: AppOpenAdManager
    private var currentActivity: Activity? = null

    private val PREFS_NAME = "MyAppPrefs"
    private val TOPIC_SUBSCRIBED_KEY = "isSubscribedToTopic"

    private val TOKEN_KEY = "fcmToken"



    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appOpenAdManager = AppOpenAdManager()
        //countDeviceTokens()
        try {
            // Configura Firebase Crashlytics
            if (BuildConfig.DEBUG) {
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
            } else {
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            }
        } catch (e: Exception) {
            // Maneja el error, por ejemplo, registrándolo localmente o mostrando un mensaje al usuario
            Log.e("Firestore", " Crashlity Error initializing Firebase Crashlytics mandado por la app", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            // Opcional: notificar al usuario o enviar el error a otra herramienta de monitoreo
        }

        try {
           // crearTopicPrueba()
            getOrRequestToken()
            // borraTokenYcrear()  // Si este método es necesario, asegúrate de manejar posibles errores dentro de él también
            // checkAndSubscribeToTopic()  // Asegúrate de manejar los errores en este método si es necesario
        } catch (e: Exception) {
            // Maneja el error de inicialización de token o suscripción
            Log.e("firestore", "Error during initialization Mandado por la app", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }


    }

// excessive call to the Firestore database* NO ACTIVAR NUNCA SOLO RECUERDO!!*********************************************************************
    fun countDeviceTokens() {
        val db = FirebaseFirestore.getInstance()
        val tokensCollection = db.collection("device_tokens")

        tokensCollection.get()
            .addOnSuccessListener { snapshot ->
                val tokenCount = snapshot.size()
                Log.e("DeviceTokenCount", "Total de tokens guardados: $tokenCount")
            }
            .addOnFailureListener { exception ->
                Log.e("DeviceTokenCount", "Error al contar los tokens de dispositivos", exception)
            }
    }

//********End funcion************************************************************************

    // Función para obtener el token solo si no se tiene uno o si es necesario solicitar uno nuevo
    // Usa coroutines para ejecutar tareas largas fuera del hilo principal
    private fun getOrRequestToken() = CoroutineScope(Dispatchers.IO).launch {
        val savedToken = getSavedTokenFromPreferences(this@MyApplication)
        Log.d("Firestore", "getOrRequestToken: savedToken $savedToken")
        if (savedToken == null) {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d("Firestore", "getOrRequestToken: Obtener o salvar Token")
                saveTokenToPreferences(this@MyApplication, token)
                saveTokenToFirestore(token)
                crearTopic()
            } catch (e: Exception) {
                Log.e("Firestore", "Error obteniendo token", e)
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    // Función para obtener el token guardado en SharedPreferences
    private fun getSavedTokenFromPreferences(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(TOKEN_KEY, null) // Retorna null si no hay token guardado
    }

    // Función para guardar el token en SharedPreferences
    private fun saveTokenToPreferences(context: Context, token: String) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        Log.d("firestore", "saveTokenToPreferences: ")
        with(sharedPreferences.edit()) {
            putString(TOKEN_KEY, token)
            apply() // O usar commit() para hacerlo de manera sincrónica
        }
    }


    private fun borrraTokenYcrear(){
        FirebaseMessaging.getInstance().deleteToken()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    FirebaseMessaging.getInstance().token
                        .addOnCompleteListener { tokenTask ->
                            if (tokenTask.isSuccessful) {
                                val newToken = tokenTask.result
                                Log.d("Firestore", "Nuevo token: $newToken")
                                saveTokenToFirestore(newToken)
                            }
                        }
                }
            }

    }

    private fun saveTokenToFirestore(token: String?) {
        val db = FirebaseFirestore.getInstance()
        val userTokenMap = hashMapOf("token" to token)

        try {
            db.collection("device_tokens")
                .add(userTokenMap)
                .addOnSuccessListener { documentReference ->
                   // Log.d("Firestore", "Token registrado con ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                  //  Log.w("Firestore", "Error añadiendo token", e)
                    // Opcional: Registrar el error en Crashlytics para análisis posterior
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
        } catch (e: Exception) {
            Log.e("Firestore", "Error inesperado al guardar el token Guardado por la app", e)
            // Opcional: Registrar el error inesperado en Crashlytics
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }



    private fun crearTopic() {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("general")
                .addOnCompleteListener { task: Task<Void?> ->
                    val msg = if (task.isSuccessful) {
                        "Suscripción exitosa"
                    } else {
                        "Suscripción fallida"
                    }
                  //  Log.d("FirebaseTopic", "Creación de Topic: $msg")
                }
                .addOnFailureListener { e ->
                    // Registra el error específico de suscripción
                 //   Log.e("FirebaseTopic", "Error al suscribirse al Topic", e)
                    // Opcional: Puedes enviar el error a Crashlytics o notificar al usuario
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
        } catch (e: Exception) {
            // Maneja errores que puedan surgir fuera del proceso de suscripción
            Log.e("FirebaseTopic", "Error inesperado al crear Topic Guardado por la app", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }


    // Para crear Topic de Prueba de Notificaciones*********************
/*
    private fun crearTopicPrueba() {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("prueba")
                .addOnCompleteListener { task: Task<Void?> ->
                    val msg = if (task.isSuccessful) {
                        "Suscripción exitosa Topic Prueba"
                    } else {
                        "Suscripción fallida Topic Prueba"
                    }
                    Log.d("FirebaseTopic", "Creación de Topic: $msg ")
                }
                .addOnFailureListener { e ->
                    // Registra el error específico de suscripción
                    Log.e("FirebaseTopic", "Error al suscribirse al Topic de Prueba", e)
                    // Opcional: Puedes enviar el error a Crashlytics o notificar al usuario
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
        } catch (e: Exception) {
            // Maneja errores que puedan surgir fuera del proceso de suscripción
            Log.e("FirebaseTopic", "Error inesperado al crear Topic Guardado por la app", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }
*/
    //******** final token de Prueba Notificaciones

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

    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
        // Ajustamos el anuncio del programa si está disponible para exigir que otras clases solo interactúen con MiAplicación
        // clase.
        appOpenAdManager.showAdIfAvailable(activity, onShowAdCompleteListener)
    }

    fun loadAd(activity: Context) {
        // We wrap the loadAd to enforce that other classes only interact with MyApplication
        // class.
        appOpenAdManager.loadAd(activity)
    }

    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }

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
                     //   Log.d(LOG_TAG, "Cargado!!")
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