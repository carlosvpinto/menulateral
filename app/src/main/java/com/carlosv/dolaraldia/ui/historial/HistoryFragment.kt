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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.carlosv.dolaraldia.ApiService
import com.carlosv.dolaraldia.model.history.HistoryModelResponse
import com.carlosv.dolaraldia.ui.home.HomeFragment
import com.carlosv.dolaraldia.utils.Constants.URL_BASE
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentHistoryBinding
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null")

    private var diasMenos: Int = 7
    private var fechaSeleccionada = ""
    private val TAG = "HISTORYFRAGMENT"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        llamarApiHistory(diasMenos)
        configurarCarga()

        return root
    }

    private fun configurarCarga() {
        val listaHistory = resources.getStringArray(R.array.lista_history)
        val adaptadorHist: ArrayAdapter<String> = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listaHistory
        )
        binding.spinnerHistory.adapter = adaptadorHist
        binding.spinnerHistory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                diasMenos = if (position == 0) 7 else 30
                llamarApiHistory(diasMenos)
                binding.progressBar.visibility = View.VISIBLE
                fechaSeleccionada = parent.getItemAtPosition(position).toString()
                Log.d(TAG, "onItemSelected: fechaSeleccionada $fechaSeleccionada")
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                Toast.makeText(requireContext(), "Debe seleccionar un período", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarGrafico() {
        val isDarkTheme = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val primaryTextColor = if (isDarkTheme) Color.WHITE else Color.BLACK

        val responseHistoryBcv = HomeFragment.ApiResponseHolder.getResponseHistoryBcv()
        if (responseHistoryBcv == null) {
            Log.e(TAG, "cargarGrafico: La respuesta de la API del BCV es nula.")
            Toast.makeText(requireContext(), "No se pudieron cargar los datos", Toast.LENGTH_SHORT).show()
            return
        }

        val (dates, dolarBcvValues) = convertirResponseApiHistory(responseHistoryBcv)
        Log.d(TAG, "cargarGrafico: dates $dates, dolarBcvValues: $dolarBcvValues")

        val entriesBcv = dolarBcvValues.mapIndexed { index, value ->
            Entry(index.toFloat(), value)
        }

        val dataSetBcv = LineDataSet(entriesBcv, "Dólar BCV").apply {
            color = ContextCompat.getColor(requireContext(), R.color.md_theme_light_primary) // Un color más agradable
            valueTextColor = primaryTextColor
            valueTextSize = 14f
            lineWidth = 3f
            setCircleColor(Color.BLUE)
            circleRadius = 4f
            setDrawCircleHole(false)
        }

        val lineData = LineData(dataSetBcv)
        binding.lineChart.fitScreen()
        binding.lineChart.data = lineData

        val xAxis = binding.lineChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(dates)
            position = XAxis.XAxisPosition.BOTTOM
            textColor = primaryTextColor
            granularity = 1f
            labelRotationAngle = -45f
        }

        binding.lineChart.description.isEnabled = false
        binding.lineChart.axisRight.isEnabled = false // Ocultar el eje Y derecho

        val legend = binding.lineChart.legend.apply {
            textSize = 16f
            textColor = primaryTextColor
            formSize = 14f
            xEntrySpace = 20f
        }

        binding.lineChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                e?.let {
                    val index = it.x.toInt()
                    val valores = mutableListOf<String>()

                    if (index in dates.indices) {
                        val fecha = dates[index]
                        valores.add("Fecha: $fecha")
                    }
                    if (index in dolarBcvValues.indices) {
                        val dolarBcv = dolarBcvValues[index]
                        valores.add("Dólar BCV: $dolarBcv")
                    }

                    binding.txtValores.text = if (valores.isNotEmpty()) valores.joinToString("\n") else "Valores no disponibles"
                    binding.txtValores.setTextColor(primaryTextColor)
                    animacionCrecerTexto(binding.txtValores)
                } ?: run {
                    binding.txtValores.text = "No se seleccionó ningún valor"
                    binding.txtValores.setTextColor(primaryTextColor)
                }
            }

            override fun onNothingSelected() {
                binding.txtValores.text = ""
            }
        })

        binding.lineChart.invalidate() // Refrescar el gráfico
    }

    private fun convertirResponseApiHistory(
        responseApiHistoryBcv: HistoryModelResponse
    ): Pair<List<String>, List<Float>> {
        val dates = mutableListOf<String>()
        val pricesBcv = mutableListOf<Float>()

        // Procesar los datos de BCV
        responseApiHistoryBcv.history.forEach { history ->
            val dateOnly = history.last_update.split(",")[0].trim()
            dates.add(dateOnly)
            pricesBcv.add(history.price.toFloat())
        }

        // Invertir el orden para mostrar del más antiguo al más reciente
        return Pair(dates.reversed(), pricesBcv.reversed())
    }

    private fun llamarApiHistory(diasMenos: Int) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            Log.d(TAG, "llamarApiHistory: Iniciando llamada a la API")
            val baseUrl = URL_BASE
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer 2x9Qjpxl5F8CoKK6T395KA")
                        .build()
                    chain.proceed(request)
                }
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            val apiService = retrofit.create(ApiService::class.java)

            try {
                val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val calendar = Calendar.getInstance()
                val diaDeHoy = dateFormat.format(calendar.time)

                calendar.add(Calendar.DAY_OF_YEAR, -diasMenos)
                val fechaInicial = dateFormat.format(calendar.time)
                Log.d(TAG, "llamarApiHistory: Fecha Inicial: $fechaInicial, Fecha Final: $diaDeHoy")

                // Realizar solo la solicitud para el BCV
                val responseBcv = apiService.getDollarHistory(
                    page = "bcv",
                    monitor = "usd",
                    startDate = fechaInicial,
                    endDate = diaDeHoy
                )

                if (responseBcv != null) {
                    withContext(Dispatchers.Main) {
                        HomeFragment.ApiResponseHolder.setResponseHistoryBcv(responseBcv)
                        guardarResponseHistoryBcv(requireContext(), responseBcv)
                        binding.progressBar.visibility = View.GONE
                        cargarGrafico()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "No se recibió respuesta del BCV", Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en llamarApiHistory: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Problemas de Conexión", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun guardarResponseHistoryBcv(context: Context, responseHistory: HistoryModelResponse) {
        val gson = Gson()
        val responseJson = gson.toJson(responseHistory)
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("ResponseHistoryBcv", responseJson) // Clave específica para BCV
        editor.apply()
    }

    private fun animacionCrecerTexto(texto: TextView) {
        val scaleUpAnimation = ScaleAnimation(
            1f, 1.2f, 1f, 1.2f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 300
        }

        val scaleDownAnimation = ScaleAnimation(
            1.2f, 1f, 1.2f, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 300
        }

        scaleUpAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                texto.startAnimation(scaleDownAnimation)
            }
            override fun onAnimationRepeat(animation: Animation) {}
        })

        texto.startAnimation(scaleUpAnimation)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Prevenir fugas de memoria
    }
}