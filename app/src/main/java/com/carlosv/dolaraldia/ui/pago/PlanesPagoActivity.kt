package com.carlosv.dolaraldia.ui.pago



import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.findNavController
import com.carlosv.dolaraldia.ui.home.HomeFragment
import com.carlosv.dolaraldia.utils.Constants
import com.carlosv.menulateral.R

import com.carlosv.menulateral.databinding.ActivityPlanesPagoBinding
import java.util.Locale

class PlanesPagoActivity : AppCompatActivity() {

    // 1. Declaramos la variable para el binding.
    //    Será nuestra única referencia a todas las vistas del layout.
    private lateinit var binding: ActivityPlanesPagoBinding
    private val TAG = "PlanesPagoActivity"
    val tasaDelBCV = HomeFragment.ApiResponseHolder.getTasaVentaBcv()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPlanesPagoBinding.inflate(layoutInflater)

        setContentView(binding.root)


        ViewCompat.setOnApplyWindowInsetsListener(binding.planesPago) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // --- Botón de la barra de herramientas ---
        binding.toolbar.setNavigationOnClickListener {
            finish() // Cierra la actividad al presionar la flecha de atrás.
        }

        binding.btnSubscribe.setOnClickListener{
            val tasaDelBCV = HomeFragment.ApiResponseHolder.getTasaVentaBcv()
            val infoSuscripcion = obtenerInfoSuscripcion(binding.rgSubscriptions, tasaDelBCV)

            irSuscripcion(infoSuscripcion)

        }






    }

    private fun irSuscripcion(info: SuscripcionInfo) {
        val intent = Intent(this, DetallesPagoMActivity::class.java)

        // Añadimos los tres datos al Intent con sus respectivas claves.
        intent.putExtra(Constants.SUBSCRIPTION_PLAN_NAME, info.planName)
        intent.putExtra(Constants.PRICE_USD, info.precioEnDolares)
        intent.putExtra(Constants.PRICE_BS, info.precioEnBolivares)

        startActivity(intent)
    }

    fun obtenerValorSuscripcion(radioGroup: RadioGroup): Float {
        return when (radioGroup.checkedRadioButtonId) {
            R.id.rbMonthly -> 1.5f // Suscripción Mensual
            R.id.rbAnnual -> 9.0f  // Suscripción Anual
            R.id.rbLifetime -> 12.0f // Suscripción de Por Vida
            else -> 0.0f // Retorna 0 si no hay ninguna opción seleccionada
        }
    }

    private fun obtenerInfoSuscripcion(radioGroup: RadioGroup, tasaBcv: Double): SuscripcionInfo {
        var planName = "Ninguno"
        var precioEnDolares = 0.0

        when (radioGroup.checkedRadioButtonId) {
            R.id.rbMonthly -> {
                planName = "Mensual"
                precioEnDolares = 1.5
            }
            R.id.rbAnnual -> {
                planName = "Anual"
                precioEnDolares = 9.0
            }
            R.id.rbLifetime -> {
                planName = "Vitalicio"
                precioEnDolares = 12.0
            }
        }

        val precioEnBolivares = if (tasaBcv > 0) precioEnDolares * tasaBcv else 0.0

        return SuscripcionInfo(planName, precioEnDolares, precioEnBolivares)
    }

    data class SuscripcionInfo(
        val planName: String,
        val precioEnDolares: Double,
        val precioEnBolivares: Double
    )
}