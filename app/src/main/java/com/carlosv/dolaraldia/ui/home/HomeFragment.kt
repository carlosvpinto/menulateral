package com.carlosv.dolaraldia.ui.home


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
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
import android.graphics.Color
import android.graphics.Rect

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
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
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentHomeBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.DecimalFormat
import java.util.Date
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

import com.carlosv.dolaraldia.AppPreferences
import com.carlosv.dolaraldia.model.apiAlcambioEuro.ApiOficialTipoCambio
import com.carlosv.dolaraldia.model.apicontoken2.ApiModelResponseCripto
import com.carlosv.dolaraldia.model.apicontoken2.ApiModelResponseBCV
import com.carlosv.dolaraldia.model.clickAnuncios.ClickAnunicosModel
import com.carlosv.dolaraldia.model.history.HistoryModelResponse
import com.carlosv.dolaraldia.provider.ClickAnuncioProvider
import com.carlosv.dolaraldia.utils.Constants
import com.carlosv.dolaraldia.utils.VibrationHelper.vibrateOnError
import com.carlosv.dolaraldia.utils.VibrationHelper.vibrateOnSuccess
import com.carlosv.dolaraldia.utils.ads.RewardedAdManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import android.graphics.Typeface
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.NotificationCompat.getColor
import com.carlosv.dolaraldia.utils.VibrationHelper

import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.switchmaterial.SwitchMaterial



class HomeFragment : Fragment(), RewardedAdManager.AdLoadListener {

    private var _binding: FragmentHomeBinding? = null


    private val binding get() = _binding ?: throw IllegalStateException("Binding is null")
    private var isFragmentAttached: Boolean = false

    private var snackbar: Snackbar? = null

    var url: String? = null
    var pagina: String? = null

    private val clickAnuncioProvider = ClickAnuncioProvider()

    private var valorActualParalelo: Double? = 0.0
    private var valorActualEuro: Double? = 0.0
    private var valorActualUsdt: Double? = 0.0
    private var ultimoTecleado: Int? = 0
    var numeroNoturno = 0
    lateinit var mAdView: AdView

    private lateinit var layout: LinearLayout

    private var repeatCount = 0

    // Para Admob Reguard

    private var TAG = "HomeFragment"

    lateinit var navigation: BottomNavigationView

    private var visibleLayoutProxBcv = 0

    private var diaActual: Boolean= false


    // 1. Definimos cuántos toques son necesarios.
    private val SECRET_TAP_COUNT = 7

    // 2. Un contador para llevar la cuenta de los toques.
    private var tapCounter = 0

    // 3. Un 'Handler' para resetear el contador si el usuario tarda mucho entre toques.
    private val resetHandler = Handler(Looper.getMainLooper())
    private val resetRunnable = Runnable {
        tapCounter = 0
        Log.d("EasterEgg", "Contador de toques reseteado por tiempo.")
    }


    // ¡NUEVO! Creamos una instancia del gestor de anuncios bonificados.
    private lateinit var rewardedAdManager: RewardedAdManager

    private var isRewardedAdReady = false

    private var colorOriginalFecha: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root // ¡SOLO ESTO!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. INICIALIZAR MANAGER (Lo primero)
        rewardedAdManager = RewardedAdManager(requireContext())

        // 2. CAPTURAR EL COLOR ORIGINAL (Soluciona que la fecha se vea invisible)
        colorOriginalFecha = binding.txtFechaActualizacionBcv.currentTextColor

        // 3. CONFIGURAR LISTENERS (Clics)
        setupClickListeners()

        // 4. PREPARAR UI
        updatePremiumIconVisibility()
        visibleLayoutProxBcv += 1
        configurarBannerWhatsApp()

        // Reset del Easter Egg
        resetHandler.removeCallbacks(resetRunnable)
        binding.imglogo.setOnClickListener {
            manejarEasterEgg() // (He movido tu lógica de toques a una funcioncita para limpiar aqui)
        }

        // 5. SOLUCIÓN AL CONGELAMIENTO (Botón BCV)
        binding.btnBcv.post {
            if (_binding != null) {
                binding.btnBcv.isChecked = true
            }
        }

        // 6. CARGA DE ANUNCIOS (Banner)
        layout = binding.linearLayout3
        mAdView = binding.adView
        try {
            val adRequest = AdRequest.Builder().build()
            mAdView.loadAd(adRequest)
        } catch (e: Exception) {
            Log.e("AdMob", "Error Banner: ${e.localizedMessage}")
        }

        // 7. CARGA DE RECOMPENSADO
        binding.buttonRewardedAd.isExtended = false
        binding.buttonRewardedAd.isEnabled = false
        rewardedAdManager.loadAd(this)

        // 8. OTRAS PREFERENCIAS
        val sharedPreferences = requireContext().getSharedPreferences("MiPreferencia", AppCompatActivity.MODE_PRIVATE)
        numeroNoturno = sharedPreferences.getInt("numero_noturno", 0)

        // Animación de entrada del fragmento (Opcional, si no causa lag)
        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.appear_from_top)
        binding.root.startAnimation(animation)
    }

    override fun onAdLoaded() {
        _binding?.let { safeBinding ->
            activity?.runOnUiThread {
                // Configuración visual
                safeBinding.buttonRewardedAd.isEnabled = true
                safeBinding.buttonRewardedAd.extend() // O shrink, según prefieras iniciar
                isRewardedAdReady = true
                safeBinding.buttonRewardedAd.shrink() // Asegurar estado shrink

                // ANIMACIÓN XML SEGURA (Evita el error SurfaceFlinger)
                if (!AppPreferences.isUserPremiumActive()) {
                    safeBinding.buttonRewardedAd.post { // POST para seguridad extra
                        try {
                            if (context != null) {
                                // Cargar binance_shake.xml (El que creamos antes)
                                val shake = AnimationUtils.loadAnimation(
                                    requireContext(),
                                    R.anim.binance_shake
                                )
                                safeBinding.buttonRewardedAd.startAnimation(shake)

                                // Vibración
                                VibrationHelper.vibrateOnError(requireContext())
                            }
                        } catch (e: Exception) {
                            Log.e("Animacion", "Error leve: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    override fun onAdLoadFailed() {
        // El anuncio falló.

        // 1. Comprueba si el binding (y por tanto la vista) todavía existe.
        // El bloque 'let' solo se ejecutará si '_binding' no es nulo.
        _binding?.let { safeBinding ->

            // 2. Ejecuta el código de la UI en el hilo principal de forma segura.
            activity?.runOnUiThread {
                // 3. Usa 'safeBinding' en lugar de 'binding' para acceder a las vistas.
                safeBinding.buttonRewardedAd.isEnabled = false
                safeBinding.buttonRewardedAd.clearAnimation() // Detener de golpe si falla


                isRewardedAdReady = false // ¡Importante!
            }
        }
    }

    private fun manejarEasterEgg() {
        // 1. Incrementamos el contador
        tapCounter++
        Log.d("EasterEgg", "Toque número: $tapCounter")

        // 2. Limpiamos cualquier reseteo pendiente (para reiniciar el cronómetro de 1.5s)
        resetHandler.removeCallbacks(resetRunnable)

        // 3. Verificamos si llegó a la meta
        if (tapCounter == SECRET_TAP_COUNT) {
            Log.d("EasterEgg", "¡Secreto activado!")

            // Reiniciamos el contador inmediatamente
            tapCounter = 0

            try {
                // Navegamos al menú secreto
                findNavController().navigate(R.id.nav_debug_premium)

                // Opcional: Feedback visual
                Toast.makeText(requireContext(), "🛠️ Modo Developer", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Log.e("EasterEgg", "Error al navegar: ${e.message}")
            }

        } else {
            // 4. Si no ha llegado a 7, esperamos 1.5 segundos.
            // Si no toca de nuevo en ese tiempo, se resetea a 0.
            resetHandler.postDelayed(resetRunnable, 1500)
        }
    }

    private fun animarShakeBinance(view: View) {
        // Mover 10 pixeles a la izquierda y derecha
        val shake = android.view.animation.TranslateAnimation(0f, 10f, 0f, 0f)

        // DURACIÓN: 1000ms (1 segundo)
        shake.duration = 1000

        // El número '9f' significa que vibrará 9 veces en ese segundo.
        shake.interpolator = android.view.animation.CycleInterpolator(8f)

        // --- AQUÍ CONECTAMOS LA VIBRACIÓN ---
        // Usamos el contexto de la propia vista para llamar al helper
        try {
            VibrationHelper.vibrateOnError(view.context)
        } catch (e: Exception) {
            Log.e("Animacion", "Error al vibrar: ${e.message}")
        }

        view.startAnimation(shake)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupClickListeners() {

        binding.imgpremium.setOnClickListener {
           // showPremiumExpirationDate()
            findNavController().navigate(R.id.action_nav_home_to_premiumStatusFragment)
        }

        binding.nestedScrollView.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (binding.buttonRewardedAd.isExtended) {
                    val outRect = Rect()
                    binding.buttonRewardedAd.getGlobalVisibleRect(outRect)
                    if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        binding.buttonRewardedAd.shrink()
                    }
                }
            }
            if (event.action == MotionEvent.ACTION_UP) {
                view.performClick()
            }
            false
        }



        //Hacer clic al Botoen de recompensas de 4 horas
        binding.buttonRewardedAd.setOnClickListener {

            if (binding.buttonRewardedAd.isExtended) {
                verPrimerDialogoRecompensa()
            } else {
                // SI ESTÁ CONTRAÍDO -> Solo lo expande
                (binding.buttonRewardedAd.extend())
            }

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
                val textoDelBotonActivo = tomarValorBotonActivo(
                    binding.btnBcv,
                    binding.btnUsdt,
                    binding.btnEuroP
                )
                actualzarMultiplicacion(textoDelBotonActivo ?: 0.0)

            } else {

                val savedResponseDolar = getResponseApiDolar(requireContext())
                cambioSwictValor(savedResponseDolar, false)
                val textoDelBotonActivo = tomarValorBotonActivo(
                    binding.btnBcv,
                    binding.btnUsdt,
                    binding.btnEuroP
                )
                actualzarMultiplicacion(textoDelBotonActivo ?: 0.0)
            }
        }


        binding.btnBcv.setOnClickListener {
            activarBtnBcv()
            binding.buttonRewardedAd.shrink()
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
            binding.buttonRewardedAd.shrink()
            val valorDolar = binding.btnEuroP.text.toString()

            val doubleValue = try {
                valorDolar.toDouble()
            } catch (e: NumberFormatException) {
                null // Si la conversión falla, asigna null
            }

            // Actualiza la multiplicación con el valor convertido o 0.0 si la conversión falla
            actualzarMultiplicacion(doubleValue ?: 0.0)

        }

        binding.btnUsdt.setOnClickListener {
            activarBtnUsdt()
            binding.buttonRewardedAd.shrink()
            val valorDolar = binding.btnUsdt.text.toString()

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
//        binding.switchDolar.setOnCheckedChangeListener { _, isChecked ->
//            if (isChecked) {
//                // Si el switch está activado, simula un clic en el botón de Euro
//                binding.btnEuroP.performClick()
//            } else {
//                // Si el switch está desactivado, simula un clic en el botón de BCV
//                binding.btnBcv.performClick()
//            }
//        }


    }

    // ¡NUEVO! Una función dedicada para mostrar u ocultar el botón de recompensa.
    private fun updateRewardButtonVisibility() {
        if (AppPreferences.isUserPremiumActive()) {
            binding.buttonRewardedAd.visibility = View.GONE // Oculta el botón
        } else {
            binding.buttonRewardedAd.visibility = View.VISIBLE // Muestra el botón
        }
    }


    /**
     * Muestra un diálogo informativo antes de que el usuario vea el anuncio.
     */
    private fun verPrimerDialogoRecompensa() {
        // 1. Inflamos nuestro nuevo layout personalizado
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_initial_reward, null)

        // 2. Creamos el constructor del diálogo
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView) // Usamos nuestro layout
            .setCancelable(false) // El usuario debe tomar una decisión

        // 3. Creamos y mostramos el diálogo
        val dialog = builder.create()

        // --- Personalización del contenido ---
        val messageTextView = dialogView.findViewById<TextView>(R.id.dialog_message)
        val messageText = "Mira un video corto y disfruta la app sin publicidad durante 4 horas."

        // Hacemos que "sin publicidad" y "4 horas" resalten en negrita
        val spannable = SpannableStringBuilder(messageText)
        spannable.setSpan(StyleSpan(Typeface.BOLD), messageText.indexOf("sin publicidad"), messageText.indexOf("sin publicidad") + "sin publicidad".length, 0)
        spannable.setSpan(StyleSpan(Typeface.BOLD), messageText.indexOf("4 horas"), messageText.indexOf("4 horas") + "4 horas".length, 0)
        messageTextView.text = spannable

        // --- Lógica de los botones ---
        val btnNegative = dialogView.findViewById<Button>(R.id.button_negative)
        val btnPositive = dialogView.findViewById<Button>(R.id.button_positive)

        btnNegative.setOnClickListener {
            dialog.dismiss()
        }

        btnPositive.setOnClickListener {
            if (isRewardedAdReady) {
                MuestraPublicidadRecompensa()
            } else {
                Toast.makeText(requireContext(), "El anuncio aún no está listo, espera un momento.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss() // Cerramos el diálogo después de la acción
        }

        dialog.show()
    }

    private fun verSegundoDialogoRecompensa() {
        // 1. Inflamos nuestro layout personalizado
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_reward_upgrade, null)

        // 2. Creamos el constructor del diálogo
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView) // ¡La clave! Usamos nuestro layout
            .setCancelable(false)

        // 3. Creamos y mostramos el diálogo
        val dialog = builder.create()

        // --- Personalización del contenido ---
        val messageTextView = dialogView.findViewById<TextView>(R.id.dialog_message)
        val messageText = "Ya tienes 4 horas. ¡Mira un video más y suma otras 12 HORAS para un total de 16!"

        // Hacemos que "12 HORAS" y "16" sean negritas para que resalten
        val spannable = SpannableStringBuilder(messageText)
        spannable.setSpan(StyleSpan(Typeface.BOLD), messageText.indexOf("12 HORAS"), messageText.indexOf("12 HORAS") + "12 HORAS".length, 0)
        spannable.setSpan(StyleSpan(Typeface.BOLD), messageText.indexOf("16"), messageText.indexOf("16") + "16".length, 0)
        messageTextView.text = spannable

        // --- Lógica de los botones ---
        val btnNegative = dialogView.findViewById<Button>(R.id.button_negative)
        val btnPositive = dialogView.findViewById<Button>(R.id.button_positive)

        btnNegative.setOnClickListener {
            dialog.dismiss()
        }

        btnPositive.setOnClickListener {
            // El usuario aceptó la oferta.
            binding.buttonRewardedAd.isEnabled = false // Deshabilitamos temporalmente
            rewardedAdManager.loadAd(object : RewardedAdManager.AdLoadListener {
                override fun onAdLoaded() {
                    isRewardedAdReady = true
                    MuestraPublicidadRecompensa()
                }
                // VERSIÓN CORREGIDA Y SEGURA
                override fun onAdLoadFailed() {
                    // 1. Usa 'context' en lugar de 'requireContext()'.
                    //    'context' es nulable y no crasheará si el fragmento no está adjunto.
                    val safeContext = context ?: return // Si el contexto es nulo, simplemente sal de la función.

                    // 2. Ahora puedes usar 'safeContext' de forma segura.
                    Toast.makeText(safeContext, "No se pudo cargar el segundo anuncio.", Toast.LENGTH_SHORT).show()
                }
            })
            dialog.dismiss()
        }

        dialog.show()
    }


    // La función showRewardedAd ahora es mucho más simple
    private fun MuestraPublicidadRecompensa() {
        if (!isRewardedAdReady) return
        isRewardedAdReady = false
        // El botón principal se oculta aquí como parte de la lógica de visibilidad general
        updateRewardButtonVisibility()

        rewardedAdManager.showAd(requireActivity(), object : RewardedAdManager.RewardListener {
            override fun onRewardEarned() {
                // La recompensa se da, pero el diálogo y la recarga ocurren al cerrar.
            }

            override fun onAdDismissed() {
                // ¡LÓGICA PRINCIPAL! Esto se ejecuta cuando el usuario cierra el anuncio.
                manejoAnuncioCerrado()
            }

            override fun onAdFailedToLoad() {
                Toast.makeText(requireContext(), "El anuncio no se pudo mostrar. Intenta más tarde.", Toast.LENGTH_SHORT).show()
                // Si falla al mostrarse, intentamos cargar otro.
                rewardedAdManager.loadAd(this@HomeFragment)
            }

            override fun onAdNotReady() {
                // Este caso es ahora casi imposible, pero por si acaso, intentamos recargar.
                Toast.makeText(requireContext(), "El anuncio no está listo todavía.", Toast.LENGTH_SHORT).show()
                rewardedAdManager.loadAd(this@HomeFragment)
            }
        })
    }

    // ¡NUEVO! Función que maneja la lógica DESPUÉS de ver un anuncio.
    private fun manejoAnuncioCerrado() {
        // Averiguamos cuántos videos ha visto el usuario.
        val videosWatched = AppPreferences.getRewardVideosWatched()
        Log.d("PremiumStatus", "handleAdDismissed:videosWatched $videosWatched ")
        if (videosWatched == 0) {
            // Es la primera vez que ve un video en este ciclo. Le damos 4 horas.
            AppPreferences.setUserAsPremium("Recompensa", 4)
            verSegundoDialogoRecompensa() // Le ofrecemos la oportunidad de ver otro por 12 horas más.
        } else {
            // Ya había visto uno, así que este es el segundo. Le damos 12 horas adicionales.
            AppPreferences.setUserAsPremium("Recompensa", 12)
            AppPreferences.resetRewardVideosWatched()
            verDialogoFinal() // Le informamos que la recompensa total fue aplicada.
        }

        // En ambos casos, actualizamos la UI para ocultar el botón.
        updateRewardButtonVisibility()
    }


    // ¡NUEVO! Diálogo que aparece después del SEGUNDO video.
    private fun verDialogoFinal() {

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("¡Recompensa Máxima!")
            .setMessage("¡Excelente! Ahora tienes un total de 16 horas sin publicidad. ¡Disfrútalo!")
            .setPositiveButton("Entendido") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }


private fun tomarValorBotonActivo(vararg buttons: ToggleButton): Double? {
    // 1. Encuentra el primer botón activo de forma segura (devuelve null si no hay ninguno).
    // 2. Accede a su texto de forma segura.
    // 3. Convierte el texto a Double de forma segura (devuelve null si no es un número).
    return buttons.firstOrNull { it.isChecked }?.text?.toString()?.toDoubleOrNull()
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
            llenarCampoBCVyUSDT(responseMostrar,diaActualTem)
            llenarDolarEuro(responseMostrar,diaActualTem)
            //llenarCampoPromedio(responseMostrar)
            // Actualiza la multiplicación con el valor


            animacionFlipCondicional(
                binding.txtFechaActualizacionBcv, // El TextView a colorear
                binding.SwUtimaAct,               // El switch que manda
                colorOriginalFecha,               // El color a restaurar si el switch está off
                binding.btnBcv,                   // Otras vistas que solo giran
                binding.btnEuroP                  // ...
            )
        }

    }

    private fun guardarClickAnuncio() {
        try {
            val nombreAnuncio = "Compartor App Ios"


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

        private var responseApiNew: ApiOficialTipoCambio? = null
        private var responseApiEuroBcv: ApiOficialTipoCambio? = null

        private var responseApiOriginal: ApiOficialTipoCambio? = null
        private var responseApiNew2: ApiModelResponseCripto? = null
        private var responseApiBancoNew2: ApiModelResponseBCV? = null
        private var responseHistoryBcv: HistoryModelResponse? = null



        private const val VALOR_EURO = "ValorEuro"
        private const val NUMERO_EURO = "euro"
        private const val FECHA_EURO = "fecha"

        fun getTasaVentaBcv(): Double {
            // Usamos la misma fuente de datos que usa tu UI (llamarApiTipoCambio)
            val respuesta = getResponseEuroTipoCambio() // O getResponse(), dependiendo de cuál sea el principal

            // Usamos el operador de encadenamiento seguro (?.) para evitar null pointers.
            // Si 'respuesta' o 'monitors' o 'usd' es nulo, la expresión devolverá null.
            // El '?: 0.0' al final asegura que si algo es nulo, devolvamos 0.0.
            return respuesta?.monitors?.usd?.price ?: 0.0
        }


        fun getResponseEuroTipoCambio(): ApiOficialTipoCambio? {
            return responseApiEuroBcv
        }


        fun setResponse(newResponse: ApiOficialTipoCambio) {
            responseApiNew = newResponse
        }

        fun setResponseOriginal(newResponse: ApiOficialTipoCambio) {
            responseApiOriginal = newResponse
        }


        fun setResponseApiEurosTipoCambio(ResponseEuroBcv: ApiOficialTipoCambio) {
            responseApiEuroBcv = ResponseEuroBcv
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

        fun getResponseHistoryBcv(): HistoryModelResponse? {
            return responseHistoryBcv
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



    private fun activarBtnBcv() {
        if (binding.btnBcv.isChecked == true) {

            binding.btnEuroP.isChecked = false
            binding.btnUsdt.isChecked = false
            //binding.switchDolar.isChecked = false


            binding.edtxtDolares.hint = "Dolares"
            binding.edtxtDolares.setStartIconDrawable(R.drawable.ic_dolar)
            binding.seekBar.progress = 0
           // binding.switchDolar.isChecked= false
            deshabilitarSeekBar()

        } else {
            binding.btnBcv.isChecked = true
            binding.btnUsdt.isChecked = false
            binding.btnEuroP.isChecked = false
            binding.seekBar.progress = 0
            deshabilitarSeekBar()

        }
    }

    private fun activarBtnEuro() {
        if (binding.btnEuroP.isChecked == true) {
            binding.btnBcv.isChecked = false
            binding.btnUsdt.isChecked = false
            binding.seekBar.progress = 2
          //  binding.switchDolar.isChecked= true
            binding.edtxtDolares.hint = "Euros"
            binding.seekBar.progress = 2
            binding.edtxtDolares.setStartIconDrawable(R.drawable.euro_img)
            deshabilitarSeekBar()
        } else {
            binding.btnEuroP.isChecked = true
            binding.btnBcv.isChecked = false
            binding.btnUsdt.isChecked = false



            deshabilitarSeekBar()
        }
    }

    private fun activarBtnUsdt() {
        if (binding.btnUsdt.isChecked == true) {
            binding.btnBcv.isChecked = false
            binding.btnEuroP.isChecked= false
            binding.seekBar.progress = 1
           // binding.switchDolar.isChecked= true
            binding.edtxtDolares.hint = "Usdt"

            binding.edtxtDolares.setStartIconDrawable(R.drawable.usdt)
            deshabilitarSeekBar()
        } else {
            binding.btnUsdt.isChecked = true
            binding.btnBcv.isChecked = false
            binding.btnEuroP.isChecked = false



            deshabilitarSeekBar()
        }
    }

    private fun deshabilitarSeekBar() {
        binding.seekBar.setOnTouchListener { _, _ -> true } // Bloquea la interacción
    }



    private fun visibilidadSwicheDiaManana(resposeApiTipoCambio: ApiOficialTipoCambio ) {
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
        try {
            if (savedResponseDolar != null) {

                ApiResponseHolder.setResponse(savedResponseDolar)
                diaActual = verificafechaActBcv(savedResponseDolar)
                valorActualEuro = savedResponseDolar.monitors.eur.price

                llenarDolarEuro(savedResponseDolar, diaActual)

                llenarCampoBCVyUSDT(savedResponseDolar, diaActual)

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
                        valorActualUsdt = apiResponseTipoCambio.monitors.usdt.price
                        guardarResponse(requireContext(), apiResponseTipoCambio)

                        animacionPulsoCondicional(
                            binding.SwUtimaAct,
                            binding.txtFechaActualizacionBcv,
                            colorOriginalFecha,
                            binding.txtFechaActualizacionUsdt,

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


                        llenarCampoBCVyUSDT(apiResponseTipoCambio, diaActual)
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
        // Obtenemos el servicio de conectividad del sistema.
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Obtenemos la red activa actual. Si no hay ninguna, no hay conexión.
        val network = connectivityManager.activeNetwork ?: return false

        // Obtenemos las capacidades de esa red activa.
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        // La clave está aquí: comprobamos si la red tiene la capacidad de 'VALIDATED'.
        // Esto significa que el sistema ha verificado que esta conexión puede alcanzar Internet.
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun animacionPulsoCondicional(
        switchCondicional: SwitchCompat,
        texto1: TextView,
        colorOriginal1: Int,
        texto2: TextView
    ) {
        val colorNaranja = Color.parseColor("#FFA500")

        // --- LÓGICA PARA TEXTO 1 (Con cambio de color y animación) ---

        // 1. Determina y aplica el color final inmediatamente, SOLO a texto1.
        val colorFinalTexto1 = if (switchCondicional.isChecked) colorNaranja else colorOriginal1
        texto1.setTextColor(colorFinalTexto1)

        // 2. Ejecuta la animación de pulso en texto1.
        //    CAMBIO: Se llama a .animate() directamente sobre la vista.
        texto1.animate()
            .scaleX(1.5f)
            .scaleY(1.5f)
            .setDuration(300)
            .withEndAction {
                // Al terminar de crecer, vuelve al tamaño original
                texto1.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .start()
            }
            .start()

        // --- LÓGICA PARA TEXTO 2 (SOLO animación) ---

        // 3. Ejecuta la misma animación de pulso en texto2.
        //    CAMBIO: Se llama a .animate() directamente sobre la vista.
        texto2.animate()
            .scaleX(1.5f)
            .scaleY(1.5f)
            .setDuration(300)
            .withEndAction {
                // Al terminar de crecer, vuelve al tamaño original
                texto2.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .start()
            }
            .start()
    }



    private fun animacionFlipCondicional(
        textView: TextView,
        switchCondicional: SwitchCompat,
        colorOriginal: Int,
        vararg otrasVistas: View
    ) {
        // Une todas las vistas en una sola lista para aplicarles la animación.
        val todasLasVistas = listOf(textView) + otrasVistas

        // Define el color naranja para cuando el switch está activo.
        val colorNaranja = Color.parseColor("#FFA500")

        // Determina el color final basado en el estado ACTUAL del switch.
        val colorFinal = if (switchCondicional.isChecked) colorNaranja else colorOriginal

        // Aplica la animación a cada una de las vistas.
        todasLasVistas.forEach { view ->

            val animator = ObjectAnimator.ofFloat(view, View.ROTATION_Y, 0f, 90f)
            animator.duration = 200 // Duración de la primera mitad

            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {

                    // Si la vista actual es el TextView, establece su color final.
                    // Lo hacemos aquí, en el punto medio, para un efecto visual limpio.
                    if (view == textView) {
                        textView.setTextColor(colorFinal)
                    }

                    // Prepara la vista para la segunda mitad del giro.
                    view.rotationY = -90f

                    // Animador para la segunda mitad del giro (-90 a 0 grados).
                    val animatorVuelta = ObjectAnimator.ofFloat(view, View.ROTATION_Y, -90f, 0f)
                    animatorVuelta.duration = 200
                    animatorVuelta.start()
                }
            })

            // Inicia la primera parte de la animación.
            animator.start()
        }
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

    fun llenarCampoBCVyUSDT(response: ApiOficialTipoCambio,diaActual: Boolean) {
        val usdtData = response.monitors?.usdt

        // 2. COMPROBAR SI LOS DATOS EXISTEN
        if (usdtData != null) {
            // --- CASO DE ÉXITO: El objeto 'usdt' no es nulo, podemos usar sus datos. ---

            // Llenar los campos de precio
            binding.btnUsdt.text = usdtData.price?.toString()
            binding.btnUsdt.textOff = usdtData.price?.toString() ?: "0.0"
            binding.btnUsdt.textOn = usdtData.price?.toString() ?: "0.0"

            // Llenar la fecha de actualización
            binding.txtFechaActualizacionUsdt.text = usdtData.last_update ?: "N/A"

            // Actualizar la imagen de la flecha según el color
            when (usdtData.color) {
                "red" -> binding.imgflechaUsdt.setImageResource(R.drawable.ic_flecha_roja)
                "green" -> binding.imgflechaUsdt.setImageResource(R.drawable.ic_flechaverde)
                "neutral" -> binding.imgflechaUsdt.setImageResource(R.drawable.ic_flecha_igual)
                else -> {
                    // Opcional: poner una imagen por defecto si el color no es ninguno de los esperados
                }
            }

            // Llenar el texto de la variación
            binding.txtVariacionUsdt.text = usdtData.percent?.toString() ?: "0.0"

        } else {
            // --- CASO DE FALLO: El objeto 'usdt' es nulo. ---
            // Asignamos valores por defecto a toda la UI para no dejarla en blanco y evitar crashes.

            binding.btnUsdt.text = "0.0"
            binding.btnUsdt.textOff = "0.0"
            binding.btnUsdt.textOn = "0.0"
            binding.txtFechaActualizacionUsdt.text = "No disposable"
            binding.imgflechaUsdt.setImageResource(R.drawable.ic_flecha_igual) // O la imagen que prefieras
            binding.txtVariacionUsdt.text = "0.0"
        }



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

            if (response.monitors.eur.color == "red") binding.imgflechaEuro.setImageResource(
                R.drawable.ic_flecha_roja
            )
            if (response.monitors.eur.color == "neutral") binding.imgflechaEuro.setImageResource(
                R.drawable.ic_flecha_igual
            )
            if (response.monitors.eur.color == "green") binding.imgflechaEuro.setImageResource(
                R.drawable.ic_flechaverde
            )

            binding.txtVariacionEuro.text = response.monitors.eur.percent.toString()
        }else{

            binding.btnEuroP.text = response.monitors.eur.price_old.toString()
            binding.btnEuroP.textOff = response.monitors.eur.price_old.toString()
            binding.btnEuroP.textOn = response.monitors.eur.price_old.toString()
            binding.txtFechaActualizacionPara.text = response.monitors.eur.last_update_old
            //binding.txtFechaActualizacionBcv.text= response.monitors.eur.last_update_old

            if (response.monitors.eur.color == "red") binding.imgflechaEuro.setImageResource(
                R.drawable.ic_flecha_roja
            )
            if (response.monitors.eur.color == "neutral") binding.imgflechaEuro.setImageResource(
                R.drawable.ic_flecha_igual
            )
            if (response.monitors.eur.color == "green") binding.imgflechaEuro.setImageResource(
                R.drawable.ic_flechaverde
            )
            binding.txtVariacionEuro.text = response.monitors.eur.percent_old.toString()
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

                    val dolarEuro = binding.btnEuroP.text.toString().toDoubleOrNull()
                        ?: 0.0 //precio del paralelo
                    val dolarBcv = binding.btnBcv.text.toString().toDoubleOrNull() ?: 0.0 //precio del paralelo

                    val dolarUsdt = binding.btnUsdt.text.toString().toDoubleOrNull() ?: 0.0 //precio del paralelo

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
                                valorDolares = dolarLimpio * dolarEuro.toDouble()
                            }
                        }

                        if (binding.btnBcv.isChecked) {

                            val cleanedText =
                                entradaDolares.replace(
                                    "[,]".toRegex(),
                                    ""
                                ) // Elimina puntos y comas
                            val entradadolarBcvLimpio = cleanedText.toDoubleOrNull() ?: 0.0


                            if (dolarBcv != null) {
                                valorDolares = entradadolarBcvLimpio * dolarBcv!!.toDouble()
                            }

                        }

                        if (binding.btnUsdt.isChecked) {

                            val cleanedText =
                                entradaDolares.replace(
                                    "[,]".toRegex(),
                                    ""
                                ) // Elimina puntos y comas
                            val entradadolarBcvLimpio = cleanedText.toDoubleOrNull() ?: 0.0


                            if (dolarUsdt != null) {
                                valorDolares = entradadolarBcvLimpio * dolarUsdt.toDouble()
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
                    val dolarEuro = binding.btnEuroP.text.toString().toDoubleOrNull() ?: 0.0
                    val dolarBcv = binding.btnBcv.text.toString().toDoubleOrNull() ?: 0.0
                    val dolarUsdt = binding.btnUsdt.text.toString().toDoubleOrNull() ?: 0.0


                    if (inputText.isNotEmpty()) {
                        val cleanedText =
                            inputText.replace("[,]".toRegex(), "").toDoubleOrNull() ?: 0.0

                        if (binding.btnEuroP.isChecked) {
                            // Dividir el valor en bolívares por el dólar paralelo
                            valorDolares = cleanedText / dolarEuro

                        }
                        if (binding.btnBcv.isChecked){
                            // Convertir bolívares a dólares usando el paralelo o BCV dependiendo del estado del switch
                            valorDolares = cleanedText / dolarBcv
                        }

                        if (binding.btnUsdt.isChecked){
                            // Convertir bolívares a dólares usando el paralelo o BCV dependiendo del estado del switch
                            valorDolares = cleanedText / dolarUsdt
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
            binding.imglogo.setImageResource(R.drawable.logopremiun)
        } else {
            // Si no lo es, lo ocultamos.
            binding.imgpremium.visibility = View.GONE
            binding.imglogo.setImageResource(R.drawable.logoredondo)
        }
    }


    override fun onResume() {
        super.onResume()

        comenzarCarga()
        updatePremiumIconVisibility()

        //Elimina el boton de anuncios por recompensa
        updateRewardButtonVisibility()
   
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
            llenarCampoBCVyUSDT(savedResponseDolar, diaActual)
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

    private fun calcularDiferencia() {
        val dolarEuro = binding.btnEuroP.text?.toString()?.toDoubleOrNull() ?: 0.0
        val dolarBcv = binding.btnBcv.text?.toString()?.toDoubleOrNull() ?: 0.0
        val dolarUSDT = binding.btnUsdt.text?.toString()?.toDoubleOrNull() ?: 0.0

        // 1. Capturamos lo que escribió el usuario
        val inputUsuario = binding.inputDolares.text?.toString()?.toDoubleOrNull() ?: 0.0

        // 2. LOGICA DE NEGOCIO: Si es 0 o vacío, usamos 1.0 para el cálculo y reporte
        val cantidadBase = if (inputUsuario == 0.0) 1.0 else inputUsuario

        if (dolarBcv == 0.0) return // Evitar errores si no hay tasa BCV

        // Cálculos (usando cantidadBase que nunca será 0)
        val totalBsBcv = dolarBcv * cantidadBase
        val totalBsEuro = dolarEuro * cantidadBase
        val totalBsUsdt = dolarUSDT * cantidadBase

        val difBsEuro = totalBsEuro - totalBsBcv
        val difPorcEuro = ((dolarEuro - dolarBcv) / dolarBcv) * 100

        val difBsUsdt = totalBsUsdt - totalBsBcv
        val difPorcUsdt = ((dolarUSDT - dolarBcv) / dolarBcv) * 100

        showResultadosSheetPro(cantidadBase, difBsEuro, difPorcEuro, difBsUsdt, difPorcUsdt)
    }


    private fun showResultadosSheetPro(
        cantidadBase: Double,
        difBsEuro: Double,
        difPorcEuro: Double,
        difBsUsdt: Double,
        difPorcUsdt: Double
    ) {
        // Usamos 'view.context' o 'requireContext()' para asegurar el contexto válido
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_resultados, null)

        val dfMoney = DecimalFormat("#,##0.00")
        val dfPercent = DecimalFormat("0.00")

        // Vincular vistas
        val tvResumen = view.findViewById<TextView>(R.id.tvResumenBase)
        val tvDifBsEuro = view.findViewById<TextView>(R.id.tvDifBsEuro)
        val tvPorcEuro = view.findViewById<TextView>(R.id.tvPorcEuro)
        val tvDifBsUsdt = view.findViewById<TextView>(R.id.tvDifBsUsdt)
        val tvPorcUsdt = view.findViewById<TextView>(R.id.tvPorcUsdt)
        val btnClose = view.findViewById<Button>(R.id.btnCloseSheet)

        // Texto de Resumen
       // tvResumen.text = "Análisis base: $${dfMoney.format(cantidadBase)} BCV"
        tvResumen.text = getString(R.string.texto_analisis_base, dfMoney.format(cantidadBase))

        // Datos Euro
        // 1. EURO: Definir qué String usar dependiendo si es > 0
        val stringIdEuro = if (difBsEuro > 0) R.string.diferencia_bs_positiva else R.string.diferencia_bs_estandar
// Asignar texto pasando el número formateado
        tvDifBsEuro.text = getString(stringIdEuro, dfMoney.format(difBsEuro))

// Porcentaje Euro
        tvPorcEuro.text = getString(R.string.texto_porcentaje, dfPercent.format(difPorcEuro))


// 2. USDT: Misma lógica
        val stringIdUsdt = if (difBsUsdt > 0) R.string.diferencia_bs_positiva else R.string.diferencia_bs_estandar
        tvDifBsUsdt.text = getString(stringIdUsdt, dfMoney.format(difBsUsdt))

// Porcentaje USDT
        tvPorcUsdt.text = getString(R.string.texto_porcentaje, dfPercent.format(difPorcUsdt))
        // --- Lógica de Colores (Negritas y Semánticos) ---

        // Obtenemos los colores de forma segura
        // R.color.positive_green debe existir en tus colors.xml, o usa Color.GREEN
        val colorPositivo = ContextCompat.getColor(requireContext(), R.color.green)
        val colorNegativo = Color.RED // O un color definido en tu xml como R.color.negative_red

        // Aplicar color a Euro
        tvDifBsEuro.setTextColor(if (difBsEuro >= 0) colorPositivo else colorNegativo)

        // Aplicar color a USDT
        tvDifBsUsdt.setTextColor(if (difBsUsdt >= 0) colorPositivo else colorNegativo)

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.setContentView(view)
        dialog.show()
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