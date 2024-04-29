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
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.ScaleAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.lifecycleScope
import com.carlosv.dolaraldia.ApiService
import com.carlosv.dolaraldia.InterstitialActivity

import com.carlosv.dolaraldia.model.bancos.DolarNew
import com.carlosv.dolaraldia.model.bcv.BcvNew
import com.carlosv.dolaraldia.model.paralelo.ParaleloVzla
import com.carlosv.dolaraldia.ui.bancos.BancosModel
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentHomeBinding
import com.facebook.gamingservices.cloudgaming.InAppAdLibrary.loadInterstitialAd
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
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
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.*




class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    private val binding get() = _binding ?: throw IllegalStateException("Binding is null")
    private var isFragmentAttached: Boolean = false

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



        //PARA CARGAR ADMOB
        mAdView= binding.adView
       // mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)



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
            llamarDolarNew()
            llamarBcvNew()
            actualizarEuro()

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
//        binding.progressBar.setOnClickListener {
//            binding.progressBar.visibility = View.INVISIBLE
//        }

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
//    fun llamarParaVzla() {
//        try {
//            val savedResponseVzla = getResponsePreferencesVzla(requireContext())
//            Log.d("RESPUESTA", "llamarParaVzla: TRY 1 savedResponseVzla $savedResponseVzla ")
//            if (savedResponseVzla != null) {
//                ApiResponseHolder.getResponseParalelovzla(savedResponseVzla)
//
//                valorActualParalelo = savedResponseVzla.price.toDouble()
//                //llenarCampoBCV(savedResponseBCV)
//                llenarParaleloVzla(savedResponseVzla)
//                multiplicaDolares()
//                dividirABolivares()
//                binding.swipeRefreshLayout.isRefreshing = false
//            }
//        }catch (e: Exception){
//            binding.swipeRefreshLayout.isRefreshing = false
//            animarSwipe()
//           // Toast.makeText(requireContext(), "Problemas de Conexion $e", Toast.LENGTH_SHORT).show()
//            Log.d("RESPUESTA", "llamarParaVzla: catch 1 savedResponseBCV $e ")
//        }
//
//
//        lifecycleScope.launch(Dispatchers.IO) {
//
//            val url = "https://pydolarvenezuela-api.vercel.app/api/v1/dollar/unit/enparalelovzla"
//            val baseUrl = "https://pydolarvenezuela-api.vercel.app/api/v1/dollar/"
//
//            val client = OkHttpClient.Builder().build()
//            val request = Request.Builder()
//                .url(url)
//                .build()
//
//            val retrofit = Retrofit.Builder()
//                .baseUrl(baseUrl)
//                .addConverterFactory(GsonConverterFactory.create())
//                .client(client)
//                .build()
//
//            val apiService = retrofit.create(ApiService::class.java)
//
//            try {
//                val response = apiService.getParalelovzla(url)
//                Log.d("RESPUESTA", "llamarParaVzla: TRY 2 response $response ")
//                binding.swipeRefreshLayout.isRefreshing = false
//                if (response != null) {
//                    ApiResponseHolder.getResponseParalelovzla(response)
//                    valorActualParalelo = response.price.toDouble()
//
//                    guardarResponseVzla(requireContext(), response)
//
//                    withContext(Dispatchers.Main) {
//                        animacionCrecerTexto(binding.txtFechaActualizacionPara)
//                        binding.swipeRefreshLayout.isRefreshing = false
//                        binding.txtFechaActualizacionPara.setTextColor(ContextCompat.getColor(requireContext(),
//                            R.color.md_theme_light_surfaceTint))
//                        llenarParaleloVzla(response)
//                    }
//
//                    multiplicaDolares()
//                    dividirABolivares()
//                }
//
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(
//                        requireContext(),
//                        "No se actualizó el dólar Paralelo! Revise la conexión: $e",
//                        Toast.LENGTH_LONG
//                    ).show()
//                    animarSwipe()
//                    binding.txtFechaActualizacionPara.setTextColor(ContextCompat.getColor(requireContext(),R.color.red))
//                    Log.d("RESPUESTA", "llamarParaVzla: catch 2 response $e ")
//                    binding.swipeRefreshLayout.isRefreshing = false
//                    binding.progressBar.visibility= View.INVISIBLE
//                }
//
//                println("Error: ${e.message}")
//            }
//        }
//    }
//
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
    private fun guardarResponseVzla(context: Context, responseBCVNew: ParaleloVzla) {
        val gson = Gson()
        val responseJson = gson.toJson(responseBCVNew)

        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferencesVzla", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("dolarVzlaNew", responseJson)
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
            binding.txtFechaActualizacionBcv.text = response.monitors.last_update

            if (response.monitors.usd.color == "red") binding.imgflechaBcv.setImageResource(R.drawable.ic_flecha_roja)
            if (response.monitors.usd.color == "neutral") binding.imgflechaBcv.setImageResource(R.drawable.ic_flecha_igual)
            if (response.monitors.usd.color == "green") binding.imgflechaBcv.setImageResource(R.drawable.ic_flechaverde)
            binding.txtVariacionBcv.text = response.monitors.usd.percent


        }

    }
    fun llenarDolarNew(response: DolarNew) {
        Log.d("RESPUESTA", " llenarCampoBCVNew response $response valor de ${response.monitors.bcv?.price}  ")
        // DATOS DEL BCV
//        if (!response.monitors.bcv.price.isNullOrEmpty()){
//            binding.btnBcv.text = response.monitors.bcv?.price
//            binding.btnBcv.textOff =  response.monitors.bcv?.price
//            binding.btnBcv.textOn =  response.monitors.bcv?.price
//            binding.txtFechaActualizacionBcv.text = response.monitors.bcv.last_update
//
//            if (response.monitors.bcv.color=="red") binding.imgflechaBcv.setImageResource(R.drawable.ic_flecha_roja)
//            if (response.monitors.bcv.color=="neutral") binding.imgflechaBcv.setImageResource(R.drawable.ic_flecha_igual)
//            if (response.monitors.bcv.color=="green") binding.imgflechaBcv.setImageResource(R.drawable.ic_flechaverde)
//            binding.txtVariacionBcv.text= response.monitors.bcv.percent
//
//
//
//
//        }

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


    private fun multiplicaDolares() {
        val decimalFormat = DecimalFormat("#,##0.00") // Declaración de DecimalFormat
        Log.d(TAG, "multiplicaDolares: entro a funcion")
        binding.inputDolares?.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                ultimoTecleado= 1
                var valorDolares = 0.0
                if (binding.inputDolares.isFocused) {
                    val inputText = binding.inputDolares.text.toString()
                    if (inputText.isNotEmpty()) {
                        Log.d(TAG, "afterTextChanged: entro a isNotEmpy")
                        if (binding.switchDolar.isChecked) {
                            Log.d(TAG, "afterTextChanged: valorActualParalelo $valorActualParalelo")
                            if (valorActualParalelo != null) {
                                val cleanedText =
                                    inputText.replace("[,]".toRegex(), "") // Elimina puntos y comas
                                val parsedValue = cleanedText.toDoubleOrNull() ?: 0.0

                                valorDolares = parsedValue * valorActualParalelo!!.toDouble()
                                Log.d(TAG, "afterTextChanged: valorDolares $valorDolares")
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

    override fun onStart() {
        super.onStart()

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
        llamarDolarNew()
        llamarBcvNew()
    }


}



