package com.carlosv.dolaraldia.ui.notificaciones

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentDatosPerBinding
import com.carlosv.menulateral.databinding.FragmentNotificacionesBinding
import com.google.firebase.messaging.FirebaseMessaging
private var _binding: FragmentNotificacionesBinding? = null
private val binding get() = _binding ?: throw IllegalStateException("Binding is null")

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class NotificacionesFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

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
    ): View? {
        _binding = FragmentNotificacionesBinding.inflate(inflater, container, false)
        val root: View = binding.root
        verificarSwitch()
        return root
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
}
