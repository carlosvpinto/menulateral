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
import android.view.animation.AnimationUtils
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

    // Variable local para guardar la respuesta de USDT temporalmente
    private var responseUsdtCache: HistoryModelResponse? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Configuración inicial de UI
        configurarCarga()

        // Listener para los CheckBox (para ocultar/mostrar sin recargar API)
        setupCheckBoxListeners()

        // Cargar datos iniciales
        llamarApiHistory(diasMenos)
        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.appear_from_top)
        binding.root.startAnimation(animation)
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
                binding.progressBar.visibility = View.VISIBLE
                fechaSeleccionada = parent.getItemAtPosition(position).toString()

                // Recargamos datos de API al cambiar fecha
                llamarApiHistory(diasMenos)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) { }
        }
    }

    private fun setupCheckBoxListeners() {
        // Listener para BCV
        binding.cbBcv.setOnCheckedChangeListener { _, isChecked ->
            val data = binding.lineChart.data
            if (data != null && data.dataSetCount > 0) {
                // Asumimos que el primer conjunto (index 0) es BCV
                val setBcv = data.getDataSetByLabel("Dólar BCV", true)
                setBcv?.isVisible = isChecked
                binding.lineChart.invalidate()
            }
        }

        // Listener para USDT
        binding.cbUsdt.setOnCheckedChangeListener { _, isChecked ->
            val data = binding.lineChart.data
            if (data != null && data.dataSetCount > 0) {
                // Buscamos el set por su etiqueta
                val setUsdt = data.getDataSetByLabel("USDT (Binance)", true)
                setUsdt?.isVisible = isChecked
                binding.lineChart.invalidate()
            }
        }
    }

    private fun cargarGrafico() {
        val isDarkTheme = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val primaryTextColor = if (isDarkTheme) Color.WHITE else Color.BLACK

        val responseHistoryBcv = HomeFragment.ApiResponseHolder.getResponseHistoryBcv()
        val responseHistoryUsdt = responseUsdtCache

        if (responseHistoryBcv == null && responseHistoryUsdt == null) {
            Toast.makeText(requireContext(), "No hay datos para mostrar", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Obtener mapas de Fecha -> Precio
        val mapBcv = if (responseHistoryBcv != null) convertirAMapa(responseHistoryBcv) else emptyMap()
        val mapUsdt = if (responseHistoryUsdt != null) convertirAMapa(responseHistoryUsdt) else emptyMap()

        // 2. Crear la LISTA MAESTRA DE FECHAS (Uniendo ambas y ordenando)
        // Usamos un Set para evitar duplicados y luego ordenamos por fecha real
        val allDatesSet = mutableSetOf<String>()
        allDatesSet.addAll(mapBcv.keys)
        allDatesSet.addAll(mapUsdt.keys)

        // Ordenar las fechas cronológicamente (Importante para que el gráfico tenga sentido)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val masterDateList = allDatesSet.sortedBy {
            try { dateFormat.parse(it) } catch (e: Exception) { null }
        }

        // 3. Crear las Entries alineadas al eje X maestro
        val entriesBcv = ArrayList<Entry>()
        val entriesUsdt = ArrayList<Entry>()

        masterDateList.forEachIndexed { index, date ->
            // Si existe precio BCV para esta fecha, agregamos punto
            if (mapBcv.containsKey(date)) {
                entriesBcv.add(Entry(index.toFloat(), mapBcv[date]!!))
            }
            // Si existe precio USDT para esta fecha, agregamos punto
            if (mapUsdt.containsKey(date)) {
                entriesUsdt.add(Entry(index.toFloat(), mapUsdt[date]!!))
            }
        }

        // 4. Configurar DataSets
        val lineData = LineData()

        if (entriesBcv.isNotEmpty()) {
            val dataSetBcv = LineDataSet(entriesBcv, "Dólar BCV").apply {
                color = Color.parseColor("#2E7D32") // Verde
                valueTextColor = primaryTextColor
                valueTextSize = 10f
                lineWidth = 3f
                setCircleColor(Color.parseColor("#1B5E20"))
                circleRadius = 4f
                setDrawCircleHole(false)
                isVisible = binding.cbBcv.isChecked
                setDrawValues(false) // Opcional: Ocultar valores en la línea para limpieza
            }
            lineData.addDataSet(dataSetBcv)
        }

        if (entriesUsdt.isNotEmpty()) {
            val dataSetUsdt = LineDataSet(entriesUsdt, "USDT (Binance)").apply {
                color = Color.parseColor("#1565C0") // Azul
                valueTextColor = primaryTextColor
                valueTextSize = 10f
                lineWidth = 3f
                setCircleColor(Color.parseColor("#0D47A1"))
                circleRadius = 4f
                setDrawCircleHole(false)
                isVisible = binding.cbUsdt.isChecked
                setDrawValues(false)
            }
            lineData.addDataSet(dataSetUsdt)
        }

        // 5. Asignar al Gráfico
        binding.lineChart.data = lineData
        binding.lineChart.description.isEnabled = false
        binding.lineChart.axisRight.isEnabled = false

        // Configurar Eje X con la LISTA MAESTRA
        binding.lineChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(masterDateList)
            position = XAxis.XAxisPosition.BOTTOM
            textColor = primaryTextColor

            // --- CORRECCIÓN CRÍTICA ---
            granularity = 1f // Obliga a que los pasos sean de 1 en 1 (0, 1, 2...)
            isGranularityEnabled = true

            // Asegura que empiece y termine exacto
            axisMinimum = 0f
            axisMaximum = (masterDateList.size - 1).toFloat() // Si hay 7 fechas, el max es índice 6

            // Estética
            setDrawGridLines(false) // Quita las líneas verticales para limpiar
            labelRotationAngle = -45f // Rotar para que quepan

            // Calcular cantidad de etiquetas
            // Si son pocos días (ej. 7), forzamos mostrar todos. Si son 30, dejamos que la librería decida.
            if (masterDateList.size < 10) {
                setLabelCount(masterDateList.size, true) // true = forzar cantidad exacta
            } else {
                setLabelCount(5, false) // Mostrar aprox 5 fechas para no saturar
            }
        }

        // 6. Listener de Clic Corregido
        binding.lineChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                e?.let { entry ->
                    val index = entry.x.toInt()
                    val sb = StringBuilder()

                    // Obtener la fecha real de la lista maestra usando el índice X
                    if (index in masterDateList.indices) {
                        val fechaReal = masterDateList[index]
                        sb.append("📅 Fecha: $fechaReal\n")

                        // Buscar valores de esa fecha específica en los mapas
                        // Esto arregla el problema: muestra el valor aunque el punto no esté exacto en el clic
                        val valorBcv = mapBcv[fechaReal]
                        val valorUsdt = mapUsdt[fechaReal]

                        if (binding.cbBcv.isChecked && valorBcv != null) {
                            sb.append("🟢 BCV: $valorBcv\n")
                        }
                        if (binding.cbUsdt.isChecked && valorUsdt != null) {
                            sb.append("🔵 USDT: $valorUsdt\n")
                        }
                    }

                    binding.txtValores.text = sb.toString()
                    binding.txtValores.setTextColor(primaryTextColor)
                    animacionCrecerTexto(binding.txtValores)
                }
            }

            override fun onNothingSelected() {
                binding.txtValores.text = getString(R.string.mensaje_toca_punto)
            }
        })

        binding.lineChart.fitScreen()
        binding.lineChart.invalidate()
    }

    // --- Función Auxiliar Nueva ---
    // Convierte la respuesta API a un Mapa simple: "05/01/2026" -> 604.94
    private fun convertirAMapa(response: HistoryModelResponse): Map<String, Float> {
        val mapa = mutableMapOf<String, Float>()
        response.history.forEach { item ->
            try {
                // Extraer solo la fecha: "05/01/2026, 01:59 AM" -> "05/01/2026"
                val fechaLimpia = item.last_update.split(",")[0].trim()
                mapa[fechaLimpia] = item.price.toFloat()
            } catch (e: Exception) {
                Log.e(TAG, "Error parseando fecha: ${item.last_update}")
            }
        }
        return mapa
    }

    private fun convertirResponseApiHistory(
        responseApiHistory: HistoryModelResponse
    ): Pair<List<String>, List<Float>> {
        val dates = mutableListOf<String>()
        val prices = mutableListOf<Float>()

        // Procesar los datos
        responseApiHistory.history.forEach { history ->
            val dateOnly = history.last_update.split(",")[0].trim()
            dates.add(dateOnly)
            prices.add(history.price.toFloat())
        }

        // Invertir orden (Antiguo -> Nuevo)
        return Pair(dates.reversed(), prices.reversed())
    }

    private fun llamarApiHistory(diasMenos: Int) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            // Configurar Retrofit
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer 2x9Qjpxl5F8CoKK6T395KA")
                        .build()
                    chain.proceed(request)
                }
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(URL_BASE)
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

                // 1. LLAMADA BCV
                val responseBcv = apiService.getDollarHistory(
                    page = "bcv",
                    monitor = "usd",
                    startDate = fechaInicial,
                    endDate = diaDeHoy
                )

                // 2. LLAMADA USDT (Binance)
                val responseUsdt = try {
                    apiService.getDollarHistory(
                        page = "binance", // O "criptodolar" según tu API
                        monitor = "binance",
                        startDate = fechaInicial,
                        endDate = diaDeHoy
                    )


                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching USDT: ${e.message}")
                    null
                }
                withContext(Dispatchers.Main) {
                    // Guardar respuesta BCV en Singleton (Legacy)
                    if (responseBcv != null) {
                        HomeFragment.ApiResponseHolder.setResponseHistoryBcv(responseBcv)
                        guardarResponseHistoryBcv(requireContext(), responseBcv)
                    }

                    // Guardar respuesta USDT en variable local
                    if (responseUsdt != null) {
                        responseUsdtCache = responseUsdt
                    }

                    binding.progressBar.visibility = View.GONE

                    // Si ambos fallaron
                    if (responseBcv == null && responseUsdt == null) {
                        Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_SHORT).show()
                    } else {
                        cargarGrafico()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error General API: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_SHORT).show()
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
        editor.putString("ResponseHistoryBcv", responseJson)
        editor.apply()
    }

    private fun animacionCrecerTexto(texto: TextView) {
        val scaleUpAnimation = ScaleAnimation(
            1f, 1.2f, 1f, 1.2f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply { duration = 300 }

        val scaleDownAnimation = ScaleAnimation(
            1.2f, 1f, 1.2f, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply { duration = 300 }

        scaleUpAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) { texto.startAnimation(scaleDownAnimation) }
            override fun onAnimationRepeat(animation: Animation) {}
        })
        texto.startAnimation(scaleUpAnimation)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}