package com.carlosv.dolaraldia.ui.notificaciones

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.carlosv.dolaraldia.adapter.NotificationsAdapter
import com.carlosv.dolaraldia.utils.roomDB.NotificationsViewModel
import com.carlosv.dolaraldia.utils.roomDB.NotificationsViewModelFactory

import androidx.fragment.app.viewModels
import com.carlosv.dolaraldia.MyApplication
import com.carlosv.dolaraldia.utils.roomDB.NotificationEntity
import com.carlosv.menulateral.databinding.FragmentGuardarNotificationsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder // Para el diálogo bonito
import java.text.SimpleDateFormat
import java.util.*

class FragmentGuardarNotifications : Fragment() {

    private var _binding: FragmentGuardarNotificationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificationsViewModel by viewModels {
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

        // 1. Configurar el botón "Borrar Todo"
        binding.btnDeleteAll.setOnClickListener {
            // Verificamos si hay algo que borrar antes de preguntar
            if (binding.recyclerNotifications.adapter?.itemCount ?: 0 > 0) {
                mostrarConfirmacionBorrarTodo()
            } else {
                Toast.makeText(requireContext(), "La lista ya está vacía", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. Observador (Tu código existente, con un pequeño ajuste para visibilidad del botón)
        viewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            if (notifications.isNullOrEmpty()) {
                binding.recyclerNotifications.visibility = View.GONE
                binding.textEmptyState.visibility = View.VISIBLE // Asegúrate de tener este ID en el XML
                binding.btnDeleteAll.isEnabled = false // Desactivamos el botón si no hay nada
            } else {
                binding.recyclerNotifications.visibility = View.VISIBLE
                binding.textEmptyState.visibility = View.GONE
                binding.btnDeleteAll.isEnabled = true // Activamos el botón

                binding.recyclerNotifications.adapter = NotificationsAdapter(
                    notifications,
                    onDeleteClick = { notification -> confirmarEliminacion(notification) },
                    onItemClick = { notification -> mostrarDetalleNotificacion(notification) }
                )
            }
        }
    }

    // FUNCIÓN 1: Mostrar diálogo con el contenido completo
    private fun mostrarDetalleNotificacion(notification: NotificationEntity) {
        val fechaFormateada = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(notification.timestamp))

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(notification.title)
            .setMessage("${notification.body}\n\nRecibido el: $fechaFormateada")
            .setPositiveButton("Cerrar") { dialog, _ ->
                dialog.dismiss()
            }
            // Opcional: Agregar botón de borrar dentro del detalle también
            .setNegativeButton("Borrar") { dialog, _ ->
                viewModel.delete(notification)
                dialog.dismiss()
            }
            .show()
    }

    // FUNCIÓN 2: Confirmar antes de borrar (Opcional, o borrar directo)
    private fun confirmarEliminacion(notification: NotificationEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar notificación")
            .setMessage("¿Estás seguro de que deseas eliminar este mensaje?")
            .setPositiveButton("Eliminar") { dialog, _ ->
                viewModel.delete(notification) // Llamada al ViewModel para borrar
                Toast.makeText(requireContext(), "Eliminado", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // --- FUNCIÓN NUEVA: Diálogo de confirmación masiva ---
    private fun mostrarConfirmacionBorrarTodo() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("¿Vaciar historial?")
            .setMessage("Esta acción eliminará todas las notificaciones guardadas y no se puede deshacer.")
            .setPositiveButton("Sí, borrar todo") { dialog, _ ->
                viewModel.deleteAll() // ¡Llamada al ViewModel!
                Toast.makeText(requireContext(), "Historial eliminado", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}