package com.carlosv.dolaraldia.ui.pago

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.carlosv.dolaraldia.utils.Constants
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentPaginasBinding
import com.carlosv.menulateral.databinding.FragmentPagoBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val TAG= "PAYPALPRICE"


class PagoFragment : Fragment() {

    private var _binding: FragmentPagoBinding? = null
    private var param1: String? = null
    private var param2: String? = null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentPagoBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.btnSubscribe.setOnClickListener{
            irSuscripcion( obtenerValorSuscripcion(binding.rgSubscriptions))
        }

        //borrar es temporal*********************
        binding.btnBorrar.setOnClickListener {
            borrarSubscriptionPorVida(requireContext())
            borraSubscription(requireContext())
            Toast.makeText(requireContext(), "Borrada la suscripcion", Toast.LENGTH_SHORT).show()
        }

        colocarFechaSuscripcion()

        return root
    }

    private fun colocarFechaSuscripcion() {
        if (isSubscriptionFecha(requireContext())== ""){
            binding.tcFechaSuscripcion.visibility = View.GONE
        } else{
            binding.tcFechaSuscripcion.visibility= View.VISIBLE
            binding.tcFechaSuscripcion.text = getString(R.string.fecha_vencimiento, isSubscriptionFecha(requireContext()))
            //binding.tcFechaSuscripcion.text = isSubscriptionFecha(requireContext())
        }

    }


    private fun irSuscripcion(precio:Float){
        val args = Bundle()

       // args.putFloat(Constants.ARG_PRICE, food.price)
        args.putFloat(Constants.ARG_PRICE, precio)
        Log.d(TAG, "setupTexts: Price $precio y args: $args")
        findNavController().navigate(R.id.nav_payment, args)
    }

    fun obtenerValorSuscripcion(radioGroup: RadioGroup): Float {
        return when (radioGroup.checkedRadioButtonId) {
            R.id.rbMonthly -> 1.5f // Suscripción Mensual
            R.id.rbAnnual -> 9.0f  // Suscripción Anual
            R.id.rbLifetime -> 12.0f // Suscripción de Por Vida
            else -> 0.0f // Retorna 0 si no hay ninguna opción seleccionada
        }
    }

    //Funcion Temporal para borrar la suscripcion de por vida
    // Función para guardar una suscripción de por vida
    fun borrarSubscriptionPorVida(context: Context) {

        Log.d(TAG, "saveLifetimeSubscription: Entro a sucrupcion de por vida")
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("UserSubscription", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Guardar un valor booleano indicando que es una suscripción de por vida
        editor.putBoolean("lifetime_subscription", false)
        editor.apply()
    }
    fun borraSubscription(context: Context) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("UserSubscription", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Eliminar la fecha de expiración de la suscripción
        editor.remove("subscription_expiration")
        editor.apply()
        Log.d(TAG, "borraSubscription: Borrada la SUSCRIPCION")
    }

    fun isSubscriptionFecha(context: Context): String {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("UserSubscription", Context.MODE_PRIVATE)

        // Verificar si el usuario tiene una suscripción de por vida
        val hasLifetimeSubscription = sharedPreferences.getBoolean("lifetime_subscription", false)

        if (hasLifetimeSubscription) {
            // Si tiene una suscripción de por vida, retorna false (es decir, sí tiene una suscripción activa)

            return "de por vida"
        }

        // Obtener la fecha actual
        val currentDate = Calendar.getInstance().timeInMillis

        // Obtener la fecha de expiración de la suscripción
        val subscriptionExpiration = sharedPreferences.getLong("subscription_expiration", 0)

        // Log para verificar las fechas
        Log.d(TAG, "Fecha de expiración convertida: ${convertMillisToDate(subscriptionExpiration)}")
        Log.d(TAG, "Fecha actual convertida: ${convertMillisToDate(currentDate)}")
        Log.d(TAG, "ACTIVACION DE POR VIDA: $hasLifetimeSubscription")


        Log.d(TAG, "comparacion de fecha: currentDate: $currentDate >= subscriptionExpiration: $subscriptionExpiration")
        if (subscriptionExpiration>= currentDate) {

            return convertMillisToDate(subscriptionExpiration)
        } else {

            return ""
        }

    }


    // Función auxiliar para convertir milisegundos a una fecha legible
    fun convertMillisToDate(millis: Long): String {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        return format.format(calendar.time)
    }




    companion object {

    }
}