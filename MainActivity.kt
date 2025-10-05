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
import java.util.Locale
import java.util.*

import android.Manifest
import android.app.Activity

import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Editable

import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.ImageSpan
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.ToggleButton
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn


import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.carlosv.dolaraldia.model.datosPMovil.DatosPMovilModel
import com.carlosv.dolaraldia.utils.Constants.URL_DESCARGA
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.common.reflect.TypeToken
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

import com.google.gson.Gson
import java.util.concurrent.atomic.AtomicBoolean
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParseException


class MainActivity : AppCompatActivity() {


   // private lateinit var navController2: NavController
   // AÑADE ESTA LÍNEA (la haremos lazy para que se inicialice de forma segura)
   private val navController by lazy {
       val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
       navHostFragment.navController
   }


    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private val LOG_TAG: String = "AppOpenAdManager"
    private val TAG: String = "MAINACTIVITY"

    private val AD_UNIT_ID: String = "ca-app-pub-3940256099942544/9257395921"


    private var enviarImagenP: Boolean? = null
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val PERMISSION_REQUEST_CODE = 123


    private var pagoMovilListTrue: DatosPMovilModel? = null

    private var personalBadge: BadgeDrawable? = null
    private var plaftforBagde: BadgeDrawable? = null

    //Propiedad para guardar una referencia al menú ---
    private var optionsMenu: Menu? = null

    // ¡NUEVO! Bandera para controlar el ciclo de reintento.
    private var isInitialAdFlowDone = false




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Esta línea es CRUCIAL. Le dice a la app que dibuje detrás de las barras del sistema.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.statusBarColor = Color.TRANSPARENT

        // 3. (Opcional pero recomendado) Controlar el color de los iconos de la barra de estado
        // Usa el Controller para decirle si los iconos deben ser claros u oscuros.
        // Como tu fondo es azul oscuro, quieres iconos blancos (apariencia clara en falso).
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // isAppearanceLightStatusBars = false significa iconos BLANCOS
        // isAppearanceLightStatusBars = true significa iconos NEGROS
        windowInsetsController.isAppearanceLightStatusBars = false


        //enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val appBar = binding.appBarLayout // O findViewById(R.id.app_bar_layout)

        ViewCompat.setOnApplyWindowInsetsListener(appBar) { view, windowInsets ->
            // Obtiene los insets (espacios) de las barras del sistema (status bar, etc.)
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Aplica el inset superior como padding a la parte superior de la vista
            view.setPadding(view.paddingLeft, insets.top, view.paddingRight, view.paddingBottom)

            // Devuelve los insets para que otros vistas también puedan usarlos si es necesario
            windowInsets
        }




        val application = application as? MyApplication ?: return

        // Inicia el flujo del anuncio de apertura.
        application.showAdIfAvailable(
            this,
            object : MyApplication.OnShowAdCompleteListener {
                override fun onShowAdComplete() {
                    // Marcamos que el flujo ha terminado, ya sea por éxito o por fallo.
                    isInitialAdFlowDone = true
                    Log.d("MainActivity", "Flujo de anuncio de inicio completado.")
                }

                override fun onAdLoaded() {
                    // --- ¡AQUÍ ESTÁ LA LÓGICA ANTI-CICLOS! ---
                    // Solo reintentamos mostrar el anuncio si el flujo inicial AÚN NO ha terminado.
                    if (!isInitialAdFlowDone) {
                        Log.d("MainActivity", "El anuncio se cargó, reintentando mostrar ahora.")
                        // Marcamos que ya no necesitamos más reintentos.
                        isInitialAdFlowDone = true
                        application.showAdIfAvailable(this@MainActivity, this)
                    } else {
                        Log.d("MainActivity", "El anuncio se cargó, pero el flujo inicial ya terminó. No se mostrará ahora.")
                    }
                }
            }
        )


        // 1. Configurar la Toolbar
        setSupportActionBar(binding.toolbar)

        // 2. Obtener la BottomNavigationView
        val navView: BottomNavigationView = binding.bottomNavView

        // 3. Definir la configuración de la AppBar.
        // El NavController se obtiene de la propiedad `lazy` que definimos arriba.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_platforms, R.id.nav_bancos, R.id.nav_pago, R.id.nav_more
            )
        )

        // 4. Conectar TODO.
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)



        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                // Fragmentos donde el menú SÍ debe ser visible
                R.id.nav_home,
                R.id.nav_Personal,
                R.id.nav_platforms,
                R.id.nav_bancos,
                R.id.nav_more
                    -> {
                    navView.visibility = View.VISIBLE
                }
                // Para todos los demás fragmentos, se oculta
                else -> {
                    navView.visibility = View.GONE
                }
            }
        }

        // --- El resto de tu lógica de onCreate ---
        MobileAds.initialize(this) {}
        AppPreferences.init(this)
        versionUltima()
        movilidadPantalla()
    }


    @OptIn(ExperimentalBadgeUtils::class)
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.optionsMenu = menu
        menuInflater.inflate(R.menu.main, menu)

        val platformsItem = menu.findItem(R.id.action_platforms)

        if (AppPreferences.haVistoDialogoPlatformas()) {
            platformsItem.isVisible = false
        } else {
            platformsItem.isVisible = true
            plaftforBagde = BadgeDrawable.create(this)
            plaftforBagde?.number = 1
            plaftforBagde?.isVisible = true
            // CAMBIO: La referencia a la toolbar ahora es directa desde el binding
            BadgeUtils.attachBadgeDrawable(plaftforBagde!!, binding.toolbar, R.id.action_platforms)
        }

        if (!AppPreferences.haVistoDialogoPagoMovil()) {
            personalBadge = BadgeDrawable.create(this)
            personalBadge?.number = 1
            personalBadge?.isVisible = true
            // CAMBIO: La referencia a la toolbar ahora es directa desde el binding
            BadgeUtils.attachBadgeDrawable(personalBadge!!, binding.toolbar, R.id.action_personal)
        }
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        // Usamos la propiedad `navController` de la clase.
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_compartir -> {
                showCustomDialogPM()
            }
            R.id.action_platforms -> {
                if (!AppPreferences.haVistoDialogoPlatformas()) {
                    mostrarDialogoNovedadPlataformas()
                } else {
                    // Usamos la propiedad `navController` de la clase.
                    binding.bottomNavView.selectedItemId = R.id.nav_platforms


                    true

                }
            }
            R.id.action_personal -> {
                if (!AppPreferences.haVistoDialogoPagoMovil()) {
                    mostrarDialogoNovedadPagoMovil()
                } else {
                    // 1. Buscamos el ítem correspondiente en la BottomNavigationView
                    //    usando el ID del DESTINO de navegación (`R.id.nav_Personal`).
                    binding.bottomNavView.selectedItemId = R.id.nav_Personal

                    // 2. Devolvemos `true` para indicar que hemos manejado el clic.
                    true
                }
            }
            R.id.action_salir -> {
                salirdelApp()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // --- DIÁLOGO DE NOVEDAD ACTUALIZADO ---
    private fun mostrarDialogoNovedadPlataformas() {
        MaterialAlertDialogBuilder(this)
            .setTitle("¡Nueva Pantalla Disponible!")
            .setMessage("Hemos añadido una nueva sección de 'Plataformas'. Ahora puedes consultar las tasas de cambio de Binance, Bybit y Yadio directamente en la app.")
            .setPositiveButton("¡Genial, quiero verla!") { dialog, _ ->
                // 1. Guardamos que el usuario ya vio el mensaje.
                AppPreferences.marcarDialogoPlatformasComoVisto()
                ocultarBadgePlatformas()

                // Usamos la propiedad `navController` de la clase.
                binding.bottomNavView.selectedItemId = R.id.nav_platforms


                true

                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }



    private fun mostrarDialogoNovedadPagoMovil() {
        MaterialAlertDialogBuilder(this)
            .setTitle("¡Nueva Función Disponible!")
            .setMessage("Ahora puedes personalizar y compartir una imagen con tus datos de Pago Móvil para que tus transacciones sean más rápidas y profesionales.")
            .setPositiveButton("Entendido") { dialog, which ->
                // Cuando el usuario pulsa "Entendido":

                // Guardamos que ya vio el mensaje para no mostrarlo de nuevo
                AppPreferences.marcarDialogoPagoMovilComoVisto()

                // Ocultamos el badge
                ocultarBadgePersonal()
                binding.bottomNavView.selectedItemId = R.id.nav_Personal

                // 2. Devolvemos `true` para indicar que hemos manejado el clic.
                true
                //llevarlo directamente a la función de Pago Móvil
               // navController.navigate(R.id.nav_Personal)
            }
            .setCancelable(false) // El usuario debe presionar el botón para cerrar
            .show()
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

    fun mostrarBadgePersonal(contador: Int) {
        personalBadge?.let {
            if (contador > 0) {
                it.number = contador
                it.isVisible = true
            } else {
                // Si el contador es 0 o menos, simplemente lo ocultamos
                it.isVisible = false
            }
        }
    }

    /**
     * Oculta el badge del ícono de Pago Móvil.
     */
    fun ocultarBadgePersonal() {
        personalBadge?.isVisible = false
    }

    // --- FUNCIÓN ocultarBadgePlatformas TOTALMENTE ACTUALIZADA ---
    fun ocultarBadgePlatformas() {
        // 1. Oculta el badge visualmente (buena práctica)
        plaftforBagde?.isVisible = false

        // 2. Busca el ítem del menú usando la referencia que guardamos
        val platformsItem = optionsMenu?.findItem(R.id.action_platforms)

        // 3. Oculta el ítem del menú
        platformsItem?.isVisible = false

        // 4. (Opcional pero recomendado) Invalida el menú para asegurar que se redibuje
        // A veces, cambiar 'isVisible' no es suficiente y es bueno forzar un redibujado.
       // invalidateOptionsMenu()
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
                //showCustomSnackbarPM()
                showCustomDialogPM()
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


    private fun showCustomDialogPM() {
        val customView = LayoutInflater.from(this).inflate(R.layout.custom_toast_pago_movil, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(customView)
        val dialog = builder.create()

        // Establece el fondo transparente ANTES de mostrar.
        val window = dialog.window
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // ENCONTRAR VISTAS Y CONFIGURAR LISTENERS
        val chechPagoMovil: MaterialCheckBox = customView.findViewById(R.id.checPagomovil)
        val chechImagenPersonalizada: MaterialCheckBox = customView.findViewById(R.id.checImagenpersonalizada)
        val closeButton: ImageButton = customView.findViewById(R.id.close_button)
        val enviareButton: MaterialButton = customView.findViewById(R.id.btnEnviarPM)
        val chekMontoPersonalizado: MaterialCheckBox = customView.findViewById(R.id.checkMontoPerso)
        val montoPersonalizado: EditText = customView.findViewById(R.id.editMontoPersonalizado)
        val layoutMontoPersonalizado: TextInputLayout = customView.findViewById(R.id.montoPersonalizadoLayout)

        // ¡Aquí aplicamos la magia de la validación a nuestro EditText!
        setupDecimalInputValidation(montoPersonalizado)

        pagoMovilListTrue = obtenerPagoMovilListTrue(this)
        if (pagoMovilListTrue?.seleccionado == null) {

            inabilitarCheck(chechPagoMovil, getString(R.string.sin_cuenta_seleccionada))
            inabilitarCheck(chechImagenPersonalizada,getString(R.string.imagen_personalizada) )

        } else {
            chechPagoMovil.text = pagoMovilListTrue?.nombre
            habilitarCheck(chechPagoMovil,pagoMovilListTrue?.nombre.toString() )

            if (pagoMovilListTrue?.imagen !== null){
               habilitarCheck(chechImagenPersonalizada,getString(R.string.imagen_personalizada))

            }else{
                inabilitarCheck(chechImagenPersonalizada,getString(R.string.sin_imagen_personalizada))

            }
        }

// Configura UN SOLO listener para manejar todos los cambios del CheckBox
        chekMontoPersonalizado.setOnCheckedChangeListener { _, isChecked ->

            // La variable 'isChecked' nos dice el nuevo estado del CheckBox
            if (isChecked) {
                // --- ACCIONES CUANDO EL CHECKBOX SE MARCA ---

                // 1. Actualizar la UI
                chekMontoPersonalizado.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                montoPersonalizado.visibility = View.VISIBLE
                layoutMontoPersonalizado.visibility = View.VISIBLE
                // 2. Mover el foco y mostrar el teclado
                montoPersonalizado.requestFocus()
                showKeyboard(montoPersonalizado)

            } else {
                // --- ACCIONES CUANDO EL CHECKBOX SE DESMARCA ---

                // 1. Restaurar la UI
                chekMontoPersonalizado.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                montoPersonalizado.visibility = View.GONE
                layoutMontoPersonalizado.visibility = View.GONE
                // 2. Ocultar el teclado y limpiar el foco
                //hideKeyboard(montoPersonalizado)
                hideKeyboard()
                montoPersonalizado.clearFocus()
            }
        }


        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        enviareButton.setOnClickListener {

            val textoMonto = montoPersonalizado.text.toString() // Obtiene el texto del EditText como String
                enviarImagenP = chechImagenPersonalizada.isChecked
            // Intenta convertir el texto a Float. Si es nulo (vacío o no es un número válido), usa 0.0f
            val montoFloat: String = (textoMonto.toString() ?: 0.0f).toString()
            shareLogic(chechPagoMovil.isChecked,chechImagenPersonalizada.isChecked,chekMontoPersonalizado.isChecked, montoFloat)
            dialog.dismiss()
        }

        // --- INICIO: CÓDIGO PARA POSICIONAR EL DIÁLOGO ---
        if (window != null) {
            // 1. Establece la gravedad a la parte superior y centrado horizontalmente.
            window.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL)

            // 2. Añade un margen superior para que no se pegue al status bar.
            val params = window.attributes
            // La distancia en píxeles desde el borde superior. Puedes ajustar este valor.
            params.y = 140
            window.attributes = params
        }


        // Muestra el diálogo.
        dialog.show()

        // Aplica el desenfoque DESPUÉS de mostrar.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window?.setBackgroundBlurRadius(90)
                Log.d("DialogBlur", "showCustomDialogPM: realiz el efecto burt")
            }
        } catch (e: Exception) {
            Log.e("DialogBlur", "Failed to set background blur", e)
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        // Buscamos la vista que tiene el foco actual. Si no hay ninguna, usamos la vista raíz.
        // Esto es más seguro que depender de una vista específica que podría no ser válida.
        val view = currentFocus ?: View(this)
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showKeyboard(view: View) {
        // Usar view.post asegura que este código se ejecute después de que la vista
        // esté completamente medida y dibujada, evitando condiciones de carrera.
        view.post {
            if (view.requestFocus()) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }


    //Inabilita el CheckBox que le envien
    private fun inabilitarCheck(checkInvilitar: CheckBox, text:String){
        checkInvilitar.isChecked = false
        checkInvilitar.isEnabled = false
        checkInvilitar.setTypeface(null, Typeface.ITALIC)
        checkInvilitar.text = text
        checkInvilitar.alpha = 0.5f

    }

    //Habilita el CheckBox que le envien
    private fun habilitarCheck(checkInvilitar: CheckBox, text: String){
        checkInvilitar.isChecked = true
        checkInvilitar.isEnabled = true
        checkInvilitar.setTypeface(null, Typeface.NORMAL)
        checkInvilitar.text = text
        checkInvilitar.alpha = 1.0f

    }

    private fun setupDecimalInputValidation(editText: EditText) {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No es necesario hacer nada aquí
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No es necesario hacer nada aquí
            }

            override fun afterTextChanged(s: Editable?) {
                val originalString = s.toString()
                if (originalString.isEmpty()) return

                // Para evitar bucles infinitos al modificar el texto, quitamos el watcher temporalmente.
                editText.removeTextChangedListener(this)

                var correctedString = originalString

                // 1. Reemplazar comas por puntos para estandarizar.
                if (correctedString.contains(',')) {
                    correctedString = correctedString.replace(',', '.')
                }

                // 2. Asegurarse de que no haya más de un punto.
                val firstDotIndex = correctedString.indexOf('.')
                if (firstDotIndex != -1) { // Si hay un punto
                    // Si hay más puntos después del primero, los eliminamos.
                    if (correctedString.lastIndexOf('.') != firstDotIndex) {
                        // Conservamos solo hasta el último carácter válido antes del segundo punto.
                        val lastValidCharIndex = correctedString.length - 1
                        correctedString = correctedString.substring(0, lastValidCharIndex)
                    }
                }

                // 3. Limitar a dos dígitos después del punto.
                val dotIndex = correctedString.indexOf('.')
                if (dotIndex != -1) {
                    val decimalPart = correctedString.substring(dotIndex + 1)
                    if (decimalPart.length > 2) {
                        correctedString = correctedString.substring(0, dotIndex + 3) // 0-based index + dot + 2 digits
                    }
                }

                // Si hemos corregido la cadena, la actualizamos en el EditText.
                if (originalString != correctedString) {
                    editText.setText(correctedString)
                    // Movemos el cursor al final del texto.
                    editText.setSelection(correctedString.length)
                }

                // Volvemos a añadir el watcher.
                editText.addTextChangedListener(this)
            }
        }

        editText.addTextChangedListener(textWatcher)
    }
//Logica para Compartr el pago movil con el texto y la imagen
//    private fun shareLogic(
//    enviarDatosPM: Boolean,
//    enviarImagenP: Boolean,
//    enviarMontoP: Boolean,
//    montoPersonalizado: String,
//) {
//        pagoMovilListTrue = obtenerPagoMovilListTrue(this@MainActivity)
//        Log.d(TAG, "Decidiendo lógica para: DatosPM=$enviarDatosPM, ImagenP=$enviarImagenP, MontoP=$enviarMontoP")
//
//        // Usamos una expresión 'when' sobre una Triple para evaluar todas las combinaciones.
//        // El formato es (enviarDatosPM, enviarImagenP, enviarMontoP)
//        when (Triple(enviarDatosPM, enviarImagenP, enviarMontoP)) {
//
//
//
//
//            Triple(true, true, true) -> {
//                // CASO 1:
//                val texto = crearTextoCapture(enviarDatosPM, enviarMontoP,enviarImagenP,  pagoMovilListTrue, montoPersonalizado)
//                shareImageWithText(pagoMovilListTrue!!.imagen!!, texto)
//            }
//
//            // CASO 2: Datos + Imagen (true, true, false)
//            Triple(true, true, false) -> {
//
//                val texto = crearTextoCapture(enviarDatosPM, enviarImagenP, enviarMontoP  , pagoMovilListTrue, montoPersonalizado)
//                shareImageWithText(pagoMovilListTrue!!.imagen!!, texto)
//            }
//
//            // CASO 3: Datos + Monto (true, false, true)
//            Triple(true, false, true) -> {
//                val texto = crearTextoCapture(enviarDatosPM,enviarImagenP, enviarMontoP,  pagoMovilListTrue, montoPersonalizado)
//                shareImageWithText(capturarPantalla()!!, texto) // Ejemplo de llamada a otra función
//            }
//
//            // CASO 4: Solo Datos (true, false, false)
//            Triple(true, false, false) -> {
//                Log.d(TAG, "CASO 4: Solo Datos")
//                // Reemplaza esto con la función que desees
//                val texto = crearTextoCapture(enviarDatosPM, enviarImagenP, enviarMontoP, pagoMovilListTrue, montoPersonalizado)
//                shareImageWithText(capturarPantalla()!!, texto)
//            }
//
//            // CASO 5: Imagen + Monto (false, true, true)
//            Triple(false, true, true) -> {
//                // Reemplaza esto con la función que desees
//                val texto = crearTextoCapture(enviarDatosPM, enviarImagenP, enviarMontoP, pagoMovilListTrue, montoPersonalizado)
//                shareImageWithText(pagoMovilListTrue!!.imagen!!, texto)
//            }
//
//            // CASO 6: Solo Imagen (false, true, false)
//            Triple(false, true, false) -> {
//                // Reemplaza esto con la función que desees
//                val texto = crearTextoCapture(enviarDatosPM, enviarImagenP, enviarMontoP, pagoMovilListTrue, montoPersonalizado)// Texto podría ser vacío o un título
//                shareImageWithText(pagoMovilListTrue!!.imagen!!, texto)
//            }
//
//            // CASO 7: Solo Monto (false, false, true)
//            Triple(false, false, true) -> {
//                // Reemplaza esto con la función que desees
//                val texto = crearTextoCapture(enviarDatosPM, enviarImagenP, enviarMontoP,  pagoMovilListTrue, montoPersonalizado)// Texto podría ser vacío o un título
//                shareImageWithText(capturarPantalla()!!, texto)
//            }
//
//            // CASO 8: NADA seleccionado (false, false, false)
//            Triple(false, false, false) -> {
//                // Reemplaza esto con la función que desees
//                val texto = crearTextoCapture(enviarDatosPM, enviarImagenP, enviarMontoP,  pagoMovilListTrue, montoPersonalizado)// Texto podría ser vacío o un título
//                shareImageWithText(capturarPantalla()!!, texto)
//            }
//        }
//    }


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





    // VERSIÓN REFACTORIZADA PARA PRIORIZAR WHATSAPP
    private fun shareImageWithText(imagePath: String?, shareText: String) {
        val imageFile = imagePath?.let { File(it) }
        Log.d(TAG, "Intentando compartir: imageFile=$imageFile, texto=$shareText")

        if (imageFile == null || !imageFile.exists()) {
            // Si no hay imagen, comparte solo el texto para no fallar.
            Log.d(TAG, "No se encontró archivo de imagen. Compartiendo solo texto.")
            shareText(shareText)
            return
        }

        try {
            // 1. Obtener la URI segura para nuestro archivo
            val uri = FileProvider.getUriForFile(
                this,
                "com.carlosv.menulateral.fileprovider",
                imageFile
            )

            // 2. Crear el Intent genérico para buscar TODAS las apps que pueden compartir
            val genericShareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // 3. Buscar todas las actividades que pueden manejar nuestro intent genérico
            val packageManager = packageManager
            val resolvedInfoList: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(genericShareIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
            } else {
                packageManager.queryIntentActivities(genericShareIntent, PackageManager.MATCH_DEFAULT_ONLY)
            }

            if (resolvedInfoList.isEmpty()) {
                Toast.makeText(this, "No se encontraron apps para compartir", Toast.LENGTH_SHORT).show()
                return
            }

            // 4. Crear una lista para nuestros intents "fijados" (WhatsApp)
            val targetedIntents = mutableListOf<Intent>()

            for (resolveInfo in resolvedInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                // Buscamos WhatsApp y WhatsApp Business
                if (packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b") {
                    val targetedShareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        // La parte clave: especificamos el paquete y la clase de la actividad
                        setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name)
                    }
                    targetedIntents.add(targetedShareIntent)
                }
            }

            // 5. Crear el Chooser (selector)
            // Usamos el intent genérico como base, que mostrará TODAS las demás apps
            val chooserIntent = Intent.createChooser(genericShareIntent, "Compartir vía...")

            // 6. ¡LA MAGIA! Añadimos nuestra lista de intents de WhatsApp como "opciones iniciales".
            // El sistema las pondrá en la parte superior de la lista.
            if (targetedIntents.isNotEmpty()) {
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedIntents.toTypedArray())
            }

            // 7. Iniciar el Chooser
            startActivity(chooserIntent)

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Error al crear el intent para compartir: ${e.message}")
            Toast.makeText(this, "No se pudo compartir la imagen", Toast.LENGTH_SHORT).show()
        }
    }


    fun Activity.capturarPantalla(): String? {
        return try {
            // Captura la vista raíz de la actividad
            val rootView: View = window.decorView.rootView
            val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            rootView.draw(canvas)

            // CAMBIO CLAVE 1: Definimos la subcarpeta "images", igual que en la otra función.
            val imageCacheDir = File(cacheDir, "images")
            imageCacheDir.mkdirs() // Nos aseguramos de que el directorio exista.

            // CAMBIO CLAVE 2: Creamos el archivo DENTRO de la subcarpeta.
            val imageFile = File(imageCacheDir, "captura_pantalla.png")
            FileOutputStream(imageFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            imageFile.absolutePath // Devuelve la ruta absoluta del archivo
        } catch (e: Exception) {
            e.printStackTrace()
            null
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



    override fun onRestart() {
        super.onRestart()
        //initializeMobileAdsSdk()
    }


    //Crea el Texto que va a ir junto con el Capture
    private fun crearTextoCapture(
        enviarDatosPM: Boolean,
        enviarImagenPM: Boolean,
        enviarMontoPM: Boolean,
        pagoMovilListTrue: DatosPMovilModel?,
        montoPersonalizado: String,
    ): String {
        var textoCapture = ""
        var inputTextoBs = ""
        var inputTextoDolla = ""
        var inputTextoDollaFotmateado = ""
        var inputTextoBolivarFotmateado = ""
        var bcv = ""
        var paralelo = ""
        var promedio = ""
        var tasa = ""
        val linkCorto = URL_DESCARGA


        //**************************
        val nombreFragmentAct = getCurrentFragmentTag()

        //***********************************************
        val fragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
        val botonEuro = fragment?.view?.findViewById<ToggleButton>(R.id.btnEuroP)
        val botonPromedio = fragment?.view?.findViewById<ToggleButton>(R.id.btnPromedio)
        val botonBcv = fragment?.view?.findViewById<ToggleButton>(R.id.btnBcv)

        val euroBcv = botonEuro?.textOn.toString()
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

                inputTextoDollaFotmateado = formatNumberForReceipt(inputTextoDolla)
                inputTextoBolivarFotmateado= formatNumberForReceipt(inputTextoBs)
                if (editTextInFragmentBs != null && !inputTextoBs.isNullOrEmpty()) {

                    //Verifica se el Usuario quiere enviar Datos de pago Movil
                    if (enviarDatosPM && enviarMontoPM) {

                        textoCapture=  "-Monto Bs: $montoPersonalizado \n \n -Pago Movil:\n -Tlf: ${pagoMovilListTrue?.tlf} \n -${
                            prefijo(pagoMovilListTrue?.cedula)
                        } ${pagoMovilListTrue?.cedula}  \n -Banco: ${pagoMovilListTrue?.banco}\n \n -Descarga la App \n $linkCorto"

                    }

                    if (enviarDatosPM &&  !enviarMontoPM){
                        textoCapture = "-Tasa: $tasa \n -Monto en Dolares: $inputTextoDollaFotmateado \n -Monto Bs: $inputTextoBolivarFotmateado \n \n -Pago Movil:\n -Tlf: ${pagoMovilListTrue?.tlf} \n -${
                                prefijo(pagoMovilListTrue?.cedula)
                            } ${pagoMovilListTrue?.cedula}  \n -Banco: ${pagoMovilListTrue?.banco}\n \n -Descarga la App \n $linkCorto"
                    }

                    if (!enviarDatosPM &&  !enviarMontoPM){
                        textoCapture = "-Tasa: $tasa \n -Monto en Bs: $inputTextoBolivarFotmateado  \n -Monto en Dolares: $inputTextoDollaFotmateado  \n \n -Descarga la App \n $linkCorto"
                    }

                    if (!enviarDatosPM &&  enviarMontoPM){
                        textoCapture = "-Monto Bs: $montoPersonalizado \n \n -Pago Movil:\n -Tlf: ${pagoMovilListTrue?.tlf} \n -${
                            prefijo(pagoMovilListTrue?.cedula)
                        } ${pagoMovilListTrue?.cedula}  \n -Banco: ${pagoMovilListTrue?.banco}\n \n -Descarga la App \n $linkCorto"
                    }


                } else {

                    if (enviarDatosPM && enviarMontoPM) {

                        textoCapture=  "-Monto Bs: $montoPersonalizado \n \n -Pago Movil:\n -Tlf: ${pagoMovilListTrue?.tlf} \n -${
                            prefijo(pagoMovilListTrue?.cedula)
                        } ${pagoMovilListTrue?.cedula}  \n -Banco: ${pagoMovilListTrue?.banco}\n \n -Descarga la App \n $linkCorto"

                    }

                    if (!enviarDatosPM && enviarMontoPM) {

                        textoCapture=
                            "-Dolar Bcv: $bcv2 \n -Euro Bcv: $euroBcv \n \n-Monto Bs: $montoPersonalizado \n \n -Descarga la App \n $linkCorto"

                    }

                     if (enviarDatosPM && !enviarMontoPM){
                         textoCapture =
                             "-Dolar Bcv: $bcv2 \n -Euro Bcv: $euroBcv \n" +
                                     " \n -Pago Movil:\n -Tlf: ${pagoMovilListTrue?.tlf} \n-${prefijo(pagoMovilListTrue?.cedula)} ${pagoMovilListTrue?.cedula}  \n" +
                                     " -Banco: ${pagoMovilListTrue?.banco}\n\n" +
                                     " -Descarga la App \n $linkCorto"
                     }

                    if (!enviarDatosPM &&  !enviarMontoPM){
                        textoCapture = "-Tasa: $tasa \n -Dolar Bcv: $bcv2 \n-Euro Bcv: $euroBcv  \n" +
                                " \n -Descarga la App \n $linkCorto"
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
            val textBsFragmentEuro =
                fragment?.view?.findViewById<TextInputEditText>(R.id.inputBolivares)
            val textEurosFragmentEuro =
                fragment?.view?.findViewById<TextInputEditText>(R.id.inputEuros)
            val euro = btnTextInFragmentEuro?.text
            val totalBs = textBsFragmentEuro?.text
            val totalEuro = textEurosFragmentEuro?.text

            textoCapture =
                "Precio del Euro: $euro total en Bs:$totalBs Total en Euro: $totalEuro \n -Descarga la App \n $linkCorto"
        }

        if (nombreFragmentAct == "Pago Movil") {
            //Verifica se el Usuario quiere enviar Datos
            if (enviarDatosPM) {

                textoCapture =
                    "-Pago Movil:\n -Tlf: ${pagoMovilListTrue?.tlf} \n -${prefijo(pagoMovilListTrue?.cedula)} ${pagoMovilListTrue?.cedula}  \n -Banco: ${pagoMovilListTrue?.banco}\n \n -Descarga la App \n $linkCorto"
            }

            if (enviarDatosPM && enviarMontoPM){
                textoCapture = "-Monto en bs: $montoPersonalizado \n\n -Pago Movil:\n -Tlf: ${pagoMovilListTrue?.tlf} \n -${prefijo(pagoMovilListTrue?.cedula)} ${pagoMovilListTrue?.cedula}  \n -Banco: ${pagoMovilListTrue?.banco}\n \n -Descarga la App \n $linkCorto"
            }

            if (!enviarDatosPM && !enviarMontoPM && !enviarImagenPM){
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

        if (nombreFragmentAct == "Plataformas"){
            if (enviarDatosPM) {

                textoCapture =
                    "-Pago Movil:\n -Tlf: ${pagoMovilListTrue?.tlf} \n -${prefijo(pagoMovilListTrue?.cedula)} ${pagoMovilListTrue?.cedula}  \n -Banco: ${pagoMovilListTrue?.banco}\n \n -Descarga la App \n $linkCorto"
            }
            if (enviarDatosPM && enviarMontoPM){
                textoCapture = "-Monto en bs: $montoPersonalizado \n\n -Pago Movil:\n -Tlf: ${pagoMovilListTrue?.tlf} \n -${prefijo(pagoMovilListTrue?.cedula)} ${pagoMovilListTrue?.cedula}  \n -Banco: ${pagoMovilListTrue?.banco}\n \n -Descarga la App \n $linkCorto"
            }
            if (!enviarDatosPM && !enviarMontoPM && !enviarImagenPM){
                textoCapture =
                    "-Precio de Usdt en Plataformas \n -Descarga la app $linkCorto "
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
            tasa = "Euro"
        }

        if (botonPromedio?.isChecked == true) {
            tasa = "Promedio"
        }
        return tasa
    }

    private fun prefijo(cedula: String?): String {
        var letra = ""
        if (!cedula.isNullOrEmpty()){
        //if (cedula!!.isNotEmpty()) {
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
            letra = "CI:"
            return letra
           // println("La variable está vacía")
        }

       // return letra
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

    //GENERAR NUEVA RECIBO MEJORADO*****************************

    private fun shareLogic(
        enviarDatosPM: Boolean,
        enviarImagenP: Boolean,
        enviarMontoP: Boolean,
        montoPersonalizado: String,
    ) {
        pagoMovilListTrue = obtenerPagoMovilListTrue(this@MainActivity)
        Log.d(TAG, "Decidiendo lógica para: DatosPM=$enviarDatosPM, ImagenP=$enviarImagenP, MontoP=$enviarMontoP")

        when (Triple(enviarDatosPM, enviarImagenP, enviarMontoP)) {

            Triple(true, true, true) -> {
                val texto = crearTextoCapture(enviarDatosPM, enviarMontoP,enviarImagenP,  pagoMovilListTrue, montoPersonalizado)
                shareImageWithText(pagoMovilListTrue!!.imagen!!, texto)
            }

            Triple(true, true, false) -> {
                val texto = crearTextoCapture(enviarDatosPM, enviarImagenP, enviarMontoP  , pagoMovilListTrue, montoPersonalizado)
                shareImageWithText(pagoMovilListTrue!!.imagen!!, texto)
            }

            Triple(true, false, true) -> {
                val texto = crearTextoCapture(enviarDatosPM,enviarImagenP, enviarMontoP,  pagoMovilListTrue, montoPersonalizado)
                // CAMBIO: Se reemplaza la captura de pantalla por la generación del recibo.
                val imagePath = generarReciboYObtenerPath()
                if (imagePath != null) {
                    shareImageWithText(imagePath, texto)
                } else {
                    shareText(texto) // Si no se pudo generar imagen (ej. no hay montos), comparte solo texto.
                }
            }

            Triple(true, false, false) -> {
                val texto = crearTextoCapture(enviarDatosPM, enviarImagenP, enviarMontoP, pagoMovilListTrue, montoPersonalizado)
                // CAMBIO: Se reemplaza la captura de pantalla por la generación del recibo.
                val imagePath = generarReciboYObtenerPath()
                if (imagePath != null) {
                    shareImageWithText(imagePath, texto)
                } else {
                    shareText(texto)
                }
            }

            Triple(false, true, true) -> {
                val texto = crearTextoCapture(enviarDatosPM, enviarImagenP, enviarMontoP, pagoMovilListTrue, montoPersonalizado)
                shareImageWithText(pagoMovilListTrue!!.imagen!!, texto)
            }

            Triple(false, true, false) -> {
                val texto = crearTextoCapture(enviarDatosPM, enviarImagenP, enviarMontoP, pagoMovilListTrue, montoPersonalizado)
                shareImageWithText(pagoMovilListTrue!!.imagen!!, texto)
            }

            Triple(false, false, true) -> {
                val texto = crearTextoCapture(enviarDatosPM, enviarImagenP, enviarMontoP,  pagoMovilListTrue, montoPersonalizado)
                // CAMBIO: Se reemplaza la captura de pantalla por la generación del recibo.
                val imagePath = generarReciboYObtenerPath()
                if (imagePath != null) {
                    shareImageWithText(imagePath, texto)
                } else {
                    shareText(texto)
                }
            }

            Triple(false, false, false) -> {
                val texto = crearTextoCapture(enviarDatosPM, enviarImagenP, enviarMontoP,  pagoMovilListTrue, montoPersonalizado)
                // CAMBIO: Se reemplaza la captura de pantalla por la generación del recibo.
                val imagePath = generarReciboYObtenerPath()
                Log.d(TAG, "shareLogic: imagePath $imagePath")
                if (imagePath != null) {
                    shareImageWithText(imagePath, texto)
                } else {
                    shareText(texto)
                }
            }
        }
    }


// ... (tus imports)
private fun generarReciboYObtenerPath(): String? {
    try {
        val currentFragmentTag = getCurrentFragmentTag()

        val imageCacheDir = File(cacheDir, "images")
        imageCacheDir.mkdirs()

        return when (currentFragmentTag) {
            "Dolar al Dia" -> {
                val fragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) ?: return null
                val tasaNombre = verificarTasa(fragment)
                val tasaValorView = when(tasaNombre) {
                    "BCV" -> fragment.view?.findViewById<ToggleButton>(R.id.btnBcv)
                    "Euro" -> fragment.view?.findViewById<ToggleButton>(R.id.btnEuroP)
                    else -> null
                } ?: return null

                // La tasa se mantiene como un string original
                val tasaValorStr = tasaValorView.text.toString()
                val fechaTasa = fragment.view?.findViewById<TextView>(R.id.txtFechaActualizacionBcv)?.text.toString()
                val inputMontoDolares = fragment.view?.findViewById<EditText>(R.id.inputDolares)?.text.toString()
                val inputMontoBolivares = fragment.view?.findViewById<EditText>(R.id.inputBolivares)?.text.toString()

                var finalMontoDolares: String
                var finalMontoBolivares: String

                if (inputMontoDolares.isBlank() || inputMontoBolivares.isBlank()) {
                    finalMontoDolares = formatNumberForReceipt("1.00")
                    val tasaDouble = tasaValorStr.replace(Regex("[^0-9,.]"), "").replace(",", ".").toDoubleOrNull()
                    finalMontoBolivares = if (tasaDouble != null) {
                        val bolivaresCalculados = 1.0 * tasaDouble
                        formatNumberForReceipt(bolivaresCalculados.toString())
                    } else {
                        // Si la tasa no se puede convertir, no la podemos usar para calcular
                        "0,00"
                    }
                } else {
                    finalMontoDolares = formatNumberForReceipt(inputMontoDolares)
                    finalMontoBolivares = formatNumberForReceipt(inputMontoBolivares)
                }

                val inflater = LayoutInflater.from(this)
                val receiptView = inflater.inflate(R.layout.receipt_layout, null)

                // CAMBIO: Usamos la variable original `tasaValorStr` sin formatear
                receiptView.findViewById<TextView>(R.id.tv_tasa_valor).text = "Bs. $tasaValorStr"
                receiptView.findViewById<TextView>(R.id.tv_tasa_nombre).text = "(Tasa $tasaNombre)"
                receiptView.findViewById<TextView>(R.id.tv_monto_dolares).text = "$ $finalMontoDolares"
                receiptView.findViewById<TextView>(R.id.tv_monto_bolivares).text = "Bs. $finalMontoBolivares"
                receiptView.findViewById<TextView>(R.id.tv_fecha_actualiza).text = fechaTasa

                val receiptBitmap = viewToBitmap(receiptView)

                val imageFile = File(imageCacheDir, "recibo_generado.png")
                FileOutputStream(imageFile).use { fos ->
                    receiptBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
                imageFile.absolutePath
            }

            "Pago Movil" -> {

                val logoDrawable = ContextCompat.getDrawable(this, R.drawable.logoredondo) ?: return null
                val logoBitmap = drawableToBitmap(logoDrawable)
                val imageFile = File(imageCacheDir, "logo_share.png")
                FileOutputStream(imageFile).use { fos ->
                    logoBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
                imageFile.absolutePath
            }

            else -> {

                capturarPantalla()
            }
        }

    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}


    // FORMATEA EL MOTON PARA EL RECIBO
    private fun formatNumberForReceipt(numberString: String): String {
        if (numberString.isBlank()) return "0,00"

        // 1. Detectar el formato de entrada (punto como miles o como decimal)
        val hasPoint = numberString.contains('.')
        val hasComma = numberString.contains(',')

        val numberToParse: String
        if (hasPoint && hasComma) {
            // Formato complejo como "1.234,56" o "1,234.56"
            val lastPoint = numberString.lastIndexOf('.')
            val lastComma = numberString.lastIndexOf(',')
            numberToParse = if (lastPoint > lastComma) {
                // El punto es el decimal: "1,234.56"
                numberString.replace(",", "")
            } else {
                // La coma es el decimal: "1.234,56"
                numberString.replace(".", "").replace(',', '.')
            }
        } else if (hasPoint) {
            // Solo tiene puntos: "100.2" o "1.000"
            val lastPoint = numberString.lastIndexOf('.')
            if (numberString.length - 1 - lastPoint == 3 && numberString.count { it == '.' } > 1) {
                // Probablemente separador de miles: "1.000.000"
                numberToParse = numberString.replace(".", "")
            } else {
                // Probablemente decimal: "100.2"
                numberToParse = numberString
            }
        } else if (hasComma) {
            // Solo tiene comas, asumimos que es decimal
            numberToParse = numberString.replace(',', '.')
        } else {
            // No tiene separadores, es un número entero
            numberToParse = numberString
        }

        // 2. Intentar convertir la cadena limpia a un Double
        val number = numberToParse.toDoubleOrNull() ?: return numberString // Si falla, devuelve el original

        // 3. Formatear el Double al formato de salida deseado
        val symbols = DecimalFormatSymbols(Locale.GERMANY) // Usa punto para miles, coma para decimales
        val formatter = DecimalFormat("#,##0.00", symbols)

        return formatter.format(number)
    }


    private fun viewToBitmap(view: View): Bitmap {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }



    // FUNCIÓN AUXILIAR PARA CONVERTIR UN DRAWABLE A BITMAP
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    //**********************************************************


}