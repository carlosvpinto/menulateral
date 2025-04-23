package com.carlosv.dolaraldia.ui.historial

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.carlosv.dolaraldia.ui.home.HomeViewModel
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentHistoryBinding
import com.carlosv.menulateral.databinding.FragmentHomeBinding

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import androidx.annotation.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.carlosv.dolaraldia.ApiService
import com.carlosv.dolaraldia.adapter.BancosAdapter
import com.carlosv.dolaraldia.model.apicontoken.ApiConTokenResponse
import com.carlosv.dolaraldia.model.history.History
import com.carlosv.dolaraldia.model.history.HistoryModelResponse
import com.carlosv.dolaraldia.ui.bancos.BancoModelAdap
import com.carlosv.dolaraldia.ui.home.HomeFragment
import com.carlosv.dolaraldia.utils.Constants.URL_BASE

import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.highlight.Highlight
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import kotlin.math.roundToInt


class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null")

    private var diasMenos: Int = 7
    private var fechaInicio = 0
    private var fechaSeleccionada= ""
    private val TAG = "HISTORYFRAGMENT"
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val root: View = binding.root


        llamarApiHistory(diasMenos)
        configurarCarga()


        return root
    }

    private fun configurarCarga(){
        // Obtener el array de bancos desde los recursos
        val listaHistory = resources.getStringArray(R.array.lista_history)

        val adaptadorHist: ArrayAdapter<String> = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listaHistory
        )
        binding.spinnerHistory.adapter = adaptadorHist
        // Establecer un listener para manejar la selección del spinner
        binding.spinnerHistory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position== 0){
                    diasMenos = 7
                    llamarApiHistory(diasMenos)
                    binding.progressBar.visibility= View.VISIBLE
                   
                }
                if (position==1){
                    diasMenos = 30
                    llamarApiHistory(diasMenos)
                    binding.progressBar.visibility= View.VISIBLE
                }
                fechaSeleccionada = parent.getItemAtPosition(position).toString()
                Log.d(TAG, "onItemSelected: fechaSeleccionada $fechaSeleccionada")
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                Toast.makeText(requireContext(), "Debe selecionar el banco", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun cargarGrafico() {
        // Determinar si está en modo oscuro o claro
        val isDarkTheme = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val primaryTextColor = if (isDarkTheme) Color.WHITE else Color.BLACK

        val responseHistoryBcv = HomeFragment.ApiResponseHolder.getResponseHistoryBcv()
        val responseHistoryParalelo = HomeFragment.ApiResponseHolder.getResponseHistoryParalelo()
        Log.d(TAG, "cargarGrafico:responseHistory $responseHistoryBcv responseHistoryParalelo $responseHistoryParalelo")
        val (dates, dolarBcvValues, dolarParaloValues) = convertirResponseApiHistory(responseHistoryBcv!!, responseHistoryParalelo!!)
        Log.d(TAG, "cargarGrafico:responseHistory: $responseHistoryBcv ")
        Log.d(TAG, "cargarGrafico: dates $dates dolarParallelValues: $dolarParaloValues")

        val entriesBcv = dolarBcvValues.mapIndexed { index, value ->
            Entry(index.toFloat(), value)
        }

        val entriesParallel = dolarParaloValues.mapIndexed { index, value ->
            Entry(index.toFloat(), value)
        }

        val dataSetBcv = LineDataSet(entriesBcv, "Dólar BCV").apply {
            color = Color.BLUE
            valueTextColor = primaryTextColor
            valueTextSize = 14f
            lineWidth = 3f
        }

        val dataSetParallel = LineDataSet(entriesParallel, "Dólar Paralelo").apply {
            color = Color.RED
            valueTextColor = primaryTextColor
            valueTextSize = 14f
            lineWidth = 3f
        }

        val lineData = LineData(dataSetBcv, dataSetParallel)
        binding.lineChart.fitScreen()
        binding.lineChart.data = lineData

        val xAxis = binding.lineChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(dates)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = primaryTextColor
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f

        binding.lineChart.description.isEnabled = false

        val description = Description().apply {
            text = "Comparación del Dólar BCV y Paralelo"
            textColor = primaryTextColor
            textSize = 10f
        }
        // binding.lineChart.description = description

        val legend = binding.lineChart.legend
        legend.textSize = 16f
        legend.textColor = primaryTextColor
        legend.formSize = 14f
        legend.xEntrySpace = 20f

        binding.lineChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                e?.let {
                    val index = it.x.toInt()
                    val valores = mutableListOf<String>()

                    // Verificar y agregar valores según disponibilidad
                    if (index in dates.indices) {
                        val fecha = dates[index] ?: "N/A"
                        valores.add("Fecha: $fecha")
                    }
                    if (index in dolarBcvValues.indices) {
                        val dolarBcv = dolarBcvValues[index]
                        valores.add("Dólar BCV: $dolarBcv")
                    }
                    if (index in dolarParaloValues.indices) {
                        val dolarParallel = dolarParaloValues[index]
                        valores.add("Dólar Paralelo: $dolarParallel")
                    }

                    // Mostrar los valores disponibles o un mensaje de no disponible
                    binding.txtValores.text = if (valores.isNotEmpty()) valores.joinToString("\n") else "Valores no disponibles"
                    binding.txtValores.setTextColor(primaryTextColor)
                    animacionCrecerTexto(binding.txtValores)
                } ?: run {
                    binding.txtValores.text = "No se seleccionó ningún valor"
                    binding.txtValores.setTextColor(primaryTextColor)
                }
            }

            override fun onNothingSelected() {
                // No hacer nada si no se selecciona nada
            }
        })

        binding.lineChart.invalidate()
    }





    fun convertirResponseApiHistory(
        responseApiHistoryBcv: HistoryModelResponse,
        responseApiHistoryParalelo: HistoryModelResponse
    ): Triple<List<String>, List<Float>, List<Float>> {
        val dates = mutableListOf<String>()
        val pricesBcv = mutableListOf<Float>()
        val pricesParalelo = mutableListOf<Float>()

        // Agregar los precios de BCV en la lista
        responseApiHistoryBcv.history.forEach { history ->
            pricesBcv.add(history.price.toFloat())
        }

        // Agregar las fechas y precios paralelos en las listas correspondientes
        responseApiHistoryParalelo.history.forEach { historyParalelo ->
            // Eliminar la hora y mantener solo la fecha
            val dateOnly = historyParalelo.last_update.split(",")[0].trim()
            dates.add(dateOnly)
            pricesParalelo.add(historyParalelo.price.toFloat())
        }

        // Invertir el orden de las listas antes de devolverlas
        return Triple(dates.reversed(), pricesBcv.reversed(), pricesParalelo.reversed())
    }


    //LLAMA AL API DE HISTORY ***************************
    private fun llamarApiHistory(diasMenos: Int) {
        try {
            val savedResponseDolar = getResponseFromSharedPreferences(requireContext())
            if (savedResponseDolar != null) {
                HomeFragment.ApiResponseHolder.setResponse(savedResponseDolar)
            }
        } catch (e: Exception) {
            Log.d(TAG, "llamarApiHistory: error $e")
        } finally {
            // Cualquier código adicional que desees agregar
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            Log.d(TAG, "llamarApiHistory: dentro de viewLifecycleOwner")
            val baseUrl = URL_BASE
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer 2x9Qjpxl5F8CoKK6T395KA")
                        .build()
                    try {
                        val response = chain.proceed(request)
                        Log.d(TAG, "Request URL: ${response.request.url}")
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
                Log.d(TAG, "llamarApiHistory: dentro de TRY viewLifecycleOwner")
                val dateFormat = SimpleDateFormat("dd-MM-yyyy")

                // Obtener la fecha actual
                val calendar = Calendar.getInstance()
                val diaDeHoy = dateFormat.format(calendar.time)

                // Restar 7 días
                calendar.add(Calendar.DAY_OF_YEAR, -diasMenos)
                val fechaInicial = dateFormat.format(calendar.time)

                // Realizar las dos solicitudes a la API para obtener enparalelovzla y bcv
                val responseEnParaleloVzla = apiService.getDollarHistory(
                    page = "enparalelovzla",
                    monitor = "enparalelovzla",
                    startDate = fechaInicial, // Fecha inicial con 7 días menos
                    endDate = diaDeHoy // Fecha de hoy
                )

                val responseBcv = apiService.getDollarHistory(
                    page = "bcv",
                    monitor = "usd",
                    startDate = fechaInicial, // Fecha inicial con 7 días menos
                    endDate = diaDeHoy // Fecha de hoy
                )

                Log.d(TAG, "llamarApiHistory: Response enparalelovzla: ${responseEnParaleloVzla.history}")
                Log.d(TAG, "llamarApiHistory: Response bcv: ${responseBcv.history}")

                if (responseEnParaleloVzla != null && responseBcv != null) {
                    withContext(Dispatchers.Main) {
                        // Guarda y establece ambas respuestas
                        HomeFragment.ApiResponseHolder.setResponseHistoryParalelo(responseEnParaleloVzla)
                        HomeFragment.ApiResponseHolder.setResponseHistoryBcv(responseBcv)
                        binding.progressBar.visibility= View.GONE
                        guardarResponseHistoryParalelo(requireContext(), responseEnParaleloVzla)
                        guardarResponseHistoryBcv(requireContext(), responseBcv)
                        cargarGrafico()

                        // Aquí podrías actualizar la UI con los datos obtenidos de ambas respuestas
                        // Por ejemplo, puedes mostrar ambos datos en gráficos separados o combinados
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Problemas de Conexión",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                // Cualquier código adicional que deba ejecutarse al finalizar la solicitud
            }
        }
    }

    private fun guardarResponseHistoryBcv(context: Context, responseHistory: HistoryModelResponse) {
        val gson = Gson()
        val responseJson = gson.toJson(responseHistory)

        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("ResponseHistory", responseJson)
        editor.apply()

    }
    private fun guardarResponseHistoryParalelo(context: Context, responseHistory: HistoryModelResponse) {
        val gson = Gson()
        val responseJson = gson.toJson(responseHistory)

        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("ResponseHistoryParalelo", responseJson)
        editor.apply()

    }






    private fun verificarVacio(precio:String?):Boolean {
        var vacio = true
        if(precio!=null){
            vacio= false
        }
        return vacio
    }

    private fun animacionCrecerTexto(texto: TextView) {
        val scaleUpAnimation = ScaleAnimation(
            1f, 1.5f, // De tamaño normal a 1.5 veces el tamaño original
            1f, 1.5f, // Igual para la altura
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        binding.txtValores.setTextColor(
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

            }

            override fun onAnimationRepeat(animation: Animation) {}
        })

        // Iniciar la primera animación
        texto.startAnimation(scaleUpAnimation)


    }

    fun getHistoricalData(token: String, page: String, startDate: String, endDate: String): String? {
        var retorno: String? = null
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()

            val url =
                "https://pydolarve.org/api/v1/dollar/history?page=$page&start_date=$startDate&end_date=$endDate"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .build()

            val response: Response = client.newCall(request).execute()
            retorno= response.body?.string()
        }
        return retorno
    }

    private fun getResponseFromSharedPreferences(context: Context): ApiConTokenResponse? {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val responseJson = sharedPreferences.getString("dolarBCVResponse", null)

        if (responseJson != null) {
            val gson = Gson()

            return gson.fromJson(responseJson, ApiConTokenResponse::class.java)
        }

        return null // Retorna null si no se encontró la respuesta en SharedPreferences
    }

    private fun guardarResponseHistory(context: Context, responseBCV: ApiConTokenResponse) {
        val gson = Gson()
        val responseJson = gson.toJson(responseBCV)

        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("dolarBCVResponse", responseJson)
        editor.apply()
    }


    companion object {

    }
}