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
            val response = HomeFragment.ApiResponseHolder.getResponse()
            Log.d("llamarPrecioOtros", " VALOR DEL RESPONSE llamarPaginasdelfragmen $response ")
            if (response != null) {
                valorActualBcv = response.monitors.banesco?.price!!.toDouble()


                // Agregar más Paginas según sea necesario
                if ( !verificarVacio( response.monitors.amazon_gift_card?.price)) {
                    bancosList.add(
                        BancoModelAdap(
                            "amazon_gift_card",
                            response.monitors.amazon_gift_card.price.toDouble(),
                            response.monitors.amazon_gift_card.percent,
                            response.monitors.amazon_gift_card.color,
                            response.monitors.amazon_gift_card.last_update,
                            response.monitors.amazon_gift_card.percent,
                            response.monitors.amazon_gift_card.symbol,
                            response.monitors.amazon_gift_card.title,
                        )
                    )
                }
                if ( !verificarVacio( response.monitors.binance?.price)) {
                    bancosList.add(
                        BancoModelAdap(
                            "binance",
                            response.monitors.binance.price.toDouble(),
                            response.monitors.binance.percent,
                            response.monitors.binance.color,
                            response.monitors.binance.last_update,
                            response.monitors.binance.percent,
                            response.monitors.binance.symbol,
                            response.monitors.binance.title,
                        )
                    )
                }

                if ( !verificarVacio( response.monitors.cambios_ra?.price)) {
                    bancosList.add(
                        BancoModelAdap(
                            "cambios_r&a",
                            response.monitors.cambios_ra.price.toDouble(),
                            response.monitors.cambios_ra.percent,
                            response.monitors.cambios_ra.color,
                            response.monitors.cambios_ra.last_update,
                            response.monitors.cambios_ra.percent,
                            response.monitors.cambios_ra.symbol,
                            response.monitors.cambios_ra.title,
                        )
                    )

                }

                if ( !verificarVacio( response.monitors.dolartoday?.price)) {
                    bancosList.add(
                        BancoModelAdap(
                            "dolartoday",
                            response.monitors.dolartoday.price.toDouble(),
                            response.monitors.dolartoday.percent,
                            response.monitors.dolartoday.color,
                            response.monitors.dolartoday.last_update,
                            response.monitors.dolartoday.percent,
                            response.monitors.dolartoday.symbol,
                            response.monitors.dolartoday.title,
                        )
                    )
                }


                if ( !verificarVacio( response.monitors.el_dorado?.price)) {
                    bancosList.add(
                        BancoModelAdap(
                            "el_dorado",
                            response.monitors.el_dorado.price.toDouble(),
                            response.monitors.el_dorado.percent,
                            response.monitors.el_dorado.color,
                            response.monitors.el_dorado.last_update,
                            response.monitors.el_dorado.percent,
                            response.monitors.el_dorado.symbol,
                            response.monitors.el_dorado.title,
                        )
                    )
                }

                if ( !verificarVacio( response.monitors.zinli?.price)) {
                    bancosList.add(
                        BancoModelAdap(
                            "zinli",
                            response.monitors.zinli.price.toDouble(),
                            response.monitors.zinli.percent,
                            response.monitors.zinli.color,
                            response.monitors.zinli.last_update,
                            response.monitors.zinli.percent,
                            response.monitors.zinli.symbol,
                            response.monitors.zinli.title,
                        )
                    )
                }

                if ( !verificarVacio( response.monitors.mkambio?.price)) {
                    bancosList.add(
                        BancoModelAdap(
                            "mkambio",
                            response.monitors.mkambio.price.toDouble(),
                            response.monitors.mkambio.percent,
                            response.monitors.mkambio.color,
                            response.monitors.mkambio.last_update,
                            response.monitors.mkambio.percent,
                            response.monitors.mkambio.symbol,
                            response.monitors.mkambio.title,
                        )
                    )
                }

                if ( !verificarVacio( response.monitors.paypal?.price)) {
                    bancosList.add(
                        BancoModelAdap(
                            "paypal",
                            response.monitors.paypal.price.toDouble(),
                            response.monitors.paypal.percent,
                            response.monitors.paypal.color,
                            response.monitors.paypal.last_update,
                            response.monitors.paypal.percent,
                            response.monitors.paypal.symbol,
                            response.monitors.paypal.title,
                        )
                    )
                }

                if ( !verificarVacio( response.monitors.monitor_dolar_venezuela?.price)) {
                    bancosList.add(
                        BancoModelAdap(
                            "monitor_dolar_venezuela",
                            response.monitors.monitor_dolar_venezuela.price.toDouble(),
                            response.monitors.monitor_dolar_venezuela.percent,
                            response.monitors.monitor_dolar_venezuela.color,
                            response.monitors.monitor_dolar_venezuela.last_update,
                            response.monitors.monitor_dolar_venezuela.percent,
                            response.monitors.monitor_dolar_venezuela.symbol,
                            response.monitors.monitor_dolar_venezuela.title,
                        )
                    )
                }


                if ( !verificarVacio( response.monitors.petro?.price)) {
                    bancosList.add(
                        BancoModelAdap(
                            "petro",
                            response.monitors.petro.price.toDouble(),
                            response.monitors.petro.percent,
                            response.monitors.petro.color,
                            response.monitors.petro.last_update,
                            response.monitors.petro.percent,
                            response.monitors.petro.symbol,
                            response.monitors.petro.title,
                        )
                    )
                }

                if ( !verificarVacio( response.monitors.skrill?.price)) {
                    bancosList.add(
                        BancoModelAdap(
                            "skrill",
                            response.monitors.skrill.price.toDouble(),
                            response.monitors.skrill.percent,
                            response.monitors.skrill.color,
                            response.monitors.skrill.last_update,
                            response.monitors.skrill.percent,
                            response.monitors.skrill.symbol,
                            response.monitors.skrill.title,
                        )
                    )
                }
                if ( !verificarVacio( response.monitors.yadio?.price)) {
                    bancosList.add(
                        BancoModelAdap(
                            "yadio",
                            response.monitors.yadio.price.toDouble(),
                            response.monitors.yadio.percent,
                            response.monitors.yadio.color,
                            response.monitors.yadio.last_update,
                            response.monitors.yadio.percent,
                            response.monitors.yadio.symbol,
                            response.monitors.yadio.title,
                        )
                    )
                }

                if ( !verificarVacio( response.monitors.syklo?.price)) {
                    bancosList.add(
                        BancoModelAdap(
                            "syklo",
                            response.monitors.syklo.price.toDouble(),
                            response.monitors.syklo.percent,
                            response.monitors.syklo.color,
                            response.monitors.syklo.last_update,
                            response.monitors.syklo.percent,
                            response.monitors.syklo.symbol,
                            response.monitors.syklo.title,
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