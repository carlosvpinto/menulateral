package com.carlosv.dolaraldia.ui.notificaciones

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.carlosv.dolaraldia.adapter.NotificationsAdapter
import com.carlosv.dolaraldia.utils.roomDB.NotificationsViewModel
import com.carlosv.dolaraldia.utils.roomDB.NotificationsViewModelFactory
import com.carlosv.menulateral.databinding.FragmentGuardarNotificationsBinding

import androidx.fragment.app.viewModels // CAMBIO: Import necesario
import com.carlosv.dolaraldia.MyApplication // CAMBIO: Importa tu clase Application


class FragmentGuardarNotifications : Fragment() {

    private var _binding: FragmentGuardarNotificationsBinding? = null
    private val binding get() = _binding!!

    // CAMBIO: Inyección del ViewModel usando la Factory
    private val viewModel: NotificationsViewModel by viewModels {
        // Obtenemos el repositorio desde nuestra clase MyApplication
        NotificationsViewModelFactory((requireActivity().application as MyApplication).repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuardarNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // CAMBIO: Descomentamos y activamos el observador
        viewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            if (notifications.isNullOrEmpty()) {
                // Si la lista está vacía, mostramos el mensaje
                binding.recyclerNotifications.visibility = View.GONE
               // binding.textEmptyState.visibility = View.VISIBLE
            } else {
                // Si hay notificaciones, mostramos la lista
                binding.recyclerNotifications.visibility = View.VISIBLE
              //  binding.textEmptyState.visibility = View.GONE

                // Creamos y asignamos el adapter a nuestro RecyclerView
                // No es necesario ordenar aquí si el DAO ya lo hace.
                binding.recyclerNotifications.adapter = NotificationsAdapter(notifications)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}