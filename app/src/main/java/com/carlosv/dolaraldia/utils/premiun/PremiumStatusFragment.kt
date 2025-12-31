package com.carlosv.dolaraldia.utils.premiun

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.carlosv.dolaraldia.AppPreferences
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentPremiumStatusDosBinding
import com.google.android.gms.tasks.Tasks
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import java.text.SimpleDateFormat
import java.util.*

class PremiumStatusFragment : Fragment() {

    private var _binding: FragmentPremiumStatusDosBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPremiumStatusDosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Cargar estado inicial de la UI
        actualizarEstadoVisual()

        // 2. Listener del Botón
        binding.btnRedeem.setOnClickListener {
            val codigo = binding.etPromoCode.text.toString().trim().uppercase()

            if (codigo.isEmpty()) {
                binding.inputLayoutCode.error = "Escribe un código"
            } else {
                binding.inputLayoutCode.error = null // Limpiar error
                ocultarTeclado()
                validarYQuemarcodigo(codigo)
            }
        }
    }

    // --- ACTUALIZAR UI SEGÚN ESTADO ---
    private fun actualizarEstadoVisual() {
        val isPremium = AppPreferences.isUserPremiumActive()

        if (isPremium) {
            // DISEÑO PREMIUM (Dorado/Verde)
            binding.cardStatus.setCardBackgroundColor(Color.parseColor("#E8F5E9")) // Verde muy claro
            binding.imgStatusIcon.setImageResource(R.drawable.premiun) // Tu icono de diamante
            binding.imgStatusIcon.setColorFilter(Color.parseColor("#2E7D32")) // Verde oscuro

            binding.txtStatusTitle.text = "¡Eres Premium!"
            binding.txtStatusTitle.setTextColor(Color.parseColor("#1B5E20"))

            binding.txtStatusSubtitle.text = "Disfrutando Dólar al Día sin interrupciones."

            // Fecha de vencimiento
            val expiraMillis = AppPreferences.getPremiumExpirationDate()
            val formatoFecha = SimpleDateFormat("dd 'de' MMMM, yyyy - hh:mm a", Locale.getDefault())
            val fechaTexto = formatoFecha.format(Date(expiraMillis))

            binding.txtExpiration.text = "Vence: $fechaTexto"
            binding.txtExpiration.visibility = View.VISIBLE

        } else {
            // DISEÑO GRATIS (Gris)
            binding.cardStatus.setCardBackgroundColor(Color.parseColor("#F5F5F5"))
            binding.imgStatusIcon.setImageResource(R.drawable.ic_lock) // Icono candado abierto o publicidad
            binding.imgStatusIcon.setColorFilter(Color.parseColor("#757575"))

            binding.txtStatusTitle.text = "Versión Gratuita"
            binding.txtStatusTitle.setTextColor(Color.parseColor("#212121"))

            binding.txtStatusSubtitle.text = "Viendo anuncios para mantener el servicio."

            binding.txtExpiration.visibility = View.GONE
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnRedeem.text = "" // Ocultamos texto
            binding.btnRedeem.isEnabled = false
            binding.inputLayoutCode.isEnabled = false
        } else {
            binding.progressBar.visibility = View.GONE
            binding.btnRedeem.text = "Canjear Código"
            binding.btnRedeem.isEnabled = true
            binding.inputLayoutCode.isEnabled = true
        }
    }

    private fun validarYQuemarcodigo(codigo: String) {
        setLoadingState(true)

        val deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"

        // 1. Preparamos las tareas (se lanzan al mismo tiempo)
        val taskInstallId = FirebaseInstallations.getInstance().id
        val taskToken = FirebaseMessaging.getInstance().token

        // 2. Esperamos a que AMBAS terminen (paralelismo)
        Tasks.whenAllComplete(taskInstallId, taskToken).addOnCompleteListener {
            // No importa si fallan o tienen éxito, el código continúa aquí

            // Recuperamos resultados (o ponemos valores por defecto si fallaron)
            val installId = if (taskInstallId.isSuccessful) taskInstallId.result else "Error_ID"
            val token = if (taskToken.isSuccessful) taskToken.result else "Error_Token"

            // 3. Ejecutamos la transacción
            ejecutarTransaccion(codigo, deviceModel, installId, token)
        }
    }

    // Se agregó el parámetro "token: String" al final
    private fun ejecutarTransaccion(codigo: String, device: String, installId: String, token: String) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val docRef = db.collection("cupones_promo").document(codigo)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)

            if (!snapshot.exists()) {
                throw FirebaseFirestoreException("INV", FirebaseFirestoreException.Code.ABORTED)
            }
            if (snapshot.getBoolean("usado") == true) {
                throw FirebaseFirestoreException("USED", FirebaseFirestoreException.Code.ABORTED)
            }

            val dias = snapshot.getLong("dias") ?: 0

            // Quemar código
            transaction.update(docRef, "usado", true)
            transaction.update(docRef, "fecha_uso", Date())
            transaction.update(docRef, "usado_por_device", device)
            transaction.update(docRef, "usado_por_id", installId)

            // Guardar el token
            transaction.update(docRef, "usado_por_token", token)

            dias
        }.addOnSuccessListener { dias ->
            setLoadingState(false)

            // Guardar en AppPreferences
            val horas = dias * 24
            AppPreferences.setUserAsPremium("Cupón Promo", horas.toInt())

            // Actualizar UI visualmente
            actualizarEstadoVisual()
            binding.etPromoCode.text?.clear()

            mostrarDialogoExito(dias.toInt())

        }.addOnFailureListener { e ->
            setLoadingState(false)
            val msj = when (e.message) {
                "INV" -> "El código no existe."
                "USED" -> "Este código ya fue utilizado."
                else -> "Error de conexión. Verifica tu internet."
            }
            Toast.makeText(requireContext(), msj, Toast.LENGTH_LONG).show()
        }
    }

    private fun mostrarDialogoExito(dias: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("¡Felicidades! 💎")
            .setMessage("Has canjeado tu cupón exitosamente.\n\nDisfruta de $dias DÍAS sin publicidad.")
            .setPositiveButton("¡Gracias!") { d, _ -> d.dismiss() }
            .show()
    }

    private fun ocultarTeclado() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}