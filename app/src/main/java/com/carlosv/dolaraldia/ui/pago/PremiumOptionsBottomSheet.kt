package com.carlosv.dolaraldia.ui.pago


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.BottomSheetPremiumOptionsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PremiumOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPremiumOptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPremiumOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Listener para la opción "Ver estado de mi suscripción"
        binding.optionViewSubscription.setOnClickListener {
            // Navega al fragmento que muestra el estado premium
            findNavController().navigate(R.id.nav_premium_status)
            // Cierra el bottom sheet después de navegar
            dismiss()
        }

        // Listener para la opción "Obtener Premium"
        binding.optionGoToPurchase.setOnClickListener {
            // 1. Crea un Intent para lanzar tu PaymentPlansActivity.
            //    Asegúrate de que 'PaymentPlansActivity::class.java' sea el nombre correcto de tu Activity.
            val intent = Intent(requireContext(), PlanesPagoActivity::class.java)

            // 2. Inicia la nueva Activity.
            startActivity(intent)

            // 3. Cierra el bottom sheet después de lanzar la Activity.
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    // Un "companion object" para crear la instancia de forma limpia
    companion object {
        const val TAG = "PremiumOptionsBottomSheet"
        fun newInstance(): PremiumOptionsBottomSheet {
            return PremiumOptionsBottomSheet()
        }
    }
}