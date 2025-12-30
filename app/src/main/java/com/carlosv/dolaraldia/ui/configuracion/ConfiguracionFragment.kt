package com.carlosv.dolaraldia.ui.configuracion

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.carlosv.dolaraldia.AppPreferences
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentConfigurationBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

        binding.promoButton.setOnClickListener {
            mostrarDialogoInputCodigo()
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


    // Codigo para canjear cupones*********************************
    // --- CONTROL DE UI (Carga / Espera) ---
    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.promoButton.isEnabled = false // Desactivamos para evitar doble clic
            binding.promoButton.text = "Verificando..."
        } else {
            binding.progressBar.visibility = View.GONE
            binding.promoButton.isEnabled = true
            binding.promoButton.text = "Canjear Código Promocional"
        }
    }

    // --- DIÁLOGO PARA PEDIR EL CÓDIGO ---
    private fun mostrarDialogoInputCodigo() {
        val input = EditText(requireContext())
        input.hint = "Ingresa tu código (ej: PROMO2025)"
        input.inputType = InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS // Teclado en mayúsculas

        val container = FrameLayout(requireContext())
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(50, 20, 50, 0)
        input.layoutParams = params
        container.addView(input)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Canjear Cupón 🎟️")
            .setView(container)
            .setCancelable(false) // El usuario debe cancelar o aceptar explícitamente
            .setPositiveButton("Validar") { dialog, _ ->
                val codigo = input.text.toString().trim().uppercase()
                if (codigo.isNotEmpty()) {
                    // AQUÍ EMPIEZA EL PROCESO
                    validarYQuemarcodigo(codigo)
                } else {
                    Toast.makeText(requireContext(), "El código no puede estar vacío", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // --- LÓGICA DE FIRESTORE ---
    private fun validarYQuemarcodigo(codigo: String) {
        // 1. Activamos la UI de carga
        setLoadingState(true)

        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val docRef = db.collection("cupones_promo").document(codigo)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)

            // Validaciones dentro de la transacción (Seguridad)
            if (!snapshot.exists()) {
                throw com.google.firebase.firestore.FirebaseFirestoreException(
                    "Código inválido",
                    com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED
                )
            }

            val yaUsado = snapshot.getBoolean("usado") ?: true
            if (yaUsado) {
                throw com.google.firebase.firestore.FirebaseFirestoreException(
                    "Código ya utilizado",
                    com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED
                )
            }

            // Obtener datos
            val diasRegalo = snapshot.getLong("dias") ?: 0

            // Quemar el código
            transaction.update(docRef, "usado", true)
            transaction.update(docRef, "fecha_uso", java.util.Date())
            // Opcional: transaction.update(docRef, "usuario_id", android_id)

            diasRegalo // Retornamos los días
        }.addOnSuccessListener { diasGanados ->

            // --- ÉXITO ---
            setLoadingState(false) // Apagamos carga

            aplicarPremium(diasGanados.toInt()) // Tu función para guardar en SharedPreferences

            // Mostrar mensaje de éxito y LUEGO navegar
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("¡Éxito! 🎉")
                .setMessage("Código canjeado correctamente. Tienes $diasGanados días de Premium.")
                .setPositiveButton("Ir al Inicio") { dialog, _ ->
                    dialog.dismiss()
                    navegarAlHome() // <--- NAVEGACIÓN AQUÍ
                }
                .setCancelable(false)
                .show()

        }.addOnFailureListener { e ->

            // --- FALLO ---
            setLoadingState(false) // Apagamos carga, el botón vuelve a estar disponible

            val mensaje = when (e.message) {
                "Código inválido" -> "El código ingresado no existe."
                "Código ya utilizado" -> "Este cupón ya fue canjeado."
                else -> "Error de conexión. Intenta de nuevo."
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Error")
                .setMessage(mensaje)
                .setPositiveButton("Reintentar", null)
                .show()

            // NO NAVEGAMOS, nos quedamos aquí para que intente de nuevo.
        }
    }

    private fun navegarAlHome() {
        try {
            // Asegúrate de usar el ID correcto de tu Home en el navigation graph
            // Normalmente es R.id.nav_home o puedes usar popBackStack si vienes de ahí
            findNavController().navigate(R.id.nav_home)

            // O si quieres limpiar todo y reiniciar el home:
            // findNavController().popBackStack(R.id.nav_home, false)
        } catch (e: Exception) {
            Log.e("Promo", "Error navegando: ${e.message}")
        }
    }

    private fun aplicarPremium(dias: Int) {
        val horas = dias * 24
        AppPreferences.setUserAsPremium("Cupón Promocional", horas)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}