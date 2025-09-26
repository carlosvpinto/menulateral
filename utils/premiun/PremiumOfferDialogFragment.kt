package com.carlosv.dolaraldia.utils.premiun

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment

import com.carlosv.dolaraldia.ui.pago.DetallesPagoMActivity
import com.carlosv.menulateral.databinding.DialogPremiumOfferBinding

class PremiumOfferDialogFragment : DialogFragment() {

    private var _binding: DialogPremiumOfferBinding? = null
    private val binding get() = _binding!!

    companion object {
        // TAG para mostrar el diálogo de forma segura.
        const val TAG = "PremiumOfferDialog"

        // Método factory para crear instancias del fragmento.
        fun newInstance(): PremiumOfferDialogFragment {
            return PremiumOfferDialogFragment()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPremiumOfferBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar el click del botón para obtener premium
        binding.buttonGetPremium.setOnClickListener {
            // Lanza la nueva actividad con los detalles del pago
            val intent = Intent(requireContext(), DetallesPagoMActivity::class.java)
            startActivity(intent)
            dismiss() // Cierra este diálogo
        }

        // Configurar el click del botón "Más tarde"
        binding.buttonLater.setOnClickListener {
            dismiss() // Simplemente cierra el diálogo
        }
    }

    override fun onStart() {
        super.onStart()
        // Verificamos que el diálogo y su ventana no sean nulos.
        dialog?.window?.let { window ->
            // Establecemos el ancho para que coincida con el padre (la pantalla)
            // y la altura para que se ajuste al contenido.
            window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            // Hacemos que el fondo del diálogo sea transparente para que nuestros
            // bordes redondeados del XML sean visibles.
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Evita memory leaks
    }
}