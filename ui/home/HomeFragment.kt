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
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.carlosv.dolaraldia.ApiService
import com.carlosv.dolaraldia.ui.bancos.BancosModel
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentHomeBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import javax.inject.Singleton


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var bcvActivo: Boolean?= null
    private var valorActualParalelo: Double? = 0.0
    private var valorActualBcv: Double? = 0.0
    var numeroNoturno = 0


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

        llamarPrecio()
        // Obtén una referencia a SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("MiPreferencia", AppCompatActivity.MODE_PRIVATE  )
        // Obtener referencia a SharedPreferences

// Recuperar el valor entero
        numeroNoturno = sharedPreferences.getInt("numero_noturno", 0)

        //VERIFICA SI QUE MEDO TIENE GUARDADO
       // setDayNight(modoDark())
        binding.swipeRefreshLayout.setOnRefreshListener {
           binding.swipeRefreshLayout.isRefreshing = true
            llamarPrecio()
           // llamarParalelo()
        }

        binding.switchDolar.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                println("isChecked derecha: $isChecked valorActualParalelo $valorActualParalelo")
                // El interruptor está derecha
                Log.d("Multiplicacion", "CAMBIO SUICHE: ")

                actualzarMultiplicacion(valorActualParalelo)

                bcvActivo = false
                binding.btnBcv.isChecked = false
                binding.btnParalelo.isChecked = true

            } else {
                // El interruptor está izquierda
                println("isChecked Izquierda: $isChecked valorActualBcv $valorActualBcv")
                actualzarMultiplicacion(valorActualBcv)
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


        // Aplicar la animación
        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.appear_from_top)
        root.startAnimation(animation)

        //***********************
        return root
    }

    //INTERFACE PARA COMUNICAR CON EL ACTIVITY
    object ApiResponseHolder {
        private var response: BancosModel? = null

        fun getResponse(): BancosModel? {
            return response
        }

        fun setResponse(newResponse: BancosModel) {
            response = newResponse
        }
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


    private fun actualzarMultiplicacion(valorActualDolar: Double?) {
        val decimalFormat = DecimalFormat("#,##0.00") // Declaración de DecimalFormat
        var valorDolares = 0.0
        val inputText = binding.inputDolares.text.toString()

        if (inputText.isNotEmpty()) {

            try {
                if (valorActualDolar != null) {
                    val precioSincoma = inputText.replace("[,]".toRegex(), "") // Elimina puntos y comas
                    // val precioSincoma = precioTexto.toDoubleOrNull() ?: 0.0
                    valorDolares = precioSincoma.toDouble() * valorActualDolar.toDouble()
                    // valorDolares = inputText.toDouble() * valorActualDolar!!.toDouble()
                    println("ENTRO AL TRY Y EL VALOR DE  valorDolares $valorDolares inputText $inputText")
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
    private fun actualzarDivision(valorActualDolar: Double?) {
        val decimalFormat = DecimalFormat("#,##0.00") // Declaración de DecimalFormat
        var valorDolares = 0.0
        val inputTextBs = binding.inputBolivares.text.toString()
        println("Actualizar Multiplicacion: valorActualDolar $valorActualDolar inputText $inputTextBs")
        if (inputTextBs.isNotEmpty()) {

            try {
                if (valorActualDolar != null) {
                    val precioSincomaBs = inputTextBs.replace("[,]".toRegex(), "") // Elimina puntos y comas
                    // val precioSincoma = precioTexto.toDoubleOrNull() ?: 0.0
                    valorDolares = precioSincomaBs.toDouble() / valorActualDolar.toDouble()
                    // valorDolares = inputText.toDouble() * valorActualDolar!!.toDouble()
                    println("ENTRO AL TRY Y EL VALOR DE  valorDolares $valorDolares inputText $inputTextBs")
                }
                val formattedValorDolares = decimalFormat.format(valorDolares)
                binding.inputDolares.setText(formattedValorDolares)
            }catch (e: NumberFormatException){
                Log.d("Multiplicacion", "actualzarMultiplicacion: $e")
            }

        } else {
            binding.inputBolivares.text?.clear()
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

    fun llamarPrecio() {
        try {
            val savedResponseBCV = getResponseFromSharedPreferences(requireContext())

            if (savedResponseBCV != null) {
                ApiResponseHolder.setResponse(savedResponseBCV)
                valorActualBcv = savedResponseBCV.monitors.bcv.price.toDouble()
                valorActualParalelo = savedResponseBCV.monitors.enparalelovzla.price.toDouble()
                llenarCampoBCV(savedResponseBCV)
                llenarCampoParalelo(savedResponseBCV)
                multiplicaDolares()
                dividirABolivares()
            }
        }catch (e: Exception){
            Toast.makeText(requireContext(), "Problemas de Conexion $e", Toast.LENGTH_SHORT).show()
        }


        lifecycleScope.launch(Dispatchers.IO) {
            val url = "https://pydolarvenezuela-api.vercel.app/api/v1/dollar/page?page=exchangemonitor"
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
                Log.d("RESPUESTA", " VALOR DEL RESPONSE en BCV VIEJO $response ")

                if (response != null) {
                    ApiResponseHolder.setResponse(response)
                    valorActualBcv = response.monitors.bcv.price.toDouble()
                    valorActualParalelo = response.monitors.enparalelovzla.price.toDouble()
                    guardarResponse(requireContext(), response)

                    withContext(Dispatchers.Main) {
                        binding.swipeRefreshLayout.isRefreshing = false
                        llenarCampoBCV(response)
                        llenarCampoParalelo(response)
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

                    binding.swipeRefreshLayout.isRefreshing = false
                }

                println("Error: ${e.message}")
            }
        }
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


    fun llenarCampoBCV(response: BancosModel) {
        Log.d("RESPUESTA", " VALOR DEL response $response valor de ${response.monitors.bcv.price}  ")
        if (!response.monitors.bcv.price.isNullOrEmpty()){
            binding.btnBcv.text = response.monitors.bcv.price
            binding.btnBcv.textOff =  response.monitors.bcv.price
            binding.btnBcv.textOn =  response.monitors.bcv.price
            //binding.txtFechaActualizacion.text= response.datetime.date
            if (response.monitors.bcv.color == "red"){
                binding.imgflechaBcv.setImageResource(R.drawable.ic_flecha_roja)
            }
            if (response.monitors.bcv.color == "green"){
                binding.imgflechaBcv.setImageResource(R.drawable.ic_flechaverde)
            }

            if (response.monitors.bcv.color == "neutral"){
                binding.imgflechaBcv.setImageResource(R.drawable.ic_flecha_igual)
            }
        }
        binding.txtVariacionBcv.text= response.monitors.bcv.percent


    }

    fun llenarCampoParalelo(response: BancosModel) {
        val decimalFormat = DecimalFormat("#,##0.00") // Declaración de DecimalFormat
        if (!response.monitors.enparalelovzla.price.isNullOrEmpty()) {
            binding.btnParalelo.text = response.monitors.enparalelovzla.price
            binding.btnParalelo.textOff = response.monitors.enparalelovzla.price
            binding.btnParalelo.textOn = response.monitors.enparalelovzla.price
            binding.txtFechaActualizacion.text = response.monitors.enparalelovzla.last_update

            if (response.monitors.enparalelovzla.color == "red") {
                binding.imgFlechaParalelo.setImageResource(R.drawable.ic_flecha_roja)
            }
            if (response.monitors.enparalelovzla.color == "green") {
                binding.imgFlechaParalelo.setImageResource(R.drawable.ic_flechaverde)
            }

            if (response.monitors.enparalelovzla.color == "neutral") {
                binding.imgFlechaParalelo.setImageResource(R.drawable.ic_flecha_igual)
            }
            binding.txtVariacionParalelo.text = response.monitors.enparalelovzla.percent
        }
    }





    private fun multiplicaDolares() {
        val decimalFormat = DecimalFormat("#,##0.00") // Declaración de DecimalFormat
        Log.d("RESPUESTA", "DENTRO DEL IF INPUTTEX: multiplicaDolares  valorActualParalelo $valorActualParalelo ")
        binding.inputDolares?.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
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
                            Log.d("RESPUESTA", "DENTRO DEL IF INPUTTEX valorActualBcv $valorActualBcv ")
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



