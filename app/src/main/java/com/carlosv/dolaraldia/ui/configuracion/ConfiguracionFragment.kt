package com.carlosv.dolaraldia.ui.configuracion

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentConfigurationBinding
import com.google.android.ump.ConsentInformation
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.messaging.FirebaseMessaging

// Mantenemos estas variables de binding fuera, como las tenías
private var _binding: FragmentConfigurationBinding? = null
private val binding get() = _binding!!

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ConfiguracionFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    // 1. Mover la declaración de la variable aquí, a la clase principal del Fragment
    private lateinit var consentInformation: ConsentInformation

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
    ): View { // Cambiado a View no nulable por consistencia
        _binding = FragmentConfigurationBinding.inflate(inflater, container, false)
        return binding.root
    }

    // 2. Usar onViewCreated para toda la lógica que interactúa con las vistas.
    // Este método se llama justo después de que la vista ha sido creada.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Lógica del switch de notificaciones (como ya la tenías)
        verificarSwitch()

        // --- LÓGICA DEL BOTÓN DE PRIVACIDAD MOVIDA AQUÍ ---

        // Inicializa la instancia de ConsentInformation
        consentInformation = UserMessagingPlatform.getConsentInformation(requireContext())

        // Configura el listener del clic para el botón de privacidad
        binding.privacySettingsButton.setOnClickListener {
            Log.d("ConsentDebug", "Botón de privacidad presionado!") // Log para verificar
            showPrivacyOptionsForm()
        }
    }

    private fun verificarSwitch() {
        val sharedPreferences = requireContext().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        binding.switchNotificaciones.isChecked = sharedPreferences.getBoolean("notificationsEnabled", true)
        binding.switchNotificaciones.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                FirebaseMessaging.getInstance().subscribeToTopic("general")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("FirebaseTopic", "Suscripción exitosa")
                            saveNotificationPreference(true, sharedPreferences)
                        } else {
                            Log.d("FirebaseTopic", "Error al suscribirse")
                        }
                    }
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("general")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("FirebaseTopic", "Desuscripción exitosa")
                            saveNotificationPreference(false, sharedPreferences)
                        } else {
                            Log.d("FirebaseTopic", "Error al desuscribirse")
                        }
                    }
            }
        }
    }

    private fun saveNotificationPreference(enabled: Boolean, sharedPreferences: SharedPreferences) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("notificationsEnabled", enabled)
        editor.apply()
    }

    // 3. Mover la función showPrivacyOptionsForm aquí
    private fun showPrivacyOptionsForm() {
        Log.d("ConsentDebug", "Llamando a showPrivacyOptionsForm...")
        Toast.makeText(requireContext(), "Cargando opciones de privacidad...", Toast.LENGTH_SHORT).show()

        // Resetea el estado de consentimiento
        consentInformation.reset()
        Log.d("ConsentDebug", "Estado de consentimiento reseteado.")

        // Vuelve a cargar y mostrar el formulario
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(requireActivity()) { loadAndShowError ->
            if (loadAndShowError != null) {
                Log.e("ConsentDebug", "Error al cargar el formulario: ${loadAndShowError.message}")
                Toast.makeText(requireContext(), "No se pudieron cargar las opciones: ${loadAndShowError.message}", Toast.LENGTH_LONG).show()
            } else {
                Log.d("ConsentDebug", "El formulario se cargó (o no era necesario).")
            }
        }
    }

    // Es una buena práctica limpiar el binding para evitar fugas de memoria
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ELIMINAMOS POR COMPLETO LA CLASE ANIDADA SettingsFragment
}