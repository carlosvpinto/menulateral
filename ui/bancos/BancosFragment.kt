package com.carlosv.dolaraldia.ui.bancos

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.carlosv.dolaraldia.adapter.BancosAdapter
import com.carlosv.dolaraldia.ui.home.HomeFragment
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentBancosBinding

//import com.carlosv.dolaraldia.databinding.FragmentSlideshowBinding

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
     //   val fabBoton = (activity as MainActivity).fabBoton

        // Ahora puedes trabajar con el FloatingActionButton
       // fabBoton.visibility= View.GONE

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
                val response = HomeFragment.ApiResponseHolder.getResponseApiBancoBCV()
                Log.d("llamarPrecioOtros", " VALOR DEL RESPONSE en bancosFragmnet $response ")
                if (response != null) {
                   // valorActualBcv = response.monitors.bnc?.price?


                    if ( !verificarVacio( response.monitors.bnc?.price.toString())){
                        bancosList.add(
                            BancoModelAdap(
                                response.monitors.bnc.title,
                                response.monitors.bnc?.price!!.toDouble(),
                                response.monitors.bnc?.percent.toString(),
                                response.monitors.bnc?.color!!,
                                response.monitors.bnc?.last_update!!,
                                response.monitors.bnc?.percent.toString(),
                                response.monitors.bnc?.symbol!!,
                                response.monitors.bnc?.title!!,


                                )
                        )
                    }

                    if ( !verificarVacio( response.monitors.bancamiga?.price.toString())) {
                        bancosList.add(
                            BancoModelAdap(
                                response.monitors.bancamiga.title,
                                response.monitors.bancamiga?.price!!.toDouble(),
                                response.monitors.bancamiga?.percent.toString(),
                                response.monitors.bancamiga?.color!!,
                                response.monitors.bancamiga?.last_update!!,
                                response.monitors.bancamiga?.percent.toString(),
                                response.monitors.bancamiga?.symbol!!,
                                response.monitors.bancamiga?.title!!,

                            )
                        )
                    }
                    if ( !verificarVacio( response.monitors.bdv.price.toString())) {
                        bancosList.add(
                            BancoModelAdap(
                                response.monitors.bdv.title,
                                response.monitors.bdv.price,
                                response.monitors.bdv.percent.toString(),
                                response.monitors.bdv.color,
                                response.monitors.bdv.last_update,
                                response.monitors.bdv.percent.toString(),
                                response.monitors.bdv.symbol,
                                response.monitors.bdv.title,

                            )
                        )
                    }


                    if ( !verificarVacio( response.monitors.activo.price.toString())) {
                        bancosList.add(
                            BancoModelAdap(
                                response.monitors.activo.title,
                                response.monitors.activo.price,
                                response.monitors.activo.percent.toString(),
                                response.monitors.activo.color,
                                response.monitors.activo.last_update,
                                response.monitors.activo.percent.toString(),
                                response.monitors.activo.symbol,
                                response.monitors.activo.title,

                            )
                        )
                    }
                    if ( !verificarVacio( response.monitors.banplus.price.toString())) {
                        bancosList.add(
                            BancoModelAdap(
                                response.monitors.banplus.title,
                                response.monitors.banplus.price,
                                response.monitors.banplus.percent.toString(),
                                response.monitors.banplus.color,
                                response.monitors.banplus.last_update,
                                response.monitors.banplus.percent.toString(),
                                response.monitors.banplus.symbol,
                                response.monitors.banplus.title,

                            )
                        )
                    }

                    if ( !verificarVacio( response.monitors.bvc.price.toString())) {
                        bancosList.add(
                            BancoModelAdap(
                                response.monitors.bvc.title,
                                response.monitors.bvc.price,
                                response.monitors.bvc.percent.toString(),
                                response.monitors.bvc.color,
                                response.monitors.bvc.last_update,
                                response.monitors.bvc.percent.toString(),
                                response.monitors.bvc.symbol,
                                response.monitors.bvc.title,
                            )
                        )
                    }

                    if ( !verificarVacio( response.monitors.exterior.price.toString())) {
                        bancosList.add(
                            BancoModelAdap(
                                response.monitors.exterior.title,
                                response.monitors.exterior.price,
                                response.monitors.exterior.percent.toString(),
                                response.monitors.exterior.color,
                                response.monitors.exterior.last_update,
                                response.monitors.exterior.percent.toString(),
                                response.monitors.exterior.symbol,
                                response.monitors.exterior.title,
                            )
                        )
                    }
                    if ( !verificarVacio( response.monitors.mercantil_banco.price.toString())) {
                        bancosList.add(
                            BancoModelAdap(
                                response.monitors.mercantil_banco.title,
                                response.monitors.mercantil_banco.price,
                                response.monitors.mercantil_banco.percent.toString(),
                                response.monitors.mercantil_banco.color,
                                response.monitors.mercantil_banco.last_update,
                                response.monitors.mercantil_banco.percent.toString(),
                                response.monitors.mercantil_banco.symbol,
                                response.monitors.mercantil_banco.title,
                            )
                        )
                    }
                    if ( !verificarVacio( response.monitors.provincial.price.toString())) {
                        bancosList.add(
                            BancoModelAdap(
                                response.monitors.provincial.title,
                                response.monitors.provincial.price,
                                response.monitors.provincial.percent.toString(),
                                response.monitors.provincial.color,
                                response.monitors.provincial.last_update,
                                response.monitors.provincial.percent.toString(),
                                response.monitors.provincial.symbol,
                                response.monitors.provincial.title,
                            )
                        )
                    }
                }
                Log.d("llamarPrecioOtros", " ERROR DE RESPONSE adapter: $adapter ")
                    // Inicializar el adaptador si aún no se ha hecho
               //     if (adapter == null) {
                        adapter = BancosAdapter(this@BancosFragment, ArrayList())
                        binding.recyclerOtrasBancos.adapter = adapter
                  //  }

                    // Actualizar los datos del adaptador
                    adapter?.updatePrecioBancos(bancosList)


            } catch (e: Exception) {
                    Log.d("llamarPrecioOtros", " ERROR DE RESPONSE $e ")
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
            vacio= false
        }
        return vacio
    }


}