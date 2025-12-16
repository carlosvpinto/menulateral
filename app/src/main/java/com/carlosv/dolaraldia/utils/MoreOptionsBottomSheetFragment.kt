package com.carlosv.dolaraldia.utils

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.carlosv.dolaraldia.ui.pago.PlanesPagoActivity
import com.carlosv.dolaraldia.ui.pago.PremiumOptionsBottomSheet
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentMoreOptionsBinding // Importa el ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MoreOptionsBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentMoreOptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoreOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar los clics para cada opción del menú
        binding.optionHitoria.setOnClickListener {
            // Navega al fragmento de Cuentas Personales
            findNavController().navigate(R.id.nav_history)
            dismiss() // Cierra el BottomSheet después de la selección
        }
// TEMPORALMENTE ELIMINADO POR POLITICAS DE GOOGLE
//        binding.optionPremium.setOnClickListener {
//            // En lugar de navegar, mostramos nuestro nuevo BottomSheet
//            PremiumOptionsBottomSheet.newInstance()
//                .show(parentFragmentManager, PremiumOptionsBottomSheet.TAG)
//        }



        // --- ¡AQUÍ ESTÁ LA MODIFICACIÓN! ---
//        binding.optionPremium.setOnClickListener {
//            // 1. Crear un Intent para especificar qué Activity queremos abrir.
//            val intent = Intent(requireContext(), PlanesPagoActivity::class.java)
//
//            // 2. Iniciar la Activity.
//            startActivity(intent)
//
//            // 3. Cerrar el BottomSheet después de la selección.
//            dismiss()
//        }
        // --- FIN DE LA MODIFICACIÓN ---



        binding.optionAcerca.setOnClickListener {
            findNavController().navigate(R.id.nav_acerca)
            dismiss()
        }

        binding.optionNotificaciones.setOnClickListener {
            findNavController().navigate(R.id.nav_guardar_noti)
            dismiss()
        }

        binding.optionConfiguration.setOnClickListener {
            findNavController().navigate(R.id.nav_configuration)
            dismiss()
        }

        binding.btnOpcionContacto.setOnClickListener {
            findNavController().navigate(R.id.nav_contacto)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}