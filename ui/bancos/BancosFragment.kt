package com.carlosv.dolaraldia.ui.bancos

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
import com.carlosv.dolaraldia.ui.home.HomeFragment
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentBancosBinding

//import com.carlosv.dolaraldia.databinding.FragmentSlideshowBinding

import kotlinx.coroutines.Dispatchers
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
    val bancosList = mutableListOf<BancoModelAdap>()
    private var bancos = ArrayList<BancoModelAdap>()

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
        val linearLayoutManager = LinearLayoutManager(requireContext())
        binding.recyclerOtrasBancos.layoutManager = linearLayoutManager
        val fabBoton = (activity as MainActivity).fabBoton

        // Ahora puedes trabajar con el FloatingActionButton
        fabBoton.visibility= View.GONE

        //llamarBancos()
        llamarBancosdelfragmen()

        binding.editTexFiltroC.addTextChangedListener { userfilter ->
            val bancosFiltrados = bancosList.filter { banco->
                banco.nombre?.lowercase()?.contains(userfilter.toString().lowercase())== true}
                adapter?.updatePrecioBancos(bancosFiltrados)
        }

        // Aplicar la animación
        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.appear_from_top)
        root.startAnimation(animation)

        //***********************


        return root

    }


        //USA LA INTERFACE PARA TRAER EL VALOR DEL RESPONSE

    fun llamarBancosdelfragmen() {


            try {
                val response = HomeFragment.ApiResponseHolder.getResponse()
                Log.d("RESPUESTA", " VALOR DEL RESPONSE $response ")
                if (response != null) {
                    valorActualBcv = response.monitors.banesco?.price?.toDouble()


                    if ( !verificarVacio( response.monitors.banesco?.price)){
                        bancosList.add(
                            BancoModelAdap(
                                "Banesco",
                                response.monitors.banesco?.price!!.toDouble(),
                                response.monitors.banesco?.percent!!,
                                response.monitors.banesco?.color!!,
                                response.monitors.banesco?.last_update!!,
                                response.monitors.banesco?.percent!!,
                                response.monitors.banesco?.symbol!!,
                                response.monitors.banesco?.title!!,

                                )
                        )
                    }

                    // Agregar más bancos según sea necesario
                    if ( !verificarVacio( response.monitors.mercantil?.price)) {
                        bancosList.add(
                            BancoModelAdap(
                                "Mercantil",
                                response.monitors.mercantil?.price!!.toDouble(),
                                response.monitors.mercantil?.percent!!,
                                response.monitors.mercantil?.color!!,
                                response.monitors.mercantil?.last_update!!,
                                response.monitors.mercantil?.percent!!,
                                response.monitors.mercantil?.symbol!!,
                                response.monitors.mercantil?.title!!,
                            )
                        )
                    }
                    if ( !verificarVacio( response.monitors.bancamiga.price)) {
                        bancosList.add(
                            BancoModelAdap(
                                "Bancamiga",
                                response.monitors.bancamiga.price.toDouble(),
                                response.monitors.bancamiga.percent,
                                response.monitors.bancamiga.color,
                                response.monitors.bancamiga.last_update,
                                response.monitors.bancamiga.percent,
                                response.monitors.bancamiga.symbol,
                                response.monitors.bancamiga.title,
                            )
                        )
                    }


                    if ( !verificarVacio( response.monitors.banco_de_venezuela?.price)) {
                        bancosList.add(
                            BancoModelAdap(
                                "Banco de Venezuela",
                                response.monitors.banco_de_venezuela?.price!!.toDouble(),
                                response.monitors.banco_de_venezuela?.percent!!,
                                response.monitors.banco_de_venezuela?.color!!,
                                response.monitors.banco_de_venezuela?.last_update!!,
                                response.monitors.banco_de_venezuela?.percent!!,
                                response.monitors.banco_de_venezuela?.symbol!!,
                                response.monitors.banco_de_venezuela?.title!!,
                            )
                        )
                    }

                    if ( !verificarVacio( response.monitors.bbva_provincial?.price)) {
                        bancosList.add(
                            BancoModelAdap(
                                "Banco Provincial",
                                response.monitors.bbva_provincial?.price!!.toDouble(),
                                response.monitors.bbva_provincial?.percent!!,
                                response.monitors.bbva_provincial?.color!!,
                                response.monitors.bbva_provincial?.last_update!!,
                                response.monitors.bbva_provincial?.percent!!,
                                response.monitors.bbva_provincial?.symbol!!,
                                response.monitors.bbva_provincial?.title!!,
                            )
                        )
                    }

                    if ( !verificarVacio( response.monitors.bnc?.price)) {
                        bancosList.add(
                            BancoModelAdap(
                                "BNC",
                                response.monitors.bnc?.price!!.toDouble(),
                                response.monitors.bnc?.percent!!,
                                response.monitors.bnc?.color!!,
                                response.monitors.bnc?.last_update!!,
                                response.monitors.bnc?.percent!!,
                                response.monitors.bnc?.symbol!!,
                                response.monitors.bnc?.title!!,
                            )
                        )
                    }

                    if ( !verificarVacio( response.monitors.remesas_zoom.price)) {
                        bancosList.add(
                            BancoModelAdap(
                                "remesas_zoom",
                                response.monitors.remesas_zoom.price.toDouble(),
                                response.monitors.remesas_zoom.percent,
                                response.monitors.remesas_zoom.color,
                                response.monitors.remesas_zoom.last_update,
                                response.monitors.remesas_zoom.percent,
                                response.monitors.remesas_zoom.symbol,
                                response.monitors.remesas_zoom.title,
                            )
                        )
                    }
                    if ( !verificarVacio( response.monitors.bcv?.price)) {
                        bancosList.add(
                            BancoModelAdap(
                                "BCV",
                                response.monitors.bcv?.price!!.toDouble(),
                                response.monitors.bcv?.percent!!,
                                response.monitors.bcv?.color!!,
                                response.monitors.bcv?.last_update!!,
                                response.monitors.bcv?.percent!!,
                                response.monitors.bcv?.symbol!!,
                                response.monitors.bcv?.title!!,
                            )
                        )
                    }
                        if ( !verificarVacio( response.monitors.italcambio.price)) {
                            bancosList.add(
                                BancoModelAdap(
                                    "italcambio",
                                    response.monitors.italcambio.price.toDouble(),
                                    response.monitors.italcambio.percent,
                                    response.monitors.italcambio.color,
                                    response.monitors.italcambio.last_update,
                                    response.monitors.italcambio.percent,
                                    response.monitors.italcambio.symbol,
                                    response.monitors.italcambio.title,
                                )
                            )

                        }
                }

                    // Inicializar el adaptador si aún no se ha hecho
                    if (adapter == null) {
                        adapter = BancosAdapter(this@BancosFragment, ArrayList())
                        binding.recyclerOtrasBancos.adapter = adapter
                    }

                    // Actualizar los datos del adaptador
                    adapter?.updatePrecioBancos(bancosList)


            } catch (e: Exception) {
                    Log.d("RESPUESTA", " ERROR DE RESPONSE $e ")
                    Toast.makeText(
                        requireContext(),
                        "No Actualizo dolar BCV Revise Conexion $e",
                        Toast.LENGTH_LONG
                    ).show()


                println("Error: ${e.message}")
            }



    }

    private fun verificarVacio(precio:String?):Boolean {
        var vacio = true
        if(precio!=null){
            Log.d("RESPUESTA", "verificarVacio: precio $precio  ")
            vacio= false
        }
        return vacio
    }


}