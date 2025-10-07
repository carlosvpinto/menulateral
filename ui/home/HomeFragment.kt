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

import android.content.SharedPreferences
import android.content.pm.PackageManager

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
import androidx.navigation.fragment.findNavController
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
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

import com.carlosv.dolaraldia.AppPreferences

import com.carlosv.dolaraldia.model.FCMBody
import com.carlosv.dolaraldia.model.FCMResponse
import com.carlosv.dolaraldia.model.apiAlcambioEuro.ApiOficialTipoCambio
import com.carlosv.dolaraldia.model.apicontoken.ApiConTokenResponse
import com.carlosv.dolaraldia.model.apicontoken2.ApiModelResponseCripto
import com.carlosv.dolaraldia.model.apicontoken2.ApiModelResponseBCV
import com.carlosv.dolaraldia.model.clickAnuncios.ClickAnunicosModel
import com.carlosv.dolaraldia.model.controlPublicidad.ConfigImagenModel
import com.carlosv.dolaraldia.model.history.HistoryModelResponse
import com.carlosv.dolaraldia.provider.ClickAnuncioProvider
import com.carlosv.dolaraldia.provider.RegistroPubliProvider
import com.carlosv.dolaraldia.services.NotificationProvider
import com.carlosv.dolaraldia.ui.debug.DebugPremiumFragment
import com.carlosv.dolaraldia.utils.Constants
import com.carlosv.dolaraldia.utils.VibrationHelper.vibrateOnError
import com.carlosv.dolaraldia.utils.VibrationHelper.vibrateOnSuccess
import com.carlosv.dolaraldia.utils.ads.RewardedAdManager
import com.carlosv.dolaraldia.utils.premiun.PremiumDialogManager
import com.denzcoskun.imageslider.ImageSlider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.Calendar


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
    private var valorActualEuro: Double? = 0.0
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

    private var diaActual: Boolean= false

    private val notificationProvider = NotificationProvider()

    private val premiumDialogManager: PremiumDialogManager by lazy {
        PremiumDialogManager(requireContext())
    }

    // ¡NUEVO! Creamos una instancia del gestor de anuncios bonificados.
    private lateinit var rewardedAdManager: RewardedAdManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //Inicializamos el gestor.
        rewardedAdManager = RewardedAdManager(requireContext())

        //Llama a la función para actualizar la visibilidad del ícono premium.
        updatePremiumIconVisibility()


        visibleLayoutProxBcv += 1

        configurarBannerWhatsApp()
        //cargarDisponibleIOs()
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


        setupClickListeners() // Una nueva función para organizar los listeners.

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
            refreshAllApis()
        }


        //MODO DESRROLLLO BORRAR datos PREMIUN Y CONTADOR**********
//        binding.imglogo.setOnClickListener {
//
//            // 1. Obtenemos el NavController del fragmento actual.
//            val navController = findNavController()
//
//            // 2. Le pedimos que navegue al ID del nuevo destino que definimos en el XML.
//            navController.navigate(R.id.nav_debug_premium)
//
//            // Devuelve 'true' para indicar que el evento ha sido manejado.
//            true
//        }


        // Aplicar la animación
        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.appear_from_top)
        root.startAnimation(animation)

        //***********************
        return root
    }

    private fun setupClickListeners() {

        binding.imgpremium.setOnClickListener {
           // showPremiumExpirationDate()
            findNavController().navigate(R.id.action_nav_home_to_premiumStatusFragment)
        }

        binding.buttonRewardedAd.setOnClickListener {
            // Le pedimos al gestor que muestre el anuncio.
            rewardedAdManager.showAd(requireActivity(), object : RewardedAdManager.RewardListener {

                override fun onRewardEarned() {
                    // ¡Éxito! El usuario vio el video. Otorgamos la recompensa.
                    AppPreferences.setUserAsPremium("Recompensa",4) // 4 horas
                    Toast.makeText(requireContext(), "¡Recompensa obtenida! Disfruta de 4 horas sin publicidad.", Toast.LENGTH_LONG).show()

                    // (Opcional) Actualizamos la UI inmediatamente para ocultar el ícono de premium.
                    updatePremiumIconVisibility()
                }

                override fun onAdFailedToLoad() {
                    Toast.makeText(requireContext(), "El anuncio no se pudo cargar. Intenta más tarde.", Toast.LENGTH_SHORT).show()
                }

                override fun onAdNotReady() {
                    Toast.makeText(requireContext(), "El anuncio no está listo todavía. Intenta de nuevo en unos segundos.", Toast.LENGTH_SHORT).show()
                }
            })
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshAllApis()
        }
        binding.btnRefres.setOnClickListener {
            refreshAllApis()
        }

        binding.SwUtimaAct.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {

                val savedResponseDolar = getResponseApiDolar(requireContext())
                cambioSwictValor(savedResponseDolar, true)
            } else {

                val savedResponseDolar = getResponseApiDolar(requireContext())
                cambioSwictValor(savedResponseDolar, false)
            }
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
        binding.btnEuroP.setOnClickListener {
            activarBtnEuro()
            val valorDolar = binding.btnEuroP.text.toString()

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

        binding.btnRefres.setOnClickListener {
            comenzarCarga()
            llamarApiDolar { isSuccessful ->

                // Solo habilitar el botón si ambas APIs responden
                if (isSuccessful) {
                    finalizarCarga()
                } else {
                    finalizarCarga()
                }
            }


        }

        binding.imgVerDifBs.setOnClickListener {
            calcularDiferencia()
            //showCustomSnackbar("nada")
        }

        // Asigna un listener para los cambios de estado del Switch
        binding.switchDolar.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Si el switch está activado, simula un clic en el botón de Euro
                binding.btnEuroP.performClick()
            } else {
                // Si el switch está desactivado, simula un clic en el botón de BCV
                binding.btnBcv.performClick()
            }
        }


        // ... (Copia aquí el resto de tus setOnClickListener de onCreateView)
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
    private fun cambioSwictValor(responseMostrar: ApiOficialTipoCambio?, diaActualTem: Boolean) {

        if (responseMostrar != null) {
            llenarCampoBCVNew(responseMostrar,diaActualTem)
            llenarDolarEuro(responseMostrar,diaActualTem)
            //llenarCampoPromedio(responseMostrar)
            val valorDolar = valorBotonActivo()
            // Actualiza la multiplicación con el valor
            animacionCrecerBoton(
                binding.btnBcv,
                binding.btnEuroP,
                binding.txtFechaActualizacionBcv
            )
            actualzarMultiplicacion(valorDolar)
        }

    }


    //Devuelve el Valor del Boton Activo
    private fun valorBotonActivo(): Double {
        val btnBcv = binding.btnBcv
        val btnParalelo = binding.btnEuroP
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
            val nombreAnuncio = "Compartor App Ios"
            val uri = url

            val clickAnuncioModel = ClickAnunicosModel(

                articulo = nombreAnuncio,
                date = Date(),
                timestamp = Date().time,
                pagina = pagina


            )

            clickAnuncioProvider.create(clickAnuncioModel).addOnCompleteListener { it ->
                if (it.isSuccessful) {

                     //Toast.makeText(requireContext(), "GUARDANDO DATOS DEL CLICK", Toast.LENGTH_LONG).show()

                } else {
                    Log.d(TAG, "crearImagenUrl: ${it.exception}")
//                    Toast.makeText(requireContext(), "Error ${it.exception}", Toast.LENGTH_LONG)
//                        .show()
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



    //PUBLICIDAD INTERNA*****************************


    private fun configurarBannerWhatsApp() {
        val bannerImageView = binding.bannerPromocionIos
        val urlImagenBanner = "https://firebasestorage.googleapis.com/v0/b/dolar-mexico-739d5.firebasestorage.app/o/compartir_ios_gif2.gif?alt=media&token=8e3e621e-f46e-4d09-b533-20517185f9bb"

        // --- Carga del GIF con diagnóstico (versión corregida) ---
        Glide.with(this)
            .asGif()
            //.load(R.drawable.compartir_ios_gif2)
            .load(urlImagenBanner)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            // CORRECCIÓN: El listener ahora es específico para GifDrawable
            .listener(object : RequestListener<GifDrawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<GifDrawable>,
                    isFirstResource: Boolean,
                ): Boolean {
                    Log.e("Glide_GIF", "Error crítico al cargar el GIF.", e)
                    return false
                }

                override fun onResourceReady(
                    resource: GifDrawable,
                    model: Any,
                    target: Target<GifDrawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean,
                ): Boolean {
                    Log.d("Glide_GIF", "El recurso GIF ha sido cargado exitosamente.")
                    resource.setLoopCount(GifDrawable.LOOP_FOREVER) // Asegurar repetición
                    return false
                }

            })
            .into(bannerImageView)


        // --- Configuración del clic (sin cambios) ---
        bannerImageView.setOnClickListener {
            guardarClickAnuncio()
            val linkAppStore = "https://www.dolaraldiavzla.com/descarga/"
            val mensajeParaCompartir = "¡Hola! Te recomiendo esta app para seguir el dólar en Venezuela, ahora también disponible para iPhone. Descárgala aquí: $linkAppStore"

            val intent = Intent(Intent.ACTION_VIEW)
            try {
                val urlEncodedMessage = Uri.encode(mensajeParaCompartir)
                intent.data = Uri.parse("https://api.whatsapp.com/send?text=$urlEncodedMessage")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "WhatsApp no está instalado en este dispositivo.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    //INTERFACE PARA COMUNICAR CON EL ACTIVITY
    object ApiResponseHolder {
        private var response: ApiConTokenResponse? = null
        private var responseApiNew: ApiOficialTipoCambio? = null
        private var responseApiEuroBcv: ApiOficialTipoCambio? = null

        private var responseApiOriginal: ApiOficialTipoCambio? = null
        private var responseApiNew2: ApiModelResponseCripto? = null
        private var responseApiBancoNew2: ApiModelResponseBCV? = null
        private var responseHistoryBcv: HistoryModelResponse? = null
        private var responseHistoryParalelo: HistoryModelResponse? = null


        private const val VALOR_EURO = "ValorEuro"
        private const val NUMERO_EURO = "euro"
        private const val FECHA_EURO = "fecha"
        private const val VALOR_DOLAR = "ValorDolar"
        private const val NUMERO_DOLAR = "ValorEuro"

        fun getTasaVentaBcv(): Double {
            // Usamos la misma fuente de datos que usa tu UI (llamarApiTipoCambio)
            val respuesta = getResponseEuroTipoCambio() // O getResponse(), dependiendo de cuál sea el principal

            // Usamos el operador de encadenamiento seguro (?.) para evitar null pointers.
            // Si 'respuesta' o 'monitors' o 'usd' es nulo, la expresión devolverá null.
            // El '?: 0.0' al final asegura que si algo es nulo, devolvamos 0.0.
            return respuesta?.monitors?.usd?.price ?: 0.0
        }


        fun getResponse(): ApiOficialTipoCambio? {
            return responseApiNew
        }

        fun getResponseEuroTipoCambio(): ApiOficialTipoCambio? {
            return responseApiEuroBcv
        }


        fun getResponseApiAlCambio(): ApiOficialTipoCambio? {
            return responseApiNew
        }


        fun setResponse(newResponse: ApiOficialTipoCambio) {
            responseApiNew = newResponse
        }

        fun setResponseOriginal(newResponse: ApiOficialTipoCambio) {
            responseApiOriginal = newResponse
        }

        fun setResponseApiAlCambio(newResponse: ApiOficialTipoCambio) {
            responseApiNew = newResponse
        }

        fun setResponseApiEurosTipoCambio(ResponseEuroBcv: ApiOficialTipoCambio) {
            responseApiEuroBcv = ResponseEuroBcv
        }

        fun setResponseHistory(newResponse: ApiOficialTipoCambio) {
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


        fun recuperarEuro(context: Context): Float {
            val prefs: SharedPreferences =
                context.getSharedPreferences(VALOR_EURO, Context.MODE_PRIVATE)
            return prefs.getFloat(
                NUMERO_EURO,
                0.0f
            ) // 0 es el valor predeterminado si no se encuentra el número
        }


        fun recuperarEuroFecha(context: Context): String? {
            val prefs: SharedPreferences =
                context.getSharedPreferences(VALOR_EURO, Context.MODE_PRIVATE)
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

            binding.btnEuroP.isChecked = false
            binding.btnBcv.isChecked = false
            binding.seekBar.progress = 1
            deshabilitarSeekBar()

        } else {
            binding.btnPromedio.isChecked = true
            binding.btnEuroP.isChecked = false
            binding.btnBcv.isChecked = false
            binding.seekBar.progress = 1
            deshabilitarSeekBar()

        }
    }

    private fun activarBtnBcv() {
        if (binding.btnBcv.isChecked == true) {

            binding.btnEuroP.isChecked = false
            binding.switchDolar.isChecked = false
            binding.btnPromedio.isChecked = false

            binding.edtxtDolares.hint = "Dolares"
            binding.edtxtDolares.setStartIconDrawable(R.drawable.ic_dolar)
            binding.seekBar.progress = 0
            binding.switchDolar.isChecked= false
            deshabilitarSeekBar()

        } else {
            binding.btnBcv.isChecked = true
            binding.btnPromedio.isChecked = false
            binding.btnEuroP.isChecked = false
            binding.seekBar.progress = 0
            deshabilitarSeekBar()

        }
    }

    private fun activarBtnEuro() {
        if (binding.btnEuroP.isChecked == true) {
            binding.btnBcv.isChecked = false
            binding.btnPromedio.isChecked = false
            binding.seekBar.progress = 2
            binding.switchDolar.isChecked= true
            binding.edtxtDolares.hint = "Euros"
            binding.seekBar.progress = 2
            binding.edtxtDolares.setStartIconDrawable(R.drawable.euro_img)
            deshabilitarSeekBar()
        } else {
            binding.btnEuroP.isChecked = true
            binding.btnBcv.isChecked = false
            binding.btnPromedio.isChecked = false



            deshabilitarSeekBar()
        }
    }

    private fun deshabilitarSeekBar() {
        binding.seekBar.setOnTouchListener { _, _ -> true } // Bloquea la interacción
    }



    private fun visibilidadSwicheDiaManana(resposeApiTipoCambio: ApiOficialTipoCambio ) {
        Log.d(TAG, "visibilidadSwicheDiaManana:resposeApiTipoCambio: $resposeApiTipoCambio ")
        if (!diaActual) {
            if (visibleLayoutProxBcv < 2) {
                visibleLayoutProxBcv += 1
                val slideIn = AnimationUtils.loadAnimation(
                    requireContext(),
                    R.anim.slide_in
                )
                val layoutUltActBcv = binding.layoutUltActBcv
                layoutUltActBcv.startAnimation(slideIn)
                binding.layoutUltActBcv.visibility = View.VISIBLE
                ApiResponseHolder.setResponseOriginal(resposeApiTipoCambio)

                val fechaSinHora =
                    extraerFecha(resposeApiTipoCambio.monitors.eur.last_update)
                binding.txtUltActBcv.text = fechaSinHora

            }
        } else {
            binding.layoutUltActBcv.visibility = View.GONE
            binding.SwUtimaAct.isChecked = false
        }

    }


//*************************************
//LLAMADO APISS*************************************************************APIS**********************
//*************************************

    fun llamarApiDolar(callback: (Boolean) -> Unit) {


        val savedResponseDolar = getResponseApiDolar(requireContext())
        Log.d(TAG, "llamarApiTipoCambio: ENTRO AL LLAMADO DEL API")
        try {
            if (savedResponseDolar != null) {

                ApiResponseHolder.setResponse(savedResponseDolar)
                diaActual = verificafechaActBcv(savedResponseDolar)
                valorActualEuro = savedResponseDolar.monitors.eur.price

                llenarDolarEuro(savedResponseDolar, diaActual)

                llenarCampoBCVNew(savedResponseDolar, diaActual)

                multiplicaDolares()
                dividirABolivares()
                binding.swipeRefreshLayout.isRefreshing = false
            }
        } catch (e: Exception) {
            Log.d(TAG, "llamarDolarApiNew: error $e")
            callback(false) // Operación fallida
        } finally {
            binding.swipeRefreshLayout.isRefreshing =
                false // Asegura que se detenga el refresco siempre
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            val baseUrl: String

            baseUrl = Constants.URL_BASE // URL base

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
                val apiResponseTipoCambio = apiService.tipocambio()

                if (apiResponseTipoCambio != null) {
                    withContext(Dispatchers.Main) {

                        ApiResponseHolder.setResponseApiEurosTipoCambio(apiResponseTipoCambio)
                        diaActual = verificafechaActBcv(apiResponseTipoCambio)

                        visibilidadSwicheDiaManana(apiResponseTipoCambio)

                        valorActualParalelo = apiResponseTipoCambio.monitors.eur.price
                        valorActualEuro = apiResponseTipoCambio.monitors.eur.price
                        guardarResponse(requireContext(), apiResponseTipoCambio)
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

                        //Desaparece el boton de Conectado
                        if (binding.imgSinConext.visibility == View.VISIBLE){
                            binding.imgSinConext.visibility= View.GONE
                            vibrateOnSuccess(requireContext())
                            Toast.makeText(
                                requireContext(),
                                "Reconexion!!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }


                        llenarCampoBCVNew(apiResponseTipoCambio, diaActual)
                        llenarDolarEuro(apiResponseTipoCambio, diaActual)

                        multiplicaDolares()
                        dividirABolivares()
                        callback(true) // Operación exitosa
                    }
                }
            } catch (e: Exception) {
                // --- ¡AQUÍ ESTÁ LA NUEVA LÓGICA DE MANEJO DE ERRORES! ---
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Excepción durante la llamada a la API: ${e.message}")

                    // 1. Verificamos si el dispositivo tiene conexión a internet.
                    if (isInternetAvailable(requireContext())) {
                        //SI HAY INTERNET: El problema es del servidor.
                        showServerErrorDialog()
                    } else {
                        // SI NO HAY INTERNET: El problema es del usuario.
                        //    Mostramos el mensaje de conexión y la vibración de error.
                        binding.txtFechaActualizacionPara.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                        binding.txtFechaActualizacionBcv.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                        Toast.makeText(requireContext(), "Problemas de Conexión. Revisa tu internet.", Toast.LENGTH_SHORT).show()
                        vibrateOnError(requireContext())
                        binding.imgSinConext.visibility = View.VISIBLE
                    }

                    // En cualquier caso de error, indicamos que la operación falló.
                    callback(false)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.swipeRefreshLayout.isRefreshing =
                        false // Asegura que se detenga el refresco siempre
                    callback(false) // Operación completada, aunque con error
                }
            }
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
    private fun guardarResponse(context: Context, responseBCV: ApiOficialTipoCambio) {
        val gson = Gson()
        val responseJson = gson.toJson(responseBCV)

        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("dolarBCVResponse", responseJson)
        editor.apply()
    }

    // Define una función para recuperar la respuesta de SharedPreferences
    private fun getResponseApiDolar(context: Context): ApiOficialTipoCambio? {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val responseJson = sharedPreferences.getString("dolarBCVResponse", null)

        if (responseJson != null) {
            val gson = Gson()

            return gson.fromJson(responseJson, ApiOficialTipoCambio::class.java)
        }

        return null // Retorna null si no se encontró la respuesta en SharedPreferences
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
    //***************************getResponseFromSharedPreferences******************************************************************




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
    private fun getBancosBCV(context: Context): ApiModelResponseBCV? {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val responseJson = sharedPreferences.getString("dolarResponseBCV", null)

        if (responseJson != null) {
            val gson = Gson()

            return gson.fromJson(responseJson, ApiModelResponseBCV::class.java)
        }

        return null // Retorna null si no se encontró la respuesta en SharedPreferences
    }



    // verifica Si la actualizacion del dolar es diferente a la fecha actual
    fun verificafechaActBcv(response: ApiOficialTipoCambio): Boolean {
        try {
            // Cambia el locale a Locale.US para que acepte "AM/PM"
            val dateFormat = SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.US)

            // Obtener la fecha actual
            val currentDate = Calendar.getInstance().time

            // Obtener los datos del monitor BCV
            val bcvMonitor = response.monitors.eur.last_update

            if (!bcvMonitor.isNullOrEmpty()) {
                // Parsear la fecha del last_update
                val lastUpdateDate = dateFormat.parse(bcvMonitor)

                if (lastUpdateDate != null && !lastUpdateDate.after(currentDate)) {

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

    fun llenarCampoBCVNew(response: ApiOficialTipoCambio,diaActual: Boolean) {
        // DATOS DEL BCV

        if (diaActual || binding.SwUtimaAct.isChecked) {
            if (!response.monitors.usd.price.toString().isNullOrEmpty()) {
                binding.btnBcv.text = response.monitors.usd?.price.toString()
                binding.btnBcv.textOff = response.monitors.usd?.price.toString()
                binding.btnBcv.textOn = response.monitors.usd?.price.toString()
                val fechaSinHora = extraerFecha(response.monitors.usd.last_update)
                binding.txtFechaActualizacionBcv.text = fechaSinHora

                if (response.monitors.usd.color == "red") binding.imgflechaBcv.setImageResource(R.drawable.ic_flecha_roja)
                if (response.monitors.usd.color == "neutral") binding.imgflechaBcv.setImageResource(R.drawable.ic_flecha_igual)
                if (response.monitors.usd.color == "green") binding.imgflechaBcv.setImageResource(R.drawable.ic_flechaverde)

                binding.txtVariacionBcv.text = response.monitors.usd.percent.toString()


            }
        }else{
            if (!response.monitors.usd.price.toString().isNullOrEmpty()) {
                binding.btnBcv.text = response.monitors.usd?.price_old.toString()
                binding.btnBcv.textOff = response.monitors.usd?.price_old.toString()
                binding.btnBcv.textOn = response.monitors.usd?.price_old.toString()
                val fechaSinHora = extraerFecha(response.monitors.usd.last_update_old)
                binding.txtFechaActualizacionBcv.text = fechaSinHora
                if (response.monitors.usd.color == "red") binding.imgflechaBcv.setImageResource(R.drawable.ic_flecha_roja)
                if (response.monitors.usd.color == "neutral") binding.imgflechaBcv.setImageResource(R.drawable.ic_flecha_igual)
                if (response.monitors.usd.color == "green") binding.imgflechaBcv.setImageResource(R.drawable.ic_flechaverde)

                binding.txtVariacionBcv.text = response.monitors.usd.percent_old.toString()

            }

        }


    }


    fun llenarDolarEuro(response: ApiOficialTipoCambio,diaActual: Boolean) {

        if (diaActual || binding.SwUtimaAct.isChecked) {

            binding.btnEuroP.text = response.monitors.eur.price.toString()
            binding.btnEuroP.textOff = response.monitors.eur.price.toString()
            binding.btnEuroP.textOn = response.monitors.eur.price.toString()
            binding.txtFechaActualizacionPara.text = response.monitors.eur.last_update

            if (response.monitors.eur.color == "red") binding.imgFlechaParalelo.setImageResource(
                R.drawable.ic_flecha_roja
            )
            if (response.monitors.eur.color == "neutral") binding.imgFlechaParalelo.setImageResource(
                R.drawable.ic_flecha_igual
            )
            if (response.monitors.eur.color == "green") binding.imgFlechaParalelo.setImageResource(
                R.drawable.ic_flechaverde
            )

            binding.txtVariacionParalelo.text = response.monitors.eur.percent.toString()
        }else{

            binding.btnEuroP.text = response.monitors.eur.price_old.toString()
            binding.btnEuroP.textOff = response.monitors.eur.price_old.toString()
            binding.btnEuroP.textOn = response.monitors.eur.price_old.toString()
            binding.txtFechaActualizacionPara.text = response.monitors.eur.last_update_old
            //binding.txtFechaActualizacionBcv.text= response.monitors.eur.last_update_old

            if (response.monitors.eur.color == "red") binding.imgFlechaParalelo.setImageResource(
                R.drawable.ic_flecha_roja
            )
            if (response.monitors.eur.color == "neutral") binding.imgFlechaParalelo.setImageResource(
                R.drawable.ic_flecha_igual
            )
            if (response.monitors.eur.color == "green") binding.imgFlechaParalelo.setImageResource(
                R.drawable.ic_flechaverde
            )
            binding.txtVariacionParalelo.text = response.monitors.eur.percent_old.toString()
        }


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
                    val dolarParalelo = binding.btnEuroP.text.toString().toDoubleOrNull()
                        ?: 0.0 //precio del paralelo
                    val dolarBcv = binding.btnBcv.text.toString().toDoubleOrNull() ?: 0.0 //precio del paralelo
                    val dolarPromedio = binding.btnPromedio.text.toString().toDoubleOrNull() ?: 0.0 //precio del paralelo
                    val entradaDolares = binding.inputDolares.text.toString()
                    if (entradaDolares.isNotEmpty()) {
                        if (binding.btnEuroP.isChecked) {
                          
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
                    val dolarParalelo = binding.btnEuroP.text.toString().toDoubleOrNull() ?: 0.0
                    val dolarBcv = binding.btnBcv.text.toString().toDoubleOrNull() ?: 0.0
                    val dolarPromedio = binding.btnPromedio.text.toString().toDoubleOrNull() ?: 0.0

                    if (inputText.isNotEmpty()) {
                        val cleanedText =
                            inputText.replace("[,]".toRegex(), "").toDoubleOrNull() ?: 0.0

                        if (binding.btnEuroP.isChecked) {
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
    private fun updatePremiumIconVisibility() {
        // Usamos nuestra función centralizada en AppPreferences.
        if (AppPreferences.isUserPremiumActive()) {
            // Si el usuario es premium activo, hacemos visible el ícono.
            binding.imgpremium.visibility = View.VISIBLE
        } else {
            // Si no lo es, lo ocultamos.
            binding.imgpremium.visibility = View.GONE
        }
    }

    private fun showPremiumExpirationDate() {


        // Obtenemos el nombre del plan y la fecha de vencimiento.
        val planName = AppPreferences.getPremiumPlan()
        val expirationDateMillis = AppPreferences.getPremiumExpirationDate()

        val message: String

        if (expirationDateMillis == -1L) {
            // Caso especial para el plan vitalicio.
            message = "Tienes una suscripción '$planName' que no vence."
        } else if (expirationDateMillis > 0L) {
            // Formateamos la fecha para que sea legible.
            val dateFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
            val formattedDate = dateFormat.format(Date(expirationDateMillis))
            message = "Tu plan '$planName' vence el: $formattedDate"
        } else {
            // Caso de respaldo por si algo sale mal.
            message = "No se pudo obtener la información de tu suscripción."
        }

        // Mostramos el mensaje en un Toast de larga duración.
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }


    override fun onResume() {
        super.onResume()

        comenzarCarga()
        updatePremiumIconVisibility()
   
        // La lógica principal ahora está aquí y es la única que decide si llamar a la red.
        if (AppPreferences.shouldRefreshApi()) {
            Log.d(TAG, "Tiempo de cache expirado o primera carga. Refrescando datos de la API...")
            refreshAllApis()
        } else {
            Log.d(TAG, "Usando datos cacheados. No se llamará a ninguna API.")
            // Aseguramos que los datos cacheados se muestren al volver a la app.
            loadDataFromCache()
        }

    }

    /**
     * Función UNIFICADA para agrupar TODAS las llamadas a la API.
     */
    private fun refreshAllApis() {
        // Mostramos el indicador de carga.
        comenzarCarga()
        Log.d(TAG, "refreshAllApis: llamando api")
        // 1. Llamada a la API principal.
        llamarApiDolar { isSuccessful ->
            if (isSuccessful) {
                // Actualizamos la marca de tiempo SOLO si la llamada principal fue exitosa.
                AppPreferences.updateLastRefreshTimestamp()
            }
            // Finalizamos la carga independientemente del resultado.
            finalizarCarga()
        }

       // actualizarEuro() // La función de scraping también es una llamada de red.
    }

    /**
     * Carga y muestra los datos guardados en SharedPreferences sin llamar a la red.
     */
    private fun loadDataFromCache() {
        // Cargamos los datos de la API principal desde el cache.
        val savedResponseDolar = getResponseApiDolar(requireContext())
        if (savedResponseDolar != null) {
            ApiResponseHolder.setResponse(savedResponseDolar)
            llenarDolarEuro(savedResponseDolar, diaActual) // Asume que 'diaActual' se guarda o recalcula
            llenarCampoBCVNew(savedResponseDolar, diaActual)
            visibilidadSwicheDiaManana(savedResponseDolar)
            finalizarCarga()
        }

        // Cargamos los datos de las otras APIs desde su respectivo cache.
        val savedResponseCripto = getResponseSharedPreferencesCriptodolar(requireContext())
        if (savedResponseCripto != null) {
            ApiResponseHolder.setResponseCripto(savedResponseCripto)
            // Aquí iría la función que usa los datos de CriptoDolar, si la tienes.
        }

        val savedResponseBCV = getBancosBCV(requireContext())
        if (savedResponseBCV != null) {
            ApiResponseHolder.setResponseBCV(savedResponseBCV)
            // Aquí iría la función que usa los datos de la página del BCV.
        }

        // No podemos "recargar" el scraping de `actualizarEuro` desde el cache,
        // pero la UI mostrará los últimos datos que esa función guardó en sus SharedPreferences.
    }






    override fun onPause() {
        //eliminarListener()
        super.onPause()
       // shakeDetector.stop()
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
        var dolarEuro = binding.btnEuroP.text?.toString()?.toDoubleOrNull() ?: 1.0
        var dolarBcv = binding.btnBcv.text?.toString()?.toDoubleOrNull() ?: 1.0
        var cantidadDolares = binding.inputDolares.text?.toString()?.toDoubleOrNull() ?: 1.0
        var cantidadBs = binding.inputBolivares.text?.toString()?.toDoubleOrNull() ?: 1.0

        val totalDolaresBcv = dolarBcv * cantidadDolares
        val totalBsbcv = dolarBcv * cantidadDolares
        val totalDolaresParalelo = dolarEuro * cantidadDolares
        val totalBsParalelo = dolarEuro * cantidadBs




            mensaje = getString(R.string.mensaje_dolar)
            diferenciaBs = totalDolaresParalelo - totalDolaresBcv
            diferenciaDolares = (totalDolaresParalelo - totalDolaresBcv) / dolarBcv
            val diferencia = Math.abs(dolarEuro - dolarBcv)
            diferenciaPorcentual= (diferencia / dolarBcv) * 100


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

    private fun showServerErrorDialog() {
        // Aseguramos que el diálogo se muestre en el hilo principal de la UI.
        activity?.runOnUiThread {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Problema con el Servidor")
                .setMessage("Estamos experimentando inconvenientes con nuestros servidores en este momento. Ya estamos trabajando para solucionarlo. Por favor, intenta de nuevo más tarde.")
                .setPositiveButton("Entendido") { dialog, _ ->
                    dialog.dismiss()
                }
                .setIcon(R.drawable.ic_cloud_off_24) // ¡Opcional, pero recomendado!
                .show()
        }
    }


}