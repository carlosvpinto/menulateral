package com.carlosv.dolaraldia.utils

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.carlosv.dolaraldia.AppPreferences
import com.carlosv.dolaraldia.ui.pago.PlanesPagoActivity
import com.carlosv.dolaraldia.ui.pago.PremiumOptionsBottomSheet
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentMoreOptionsBinding // Importa el ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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

        try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            val versionName = pInfo.versionName
            // Mostramos algo como: "v1.55 (Código: 155)"
            binding.txtVersionApp.text = "$versionName"
        } catch (e: Exception) {
            binding.txtVersionApp.text = "Versión desconocida"
        }

        // 2. BOTÓN DE VERSIÓN (Funciona como botón de "Actualizar")
        binding.optionVersionContainer.setOnClickListener {
            abrirPlayStore()
        }

        // 3. BOTÓN DE CALIFICAR
        binding.optionRate.setOnClickListener {
            abrirPlayStore()
        }

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

    // --- FUNCIÓN PARA ABRIR LA PLAY STORE ---
    private fun abrirPlayStore() {
        val appPackageName =
            requireContext().packageName // Obtiene tu paquete (com.carlosv.menulateral)
        try {
            // Intenta abrir la app de Play Store directamente
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$appPackageName")
                )
            )
        } catch (anfe: android.content.ActivityNotFoundException) {
            // Si no tiene Play Store instalada, abre el navegador web
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                )
            )
        }
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}