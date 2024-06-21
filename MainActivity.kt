package com.carlosv.dolaraldia

import ShakeDetector
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
import android.content.res.Resources
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.ToggleButton

import androidx.annotation.RequiresApi

import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.PackageManagerCompat.LOG_TAG
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import java.util.concurrent.atomic.AtomicBoolean


class MainActivity : AppCompatActivity() {

    private lateinit var appOpenAdManager: AppOpenAdManager

    val miAplicacion = MyApplication()
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private val LOG_TAG: String  = "AppOpenAdManager"

    private  val AD_UNIT_ID: String? = "ca-app-pub-3940256099942544/9257395921"

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

//
//        binding.appBarMain.botonFloating.setOnClickListener { view ->
//
//            // Verificar si se tienen los permisos antes de capturar la pantalla
//            if (checkPermission()) {
//                captureScreen()
//                binding.appBarMain.botonFloating.isEnabled= false
//               binding.appBarMain.botonFloating.isEnabled = true
//
//            } else {
//                // Si no se tienen los permisos, solicitarlos
//                requestPermissionsIfNecessary()
//            }
//
//        }
        //verificarVersionMinima()

        //adOpen*********************
        initializeMobileAdsSdk()

        binding.navView

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Pasando cada ID de menú como un conjunto de ID porque cada
        // el menú debe considerarse como destino de nivel superior.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,R.id.nav_monedas, R.id.nav_bancos, R.id.nav_acerca, R.id.nav_Euros
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

        when(item.itemId){
            R.id.action_compartir->{
                // Verificar si se tienen los permisos antes de capturar la pantalla
                if (checkPermission()) {
                    captureScreen()

                } else {
                    // Si no se tienen los permisos, solicitarlos
                    requestPermissionsIfNecessary()
                }

            }
            R.id.action_salir->{
                salirdelApp()
            }


        }
        return super.onOptionsItemSelected(item)
    }
    //VERIFICA LA VERSION DEL LA APP
//    private fun versionUltima(){
//        val remoteConfig = FirebaseRemoteConfig.getInstance()
//        remoteConfig.fetchAndActivate()
//        val requiredVersionMinima = remoteConfig.getLong("version_min_dolar_al_dia")
//        val requiredSaludo = remoteConfig.getString("mensajebienvenida")
//        val packageInfo = packageManager.getPackageInfo(packageName, 0)
//        val versionCodeActual = packageInfo.versionCode
//
//        Log.d("totalDolarConfig", "VALOR DE LA VERSION requiredSaludo: $requiredSaludo")
//        Log.d("totalDolarConfig", "VALOR DE LA VERSION requiredVersionMinima: $requiredVersionMinima packageInfo $packageInfo versionCode $versionCodeActual")
//        if (versionCodeActual < requiredVersionMinima) {
//            // Mostrar un diálogo de actualización y redirigir a la Play Store.
//            Toast.makeText(this, "Necesitas Actualizar Tu version por la $requiredVersionMinima", Toast.LENGTH_LONG).show()
//            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.carlosvicente.uberkotlin"))
//            startActivity(intent)
//            finish() // O bloquea el acceso a la aplicación
//        } else {
//            // Continuar con la aplicación normalmente.
//            if (requiredSaludo.isNotEmpty()){
//                Toast.makeText(this, " $requiredSaludo", Toast.LENGTH_LONG).show()
//            }
//
//        }
//
//    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this) {}

        // Load an ad.
        (application as MyApplication).loadAd(this)
    }

    private fun verificarVersionMinima() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()

        // Fetch de la configuración remota
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Configuración remota obtenida con éxito
                    val requiredVersionMinima = remoteConfig.getLong("version_min_dolar_al_dia")
                    val requiredSaludo = remoteConfig.getString("mensajebienvenida")
                    val packageInfo = packageManager.getPackageInfo(packageName, 0)
                    val versionCodeActual = packageInfo.versionCode

                    Log.d("totalDolarConfig", "VALOR DE LA VERSION requiredSaludo: $requiredSaludo")
                    Log.d("totalDolarConfig", "VALOR DE LA VERSION requiredVersionMinima: $requiredVersionMinima packageInfo $packageInfo versionCode $versionCodeActual")

                    if (versionCodeActual < requiredVersionMinima) {
                        // La versión actual no cumple con la versión mínima requerida
                        // Mostrar un diálogo de actualización y redirigir a la Play Store.
                        val mensaje = "Necesitas actualizar tu versión a la $requiredVersionMinima"
                        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${this.packageName}"))
                        startActivity(intent)
                        finish() // O bloquear el acceso a la aplicación
                    } else {
                        // La versión actual cumple con la versión mínima requerida
                        // Continuar con la aplicación normalmente.
                        if (requiredSaludo.isNotEmpty()) {
                            Toast.makeText(this, requiredSaludo, Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    // Error al obtener la configuración remota
                    Log.e("totalDolarConfig", "Error al obtener la configuración remota", task.exception)
                    // Podrías manejar el error mostrando un mensaje al usuario o intentando nuevamente.
                }
            }
    }




    fun compartirEnlacePlayStore(context: Context) {
        val packageName = context.packageName

        try {
            // Crear la URL de la Play Store con el paquete de la aplicación
            val playStoreLink = "https://play.google.com/store/apps/details?id=$packageName"

            // Crear un Intent para compartir
            val compartirIntent = Intent(Intent.ACTION_SEND)
            compartirIntent.type = "text/plain"
            compartirIntent.putExtra(Intent.EXTRA_TEXT, playStoreLink)

            // Mostrar el diálogo de compartir
            context.startActivity(Intent.createChooser(compartirIntent, "Compartir enlace de la Play Store"))

        } catch (e: Exception) {
            // Manejar excepciones, por ejemplo, si la aplicación de la Play Store no está instalada
            Toast.makeText(context, "No se puede abrir la Play Store", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }








    private fun salirdelApp(){

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Salir")
        builder.setMessage("Desea salir de la aplicacion?")
        builder.setPositiveButton("Salir", DialogInterface.OnClickListener { dialog, which ->
            finishAffinity()
        })
        builder.setNegativeButton("Cancelar",null )
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

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun captureScreen() {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "screenshot_$timestamp.png"
            val picturesDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above
                getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            } else {
                // For older versions
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            }
            filePath = "$picturesDir/$fileName"

            // Create a File for the screenshot
            val file = File(filePath)

            // Take the screenshot and save it to the file
            val rootView = window.decorView.rootView
            rootView.isDrawingCacheEnabled = true
            val bitmap = Bitmap.createBitmap(rootView.drawingCache)
            rootView.isDrawingCacheEnabled = false

            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
            stream.flush()
            stream.close()

            // Show a message indicating the file location
            Toast.makeText(
                this,
                "Captura de pantalla guardada en: $filePath",
                Toast.LENGTH_SHORT
            ).show()

            // Call the share function
            shareImageWithText(filePath, crearTextoCapture())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (checkPermission()) {
                // If permissions are granted, take the screenshot
                captureScreen()
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



    private fun shareImageWithText(imagePath: String, shareText: String) {
        val imageFile = File(imagePath)

        if (imageFile.exists()) {
            try {
                // Crear un archivo temporal para compartir
                val tempFile = createTempImageFile()

                // Copiar la imagen original al archivo temporal
                copyFile(imageFile, tempFile)

                // Obtener la Uri segura utilizando FileProvider
                val uri = FileProvider.getUriForFile(
                    this,
                    "com.carlosv.menulateral.fileprovider",  // Reemplaza con el nombre de tu paquete
                    tempFile
                )
                Log.d("Capture", "shareImageWithText:uri $uri ")
                // Crear un intent para compartir
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "multipart/*"
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                // Iniciar la actividad de compartir
                startActivity(Intent.createChooser(shareIntent, "Compartir imagen"))

            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(
                    this,
                    "Error al compartir la imagen",
                    Toast.LENGTH_SHORT
                ).show()

            }
        } else {
            Toast.makeText(
                this,
                "La imagen no existe en la ruta especificada",
                Toast.LENGTH_SHORT
            ).show()

        }
    }

    override fun onRestart() {
        super.onRestart()
        initializeMobileAdsSdk()
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
    private fun crearTextoCapture():String{
        var textoCapture=""
        var inputTextoBs=""
        var inputTextoDolla=""
        var bcv= ""
        var paralelo= ""
        val linkCorto= "https://bit.ly/dolaraldia"

        //**************************
        val nombreFragmentAct = getCurrentFragmentTag()
        Log.d("CAPTURA", "crearTextoCapture: currentFragmentTag $nombreFragmentAct")
        if (nombreFragmentAct == "fragment_tag_que_quieres_comparar") {
            // Realiza la lógica específica del fragmento actual
        }
        //***********************************************
        val fragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
        if (fragment != null && fragment.isAdded) {
            if (nombreFragmentAct== "Dolar al Dia"){
                val editTextInFragmentBs = fragment.view?.findViewById<EditText>(R.id.inputBolivares)
                val editTextInFragmentDolar = fragment.view?.findViewById<EditText>(R.id.inputDolares)
                inputTextoBs = editTextInFragmentBs?.text.toString()
                inputTextoDolla= editTextInFragmentDolar?.text.toString()
                if (editTextInFragmentBs != null && !inputTextoBs.isNullOrEmpty()) {

                    textoCapture="Monto en Dolares: $inputTextoDolla Monto Bs: $inputTextoBs \n -Descarga la App \n $linkCorto"
            }else{
                    val btnTextInFragmentBcv = fragment.view?.findViewById<ToggleButton>(R.id.btnBcv)
                    val btnTextInFragmentParalelo = fragment.view?.findViewById<ToggleButton>(R.id.btnParalelo)
                    bcv = btnTextInFragmentBcv?.text.toString()
                    paralelo= btnTextInFragmentParalelo?.text.toString()
                    textoCapture="-Dolar Bcv: $bcv \n -Precio del Paralelo es: $paralelo \n -Descarga la App \n $linkCorto"
                }

            }
        }
        if (nombreFragmentAct== "Otras Paginas"){
                textoCapture= "Precio del Dolar en Paginas Web"
        }
        if (nombreFragmentAct== "Precio en Bancos"){
            textoCapture= "Precio de venta del dolar en Bancos Venezolanos \n -Descarga la App \n $linkCorto"
        }
        if (nombreFragmentAct== "Acerca..."){
            textoCapture= "Acerca de la Aplicacion la App \n -Descarga la App \n" +
                    " $linkCorto"
        }

        if (nombreFragmentAct== "Precio del Euro"){
            val btnTextInFragmentEuro = fragment?.view?.findViewById<Button>(R.id.btnEuro)
            val TextBsFragmentEuro = fragment?.view?.findViewById<TextInputEditText>(R.id.inputBolivares)
            val TextEurosFragmentEuro = fragment?.view?.findViewById<TextInputEditText>(R.id.inputEuros)
            val euro = btnTextInFragmentEuro?.text
            val totalBs = TextBsFragmentEuro?.text
            val totalEuro = TextEurosFragmentEuro?.text
            textoCapture = "Precio del Euro: $euro total en Bs:$totalBs Total en Euro: $totalEuro \n -Descarga la App \n $linkCorto"
        }
        Log.d("Capture", "crearTextoCapture: $textoCapture")
        return textoCapture
    }


    //**********************************
//    fun compartirLinkconFoto(context: Context, text: String) {
//        try {
//                       // Crear un archivo temporal para compartir
//            val tempFile = createTempImageFile()
//
//
//            writeDrawableImageToFile(context, R.drawable.logodolar_al_dia, tempFile)
//            // Obtener la Uri segura utilizando FileProvider
//            val uri = FileProvider.getUriForFile(
//                this,
//                "com.carlosv.menulateral.fileprovider",  // Reemplaza con el nombre de tu paquete
//                tempFile
//            )
//            Log.d("Capture", "shareImageWithText:uri $uri ")
//
//            // Crear un intent para compartir
//            val shareIntent = Intent().apply {
//                action = Intent.ACTION_SEND
//                type = "image/*"
//                putExtra(Intent.EXTRA_STREAM, uri)
//                putExtra(Intent.EXTRA_TEXT, text)
//                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//            }
//
//            // Iniciar la actividad de compartir
//            context.startActivity(Intent.createChooser(shareIntent, "Compartir con"))
//        } catch (e: IOException) {
//            // Manejar cualquier excepción que pueda ocurrir durante la creación del archivo temporal
//            e.printStackTrace()
//        }
//    }
    fun compartirLinkconFoto(context: Context, text: String) {
        try {
            // Crear un archivo temporal para compartir
            val tempFile = createTempImageFile()

            writeDrawableImageToFile(context, R.drawable.logodolar_al_dia, tempFile)

            // Obtener la Uri segura utilizando FileProvider
            val uri = FileProvider.getUriForFile(
                context,
                "com.carlosv.menulateral.fileprovider",  // Reemplaza con el nombre de tu paquete
                tempFile
            )
            Log.d("Capture", "shareImageWithText:uri $uri ")

            // Crear un intent para compartir
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Iniciar la actividad de compartir
            context.startActivity(Intent.createChooser(shareIntent, "Compartir con"))
        } catch (e: IOException) {
            // Manejar cualquier excepción que pueda ocurrir durante la creación del archivo temporal
            e.printStackTrace()
        }
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
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        val currentDestination = navController.currentDestination

        return currentDestination?.label.toString()
    }
    //***********************************


}