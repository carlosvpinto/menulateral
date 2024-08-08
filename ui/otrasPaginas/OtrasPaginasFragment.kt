package com.carlosv.dolaraldia.ui.otrasPaginas

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.carlosv.dolaraldia.ApiService
import com.carlosv.dolaraldia.MainActivity
import com.carlosv.dolaraldia.adapter.BancosAdapter
import com.carlosv.dolaraldia.adapter.OtrasPaginasAdapter
//import com.carlosv.dolaraldia.databinding.FragmentGalleryBinding
import com.carlosv.dolaraldia.model.monedas.OtrasPaginas
import com.carlosv.dolaraldia.ui.bancos.BancoModelAdap
import com.carlosv.dolaraldia.ui.home.HomeFragment
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentPaginasBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OtrasPaginasFragment : Fragment() {

    private var _binding: FragmentPaginasBinding? = null

    private var otrasPaginas = ArrayList<OtrasPaginas>()
    private var valorActualBcv: Double? = 0.0
    private  var adapter: OtrasPaginasAdapter? = null
    val bancosList = mutableListOf<BancoModelAdap>()
    private var bancos = ArrayList<BancoModelAdap>()

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

       // val fabBoton = (activity as MainActivity).fabBoton

        // Ahora puedes trabajar con el FloatingActionButton
    //    fabBoton.visibility= View.GONE

        // Aquí puedes añadir la animación de aparición hacia abajo lentamente
        // Aplicar la animación
        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.appear_from_top)
        root.startAnimation(animation)

        //***********************

        llamarPaginasdelfragmen()
        val linearLayoutManager = LinearLayoutManager(requireContext())
        binding.recyclerOtrasPaginas.layoutManager = linearLayoutManager

        binding.editTexFiltroC.addTextChangedListener { userfilter ->
            val bancosFiltrados = bancosList.filter { banco->
                banco.nombre?.lowercase()?.contains(userfilter.toString().lowercase())== true}
            adapter?.updatePrecioBancos(bancosFiltrados)
        }
        return root
    }

    //USA LA INTERFACE PARA TRAER EL VALOR DEL RESPONSE

    fun llamarPaginasdelfragmen() {


        try {
            val response = HomeFragment.ApiResponseHolder.getResponseApiNew()
            Log.d("llamarPrecioOtros", " VALOR DEL RESPONSE llamarPaginasdelfragmen $response ")
            if (response != null) {



                // Agregar más Paginas según sea necesario
                if ( !verificarVacio( response.monitors.amazon_gift_card?.price.toString())) {
                    bancosList.add(
                        BancoModelAdap(
                            "amazon_gift_card",
                            response.monitors.amazon_gift_card.price.toDouble(),
                            response.monitors.amazon_gift_card.percent.toString(),
                            response.monitors.amazon_gift_card.color,
                            response.monitors.amazon_gift_card.last_update,
                            response.monitors.amazon_gift_card.percent.toString(),
                            response.monitors.amazon_gift_card.symbol,
                            response.monitors.amazon_gift_card.title,
                        )
                    )
                }
                if ( !verificarVacio( response.monitors.binance?.price.toString())) {
                    bancosList.add(
                        BancoModelAdap(
                            "binance",
                            response.monitors.binance.price.toDouble(),
                            response.monitors.binance.percent.toString(),
                            response.monitors.binance.color,
                            response.monitors.binance.last_update,
                            response.monitors.binance.percent.toString(),
                            response.monitors.binance.symbol,
                            response.monitors.binance.title,
                        )
                    )
                }

                if ( !verificarVacio( response.monitors.dolar_today.price.toString())) {
                    bancosList.add(
                        BancoModelAdap(
                            "dolartoday",
                            response.monitors.dolar_today.price.toDouble(),
                            response.monitors.dolar_today.percent.toString(),
                            response.monitors.dolar_today.color,
                            response.monitors.dolar_today.last_update,
                            response.monitors.dolar_today.percent.toString(),
                            response.monitors.dolar_today.symbol,
                            response.monitors.dolar_today.title,
                        )
                    )
                }


                if ( !verificarVacio( response.monitors.cripto_dolar?.price.toString())) {
                    bancosList.add(
                        BancoModelAdap(
                            "el_dorado",
                            response.monitors.cripto_dolar.price.toDouble(),
                            response.monitors.cripto_dolar.percent.toString(),
                            response.monitors.cripto_dolar.color,
                            response.monitors.cripto_dolar.last_update,
                            response.monitors.cripto_dolar.percent.toString(),
                            response.monitors.cripto_dolar.symbol,
                            response.monitors.cripto_dolar.title,
                        )
                    )
                }



                if ( !verificarVacio( response.monitors.paypal?.price.toString())) {
                    bancosList.add(
                        BancoModelAdap(
                            "paypal",
                            response.monitors.paypal.price.toDouble(),
                            response.monitors.paypal.percent.toString(),
                            response.monitors.paypal.color,
                            response.monitors.paypal.last_update,
                            response.monitors.paypal.percent.toString(),
                            response.monitors.paypal.symbol,
                            response.monitors.paypal.title,
                        )
                    )
                }

                if ( !verificarVacio( response.monitors.skrill?.price.toString())) {
                    bancosList.add(
                        BancoModelAdap(
                            "monitor_dolar_venezuela",
                            response.monitors.skrill.price.toDouble(),
                            response.monitors.skrill.percent.toString(),
                            response.monitors.skrill.color,
                            response.monitors.skrill.last_update,
                            response.monitors.skrill.percent.toString(),
                            response.monitors.skrill.symbol,
                            response.monitors.skrill.title,
                        )
                    )
                }


                if ( !verificarVacio( response.monitors.dolar_today?.price.toString())) {
                    bancosList.add(
                        BancoModelAdap(
                            "petro",
                            response.monitors.dolar_today.price.toDouble(),
                            response.monitors.dolar_today.percent.toString(),
                            response.monitors.dolar_today.color,
                            response.monitors.dolar_today.last_update,
                            response.monitors.dolar_today.percent.toString(),
                            response.monitors.dolar_today.symbol,
                            response.monitors.dolar_today.title,
                        )
                    )
                }

                if ( !verificarVacio( response.monitors.skrill?.price.toString())) {
                    bancosList.add(
                        BancoModelAdap(
                            "skrill",
                            response.monitors.skrill.price.toDouble(),
                            response.monitors.skrill.percent.toString(),
                            response.monitors.skrill.color,
                            response.monitors.skrill.last_update,
                            response.monitors.skrill.percent.toString(),
                            response.monitors.skrill.symbol,
                            response.monitors.skrill.title,
                        )
                    )
                }


                if ( !verificarVacio( response.monitors.uphold?.price.toString())) {
                    bancosList.add(
                        BancoModelAdap(
                            "syklo",
                            response.monitors.uphold.price.toDouble(),
                            response.monitors.uphold.percent.toString(),
                            response.monitors.uphold.color,
                            response.monitors.uphold.last_update,
                            response.monitors.uphold.percent.toString(),
                            response.monitors.uphold.symbol,
                            response.monitors.uphold.title,
                        )
                    )
                }


            }

                    // Inicializar el adaptador si aún no se ha hecho
                    if (adapter == null) {
                        adapter = OtrasPaginasAdapter(this@OtrasPaginasFragment, ArrayList())
                        binding.recyclerOtrasPaginas.adapter = adapter
                    }

                    // Actualizar los datos del adaptador
                    adapter?.updatePrecioBancos(bancosList)


                // Actualizar los datos del adaptador
                adapter?.updatePrecioBancos(bancosList)


        } catch (e: Exception) {
            Log.d("llamarPrecioOtros", " ERROR DE RESPONSE $e ")
            Toast.makeText(
                requireContext(),
                "No ACTUALIZO PRECIO DE PAGINAS $e",
                Toast.LENGTH_LONG
            ).show()


            println("Error: ${e.message}")
        }



    }

    private fun verificarVacio(precio:String?):Boolean {
        var vacio = true
        if(precio!=null){
            vacio= false
        }
        return vacio
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}