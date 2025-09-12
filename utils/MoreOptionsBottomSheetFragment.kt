package com.carlosv.dolaraldia.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
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



        binding.optionPremium.setOnClickListener {
            findNavController().navigate(R.id.nav_pago)
            dismiss()
        }

        binding.optionNotificaciones.setOnClickListener {
            findNavController().navigate(R.id.nav_notificaciones)
            dismiss()
        }

        binding.optionAcerca.setOnClickListener {
            findNavController().navigate(R.id.nav_acerca)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}