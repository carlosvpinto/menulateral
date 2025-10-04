package com.carlosv.dolaraldia.ui.pago



import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.carlosv.dolaraldia.AppPreferences // ¡Asegúrate de que la ruta sea correcta!
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentPremiumStatusBinding
import java.text.SimpleDateFormat
import java.util.*

class PremiumStatusFragment : Fragment() {

    private var _binding: FragmentPremiumStatusBinding? = null
    private val binding get() = _binding!!

    // Formateador de fechas para mostrar los datos de forma legible
    private val dateFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPremiumStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // La función principal que lee los datos y actualiza la UI
        populatePremiumStatus()


        binding.btnBecomePremium.setOnClickListener {
            val intent = Intent(requireContext(), PlanesPagoActivity::class.java)
            startActivity(intent)

        }
    }

    /**
     * Lee el estado premium desde AppPreferences y actualiza la interfaz de usuario.
     */
    private fun populatePremiumStatus() {
        if (AppPreferences.isUserPremiumActive()) {
            // CASO 1: El usuario tiene una suscripción activa
            showActiveStatus()
        } else {
            // CASO 2: El usuario no tiene una suscripción activa
            showInactiveStatus()
        }
    }

    private fun showActiveStatus() {
        val planName = AppPreferences.getPremiumPlan() ?: "Desconocido"
        val expirationMillis = AppPreferences.getPremiumExpirationDate()

        // Configura la UI para el estado ACTIVO
        binding.ivStatusIcon.setImageResource(R.drawable.check_circle)
        binding.ivStatusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green))
        binding.tvStatusValue.text = "Activo"
        binding.tvStatusValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))

        binding.tvPlanValue.text = planName

        // Muestra la fecha de vencimiento
        val expirationText = if (expirationMillis == -1L) "Nunca" else dateFormat.format(Date(expirationMillis))
        binding.tvExpirationValue.text = expirationText

        // ==========> ¡AQUÍ ESTÁ EL CAMBIO! <==========
        // Muestra el tiempo restante y asegúrate de que sea visible
        binding.tvTimeRemainingValue.text = formatRemainingTime(expirationMillis)
        binding.tvTimeRemainingValue.visibility = View.VISIBLE

        // Oculta el llamado a la acción para comprar
        binding.layoutCtaPremium.visibility = View.GONE
    }

    private fun showInactiveStatus() {
        val expirationMillis = AppPreferences.getPremiumExpirationDate()
        var timeRemainingText = "" // Variable para el texto de tiempo restante

        if (expirationMillis > 0L) {
            // La suscripción ha EXPIRADO
            binding.ivStatusIcon.setImageResource(R.drawable.ic_warning)
            binding.ivStatusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.orange_500))
            binding.tvStatusValue.text = "Expirado"
            binding.tvStatusValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange_500))
            binding.tvExpirationValue.text = dateFormat.format(Date(expirationMillis))
            timeRemainingText = formatRemainingTime(expirationMillis) // Esto devolverá "(Expirado)"
        } else {
            // NUNCA ha sido premium
            binding.ivStatusIcon.setImageResource(R.drawable.ic_info)
            binding.ivStatusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.grey))
            binding.tvStatusValue.text = "No Premium"
            binding.tvStatusValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey))
            binding.tvExpirationValue.text = "N/A"
            // No hay tiempo restante que mostrar, así que el texto se queda vacío
        }

        binding.tvPlanValue.text = "Ninguno"

        // ==========> ¡AQUÍ ESTÁ EL CAMBIO! <==========
        // Muestra u oculta el texto de tiempo restante según corresponda
        if (timeRemainingText.isNotEmpty()) {
            binding.tvTimeRemainingValue.text = timeRemainingText
            binding.tvTimeRemainingValue.visibility = View.VISIBLE
        } else {
            binding.tvTimeRemainingValue.visibility = View.GONE
        }

        // Muestra el llamado a la acción para comprar
        binding.layoutCtaPremium.visibility = View.VISIBLE
    }

    private fun formatRemainingTime(expirationMillis: Long): String {
        // Casos especiales primero
        when {
            expirationMillis == -1L -> return "(No expira)"
            expirationMillis == 0L -> return "" // No mostrar nada si no está establecido
            expirationMillis <= System.currentTimeMillis() -> return "(Expirado)"
        }

        // Calcula la duración entre el momento actual y la fecha de vencimiento.
        val duration = java.time.Duration.ofMillis(expirationMillis - System.currentTimeMillis())

        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60

        // Construye el texto
        val parts = mutableListOf<String>()
        if (days > 0) parts.add("$days día${if (days > 1) "s" else ""}")
        if (hours > 0) parts.add("$hours hora${if (hours > 1) "s" else ""}")
        if (minutes > 0) parts.add("$minutes min${if (minutes > 1) "s" else ""}")

        return if (parts.isEmpty()) {
            "(Menos de un minuto)"
        } else {
            "(Faltan: ${parts.joinToString(", ")})"
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}