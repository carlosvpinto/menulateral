package com.carlosv.dolaraldia.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.carlosv.dolaraldia.ApiService
import com.carlosv.dolaraldia.model.bcv.BcvNew
import com.carlosv.dolaraldia.model.paralelo.ParaleloVzla
import com.carlosv.dolaraldia.ui.bancos.BancosModel
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentHomeBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
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
    lateinit var mAdView : AdView

    private var repeatCount = 0



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

        llamarBcvNew()
        llamarParaVzla()
        llamarPrecioOtros()

        MobileAds.initialize(requireContext()) {}

        //PARA CARGAR ADMOB
        mAdView= binding.adView
       // mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        // Obtén una referencia a SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("MiPreferencia", AppCompatActivity.MODE_PRIVATE  )
        // Obtener referencia a SharedPreferences

        // Recuperar el valor entero
        numeroNoturno = sharedPreferences.getInt("numero_noturno", 0)

        //VERIFICA SI QUE MEDO TIENE GUARDADO
       // setDayNight(modoDark())
        binding.swipeRefreshLayout.setOnRefreshListener {
           binding.swipeRefreshLayout.isRefreshing = true
            llamarPrecioOtros()
            llamarBcvNew()
            llamarParaVzla()
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

    override fun onStart() {
        super.onStart()

    }

    //INTERFACE PARA COMUNICAR CON EL ACTIVITY
    object ApiResponseHolder {
        private var response: BancosModel? = null
        private var responseBcv: BcvNew? = null
        private var responsePVzla: ParaleloVzla? = null
        private var precioEuro: Double? = null
        private const val VALOR_EURO = "ValorEuro"
        private const val NUMERO_EURO = "euro"
        private const val FECHA_EURO = "fecha"

        fun getResponseParalelovzla(savedResponseVzla: ParaleloVzla): ParaleloVzla? {
            return savedResponseVzla
        }
        fun getResponseBcv(responseBcvNew: BcvNew):BcvNew? {
            return responseBcvNew
        }
        fun getResponse(): BancosModel? {
            return response
        }

        fun setResponse(newResponse: BancosModel) {
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
    fun llamarParaVzla() {
        try {
            val savedResponseVzla = getResponsePreferencesVzla(requireContext())
            Log.d("RESPUESTA", "llamarParaVzla: TRY 1 savedResponseVzla $savedResponseVzla ")
            if (savedResponseVzla != null) {
                ApiResponseHolder.getResponseParalelovzla(savedResponseVzla)

                valorActualParalelo = savedResponseVzla.price.toDouble()
                //llenarCampoBCV(savedResponseBCV)
                llenarParaleloVzla(savedResponseVzla)
                multiplicaDolares()
                dividirABolivares()
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }catch (e: Exception){
            binding.swipeRefreshLayout.isRefreshing = false
            animarSwipe()
           // Toast.makeText(requireContext(), "Problemas de Conexion $e", Toast.LENGTH_SHORT).show()
            Log.d("RESPUESTA", "llamarParaVzla: catch 1 savedResponseBCV $e ")
        }


        lifecycleScope.launch(Dispatchers.IO) {
            val url = "https://pydolarvenezuela-api.vercel.app/api/v1/dollar/unit/enparalelovzla"
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
                val response = apiService.getParalelovzla(url)
                Log.d("RESPUESTA", "llamarParaVzla: TRY 2 response $response ")
                binding.swipeRefreshLayout.isRefreshing = false
                if (response != null) {
                    ApiResponseHolder.getResponseParalelovzla(response)
                    valorActualParalelo = response.price.toDouble()

                    guardarResponseVzla(requireContext(), response)

                    withContext(Dispatchers.Main) {
                        binding.swipeRefreshLayout.isRefreshing = false
                        binding.txtFechaActualizacionPara.setTextColor(ContextCompat.getColor(requireContext(),
                            R.color.md_theme_light_surfaceTint))
                        llenarParaleloVzla(response)
                    }

                    multiplicaDolares()
                    dividirABolivares()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "No se actualizó el dólar Paralelo! Revise la conexión: $e",
                        Toast.LENGTH_LONG
                    ).show()
                    animarSwipe()
                    binding.txtFechaActualizacionPara.setTextColor(ContextCompat.getColor(requireContext(),R.color.red))
                    Log.d("RESPUESTA", "llamarParaVzla: catch 2 response $e ")
                    binding.swipeRefreshLayout.isRefreshing = false
                }

                println("Error: ${e.message}")
            }
        }
    }

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
                Log.d("RESPUESTA", " llamarBcvNew try 2 RESPONSE$response ")
                binding.swipeRefreshLayout.isRefreshing = false
                if (response != null) {
                    ApiResponseHolder.getResponseBcv(response)
                    valorActualBcv = response.monitors.usd?.price!!.toDouble()

                    guardarResponseBcvNew(requireContext(), response)

                    withContext(Dispatchers.Main) {
                        binding.swipeRefreshLayout.isRefreshing = false
                        binding.txtFechaActualizacionPara.setTextColor(ContextCompat.getColor(requireContext(),
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
                    binding.swipeRefreshLayout.isRefreshing = false
                }
                Log.d("RESPUESTA", " llamarBcvNew caych 2 RESPONSE$e ")

                println("Error: ${e.message}")
            }
        }
    }

    fun llamarPrecioOtros() {
        try {
            val savedResponseBCV = getResponseFromSharedPreferences(requireContext())

            if (savedResponseBCV != null) {
                ApiResponseHolder.setResponse(savedResponseBCV)
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }catch (e: Exception){
            binding.swipeRefreshLayout.isRefreshing = false
            Toast.makeText(requireContext(), "Problemas de Conexion $e", Toast.LENGTH_SHORT).show()
            Log.d("llamarPrecioOtros", " llamarPrecio catch 1 RESPONSE$e ")
        }


        lifecycleScope.launch(Dispatchers.IO) {
            //val url = "https://pydolarvenezuela-api.vercel.app/api/v1/dollar/page?page=exchangemonitor"
            val url = "https://pydolarvenezuela-api.vercel.app/api/v1/dollar"

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
                val response = apiService.getBancos(url)
                Log.d("llamarPrecioOtros", " llamarPrecioOtros try segundo  $response ")
                binding.swipeRefreshLayout.isRefreshing = false
                if (response != null) {
                    ApiResponseHolder.setResponse(response)
                    guardarResponse(requireContext(), response)
                    withContext(Dispatchers.Main) {
                        binding.swipeRefreshLayout.isRefreshing = false
                        binding.txtFechaActualizacionPara.setTextColor(ContextCompat.getColor(requireContext(),
                            R.color.md_theme_light_surfaceTint))

                    }

                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.txtFechaActualizacionPara.setTextColor(ContextCompat.getColor(requireContext(),R.color.red))
                    Log.d("llamarPrecioOtros", " llamarPrecioOtros catch segundo  $e ")
                    binding.swipeRefreshLayout.isRefreshing = false
                }

                println("Error: ${e.message}")
            }
        }
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

    private fun guardarResponse(context: Context, responseBCV: BancosModel) {
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
    private fun getResponseFromSharedPreferences(context: Context): BancosModel? {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val responseJson = sharedPreferences.getString("dolarBCVResponse", null)

        if (responseJson != null) {
            val gson = Gson()

            return gson.fromJson(responseJson, BancosModel::class.java)
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
        Log.d("RESPUESTA", " llenarCampoBCVNew response $response valor de ${response.monitors.usd?.price}  ")
        if (!response.monitors.usd?.price.isNullOrEmpty()){
            binding.btnBcv.text = response.monitors.usd?.price
            binding.btnBcv.textOff =  response.monitors.usd?.price
            binding.btnBcv.textOn =  response.monitors.usd?.price
            binding.txtFechaActualizacionBcv.text = response.monitors.last_update
        }
    }


    fun llenarParaleloVzla(response: ParaleloVzla) {
        Log.d("RESPUESTA", " llenarParaleloVzla DEL response $response valor de ${response.price}  ")
        if (!response.price.isNullOrEmpty()){
            binding.btnParalelo.text = response.price
            binding.btnParalelo.textOff =  response.price
            binding.btnParalelo.textOn =  response.price
            binding.txtFechaActualizacionPara.text= response.last_update

        }
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
}



