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
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.carlosv.dolaraldia.ApiService
import com.carlosv.dolaraldia.DolarBCVResponse
import com.carlosv.dolaraldia.DolarParaleloResponse
import com.carlosv.menulateral.databinding.FragmentHomeBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat


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


       // CargarFragment()
        llamarParalelo()
        llamarBCV()
        // Obtén una referencia a SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("MiPreferencia", AppCompatActivity.MODE_PRIVATE  )
        // Obtener referencia a SharedPreferences

// Recuperar el valor entero
        numeroNoturno = sharedPreferences.getInt("numero_noturno", 0)

        //VERIFICA SI QUE MEDO TIENE GUARDADO
       // setDayNight(modoDark())
        binding.swipeRefreshLayout.setOnRefreshListener {
           binding.swipeRefreshLayout.isRefreshing = true
            llamarBCV()
            llamarParalelo()
        }

        binding.switchDolar.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                println("isChecked derecha: $isChecked valorActualParalelo $valorActualParalelo")
                // El interruptor está derecha
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


//        binding.btnCompartir.setOnClickListener {
//            takeScreenshot()
//        }

//        val textView: TextView = binding.btnBcv
//        homeViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
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

    private fun actualzarMultiplicacion(valorActualDolar: Double?) {
        val decimalFormat = DecimalFormat("#,##0.00") // Declaración de DecimalFormat
        var valorDolares = 0.0
        val inputText = binding.inputDolares.text.toString()
        println("Actualizar Multiplicacion: valorActualDolar $valorActualDolar inputText $inputText")
        if (inputText.isNotEmpty()) {

            if (valorActualDolar != null) {
                val cleanedText = inputText.replace("[,]".toRegex(), "") // Elimina puntos y comas
                val parsedValue = cleanedText.toDoubleOrNull() ?: 0.0
                valorDolares = parsedValue * valorActualParalelo!!.toDouble()
                valorDolares = inputText.toDouble() * valorActualDolar!!.toDouble()
            }
            val formattedValorDolares = decimalFormat.format(valorDolares)
            binding.inputBolivares.setText(formattedValorDolares)
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

    fun llamarBCV() {
        val savedResponseBCV = getResponseFromSharedPreferencesBCV(requireContext())

        if (savedResponseBCV != null) {
          //  llenarCampoBCV(savedResponseBCV)
            valorActualBcv = savedResponseBCV.monitors.usd.price
        }
        lifecycleScope.launch(Dispatchers.IO){


        val url = "https://pydolarvenezuela-api.vercel.app/api/v1/dollar/page?page=bcv"
        val baseUrl = "https://pydolarvenezuela-api.vercel.app/api/v1/dollar/"

        val client = OkHttpClient.Builder().build()
        val request = Request.Builder()
            .url(url)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)  // Modifica la URL base para que termine con una barra diagonal
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()


        val apiService = retrofit.create(ApiService::class.java)

        try {
            val response = apiService.getBcv(url)
            Log.d("RESPUESTA", " VALOR DEL RESPONSE en BCV VIEJO $response ")
            if (response != null) {
                valorActualBcv = response.monitors.usd.price
                guardarResponseBCV(requireContext(), response)
                withContext(Dispatchers.Main){
                    llenarCampoBCV(response)
                }

            }

            multiplicaDolares()
            dividirABolivares()

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    requireContext(),
                    "No Actualizo dolar BCV Revise Conexion $e",
                    Toast.LENGTH_LONG
                ).show()
            }

            println("Error: ${e.message}")
        }
        }
    }


    fun llamarParalelo() {
        val savedResponse = getResponseFromSharedPreferencesParalelo(requireContext())
        if (savedResponse != null) {
                llenarCampoParalelo(savedResponse)
                valorActualParalelo = savedResponse.preciodolarParalelo
        }
        lifecycleScope.launch(Dispatchers.IO) {
        // binding.progressBar.visibility = View.VISIBLE
        val url = "https://pydolarvenezuela-api.vercel.app/api/v1/dollar/unit/enparalelovzla"
        val baseUrl = "https://pydolarvenezuela-api.vercel.app/api/v1/dollar/unit/"

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
                val response = apiService.getPrecioParalelo(url)
                if (response != null) {
                    valorActualParalelo = response.preciodolarParalelo
                    guardarResponse(requireContext(), response)
                    withContext(Dispatchers.Main){
                        llenarCampoParalelo(response)
                    }


                    withContext(Dispatchers.Main) {
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                    // binding.progressBar.visibility = View.GONE
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "No Actualizo dolar Paralelo Revise Conexion $e",
                        Toast.LENGTH_LONG

                    ).show()
                    Log.d("RESPUESTA", " ERROR DEL CATCH $e ")
                }

                withContext(Dispatchers.Main) {
                    binding.swipeRefreshLayout.isRefreshing = false
                }

                println("Error DEL CATCH: ${e.message}")
            }
        }
    }


    // Define una función para recuperar la respuesta de SharedPreferences
    private fun getResponseFromSharedPreferencesParalelo(context: Context): DolarParaleloResponse? {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val responseJson = sharedPreferences.getString("dolarParaleloResponse", null)

        if (responseJson != null) {
            val gson = Gson()
            return gson.fromJson(responseJson, DolarParaleloResponse::class.java)
        }

        return null // Retorna null si no se encontró la respuesta en SharedPreferences
    }


    private fun guardarResponse(context: Context, response: DolarParaleloResponse) {
        val gson = Gson()
        val responseJson = gson.toJson(response)

        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("dolarParaleloResponse", responseJson)
        editor.apply()
    }

    private fun guardarResponseBCV(context: Context, responseBCV: DolarBCVResponse) {
        val gson = Gson()
        val responseJson = gson.toJson(responseBCV)

        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("dolarBCVResponse", responseJson)
        editor.apply()
    }

    // Define una función para recuperar la respuesta de SharedPreferences
    private fun getResponseFromSharedPreferencesBCV(context: Context): DolarBCVResponse? {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val responseJson = sharedPreferences.getString("dolarBCVResponse", null)

        if (responseJson != null) {
            val gson = Gson()
            return gson.fromJson(responseJson, DolarBCVResponse::class.java)
        }

        return null // Retorna null si no se encontró la respuesta en SharedPreferences
    }


    fun llenarCampoBCV(response: DolarBCVResponse) {
        Log.d("RESPUESTA", " VALOR DEL response $response valor de ${response.monitors.usd.price.toString()}  ")
        if (!response.monitors.usd.price.toString().isNullOrEmpty()){
            binding.btnBcv.text = response.monitors.usd.price.toString()
            binding.btnBcv.textOff = response.monitors.usd.price.toString()
            binding.btnBcv.textOn = response.monitors.usd.price.toString()
            // binding.txtFechaActualizacion.text= response.datetime.date
        }


    }

    fun llenarCampoParalelo(response: DolarParaleloResponse) {
        val decimalFormat = DecimalFormat("#,##0.00") // Declaración de DecimalFormat
        binding.btnParalelo.text = response.preciodolarParalelo.toString()
        binding.btnParalelo.textOff = response.preciodolarParalelo.toString()
        binding.btnParalelo.textOn = response.preciodolarParalelo.toString()
        binding.txtFechaActualizacion.text = response.fecha
        val precioOld = response.priceparalelo_old
        val precioNew = response.preciodolarParalelo
        var variacion = ((precioNew - precioOld) / precioOld)
        //val formattedValorDolares = decimalFormat.format(variacion)
        binding.txtVariacionParalelo.text = precioOld.toString()
    }


    private fun multiplicaDolares() {
        val decimalFormat = DecimalFormat("#,##0.00") // Declaración de DecimalFormat

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
            montoDolarCopy.toDouble()
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

//    fun setDayNight(sw: Int) {
//        if (sw == 1) {
//            requireActivity().delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
//        } else {
//            requireActivity().delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_NO
//        }
//    }


    // Solicitar permiso de almacenamiento
    private val STORAGE_REQUEST_CODE = 100
    private fun requestStoragePermission() {

        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_REQUEST_CODE
        )

    }

    // Capturar y compartir imagen
    private var imageUri: Uri? = null

    private fun takeScreenshot() {

        requestStoragePermission()

        val bitmap = screenshot()

        imageUri = saveImage(bitmap)

        shareImage(imageUri!!)

    }

    private fun screenshot(): Bitmap {
        // Obtener referencia al view raíz del fragmento
        val rootView = requireView()

        // Obtener dimensiones para crear Bitmap
        val width = rootView.width
        val height = rootView.height

        // Crear Bitmap
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Crear Canvas asociado al Bitmap
        val canvas = Canvas(bitmap)

        // Dibujar la vista raíz en el Canvas
        rootView.draw(canvas)

        return bitmap
    }


    private fun saveImage(image: Bitmap): Uri? {
        val contentResolver = requireContext().contentResolver
        val name = "MiCapture.jpg"
        val imagesFolder = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File(imagesFolder, name)

        val stream = FileOutputStream(imageFile)
        image.compress(Bitmap.CompressFormat.JPEG, 100, stream)

        stream.flush()
        stream.close()

        return Uri.parse(
            MediaStore.Images.Media.insertImage(
                contentResolver,
                imageFile.absolutePath,
                name,
                null
            )
        )

    }

    private fun shareImage(imageUri: Uri) {

        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_STREAM, imageUri)
        intent.type = "image/jpeg"
        intent.putExtra(Intent.EXTRA_TITLE, "Compartir captura")
        intent.putExtra(Intent.EXTRA_TEXT, "Descarla la aplicacion y disfruta de la comodidad")
        startActivity(Intent.createChooser(intent, "Compartir captura"))

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permiso otorgado
        } else {
            // Permiso denegado
        }

    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



