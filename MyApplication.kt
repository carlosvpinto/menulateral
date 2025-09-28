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
//private const val AD_UNIT_ID: String ="ca-app-pub-3940256099942544/9257395921" //Para desarrollo y Pruebas
//private const val AD_UNIT_ID: String  = "ca-app-pub-5303101028880067/8981364608"
private const val AD_UNIT_ID: String  = "ca-app-pub-3265312813580307/7449206999" // Admob Dolarmexico 2
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

    // --- FUNCIÓN ACTUALIZADA PARA MOSTRAR EL DIÁLOGO DE MATERIAL 3 ---
    private fun showPremiumDialog(activity: Activity) {
        activity.runOnUiThread {
            MaterialAlertDialogBuilder(activity)
                // 1. Usa el nuevo string para el título, que ahora es el gancho principal.
                .setTitle(R.string.premium_dialog_title)

                // 2. Usa el nuevo string para el mensaje, que da más detalles.
                .setMessage(R.string.premium_dialog_message_v3)

                // 3. El botón positivo ahora es más claro sobre su acción.
                .setPositiveButton(R.string.premium_dialog_positive_button) { dialog, _ ->
                    val intent = Intent(activity, PlanesPagoActivity::class.java)
                    activity.startActivity(intent)
                    dialog.dismiss()
                }

                // 4. El botón negativo no cambia.
                .setNegativeButton(R.string.premium_dialog_negative_button) { dialog, _ ->
                    dialog.dismiss()
                }

                .setIcon(R.drawable.premiun) // El ícono se mantiene

                .show()
        }
    }

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

        // La carga inicial y proactiva del anuncio
        appOpenAdManager.loadAd(this)
        // Forzamos la inicialización de la base de datos aquí para que el callback se ejecute al inicio.
        // Esto es una buena práctica.
        database.openHelper.writableDatabase
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
            Log.e(
                "Firestore",
                " Crashlity Error initializing Firebase Crashlytics mandado por la app",
                e
            )
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

        // --- ¡LÓGICA DE TIEMPO ACTUALIZADA! ---
        // Verificamos si ha pasado el tiempo suficiente desde que la app se fue a segundo plano.
        val timeInBackground = System.currentTimeMillis() - appBackgroundTime
        if (appBackgroundTime > 0 && timeInBackground >= MIN_BACKGROUND_TIME_MS) {
            Log.d(LOG_TAG, "App ha vuelto a primer plano después de ${timeInBackground / 60000} minutos. Intentando mostrar anuncio.")
            currentActivity?.let { appOpenAdManager.showAdIfAvailableOnResume(it) }
        } else {
            Log.d(LOG_TAG, "App ha vuelto a primer plano, pero no ha pasado el tiempo mínimo. No se mostrará anuncio.")
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
                context, AD_UNIT_ID, request,
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
                    }
                }
            )
        }

        private fun isAdAvailable(): Boolean = appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)

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

                        //llama a la funcion para mostrar el dialogo
                        if (premiumDialogManager.shouldShowPremiumDialog()) { showPremiumDialog(activity) }
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