package com.carlosv.dolaraldia.ui.pago

import android.app.DatePickerDialog
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.carlosv.dolaraldia.AppPreferences
import com.carlosv.dolaraldia.MainActivity
import com.carlosv.menulateral.databinding.ActivityMercantilSearchBinding
import com.carlosv.dolaraldia.model.searchc2p.*
import com.carlosv.dolaraldia.utils.Constants

import com.carlosv.dolaraldia.utils.mercantil.CryptoUtils
import com.carlosv.dolaraldia.utils.mercantil.RetrofitClient
import com.carlosv.menulateral.BuildConfig
import com.carlosv.menulateral.databinding.DialogFailureLayoutBinding
import com.carlosv.menulateral.databinding.DialogSuccessLayoutBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MercantilSearchActivity : AppCompatActivity() {

    private var planSeleccionado: String? = null // Variable para guardar el nombre del plan

    companion object {
        private const val TAG = "MercantilSearchActivity"
    }

    private lateinit var binding: ActivityMercantilSearchBinding

    // ¡NUEVO! Variable para guardar la fecha en el formato que necesita la API (YYYY-MM-DD)
    private var selectedDateForApi: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMercantilSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupIdTypeDropdown()
        setupDateSelection()
        setupPhonePrefixDropdown() // ¡NUEVO! Llamamos a la nueva función de configuración.
        setupSearchButton()

        // Recibimos el nombre del plan y el monto.
        planSeleccionado = intent.getStringExtra(Constants.SUBSCRIPTION_PLAN_NAME)
        val montoRecibido = intent.getStringExtra(Constants.PRICE_BS)

        binding.textViewPaste.setOnClickListener {
            pegarDesdePortapapeles()
        }

        binding.editTextAmount.setText(montoRecibido.toString())

    }

    private fun pegarDesdePortapapeles() {

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        if (clipboard.hasPrimaryClip() && clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true) {

            val textoPegado = clipboard.primaryClip?.getItemAt(0)?.text?.toString()

            if (!textoPegado.isNullOrEmpty()) {

                // Esta línea establece el texto. El EditText lo truncará si es necesario.
                binding.editTextReference.setText(textoPegado)

                // --- ¡AQUÍ ESTÁ LA CORRECCIÓN! ---
                // Le pedimos la longitud al EditText MISMO, no al texto original del portapapeles.
                // De esta forma, la longitud siempre será la correcta (máximo 20).
                val longitudReal = binding.editTextReference.text?.length ?: 0
                binding.editTextReference.setSelection(longitudReal)

            } else {
                Toast.makeText(this, "El portapapeles está vacío", Toast.LENGTH_SHORT).show()
            }

        } else {
            Toast.makeText(this, "No hay texto para pegar en el portapapeles", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    /**
     * Configura el menú desplegable para el tipo de documento (V, E, J, G).
     */

    private fun setupPhonePrefixDropdown() {
        val phonePrefixes = arrayOf("0414", "0424", "0416", "0426", "0412", "0422")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, phonePrefixes)
        binding.autoCompletePhonePrefix.setAdapter(adapter)
    }

    private fun setupIdTypeDropdown() {
        val idTypes = arrayOf("V", "E", "J", "G")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, idTypes)
        binding.autoCompleteIdType.setAdapter(adapter)
    }


    /**
     * Configura el campo de fecha para mostrar un diálogo con opciones rápidas.
     */
    // En MercantilSearchActivity.kt

    private fun setupDateSelection() {

        binding.autoCompleteTransactionDate.setOnClickListener {
            // Ocultamos el teclado por si algún otro campo lo tenía abierto.
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)

            showDateSelectionDialog()
        }

        // 2. Mantenemos el OnFocusChangeListener para manejar la navegación por teclado.
        binding.autoCompleteTransactionDate.setOnFocusChangeListener { view, hasFocus ->
            // Si el campo GANA el foco...
            if (hasFocus) {
                // Ocultamos el teclado inmediatamente.
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)

                // Mostramos nuestro diálogo de selección de fecha.
                showDateSelectionDialog()

                // ¡ESTA ES LA CLAVE!
                // Justo después de mostrar el diálogo, le decimos al sistema que
                // este campo ya no debe tener el foco. Esto lo "libera" y permite
                // que el próximo clic sea detectado correctamente por el OnClickListener.
                view.clearFocus()
            }
        }

        // 3. Mantenemos el listener en el layout contenedor por si el usuario toca el borde.
        // Esto es una buena práctica de UX.
        binding.textFieldLayoutTransactionDate.setOnClickListener {
            binding.autoCompleteTransactionDate.performClick()
        }
    }
    /**
     * Muestra un diálogo con opciones de fecha: Hoy, Ayer, Anteayer y un selector de calendario.
     */
    private fun showDateSelectionDialog() {
        val dateOptions = generateDateOptions()
        val displayOptions = dateOptions.map { it.first }.toTypedArray() // "Hoy, Jueves...", "Ayer, Miércoles..."

        MaterialAlertDialogBuilder(this)
            .setTitle("Seleccionar Fecha")
            .setItems(displayOptions) { dialog, which ->
                if (which < dateOptions.size - 1) { // Si se selecciona Hoy, Ayer o Anteayer
                    val (displayText, apiDate) = dateOptions[which]
                    binding.autoCompleteTransactionDate.setText(displayText, false) // Actualizar texto visible
                    selectedDateForApi = apiDate // Guardar fecha para la API
                } else { // Si se selecciona "Elegir otra fecha..."
                    showDatePickerDialog()
                }
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Genera las opciones de fecha para el diálogo.
     * @return Una lista de pares (Texto para mostrar, Fecha para la API).
     */
    private fun generateDateOptions(): List<Pair<String, String>> {
        val options = mutableListOf<Pair<String, String>>()
        val calendar = Calendar.getInstance()
        val displayFormat = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("es", "ES"))
        val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        // Hoy
        options.add(Pair("Hoy, ${displayFormat.format(calendar.time)}", apiFormat.format(calendar.time)))

        // Ayer
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        options.add(Pair("Ayer, ${displayFormat.format(calendar.time)}", apiFormat.format(calendar.time)))

        // Anteayer
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        options.add(Pair(displayFormat.format(calendar.time), apiFormat.format(calendar.time)))

        // Opción final
        options.add(Pair("Elegir otra fecha...", ""))

        return options
    }

    /**
     * Muestra el selector de calendario tradicional.
     */
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)

                val displayFormat = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("es", "ES"))
                val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

                val displayText = displayFormat.format(selectedCalendar.time)
                val apiDate = apiFormat.format(selectedCalendar.time)

                binding.autoCompleteTransactionDate.setText(displayText, false)
                selectedDateForApi = apiDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun setupSearchButton() {
        binding.buttonSearch.setOnClickListener {

            if (!validateFields()) {
                // Si la validación falla, la función se detiene aquí.
                return@setOnClickListener
            }

            val idType = binding.autoCompleteIdType.text.toString().trim()
            val idNumber = binding.editTextCustomerIdNumber.text.toString().trim()
            val fullCustomerId = "$idType$idNumber"

            val phonePrefix = binding.autoCompletePhonePrefix.text.toString().trim()
            val phoneNumber = binding.editTextPhoneNumber.text.toString().trim()

            // --- ¡CORRECCIÓN #1! ---
            // Construimos el número completo (ej. "04141234567") tal como lo ingresó el usuario.
            val customerPhoneRaw = "$phonePrefix$phoneNumber"

            val fullReference = binding.editTextReference.text.toString().trim()
            // ... (validación de la referencia)
            if (fullReference.length < 5) {
                binding.textFieldLayoutReference.error = "Debe tener al menos 5 dígitos"
                return@setOnClickListener
            } else {
                binding.textFieldLayoutReference.error = null
            }
            val lastFiveDigitsOfReference = fullReference.takeLast(5)

            val transactionDate = selectedDateForApi
            val amount = binding.editTextAmount.text.toString().trim()

            // Validamos que los campos no estén vacíos.
            if (customerPhoneRaw.length == 11 && idNumber.isNotEmpty() && lastFiveDigitsOfReference.isNotEmpty() && transactionDate.isNotEmpty() && amount.isNotEmpty()) {

                // Pasamos el NÚMERO CRUDO (raw) a la función de búsqueda.
                // La responsabilidad de formatearlo para la API recaerá en esa función.
                searchMercantilPayment(
                    customerPhoneRaw,
                    fullCustomerId,
                    lastFiveDigitsOfReference,
                    transactionDate,
                    amount
                )

            } else {
                Toast.makeText(this, "Por favor, completa todos los campos correctamente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchMercantilPayment(customerPhoneNumberRaw: String, customerId: String, refNumber: String, transactionDate: String, amount: String) {
        binding.textViewResult.isVisible = true
        binding.textViewResult.text = "Buscando transacción..."
        binding.buttonSearch.isEnabled = false

        val gson = GsonBuilder().setPrettyPrinting().create()

        lifecycleScope.launch {
            try {

                // 1. Aseguramos que el número tenga el formato internacional (58...)
                var formattedCustomerPhone = customerPhoneNumberRaw
                if (formattedCustomerPhone.startsWith("0")) {
                    formattedCustomerPhone = "58" + formattedCustomerPhone.substring(1)
                }

                // 2. Encriptamos el número ya formateado.
                val encryptedCustomerPhone = CryptoUtils.encrypt(formattedCustomerPhone, BuildConfig.MERCANTIL_SECRET_KEY)

                // ... (el resto de la lógica de encriptación y construcción del request no cambia)
                val merchantId = BuildConfig.MERCANTIL_MERCHANT_ID
                val integratorId = BuildConfig.MERCANTIL_INTEGRATOR_ID
                val terminalId = BuildConfig.MERCANTIL_TERMINAL_ID
                val clientId = BuildConfig.MERCANTIL_CLIENT_ID
                val secretKey = BuildConfig.MERCANTIL_SECRET_KEY
                val commercePhoneNumber = BuildConfig.MERCANTIL_PHONE_NUMBER
                val encryptedCommercePhone = CryptoUtils.encrypt(commercePhoneNumber, secretKey)


                val requestBody = MercantilSearchRequest(
                    merchantIdentify = MerchantIdentify(integratorId, merchantId, terminalId),
                    clientIdentify = ClientIdentify("127.0.0.1", "Android App", MobileInfo("Android")),
                    searchBy = SearchBy(
                        amount = amount,
                        destinationMobileNumber = encryptedCommercePhone,
                        originMobileNumber = encryptedCustomerPhone,
                        paymentReference = refNumber,
                        transactionDate = transactionDate
                    )
                )

                Log.d(TAG, "==================== PETICIÓN ENVIADA ====================")
                Log.d(TAG, gson.toJson(requestBody))
                Log.d(TAG, "==========================================================")

                val response = RetrofitClient.instance.searchPayment(clientId, requestBody)

                if (response.isSuccessful && response.body() != null) {
                    val jsonString = response.body()!!.string()
                    Log.d(TAG, "RESPUESTA CRUDA RECIBIDA: $jsonString")

                    try {
                        val searchResult = gson.fromJson(jsonString, MercantilSearchResponse::class.java)

                        if (!searchResult.transactionList.isNullOrEmpty()) {
                            //  llamamos al diálogo de éxito.
                            showSuccessDialog()
                        } else {
                            // FALLO: La API respondió pero no encontró la transacción.
                            showFailureDialog("Transacción no encontrada. Por favor, verifica que los datos sean correctos.")
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "¡ERROR DE PARSEO!", e)
                        binding.textViewResult.text = "Error al interpretar la respuesta del banco."
                    }

                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "RESPUESTA DE ERROR (${response.code()}): $errorBody")
                    // FALLO: La API respondió pero no encontró la transacción.
                    showFailureDialog("Transacción no encontrada. Por favor, verifica que los datos sean correctos.")
                    binding.textViewResult.text = "Error al consultar: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "EXCEPCIÓN EN LA LLAMADA", e)
                binding.textViewResult.text = "Error de conexión: ${e.message}"
            } finally {
                // Restauramos el botón solo si la búsqueda falla. Si tiene éxito,
                // la actividad se cerrará de todos modos.
                if (binding.textViewResult.text != "Buscando transacción...") {
                    binding.buttonSearch.isEnabled = true
                }
            }
        }
    }
    private fun showSuccessDialog() {
        // 1. Inflar el layout personalizado usando View Binding.
        val dialogBinding = DialogSuccessLayoutBinding.inflate(layoutInflater)

        // 2. (Opcional) Personalizar el mensaje con el nombre del plan.
        val message = "Tu suscripción '${planSeleccionado ?: "Premium"}' ha sido activada."
        dialogBinding.successMessage.text = message

        MaterialAlertDialogBuilder(this)
            // .setTitle() y .setMessage() ya no son necesarios, están en el XML.

            // 3. Establecer nuestra vista personalizada como el contenido del diálogo.
            .setView(dialogBinding.root)

            .setPositiveButton("Aceptar") { dialog, _ ->
                AppPreferences.setUserAsPremium(planSeleccionado)
                Log.d(TAG, "Estado Premium guardado a través de AppPreferences.")

                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }
    /**
     * Muestra un diálogo de error/fallo al usuario.
     * @param message El mensaje específico que se mostrará en el diálogo.
     */
    private fun showFailureDialog(message: String) {
        // Inflamos el layout personalizado de error.
        val dialogBinding = DialogFailureLayoutBinding.inflate(layoutInflater)

        // Asignamos el mensaje de error dinámico.
        dialogBinding.failureMessage.text = message

        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Entendido") { dialog, _ ->
                // Simplemente cierra el diálogo para que el usuario pueda corregir los datos.
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun validateFields(): Boolean {
        // Usamos una bandera para rastrear la validez general.
        var isValid = true

        // 1. Validar el número de teléfono
        if (binding.editTextPhoneNumber.text.isNullOrEmpty()) {
            binding.textFieldLayoutPhoneNumber.error = "Campo requerido"
            isValid = false
        } else {
            binding.textFieldLayoutPhoneNumber.error = null // Limpiar error si está lleno
        }

        // 2. Validar el número de Cédula/RIF
        if (binding.editTextCustomerIdNumber.text.isNullOrEmpty()) {
            binding.textFieldLayoutCustomerIdNumber.error = "Campo requerido"
            isValid = false
        } else {
            binding.textFieldLayoutCustomerIdNumber.error = null
        }

        // 3. Validar el número de referencia
        if (binding.editTextReference.text.isNullOrEmpty()) {
            binding.textFieldLayoutReference.error = "Campo requerido"
            isValid = false
        } else if (binding.editTextReference.text.toString().length < 5) {
            // Mantenemos la validación de longitud que ya tenías.
            binding.textFieldLayoutReference.error = "Debe tener al menos 5 dígitos"
            isValid = false
        } else {
            binding.textFieldLayoutReference.error = null
        }

        // 4. Validar la fecha
        if (selectedDateForApi.isEmpty()) {
            binding.textFieldLayoutTransactionDate.error = "Campo requerido"
            isValid = false
        } else {
            binding.textFieldLayoutTransactionDate.error = null
        }

        return isValid
    }

}