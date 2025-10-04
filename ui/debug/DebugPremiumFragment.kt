package com.carlosv.dolaraldia.ui.debug

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.carlosv.dolaraldia.AppPreferences // ¡Asegúrate de que la ruta a tu AppPreferences sea correcta!
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentDebugPremiumBinding
import java.text.SimpleDateFormat
import java.util.*

class DebugPremiumFragment : Fragment() {

    private var _binding: FragmentDebugPremiumBinding? = null
    private val binding get() = _binding!!

    // Formateador de fechas para mostrar los datos de forma legible
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDebugPremiumBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Al crear la vista, lo primero es cargar y mostrar los datos actuales
        refreshDisplayedData()

        // Configura los listeners para los botones de acción
        setupActionButtons()
    }

    /**
     * Lee los datos desde AppPreferences y actualiza los TextViews en la pantalla.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun refreshDisplayedData() {
        // Lee todos los valores relevantes de AppPreferences
        val isPremiumActive = AppPreferences.isUserPremiumActive()
        val planName = AppPreferences.getPremiumPlan()
        val expirationMillis = AppPreferences.getPremiumExpirationDate()

        // Actualiza el estado
        binding.tvIsPremiumActive.text = "Estado Activo: $isPremiumActive"

        // Actualiza el nombre del plan
        binding.tvPremiumPlan.text = if (planName != null) "Plan Actual: $planName" else "Plan Actual: Ninguno"

        // Formatea y muestra la fecha de vencimiento
        val expirationText = when {
            expirationMillis == -1L -> "NUNCA (Vitalicio)"
            expirationMillis > 0L -> dateFormat.format(Date(expirationMillis))
            else -> "No establecido"
        }
        binding.tvExpirationDate.text = "Fecha de Vencimiento: $expirationText"

        // ==========> ¡AQUÍ ESTÁ EL CAMBIO! <==========
        // Usamos la nueva función para mostrar el tiempo restante.
        binding.tvExpirationMillis.text = "(${formatRemainingTime(expirationMillis)})"
    }
    /**
     * Configura los OnClickListeners para todos los botones de depuración.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun setupActionButtons() {
        binding.btnGrantMonthly.setOnClickListener {
            AppPreferences.setUserAsPremium("Mensual")
            showToastAndRefresh("Plan Mensual otorgado.")
        }

        binding.btnGrantAnnual.setOnClickListener {
            AppPreferences.setUserAsPremium("Anual")
            showToastAndRefresh("Plan Anual otorgado.")
        }

        binding.btnGrantLifetime.setOnClickListener {
            AppPreferences.setUserAsPremium("Vitalicio")
            showToastAndRefresh("Plan Vitalicio otorgado.")
        }

        binding.btnGrantTemporary.setOnClickListener {
            AppPreferences.setUserAsPremium("Recompensa",4) // Otorga 4 horas
            showToastAndRefresh("4 horas de premium temporal otorgadas.")
        }

        binding.btnClearPremium.setOnClickListener {
            AppPreferences.clearPremiumStatus()
            showToastAndRefresh("Estado premium borrado.")
        }
    }

    /**
     * Muestra un mensaje Toast y actualiza la información en pantalla.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun showToastAndRefresh(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        refreshDisplayedData()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun formatRemainingTime(expirationMillis: Long): String {
        // Casos especiales primero
        when {
            expirationMillis == -1L -> return "No expira (Vitalicio)"
            expirationMillis == 0L -> return "No establecido"
            expirationMillis <= System.currentTimeMillis() -> return "Expirado"
        }

        // 1. Calcula la duración entre el momento actual y la fecha de vencimiento.
        val duration = java.time.Duration.ofMillis(expirationMillis - System.currentTimeMillis())

        // 2. Extrae los componentes de la duración (días, horas, minutos).
        // toDaysPart(), toHoursPart(), etc., requieren API 31, así que lo haremos manualmente
        // para máxima compatibilidad.
        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60
        val seconds = duration.toSeconds() % 60 // Opcional: añadimos segundos para más detalle

        // 3. Construye el texto de forma inteligente.
        val parts = mutableListOf<String>()
        if (days > 0) parts.add("$days día${if (days > 1) "s" else ""}")
        if (hours > 0) parts.add("$hours hora${if (hours > 1) "s" else ""}")
        if (minutes > 0) parts.add("$minutes min${if (minutes > 1) "s" else ""}")

        // Si no hay días ni horas ni minutos, mostramos los segundos.
        if (parts.isEmpty() && seconds > 0) {
            parts.add("$seconds seg${if (seconds > 1) "s" else ""}")
        }

        return if (parts.isEmpty()) {
            "Expirando..."
        } else {
            // Unimos las 3 partes más significativas (ej: "1 día, 5 horas, 30 mins")
            "Faltan: ${parts.take(3).joinToString(", ")}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}