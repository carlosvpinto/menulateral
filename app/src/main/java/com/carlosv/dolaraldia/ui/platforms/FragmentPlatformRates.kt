package com.carlosv.dolaraldia.ui.platforms

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.ScaleAnimation
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.carlosv.dolaraldia.ApiService
import com.carlosv.dolaraldia.model.apiPlatforms.PlatformDetail
import com.carlosv.dolaraldia.model.apiPlatforms.PlatformResponse
import com.carlosv.dolaraldia.utils.Constants
import com.carlosv.dolaraldia.utils.VibrationHelper.vibrateOnError
import com.carlosv.dolaraldia.utils.VibrationHelper.vibrateOnSuccess
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentPlatformRatesBinding
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PlatformRatesFragment : Fragment() {

    private var _binding: FragmentPlatformRatesBinding? = null
    private val binding get() = _binding!!
    private val TAG = "PlatformRatesFragment"

    private var isOffline = false // Para llevar el registro del estado de conexión

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlatformRatesBinding.inflate(inflater, container, false)

        // Aplicar la animación
        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.appear_from_top)
        binding.root.startAnimation(animation)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Cargamos los datos del caché al iniciar para una carga instantánea
        loadDataFromCache()

        // 2. Llamamos a la API para obtener los datos más recientes
        fetchPlatformRates()

        // 3. Configuramos el listener para el botón de refrescar
        binding.btnRefresh.setOnClickListener {
            fetchPlatformRates()
        }
    }

    // --- LÓGICA PRINCIPAL ---

    private fun fetchPlatformRates() {
        showLoading(true)

        // Creación del cliente Retrofit (reutilizada de tu HomeFragment)
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", Constants.BEARER_TOKEN)
                    .build()
                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.URL_BASE)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        // Lanzamos la corrutina para la llamada de red
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getPlatformRates()
                withContext(Dispatchers.Main) {
                    if (response != null) {
                        handleSuccessfulResponse(response)
                        Log.d(TAG, "fetchPlatformRates: response: $response")
                    } else {
                        // El response fue nulo, lo tratamos como un error
                        handleApiError()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción en la llamada a la API: ${e.message}")
                withContext(Dispatchers.Main) {
                    handleApiError()
                }
            }
        }
    }

    // --- FUNCIÓN handleSuccessfulResponse MODIFICADA ---
    private fun handleSuccessfulResponse(response: PlatformResponse) {
        // Si estábamos en modo offline antes de esta llamada exitosa,
        // significa que la conexión se ha restablecido.
        if (isOffline) {
            binding.imgSinConexion.visibility = View.GONE
            vibrateOnSuccess(requireContext())
            showStatusSnackbar("¡Conexión restablecida!")
            isOffline = false // Volvemos al estado online
        }

        updateUI(response.platforms)
        saveResponseToCache(requireContext(), response)
        showLoading(false)
        animateUpdateDates()
    }

    // --- FUNCIÓN handleApiError MODIFICADA ---
    private fun handleApiError() {
        // Mostramos el ícono de sin conexión y vibramos
        binding.imgSinConexion.visibility = View.VISIBLE
        vibrateOnError(requireContext())
        showLoading(false)
        showStatusSnackbar("No se pudo actualizar. Verifique su conexión.")
        isOffline = true // Marcamos que estamos en modo offline
    }

    // --- NUEVA FUNCIÓN PARA MOSTRAR MENSAJES (Snackbar) ---
    private fun showStatusSnackbar(message: String) {
        // Usamos la vista raíz del fragmento como ancla para el Snackbar
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun updateUI(platforms: Map<String, PlatformDetail>) {
        // Usamos 'let' para manejar de forma segura los posibles valores nulos
        platforms["binance"]?.let { platform ->
            binding.binanceRow.tvPlatformName.text = platform.title
            binding.binanceRow.tvPrice.text = "Bs ${"%.2f".format(platform.price)}"
            binding.binanceRow.tvPercentage.text = "${platform.symbol} ${"%.2f".format(platform.percent)}%"
            binding.binanceRow.tvPercentage.setTextColor(ContextCompat.getColor(requireContext(), if (platform.color == "green") R.color.green else R.color.red))
            binding.binanceRow.tvLastUpdate.text = platform.lastUpdate
            Glide.with(this).load(R.drawable.binance_svg).into(binding.binanceRow.ivLogo)
        }

        platforms["bybit"]?.let { platform ->
            binding.bybitRow.tvPlatformName.text = platform.title
            binding.bybitRow.tvPrice.text = "Bs ${"%.2f".format(platform.price)}"
            binding.bybitRow.tvPercentage.text = "${platform.symbol} ${"%.2f".format(platform.percent)}%"
            binding.bybitRow.tvPercentage.setTextColor(ContextCompat.getColor(requireContext(), if (platform.color == "green") R.color.green else R.color.red))
            binding.bybitRow.tvLastUpdate.text = platform.lastUpdate
            Glide.with(this).load(R.drawable.bybit_svg).into(binding.bybitRow.ivLogo)
        }

        platforms["yadio"]?.let { platform ->
            binding.yadioRow.tvPlatformName.text = platform.title
            binding.yadioRow.tvPrice.text = "Bs ${"%.2f".format(platform.price)}"
            binding.yadioRow.tvPercentage.text = "${platform.symbol} ${"%.2f".format(platform.percent)}%"
            binding.yadioRow.tvPercentage.setTextColor(ContextCompat.getColor(requireContext(), if (platform.color == "green") R.color.green else R.color.red))
            binding.yadioRow.tvLastUpdate.text = platform.lastUpdate
            Glide.with(this).load(R.drawable.yadio_svg).into(binding.yadioRow.ivLogo)
        }
    }

    // --- FUNCIÓN showLoading() MODIFICADA ---
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.btnRefresh.visibility = View.GONE
            binding.progressBarRefresh.visibility = View.VISIBLE
        } else {
            binding.btnRefresh.visibility = View.VISIBLE
            binding.progressBarRefresh.visibility = View.GONE
        }
    }

    // --- NUEVA FUNCIÓN PARA LA ANIMACIÓN DE "PULSO" ---
    private fun animateUpdateDates() {
        val textViewsToAnimate = listOf(
            binding.binanceRow.tvLastUpdate,
            binding.bybitRow.tvLastUpdate,
            binding.yadioRow.tvLastUpdate
        )

        textViewsToAnimate.forEach { textView ->
            animatePulse(textView)
        }
    }

    private fun animatePulse(textView: TextView) {
        val scaleUp = ScaleAnimation(
            1f, 1.2f, // Escala de 100% a 120%
            1f, 1.2f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 300 // ms
            fillAfter = false
        }

        val scaleDown = ScaleAnimation(
            1.2f, 1f, // Escala de 120% de vuelta a 100%
            1f, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 300 // ms
            fillAfter = false
        }

        scaleUp.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                textView.startAnimation(scaleDown)
            }
            override fun onAnimationRepeat(animation: Animation?) {}
        })

        textView.startAnimation(scaleUp)
    }

    // --- LÓGICA DE CACHÉ (REUTILIZADA DE TU HOMEFRAGMENT) ---

    private fun saveResponseToCache(context: Context, response: PlatformResponse) {
        val gson = Gson()
        val responseJson = gson.toJson(response)

        // Usamos un nombre de archivo de preferencias diferente para no sobreescribir el del Home
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("PlatformRatesPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("platformRatesResponse", responseJson)
        editor.apply()
    }

    private fun getResponseFromCache(context: Context): PlatformResponse? {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("PlatformRatesPreferences", Context.MODE_PRIVATE)
        val responseJson = sharedPreferences.getString("platformRatesResponse", null)

        return if (responseJson != null) {
            val gson = Gson()
            try {
                gson.fromJson(responseJson, PlatformResponse::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Error al decodificar el caché de plataformas: ${e.message}")
                null // Retorna nulo si el JSON guardado está corrupto
            }
        } else {
            null
        }
    }

    private fun loadDataFromCache() {
        val cachedResponse = getResponseFromCache(requireContext())
        if (cachedResponse != null) {
            Log.d(TAG, "Cargando datos de plataformas desde el caché.")
            updateUI(cachedResponse.platforms)
        } else {
            Log.d(TAG, "No hay datos de plataformas en el caché.")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}