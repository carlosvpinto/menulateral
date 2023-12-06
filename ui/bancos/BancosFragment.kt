package com.carlosv.dolaraldia.ui.bancos

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.carlosv.dolaraldia.ApiService
import com.carlosv.dolaraldia.adapter.BancosAdapter
import com.carlosv.dolaraldia.adapter.OtrasPaginasAdapter
import com.carlosv.menulateral.databinding.FragmentBancosBinding

//import com.carlosv.dolaraldia.databinding.FragmentSlideshowBinding

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BancosFragment : Fragment() {

    private var _binding: FragmentBancosBinding? = null
    private var valorActualBcv: Double? = 0.0
    private  var adapter: BancosAdapter? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val historyViewModel =
            ViewModelProvider(this).get(InstitucionesViewModel::class.java)



        _binding = FragmentBancosBinding.inflate(inflater, container, false)
        val root: View = binding.root

        llamarBancos()

        //val textView: TextView = binding.textSlideshow
//        historyViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
        return root

    }

    fun llamarBancos() {

        val bancosList = mutableListOf<BancoModelAdap>()

        lifecycleScope.launch(Dispatchers.IO) {


            val url =
                "https://pydolarvenezuela-api.vercel.app/api/v1/dollar/page?page=exchangemonitor"
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
                val response = apiService.getBancos(url)
                Log.d("RESPUESTA", " VALOR DEL RESPONSE $response ")
                if (response != null) {
                    valorActualBcv = response.monitors.banesco.price.toDouble()


                    bancosList.add(
                        BancoModelAdap(
                            "Banesco",
                            response.monitors.banesco.price.toDouble(),
                            response.monitors.banesco.change,
                            response.monitors.banesco.color,
                            response.monitors.banesco.last_update,
                            response.monitors.banesco.percent,
                            response.monitors.banesco.symbol,
                            response.monitors.banesco.title,

                            )
                    )
                    // Agregar más bancos según sea necesario

                    bancosList.add(
                        BancoModelAdap(
                            "Mercantil",
                            response.monitors.mercantil.price.toDouble(),
                            response.monitors.mercantil.change,
                            response.monitors.mercantil.color,
                            response.monitors.mercantil.last_update,
                            response.monitors.mercantil.percent,
                            response.monitors.mercantil.symbol,
                            response.monitors.mercantil.title,
                        )
                    )

                    bancosList.add(
                        BancoModelAdap(
                            "Bancamiga",
                            response.monitors.bancamiga.price.toDouble(),
                            response.monitors.bancamiga.change,
                            response.monitors.bancamiga.color,
                            response.monitors.bancamiga.last_update,
                            response.monitors.bancamiga.percent,
                            response.monitors.bancamiga.symbol,
                            response.monitors.bancamiga.title,
                        )
                    )

                    bancosList.add(
                        BancoModelAdap(
                            "Banco de Venezuela",
                            response.monitors.banco_de_venezuela.price.toDouble(),
                            response.monitors.banco_de_venezuela.change,
                            response.monitors.banco_de_venezuela.color,
                            response.monitors.banco_de_venezuela.last_update,
                            response.monitors.banco_de_venezuela.percent,
                            response.monitors.banco_de_venezuela.symbol,
                            response.monitors.banco_de_venezuela.title,
                        )
                    )

                    bancosList.add(
                        BancoModelAdap(
                            "Banco Provincial",
                            response.monitors.bbva_provincial.price.toDouble(),
                            response.monitors.bbva_provincial.change,
                            response.monitors.bbva_provincial.color,
                            response.monitors.bbva_provincial.last_update,
                            response.monitors.bbva_provincial.percent,
                            response.monitors.bbva_provincial.symbol,
                            response.monitors.bbva_provincial.title,
                        )
                    )

                    bancosList.add(
                        BancoModelAdap(
                            "BNC",
                            response.monitors.bnc.price.toDouble(),
                            response.monitors.bnc.change,
                            response.monitors.bnc.color,
                            response.monitors.bnc.last_update,
                            response.monitors.bnc.percent,
                            response.monitors.bnc.symbol,
                            response.monitors.bnc.title,
                        )
                    )

                    bancosList.add(
                        BancoModelAdap(
                            "remesas_zoom",
                            response.monitors.remesas_zoom.price.toDouble(),
                            response.monitors.remesas_zoom.change,
                            response.monitors.remesas_zoom.color,
                            response.monitors.remesas_zoom.last_update,
                            response.monitors.remesas_zoom.percent,
                            response.monitors.remesas_zoom.symbol,
                            response.monitors.remesas_zoom.title,
                        )
                    )
                    bancosList.add(
                        BancoModelAdap(
                            "zinli",
                            response.monitors.zinli.price.toDouble(),
                            response.monitors.zinli.change,
                            response.monitors.zinli.color,
                            response.monitors.zinli.last_update,
                            response.monitors.zinli.percent,
                            response.monitors.zinli.symbol,
                            response.monitors.zinli.title,
                        )
                    )

                    bancosList.add(
                        BancoModelAdap(
                            "italcambio",
                            response.monitors.italcambio.price.toDouble(),
                            response.monitors.italcambio.change,
                            response.monitors.italcambio.color,
                            response.monitors.italcambio.last_update,
                            response.monitors.italcambio.percent,
                            response.monitors.italcambio.symbol,
                            response.monitors.italcambio.title,
                        )
                    )
                    bancosList.add(
                        BancoModelAdap(
                            "banco_de_venezuela",
                            response.monitors.banco_de_venezuela.price.toDouble(),
                            response.monitors.banco_de_venezuela.change,
                            response.monitors.banco_de_venezuela.color,
                            response.monitors.banco_de_venezuela.last_update,
                            response.monitors.banco_de_venezuela.percent,
                            response.monitors.banco_de_venezuela.symbol,
                            response.monitors.banco_de_venezuela.title,
                        )
                    )


                }
                runOnUiThread {
                    // Inicializar el adaptador si aún no se ha hecho
                    if (adapter == null) {
                        adapter = BancosAdapter(this@BancosFragment, ArrayList())
                        binding.recyclerOtrasBancos.adapter = adapter
                    }

                    // Actualizar los datos del adaptador
                    adapter?.updatePrecioBancos(bancosList)
                }

//                runOnUiThread {
//                    adapter?.updatePrecioBancos(bancosList)
//
//                    adapter = BancosAdapter(this@BancosFragment, ArrayList(bancosList))
//
//                    binding.recyclerOtrasBancos.adapter = adapter
//                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.d("RESPUESTA", " ERROR DE RESPONSE $e ")
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
    private fun runOnUiThread(action: () -> Unit) {
        Handler(Looper.getMainLooper()).post(action)
    }


}