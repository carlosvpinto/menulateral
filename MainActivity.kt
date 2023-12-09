package com.carlosv.dolaraldia

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Button
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
import androidx.core.app.ActivityCompat
import com.carlosv.dolaraldia.ui.bancos.BancoModelAdap
import com.carlosv.dolaraldia.ui.bancos.BancosModel
import com.carlosv.dolaraldia.ui.home.HomeFragment
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.ActivityMainBinding


import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.botonFloating.setOnClickListener { view ->
            takeScreenshot()
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show()
        }


        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Pasando cada ID de menú como un conjunto de ID porque cada
        // el menú debe considerarse como destino de nivel superior.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,R.id.nav_monedas, R.id.nav_bancos
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
                takeScreenshot()
                Toast.makeText(this, "Compartir", Toast.LENGTH_SHORT).show()
            }
            R.id.action_salir->{
                salirdelApp()
            }


        }
        return super.onOptionsItemSelected(item)
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

    // Solicitar permiso de almacenamiento
    private val STORAGE_REQUEST_CODE = 100
    private fun requestStoragePermission() {

        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_REQUEST_CODE
        )

    }
    // Capturar y compartir imagen
    private var imageUri: Uri? = null

    private fun takeScreenshot() {

        requestStoragePermission()

        val bitmap = screenshot()

        imageUri = saveImage(bitmap)

        shareImage(imageUri!!)

    }

    private fun screenshot(): Bitmap {

        // Obtener referencia a la vista raíz
        val rootView = window.decorView.rootView

        // Obtener dimensiones para crear Bitmap
        val width = rootView.width
        val height = rootView.height

        // Crear Bitmap
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Crear Canvas asociado al Bitmap
        val canvas = Canvas(bitmap)

        // Dibujar la vista raíz en el Canvas
        rootView.draw(canvas)

        return bitmap

    }

    private fun saveImage(image: Bitmap): Uri? {

        val name = "MiCapture.jpg"
        val imagesFolder = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File(imagesFolder, name)

        val stream = FileOutputStream(imageFile)
        image.compress(Bitmap.CompressFormat.JPEG, 100, stream)

        stream.flush()
        stream.close()

        return Uri.parse(
            MediaStore.Images.Media.insertImage(
                contentResolver,
                imageFile.absolutePath,
                name,
                null
            )
        )

    }

    private fun shareImage(imageUri: Uri) {

        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_STREAM, imageUri)
        intent.type = "image/jpeg"
        intent.putExtra(Intent.EXTRA_TITLE, "Compartir captura")
        intent.putExtra(Intent.EXTRA_TEXT, "Descarla la aplicacion y disfruta de la comodidad")
        startActivity(Intent.createChooser(intent, "Compartir captura"))

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permiso otorgado
        } else {
            // Permiso denegado
        }

    }

}