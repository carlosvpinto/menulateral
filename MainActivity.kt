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
import android.content.res.Resources
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ToggleButton

import androidx.annotation.RequiresApi

import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.ads.MobileAds
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val PERMISSION_REQUEST_CODE = 123
    var filePath = ""

    val fabBoton: FloatingActionButton by lazy {
        binding.appBarMain.botonFloating
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        MobileAds.initialize(this) {}
        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.botonFloating.setOnClickListener { view ->

            // Verificar si se tienen los permisos antes de capturar la pantalla
            if (checkPermission()) {
                captureScreen()
                binding.appBarMain.botonFloating.isEnabled= false
               binding.appBarMain.botonFloating.isEnabled = true

            } else {
                // Si no se tienen los permisos, solicitarlos
                requestPermissionsIfNecessary()
            }

        }

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
               // compartirEnlacePlayStore(this)
                Toast.makeText(this, "Compartir link Playstore", Toast.LENGTH_SHORT).show()
                compartirLinkconFoto(this,"https://play.google.com/store/apps/details?id=$packageName")

            }
            R.id.action_salir->{
                salirdelApp()
            }


        }
        return super.onOptionsItemSelected(item)
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
        val writeExternalStoragePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val readExternalStoragePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        return writeExternalStoragePermission == PackageManager.PERMISSION_GRANTED &&
                readExternalStoragePermission == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun  captureScreen() {
        // Tomar la captura de pantalla y guardarla en el archivo
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "screenshot_$timestamp.png"
            filePath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}/$fileName"

            // Crear un File para la captura de pantalla
            val file = File(filePath)

            // Tomar la captura de pantalla y guardarla en el archivo
            val rootView = window.decorView.rootView
            rootView.isDrawingCacheEnabled = true
            val bitmap = Bitmap.createBitmap(rootView.drawingCache)
            rootView.isDrawingCacheEnabled = false

            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
            stream.flush()
            stream.close()

            // Mostrar un mensaje indicando la ubicación del archivo
            Toast.makeText(
                this,
                "Captura de pantalla guardada en: $filePath",
                Toast.LENGTH_SHORT
            ).show()
            //llama a la funcion de compartir
            shareImageWithText(filePath,crearTextoCapture())
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
                // Si se otorgan los permisos, realizar la captura de pantalla
                captureScreen()
            } else {
                // Si se niegan los permisos, mostrar un mensaje al usuario
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

                    textoCapture="Monto en Dolares: $inputTextoDolla Monto Bs: $inputTextoBs"
            }else{
                    val btnTextInFragmentBcv = fragment.view?.findViewById<ToggleButton>(R.id.btnBcv)
                    val btnTextInFragmentParalelo = fragment.view?.findViewById<ToggleButton>(R.id.btnParalelo)
                    bcv = btnTextInFragmentBcv?.text.toString()
                    paralelo= btnTextInFragmentParalelo?.text.toString()
                    textoCapture="Precio del Dolar Bcv: $bcv Precio del Paralelo es: $paralelo"
                }

            }
        }
        if (nombreFragmentAct== "Otras Paginas"){
                textoCapture= "Precio del Dolar en Paginas Web"
        }
        if (nombreFragmentAct== "Precio en Bancos"){
            textoCapture= "Precio de venta del dolar en Bancos Venezolanos"
        }
        if (nombreFragmentAct== "Acerca..."){
            textoCapture= "Acerca de la Aplicacion"
        }

        if (nombreFragmentAct== "Precio del Euro"){
            val btnTextInFragmentEuro = fragment?.view?.findViewById<Button>(R.id.btnEuro)
            val TextBsFragmentEuro = fragment?.view?.findViewById<TextInputEditText>(R.id.inputBolivares)
            val TextEurosFragmentEuro = fragment?.view?.findViewById<TextInputEditText>(R.id.inputEuros)
            val euro = btnTextInFragmentEuro?.text
            val totalBs = TextBsFragmentEuro?.text
            val totalEuro = TextEurosFragmentEuro?.text
            textoCapture = "Precio del Euro: $euro total en Bs:$totalBs Total en Euro: $totalEuro "
        }
        Log.d("Capture", "crearTextoCapture: $textoCapture")
        return textoCapture
    }


    //**********************************
    fun compartirLinkconFoto(context: Context, text: String) {
        try {
                       // Crear un archivo temporal para compartir
            val tempFile = createTempImageFile()


            writeDrawableImageToFile(context, R.drawable.logodolar_al_dia, tempFile)
            // Obtener la Uri segura utilizando FileProvider
            val uri = FileProvider.getUriForFile(
                this,
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