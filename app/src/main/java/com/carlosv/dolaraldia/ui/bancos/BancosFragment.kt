package com.carlosv.dolaraldia.ui.bancos

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.carlosv.dolaraldia.ApiService
import com.carlosv.dolaraldia.adapter.BancosAdapter
import com.carlosv.dolaraldia.model.apicontoken2.ApiModelResponseBCV
import com.carlosv.dolaraldia.ui.home.HomeFragment
import com.carlosv.dolaraldia.ui.home.HomeFragment.ApiResponseHolder
import com.carlosv.dolaraldia.utils.Constants
import com.carlosv.dolaraldia.utils.premiun.PremiumDialogManager.Companion.TAG
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentBancosBinding
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
        llamarApiPaginaBCV()
        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.d(TAG, "Swipe to refresh activado. Volviendo a cargar los datos...")
            // Cuando el usuario desliza, simplemente llamamos a nuestra función principal de carga.
            llamarApiPaginaBCV()
        }
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
                val response = getResponseBancosBCV(requireContext())
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
                Log.d("llamarPrecioOtros", " ERROR DE RESPONSE adapterrrr: $adapter ")
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

    private fun llamarApiPaginaBCV() {
        //Recupera los datos de la memoria Preference del dispositivo******************************
        try {
            val savedResponseBCV = getResponseBancosBCV(requireContext())

            //Publico en el Api Holder
            if (savedResponseBCV != null) {
                ApiResponseHolder.setResponseBCV(savedResponseBCV)

            }
        } catch (e: Exception) {

            Log.d(TAG, "llamarDolarApiNew: erre $e")
        } finally {
          //  binding.swipeRefreshLayout.isRefreshing =
           //     false // Asegura que se detenga el refresco siempre
        }
        //******************************************************************************************
        val baseUrl = Constants.URL_BASE  // URL base sin la última parte

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer 2x9Qjpxl5F8CoKK6T395KA") // Token añadido
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

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Realizar la solicitud a la API con el parámetro de consulta
                val response = apiService.getDollarBancosBcv("bcv")
                Log.d(TAG, "llamarApiPaginaBCV: RESPONSE $response")

                if (response != null) {

                    // Procesa la respuesta según tu lógica
                    withContext(Dispatchers.Main) {
                        // Actualizar la UI en el hilo principal si es necesario
                        ApiResponseHolder.setResponseBCV(response)
                        guardarResponseBancoBCV(requireContext(), response)
                        llamarBancosdelfragmen()
                        // PASO 3: Ocultamos TODOS los indicadores de carga al finalizar.
                        binding.progressBar.visibility = View.GONE
                        binding.swipeRefreshLayout.isRefreshing = false // <-- MUY IMPORTANTE
                        binding.recyclerOtrasBancos.visibility = View.VISIBLE
                    }
                } else {
                    // Manejar errores HTTP
                    Log.e("API_ERROR", "Error HTTP: del response}")
                    // PASO 3: Ocultamos TODOS los indicadores de carga al finalizar.
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false // <-- MUY IMPORTANTE
                    binding.recyclerOtrasBancos.visibility = View.VISIBLE
                    llamarBancosdelfragmen()
                }
            } catch (e: Exception) {
                // Manejo de errores generales

                Log.e("API_CALL", "Error: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    // Detener cualquier indicador de carga si es necesario

                }
            }
        }
    }


    //GUARDA EL REPONSE DEL API
    private fun guardarResponseBancoBCV(context: Context, responseBCV: ApiModelResponseBCV) {
        val gson = Gson()
        val responseJson = gson.toJson(responseBCV)

        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("dolarResponseBCV", responseJson)
        editor.apply()

    }

    // RECUPERA EL REPONSE DEL API
    private fun getResponseBancosBCV(context: Context): ApiModelResponseBCV? {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferences", AppCompatActivity.MODE_PRIVATE)
        val responseJson = sharedPreferences.getString("dolarResponseBCV", null)

        if (responseJson != null) {
            val gson = Gson()

            return gson.fromJson(responseJson, ApiModelResponseBCV::class.java)
        }

        return null // Retorna null si no se encontró la respuesta en SharedPreferences
    }







    private fun verificarVacio(precio:String?):Boolean {
        var vacio = true
        if(precio!=null){
            vacio= false
        }
        return vacio
    }


}