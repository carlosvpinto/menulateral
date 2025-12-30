package com.carlosv.dolaraldia.ui.debug

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.carlosv.dolaraldia.AppPreferences
import com.carlosv.dolaraldia.services.FcmApiClient
import com.carlosv.dolaraldia.services.FcmNotificationPayload
import com.carlosv.menulateral.databinding.FragmentDebugPremiumBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import com.carlosv.menulateral.BuildConfig
import java.util.*
import java.util.concurrent.TimeUnit // Importante para la compatibilidad

class DebugPremiumFragment : Fragment() {

    private var _binding: FragmentDebugPremiumBinding? = null
    private val binding get() = _binding!!

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDebugPremiumBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Quitamos @RequiresApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showPasswordDialog()
    }

    // Quitamos @RequiresApi
    private fun showPasswordDialog() {
        // Ocultamos la vista mientras se valida
        binding.root.visibility = View.INVISIBLE

        val input = EditText(requireContext()).apply {
            // CAMBIO: Permitimos texto y números para la contraseña
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Ingresa la clave de acceso"
        }

        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(50, 0, 50, 0)
        input.layoutParams = params
        container.addView(input)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Acceso Restringido")
            .setMessage("Esta es una pantalla de depuración. Se requiere autorización.")
            .setView(container)
            .setCancelable(false)
            .setPositiveButton("Verificar") { _, _ ->
                val claveIngresada = input.text.toString().trim()

                if (claveIngresada.isNotEmpty()) {
                    verificarClaveEnFirestore(claveIngresada)
                } else {
                    Toast.makeText(requireContext(), "La clave no puede estar vacía", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
            .setNegativeButton("Cancelar") { _, _ ->
                parentFragmentManager.popBackStack()
            }
            .show()
    }

    private fun verificarClaveEnFirestore(claveIngresada: String) {
        Toast.makeText(requireContext(), "Verificando credenciales...", Toast.LENGTH_SHORT).show()

        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        // Colección: "configuracion_app" -> Documento: "acceso_admin"
        val docRef = db.collection("configuracion_app").document("acceso_admin")

        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Campo: "clave_secreta"
                    val claveReal = document.getString("clave_secreta")

                    if (claveReal == claveIngresada) {
                        Toast.makeText(requireContext(), "Acceso concedido", Toast.LENGTH_SHORT).show()

                        // Hacemos visible la vista
                        binding.root.visibility = View.VISIBLE

                        // Iniciamos lógica
                        initializeDebugScreen()
                    } else {
                        Toast.makeText(requireContext(), "Clave incorrecta", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                } else {
                    Log.e("Debug", "No existe el documento de configuración en Firestore")
                    Toast.makeText(requireContext(), "Error de configuración", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Debug", "Error de conexión: ${e.message}")
                Toast.makeText(requireContext(), "Error de conexión. Intenta más tarde.", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
    }

    // Quitamos @RequiresApi
    private fun initializeDebugScreen() {
        binding.root.visibility = View.VISIBLE
        refreshDisplayedData()
        setupActionButtons()
    }

    // Quitamos @RequiresApi
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

    // Quitamos @RequiresApi
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

    // Quitamos @RequiresApi
    private fun showToastAndRefresh(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        refreshDisplayedData()
    }

    // --- FUNCIÓN REESCRITA PARA COMPATIBILIDAD TOTAL ---
    // Ya no usa java.time (API 31+), usa matemáticas simples compatibles con todos los Android.
    private fun formatRemainingTime(expirationMillis: Long): String {
        val currentTime = System.currentTimeMillis()

        when {
            expirationMillis == -1L -> return "No expira (Vitalicio)"
            expirationMillis == 0L -> return "No establecido"
            expirationMillis <= currentTime -> return "Expirado"
        }

        val diff = expirationMillis - currentTime

        // Matemáticas estándar (funcionan en cualquier versión de Android)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60

        val parts = mutableListOf<String>()
        if (days > 0) parts.add("$days día${if (days > 1) "s" else ""}")
        if (hours > 0) parts.add("$hours hora${if (hours > 1) "s" else ""}")
        if (minutes > 0) parts.add("$minutes min${if (minutes > 1) "s" else ""}")

        // Si queda muy poco tiempo, mostramos segundos
        if (parts.isEmpty() && seconds > 0) parts.add("$seconds seg")

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
                    Log.e("FcmSender", "Clave FCM no configurada.")
                    showResultToast("Error: Clave de servidor no encontrada.")
                    return@addOnCompleteListener
                }

                // Asegúrate que tu BuildConfig tenga el campo FCM_SERVER_KEY
                val serverKey = "key=${BuildConfig.FCM_SERVER_KEY}"

                val dataPayload = mapOf(
                    "title" to "Prueba Real desde la App",
                    "body" to "¡Esto pasó por los servidores de Google!",
                    "ir_a" to "pagomovil" // Cambiado a 'ir_a' para coincidir con tu lógica
                )

                val notificationPayload = FcmNotificationPayload(to = currentToken, data = dataPayload)

                lifecycleScope.launch {
                    try {
                        val response = FcmApiClient.api.sendNotification(serverKey, notificationPayload)
                        if (response.isSuccessful) {
                            Log.d("FcmSender", "Notificación enviada con éxito.")
                            showResultToast("Notificación enviada con éxito.")
                        } else {
                            Log.e("FcmSender", "Error: ${response.code()} ${response.errorBody()?.string()}")
                            showResultToast("Error del servidor FCM: ${response.code()}")
                        }
                    } catch (e: Exception) {
                        Log.e("FcmSender", "Excepción al enviar", e)
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