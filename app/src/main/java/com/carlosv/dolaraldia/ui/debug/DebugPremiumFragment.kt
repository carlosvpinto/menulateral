package com.carlosv.dolaraldia.ui.debug

import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.carlosv.dolaraldia.AppPreferences
import com.carlosv.dolaraldia.services.FcmApiClient
import com.carlosv.dolaraldia.services.FcmNotificationPayload
import com.carlosv.dolaraldia.utils.Constants.DEBUG_PASSWORD
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentDebugPremiumBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import com.carlosv.menulateral.BuildConfig
import java.util.*



class DebugPremiumFragment : Fragment() {

    private var _binding: FragmentDebugPremiumBinding? = null
    private val binding get() = _binding!!

    // --- ¡AÑADIDO! ---
    // Contraseña para acceder a la pantalla de depuración.


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
        showPasswordDialog()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun showPasswordDialog() {
        binding.root.visibility = View.INVISIBLE
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Ingresa la clave de acceso"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Acceso Restringido")
            .setMessage("Esta es una pantalla de depuración. Se requiere autorización.")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Verificar") { _, _ ->
                if (input.text.toString() == DEBUG_PASSWORD) {
                    Toast.makeText(requireContext(), "Acceso concedido", Toast.LENGTH_SHORT).show()
                    initializeDebugScreen()
                } else {
                    Toast.makeText(requireContext(), "Clave incorrecta", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
            .setNegativeButton("Cancelar") { _, _ ->
                parentFragmentManager.popBackStack()
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun initializeDebugScreen() {
        binding.root.visibility = View.VISIBLE
        refreshDisplayedData()
        setupActionButtons()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun refreshDisplayedData() {
        val isPremiumActive = AppPreferences.isUserPremiumActive()
        val planName = AppPreferences.getPremiumPlan()
        val expirationMillis = AppPreferences.getPremiumExpirationDate()

        binding.tvIsPremiumActive.text = "Estado Activo: $isPremiumActive"
        binding.tvPremiumPlan.text = if (planName != null) "Plan Actual: $planName" else "Plan Actual: Ninguno"
        val expirationText = when {
            expirationMillis == -1L -> "NUNCA (Vitalicio)"
            expirationMillis > 0L -> dateFormat.format(Date(expirationMillis))
            else -> "No establecido"
        }
        binding.tvExpirationDate.text = "Fecha de Vencimiento: $expirationText"
        binding.tvExpirationMillis.text = "(${formatRemainingTime(expirationMillis)})"
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setupActionButtons() {
        binding.btnSendTestNotification.setOnClickListener {
            sendRealFcmNotification()
        }
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
            AppPreferences.setUserAsPremium("Recompensa", 4)
            showToastAndRefresh("4 horas de premium temporal otorgadas.")
        }
        binding.btnClearPremium.setOnClickListener {
            AppPreferences.clearPremiumStatus()
            showToastAndRefresh("Estado premium borrado.")
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun showToastAndRefresh(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        refreshDisplayedData()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun formatRemainingTime(expirationMillis: Long): String {
        when {
            expirationMillis == -1L -> return "No expira (Vitalicio)"
            expirationMillis == 0L -> return "No establecido"
            expirationMillis <= System.currentTimeMillis() -> return "Expirado"
        }
        val duration = java.time.Duration.ofMillis(expirationMillis - System.currentTimeMillis())
        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60
        val seconds = duration.toSeconds() % 60
        val parts = mutableListOf<String>()
        if (days > 0) parts.add("$days día${if (days > 1) "s" else ""}")
        if (hours > 0) parts.add("$hours hora${if (hours > 1) "s" else ""}")
        if (minutes > 0) parts.add("$minutes min${if (minutes > 1) "s" else ""}")
        if (parts.isEmpty() && seconds > 0) parts.add("$seconds seg${if (seconds > 1) "s" else ""}")
        return if (parts.isEmpty()) "Expirando..." else "Faltan: ${parts.take(3).joinToString(", ")}"
    }

    private fun sendRealFcmNotification() {
        Toast.makeText(requireContext(), "Enviando notificación real...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FcmSender", "No se pudo obtener el token de FCM.", task.exception)
                    showResultToast("Error al obtener el token local.")
                    return@addOnCompleteListener
                }

                val currentToken = task.result
                if (BuildConfig.FCM_SERVER_KEY.isNullOrEmpty() || BuildConfig.FCM_SERVER_KEY == "null") {
                    Log.e("FcmSender", "La clave de servidor de FCM no está configurada en BuildConfig.")
                    showResultToast("Error: Clave de servidor no encontrada.")
                    return@addOnCompleteListener
                }
                val serverKey = "key=${BuildConfig.FCM_SERVER_KEY}"

                val dataPayload = mapOf(
                    "title" to "Prueba Real desde la App",
                    "body" to "¡Esto pasó por los servidores de Google!",
                    "deep_link" to "dolaraldia://fragment/bancos"
                )

                val notificationPayload = FcmNotificationPayload(to = currentToken, data = dataPayload)

                lifecycleScope.launch {
                    try {
                        val response = FcmApiClient.api.sendNotification(serverKey, notificationPayload)
                        if (response.isSuccessful) {
                            Log.d("FcmSender", "Notificación enviada con éxito a través de FCM.")
                            showResultToast("Notificación enviada con éxito.")
                        } else {
                            Log.e("FcmSender", "Error al enviar notificación: ${response.code()} ${response.errorBody()?.string()}")
                            showResultToast("Error del servidor FCM: ${response.code()}")
                        }
                    } catch (e: Exception) {
                        Log.e("FcmSender", "Excepción al enviar notificación", e)
                        showResultToast("Error de red al enviar.")
                    }
                }
            }
        }
    }

    private fun showResultToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}