package com.carlosv.dolaraldia

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap

import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem

import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.carlosv.menulateral.R

import com.carlosv.menulateral.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.*

import android.Manifest

import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Canvas
import android.net.Uri
import android.os.Environment

import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.ToggleButton

import androidx.annotation.RequiresApi


import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.carlosv.dolaraldia.model.datosPMovil.DatosPMovilModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.common.reflect.TypeToken
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

import com.google.gson.Gson
import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicBoolean



class MainActivity : AppCompatActivity() {

    private lateinit var appOpenAdManager: AppOpenAdManager
    private lateinit var navController2: NavController

    val miAplicacion = MyApplication()
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private val LOG_TAG: String = "AppOpenAdManager"
    private val TAG: String = "PAYPALPRICE"

    private val AD_UNIT_ID: String? = "ca-app-pub-3940256099942544/9257395921"

    private var snackbar: Snackbar? = null

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val PERMISSION_REQUEST_CODE = 123
    var filePath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        MobileAds.initialize(this) {}
        appOpenAdManager = AppOpenAdManager()
        setSupportActionBar(binding.appBarMain.toolbar)


        //adOpen*********************
        // initializeMobileAdsSdk()
        // Cargar el anuncio al crear la actividad
        // (application as MyApplication).loadAd(this)

        //***********************
        // Obtén el NavHostFragment y el NavController
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        navController2 = navHostFragment.navController
        //**********************

        //verificaciondeSuscripcion()// Verifica la suscripcion Con Work en segundo plano
        verificasiLeyoMsj()
        versionUltima()
        movilidadPantalla()

        binding.navView

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Pasando cada ID de menú como un conjunto de ID porque cada
        // el menú debe considerarse como destino de nivel superior.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_monedas,
                R.id.nav_bancos,
                R.id.nav_acerca,
                R.id.nav_Euros,
                R.id.nav_history

            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

    }

    inner class AppOpenAdManager {
        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        var isShowingAd = false

        fun loadAd(context: Context) {
            // Do not load ad if there is an unused ad or one is already loading.
            if (isLoadingAd || isAdAvailable()) {
                return
            }

            isLoadingAd = true
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                context, AD_UNIT_ID!!, request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                object : AppOpenAd.AppOpenAdLoadCallback() {

                    override fun onAdLoaded(ad: AppOpenAd) {
                        // Called when an app open ad has loaded.
                        Log.d(LOG_TAG, "Ad was loaded.")
                        appOpenAd = ad
                        isLoadingAd = false
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        // Called when an app open ad has failed to load.
                        Log.d(LOG_TAG, loadAdError.message)
                        isLoadingAd = false;
                    }
                })
        }

        private fun isAdAvailable(): Boolean {
            return appOpenAd != null
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflar el menú; esto agrega elementos a la barra de acciones si está presente.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
//            R.id.action_premiun -> {
//                navController2.navigate(R.id.nav_pago)
//            }

            R.id.action_compartir -> {
                showCustomSnackbarPM()
            }

            R.id.action_personal -> {
                navController2.navigate(R.id.nav_Personal)
            }

            R.id.action_salir -> {
                salirdelApp()
            }


        }
        return super.onOptionsItemSelected(item)
    }
    private fun movilidadPantalla(){
        if (isChromeOSDevice()) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

    }
    private fun isChromeOSDevice(): Boolean {
        return packageManager.hasSystemFeature("org.chromium.arc.device_management")
    }



    //Verifica se es la Ultima version Autorizada por Config
    private fun versionUltima() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        // Duración de la caché en segundos (12 horas en este caso)
        val cacheExpiration: Long = 12 * 60 * 60
        // Forzar la obtención de datos desde el servidor, ignorando la caché
        remoteConfig.fetch(cacheExpiration)  // 0 segundos de duración del caché, siempre obtiene nuevos datos
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Activar los valores obtenidos
                    remoteConfig.activate().addOnCompleteListener { activateTask ->
                        if (activateTask.isSuccessful) {
                            // Ahora puedes obtener y utilizar los valores de configuración remota
                            val requiredVersionMinima =
                                remoteConfig.getLong("version_min_dolar_al_dia")

                            // Obtener PackageInfo y usar PackageInfoCompat para obtener versionCode
                            val packageInfo = packageManager.getPackageInfo(packageName, 0)
                            val versionCodeActual =
                                PackageInfoCompat.getLongVersionCode(packageInfo)

                            if (versionCodeActual < requiredVersionMinima) {
                                // Mostrar un diálogo de actualización y redirigir a la Play Store.
                                llamaElMsjActualizacion()
                            }
                        } else {
                          //  Log.e("totalDolarConfig", "Error al activar la configuración remota")
                        }
                    }
                } else {
                  //  Log.e("totalDolarConfig", "Error al obtener la configuración remota")
                }
            }
    }


    private fun initializeMobileAdsSdk() {
        val activado = isSubscriptionActive(this)
        //Verifica si la suscripcion esta activa CAMBIALLLOOOO PRODUCCION
        if (!isSubscriptionActive(this)) {
           // Log.d(TAG, "initializeMobileAdsSdk: isSubscriptionActive $activado ENTRO A PUBLICIDAD")
            if (isMobileAdsInitializeCalled.getAndSet(true)) {
                return
            }

            // Initialize the Mobile Ads SDK.
            MobileAds.initialize(this) {}

            // Load an ad.
            (application as MyApplication).loadAd(this)

        }

    }

    private fun salirdelApp() {

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Salir")
        builder.setMessage("Desea salir de la aplicacion?")
        builder.setPositiveButton("Salir", DialogInterface.OnClickListener { dialog, which ->
            finishAffinity()
        })
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun requestPermissionsIfNecessary() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkPermission()) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val writeExternalStoragePermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val readExternalStoragePermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )

            writeExternalStoragePermission == PackageManager.PERMISSION_GRANTED &&
                    readExternalStoragePermission == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (checkPermission()) {
                // If permissions are granted, take the screenshot
                showCustomSnackbarPM()
                // captureScreen(false, null)
            } else {
                // If permissions are denied, show a message to the user
                Toast.makeText(
                    this,
                    "Los permisos son necesarios para realizar la captura de pantalla",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun captureScreen(enviarDatosPM: Boolean, datosPMovilModel: DatosPMovilModel?) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "screenshot_$timestamp.png"
            val picturesDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above
                getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            } else {
                // For older versions
                captureScreenVersionOld(enviarDatosPM, datosPMovilModel, fileName)
                return
                //Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            }
            filePath = "$picturesDir/$fileName"

            // Create a File for the screenshot
            val file = File(filePath)


            // Toma la captura de pantalla
            val rootView = window.decorView.rootView
            val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            rootView.draw(canvas)

            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
            stream.flush()
            stream.close()

            // Show a message indicating the file location
            Toast.makeText(
                this,
                "Enviando...: ",
                Toast.LENGTH_SHORT
            ).show()

            //veridica si esta en Fragmento para no hacer el Capture
            val nombreFragmentAct = getCurrentFragmentTag()
            if (nombreFragmentAct == "Pago Movil") {

                // Realiza la lógica específica del fragmento actual
                shareText(crearTextoCapture(enviarDatosPM, datosPMovilModel))
            } else {
                // Call the share function
                shareImageWithText(filePath, crearTextoCapture(enviarDatosPM, datosPMovilModel))
            }

        } catch (e: IOException) {
            e.printStackTrace()
            Log.d(TAG, "captureScreen: $e")
        }
    }


    private fun captureScreenVersionOld(enviarDatosPM: Boolean, datosPMovilModel: DatosPMovilModel?,fileName: String) {
        try {

            // Toma la captura de pantalla
            val rootView = window.decorView.rootView
            val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            rootView.draw(canvas)


            // Para Android 9 y versiones anteriores (sin permisos de almacenamiento)
                val picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) // Directorio privado
                filePath = "$picturesDir/$fileName"
                val file = File(filePath)
                val stream = FileOutputStream(file)
                stream.use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, it)
                }


            // Muestra un mensaje indicando la ubicación del archivo
            Toast.makeText(this, "Enviando...: ", Toast.LENGTH_SHORT).show()

            // Verifica si está en un fragmento para no hacer el capture
            val nombreFragmentAct = getCurrentFragmentTag()
            if (nombreFragmentAct == "Pago Movil") {
                // Realiza la lógica específica del fragmento actual
                shareText(crearTextoCapture(enviarDatosPM, datosPMovilModel))
            } else {
                // Llama a la función para compartir la imagen y el texto
                shareImageWithText(filePath, crearTextoCapture(enviarDatosPM, datosPMovilModel))
            }

        } catch (e: IOException) {
            e.printStackTrace()
            Log.d(TAG, "captureScreen: $e")
        }
    }





    //Solo Envia Texto
    private fun shareText(shareText: String) {
        // Crear un intent para compartir
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)

        // Iniciar la actividad de compartir
        startActivity(Intent.createChooser(shareIntent, "Compartir texto"))
    }

    private fun llamaElMsjActualizacion() {
        val rootView = findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(rootView, "", Snackbar.LENGTH_INDEFINITE)

        // Inflar el diseño personalizado
        val customView =
            LayoutInflater.from(this).inflate(R.layout.custom_toast_actualizacion, null)

        // Configurar el ícono y el texto
        val snackbarTextView: TextView = customView.findViewById(R.id.txtContenido)
        snackbarTextView.textSize = 16f
        snackbarTextView.text = obtenerTexoString(this)

        // Configurar el botón de Envio Pago movil
        val btnOkActualizar: Button = customView.findViewById(R.id.btnOkActualizar)
        btnOkActualizar.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.carlosv.menulateral")
            )
            startActivity(intent)
            finish() // O bloquea el acceso a la aplicación
            snackbar.dismiss()
        }

        // Obtener el ViewGroup del Snackbar para añadir la vista personalizada
        val snackbarView = snackbar.view as ViewGroup
        snackbarView.setBackgroundResource(R.drawable.snackbar_background) // Configurar el fondo personalizado

        // Agregar el diseño personalizado al Snackbar
        snackbarView.addView(customView, 0)

        // Mostrar el Snackbar
        snackbar.show()

        // Ajustar la posición del Snackbar al centro de la pantalla
        val params = snackbar.view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP // or Gravity.CENTER_HORIZONTAL
        val marginTop =
            250 // Cambia este valor según la separación que desees desde la parte superior
        params.setMargins(0, marginTop, 0, 0)
        snackbar.view.layoutParams = params
    }


    // LLma alm uso del pago Movil
    private fun llamaElMsjUsoPagoMovil() {
        val rootView = findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(rootView, "", Snackbar.LENGTH_INDEFINITE)

        // Inflar el diseño personalizado
        val customView = LayoutInflater.from(this).inflate(R.layout.custom_toast_informacion, null)

        // Configurar el ícono y el texto
        val snackbarTextView: TextView = customView.findViewById(R.id.txtContenido)
        snackbarTextView.textSize = 16f
        snackbarTextView.text = getCustomSpannableString(this)

        // Configurar el botón de cierre
        val btnRecordarDespues: Button = customView.findViewById(R.id.btnRecordarDespues)
        btnRecordarDespues.setOnClickListener {
            snackbar.dismiss()
            initializeMobileAdsSdk()
        }

        // Configurar el botón de Envio Pago movil
        val btnOkEntendi: Button = customView.findViewById(R.id.btnOkEntendi)
        btnOkEntendi.setOnClickListener {
            // Guardar que el usuario ha leído el mensaje
            saveMessageReadState(this, true)
            snackbar.dismiss()
            initializeMobileAdsSdk()
        }

        // Obtener el ViewGroup del Snackbar y agregar el diseño personalizado
        val snackbarView = snackbar.view as ViewGroup
        snackbarView.setBackgroundResource(R.drawable.snackbar_background) // Configurar el fondo personalizado
        snackbarView.addView(customView, 0)

        // Mostrar el Snackbar
        snackbar.show()

        // Ajustar la posición del Snackbar al centro de la pantalla
        val params = snackbar.view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP // or Gravity.CENTER_HORIZONTAL
        val marginTop = 250 // Cambia este valor según la separación que desees desde la parte superior
        params.setMargins(0, marginTop, 0, 0)
        snackbar.view.layoutParams = params
    }


    private fun verificasiLeyoMsj() {

        // Para verificar si el usuario ha leído el mensaje
        val hasRead = hasUserReadMessage(this)
        if (hasRead) {
            // El usuario ya ha leído el mensaje
            initializeMobileAdsSdk()

        } else {
            // El usuario no ha leído el mensaje
            llamaElMsjUsoPagoMovil()

        }
    }

    fun getCustomSpannableString(context: Context): SpannableString {
        // El texto que deseas mostrar
        val text =
            "Ahora Presionando el Siguiente Icono:  Puede Guardar tus numeros de pagos Movil para enviarlo con mayor rapidez a la hora de solicitar algun pago"

        // Crear un SpannableStringBuilder con el texto
        val spannableStringBuilder = SpannableStringBuilder(text)


        // Crear el Drawable para la imagen
        val drawable = ContextCompat.getDrawable(context, R.drawable.enviar_2_png)!!

        // Convertir 24dp a píxeles
        val widthInDp = 35
        val heightInDp = 35
        val widthInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            widthInDp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
        val heightInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            heightInDp.toFloat(),
            context.resources.displayMetrics
        ).toInt()

        // Establecer el tamaño del Drawable
        drawable.setBounds(0, 0, widthInPx, heightInPx)

        // Crear un ImageSpan con el Drawable
        val imageSpan = ImageSpan(drawable, ImageSpan.ALIGN_BASELINE)

        // Encuentra la posición donde quieres insertar la imagen
        val position = text.indexOf("Icono:  ") + "Icono: ".length

        // Añadir espacio en el texto donde la imagen será insertada
        spannableStringBuilder.insert(position, " ")

        // Añadir el ImageSpan al SpannableStringBuilder
        spannableStringBuilder.setSpan(
            imageSpan,
            position,
            position + 1,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )

        // Convertir SpannableStringBuilder a SpannableString y devolverlo
        return SpannableString(spannableStringBuilder)
    }

    fun obtenerTexoString(context: Context): SpannableString {
        // El texto que deseas mostrar
        val text =
            "Debes actualizar la aplicacion no perderas ninguna informacion y tardara menos de 20 segundos"

        // Crear un SpannableStringBuilder con el texto
        val spannableStringBuilder = SpannableStringBuilder(text)


        // Crear el Drawable para la imagen
        val drawable = ContextCompat.getDrawable(context, R.drawable.logodolar_al_dia)!!

        // Convertir 24dp a píxeles
        val widthInDp = 35
        val heightInDp = 35
        val widthInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            widthInDp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
        val heightInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            heightInDp.toFloat(),
            context.resources.displayMetrics
        ).toInt()

        // Establecer el tamaño del Drawable
        drawable.setBounds(0, 0, widthInPx, heightInPx)

        // Crear un ImageSpan con el Drawable
        val imageSpan = ImageSpan(drawable, ImageSpan.ALIGN_BASELINE)

        return SpannableString(spannableStringBuilder)
    }


    // Función para guardar el estado de lectura del mensaje
    fun saveMessageReadState(context: Context, hasRead: Boolean) {
        val sharedPreferences = context.getSharedPreferences("LeyoMsj", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("MSJLeido", hasRead)
        editor.apply()
    }

    // Función para verificar el estado de lectura del mensaje
    fun hasUserReadMessage(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences("LeyoMsj", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("MSJLeido", false)
    }

    //********************************************************************


    // Abre el mensaje Snackbar Personalizado para Enviar Datos de Pago Movil
    private fun showCustomSnackbarPM() {
        var botonPrecionado = 0
        val decimalFormat = DecimalFormat("#,##0.00") // Declaración de DecimalFormat
        val rootView = findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(rootView, "", Snackbar.LENGTH_INDEFINITE)

        // Inflar el diseño personalizado
        val customView = LayoutInflater.from(this).inflate(R.layout.custom_toast_pago_movil, null)

        // Configurar el ícono y el texto
        val snackbarTextView: TextView = customView.findViewById(R.id.toast_text)
        snackbarTextView.textSize = 12f

        val chechPagoMovil: CheckBox = customView.findViewById(R.id.checPagomovil)
        val pagoMovilListTrue = obtenerPagoMovilListTrue(this)

        if (pagoMovilListTrue?.seleccionado == null) {
            chechPagoMovil.isChecked = false
            chechPagoMovil.isEnabled = false
            chechPagoMovil.text = getString(R.string.sin_cuenta_seleccionada)
        } else {
            chechPagoMovil.text = pagoMovilListTrue?.nombre
        }

        // Configurar el botón de cierre
        val closeButton: ImageButton = customView.findViewById(R.id.close_button)
        closeButton.setOnClickListener {
            snackbar.dismiss()
            botonPrecionado = 1
        }

        // Configurar el botón de Envio Pago movil
        val enviareButton: ImageButton = customView.findViewById(R.id.btnEnviarPM)
        enviareButton.setOnClickListener {
            botonPrecionado = 0
            snackbar.dismiss()
        }

        // Añadir el callback para escuchar cuando el Snackbar se cierra
        snackbar.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                // Se llama cuando el Snackbar se cierra

                val enviarDatosPM = chechPagoMovil.isChecked
                Log.d(TAG, "onDismissed: snackbar  enviarDatosPM; $enviarDatosPM  botonPrecionado: $botonPrecionado")
                if (botonPrecionado == 0) {
                    captureScreen(enviarDatosPM, pagoMovilListTrue)
                }
            }
        })

        // Obtener el ViewGroup del Snackbar y agregar el diseño personalizado
        val snackbarView = snackbar.view as ViewGroup
        snackbarView.setBackgroundResource(R.drawable.snackbar_background) // Configurar el fondo personalizado
        snackbarView.addView(customView, 0)

        // Mostrar el Snackbar
        snackbar.show()

        // Ajustar la posición del Snackbar al centro de la pantalla
        val params = snackbar.view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP // or Gravity.CENTER_HORIZONTAL
        val marginTop =
            250 // Cambia este valor según la separación que desees desde la parte superior
        params.setMargins(0, marginTop, 0, 0)
        snackbar.view.layoutParams = params
    }


    private fun obtenerPagoMovilList(context: Context): List<DatosPMovilModel> {
        val gson = Gson()
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferencesPMovil", AppCompatActivity.MODE_PRIVATE)

        // Leer la lista existente de pagoMovil desde SharedPreferences
        val pagoMovilJson = sharedPreferences.getString("datosPMovilList", null)
        return if (pagoMovilJson != null) {
            gson.fromJson(pagoMovilJson, object : TypeToken<List<DatosPMovilModel>>() {}.type)
        } else {
            emptyList()
        }
    }

    //Obtiene el pago Movil Activo
    private fun obtenerPagoMovilListTrue(context: Context): DatosPMovilModel? {
        val gson = Gson()
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferencesPMovil", AppCompatActivity.MODE_PRIVATE)

        // Leer la lista existente de pagoMovil desde SharedPreferences
        val pagoMovilJson = sharedPreferences.getString("datosPMovilList", null)
        val pagoMovilList: MutableList<DatosPMovilModel> = if (pagoMovilJson != null) {
            gson.fromJson(
                pagoMovilJson,
                object : TypeToken<MutableList<DatosPMovilModel>>() {}.type
            )
        } else {
            mutableListOf()
        }

        // Buscar y retornar el nombre del elemento que tiene seleccionado igual a true
        for (datosPMovilModel in pagoMovilList) {
            if (datosPMovilModel.seleccionado) {
                Log.d(
                    "obtenerPagoMovilListTrue",
                    "Elemento seleccionado: ${datosPMovilModel.nombre}"
                )
                return datosPMovilModel
            }
        }

        // Retornar null si no se encuentra ningún elemento seleccionado
        return null
    }


    // Comparte imagen contexto************************
    private fun shareImageWithText(imagePath: String, shareText: String) {
        val imageFile = File(imagePath)
        Log.d(TAG, "shareImageWithText: imageFile: $imageFile y imagePath: $imagePath")
        if (imageFile.exists()) {
            try {
                // Guarda la imagen en el directorio de caché
                val cachePath = File(applicationContext.cacheDir, "images").apply { mkdirs() }
                val tempFile = File(cachePath, "shared_image.jpg").apply {
                    // Copia el contenido del archivo original al archivo temporal
                    imageFile.copyTo(this, overwrite = true)
                }

                // Obtener la Uri segura utilizando FileProvider
                val uri = FileProvider.getUriForFile(
                    this,
                    "com.carlosv.menulateral.fileprovider",  // Reemplaza con el nombre de tu paquete
                    tempFile
                )

                // Crear un intent para compartir
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                // Iniciar la actividad de compartir
                startActivity(Intent.createChooser(shareIntent, "Compartir imagen"))

            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                Log.d(TAG, "IllegalArgumentException error: $e")
                Toast.makeText(
                    this,
                    "No se pudo compartir la imagen: Ruta no encontrada",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: IOException) {
                e.printStackTrace()
                Log.d(TAG, "IOException error: $e")
                Toast.makeText(this, "No se pudo compartir la imagen", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d(TAG, "shareImageWithText:La imagen no existe: imageFile.exists() ${imageFile.exists()} ")
            Toast.makeText(this, "La imagen no existe en la ruta especificada", Toast.LENGTH_SHORT)
                .show()
        }
    }


    override fun onRestart() {
        super.onRestart()
        //initializeMobileAdsSdk()
    }


    @Throws(IOException::class)
    private fun createTempImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "temp_image_$timeStamp"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    @Throws(IOException::class)
    private fun copyFile(sourceFile: File, destFile: File) {
        val source = sourceFile.inputStream()
        val destination = FileOutputStream(destFile)

        source.use { input ->
            destination.use { output ->
                input.copyTo(output)
            }
        }
    }

    //Crea el Texto que va a ir junto con el Capture
    private fun crearTextoCapture(
        enviarDatosPM: Boolean,
        pagoMovilListTrue: DatosPMovilModel?,
    ): String {
        var textoCapture = ""
        var inputTextoBs = ""
        var inputTextoDolla = ""
        var bcv = ""
        var paralelo = ""
        var promedio = ""
        var tasa = ""
        val linkCorto = "https://bit.ly/dolaraldia"


        //**************************
        val nombreFragmentAct = getCurrentFragmentTag()

        //***********************************************
        val fragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
        val botonParalelo = fragment?.view?.findViewById<ToggleButton>(R.id.btnEuroP)
        val botonPromedio = fragment?.view?.findViewById<ToggleButton>(R.id.btnPromedio)
        val botonBcv = fragment?.view?.findViewById<ToggleButton>(R.id.btnBcv)

        val paralelo2 = botonParalelo?.textOn.toString()
        val  bcv2 = botonBcv?.textOn.toString()
        val promedio2= botonPromedio?.textOn.toString()

        if (fragment != null && fragment.isAdded) {
            if (nombreFragmentAct == "Dolar al Dia") {
                tasa = verificarTasa(fragment)
                val editTextInFragmentBs =
                    fragment.view?.findViewById<EditText>(R.id.inputBolivares)
                val editTextInFragmentDolar =
                    fragment.view?.findViewById<EditText>(R.id.inputDolares)
                inputTextoBs = editTextInFragmentBs?.text.toString()
                inputTextoDolla = editTextInFragmentDolar?.text.toString()
                if (editTextInFragmentBs != null && !inputTextoBs.isNullOrEmpty()) {

                    //Verifica se el Usuario quiere enviar Datos de pago Movil
                    if (enviarDatosPM) {

                        textoCapture =
                            "-Tasa: $tasa \n -Monto en Dolares: $inputTextoDolla \n -Monto Bs: $inputTextoBs \n \n -Pago Movil:\n -Tlf: ${pagoMovilListTrue?.tlf} \n -${
                                prefijo(pagoMovilListTrue?.cedula)
                            } ${pagoMovilListTrue?.cedula}  \n -Banco: ${pagoMovilListTrue?.banco}\n \n -Descarga la App \n $linkCorto"
                    } else {
                        textoCapture =
                            "-Tasa: $tasa \n -Monto en Dolares: $inputTextoDolla \n -Monto Bs: $inputTextoBs \n \n -Descarga la App $linkCorto"
                    }


                } else {

                    if (enviarDatosPM) {


                        textoCapture =
                            "-Dolar Bcv: $bcv2 \n  \n -Pago Movil:\n -Tlf: ${pagoMovilListTrue?.tlf} \n -${
                                prefijo(pagoMovilListTrue?.cedula)
                            } ${pagoMovilListTrue?.cedula}  \n -Banco: ${pagoMovilListTrue?.banco}\n \n -Descarga la App \n $linkCorto"
                    } else {
                        textoCapture =
                            "-Dolar Bcv: $bcv2 \n  \n -Descarga la App \n $linkCorto"
                    }

                }

            }
        }
        if (nombreFragmentAct == "Otras Paginas") {

            if (enviarDatosPM) {
                textoCapture =
                    "-Dolar Bcv: ${bcv2} \n  \n -Descarga la App \n $linkCorto"
            } else {
                textoCapture = "Precio del Dolar en Paginas Web\n \n -Descarga la App \n $linkCorto"
            }
        }
        if (nombreFragmentAct == "Precio en Bancos") {
            textoCapture =
                "Precio de venta del dolar en Bancos Venezolanos \n -Descarga la App \n $linkCorto"
        }
        if (nombreFragmentAct == "Acerca...") {
            textoCapture = "Acerca de la Aplicacion la App \n -Descarga la App \n" +
                    " $linkCorto"
        }

        if (nombreFragmentAct == "Precio del Euro") {
            val btnTextInFragmentEuro = fragment?.view?.findViewById<Button>(R.id.btnEuro)
            val TextBsFragmentEuro =
                fragment?.view?.findViewById<TextInputEditText>(R.id.inputBolivares)
            val TextEurosFragmentEuro =
                fragment?.view?.findViewById<TextInputEditText>(R.id.inputEuros)
            val euro = btnTextInFragmentEuro?.text
            val totalBs = TextBsFragmentEuro?.text
            val totalEuro = TextEurosFragmentEuro?.text

            textoCapture =
                "Precio del Euro: $euro total en Bs:$totalBs Total en Euro: $totalEuro \n -Descarga la App \n $linkCorto"
        }

        if (nombreFragmentAct == "Pago Movil") {
            //Verifica se el Usuario quiere enviar Datos
            if (enviarDatosPM) {

                textoCapture =
                    "-Pago Movil:\n -Tlf: ${pagoMovilListTrue?.tlf} \n -${prefijo(pagoMovilListTrue?.cedula)} ${pagoMovilListTrue?.cedula}  \n -Banco: ${pagoMovilListTrue?.banco}\n \n -Descarga la App \n $linkCorto"
            } else {
                textoCapture = "-Descarga la App $linkCorto"
            }

        }
        if (nombreFragmentAct == "Historia del Dolar") {
            //Verifica se el Usuario quiere enviar Datos
            if (enviarDatosPM) {

                textoCapture =
                    "-Pago Movil:\n -Tlf: ${pagoMovilListTrue?.tlf} \n -${prefijo(pagoMovilListTrue?.cedula)} ${pagoMovilListTrue?.cedula}  \n -Banco: ${pagoMovilListTrue?.banco}\n \n -Descarga la App \n $linkCorto"
            } else {
                textoCapture = "-Descarga la App $linkCorto"
            }

        }
        return textoCapture
    }

    private fun verificarTasa(fragment: Fragment): String {
        var tasa = ""
        val botonBcv = fragment.view?.findViewById<ToggleButton>(R.id.btnBcv)
        val botonParalelo = fragment.view?.findViewById<ToggleButton>(R.id.btnEuroP)
        val botonPromedio = fragment.view?.findViewById<ToggleButton>(R.id.btnPromedio)

        if (botonBcv?.isChecked == true) {
            tasa = "BCV"
        }

        if (botonParalelo?.isChecked == true) {
            tasa = "Paralelo"
        }

        if (botonPromedio?.isChecked == true) {
            tasa = "Promedio"
        }
        return tasa
    }

    private fun prefijo(cedula: String?): String {
        var letra = ""
        if (cedula!!.isNotEmpty()) {
            when (cedula.first()) {
                'V', 'E' -> {
                    // Acción para cuando la primera letra es 'V'
                    letra = "CI:"
                    return letra
                }

                'J', 'G' -> {
                    // Acción para cuando la primera letra es 'J' o 'G'
                    letra = "Rif:"
                    return letra
                }

                'P' -> {
                    // Acción para cuando la primera letra es 'P'
                    letra = "Pasaporte:"
                    return letra
                }

                else -> {
                    // Acción para cuando la primera letra no es ninguna de las anteriores
                    letra = "CI:"
                    return letra
                }
            }
        } else {
            println("La variable está vacía")
        }

        return letra
    }


    @Throws(IOException::class)
    private fun writeDrawableImageToFile(context: Context, drawableId: Int, file: File) {
        context.resources.openRawResource(drawableId).use { inputStream ->
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    fun getCurrentFragmentTag(): String? {
        return try {
            // Encuentra el NavHostFragment por su ID
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as? NavHostFragment

            // Obtén la etiqueta del destino actual
            navHostFragment?.navController?.currentDestination?.label?.toString()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            null
        }
    }


    //funcio para llamar a la Class SubsCripcionCkeckWorker para verificas si esta vencida la Suscripcion******
//    fun verificaciondeSuscripcion() {
//        // Crear un objeto Data para enviar parámetros al Worker
//        val data = Data.Builder()
//            .putString("param1", "valor1")  // Puedes añadir tantos parámetros como necesites
//            .putInt("param2", 123)
//            .build()
//
//// Crear el WorkRequest para que se ejecute todos los días, con los parámetros
//        val subscriptionCheckRequest = PeriodicWorkRequestBuilder<SubscriptionCheckWorker>(
//            1, TimeUnit.DAYS
//        )
//            .setInputData(data)  // Añadir los parámetros al WorkRequest
//            .build()
//
//// Encolar el trabajo con WorkManager
//        WorkManager.getInstance(this).enqueue(subscriptionCheckRequest)
//    }

    //***********final del usao del work

    fun isSubscriptionActive(context: Context): Boolean {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("UserSubscription", Context.MODE_PRIVATE)

        // Verificar si el usuario tiene una suscripción de por vida
        val hasLifetimeSubscription = sharedPreferences.getBoolean("lifetime_subscription", false)

        if (hasLifetimeSubscription) {
            // Si tiene una suscripción de por vida, retorna false (es decir, sí tiene una suscripción activa)
            return true
        }

        // Obtener la fecha actual
        val currentDate = Calendar.getInstance().timeInMillis

        // Obtener la fecha de expiración de la suscripción
        val subscriptionExpiration = sharedPreferences.getLong("subscription_expiration", 0)



        if (currentDate >= subscriptionExpiration) {

            return false
        } else {

            return true
        }

    }

    // Función auxiliar para convertir milisegundos a una fecha legible
    fun convertMillisToDate(millis: Long): String {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        return format.format(calendar.time)
    }


}