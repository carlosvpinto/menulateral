package com.carlosv.dolaraldia.ui.contacto

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import com.carlosv.menulateral.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ContactBottomSheetFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Asegúrate de tener el XML 'layout_bottom_sheet_contacto' que creamos antes
        return inflater.inflate(R.layout.layout_bottom_sheet_contacto, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnEnviar = view.findViewById<Button>(R.id.btnEnviarEmail)
        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroupMotivo)

        btnEnviar.setOnClickListener {
            val selectedId = radioGroup.checkedRadioButtonId
            val motivo = if (selectedId != -1) {
                view.findViewById<RadioButton>(selectedId).text.toString()
            } else {
                "Consulta General"
            }
            enviarEmail(motivo)
            dismiss() // Cierra el sheet
        }
    }

    private fun enviarEmail(motivo: String) {
        val emailDestino = "desoftsis@gmail.com" // PON TU EMAIL AQUÍ

        val datosDispositivo = """
            
            --------------------------------
            Datos del dispositivo:
            App: Dolar al Día
            Modelo: ${Build.MANUFACTURER} ${Build.MODEL}
            Android: ${Build.VERSION.RELEASE}
            --------------------------------
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(emailDestino))
            putExtra(Intent.EXTRA_SUBJECT, "Dolar al Dia: $motivo")
            putExtra(Intent.EXTRA_TEXT, "Hola equipo, \n\n[Escribe tu mensaje aquí]\n\n$datosDispositivo")
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No tienes app de correo instalada", Toast.LENGTH_SHORT).show()
        }
    }
}