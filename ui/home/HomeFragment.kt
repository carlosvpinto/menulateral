package com.carlosv.dolaraldia.ui.home


import android.annotation.SuppressLint
import com.carlosv.dolaraldia.utils.ShakeDetector
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
//import com.carlosv.dolaraldia.databinding.FragmentHomeBinding
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.ScaleAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.carlosv.dolaraldia.ApiService

import com.carlosv.dolaraldia.provider.ImagenProvider
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentHomeBinding
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.security.KeyManagementException
import java.text.DecimalFormat
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.*
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.carlosv.dolaraldia.AESCrypto
import com.carlosv.dolaraldia.MyApplication
import com.carlosv.dolaraldia.model.FCMBody
import com.carlosv.dolaraldia.model.FCMResponse
import com.carlosv.dolaraldia.model.apicontoken.ApiConTokenResponse
import com.carlosv.dolaraldia.model.apicontoken2.ApiModelResponseCripto
import com.carlosv.dolaraldia.model.apicontoken2.ApiModelResponseBCV
import com.carlosv.dolaraldia.model.apimercantil.ApiResponse

import com.carlosv.dolaraldia.model.apimercantil.busqueda.MerchantIdentify
import com.carlosv.dolaraldia.model.apimercantil.busqueda.MobileInfo
import com.carlosv.dolaraldia.model.apimercantil.busqueda.MobilePaymentSearchRequest
import com.carlosv.dolaraldia.model.apimercantil.busqueda.SearchBy
import com.carlosv.dolaraldia.model.apimercantil.busqueda.ClientIdentify
import com.carlosv.dolaraldia.model.apimercantil.busqueda.Location
import com.carlosv.dolaraldia.model.clickAnuncios.ClickAnunicosModel
import com.carlosv.dolaraldia.model.controlPublicidad.ConfigImagenModel
import com.carlosv.dolaraldia.model.controlPublicidad.ImprecionesArtiModel
import com.carlosv.dolaraldia.model.history.HistoryModelResponse

import com.carlosv.dolaraldia.provider.ClickAnuncioProvider
import com.carlosv.dolaraldia.provider.RegistroPubliProvider
import com.carlosv.dolaraldia.services.NotificationProvider
import com.carlosv.dolaraldia.utils.Constants
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.Calendar
import java.time.LocalTime
import java.time.LocalDate
import java.time.DayOfWeek


import com.google.firebase.messaging.RemoteMessage





class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null")
    private var isFragmentAttached: Boolean = false
    private lateinit var shakeDetector: ShakeDetector
    private var snackbar: Snackbar? = null
    private var snackbarInfo: Snackbar? = null


    var url: String? = null
    var nombreAnuncio: String? = null
    var pagina: String? = null
    var linkAfiliado: String? = null
    private val imagenConfProvider = ImagenProvider()
    private val registroPublicidad = RegistroPubliProvider()

    private val clickAnuncioProvider = ClickAnuncioProvider()
    private var imageConfigListener: ListenerRegistration? = null
    private var configImageModels = ArrayList<ConfigImagenModel>()

    private var valorActualParalelo: Double? = 0.0
    private var ultimoTecleado: Int? = 0
    var numeroNoturno = 0
    lateinit var mAdView: AdView

    private lateinit var layout: LinearLayout
    private var diferenciaDolares = 0.0
    private var diferenciaBs = 0.0

    private var repeatCount = 0

    // Para Admob Reguard

    private var TAG = "HomeFragment"


    private var interstitial: InterstitialAd? = null

    private var imageSlider: ImageSlider? = null

    lateinit var navigation: BottomNavigationView

    lateinit var ResponseDelBCv: ApiConTokenResponse
    lateinit var resposeVerificar: ApiConTokenResponse
    private var visibleLayoutProxBcv = 0

    private val notificationProvider = NotificationProvider()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //ImagenSlider
        imageSlider = binding.imageSlider
        shakeDetector = ShakeDetector(requireContext()) {
            // onShakeDetected()
        }
        visibleLayoutProxBcv += 1
        //  llamarDolarApiNew()
        llamarApiCriptoDolar()
        llamarApiPaginaBCV()

        //llama al end Points que actualiza el BCV a las 4pm
        llamarDolarBcvAdelantado()



        MobileAds.initialize(requireContext()) {}
        // cargarImagendelConfig()


        //PARA CARGAR ADMOB
        layout = binding.linearLayout3
        mAdView = binding.adView
        // mAdView = findViewById(R.id.adView)
        try {
            val adRequest = AdRequest.Builder().build()
            mAdView?.loadAd(adRequest)

        } catch (e: Exception) {
            Log.e("AdMob", "Error al cargar el anuncio mandado por la app: ${e.localizedMessage}")
            FirebaseCrashlytics.getInstance().recordException(e)
        }

//******************************************

        //initializeMobileAdsSdk(requireContext())

        //**************************************


//        // Inicializa AdMob
//
//        initAds()
//        initListeners()
//        binding.btnImageColaboracion.setOnClickListener {
//            showAds()
//            count = 0
//            initAds()
//        }
//
//
//        initListeners()

        // Obtén una referencia a SharedPreferences
        val sharedPreferences =
            requireContext().getSharedPreferences("MiPreferencia", AppCompatActivity.MODE_PRIVATE)
        // Obtener referencia a SharedPreferences

        // Recuperar el valor entero
        numeroNoturno = sharedPreferences.getInt("numero_noturno", 0)

        //VERIFICA SI QUE MEDO TIENE GUARDADO
        // setDayNight(modoDark())
        binding.swipeRefreshLayout.setOnRefreshListener {
            comenzarCarga()

//            eliminarListener()
            llamarDolarApiNew { isSuccessful ->

                // Solo habilitar el botón si ambas APIs responden
                if (isSuccessful) {
                    finalizarCarga()
                } else {
                    finalizarCarga()
                }
            }

            actualizarEuro()


            //PARA PUBLICIDAD INTERNA*******
            //listenerImagenConfig()
            //******************************

        }
        binding.btnprobar.setOnClickListener {
            // probarencryptado()
            // realizarBusquedaMovil2()
            // realizarBusqueda2()

            // llamdaApiMercantil()
            // sendPaymentRequest()

            sendNotification()

        }
        binding.btnRefres.setOnClickListener {
            comenzarCarga()
            llamarDolarApiNew { isSuccessful ->

                // Solo habilitar el botón si ambas APIs responden
                if (isSuccessful) {
                    finalizarCarga()
                } else {
                    finalizarCarga()
                }
            }

            actualizarEuro()


        }

        binding.imgVerDifBs.setOnClickListener {
            calcularDiferencia()
            //showCustomSnackbar("nada")
        }

        binding.btnBcv.setOnClickListener {
            activarBtnBcv()
            val valorDolar = binding.btnBcv.text.toString()

            val doubleValue = try {
                valorDolar.toDouble()
            } catch (e: NumberFormatException) {
                null // Si la conversión falla, asigna null
            }

            // Actualiza la multiplicación con el valor convertido o 0.0 si la conversión falla
            actualzarMultiplicacion(doubleValue ?: 0.0)

        }
        binding.btnParalelo.setOnClickListener {
            activarBtnParalelo()
            val valorDolar = binding.btnParalelo.text.toString()

            val doubleValue = try {
                valorDolar.toDouble()
            } catch (e: NumberFormatException) {
                null // Si la conversión falla, asigna null
            }

            // Actualiza la multiplicación con el valor convertido o 0.0 si la conversión falla
            actualzarMultiplicacion(doubleValue ?: 0.0)
        }
        binding.btnPromedio.setOnClickListener {
            activarBtnPromedio()
            val valorDolar = binding.btnPromedio.text.toString()
            val doubleValue = try {
                valorDolar.toDouble()
            } catch (e: NumberFormatException) {
                null // Si la conversión falla, asigna null
            }

            // Actualiza la multiplicación con el valor convertido o 0.0 si la conversión falla
            actualzarMultiplicacion(doubleValue ?: 0.0)
        }
        binding.imgCopyDolar.setOnClickListener {
            copiarDolar()
        }
        binding.imgCoyBolivar.setOnClickListener {
            copiarBs()
        }

        binding.imglogo.setOnClickListener {
            // crearImagenUrl() creo objeto para imagen
        }

        binding.imgVPublicidad.setOnClickListener {
            guardarClickAnuncio()
            irAlArticulo()

        }
        binding.imgCerrarAnuncio.setOnClickListener {
            // binding.layoutCerraAnun.visibility= View.GONE
            binding.LnerPubliImagen.visibility = View.GONE
        }
        binding.SwUtimaAct.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val resposeGuardadoApiAdelantado = ApiResponseHolder.getResponseOriginal()
                cambioSwictValor(resposeGuardadoApiAdelantado)

            } else {

                val responseAlCambio = ApiResponseHolder.getResponseApiAlCambio()
                cambioSwictValor(responseAlCambio)
            }
        }

        //PARA ACTUALIZAR EL PRECIO DEL DOLAR SOLO CUANDO CARGA POR PRIMERA VEZ
        if (savedInstanceState == null) {

            disableSSLVerification()

            actualizarEuro()
        }


        // Aplicar la animación
        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.appear_from_top)
        root.startAnimation(animation)

        //***********************
        return root
    }


    //NOTIFICACIONES PUSH
    private fun sendNotification() {
        val map = HashMap<String, String>()
        map.put("title", "SOLICITUD DE VIAJE")
        map.put(
            "body",
            "Un cliente esta solicitando un viaje a "
        )

        map.put("token", Constants.TOKEN_AS21)

        val body = FCMBody(
            to = Constants.TOKEN_AS21,
            priority = "high",
            ttl = "4500s",
            data = map
        )

        Log.d(TAG, "sendNotification:body: $body map $map ")

        notificationProvider.sendNotification(body).enqueue(object : Callback<FCMResponse> {
            override fun onResponse(call: Call<FCMResponse>, response: Response<FCMResponse>) {
                Log.d(TAG, "onResponse: $response response.body(): ${response.body()} ")
                if (response.body() != null) {

                    if (response.body()!!.success == 1) {
                        Toast.makeText(
                            requireContext(),
                            "Se envio la notificacion",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "No se pudo enviar la notificacion",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "hubo un error enviando la notificacion",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<FCMResponse>, t: Throwable) {
                Log.d("NOTIFICATION", "ERROR Notificacion: ${t.message}")
            }

        })
    }


    fun finalizarCarga() {
        // Habilitar el botón nuevamente
        binding.btnRefres.isEnabled = true
        binding.btnRefres.visibility = View.VISIBLE
        // Ocultar el ProgressBar
        binding.progressBarBoton.visibility = View.GONE
    }

    fun comenzarCarga() {
        // Habilitar el botón nuevamente
        binding.btnRefres.isEnabled = false
        binding.btnRefres.visibility = View.GONE

        // Ocultar el ProgressBar
        binding.progressBarBoton.visibility = View.VISIBLE
    }

    //Muestra el reposense segun seal el Swict
    private fun cambioSwictValor(responseMostrar: ApiConTokenResponse?) {

        if (responseMostrar != null) {
            llenarCampoBCVNew(responseMostrar)
            llenarCampoPromedio(responseMostrar)
            val valorDolar = valorBotonActivo()
            // Actualiza la multiplicación con el valor
            animacionCrecerBoton(
                binding.btnBcv,
                binding.btnPromedio,
                binding.txtFechaActualizacionBcv
            )
            actualzarMultiplicacion(valorDolar)
        }

    }


    //Devuelve el Valor del Boton Activo
    private fun valorBotonActivo(): Double {
        val btnBcv = binding.btnBcv
        val btnParalelo = binding.btnParalelo
        val btnPromedio = binding.btnPromedio

        // Función para convertir el texto a Double si es posible, o devolver 0.0
        fun parseToDouble(text: String): Double {
            return try {
                text.toDouble()
            } catch (e: NumberFormatException) {
                Log.e("HomeFragment", "El valor no es numérico: $text")

                // Registrar el fallo en Firebase Crashlytics
                FirebaseCrashlytics.getInstance().log("Error al convertir texto a Double: $text")
                FirebaseCrashlytics.getInstance().recordException(e)

                0.0 // Devolver un valor por defecto si no es numérico
            }
        }

        return when {
            btnBcv.isChecked -> parseToDouble(btnBcv.text.toString())
            btnParalelo.isChecked -> parseToDouble(btnParalelo.text.toString())
            btnPromedio.isChecked -> parseToDouble(btnPromedio.text.toString())
            else -> 0.0
        }
    }


    private fun guardarClickAnuncio() {
        try {
            val nombreAnuncio = nombreAnuncio
            val uri = url

            val clickAnuncioModel = ClickAnunicosModel(

                articulo = nombreAnuncio,
                date = Date(),
                timestamp = Date().time,
                pagina = pagina


            )

            clickAnuncioProvider.create(clickAnuncioModel).addOnCompleteListener { it ->
                if (it.isSuccessful) {

                    // Toast.makeText(requireContext(), "GUARDANDO DATOS DEL CLICK", Toast.LENGTH_LONG).show()

                } else {
                    Log.d(TAG, "crearImagenUrl: ${it.exception}")
                    Toast.makeText(requireContext(), "Error ${it.exception}", Toast.LENGTH_LONG)
                        .show()
                }
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Error en datos $e", Toast.LENGTH_SHORT).show()
        }
    }


    private fun irAlArticulo() {
        if (!linkAfiliado.isNullOrEmpty()) {


            val aliexpressPackage = "com.alibaba.aliexpresshd"
            val amazonPackage = "com.amazon.mShop.android.shopping"
            val packageManager = requireActivity().packageManager

            // Intent implícito para abrir el enlace en un navegador web
            val intentWeb = Intent(Intent.ACTION_VIEW, Uri.parse(linkAfiliado))

            // Verifica si AliExpress está instalado
            val isAliExpressInstalled = try {
                packageManager.getPackageInfo(aliexpressPackage, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }

            // Verifica si Amazon está instalado
            val isAmazonInstalled = try {
                packageManager.getPackageInfo(amazonPackage, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }

            // Intent explícito para abrir el enlace en la app de AliExpress
            if (isAliExpressInstalled) {
                val intentAliExpress = Intent(Intent.ACTION_VIEW, Uri.parse(linkAfiliado))
                intentAliExpress.`package` = aliexpressPackage

                // Verifica si hay alguna actividad que pueda manejar este intent
                if (intentAliExpress.resolveActivity(packageManager) != null) {
                    startActivity(intentAliExpress)
                    return
                }
            }

            // Intent explícito para abrir el enlace en la app de Amazon
            if (isAmazonInstalled) {
                val intentAmazon = Intent(Intent.ACTION_VIEW, Uri.parse(linkAfiliado))
                intentAmazon.`package` = amazonPackage

                // Verifica si hay alguna actividad que pueda manejar este intent
                if (intentAmazon.resolveActivity(packageManager) != null) {
                    startActivity(intentAmazon)
                    return
                }
            }

            // Si ninguna de las aplicaciones está instalada o no pueden manejar el intent, abre en navegador
            startActivity(intentWeb)
        } else {
            Log.d(TAG, "irAlArticulo: linkAfiliado está vacío o es nulo.")
        }
    }


    private fun techadoDesplegado(): Boolean {

        // Obtén el contexto de la actividad
        val context: Context = requireContext()

        // Verifica si el teclado está desplegado
        val inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val isKeyboardVisible = inputMethodManager.isActive

        if (isKeyboardVisible) {
            Log.d(TAG, "onCreateView: El teclado está desplegado")  // El teclado está desplegado
            // Realiza las acciones que necesites
            return true
        } else {
            // El teclado no está desplegado
            Log.d(TAG, "onCreateView:El teclado no está desplegado ")
            // Realiza las acciones que necesites
            return false
        }

    }

    private fun initializeMobileAdsSdk(context: Context) {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(context) {}

        // Load an ad.
        (requireActivity().application as MyApplication).loadAd(context)
    }

    //PUBLICIDAD INTERNA*****************************

    //    private fun irAlArticulo() {
//        if(linkAfiliado.isNullOrEmpty()){
//            // URL que quieres abrir cuando se hace clic en la imagen
//            //  url = "https://www.ejemplo.com/tu_link_de_afiliado"
//            Log.d(TAG, "irAlArticulo: URl: $url")
//            // Crea un intent implícito para abrir la URL en un navegador web
//            val intent = Intent(Intent.ACTION_VIEW)
//            intent.data = Uri.parse(linkAfiliado)
//            startActivity(intent)
//            // Comprueba si hay aplicaciones que pueden manejar este intento
//            if (intent.resolveActivity(requireActivity().packageManager) != null) {
//                // Abre la URL en un navegador web
//
//            }
//        }
//
//    }

//      ESCUCHA SI HAY UNA IMAGEN PARA PUBLICIDAD INTERNA
//    private fun listenerImagenConfig() {
//        //************************
//        imageConfigListener = imagenConfProvider.getAllImagenConfig()
//            .addSnapshotListener { querySnapshot, error ->
//                if (error != null) {
//                    return@addSnapshotListener
//                }
//
//                if (querySnapshot != null) {
//
//                    configImageModels.clear() // Limpiar la lista antes de agregar los nuevos elementos
//
//
//                    if (querySnapshot.documents.size > 0) {
//                        val documents = querySnapshot.documents
//
//                        for (d in documents) {
//                            var imageConfig = d.toObject(ConfigImagenModel::class.java)
//                            imageConfig?.id = d.id
//                            configImageModels.add(imageConfig!!)
//
//
//                        }
//                        if (configImageModels.isNotEmpty()) {
//                            // Accede al primer elemento del ArrayList
//                            // Generar un número aleatorio entre el rango de índices del array
//                            val randomIndex = (0 until configImageModels.size).random()
//                            val fecha = Date().toString()
//                            val id = configImageModels[randomIndex].id
//                            url = configImageModels[randomIndex].url
//                            url2= configImageModels[randomIndex].url2
//                            url3= configImageModels[randomIndex].url3
//                            nombreAnuncio = configImageModels[randomIndex].nombre
//                            linkAfiliado = configImageModels[randomIndex].linkAfiliado
//                            pagina = configImageModels[randomIndex].pagina
//                            Log.d(TAG, "listenerImagenConfig: url: $url, nombreAnuncio $nombreAnuncio, linkAfaliado: $linkAfiliado pagina: $pagina")
//
//                            cargarImagen(url,nombreAnuncio, id!!,fecha)
//                            //cargarImagenSlider(url,url2,url3)
//
//                        } else {
//                            binding.LnerPubliImagen.visibility = View.INVISIBLE
//                        }
//
//                    } else {
//                        Log.d(TAG, "listenerpagomovil: sin datos de imagen")
//                    }
//
//
//                }
//            }
//    }

    private fun cargarImagenSlider(url: String?, url2: String?, url3: String?) {

        //agregar imagen a la lista
        val imageList = ArrayList<SlideModel>()
        imageList.add(SlideModel(url, ScaleTypes.CENTER_CROP))
        if (url2 != null) {
            imageList.add(SlideModel(url2, ScaleTypes.CENTER_CROP))
        }
        if (url3 != null) {
            imageList.add(SlideModel(url3, ScaleTypes.CENTER_CROP))
        }
        Log.d(TAG, "cargarImagenSlider: url:$url  url2:$url2  url3:$url3")
        Log.d(TAG, "cargarImagenSlider: imageList $imageList ")
        imageSlider?.setImageList(imageList)
    }


//    private fun eliminarListener() {
//        imageConfigListener?.remove()
//    }

    //Contabiliza la veces que es Visible una Publicidad
    private fun crearImpresion(id: String, nombreArticulo: String?, fecha: String?) {
        Log.d(TAG, "crearImpresion: nombreArticulo $nombreArticulo")
        lifecycleScope.launch {
            try {
                val imprecionesArtiModel = ImprecionesArtiModel(
                    id = id,
                    nombre = nombreArticulo,
                    numeroImpresiones = 1,
                    fecha = fecha,
                    numeroClic = 1,
                )

                try {
                    val documentReference =
                        registroPublicidad.createWithCoroutines(imprecionesArtiModel)
                    Log.d(TAG, "crearImpresion: documentReference $documentReference")
                    Toast.makeText(
                        requireContext(),
                        "Datos Enviados para Validar",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Log.d(TAG, "crearImagenUrl: $e")
                    Toast.makeText(requireContext(), "Error al crear los datos", Toast.LENGTH_LONG)
                        .show()
                }
            } catch (e: NumberFormatException) {
                Toast.makeText(requireContext(), "Error en datos $e", Toast.LENGTH_SHORT).show()
            }
        }
    }


    //CRE UN ARCHIVO PARA LA PUBLICIDAD INTERNA DE LA APP
    private fun crearImagenUrl() {


        try {
            val uri = "https://loremflickr.com/g/320/240/paris,girl/all"
            val linkAfiliado = "https://www.google.com"

            val imagenConfigModel = ConfigImagenModel(
                url = uri,
                linkAfiliado = linkAfiliado,
                date = Date()
            )

            imagenConfProvider.create(imagenConfigModel).addOnCompleteListener { it ->
                if (it.isSuccessful) {

                    Toast.makeText(
                        requireContext(),
                        "Datos Enviados para Validar",
                        Toast.LENGTH_LONG
                    ).show()

                } else {
                    Toast.makeText(requireContext(), "Error al crear los datos", Toast.LENGTH_LONG)
                        .show()
                }
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Error en datos $e", Toast.LENGTH_SHORT).show()
        }
    }

    //Carga la Imagen para la piblicidad Interna de la app
    private fun cargarImagen(uri: String?, nombreAnun: String?, id: String, fecha: String?) {
        if (uri != "") {
            binding.LnerPubliImagen.visibility = View.VISIBLE
            //para crear imprecion de publicidad
            // crearImpresion(id,nombreAnun,fecha )
            Glide.with(this)
                .load(uri)
                .listener(object : RequestListener<Drawable> {

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: com.bumptech.glide.request.target.Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean,
                    ): Boolean {
                        //binding.LnerPubliImagen.visibility = View.VISIBLE
                        binding.layoutCerraAnun.visibility = View.VISIBLE

                        return false
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean,
                    ): Boolean {
                        return false
                    }
                })
                .into(binding.imgVPublicidad)
        } else {
            Log.d(TAG, "cargarImagen: URI Vacio")
        }
    }


    //INTERFACE PARA COMUNICAR CON EL ACTIVITY
    object ApiResponseHolder {
        private var response: ApiConTokenResponse? = null
        private var responseApiNew: ApiConTokenResponse? = null
        private var responseApiOriginal: ApiConTokenResponse? = null
        private var responseApiNew2: ApiModelResponseCripto? = null
        private var responseApiBancoNew2: ApiModelResponseBCV? = null
        private var responseHistoryBcv: HistoryModelResponse? = null
        private var responseHistoryParalelo: HistoryModelResponse? = null


        private const val VALOR_EURO = "ValorEuro"
        private const val NUMERO_EURO = "euro"
        private const val FECHA_EURO = "fecha"
        private const val VALOR_DOLAR = "ValorDolar"
        private const val NUMERO_DOLAR = "ValorEuro"


        fun getResponse(): ApiConTokenResponse? {
            return responseApiNew
        }

        fun getResponseOriginal(): ApiConTokenResponse? {
            return responseApiOriginal
        }


        fun getResponseApiAlCambio(): ApiConTokenResponse? {
            return responseApiNew
        }


        fun setResponse(newResponse: ApiConTokenResponse) {
            responseApiNew = newResponse
        }

        fun setResponseOriginal(newResponse: ApiConTokenResponse) {
            responseApiOriginal = newResponse
        }

        fun setResponseApiAlCambio(newResponse: ApiConTokenResponse) {
            responseApiNew = newResponse
        }

        fun setResponseHistory(newResponse: ApiConTokenResponse) {
            responseApiNew = newResponse
        }

        fun setResponseCripto(newResponse: ApiModelResponseCripto) {
            responseApiNew2 = newResponse
        }

        fun setResponseBCV(newResponse: ApiModelResponseBCV) {
            responseApiBancoNew2 = newResponse
        }

        fun getResponseApiCripto(): ApiModelResponseCripto? {
            return responseApiNew2
        }

        fun getResponseApiBancoBCV(): ApiModelResponseBCV? {
            return responseApiBancoNew2
        }

        fun setResponseHistoryBcv(ResponseHistory: HistoryModelResponse) {
            responseHistoryBcv = ResponseHistory
        }

        fun setResponseHistoryParalelo(ResponseHistory: HistoryModelResponse) {
            responseHistoryParalelo = ResponseHistory
        }

        fun getResponseHistoryBcv(): HistoryModelResponse? {
            return responseHistoryBcv
        }

        fun getResponseHistoryParalelo(): HistoryModelResponse? {
            return responseHistoryParalelo
        }


        fun guardarEuro(context: Context, numero: Float) {
            val prefs: SharedPreferences =
                context.getSharedPreferences(VALOR_EURO, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putFloat(NUMERO_EURO, numero)
            editor.apply()
        }

        fun recuperarEuro(context: Context): Float {
            val prefs: SharedPreferences =
                context.getSharedPreferences(VALOR_EURO, Context.MODE_PRIVATE)
            return prefs.getFloat(
                NUMERO_EURO,
                0.0f
            ) // 0 es el valor predeterminado si no se encuentra el número
        }

        fun guadarDolar(context: Context, numero: Float) {
            val prefs: SharedPreferences =
                context.getSharedPreferences(VALOR_DOLAR, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putFloat(NUMERO_DOLAR, numero)
            editor.apply()
        }


        fun recuperarDolar(context: Context): Float {
            val prefs: SharedPreferences =
                context.getSharedPreferences(VALOR_DOLAR, Context.MODE_PRIVATE)
            return prefs.getFloat(
                NUMERO_DOLAR,
                0.0f
            ) // 0 es el valor predeterminado si no se encuentra el número
        }

        fun guardarEuroFecha(context: Context, fecha: String) {
            val prefs: SharedPreferences =
                context.getSharedPreferences(VALOR_EURO, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString(FECHA_EURO, fecha)
            editor.apply()
        }

        fun recuperarEuroFecha(context: Context): String? {
            val prefs: SharedPreferences =
                context.getSharedPreferences(VALOR_EURO, Context.MODE_PRIVATE)
            return prefs.getString(FECHA_EURO, null)
        }

    }

    private fun initListeners() {
        interstitial?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {

            }

            override fun onAdShowedFullScreenContent() {
                interstitial = null
            }
        }
    }

    private fun initAds() {
        var adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            requireContext(),
            "ca-app-pub-5303101028880067/5189629725",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    interstitial = interstitialAd
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    interstitial = null
                }
            })
    }


    private fun showAds() {
        interstitial?.show(requireActivity())
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        isFragmentAttached = true
    }

    override fun onDetach() {
        super.onDetach()
        isFragmentAttached = false
    }

    //SCRAPING PARA WW.BCV **************************************************


    //**********************************************************************************

    private fun actualizarEuro() {
        try {
            // Recuperar el valor del euro guardado
            val savedEuro = ApiResponseHolder.recuperarEuro(requireContext())

            // Actualizar el valor del euro desde la web
            CoroutineScope(Dispatchers.IO).launch {
                var intentos = 0
                val maxIntentos = 3
                var obtenido = false
                while (!obtenido && intentos < maxIntentos) {
                    try {
                        val document = Jsoup.connect("https://www.bcv.org.ve/").timeout(60000).get()
                        val precioEuro = document.select("#euro strong").first()?.text()
                        val valorEuro = precioEuro?.replace(",", ".")?.toFloatOrNull()
                        val precioDolarBcv = document.select("#dolar strong").first()?.text()
                        val valorDolar = precioDolarBcv?.replace(",", ".")?.toFloatOrNull()


                        // Extraer la fecha del elemento span con la clase date-display-single
                        val fechaElement = document.select("span.date-display-single").firstOrNull()
                        val fecha = fechaElement?.text()

                        withContext(Dispatchers.Main) {
                            if (isAdded) { // Verifica si el Fragment está adjunto
                                if (valorEuro != null) {
                                    // Guardar el nuevo valor del euro y la fecha de actualización
                                    if (valorDolar != null) {
                                        ApiResponseHolder.guadarDolar(requireContext(), valorDolar)
                                    }

                                    ApiResponseHolder.guardarEuro(requireContext(), valorEuro)
                                    ApiResponseHolder.guardarEuroFecha(
                                        requireContext(),
                                        fecha.toString()
                                    )


                                    // Marcar como obtenido y salir del bucle
                                    obtenido = true
                                } else {
                                    Log.d(TAG, "actualizarEuro: en false!!")
                                    Toast.makeText(
                                        requireContext(),
                                        "Problemas de Internet. No se actualizó el Euro",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Log.d(
                            "EUROACTU",
                            "ERRRRORRRR $e "
                        )
                    }

                    // Incrementar el contador de intentos
                    intentos++

                    // Esperar un tiempo antes de realizar el próximo intento
                    delay(10000) // Esperar 10 segundos antes de volver a intentar obtener el precio del euro
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Problemas de Conexión $e", Toast.LENGTH_SHORT).show()
        }
    }

    //PARA LA SEGURIDAD*****************
    fun disableSSLVerification() {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(
                    chain: Array<out X509Certificate>?,
                    authType: String?,
                ) {
                }

                override fun checkServerTrusted(
                    chain: Array<out X509Certificate>?,
                    authType: String?,
                ) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())

            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        }
    }


    private fun actualzarMultiplicacion(valorActualDolar: Double?) {

        if (ultimoTecleado == 0) {
            val decimalFormat = DecimalFormat("#,##0.00") // Declaración de DecimalFormat
            var valorDolares = 0.0
            val inputPDolar = binding.inputDolares.text.toString()
            Log.d(
                TAG,
                "actualzarMultiplicacion: valor valorActualDolar $valorActualDolar ultimoTecleado $ultimoTecleado"
            )
            if (inputPDolar.isNotEmpty()) {

                try {
                    if (valorActualDolar != null) {
                        val precioSincoma =
                            inputPDolar.replace("[,]".toRegex(), "") // Elimina puntos y comas
                        valorDolares = precioSincoma.toDouble() * valorActualDolar.toDouble()
                    }
                    val formattedValorDolares = decimalFormat.format(valorDolares)
                    binding.inputBolivares.setText(formattedValorDolares)
                } catch (e: NumberFormatException) {
                    Log.d("Multiplicacion", "actualzarMultiplicacion: $e")
                }

            } else {
                binding.inputBolivares.text?.clear()
            }
        }
        if (ultimoTecleado == 1) {

            val decimalFormat = DecimalFormat("#,##0.00") // Declaración de DecimalFormat
            var valorDolares = 0.0
            var diferenciaDolares = 0.0
            val inputText = binding.inputBolivares.text.toString()
            if (inputText.isNotEmpty()) {

                try {
                    if (valorActualDolar != null) {
                        val precioSincoma =
                            inputText.replace("[,]".toRegex(), "") // Elimina puntos y comas
                        valorDolares = precioSincoma.toDouble() / valorActualDolar.toDouble()
                    }
                    val formattedValorDolares = decimalFormat.format(valorDolares)
                    binding.inputDolares.setText(formattedValorDolares)
                } catch (e: NumberFormatException) {
                    Log.d("Multiplicacion", "actualzarMultiplicacion: $e")
                }

            } else {
                binding.inputDolares.text?.clear()
            }

        }

    }

    private fun activarBtnPromedio() {
        if (binding.btnPromedio.isChecked == true) {

            binding.btnParalelo.isChecked = false
            binding.btnBcv.isChecked = false
            binding.seekBar.progress = 1
            deshabilitarSeekBar()

        } else {
            binding.btnPromedio.isChecked = true
            binding.btnParalelo.isChecked = false
            binding.btnBcv.isChecked = false
            binding.seekBar.progress = 1
            deshabilitarSeekBar()

        }
    }

    private fun activarBtnBcv() {
        if (binding.btnBcv.isChecked == true) {

            binding.btnParalelo.isChecked = false
            binding.switchDolar.isChecked = false
            binding.btnPromedio.isChecked = false
            binding.seekBar.progress = 0
            deshabilitarSeekBar()

        } else {
            binding.btnBcv.isChecked = true
            binding.btnPromedio.isChecked = false
            binding.btnParalelo.isChecked = false
            binding.seekBar.progress = 0
            deshabilitarSeekBar()

        }
    }

    private fun activarBtnParalelo() {
        if (binding.btnParalelo.isChecked == true) {

            binding.btnBcv.isChecked = false
            binding.btnPromedio.isChecked = false
            binding.seekBar.progress = 2
            deshabilitarSeekBar()
        } else {
            binding.btnParalelo.isChecked = true
            binding.btnBcv.isChecked = false
            binding.btnPromedio.isChecked = false
            binding.seekBar.progress = 2
            deshabilitarSeekBar()
        }
    }

    private fun deshabilitarSeekBar() {
        binding.seekBar.setOnTouchListener { _, _ -> true } // Bloquea la interacción
    }


    //llamar a api Una sola vez desde create  para verifica fecha
    fun llamarDolarBcvAdelantado() {
        // Obtén la hora actual
        val horaActual = LocalTime.now()

        // Obtén el día actual
        val diaActual = LocalDate.now().dayOfWeek

        // Verifica si es fin de semana
        val esFinDeSemana = diaActual == DayOfWeek.SATURDAY || diaActual == DayOfWeek.SUNDAY

        // Define los intervalos de tiempo
        val inicioSegundaConsulta = LocalTime.of(15, 1) // 3:01 PM
        val finSegundaConsulta = LocalTime.MAX // 11:59 PM

        // Si es fin de semana o está dentro del segundo intervalo de tiempo (3:01 PM - 11:59 PM)
        if (esFinDeSemana || horaActual in inicioSegundaConsulta..finSegundaConsulta) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val baseUrl = Constants.URL_BASE // URL base

                val client = OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .addHeader("Authorization", Constants.BEARER_TOKEN)
                            .build()
                        try {
                            val response = chain.proceed(request)
                            response
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in interceptor: ${e.message}")
                            throw e
                        }
                    }
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()

                val apiService = retrofit.create(ApiService::class.java)
                try {
                    val responseApiBcvAdelantado = apiService.getDollar()
                    if (responseApiBcvAdelantado != null) {
                        withContext(Dispatchers.Main) {

                            ApiResponseHolder.setResponseOriginal(responseApiBcvAdelantado)
                            guardarResponseAdelantado(requireContext(), responseApiBcvAdelantado )
                            val dateMayor = verificafechaActBcv(responseApiBcvAdelantado)
                            if (dateMayor) {
                                if (visibleLayoutProxBcv < 2) {
                                    visibleLayoutProxBcv += 1
                                    val slideIn = AnimationUtils.loadAnimation(
                                        requireContext(),
                                        R.anim.slide_in
                                    )
                                    val layoutUltActBcv = binding.layoutUltActBcv
                                    layoutUltActBcv.startAnimation(slideIn)
                                    binding.layoutUltActBcv.visibility = View.VISIBLE
                                    ApiResponseHolder.setResponseOriginal(responseApiBcvAdelantado)
                                    val fechaSinHora =
                                        extraerFecha(responseApiBcvAdelantado.monitors.bcv.last_update)
                                    binding.txtUltActBcv.text = fechaSinHora
                                }
                            } else {
                                binding.layoutUltActBcv.visibility = View.GONE
                                binding.SwUtimaAct.isChecked = false
                            }



                            multiplicaDolares()
                            dividirABolivares()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e(TAG, "llamarDolarBcvAdelantado: $e",)


                    }
                }
            }
        }

    }


    //LLAMAR A LAS APIS*****************************************************************

    fun llamarDolarApiNew(callback: (Boolean) -> Unit) {
        val savedResponseDolar = getResponseFromSharedPreferences(requireContext())

        val resposeGuardadoApiAdelantado = getResponseFromSharedPreferencesAdelantado(requireContext())

        try {
            if (savedResponseDolar != null) {
                ApiResponseHolder.setResponse(savedResponseDolar)
                valorActualParalelo = savedResponseDolar.monitors.enparalelovzla.price
                llenarDolarParalelo(savedResponseDolar)
                if (binding.SwUtimaAct.isChecked) {
                    if (resposeGuardadoApiAdelantado != null) {
                        llenarCampoBCVNew(resposeGuardadoApiAdelantado)
                        llenarCampoPromedio(resposeGuardadoApiAdelantado)
                    }
                } else {
                    llenarCampoBCVNew(savedResponseDolar)
                    llenarCampoPromedio(savedResponseDolar)
                }

                multiplicaDolares()
                dividirABolivares()
                binding.swipeRefreshLayout.isRefreshing = false
            }
        } catch (e: Exception) {
            Log.d(TAG, "llamarDolarApiNew: error $e")
            callback(false) // Operación fallida
        } finally {
            binding.swipeRefreshLayout.isRefreshing = false // Asegura que se detenga el refresco siempre
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            val baseUrl: String

            if (binding.swSeleccionApi.isChecked) {
                baseUrl = Constants.URL_BASE // URL base
                binding.swSeleccionApi.text = "Api Nueva"
            } else {
                baseUrl = Constants.URL_BASEOLD // URL base
                binding.swSeleccionApi.text = "Api Vieja"
            }

// Ahora puedes usar la variable baseUrl aquí
            println("La URL base seleccionada es: $baseUrl")
          //  val baseUrl = Constants.URL_BASE // URL base

            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Authorization", Constants.BEARER_TOKEN)
                        .build()
                    try {
                        val response = chain.proceed(request)
                        response
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in interceptor: ${e.message}")
                        throw e
                    }
                }
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            val apiService = retrofit.create(ApiService::class.java)
            try {
                val responseApiBcvAdelantado = getResponseFromSharedPreferencesAdelantado(requireContext())
               // val responseApiAlCambio = apiService.getDollarAlCambio("alcambio")
                val apiResponse = obtenerRespuestaApi(apiService)
                Log.d(TAG, "llamarDolarApiNew: apiResponse $apiResponse")
                if (apiResponse != null) {
                    withContext(Dispatchers.Main) {
                        ApiResponseHolder.setResponseApiAlCambio(apiResponse)
                        valorActualParalelo = apiResponse.monitors.enparalelovzla.price
                        guardarResponse(requireContext(), apiResponse)
                        animacionCrecerTexto(
                            binding.txtFechaActualizacionPara,
                            binding.txtFechaActualizacionBcv
                        )
                        binding.swipeRefreshLayout.isRefreshing = false
                        binding.txtFechaActualizacionPara.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.md_theme_light_surfaceTint
                            )
                        )
                        if (binding.SwUtimaAct.isChecked) {
                            if (responseApiBcvAdelantado != null) {
                                llenarCampoBCVNew(responseApiBcvAdelantado)
                                llenarCampoPromedio(responseApiBcvAdelantado)
                                llenarDolarParalelo(responseApiBcvAdelantado)
                            }
                        } else {
                            if (apiResponse != null) {
                                llenarCampoBCVNew(apiResponse)
                                llenarCampoPromedio(apiResponse)
                                llenarDolarParalelo(apiResponse)
                            }
                        }

                        multiplicaDolares()
                        dividirABolivares()
                        callback(true) // Operación exitosa
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.txtFechaActualizacionPara.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.red)
                    )
                    binding.txtFechaActualizacionBcv.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.red)
                    )
                    animarSwipe()
                    Toast.makeText(
                        requireContext(),
                        "Problemas de Conexion",
                        Toast.LENGTH_SHORT
                    ).show()
                    callback(false) // Operación fallida
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.swipeRefreshLayout.isRefreshing = false // Asegura que se detenga el refresco siempre
                    callback(false) // Operación completada, aunque con error
                }
            }
        }
    }


    private fun llamarApiCriptoDolar() {
        //Recupera los datos de la memoria Preference del dispositivo******************************
        try {
            val savedResponseCriptoDolar = getResponseSharedPreferencesCriptodolar(requireContext())

            //Publico en el Api Holder
            if (savedResponseCriptoDolar != null) {
                ApiResponseHolder.setResponseCripto(savedResponseCriptoDolar)

            }
        } catch (e: Exception) {

            Log.d(TAG, "llamarDolarApiNew: error $e")
        } finally {
            binding.swipeRefreshLayout.isRefreshing =
                false // Asegura que se detenga el refresco siempre
        }
        //******************************************************************************************

        val baseUrl = Constants.URL_BASE // URL base sin la última parte

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer 2x9Qjpxl5F8CoKK6T395KA") // Token añadido
                    .build()
                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
        val apiService = retrofit.create(ApiService::class.java)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Realizar la solicitud a la API con el parámetro de consulta
                val response = apiService.getDollarcriptodolar("criptodolar")

                if (response != null) {

                    // Procesa la respuesta según tu lógica
                    withContext(Dispatchers.Main) {
                        // Actualizar la UI en el hilo principal si es necesario
                        ApiResponseHolder.setResponseCripto(response)
                        guardarResponseCripto(requireContext(), response)

                    }
                } else {
                    // Manejar errores HTTP
                    Log.e("API_ERROR", "Error HTTP: del response}")
                }
            } catch (e: Exception) {
                // Manejo de errores generales
                Log.e("API_CALL", "Error: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    // Detener cualquier indicador de carga si es necesario
                }
            }
        }
    }

    private fun llamarApiPaginaBCV() {
        //Recupera los datos de la memoria Preference del dispositivo******************************
        try {
            val savedResponseBCV = getResponseSharedPreferencesBCV(requireContext())

            //Publico en el Api Holder
            if (savedResponseBCV != null) {
                ApiResponseHolder.setResponseBCV(savedResponseBCV)

            }
        } catch (e: Exception) {

            Log.d(TAG, "llamarDolarApiNew: erre $e")
        } finally {
            binding.swipeRefreshLayout.isRefreshing =
                false // Asegura que se detenga el refresco siempre
        }
        //******************************************************************************************
        val baseUrl = Constants.URL_BASE  // URL base sin la última parte

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer 2x9Qjpxl5F8CoKK6T395KA") // Token añadido
                    .build()
                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Realizar la solicitud a la API con el parámetro de consulta
                val response = apiService.getDollarBancosBcv("bcv")

                if (response != null) {

                    // Procesa la respuesta según tu lógica
                    withContext(Dispatchers.Main) {
                        // Actualizar la UI en el hilo principal si es necesario
                        ApiResponseHolder.setResponseBCV(response)
                        guardarResponseBancoBCV(requireContext(), response)
                    }
                } else {
                    // Manejar errores HTTP
                    Log.e("API_ERROR", "Error HTTP: del response}")
                }
            } catch (e: Exception) {
                // Manejo de errores generales
                Log.e("API_CALL", "Error: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    // Detener cualquier indicador de carga si es necesario
                }
            }
        }
    }




    //Funcion para verificar si lla al Ap al Cambio o la principal
    suspend fun obtenerRespuestaApi(apiService: ApiService): ApiConTokenResponse? {
        // Obtén la hora actual
        val horaActual = LocalTime.now()

        // Obtén el día actual
        val diaActual = LocalDate.now().dayOfWeek

        // Verifica si es fin de semana
        val esFinDeSemana = diaActual == DayOfWeek.SATURDAY || diaActual == DayOfWeek.SUNDAY

        // Define los intervalos de tiempo
        val inicioPrimeraConsulta = LocalTime.MIDNIGHT // 12:00 AM
        val finPrimeraConsulta = LocalTime.of(15, 0) // 3:00 PM
        val inicioSegundaConsulta = LocalTime.of(15, 1) // 3:01 PM
        val finSegundaConsulta = LocalTime.MAX // 11:59 PM

        // Si es fin de semana o está dentro del segundo intervalo de tiempo (3:01 PM - 11:59 PM)
        return if (esFinDeSemana || horaActual in inicioSegundaConsulta..finSegundaConsulta) {
            // Llamar a la API AlCambio
            apiService.getDollarAlCambio("alcambio")
        } else {
            // Llamar a la API BCV
            apiService.getDollar()
        }
    }






    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }


    private fun animacionCrecerTexto(texto: TextView, texto2: TextView) {
        val scaleUpAnimation = ScaleAnimation(
            1f, 1.5f, // De tamaño normal a 1.5 veces el tamaño original
            1f, 1.5f, // Igual para la altura
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        binding.txtFechaActualizacionBcv.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.md_theme_light_surfaceTint
            )
        )

        binding.txtFechaActualizacionPara.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.md_theme_light_surfaceTint
            )
        )
        scaleUpAnimation.duration = 300 // Duración de la animación (en milisegundos)
        scaleUpAnimation.fillAfter = false // Mantener la escala después de la animación

        val scaleDownAnimation = ScaleAnimation(
            1.5f, 1f, // De 1.5 veces el tamaño original a tamaño normal
            1.5f, 1f, // Igual para la altura
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        scaleDownAnimation.duration = 300 // Duración de la animación (en milisegundos)
        scaleDownAnimation.fillAfter = false // Mantener la escala después de la animación

        scaleUpAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                // Al finalizar la primera animación, iniciar la segunda
                texto.startAnimation(scaleDownAnimation)
                texto2.startAnimation(scaleDownAnimation)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })

        // Iniciar la primera animación
        texto.startAnimation(scaleUpAnimation)
        texto2.startAnimation(scaleUpAnimation)

    }

    private fun animacionCrecerBoton(boton: ToggleButton, boton2: ToggleButton, textFecha: TextView) {
        val scaleUpAnimation = ScaleAnimation(
            1f, 1.3f, // De tamaño normal a 1.5 veces el tamaño original
            1f, 1.3f, // Igual para la altura
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )

        scaleUpAnimation.duration = 200 // Duración de la animación (en milisegundos)
        scaleUpAnimation.fillAfter = false // Mantener la escala después de la animación

        val scaleDownAnimation = ScaleAnimation(
            1.3f, 1f, // De 1.5 veces el tamaño original a tamaño normal
            1.3f, 1f, // Igual para la altura
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        scaleDownAnimation.duration = 200 // Duración de la animación (en milisegundos)
        scaleDownAnimation.fillAfter = false // Mantener la escala después de la animación

        scaleUpAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                // Al finalizar la primera animación, iniciar la segunda
                boton.startAnimation(scaleDownAnimation)
                boton2.startAnimation(scaleDownAnimation)
                textFecha.startAnimation(scaleDownAnimation)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })

        // Iniciar la primera animación
        boton.startAnimation(scaleUpAnimation)
        boton2.startAnimation(scaleUpAnimation)
        textFecha.startAnimation(scaleUpAnimation)

    }



    //Guarda en SharePreference los Respose de cada solicitud al API
    private fun guardarResponse(context: Context, responseBCV: ApiConTokenResponse) {
        val gson = Gson()
        val responseJson = gson.toJson(responseBCV)

        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("dolarBCVResponse", responseJson)
        editor.apply()
    }

    //Guarda en SharePreference los Respose de cada solicitud al API ADEKANTADO
    private fun guardarResponseAdelantado(context: Context, responseBCV: ApiConTokenResponse) {
        val gson = Gson()
        val responseJson = gson.toJson(responseBCV)

        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("dolarBCVAdelantado", responseJson)
        editor.apply()
    }


    private fun guardarResponseCripto(context: Context, responseBCV: ApiModelResponseCripto) {
        val gson = Gson()
        val responseJson = gson.toJson(responseBCV)

        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("dolarCripto", responseJson)
        editor.apply()

    }

    private fun guardarResponseBancoBCV(context: Context, responseBCV: ApiModelResponseBCV) {
        val gson = Gson()
        val responseJson = gson.toJson(responseBCV)

        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("dolarResponseBCV", responseJson)
        editor.apply()

    }


    //*********************************************************************************************

    // Define una función para recuperar la respuesta de SharedPreferences
    private fun getResponseFromSharedPreferences(context: Context): ApiConTokenResponse? {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val responseJson = sharedPreferences.getString("dolarBCVResponse", null)

        if (responseJson != null) {
            val gson = Gson()

            return gson.fromJson(responseJson, ApiConTokenResponse::class.java)
        }

        return null // Retorna null si no se encontró la respuesta en SharedPreferences
    }

    private fun getResponseFromSharedPreferencesAdelantado(context: Context): ApiConTokenResponse? {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val responseJson = sharedPreferences.getString("dolarBCVAdelantado", null)

        if (responseJson != null) {
            val gson = Gson()

            return gson.fromJson(responseJson, ApiConTokenResponse::class.java)
        }

        return null // Retorna null si no se encontró la respuesta en SharedPreferences
    }


    // Define una función para recuperar la respuesta de SharedPreferences
    private fun getResponseSharedPreferencesCriptodolar(context: Context): ApiModelResponseCripto? {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val responseJson = sharedPreferences.getString("dolarCripto", null)

        if (responseJson != null) {
            val gson = Gson()

            return gson.fromJson(responseJson, ApiModelResponseCripto::class.java)
        }

        return null // Retorna null si no se encontró la respuesta en SharedPreferences
    }

    // Define una función para recuperar la respuesta de SharedPreferences
    private fun getResponseSharedPreferencesBCV(context: Context): ApiModelResponseBCV? {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val responseJson = sharedPreferences.getString("dolarResponseBCV", null)

        if (responseJson != null) {
            val gson = Gson()

            return gson.fromJson(responseJson, ApiModelResponseBCV::class.java)
        }

        return null // Retorna null si no se encontró la respuesta en SharedPreferences
    }

    private fun getResponseSharedPreferencesHistory(context: Context): HistoryModelResponse? {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val responseJson = sharedPreferences.getString("ResponseHistory", null)

        if (responseJson != null) {
            val gson = Gson()

            return gson.fromJson(responseJson, HistoryModelResponse::class.java)
        }

        return null // Retorna null si no se encontró la respuesta en SharedPreferences
    }


    fun llenarCampoBCVNew(response: ApiConTokenResponse) {
        // DATOS DEL BCV
        if (!response.monitors.bcv.price.toString().isNullOrEmpty()) {
            binding.btnBcv.text = response.monitors.bcv?.price.toString()
            binding.btnBcv.textOff = response.monitors.bcv?.price.toString()
            binding.btnBcv.textOn = response.monitors.bcv?.price.toString()
            val fechaSinHora = extraerFecha(response.monitors.bcv.last_update)
            binding.txtFechaActualizacionBcv.text = fechaSinHora
            if (response.monitors.bcv.color == "red") binding.imgflechaBcv.setImageResource(R.drawable.ic_flecha_roja)
            if (response.monitors.bcv.color == "neutral") binding.imgflechaBcv.setImageResource(R.drawable.ic_flecha_igual)
            if (response.monitors.bcv.color == "green") binding.imgflechaBcv.setImageResource(R.drawable.ic_flechaverde)

            binding.txtVariacionBcv.text = response.monitors.bcv.percent.toString()



        }

    }

    // verifica Si la actualizacion del dolar es diferente a la fecha actual
    fun verificafechaActBcv(response: ApiConTokenResponse): Boolean {
        try {
            // Cambia el locale a Locale.US para que acepte "AM/PM"
            val dateFormat = SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.US)

            // Obtener la fecha actual
            val currentDate = Calendar.getInstance().time

            // Obtener los datos del monitor BCV
            val bcvMonitor = response.monitors.bcv.last_update

            if (!bcvMonitor.isNullOrEmpty()) {
                // Parsear la fecha del last_update
                val lastUpdateDate = dateFormat.parse(bcvMonitor)

                if (lastUpdateDate != null && lastUpdateDate.after(currentDate)) {

                    // Aquí iría tu lógica para realizar la acción.

                    return true



                } else {

                    return false

                }
            } else {

                return false

            }
        } catch (e: ParseException) {
            e.printStackTrace()
            Log.d(TAG, "ultimaActBcv: Error al parsear la fecha: $e")
            return false
        }
    }



    fun llenarCampoPromedio(response: ApiConTokenResponse) {

        // Verificar si el precio no está vacío o nulo
        if (!response.monitors.bcv.price.toString().isNullOrEmpty()) {
            // Calcular el promedio con dos decimales
            val promedio = ((response.monitors.bcv.price
                .plus(response.monitors.enparalelovzla.price))?.div(2))

            // Formatear el promedio a dos decimales
            val promedioConDosDecimales = String.format(Locale.US, "%.2f", promedio)

            // Asignar el texto al ToggleButton
            binding.btnPromedio.text = promedioConDosDecimales
            binding.btnPromedio.textOff = promedioConDosDecimales
            binding.btnPromedio.textOn = promedioConDosDecimales

        }
    }

    fun llenarDolarParalelo(response: ApiConTokenResponse) {
        binding.btnParalelo.text = response.monitors.enparalelovzla.price.toString()
        binding.btnParalelo.textOff = response.monitors.enparalelovzla.price.toString()
        binding.btnParalelo.textOn = response.monitors.enparalelovzla.price.toString()
        binding.txtFechaActualizacionPara.text = response.monitors.enparalelovzla.last_update

        if (response.monitors.enparalelovzla.color == "red") binding.imgFlechaParalelo.setImageResource(
            R.drawable.ic_flecha_roja
        )
        if (response.monitors.enparalelovzla.color == "neutral") binding.imgFlechaParalelo.setImageResource(
            R.drawable.ic_flecha_igual
        )
        if (response.monitors.enparalelovzla.color == "green") binding.imgFlechaParalelo.setImageResource(
            R.drawable.ic_flechaverde
        )
        binding.txtVariacionParalelo.text = response.monitors.enparalelovzla.percent.toString()

    }

    fun extraerFecha(fechaHora: String): String {
        // Dividimos la cadena por la coma para separar fecha y hora
        val partes = fechaHora.split(", ")

        // La primera parte (índice 0) corresponde a la fecha
        return partes[0]
    }

    private fun multiplicaDolares() {
        val decimalFormat = DecimalFormat("#,##0.00") // Declaración de DecimalFormat
        binding.inputDolares?.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                ultimoTecleado = 1
                var valorDolares = 0.0
                if (binding.inputDolares.isFocused) {
                    val dolarParalelo = binding.btnParalelo.text.toString().toDoubleOrNull()
                        ?: 0.0 //precio del paralelo
                    val dolarBcv = binding.btnBcv.text.toString().toDoubleOrNull() ?: 0.0 //precio del paralelo
                    val dolarPromedio = binding.btnPromedio.text.toString().toDoubleOrNull() ?: 0.0 //precio del paralelo
                    val entradaDolares = binding.inputDolares.text.toString()
                    if (entradaDolares.isNotEmpty()) {
                        if (binding.btnParalelo.isChecked) {
                          
                            if (valorActualParalelo != null) {
                                val cleanedText =
                                    entradaDolares.replace(
                                        "[,]".toRegex(),
                                        ""
                                    ) // Elimina puntos y comas
                                val dolarLimpio = cleanedText.toDoubleOrNull() ?: 0.0

                                valorDolares = dolarLimpio * dolarParalelo!!.toDouble()
                            }
                        }

                        if (binding.btnBcv.isChecked) {
                         
                            val cleanedText =
                                entradaDolares.replace(
                                    "[,]".toRegex(),
                                    ""
                                ) // Elimina puntos y comas
                            val parsedValue = cleanedText.toDoubleOrNull() ?: 0.0
                            if (dolarBcv != null) {
                                valorDolares = parsedValue * dolarBcv!!.toDouble()
                            }

                        }
                        if (binding.btnPromedio.isChecked) {
                    
                            val cleanedText =
                                entradaDolares.replace(
                                    "[,]".toRegex(),
                                    ""
                                ) // Elimina puntos y comas
                            val parsedValue = cleanedText.toDoubleOrNull() ?: 0.0
                            if (dolarPromedio != null) {
                                valorDolares = parsedValue * dolarPromedio!!.toDouble()
                            }

                        }
                        val formattedValorDolares = decimalFormat.format(valorDolares)
                        binding.inputBolivares.setText(formattedValorDolares)

                    } else {
                        binding.inputBolivares.text?.clear()

                    }
                }

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
    }


    private fun dividirABolivares() {
        val decimalFormat = DecimalFormat("#,##0.00") // Declaración de DecimalFormat

        binding.inputBolivares?.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                ultimoTecleado = 0
                var valorDolares = 0.0
                if (binding.inputBolivares.isFocused) {
                    val inputText = binding.inputBolivares.text.toString()
                    val dolarParalelo = binding.btnParalelo.text.toString().toDoubleOrNull() ?: 0.0
                    val dolarBcv = binding.btnBcv.text.toString().toDoubleOrNull() ?: 0.0
                    val dolarPromedio = binding.btnPromedio.text.toString().toDoubleOrNull() ?: 0.0

                    if (inputText.isNotEmpty()) {
                        val cleanedText =
                            inputText.replace("[,]".toRegex(), "").toDoubleOrNull() ?: 0.0

                        if (binding.btnParalelo.isChecked) {
                            // Dividir el valor en bolívares por el dólar paralelo
                            valorDolares = cleanedText / dolarParalelo

                        }
                        if (binding.btnBcv.isChecked){
                            // Convertir bolívares a dólares usando el paralelo o BCV dependiendo del estado del switch
                            valorDolares = cleanedText / dolarBcv
                        }
                        if (binding.btnPromedio.isChecked){
                            // Convertir bolívares a dólares usando el paralelo o BCV dependiendo del estado del switch
                            valorDolares = cleanedText / dolarPromedio
                        }



                        val formattedValorDolares = decimalFormat.format(valorDolares)
                        binding.inputDolares.setText(formattedValorDolares)
                    } else {
                        binding.inputDolares.text?.clear()
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No se necesita lógica aquí por ahora
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No se necesita lógica aquí por ahora
            }
        })
    }


    private fun copiarDolar() {
        try {
            val montoDolarCopy = binding.inputDolares.text.toString()
            if (!montoDolarCopy.isNullOrEmpty()) {
                //montoDolarCopy.toDouble()
                copyToClipboard(requireContext(), montoDolarCopy, "$montoDolarCopy", "$")
            } else {
                Toast.makeText(requireContext(), "Campo vacio", Toast.LENGTH_SHORT).show()
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "No se pudo Copiar", Toast.LENGTH_SHORT).show()
        }


    }

    //COPIAR LOS DATOS AL PORTA PAPELES
    private fun copiarBs() {
        try {
            var montoBolivarCopyLimpio = 0.0
            val montoBolivarCopy = binding.inputBolivares.text.toString()
            if (!montoBolivarCopy.isNullOrEmpty()) {
                val cadenaNumerica = montoBolivarCopy
                val cadenaLimpia = cadenaNumerica.replace(",", "")
                montoBolivarCopyLimpio = cadenaLimpia.toDouble()

                montoBolivarCopyLimpio
                copyToClipboard(
                    requireContext(),
                    montoBolivarCopy,
                    "$montoBolivarCopy",
                    "Bs."
                )
            } else {
                Toast.makeText(requireContext(), "Campo vacio", Toast.LENGTH_SHORT).show()
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "No se pudo Copiar", Toast.LENGTH_SHORT).show()
        }


    }

    //FUNCION PARA COPIAR AL PORTA PAPEL
//    fun copyToClipboard(context: Context, text: String, titulo: String, unidad: String) {
//        // Obtener el servicio del portapapeles
//        val clipboardManager =
//            context.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
//
//        // Crear un objeto ClipData para guardar el texto
//        val clipData = ClipData.newPlainText("text", text)
//
//        // Copiar el objeto ClipData al portapapeles
//        clipboardManager.setPrimaryClip(clipData)
//        Toast.makeText(requireContext(), "Monto Copiado: $titulo $unidad", Toast.LENGTH_SHORT)
//            .show()
//    }

    // FUNCIÓN PARA COPIAR AL PORTA PAPELES
    fun copyToClipboard(context: Context, text: String, titulo: String, unidad: String) {
        // Reemplazar comas por puntos y puntos por comas
        val modifiedText = text.replace(",", "temp").replace(".", ",").replace("temp", ".")

        // Obtener el servicio del portapapeles
        val clipboardManager =
            context.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager

        // Crear un objeto ClipData para guardar el texto modificado
        val clipData = ClipData.newPlainText("text", modifiedText)
        Log.d(TAG, "copyToClipboard: modifiedText $modifiedText ")
        // Copiar el objeto ClipData al portapapeles
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(context, "Monto Copiado: $modifiedText $unidad", Toast.LENGTH_SHORT).show()
    }

    private fun animarSwipe() {
        val imageView = binding.imageSwipe
        imageView.visibility = View.VISIBLE
        val slideDown = AnimationUtils.loadAnimation(requireContext(), R.anim.swipe_down)
        val slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.swipe_up)

        slideDown.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                imageView.startAnimation(slideUp)
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        slideUp.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                if (repeatCount < 1) {
                    repeatCount++
                    imageView.startAnimation(slideDown)
                } else {
                    repeatCount = 0
                    imageView.visibility = ImageView.GONE
                }
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        imageView.startAnimation(slideDown)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        visibleLayoutProxBcv = 0
        lifecycleScope.coroutineContext.cancel()
        //   eliminarListener()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
     //   if (binding.progressBar.visibility != View.VISIBLE) binding.swipeRefreshLayout.isRefreshing = true
        comenzarCarga()
        llamarDolarApiNew { isSuccessful ->

            // Solo habilitar el botón si ambas APIs responden
            if (isSuccessful) {
                binding.btnRefres.isEnabled = true
                finalizarCarga()
            }else{
                finalizarCarga()

            }
        }

       // shakeDetector.start()

    }


    override fun onPause() {
        //eliminarListener()
        super.onPause()
        shakeDetector.stop()
    }

    private fun onShakeDetected() {
        //showSnackbar("Cargando datos...")

        fetchDataFromServer()
    }

    private fun calcularDiferencia() {
        var mensaje = ""
        var diferenciaBs = 0.0
        var diferenciaDolares = 0.0
        var diferenciaPorcentual = 0.0
        var dolarParalelo = binding.btnParalelo.text?.toString()?.toDoubleOrNull() ?: 1.0
        var dolarBcv = binding.btnBcv.text?.toString()?.toDoubleOrNull() ?: 1.0
        var cantidadDolares = binding.inputDolares.text?.toString()?.toDoubleOrNull() ?: 1.0
        var cantidadBs = binding.inputBolivares.text?.toString()?.toDoubleOrNull() ?: 1.0

        val totalDolaresBcv = dolarBcv * cantidadDolares
        val totalBsbcv = dolarBcv * cantidadDolares
        val totalDolaresParalelo = dolarParalelo * cantidadDolares
        val totalBsParalelo = dolarParalelo * cantidadBs



        if (!binding.btnBcv.isChecked) {
            mensaje = getString(R.string.mensaje_dolar)
            diferenciaBs = totalDolaresParalelo - totalDolaresBcv
            diferenciaDolares = (totalDolaresParalelo - totalDolaresBcv) / dolarBcv
            val diferencia = Math.abs(dolarParalelo - dolarBcv)
            diferenciaPorcentual= (diferencia / dolarBcv) * 100
        } else {
            mensaje = getString(R.string.mensaje_paralelo)
            diferenciaBs = totalDolaresBcv - totalDolaresParalelo
            diferenciaDolares = (totalDolaresBcv - totalDolaresParalelo) / dolarBcv
            diferenciaPorcentual = ((dolarBcv * 100 / dolarParalelo) - 100)
            val diferencia = Math.abs(dolarParalelo - dolarBcv)
            diferenciaPorcentual= (diferencia / dolarBcv) * 100
        }

        showCustomSnackbar(mensaje, diferenciaBs, diferenciaDolares, diferenciaPorcentual)
    }


    //Abre el mensaje Anacbar Personalizado
    @SuppressLint("RestrictedApi")
    private fun showCustomSnackbar(
        mensaje: String,
        diferenciaBolivares: Double,
        difenciaDolares: Double,
        diferenciaPorcentual: Double,
    ) {
        val decimalFormat = DecimalFormat("#,##0.00") // Declaración de DecimalFormat
        val rootView = requireActivity().findViewById<View>(android.R.id.content)
        snackbar = Snackbar.make(rootView, "", Snackbar.LENGTH_INDEFINITE)

        // Inflar el diseño personalizado
        val snackbarLayout = snackbar?.view as Snackbar.SnackbarLayout
        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_toast, null)

        // Configurar el ícono y el texto
        val snackbarTextView: TextView = customView.findViewById(R.id.toast_text)
        snackbarTextView.text = "$mensaje:  ${decimalFormat.format(diferenciaPorcentual)}%"
        snackbarTextView.textSize = 14f
        val textViewDifBs: TextView = customView.findViewById(R.id.txtToastDifBs)
        val textViewDifDolar: TextView = customView.findViewById(R.id.txtToastDifDolar)
        val formattDiferenciaBS = decimalFormat.format(diferenciaBolivares)
        val formattDiferenciaDolar = decimalFormat.format(difenciaDolares)
        textViewDifBs.text = formattDiferenciaBS
        textViewDifDolar.text = formattDiferenciaDolar

        // Configurar el botón de cierre
        val closeButton: ImageButton = customView.findViewById(R.id.close_button)
        closeButton.setOnClickListener {
            snackbar?.dismiss()
        }

        // Agregar el diseño personalizado al Snackbar
        snackbarLayout.setBackgroundResource(R.drawable.snackbar_background) // Configurar el fondo personalizado
        snackbarLayout.addView(customView, 0)

        // Mostrar el Snackbar centrado
        snackbar?.show()

        // Ajustar la posición del Snackbar al centro de la pantalla
        val params = snackbar?.view?.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.CENTER
        snackbar?.view?.layoutParams = params
    }

    // Abre el mensaje De aviso que puede actualizarcon sacudor
    private fun verSnackVarInfo(mensaje: String) {
        val rootView = requireActivity().findViewById<View>(android.R.id.content)
        snackbarInfo = Snackbar.make(rootView, mensaje, Snackbar.LENGTH_INDEFINITE)
        snackbarInfo?.show()
        // Usar un Handler para ocultar el toast después de una duración personalizada
        Handler(Looper.getMainLooper()).postDelayed({
            snackbarInfo?.dismiss()
        }, 3000.toLong())

    }

    // Cierra el mensaje personalizado
    private fun hideSnackbar() {
        snackbar?.dismiss()
    }

    //Simula el Tiempo de respuesta de un servidor
    private fun fetchDataFromServer() {
        // Simular una solicitud asíncrona
        Handler(Looper.getMainLooper()).postDelayed({
            // Solicitud completada
            hideSnackbar()
        }, 5000) // Simular 5 segundos de retraso
    }

    private fun probarencryptado(){
        // Clave maestra (la que usas en tu sistema)
        val masterKey = "A11103402525120190822HB01"

        // Datos que deseas cifrar (por ejemplo, un número de teléfono)
        val dataToEncrypt = "584148508980"
        val dataToEncrypt2 = "584166227839"

        // Cifrar los datos
        val encryptedData = AESCrypto.encrypt(dataToEncrypt, masterKey)
        println("Datos cifrados: $encryptedData")
        Log.d("probarencryptado", "probarencryptado encryptedData: $encryptedData ")

        val encryptedData2 = AESCrypto.encrypt(dataToEncrypt2, masterKey)
        println("Datos cifrados: $encryptedData2")
        Log.d("probarencryptado", "probarencryptado encryptedData2: $encryptedData2 ")

        // Descifrar los datos
        val decryptedData = AESCrypto.decrypt(encryptedData, masterKey)
        println("Datos descifrados: $decryptedData")
        Log.d("probarencryptado", "probarencryptado decryptedData: $decryptedData ")

        val decryptedData2 = AESCrypto.decrypt(encryptedData2, masterKey)
        println("Datos descifrados: $decryptedData2")
        Log.d("probarencryptado", "probarencryptado decryptedData2: $decryptedData2 ")


    }




/*
    fun sendPaymentRequest() {
        Log.d("API_CALL", "entro a la funcion")
        val baseUrl = "https://apimbu.mercantilbanco.com/"
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("X-IBM-Client-ID", "81188330-c768-46fe-a378-ff3ac9e88824")
                    .build()
                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        val masterKey = "A11103402525120190822HB01"
        val destinationIdEncrypted = AESCrypto.encrypt("destination_id_value", masterKey)
        val destinationMobileEncrypted = AESCrypto.encrypt("04121234567", masterKey)
        val twoFactorAuthEncrypted = AESCrypto.encrypt("123456", masterKey)

        val paymentRequest = PaymentRequest(
            merchant_identify = MerchantIdentify(31, 200284, "abcde"),
            client_identify = ClientIdentify("127.0.0.1", "Chrome 18.1.3", Mobile("Samsung")),
            transaction_c2p = TransactionC2P(
                1.00,
                "ves",
                "",
                destinationIdEncrypted,
                destinationMobileEncrypted,
                "04141234567",
                "",
                "compra",
                "c2p",
                "",
                twoFactorAuthEncrypted
            )
        )
        Log.d("API_CALL", "entro a la funcion Valor de paymentRequest: $paymentRequest ")
        apiService.sendPaymentData(paymentRequest).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                Log.d("API_CALL", "entro onResponse paymentRequest: $paymentRequest response: $response ")
                if (response.isSuccessful) {
                    Log.d("API_CALL", "Solicitud exitosa: ${response.body()}")
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {

                    }
                } else {
                    Log.e("API_ERROR", "Error en la solicitud: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("API_CALL", "Error: ${t.message}")
            }
        })
    }

*/

/*
    fun realizarBusquedaMovil() {

        // Clave maestra (la que usas en tu sistema)
        val masterKey = "A11103402525120190822HB01"

        // Datos que deseas cifrar (por ejemplo, un número de teléfono)
        val EncryptTlfDestino = "584166227839"
        val EncryptTlfOrigen = "584148508980"

        // Cuerpo de la solicitud con datos ya encriptados
        val requestBody = MobilePaymentSearchRequest(
            merchant_identify = MerchantIdentify(
                integratorId = 31,
                merchantId = 123456,
                terminalId = "abcde"
            ),
            client_identify = ClientIdentify(
                ipaddress = "127.0.0.1",
                browser_agent = "Chrome 18.1.3",
                mobile = MobileInfo(
                    manufacturer = "Samsung"
                )
            ),
            search_by = SearchBy(
                amount = 1.00,
                currency = "ves",
                destinantion_mobile_number = AESCrypto.encrypt(EncryptTlfDestino, masterKey),
                origin_mobile_number = AESCrypto.encrypt(EncryptTlfOrigen, masterKey),
                payment_reference = "123",
                trx_date = "23-10-2024"
            )
        )

        // Realiza la solicitud POST con Retrofit
        val call = RetrofitClient.apiService.buscarPagosMoviles(
            clientId = "81188330-c768-46fe-a378-ff3ac9e88824",
            request = requestBody
        )

        Log.e("Respuesta", "requestBody: $requestBody")
        Log.e("Respuesta", "call: $call")
        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    println("Respuesta JSON: ${apiResponse?.data}")
                    Log.d("Respuesta", "Respuesta JSON: ${apiResponse?.data}")
                } else {
                    val errorBody = response.errorBody()?.string()
                    println("Error en la respuesta: ${response.code()}")
                    println("Cuerpo de error: $errorBody")
                    Log.e("Respuesta", "Error en la respuesta: ${response.code()} response: $response")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                println("Error: ${t.message}")
                Log.e("Respuesta", "Error: ${t.message}")
            }
        })
    }

 */

    private fun realizarBusqueda2(){
        val merchantIdentify = MerchantIdentify(
            integratorId = 31,
            merchantId = 150332,
            terminalId = "abcde"
        )

        val mobileInfo = MobileInfo(
            manufacturer = "Samsung",
            model = "S9",
            os_version = "Oreo 9.1"
        )

        val location = Location(
            lat = 0,
            lng = 0
        )

        val clientIdentify = ClientIdentify(
            ipaddress = "127.0.0.1",
            browser_agent = "Chrome 18.1.3",
            mobile = mobileInfo,
            os_version = "Oreo 9.1",
            location = location
        )

        val searchBy = SearchBy(
            amount = 30.0,
            currency = "ves",
            origin_mobile_number = "0PbWVea/C/hyO37XjEoFaA==",  // Cifrado
            destination_mobile_number = "mD9ROJSFzSpnLTOPGr7B7A==",  // Cifrado
            payment_reference = "118060003823",
            trx_date = "2024-10-04"
        )

        val request = MobilePaymentSearchRequest(
            merchant_identify = merchantIdentify,
            client_identify = clientIdentify,
            search_by = searchBy
        )

// Realizamos la solicitud
        val call = RetrofitClient.apiService.searchMobilePayment(request)

        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    println("Respuesta exitosa: ${response.body()}")
                    Log.d("probarencryptado", "Respuesta exitosa: ${response.body()}")
                } else {
                    println("Error en la respuesta: ${response.code()} - ${response.errorBody()} response $response")
                    Log.d("probarencryptado", "Error en la respuesta: ${response.code()} - ${response.errorBody()} response $response")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                println("Error en la solicitud: ${t.message}")
            }
        })

    }

    object RetrofitClient {
        private const val BASE_URL = "https://apimbu.mercantilbanco.com/mercantil-banco/sandbox/v1/"

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService: ApiService = retrofit.create(ApiService::class.java)
    }



}