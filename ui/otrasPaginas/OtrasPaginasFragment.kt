package com.carlosv.dolaraldia.ui.otrasPaginas

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.carlosv.dolaraldia.adapter.OtrasPaginasAdapter
//import com.carlosv.dolaraldia.databinding.FragmentGalleryBinding
import com.carlosv.dolaraldia.model.monedas.OtrasPaginas
import com.carlosv.menulateral.databinding.FragmentPaginasBinding


import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class OtrasPaginasFragment : Fragment() {

    private var _binding: FragmentPaginasBinding? = null
    private  var adapter: OtrasPaginasAdapter? = null
    private var otrasPaginas = ArrayList<OtrasPaginas>()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val galleryViewModel =
            ViewModelProvider(this).get(OtrasPaginasViewModel::class.java)

        _binding = FragmentPaginasBinding.inflate(inflater, container, false)
        val root: View = binding.root
        getDollarRates()
        val linearLayoutManager = LinearLayoutManager(requireContext())
        binding.recyclerOtrasPaginas.layoutManager = linearLayoutManager

//        val textView: TextView = binding.txtInicio
//        galleryViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
        return root
    }

    private fun getDollarRates() {

        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://pydolarvenezuela-api.vercel.app/api/v1/dollar/")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Manejar error
                Log.d("RESPUESTA", " RESPUESTA FALLIDA ")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return

                println(body)
                listarResponseOtrasPag(body)
                showListaPaginas(body)
                Log.d("RESPUESTA", " RESPUESTA body $body")
                // Aquí ya tienes la respuesta JSON
                // Puedes pasarla a processPagoMovilResponse()
                // para convertirla a objetos Kotlin
            }
        })

    }

    private fun listarResponseOtrasPag(jsonResponse: String): List<OtrasPaginas> {
        val listaOtrasPaginas = ArrayList<OtrasPaginas>()

        val json = JSONObject(jsonResponse)
        val monitors = json.getJSONObject("monitors")

        val amazonGiftCard = monitors.getJSONObject("amazon_gift_card")
        listaOtrasPaginas.add(
            OtrasPaginas(
                amazonGiftCard.getString("last_update"),
                amazonGiftCard.getDouble("price"),
                amazonGiftCard.getDouble("price_old"),
                amazonGiftCard.getString("title"),
                amazonGiftCard.getString("type")
            )
        )

        val bcv = monitors.getJSONObject("bcv")
        listaOtrasPaginas.add(
            OtrasPaginas(
                bcv.getString("last_update"),
                bcv.getDouble("price"),
                bcv.getDouble("price_old"),
                bcv.getString("title"),
                bcv.getString("type")
            )
        )


        // Y así sucesivamente para cada objeto JSON en monitors
        val binance = monitors.getJSONObject("binance")
        listaOtrasPaginas.add(
            OtrasPaginas(
                binance.getString("last_update"),
                binance.getDouble("price"),
                binance.getDouble("price_old"),
                binance.getString("title"),
                binance.getString("type")
            )
        )
        val cripto_dolar = monitors.getJSONObject("cripto_dolar")
        listaOtrasPaginas.add(
            OtrasPaginas(
                cripto_dolar.getString("last_update"),
                cripto_dolar.getDouble("price"),
                cripto_dolar.getDouble("price_old"),
                cripto_dolar.getString("title"),
                cripto_dolar.getString("type")
            )
        )

        val dolar_today = monitors.getJSONObject("dolar_today")
        listaOtrasPaginas.add(
            OtrasPaginas(
                dolar_today.getString("last_update"),
                dolar_today.getDouble("price"),
                dolar_today.getDouble("price_old"),
                dolar_today.getString("title"),
                dolar_today.getString("type")
            )
        )

        val enparalelovzla = monitors.getJSONObject("enparalelovzla")
        listaOtrasPaginas.add(
            OtrasPaginas(
                enparalelovzla.getString("last_update"),
                enparalelovzla.getDouble("price"),
                enparalelovzla.getDouble("price_old"),
                enparalelovzla.getString("title"),
                enparalelovzla.getString("type")
            )
        )

        val paypal = monitors.getJSONObject("paypal")
        listaOtrasPaginas.add(
            OtrasPaginas(
                paypal.getString("last_update"),
                paypal.getDouble("price"),
                paypal.getDouble("price_old"),
                paypal.getString("title"),
                paypal.getString("type")
            )
        )

        val skrill = monitors.getJSONObject("skrill")
        listaOtrasPaginas.add(
            OtrasPaginas(
                skrill.getString("last_update"),
                skrill.getDouble("price"),
                skrill.getDouble("price_old"),
                skrill.getString("title"),
                skrill.getString("type")
            )
        )

        val uphold = monitors.getJSONObject("uphold")
        listaOtrasPaginas.add(
            OtrasPaginas(
                uphold.getString("last_update"),
                uphold.getDouble("price"),
                uphold.getDouble("price_old"),
                uphold.getString("title"),
                uphold.getString("type")
            )
        )
      //  Log.d("RESPUESTA", " pagos PAGOSSS $pagos")

        return listaOtrasPaginas
    }


    private fun showListaPaginas(jsonResponse: String) {
        val listapaginas = listarResponseOtrasPag(jsonResponse)

        runOnUiThread {
            adapter?.updatePrecioPaginas(listapaginas)
            Log.d("RESPUESTA", " DENTRO DEL updatePrecioPaginas $listapaginas ")
            adapter = OtrasPaginasAdapter(this@OtrasPaginasFragment, ArrayList(listapaginas))
            binding.recyclerOtrasPaginas.adapter = adapter
        }

        val listapaginasString = listapaginas.joinToString("\n") {
            "${it.title}: ${it.price} (Actualizado: ${it.last_update})"
        }

        Log.d("RESPUESTA", " Arreglo $listapaginasString ")
        //binding.txtRespuesta.text = pagosString.toString()
    }

    private fun runOnUiThread(action: () -> Unit) {
        Handler(Looper.getMainLooper()).post(action)
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}