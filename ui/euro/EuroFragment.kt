package com.carlosv.dolaraldia.ui.acerca

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.carlosv.dolaraldia.ApiService
import com.carlosv.dolaraldia.MainActivity
import com.carlosv.dolaraldia.ui.bancos.BancosModel
import com.carlosv.dolaraldia.ui.home.HomeFragment
import com.carlosv.dolaraldia.ui.home.HomeViewModel
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentAcercaBinding
import com.carlosv.menulateral.databinding.FragmentEuroBinding
import com.carlosv.menulateral.databinding.FragmentHomeBinding
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
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.text.DecimalFormat
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class EuroFragment : Fragment() {


    private var _binding: FragmentEuroBinding? = null

    private val binding get() = _binding ?: throw IllegalStateException("Binding is null")

    private var bcvActivo: Boolean?= null
    private var valorActualParalelo: Double? = 0.0
    private var valorActualEuro: Float? = 0.0f
    var numeroNoturno = 0
    lateinit var mAdView : AdView


    lateinit var navigation : BottomNavigationView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentEuroBinding.inflate(inflater, container, false)
        val root: View = binding.root

      //  val fabBoton = (activity as MainActivity).fabBoton

        // Ahora puedes trabajar con el FloatingActionButton
    //    fabBoton.visibility= View.VISIBLE

        //Para el Admon
        MobileAds.initialize(requireContext()) {}
        mAdView = root.findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        //VERIFICA SI QUE MEDO TIENE GUARDADO
        // setDayNight(modoDark())
        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = true
            llenarCampoEuro()
            // llamarParalelo()
        }



        binding.imgCopyDolar.setOnClickListener {
            copiarDolar()
        }
        binding.imgCoyBolivar.setOnClickListener {
            copiarBs()
        }


        //PARA ACTUALIZAR EL PRECIO DEL DOLAR SOLO CUANDO CARGA POR PRIMERA VEZ
       // if(savedInstanceState== null){

         //  disableSSLVerification()
            llenarCampoEuro()
      //  }

        // Aplicar la animación
        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.appear_from_top)
        root.startAnimation(animation)

        //***********************
        return root
    }




    //recupera el valor del nuemero Nocturno
    private fun modoDark(): Int {
        val sharedPreferences = requireContext().getSharedPreferences("MiPreferencia",
            AppCompatActivity.MODE_PRIVATE
        )

        val numeroRecuperado = sharedPreferences.getInt("numero_noturno", 0)
        return numeroRecuperado
    }
    fun tieneFoco(textView: TextView): Boolean {
        return textView.isFocused
    }

    fun llenarCampoEuro() {
        val responseBCV = HomeFragment.ApiResponseHolder.getResponseApiBancoBCV()
        if (responseBCV!=null){
            valorActualEuro= responseBCV.monitors.eur.price.toFloat()
            val decimalFormat = DecimalFormat("#,##0.00") // Declaración de DecimalFormat
            val formattedValorDolares = decimalFormat.format(valorActualEuro)
            binding.btnEuro.text =formattedValorDolares.toString()
            binding.txtFechaActualizacion.text = responseBCV.monitors.eur.last_update
            multiplicaDolares()
            dividirABolivares()

        }else{

            val valorEuroRecuperado= HomeFragment.ApiResponseHolder.recuperarEuro(requireContext())
            val valorFechaRecuperado = HomeFragment.ApiResponseHolder.recuperarEuroFecha(requireContext())
            Log.d(
                "EUROACTU",
                "VALOR DE llenarCampoEuro:valorEuroRecuperado $valorEuroRecuperado y valorFechaRecuperado $valorFechaRecuperado")
            if (valorEuroRecuperado!= null){
                valorActualEuro = valorEuroRecuperado
                val decimalFormat = DecimalFormat("#,##0.00") // Declaración de DecimalFormat
                val formattedValorDolares = decimalFormat.format(valorEuroRecuperado)
                binding.btnEuro.text =formattedValorDolares.toString()
                multiplicaDolares()
                dividirABolivares()

            }
            if (valorFechaRecuperado!=null){
                binding.txtFechaActualizacion.text= valorFechaRecuperado
            }
            binding.swipeRefreshLayout.isRefreshing = false
        }






    }


    private fun multiplicaDolares() {
        val decimalFormat = DecimalFormat("#,##0.00") // Declaración de DecimalFormat

        binding.inputEuros?.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                var valorEuros = 0.0
                if (binding.inputEuros.isFocused) {
                    val inputText = binding.inputEuros.text.toString()
                    if (inputText.isNotEmpty()) {
                            if (valorActualEuro != null) {
                                val cleanedText =
                                    inputText.replace("[,]".toRegex(), "") // Elimina puntos y comas
                                val parsedValue = cleanedText.toDoubleOrNull() ?: 0.0
                                valorEuros = parsedValue * valorActualEuro!!.toDouble()
                            }
                        val formattedValorDolares = decimalFormat.format(valorEuros)
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
                var valorEuros = 0.0
                if (binding.inputBolivares.isFocused) {
                    val inputBs = binding.inputBolivares.text.toString()
                    if (inputBs.isNotEmpty()) {
                        if (valorActualEuro != null) {
                            val cleanedText =
                                inputBs.replace("[,]".toRegex(), "") // Elimina puntos y comas
                            val parsedValue = cleanedText.toDoubleOrNull() ?: 0.0
                            valorEuros = parsedValue / valorActualEuro!!.toDouble()

                        }

                        val formattedValorEuros = decimalFormat.format(valorEuros)
                        binding.inputEuros.setText(formattedValorEuros)
                    } else {
                        binding.inputEuros.text?.clear()
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
        var montoDolarCopy = binding.inputEuros.text.toString()
        if (!montoDolarCopy.isNullOrEmpty()) {
            //montoDolarCopy.toDouble()
            try {
                copyToClipboard(requireContext(), montoDolarCopy.toString(), "$montoDolarCopy", "$")
            }catch (e:NumberFormatException){
                Toast.makeText(requireContext(), "No se puedo guardar", Toast.LENGTH_SHORT).show()
            }

        } else {
            Toast.makeText(requireContext(), "Campo vacio", Toast.LENGTH_SHORT).show()
        }

    }

    //COPIAR LOS DATOS AL PORTAPEPEL
    private fun copiarBs() {
        val montoBolivarCopy = binding.inputBolivares.text.toString()
        if (montoBolivarCopy.isNotEmpty()) {
            val cadenaNumerica = montoBolivarCopy.replace(",", "")

            // Verificación de que la cadena es un número válido
            try {
                val montoBolivarCopyLimpio = cadenaNumerica.toDouble()
                copyToClipboard(requireContext(), montoBolivarCopyLimpio.toString(), montoBolivarCopy, "Bs.")
            } catch (e: NumberFormatException) {
                // Maneja el caso en el que la cadena no es un número válido
                Toast.makeText(requireContext(), "Por favor, ingrese un número válido", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Campo vacío", Toast.LENGTH_SHORT).show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        lifecycleScope.coroutineContext.cancel()
        _binding = null
    }
}



