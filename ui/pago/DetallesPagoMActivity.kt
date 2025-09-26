package com.carlosv.dolaraldia.ui.pago

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.carlosv.dolaraldia.utils.Constants
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.ActivityDetallesPagoBinding
import java.util.Locale


class DetallesPagoMActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetallesPagoBinding
    private var montoEnBolivares: Double = 0.0


    private var planSeleccionado: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // "Inflamos" el layout y lo preparamos para su uso.
        binding = ActivityDetallesPagoBinding.inflate(layoutInflater)
        // Establecemos la vista raíz del binding como el contenido de la actividad.
        setContentView(binding.root)

        // Llamamos a una función para configurar todos los listeners de los botones.
        setupClickListeners()

        recibirYMostrarDatos()

    }

    private fun recibirYMostrarDatos() {
        // Recibimos los datos del Intent
        planSeleccionado = intent.getStringExtra(Constants.SUBSCRIPTION_PLAN_NAME)
        val precioEnDolares = intent.getDoubleExtra(Constants.PRICE_USD, 0.0)
        montoEnBolivares = intent.getDoubleExtra(Constants.PRICE_BS, 0.0)

        Log.d("DetallesPago", "Plan: $planSeleccionado, USD: $precioEnDolares, VES: $montoEnBolivares")

        val montoTextView: TextView = binding.textBankMonto
        displayFormattedAmount(montoTextView, montoEnBolivares, precioEnDolares)
    }

    /**
     * Configura los OnClickListeners para todos los elementos interactivos.
     */
    private fun setupClickListeners() {

        binding.imageCopyBank.setOnClickListener {
            val bankText = Constants.BANK_CODE
            copyToClipboard("Banco", bankText)
        }

        binding.imageCopyRif.setOnClickListener {
            val rifText = binding.textBankRif.text.toString()
            copyToClipboard("RIF", rifText)
        }

        binding.imageCopyTlf.setOnClickListener {
            val phoneText = binding.textBankTlf.text.toString()
            copyToClipboard("Teléfono", phoneText)
        }

        binding.imageCopyMonto.setOnClickListener {
            // Formateamos el precio en bolívares para copiarlo sin separadores de miles.
            val montoParaCopiar = String.format(Locale.US, "%.2f", montoEnBolivares)
            copyToClipboard("Monto", montoParaCopiar)
        }

        // --- Botón para Copiar Todos los Datos ---

        binding.layoutCopyAll.setOnClickListener {
            // Verificamos que el monto sea válido antes de intentar copiar.
            if (montoEnBolivares <= 0.0) {
                Toast.makeText(this, "No hay un monto válido para copiar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1. FORMATEAR EL MONTO PARA PAGO MÓVIL
            //    - Locale.US asegura que el separador decimal sea un punto (.).
            //    - "%.2f" asegura que siempre haya dos decimales.
            val montoFormateadoParaCopiar = String.format(Locale.US, "%.2f", montoEnBolivares)

            // 2. CONSTRUIR EL TEXTO FINAL USANDO LAS CONSTANTES
            //    - Unimos las constantes con el monto, separando cada una con un salto de línea (\n).
            val clipboardText = "${Constants.BANK_CODE}\n${Constants.RIF}\n${Constants.PHONE}\n$montoFormateadoParaCopiar"

            // 3. COPIAR AL PORTAPAPELES Y NOTIFICAR
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Datos de Pago Móvil", clipboardText)
            clipboard.setPrimaryClip(clip)

            // Damos una confirmación clara al usuario.
            Toast.makeText(this, "Datos de Pago Móvil copiados al portapapeles", Toast.LENGTH_LONG).show()
        }


        // --- ¡AQUÍ ESTÁ LA MODIFICACIÓN! ---
        binding.buttonPaid.setOnClickListener {
            // Verificamos que tengamos un monto válido antes de continuar.
            if (montoEnBolivares > 0.0) {
                // 1. Crear el Intent para iniciar MercantilSearchActivity.
                val intent = Intent(this, MercantilSearchActivity::class.java)

                // 2. Añadir el monto como un "extra".
                //    Lo convertimos a String para que coincida con lo que el campo de texto espera.
                //    Usamos Locale.US para asegurar que el decimal sea un punto.
                val montoComoString = String.format(Locale.US, "%.2f", montoEnBolivares)
                intent.putExtra(Constants.EXTRA_AMOUNT_TO_PAY, montoComoString)

                // 3. Iniciar la nueva actividad.
                startActivity(intent)

                // 4. (Opcional) Si quieres que esta pantalla se cierre después
                //    de ir a la siguiente, puedes llamar a finish().
                // finish()

            } else {
                Toast.makeText(this, "Error: El monto a pagar no es válido.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonPaid.setOnClickListener {
            // Verificamos que tengamos un monto y un plan válidos.
            if (montoEnBolivares > 0.0 && !planSeleccionado.isNullOrEmpty()) {
                // Creamos el Intent para la siguiente pantalla.
                val searchIntent = Intent(this, MercantilSearchActivity::class.java)

                // Empaquetamos los datos que necesita la siguiente actividad.
                searchIntent.putExtra(Constants.SUBSCRIPTION_PLAN_NAME, planSeleccionado)
                searchIntent.putExtra(Constants.PRICE_BS, montoEnBolivares)

                // Iniciamos la actividad.
                startActivity(searchIntent)

                // Opcional: cierra esta pantalla.
                // finish()
            } else {
                Toast.makeText(this, "Error: No se pudieron obtener los datos del plan.", Toast.LENGTH_SHORT).show()
            }
        }


        // --- Botón de la barra de herramientas ---
        binding.toolbar.setNavigationOnClickListener {
            finish() // Cierra la actividad al presionar la flecha de atrás.
        }
    }

    private fun displayFormattedAmount(textView: TextView, montoBs: Double, montoUsd: Double) {
        // Formateamos los números a strings con 2 decimales.
        // Usamos comas para miles en Bolívares para la visualización.
        val formattedBs = String.format(Locale.GERMAN, "%,.2f Bs.", montoBs) // Locale.GERMAN usa punto como separador de miles y coma para decimales
        val formattedUsd = String.format(Locale.US, "$%.2f", montoUsd) // Locale.US usa comas y puntos

        // Creamos el string completo que se mostrará.
        val fullText = "$formattedBs / $formattedUsd"

        // Creamos un SpannableString, que es un string al que podemos aplicar estilos.
        val spannableString = SpannableString(fullText)

        // --- Aplicar Estilos ---

        // 1. Estilo para el precio referencial en USD (más pequeño y de color gris).
        val startIndexUsd = fullText.indexOf(formattedUsd)
        if (startIndexUsd != -1) {
            val endIndexUsd = startIndexUsd + formattedUsd.length

            // Hacemos el texto de los dólares un 80% del tamaño normal.
            spannableString.setSpan(
                RelativeSizeSpan(0.8f),
                startIndexUsd,
                endIndexUsd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Cambiamos el color del texto de los dólares a nuestro color secundario.
            val secondaryColor = ContextCompat.getColor(this, R.color.text_secondary_light)
            spannableString.setSpan(
                ForegroundColorSpan(secondaryColor),
                startIndexUsd,
                endIndexUsd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // Asignamos el SpannableString a nuestro TextView.
        textView.text = spannableString
    }


    /**
     * Función de ayuda para copiar un texto al portapapeles y mostrar una notificación.
     * @param label Una etiqueta para el dato copiado (uso interno del sistema).
     * @param text El texto que el usuario quiere copiar.
     */
    private fun copyToClipboard(label: String, text: String) {
        // Obtenemos el servicio de portapapeles del sistema.
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        // Creamos el objeto ClipData que contiene el texto.
        val clip = ClipData.newPlainText(label, text)
        // Establecemos el clip en el portapapeles.
        clipboard.setPrimaryClip(clip)

        // Mostramos un mensaje de confirmación al usuario.
        Toast.makeText(this, "Copiado: $text", Toast.LENGTH_SHORT).show()
    }
}