package com.carlosv.dolaraldia.ui.home

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
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.ScaleAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.carlosv.dolaraldia.ApiService

import com.carlosv.dolaraldia.model.bancos.DolarNew
import com.carlosv.dolaraldia.model.bcv.BcvNew

import com.carlosv.dolaraldia.model.paralelo.ParaleloVzla
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
import okhttp3.Request
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
import com.carlosv.dolaraldia.MyApplication
import com.carlosv.dolaraldia.model.clickAnuncios.ClickAnunicosModel
import com.carlosv.dolaraldia.model.configImagen.ConfigImagenModel
import com.carlosv.dolaraldia.provider.ClickAnuncioProvider
import java.security.Timestamp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null")
    private var isFragmentAttached: Boolean = false


    var url: String?= null
    var nombreAnuncio: String?= null
    var pagina: String?= null
    var linkAfaliado:String?= null
    private val imagenConfProvider = ImagenProvider()
    private val clickAnuncioProvider = ClickAnuncioProvider()
    private var imageConfigListener: ListenerRegistration? = null
    private var configImageModels = ArrayList<ConfigImagenModel>()

    private var bcvActivo: Boolean?= null
    private var valorActualParalelo: Double? = 0.0
    private var valorActualBcv: Double? = 0.0
    private var valorActualEuro: Float? = 0.0f
    private var ultimoTecleado: Int? = 2
    var numeroNoturno = 0
    //Verificar llamado API
    private var llamdoParalelo: Boolean= false
    private var llamadoBCV: Boolean= false
    lateinit var mAdView : AdView

    private lateinit var layout: LinearLayout

    private var repeatCount = 0

    // Para Admob Reguard

    private var TAG = "HomeFragment"


    private var interstitial: InterstitialAd? = null
    private var count = 0



    lateinit var navigation : BottomNavigationView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root


        MobileAds.initialize(requireContext()) {}
       // cargarImagendelConfig()




        //PARA CARGAR ADMOB
        layout = binding.linearLayout3
        mAdView= binding.adView
       // mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
//******************************************

        initializeMobileAdsSdk(requireContext())

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
        val sharedPreferences = requireContext().getSharedPreferences("MiPreferencia", AppCompatActivity.MODE_PRIVATE  )
        // Obtener referencia a SharedPreferences

        // Recuperar el valor entero
        numeroNoturno = sharedPreferences.getInt("numero_noturno", 0)


        //VERIFICA SI QUE MEDO TIENE GUARDADO
       // setDayNight(modoDark())
        binding.swipeRefreshLayout.setOnRefreshListener {
           binding.swipeRefreshLayout.isRefreshing = true
            eliminarListener()
            llamarDolarNew()
            llamarBcvNew()
            actualizarEuro()
            listenerImagenConfig()

        }

        binding.switchDolar.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {

                val valorParalelo = binding.btnParalelo.text.toString()

                val doubleValue = try {
                    valorParalelo.toDouble()
                } catch (e: NumberFormatException) {
                    null // Si la conversión falla, asigna null
                }

                // Actualiza la multiplicación con el valor convertido o 0.0 si la conversión falla
                actualzarMultiplicacion(doubleValue ?: 0.0)
                bcvActivo = false
                binding.btnBcv.isChecked = false
                binding.btnParalelo.isChecked = true

            } else {
                val valorBcv = binding.btnBcv.text.toString()

                val doubleValue = try {
                    valorBcv.toDouble()
                } catch (e: NumberFormatException) {
                    null // Si la conversión falla, asigna null
                }

                // Actualiza la multiplicación con el valor convertido o 0.0 si la conversión falla
                actualzarMultiplicacion(doubleValue ?: 0.0)
                bcvActivo = true
                binding.btnBcv.isChecked = true
                binding.btnParalelo.isChecked = isChecked

            }

        }
        binding.btnBcv.setOnClickListener {
            activarBtnBcv()
        }
        binding.btnParalelo.setOnClickListener {
            activarBtnParalelo()
        }
        binding.imgCopyDolar.setOnClickListener {
            copiarDolar()
        }
        binding.imgCoyBolivar.setOnClickListener {
            copiarBs()
        }

        binding.imglogo.setOnClickListener {
            crearImagenUrl()
        }

        binding.imgVPublicidad.setOnClickListener {
            guardarClickAnuncio()
             irAlArticulo()

        }
        binding.imgCerrarAnuncio.setOnClickListener {
           // binding.layoutCerraAnun.visibility= View.GONE
            binding.LnerPubliImagen.visibility= View.GONE
        }

        //PARA ACTUALIZAR EL PRECIO DEL DOLAR SOLO CUANDO CARGA POR PRIMERA VEZ
        if(savedInstanceState== null){

            disableSSLVerification()

            actualizarEuro()
        }



        // Aplicar la animación
        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.appear_from_top)
        root.startAnimation(animation)

        //***********************
        return root
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
                    Toast.makeText(requireContext(), "Error ${it.exception}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Error en datos $e", Toast.LENGTH_SHORT).show()
        }
    }

    private fun irAlArticulo() {
        // URL que quieres abrir cuando se hace clic en la imagen
        //  url = "https://www.ejemplo.com/tu_link_de_afiliado"
        Log.d(TAG, "irAlArticulo: URl: $url")
        // Crea un intent implícito para abrir la URL en un navegador web
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(linkAfaliado)
        startActivity(intent)
        // Comprueba si hay aplicaciones que pueden manejar este intento
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            // Abre la URL en un navegador web

        }
    }


    private fun techadoDesplegado():Boolean{

        // Obtén el contexto de la actividad
        val context: Context = requireContext()

        // Verifica si el teclado está desplegado
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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


    private fun listenerImagenConfig() {
        //************************
        imageConfigListener = imagenConfProvider.getAllImagenConfig()
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {

                    configImageModels.clear() // Limpiar la lista antes de agregar los nuevos elementos

                    if (querySnapshot.documents.size > 0) {
                        val documents = querySnapshot.documents

                        for (d in documents) {
                            var imageConfig = d.toObject(ConfigImagenModel::class.java)
                            imageConfig?.id = d.id
                            configImageModels.add(imageConfig!!)
                        }
                        if (configImageModels.isNotEmpty()) {
                            // Accede al primer elemento del ArrayList
                            // Generar un número aleatorio entre el rango de índices del array
                            val randomIndex = (0 until configImageModels.size).random()
                            url = configImageModels[randomIndex].url
                            nombreAnuncio = configImageModels[randomIndex].nombre
                            linkAfaliado = configImageModels[randomIndex].linkAfiliado
                            pagina = configImageModels[randomIndex].pagina
                            Log.d(TAG, "listenerImagenConfig: url: $url, nombreAnuncio $nombreAnuncio, linkAfaliado: $linkAfaliado pagina: $pagina")

                            cargarImagen(url)

                        } else {
                            binding.LnerPubliImagen.visibility = View.INVISIBLE
                        }

                    } else {
                        Log.d(TAG, "listenerpagomovil: sin datos de imagen")
                    }


                }
            }
    }

    override fun onPause() {
       eliminarListener()
        super.onPause()
    }

    private fun eliminarListener() {
        imageConfigListener?.remove()
    }


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

                    Toast.makeText(requireContext(), "Datos Enviados para Validar", Toast.LENGTH_LONG).show()

                } else {
                    Log.d(TAG, "crearImagenUrl: ${it.exception}")
                    Toast.makeText(requireContext(), "Error al crear los datos", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Error en datos $e", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarImagen(uri: String?) {
        if (uri != "") {
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
                     //   binding.layoutCerraAnun.visibility = View.VISIBLE
                        binding.layoutCerraAnun.visibility= View.VISIBLE
                        Log.d(TAG, "onResourceReady: visiblke true")
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
        private var response: DolarNew? = null

        private const val VALOR_EURO = "ValorEuro"
        private const val NUMERO_EURO = "euro"
        private const val FECHA_EURO = "fecha"

        fun getResponseParalelovzla(savedResponseVzla: ParaleloVzla): ParaleloVzla? {
            return savedResponseVzla
        }
        fun getResponseBcv(responseBcvNew: BcvNew):BcvNew? {
            return responseBcvNew
        }
        fun getResponse(): DolarNew? {
            return response
        }

        fun setResponse(newResponse: DolarNew) {
            response = newResponse
        }

        fun guardarEuro(context: Context, numero: Float) {
            val prefs: SharedPreferences = context.getSharedPreferences(VALOR_EURO, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putFloat(NUMERO_EURO, numero)
            editor.apply()
        }


        fun recuperarEuro(context: Context): Float {
            val prefs: SharedPreferences = context.getSharedPreferences(VALOR_EURO, Context.MODE_PRIVATE)
            return prefs.getFloat(NUMERO_EURO, 0.0f) // 0 es el valor predeterminado si no se encuentra el número
        }
        fun guardarEuroFecha(context: Context, fecha:String) {
            val prefs: SharedPreferences = context.getSharedPreferences(VALOR_EURO, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString(FECHA_EURO, fecha)
            editor.apply()
        }
        fun recuperarEuroFecha(context: Context): String? {
            val prefs: SharedPreferences = context.getSharedPreferences(VALOR_EURO, Context.MODE_PRIVATE)
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

//    private fun initListeners() {
////        val bannerIntent = Intent(requireContext(), InterstitialActivity::class.java)
////        binding.btnImageColaboracion.setOnClickListener { startActivity(bannerIntent) }
//
//        val interstitialIntent = Intent(requireContext(), InterstitialActivity::class.java)
//        binding.btnImageColaboracion.setOnClickListener { startActivity(interstitialIntent) }
//    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        isFragmentAttached = true
    }
    override fun onDetach() {
        super.onDetach()
        isFragmentAttached = false
    }

    private fun actualizarEuro() {
        try {
            // Recuperar el valor del euro guardado
            val savedEuro = ApiResponseHolder.recuperarEuro(requireContext())
            Log.d("EUROACTU", "VALOR DE actualizarEuro: savedEuro $savedEuro")

            // Actualizar el valor del euro desde la web
            CoroutineScope(Dispatchers.IO).launch {
                var intentos = 0
                val maxIntentos = 3
                var obtenido = false

                Log.d(
                    "EUROACTU",
                    "VALOR DE: intentos $intentos maxIntentos $maxIntentos obtenido $obtenido"
                )

                while (!obtenido && intentos < maxIntentos) {
                    try {
                        val document = Jsoup.connect("https://www.bcv.org.ve/").timeout(60000).get()
                        val precioEuro = document.select("#euro strong").first()?.text()
                        val valorEuro = precioEuro?.replace(",", ".")?.toFloatOrNull()

                        // Extraer la fecha del elemento span con la clase date-display-single
                        val fechaElement = document.select("span.date-display-single").firstOrNull()
                        val fecha = fechaElement?.text()

                        withContext(Dispatchers.Main) {
                            if (isAdded) { // Verifica si el Fragment está adjunto
                                if (valorEuro != null) {
                                    // Guardar el nuevo valor del euro y la fecha de actualización
                                    ApiResponseHolder.guardarEuro(requireContext(), valorEuro)
                                    ApiResponseHolder.guardarEuroFecha(
                                        requireContext(),
                                        fecha.toString()
                                    )

                                    Log.d(
                                        "EUROACTU",
                                        "VALOR DE: Dispatchers ENTROO A GUARDARRRRRR "
                                    )

                                    // Marcar como obtenido y salir del bucle
                                    obtenido = true
                                } else {
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
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

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
        if (ultimoTecleado== 0){
            val decimalFormat = DecimalFormat("#,##0.00") // Declaración de DecimalFormat
            var valorDolares = 0.0
            val inputText = binding.inputDolares.text.toString()

            if (inputText.isNotEmpty()) {

                try {
                    if (valorActualDolar != null) {
                        val precioSincoma = inputText.replace("[,]".toRegex(), "") // Elimina puntos y comas
                        // val precioSincoma = precioTexto.toDoubleOrNull() ?: 0.0
                        valorDolares = precioSincoma.toDouble() * valorActualDolar.toDouble()
                    }
                    val formattedValorDolares = decimalFormat.format(valorDolares)
                    binding.inputBolivares.setText(formattedValorDolares)
                }catch (e: NumberFormatException){
                    Log.d("Multiplicacion", "actualzarMultiplicacion: $e")
                }

            } else {
                binding.inputBolivares.text?.clear()
            }
        }
        if (ultimoTecleado==1){

            val decimalFormat = DecimalFormat("#,##0.00") // Declaración de DecimalFormat
            var valorDolares = 0.0
            val inputText = binding.inputBolivares.text.toString()

            if (inputText.isNotEmpty()) {

                try {
                    if (valorActualDolar != null) {
                        val precioSincoma = inputText.replace("[,]".toRegex(), "") // Elimina puntos y comas
                        // val precioSincoma = precioTexto.toDoubleOrNull() ?: 0.0
                        valorDolares = precioSincoma.toDouble() / valorActualDolar.toDouble()
                    }
                    val formattedValorDolares = decimalFormat.format(valorDolares)
                    binding.inputDolares.setText(formattedValorDolares)
                }catch (e: NumberFormatException){
                    Log.d("Multiplicacion", "actualzarMultiplicacion: $e")
                }

            } else {
                binding.inputDolares.text?.clear()
            }

        }

    }

    private fun activarBtnBcv() {
        if (binding.btnBcv.isChecked == true) {
            binding.btnParalelo.isChecked = false
            binding.switchDolar.isChecked = false
        } else {
            binding.btnParalelo.isChecked = true
            binding.switchDolar.isChecked = true
        }
    }

    private fun activarBtnParalelo() {
        if (binding.btnParalelo.isChecked == true) {
            binding.btnBcv.isChecked = true
            binding.switchDolar.isChecked = true
        } else {
            binding.btnBcv.isChecked = false
            binding.switchDolar.isChecked = false
        }
    }

    //LLAMAR A LAS APIS*****************************************************************

    fun llamarBcvNew() {
        try {
            val savedResponseBCV = getResponsePreferencesBcvNew(requireContext())
        Log.d("RESPUESTA", "llamarBcvNew:Try 1 savedResponseBCV $savedResponseBCV")
            if (savedResponseBCV != null) {
                ApiResponseHolder.getResponseBcv(savedResponseBCV)
                valorActualBcv = savedResponseBCV.monitors.usd.price!!.toDouble()
                llenarCampoBCVNew(savedResponseBCV)
                multiplicaDolares()
                dividirABolivares()

                binding.swipeRefreshLayout.isRefreshing = false
            }
        }catch (e: Exception){
            binding.swipeRefreshLayout.isRefreshing = false
            Toast.makeText(requireContext(), "Problemas de obtencion de datos llamarBcvNew $e", Toast.LENGTH_SHORT).show()
            Log.d("RESPUESTA", " llamarBcvNew catch 1 $e ")
        }


        lifecycleScope.launch(Dispatchers.IO) {
            val url = "https://pydolarvenezuela-api.vercel.app/api/v1/dollar?page=bcv"
            val baseUrl = "https://pydolarvenezuela-api.vercel.app/api/v1/dollar/"

            val client = OkHttpClient.Builder().build()
            val request = Request.Builder()
                .url(url)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            val apiService = retrofit.create(ApiService::class.java)

            try {
                val response = apiService.getBcv(url)
                Log.d("RESPUESTA", " llamarBcvNew try 2 RESPONSE: $response ")

                binding.swipeRefreshLayout.isRefreshing = false
                if (response != null) {
                    ApiResponseHolder.getResponseBcv(response)
                    valorActualBcv = response.monitors.usd?.price!!.toDouble()

                    guardarResponseBcvNew(requireContext(), response)

                    withContext(Dispatchers.Main) {
                        animacionCrecerTexto(binding.txtFechaActualizacionBcv)
                       // binding.progressBar.visibility= View.INVISIBLE
                        binding.swipeRefreshLayout.isRefreshing = false
                        binding.txtFechaActualizacionBcv.setTextColor(ContextCompat.getColor(requireContext(),
                            R.color.md_theme_light_surfaceTint))
                        llenarCampoBCVNew(response)
                    }

                    multiplicaDolares()
                    dividirABolivares()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "No se actualizó el dólar BCV. Revise la conexión: $e",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.txtFechaActualizacionBcv.setTextColor(ContextCompat.getColor(requireContext(),R.color.red))
                    animarSwipe()
                   // binding.progressBar.visibility= View.INVISIBLE
                    binding.swipeRefreshLayout.isRefreshing = false
                }
                Log.d("RESPUESTA", " llamarBcvNew caych 2 RESPONSE$e ")

                println("Error: ${e.message}")
            }
        }
    }


    fun llamarDolarNew() {
        try {
            val savedResponseDolar = getResponseFromSharedPreferences(requireContext())

            if (savedResponseDolar != null) {
                Log.d("RESPUESTA", " llamarPrecio Try 1 RESPONSE savedResponseDolar $savedResponseDolar ")
                ApiResponseHolder.setResponse(savedResponseDolar)
                valorActualParalelo = savedResponseDolar.monitors.enparalelovzla.price.toDouble()
                binding.swipeRefreshLayout.isRefreshing = false
                llenarDolarNew(savedResponseDolar)
            }
        }catch (e: Exception){
            binding.swipeRefreshLayout.isRefreshing = false
            Toast.makeText(requireContext(), "Problemas de Conexion $e", Toast.LENGTH_SHORT).show()
            Log.d("RESPUESTA", " llamarPrecio catch 1 RESPONSE$e ")
        }


        lifecycleScope.launch(Dispatchers.IO) {
            val url = "https://pydolarvenezuela-api.vercel.app/api/v1/dollar?page=exchangemonitor"

            val baseUrl = "https://pydolarvenezuela-api.vercel.app/api/v1/dollar/"

            val client = OkHttpClient.Builder().build()
            val request = Request.Builder()
                .url(url)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            val apiService = retrofit.create(ApiService::class.java)

            try {
                val response = apiService.getDolarNew(url)
                Log.d("RESPUESTA", " llamarDolarNew try segundo  $response ")
                binding.swipeRefreshLayout.isRefreshing = false
                if (response != null) {
                    ApiResponseHolder.setResponse(response)
                    valorActualParalelo = response.monitors.enparalelovzla.price.toDouble()
                    guardarResponse(requireContext(), response)
                    withContext(Dispatchers.Main) {

                        animacionCrecerTexto(binding.txtFechaActualizacionPara)
                       // binding.progressBar.visibility= View.INVISIBLE
                        binding.swipeRefreshLayout.isRefreshing = false
                       // binding.txtFechaActualizacionBcv.setTextColor(ContextCompat.getColor(requireContext(),
                       //     R.color.md_theme_light_surfaceTint))
                        llenarDolarNew(response)

                    }
                    multiplicaDolares()
                    dividirABolivares()

                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.d("RESPUESTA", " llamarDolarNew cash 2 segundo  $e ")
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.txtFechaActualizacionBcv.setTextColor(ContextCompat.getColor(requireContext(),R.color.red))
                    animarSwipe()
                 //   binding.progressBar.visibility= View.INVISIBLE
                    binding.swipeRefreshLayout.isRefreshing = false
                }

                println("Error: ${e.message}")
            }
        }
    }


    private fun animacionCrecerTexto(texto:TextView){
        val scaleUpAnimation = ScaleAnimation(
            1f, 1.5f, // De tamaño normal a 1.5 veces el tamaño original
            1f, 1.5f, // Igual para la altura
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
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
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })

        // Iniciar la primera animación
        texto.startAnimation(scaleUpAnimation)

    }
    private fun guardarResponseBcvNew(context: Context, responseBCVNew: BcvNew) {
        val gson = Gson()
        val responseJson = gson.toJson(responseBCVNew)

        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferencesBcvNew", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("dolarBCVNew", responseJson)
        editor.apply()
    }

    private fun guardarResponse(context: Context, responseBCV: DolarNew) {
        val gson = Gson()
        val responseJson = gson.toJson(responseBCV)

        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("dolarBCVResponse", responseJson)
        editor.apply()
    }
    // Define una función para recuperar la respuesta de SharedPreferences
    private fun getResponsePreferencesBcvNew(context: Context): BcvNew? {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferencesBcvNew", AppCompatActivity.MODE_PRIVATE)
        val responseJson = sharedPreferences.getString("dolarBCVNew", null)

        if (responseJson != null) {
            val gson = Gson()

            return gson.fromJson(responseJson, BcvNew::class.java)
        }

        return null // Retorna null si no se encontró la respuesta en SharedPreferences
    }

    // Define una función para recuperar la respuesta de SharedPreferences
    private fun getResponseFromSharedPreferences(context: Context): DolarNew? {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val responseJson = sharedPreferences.getString("dolarBCVResponse", null)

        if (responseJson != null) {
            val gson = Gson()

            return gson.fromJson(responseJson, DolarNew::class.java)
        }

        return null // Retorna null si no se encontró la respuesta en SharedPreferences
    }
    // Define una función para recuperar la respuesta de SharedPreferences
    private fun getResponsePreferencesVzla(context: Context): ParaleloVzla? {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferencesVzla", AppCompatActivity.MODE_PRIVATE)
        val responseJson = sharedPreferences.getString("dolarVzlaNew", null)

        if (responseJson != null) {
            val gson = Gson()

            return gson.fromJson(responseJson, ParaleloVzla::class.java)
        }

        return null // Retorna null si no se encontró la respuesta en SharedPreferences
    }

    fun llenarCampoBCVNew(response: BcvNew) {
        Log.d(
            "llenarCampoBCVNew",
            " llenarCampoBCVNew response $response valor COLORRR ${response.monitors.usd?.color}  "
        )
        // DATOS DEL BCV
        if (!response.monitors.usd.price.isNullOrEmpty()) {
            binding.btnBcv.text = response.monitors.usd?.price
            binding.btnBcv.textOff = response.monitors.usd?.price
            binding.btnBcv.textOn = response.monitors.usd?.price
            val fechaCovertida=cambiarFormatoFecha(response.monitors.last_update)
            binding.txtFechaActualizacionBcv.text = fechaCovertida
            if (response.monitors.usd.color == "red") binding.imgflechaBcv.setImageResource(R.drawable.ic_flecha_roja)
            if (response.monitors.usd.color == "neutral") binding.imgflechaBcv.setImageResource(R.drawable.ic_flecha_igual)
            if (response.monitors.usd.color == "green") binding.imgflechaBcv.setImageResource(R.drawable.ic_flechaverde)

            binding.txtVariacionBcv.text = response.monitors.usd.percent


        }

    }
    fun llenarDolarNew(response: DolarNew) {
        Log.d("RESPUESTA", " llenarCampoBCVNew response $response valor de ${response.monitors.bcv?.price}  ")


        //DATOS DEL PARALELO
        Log.d("RESPUESTA", " llenarCampoBCVNew response $response valor de ${response.monitors.bcv?.price}  ")
        if (!response.monitors.enparalelovzla.price.isNullOrEmpty()){
            binding.btnParalelo.text = response.monitors.enparalelovzla?.price
            binding.btnParalelo.textOff =  response.monitors.enparalelovzla?.price
            binding.btnParalelo.textOn =  response.monitors.enparalelovzla?.price
            binding.txtFechaActualizacionPara.text = response.monitors.enparalelovzla.last_update

            if (response.monitors.enparalelovzla.color=="red") binding.imgFlechaParalelo.setImageResource(R.drawable.ic_flecha_roja)
            if (response.monitors.enparalelovzla.color=="neutral") binding.imgFlechaParalelo.setImageResource(R.drawable.ic_flecha_igual)
            if (response.monitors.enparalelovzla.color=="green") binding.imgFlechaParalelo.setImageResource(R.drawable.ic_flechaverde)
            binding.txtVariacionParalelo.text= response.monitors.enparalelovzla.percent

        }

    }
    fun cambiarFormatoFecha(fecha: String): String {
        // Definir el formato de la fecha de entrada
        val formatoEntrada = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        // Definir el formato de la fecha de salida
        val formatoSalida = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        // Parsear la fecha de entrada al formato LocalDate
        val fechaLocalDate = LocalDate.parse(fecha, formatoEntrada)

        // Formatear la fecha al nuevo formato y devolverla
        return fechaLocalDate.format(formatoSalida)
    }



    private fun multiplicaDolares() {
        val decimalFormat = DecimalFormat("#,##0.00") // Declaración de DecimalFormat
        binding.inputDolares?.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                ultimoTecleado= 1
                var valorDolares = 0.0
                if (binding.inputDolares.isFocused) {
                    val inputText = binding.inputDolares.text.toString()
                    if (inputText.isNotEmpty()) {
                        if (binding.switchDolar.isChecked) {
                            if (valorActualParalelo != null) {
                                val cleanedText =
                                    inputText.replace("[,]".toRegex(), "") // Elimina puntos y comas
                                val parsedValue = cleanedText.toDoubleOrNull() ?: 0.0

                                valorDolares = parsedValue * valorActualParalelo!!.toDouble()
                            }
                        }

                        if (!binding.switchDolar.isChecked) {
                            val cleanedText =
                                inputText.replace("[,]".toRegex(), "") // Elimina puntos y comas
                            val parsedValue = cleanedText.toDoubleOrNull() ?: 0.0
                            if (valorActualBcv != null) valorDolares =
                                parsedValue * valorActualBcv!!.toDouble()
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
                ultimoTecleado= 0
                var valorDolares = 0.0
                if (binding.inputBolivares.isFocused) {
                    val inputText = binding.inputBolivares.text.toString()
                    if (inputText.isNotEmpty()) {
                        if (valorActualParalelo != null) {
                            val cleanedText =
                                inputText.replace("[,]".toRegex(), "") // Elimina puntos y comas
                            val parsedValue = cleanedText.toDoubleOrNull() ?: 0.0
                            valorDolares = parsedValue / valorActualParalelo!!.toDouble()

                        }

                        if (!binding.switchDolar.isChecked) {
                            val cleanedText =
                                inputText.replace("[,]".toRegex(), "") // Elimina puntos y comas
                            val parsedValue = cleanedText.toDoubleOrNull() ?: 0.0
                            valorDolares = parsedValue * valorActualParalelo!!.toDouble()
                            if (valorActualBcv != null) {
                                val numeroFormateado = inputText.replace(",", "")
                                val numero = numeroFormateado.toDoubleOrNull()
                                if (numero!=null) valorDolares = numero.toDouble() / valorActualBcv!!.toDouble()

                            }


                        }


                        val formattedValorDolares = decimalFormat.format(valorDolares)
                        binding.inputDolares.setText(formattedValorDolares)
                    } else {
                        binding.inputDolares.text?.clear()
                    }
                }

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

    }

    private fun copiarDolar() {
        var montoDolarCopy = binding.inputDolares.text.toString()
        if (!montoDolarCopy.isNullOrEmpty()) {
            //montoDolarCopy.toDouble()
            copyToClipboard(requireContext(), montoDolarCopy.toString(), "$montoDolarCopy", "$")
        } else {
            Toast.makeText(requireContext(), "Campo vacio", Toast.LENGTH_SHORT).show()
        }

    }

    //COPIAR LOS DATOS AL PORTAPEPEL
    private fun copiarBs() {
        var montoBolivarCopyLimpio =0.0
        var montoBolivarCopy = binding.inputBolivares.text.toString()
        if (!montoBolivarCopy.isNullOrEmpty()) {
            val cadenaNumerica = montoBolivarCopy
            val cadenaLimpia = cadenaNumerica.replace(",", "")
             montoBolivarCopyLimpio = cadenaLimpia.toDouble()

            montoBolivarCopyLimpio.toDouble()
            copyToClipboard(requireContext(), montoBolivarCopy.toString(), "$montoBolivarCopy", "Bs.")
        } else {
            Toast.makeText(requireContext(), "Campo vacio", Toast.LENGTH_SHORT).show()
        }

    }

    //FUNCION PARA COPIAR AL PORTA PAPEL
    fun copyToClipboard(context: Context, text: String, titulo: String, unidad: String) {
        // Obtener el servicio del portapapeles
        val clipboardManager = context.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager

        // Crear un objeto ClipData para guardar el texto
        val clipData = ClipData.newPlainText("text", text)

        // Copiar el objeto ClipData al portapapeles
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(requireContext(), "Monto Copiado: $titulo $unidad", Toast.LENGTH_SHORT).show()
    }

    private fun animarSwipe() {
        val imageView = binding.imageSwipe
        imageView.visibility= View.VISIBLE
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
                    repeatCount=0
                    imageView.visibility = ImageView.GONE
                }
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        imageView.startAnimation(slideDown)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        lifecycleScope.coroutineContext.cancel()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
      //  binding.progressBar.visibility = View.VISIBLE
        if ( binding.progressBar.visibility!= View.VISIBLE) binding.swipeRefreshLayout.isRefreshing = true
        eliminarListener()
        llamarDolarNew()
        llamarBcvNew()
        listenerImagenConfig()
        //admon appOpen

    }


}



