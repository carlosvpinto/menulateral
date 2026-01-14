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
import com.carlosv.dolaraldia.AppPreferences
import com.carlosv.dolaraldia.model.apiPlatforms.PlatformDetail
import com.carlosv.dolaraldia.model.apiPlatforms.PlatformResponse
import com.carlosv.dolaraldia.utils.Constants
import com.carlosv.dolaraldia.utils.Constants.AD_UNIT_ID_INTER_PLATAFOR
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

// --- IMPORTACIONES DE ADMOB ---
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.AdError

class PlatformRatesFragment : Fragment() {

    private var _binding: FragmentPlatformRatesBinding? = null
    private val binding get() = _binding!!
    private val TAG = "PlatformRatesFragment"

    private var isOffline = false

    // --- VARIABLES PARA EL ANUNCIO INTERSTICIAL ---
    private var mInterstitialAd: InterstitialAd? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlatformRatesBinding.inflate(inflater, container, false)

        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.appear_from_top)
        binding.root.startAnimation(animation)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Cargamos datos
        loadDataFromCache()

        // 2. Llamada API
        fetchPlatformRates()

        // 3. Listener refresh
        binding.btnRefresh.setOnClickListener {
            fetchPlatformRates()
        }

        // 4. --- CARGAR Y MOSTRAR EL ANUNCIO ---
        loadInterstitialAd()
    }

    // --- FUNCIÓN PARA CARGAR EL INTERSTICIAL ---
    // --- FUNCIÓN PARA CARGAR EL INTERSTICIAL ---
    private fun loadInterstitialAd() {

        // 1. CHEQUEO PREMIUM: Si paga, no hacemos nada.
        if (AppPreferences.isUserPremiumActive()) {
           // Log.d(TAG, "Usuario Premium: Anuncio Intersticial cancelado.")
            return // <--- ¡AQUÍ SE SALE DE LA FUNCIÓN!
        }

        // 2. Si no es premium, continúa la carga normal...
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(requireContext(), AD_UNIT_ID_INTER_PLATAFOR, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
                Log.d(TAG, "onAdLoaded: Intersticial cargado.")

                if (isAdded && activity != null) {
                    showInterstitial()
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.d(TAG, "onAdFailedToLoad: ${loadAdError.message}")
                mInterstitialAd = null
            }
        })
    }

    // --- FUNCIÓN PARA MOSTRAR EL INTERSTICIAL ---
    private fun showInterstitial() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Se llamó cuando se cerró el anuncio.
                    Log.d(TAG, "El anuncio fue cerrado.")
                    mInterstitialAd = null
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Se llamó cuando el anuncio falló al mostrarse.
                    Log.d(TAG, "El anuncio falló al mostrarse.")
                    mInterstitialAd = null
                }

                override fun onAdShowedFullScreenContent() {
                    // Se llamó cuando se mostró el anuncio.
                    mInterstitialAd = null
                }
            }
            // Mostrar el anuncio
            activity?.let { mInterstitialAd?.show(it) }
        } else {
            Log.d(TAG, "El anuncio intersticial no estaba listo aún.")
        }
    }


    // --- LÓGICA PRINCIPAL (TU CÓDIGO ORIGINAL SIN CAMBIOS) ---

    private fun fetchPlatformRates() {
        showLoading(true)

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

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getPlatformRates()
                withContext(Dispatchers.Main) {
                    if (response != null) {
                        handleSuccessfulResponse(response)
                        Log.d(TAG, "fetchPlatformRates: response: $response")
                    } else {
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

    private fun handleSuccessfulResponse(response: PlatformResponse) {
        if (isOffline) {
            binding.imgSinConexion.visibility = View.GONE
            vibrateOnSuccess(requireContext())
            showStatusSnackbar("¡Conexión restablecida!")
            isOffline = false
        }

        updateUI(response.platforms)
        saveResponseToCache(requireContext(), response)
        showLoading(false)
        animateUpdateDates()
    }

    private fun handleApiError() {
        binding.imgSinConexion.visibility = View.VISIBLE
        vibrateOnError(requireContext())
        showLoading(false)
        showStatusSnackbar("No se pudo actualizar. Verifique su conexión.")
        isOffline = true
    }

    private fun showStatusSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun updateUI(platforms: Map<String, PlatformDetail>) {
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

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.btnRefresh.visibility = View.GONE
            binding.progressBarRefresh.visibility = View.VISIBLE
        } else {
            binding.btnRefresh.visibility = View.VISIBLE
            binding.progressBarRefresh.visibility = View.GONE
        }
    }

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
            1f, 1.2f,
            1f, 1.2f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 300
            fillAfter = false
        }

        val scaleDown = ScaleAnimation(
            1.2f, 1f,
            1f, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 300
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

    private fun saveResponseToCache(context: Context, response: PlatformResponse) {
        val gson = Gson()
        val responseJson = gson.toJson(response)
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
                null
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