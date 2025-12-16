package com.carlosv.dolaraldia

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.carlosv.dolaraldia.ui.pago.PlanesPagoActivity
import com.carlosv.dolaraldia.utils.Constants.AD_UNIT_ID_OPEN
import com.carlosv.dolaraldia.utils.premiun.PremiumDialogManager
import com.carlosv.dolaraldia.utils.roomDB.NotificationEntity
import com.carlosv.dolaraldia.utils.roomDB.AppDatabase
import com.carlosv.dolaraldia.utils.roomDB.NotificationsRepository
import com.carlosv.menulateral.R
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.tasks.Task
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

//private const val AD_UNIT_ID: String  = "ca-app-pub-5303101028880067/8981364608"

/** Variable para asegurar que el anuncio se muestra solo una vez */
private var hasAdBeenShown = false

// para que se muestre un anuncio al volver. 20 minutos = 20 * 60 * 1000 = 1,200,000 milisegundos.
private const val MIN_BACKGROUND_TIME_MS = 1_200_000L


/** Application class that initializes, loads and show ads when activities change states. */

class MyApplication :
    Application(), Application.ActivityLifecycleCallbacks, LifecycleObserver {

    private lateinit var appOpenAdManager: AppOpenAdManager
    private var currentActivity: Activity? = null
    private var isAppInForeground = false
    private var adLoadCallback: OnShowAdCompleteListener? = null



    // Almacena la marca de tiempo de cuándo la app pasó a segundo plano.
    private var appBackgroundTime: Long = 0L

    // --- INICIO DE LA SECCIÓN PARA LA BASE DE DATOS DE NOTIFICACIONES ---

    private val database by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java, "dolar_al_dia_database"
        )
            // CAMBIO 1: Añadimos nuestro callback aquí
            .addCallback(databaseCallback)
            .build()
    }

    private val premiumDialogManager: PremiumDialogManager by lazy {
        PremiumDialogManager(this)
    }

    val repository by lazy {
        NotificationsRepository(database.notificationDao())
    }
// Eliminado por politicas de google**************************************************
//    private fun showPremiumDialog(activity: Activity) {
//
//        // 1. Verificamos si la actividad está finalizando o ya ha sido destruida.
//        //    'isFinishing' y 'isDestroyed' son las comprobaciones de seguridad más importantes.
//        if (activity.isFinishing || activity.isDestroyed) {
//            Log.e(LOG_TAG, "showPremiumDialog abortado: La actividad ya no es válida.")
//            return // Detenemos la función si la actividad no es válida.
//        }
//
//        // 2. Mantenemos el runOnUiThread para asegurar que se ejecute en el hilo principal.
//        activity.runOnUiThread {
//            // Re-verificamos por si acaso el estado cambió en el último instante.
//            if (!activity.isFinishing && !activity.isDestroyed) {
//                MaterialAlertDialogBuilder(activity)
//                    .setTitle(R.string.premium_dialog_title)
//                    .setMessage(R.string.premium_dialog_message_v3)
//                    .setPositiveButton(R.string.premium_dialog_positive_button) { dialog, _ ->
//                        val intent = Intent(activity, PlanesPagoActivity::class.java)
//                        activity.startActivity(intent)
//                        dialog.dismiss()
//                    }
//                    .setNegativeButton(R.string.premium_dialog_negative_button) { dialog, _ ->
//                        dialog.dismiss()
//                    }
//                    .setIcon(R.drawable.premiun)
//                    .show()
//            }
//        }
//    }

    //Creamos el objeto Callback Para las Notificaciones
    private val databaseCallback = object : RoomDatabase.Callback() {
        /**
         * Se llama una única vez, cuando la base de datos es creada por primera vez.
         */
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            Log.d(
                "RoomDatabase",
                "Base de datos creada por primera vez. Insertando notificación de bienvenida."
            )
            // Usamos una corutina para insertar los datos en un hilo de fondo.
            CoroutineScope(Dispatchers.IO).launch {
                insertWelcomeNotification()
            }
        }
    }

    //Creamos una función para la inserción
    private suspend fun insertWelcomeNotification() {
        // Creamos la notificación de bienvenida
        val welcomeNotification = NotificationEntity(
            title = "¡Bienvenido a Dólar al Día!",
            body = "Gracias por instalar la aplicación. Aquí recibirás todas las actualizaciones importantes sobre las tasas de cambio.",
            timestamp = System.currentTimeMillis() // La hora actual
        )
        // Obtenemos el DAO de la base de datos e insertamos
        database.notificationDao().insert(welcomeNotification)
    }

    // --- FIN DE LA SECCIÓN DE NOTIFICACIONES ---

    private val PREFS_NAME = "MyAppPrefs"
    private val TOPIC_SUBSCRIBED_KEY = "isSubscribedToTopic"

    private val TOKEN_KEY = "fcmToken"


    override fun onCreate() {
        super.onCreate()

        AppPreferences.init(this)
        registerActivityLifecycleCallbacks(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appOpenAdManager = AppOpenAdManager()

        // ¡IMPORTANTE! Inicializar MobileAds antes de cargar
        com.google.android.gms.ads.MobileAds.initialize(this) { initializationStatus ->
            Log.d(LOG_TAG, "✅ MobileAds inicializado. Estado: $initializationStatus")
            // Apenas el SDK responde, cargamos el anuncio.
            appOpenAdManager.loadAd(this)
        }
        database.openHelper.writableDatabase
        //countDeviceTokens()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(LOG_TAG, "⚡ Iniciando carga de recursos pesados en segundo plano...")

                // A. BASE DE DATOS (ROOM)
                // Forzamos la apertura aquí para que el callback de bienvenida se ejecute
                // sin bloquear la interfaz de usuario.
                database.openHelper.writableDatabase

                // B. CRASHLYTICS
                if (BuildConfig.DEBUG) {
                    FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
                } else {
                    FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
                }

                // C. FIREBASE TOKENS Y NOTIFICACIONES
                // Llamamos a tus funciones de token (que ya usan corrutinas internamente, pero es seguro llamarlas aquí)
                getOrRequestToken()

                // Verificamos el token actual (Solo para log)
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("FCM_TOKEN", "Token actual: ${task.result}")
                    }
                }

                Log.d(LOG_TAG, "✅ Carga de recursos pesados finalizada.")

            } catch (e: Exception) {
                Log.e(LOG_TAG, "❌ Error en inicialización en segundo plano", e)
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }

        //*******************************************************


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


    private fun borrraTokenYcrear() {
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



    @OnLifecycleEvent(Lifecycle.Event.ON_START)

    fun onMoveToForeground() {
        isAppInForeground = true

        val timeInBackground = System.currentTimeMillis() - appBackgroundTime

        // Convertimos a segundos para el Log
        val seconds = timeInBackground / 1000

        if (appBackgroundTime > 0 && timeInBackground >= MIN_BACKGROUND_TIME_MS) {
            Log.d(LOG_TAG, "ÉXITO: Pasaron $seconds segundos (Mínimo requerido: ${MIN_BACKGROUND_TIME_MS/1000}). Mostrando anuncio.")
            currentActivity?.let { appOpenAdManager.showAdIfAvailableOnResume(it) }
        } else {
            Log.d(LOG_TAG, "IGNORADO: Solo pasaron $seconds segundos (Mínimo requerido: ${MIN_BACKGROUND_TIME_MS/1000}) o es el primer inicio.")
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onMoveToBackground() {
        isAppInForeground = false
        // --- ¡AQUÍ GUARDAMOS LA HORA! ---
        // Guardamos la marca de tiempo actual cuando la app pasa a segundo plano.
        appBackgroundTime = System.currentTimeMillis()
        Log.d(LOG_TAG, "App movida a segundo plano.")
    }


    // --- MÉTODOS DE CICLO DE VIDA DE ACTIVIDADES ---
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) { if (!appOpenAdManager.isShowingAd) { currentActivity = activity } }
    override fun onActivityResumed(activity: Activity) { if (!appOpenAdManager.isShowingAd) { currentActivity = activity } }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    // --- INTERFAZ Y FUNCIÓN PÚBLICA PARA EL ANUNCIO INICIAL ---
    interface OnShowAdCompleteListener {
        fun onShowAdComplete()

        fun onAdLoaded()
    }


    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
        this.adLoadCallback = onShowAdCompleteListener
        appOpenAdManager.showAdIfAvailable(activity, onShowAdCompleteListener)
    }


    // 1. Agrega este método público en el cuerpo de MyApplication para que la SplashActivity pueda consultar
    fun isAdAvailable(): Boolean {
        return appOpenAdManager.isAdAvailable()
    }

    //123456789
    // --- CLASE INTERNA AppOpenAdManager (CON LÓGICA ANTI-CICLOS) ---
    private inner class AppOpenAdManager {
        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        var isShowingAd = false
        private var loadTime: Long = 0

        fun loadAd(context: Context) {
            if (isLoadingAd || isAdAvailable()) { return }
            isLoadingAd = true
            Log.d(LOG_TAG, "Iniciando carga del anuncio...")
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                context, AD_UNIT_ID_OPEN, request,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        isLoadingAd = false
                        loadTime = Date().time
                        Log.d(LOG_TAG, "¡Anuncio cargado y listo!")
                        // Notificamos al callback que el anuncio se cargó.
                        adLoadCallback?.onAdLoaded()
                    }
                    override fun onAdFailedToLoad(loadError: LoadAdError) {
                        isLoadingAd = false; appOpenAd = null
                        Log.e(LOG_TAG, "Fallo al cargar el anuncio: ${loadError.message}")
                        isLoadingAd = false
                        appOpenAd = null

                        // --- AGREGA ESTOS LOGS DETALLADOS ---
                        Log.e(LOG_TAG, "❌ ERROR CRÍTICO AL CARGAR ANUNCIO:")
                        Log.e(LOG_TAG, "👉 Código de Error: ${loadError.code}")
                        Log.e(LOG_TAG, "👉 Mensaje: ${loadError.message}")
                        Log.e(LOG_TAG, "👉 Dominio: ${loadError.domain}")
                        Log.e(LOG_TAG, "👉 Causa: ${loadError.cause}")
                    }
                }
            )
        }

        // private' a 'fun' (público dentro del paquete o clase)
        fun isAdAvailable(): Boolean = appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)


        private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
            // Calcula la diferencia en milisegundos entre la hora actual y la hora de carga.
            val dateDifference: Long = Date().time - loadTime

            // Define cuántos milisegundos hay en una hora.
            val numMilliSecondsPerHour: Long = 3600000

            // Devuelve 'true' si la diferencia es menor que el umbral de horas permitido.
            return dateDifference < (numMilliSecondsPerHour * numHours)
        }

        // Función para mostrar el anuncio al VOLVER a la app.
        fun showAdIfAvailableOnResume(activity: Activity) {
            showAdIfAvailable(activity, object: OnShowAdCompleteListener {
                override fun onShowAdComplete() {}
                override fun onAdLoaded() {}
            })
        }

        fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
            if (AppPreferences.isUserPremiumActive() || isShowingAd) {
                if (AppPreferences.isUserPremiumActive()) onShowAdCompleteListener.onShowAdComplete()
                Log.d(LOG_TAG, "Anuncio PREMIUN NO SE MUESTRA.... ")
                return
            }
            if (isAdAvailable()) {
                Log.d(LOG_TAG, "Anuncio disponible. Mostrando...")
                appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        appOpenAd = null; isShowingAd = false
                        if (activity !is SplashActivity) {
                            //ELIMINADO POR POLITICAS DE GOOGLE
//                            if (premiumDialogManager.shouldShowPremiumDialog()) {
//                                showPremiumDialog(activity)
//                            }
                        }


                        onShowAdCompleteListener.onShowAdComplete()
                        loadAd(activity)
                    }
                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        appOpenAd = null; isShowingAd = false
                        Log.e(LOG_TAG, "Fallo al mostrar el anuncio: ${adError.message}")
                        onShowAdCompleteListener.onShowAdComplete()
                        loadAd(activity)
                    }
                    override fun onAdShowedFullScreenContent() {
                        isShowingAd = true
                        Log.d(LOG_TAG, "Anuncio mostrado.")
                    }
                }
                appOpenAd?.show(activity)
            } else {
                Log.d(LOG_TAG, "Anuncio no disponible. Esperando a que onAdLoaded lo dispare.")
                loadAd(activity)
            }
        }
    }
}