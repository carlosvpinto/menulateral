package com.carlosv.dolaraldia.ui.pago

import android.app.DatePickerDialog
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
        setupSearchButton()

        // Recibimos el nombre del plan y el monto.
        planSeleccionado = intent.getStringExtra(Constants.SUBSCRIPTION_PLAN_NAME)
        val montoRecibido = intent.getDoubleExtra(Constants.PRICE_BS, 0.0)



            binding.editTextAmount.setText(montoRecibido.toString())

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
    private fun setupIdTypeDropdown() {
        val idTypes = arrayOf("V", "E", "J", "G")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, idTypes)
        binding.autoCompleteIdType.setAdapter(adapter)
    }

    /**
     * Configura el campo de fecha para mostrar un diálogo con opciones rápidas.
     */
    private fun setupDateSelection() {

        // --- ¡AQUÍ ESTÁ LA CORRECCIÓN PRINCIPAL! ---
        // 1. Asignamos el OnClickListener al CONTENEDOR (TextInputLayout).
        //    Esto captura el clic en cualquier parte del campo, incluido el ícono.
        binding.textFieldLayoutTransactionDate.setOnClickListener {
            showDateSelectionDialog()
        }

        // 2. Asignamos el OnFocusChangeListener al EditText INTERNO.
        //    Esto se disparará si el usuario navega al campo usando el teclado, por ejemplo.
        binding.autoCompleteTransactionDate.setOnFocusChangeListener { view, hasFocus ->
            // Solo mostramos el diálogo si el campo HA GANADO el foco
            // y si el teclado no está visible (para evitar conflictos).
            if (hasFocus) {
                // Ocultamos el teclado por si acaso se abrió.
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)

                showDateSelectionDialog()
            }
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
            val idType = binding.autoCompleteIdType.text.toString().trim()
            val idNumber = binding.editTextCustomerIdNumber.text.toString().trim()
            val fullCustomerId = "$idType$idNumber"

            val customerPhoneRaw = binding.editTextPhoneNumber.text.toString().trim()

            // --- ¡AQUÍ EMPIEZA LA NUEVA LÓGICA DE VALIDACIÓN! ---

            // 1. Leemos la referencia completa que escribió el usuario.
            val fullReference = binding.editTextReference.text.toString().trim()

            // 2. Validamos la longitud de la referencia.
            if (fullReference.length < 5) {
                // Si es muy corta, mostramos un error en el campo y detenemos el proceso.
                binding.textFieldLayoutReference.error = "Debe tener al menos 5 dígitos"
                Toast.makeText(this, "La referencia debe tener al menos 5 dígitos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Sale del listener.
            } else {
                // Si es válida, limpiamos cualquier error previo.
                binding.textFieldLayoutReference.error = null
            }

            // 3. Extraemos SOLO los últimos 5 caracteres.
            val lastFiveDigitsOfReference = fullReference.takeLast(5)

            // --- FIN DE LA NUEVA LÓGICA DE VALIDACIÓN ---

            val transactionDate = selectedDateForApi
            val amount = binding.editTextAmount.text.toString().trim()

            var formattedCustomerPhone = customerPhoneRaw
            if (formattedCustomerPhone.startsWith("0")) {
                formattedCustomerPhone = "58" + formattedCustomerPhone.substring(1)
            }

            // Validamos que el resto de los campos no estén vacíos.
            if (formattedCustomerPhone.isNotEmpty() && idNumber.isNotEmpty() && transactionDate.isNotEmpty() && amount.isNotEmpty()) {

                // 4. Pasamos la referencia recortada a la función de búsqueda.
                searchMercantilPayment(
                    formattedCustomerPhone,
                    fullCustomerId,
                    lastFiveDigitsOfReference, // ¡USAMOS LA VERSIÓN RECORTADA!
                    transactionDate,
                    amount
                )

            } else {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchMercantilPayment(customerPhoneNumber: String, customerId: String, refNumber: String, transactionDate: String, amount: String) {
        binding.textViewResult.isVisible = true
        binding.textViewResult.text = "Buscando transacción..."
        binding.buttonSearch.isEnabled = false

        val gson = GsonBuilder().setPrettyPrinting().create()

        lifecycleScope.launch {
            try {
                // (La lógica interna de la llamada a la API no cambia)
                val merchantId = BuildConfig.MERCANTIL_MERCHANT_ID
                val integratorId = BuildConfig.MERCANTIL_INTEGRATOR_ID
                val terminalId = BuildConfig.MERCANTIL_TERMINAL_ID
                val clientId = BuildConfig.MERCANTIL_CLIENT_ID
                val secretKey = BuildConfig.MERCANTIL_SECRET_KEY
                val commercePhoneNumber = BuildConfig.MERCANTIL_PHONE_NUMBER

                val encryptedCommercePhone = CryptoUtils.encrypt(commercePhoneNumber, secretKey)
                val encryptedCustomerPhone = CryptoUtils.encrypt(customerPhoneNumber, secretKey)

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
                            // --- ¡AQUÍ ESTÁ LA MODIFICACIÓN PRINCIPAL! ---
                            // En lugar de mostrar el texto en el TextView, llamamos al diálogo de éxito.
                            showSuccessDialog()
                        } else {
                            // Si la transacción no se encuentra, lo indicamos en el TextView.
                            binding.textViewResult.text = "Transacción no encontrada. Verifica los datos."
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "¡ERROR DE PARSEO!", e)
                        binding.textViewResult.text = "Error al interpretar la respuesta del banco."
                    }

                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "RESPUESTA DE ERROR (${response.code()}): $errorBody")
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
        MaterialAlertDialogBuilder(this)
            .setTitle("¡Pago Verificado!")
            .setMessage("Tu suscripción '${planSeleccionado ?: "Premium"}' ha sido activada.")
            .setPositiveButton("Aceptar") { dialog, _ ->

                // --- ¡AQUÍ ESTÁ EL CAMBIO! ---
                // Ahora llamamos a nuestra función centralizada en AppPreferences.
                AppPreferences.setUserAsPremium(planSeleccionado)
                Log.d(TAG, "Estado Premium guardado a través de AppPreferences.")

                // El resto de la navegación no cambia.
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }

}