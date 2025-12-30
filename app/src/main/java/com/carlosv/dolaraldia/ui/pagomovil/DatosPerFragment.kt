package com.carlosv.dolaraldia.ui.pagomovil

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.carlosv.dolaraldia.adapter.PagoMovilAdapter
import com.carlosv.dolaraldia.model.datosPMovil.DatosPMovilModel
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentDatosPerBinding
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Date

class DatosPerFragment : Fragment() {
    private var _binding: FragmentDatosPerBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null")

    private lateinit var sharedPref: SharedPreferences
    private var bancoSeleccionado = ""
    private var posicion: Int? = null
    private var pagosMovils = ArrayList<DatosPMovilModel>()
    private var adapterPM: PagoMovilAdapter? = null
    private var rutaImagenSeleccionada: String? = null

    // Launcher para seleccionar imágenes
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            convertToJpgAndStore(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentDatosPerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        sharedPref = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        binding.recyPagoMovil.layoutManager = LinearLayoutManager(requireContext())

        // Cargar datos iniciales
        llamarPagoMovil()

        // --- LÓGICA DEL BOTÓN "AGREGAR PRIMERA CUENTA" (DEL ESTADO VACÍO) ---
        binding.btnAgregarDesdeVacio.setOnClickListener {
            // Simulamos clic en el botón agregar principal
            binding.btnGuardar.text = "Agregar" // Aseguramos estado
            binding.btnGuardar.performClick()
        }

        binding.btnCancelar.setOnClickListener {
            binding.btnGuardar.text = "Agregar"
            binding.btnCancelar.visibility = View.GONE
            binding.linearLayoutDatosInfo.visibility = View.GONE

            // Al cancelar, verificamos si mostramos la lista o el estado vacío
            actualizarVisibilidadLista()

            rutaImagenSeleccionada = null
        }

        binding.imgLogoPersona.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnGuardar.setOnClickListener {
            when (binding.btnGuardar.text) {
                "Agregar" -> {
                    binding.linearLayoutDatosInfo.visibility = View.VISIBLE
                    limpiarDatosLinearLayout()
                    binding.btnGuardar.text = "Guardar"
                    binding.btnCancelar.visibility = View.VISIBLE

                    // Ocultamos tanto la lista como el mensaje de vacío porque estamos editando
                    binding.recyPagoMovil.visibility = View.GONE
                    binding.emptyStateView.visibility = View.GONE

                    rutaImagenSeleccionada = null
                }
                "Guardar" -> {
                    guardarPagoMovil(requireContext())
                    llamarPagoMovil()
                }
                "Actualizar" -> {
                    editarPagoMovil()
                    llamarPagoMovil()
                }
            }
        }

        val listaBancos = resources.getStringArray(R.array.lista_bancos)
        val listaLetras = resources.getStringArray(R.array.lista_letras)

        binding.spinnerLetra.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listaLetras
        )
        binding.spinnerBanco.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listaBancos
        )

        binding.spinnerBanco.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                bancoSeleccionado = parent.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                Toast.makeText(requireContext(), "Debe seleccionar el banco", Toast.LENGTH_SHORT).show()
            }
        }

        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.appear_from_top)
        binding.root.startAnimation(animation)
        return root
    }


    private fun llamarPagoMovil() {
        val pagoMovilList = obtenerPagoMovilList(requireContext())
        pagosMovils.clear()
        if (!pagoMovilList.isNullOrEmpty()) {
            pagosMovils.addAll(pagoMovilList)
        }

        // --- AQUÍ CONECTAMOS EL ADAPTER CON EL CALLBACK NUEVO ---
        adapterPM = PagoMovilAdapter(
            this@DatosPerFragment,
            pagosMovils,
            ::mostrarDatosaEditar,
            ::borrarPagoMovil,
            ::actualizarPredeterminado,
            // Callback que recibe true si la lista está vacía
            { estaVacio ->
                // --- CORRECCIÓN: BLINDAJE CONTRA CRASH ---
                // Verificamos si la vista aún existe antes de tocar nada.
                if (_binding != null) {

                    // Ahora es seguro llamar a 'binding'
                    if (binding.linearLayoutDatosInfo.visibility == View.GONE) {
                        toggleEmptyState(estaVacio)
                    }
                }
            }
        )

        // Asignamos el adapter
        binding.recyPagoMovil.adapter = adapterPM
        adapterPM?.updatePrecioBancos(pagoMovilList)

        // Verificación inicial
        actualizarVisibilidadLista()
    }

    // --- FUNCIÓN AUXILIAR PARA CONTROLAR VISIBILIDAD ---
    private fun actualizarVisibilidadLista() {
        val estaVacio = pagosMovils.isEmpty()
        toggleEmptyState(estaVacio)
    }

    private fun toggleEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recyPagoMovil.visibility = View.GONE
            binding.emptyStateView.visibility = View.VISIBLE
        } else {
            binding.recyPagoMovil.visibility = View.VISIBLE
            binding.emptyStateView.visibility = View.GONE
        }
    }

    private fun guardarPagoMovil(context: Context) {
        val (isValid, errorMessage) = validarDatos()
        if (isValid) {
            val tipo = when {
                binding.rdioPagoMovil.isChecked -> binding.rdioPagoMovil.text.toString()
                binding.rdioTransferencia.isChecked -> binding.rdioTransferencia.text.toString()
                else -> ""
            }
            val pagoMovil = DatosPMovilModel(
                seleccionado = true,
                tipo = tipo,
                nombre = binding.txtNombre.text.toString(),
                tlf = binding.txtTlf.text.toString(),
                cedula = binding.spinnerLetra.selectedItem.toString() + binding.txtCedula.text.toString(),
                banco = bancoSeleccionado,
                fecha = Date().toString(),
                imagen = rutaImagenSeleccionada
            )
            val gson = Gson()
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences("MyPreferencesPMovil", AppCompatActivity.MODE_PRIVATE)
            val pagoMovilJson = sharedPreferences.getString("datosPMovilList", null)
            val pagoMovilList: MutableList<DatosPMovilModel> = if (pagoMovilJson != null) {
                gson.fromJson(pagoMovilJson, object : TypeToken<MutableList<DatosPMovilModel>>() {}.type)
            } else {
                mutableListOf()
            }
            for (i in pagoMovilList.indices) {
                pagoMovilList[i].seleccionado = false
            }
            pagoMovilList.add(pagoMovil)
            val pagoMovilListJson = gson.toJson(pagoMovilList)
            val editor = sharedPreferences.edit()
            editor.putString("datosPMovilList", pagoMovilListJson)
            editor.apply()

            binding.btnGuardar.text = "Agregar"
            binding.linearLayoutDatosInfo.visibility = View.GONE
            binding.btnCancelar.visibility = View.GONE

            // Actualizamos la vista (esto mostrará la lista porque acabamos de guardar)
            binding.recyPagoMovil.visibility = View.VISIBLE

            ocultarTeclado(binding.btnGuardar, requireContext())
            rutaImagenSeleccionada = null
        } else {
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun editarPagoMovil() {
        val (isValid, errorMessage) = validarDatos()
        if (isValid) {
            val gson = Gson()
            val sharedPreferences: SharedPreferences =
                requireContext().getSharedPreferences("MyPreferencesPMovil", AppCompatActivity.MODE_PRIVATE)
            val pagoMovilJson = sharedPreferences.getString("datosPMovilList", null)
            val pagoMovilList: MutableList<DatosPMovilModel> = if (pagoMovilJson != null) {
                gson.fromJson(pagoMovilJson, object : TypeToken<MutableList<DatosPMovilModel>>() {}.type)
            } else {
                mutableListOf()
            }
            val pagoMovil = DatosPMovilModel(
                seleccionado = binding.rdioPagoMovil.isChecked,
                tipo = binding.rdioPagoMovil.text.toString(),
                nombre = binding.txtNombre.text.toString(),
                tlf = binding.txtTlf.text.toString(),
                cedula = binding.spinnerLetra.selectedItem.toString() + binding.txtCedula.text.toString(),
                banco = binding.spinnerBanco.selectedItem.toString(),
                fecha = Date().toString(),
                imagen = rutaImagenSeleccionada
            )
            pagoMovilList[posicion!!] = pagoMovil
            val pagoMovilListJson = gson.toJson(pagoMovilList)
            val editor = sharedPreferences.edit()
            editor.putString("datosPMovilList", pagoMovilListJson)
            editor.apply()
            binding.btnGuardar.text = "Agregar"
            binding.linearLayoutDatosInfo.visibility = View.GONE

            // Regresamos a la lista
            binding.recyPagoMovil.visibility = View.VISIBLE

            llamarPagoMovil()
            ocultarTeclado(binding.btnGuardar, requireContext())
            rutaImagenSeleccionada = null
        } else {
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarPredeterminado(position: Int) {
        val gson = Gson()
        val sharedPreferences: SharedPreferences =
            requireContext().getSharedPreferences("MyPreferencesPMovil", AppCompatActivity.MODE_PRIVATE)
        val pagoMovilJson = sharedPreferences.getString("datosPMovilList", null)
        val pagoMovilList: MutableList<DatosPMovilModel> = if (pagoMovilJson != null) {
            gson.fromJson(pagoMovilJson, object : TypeToken<MutableList<DatosPMovilModel>>() {}.type)
        } else {
            mutableListOf()
        }
        for (i in pagoMovilList.indices) {
            pagoMovilList[i].seleccionado = false
        }
        pagoMovilList[position].seleccionado = true
        val pagoMovilListJson = gson.toJson(pagoMovilList)
        val editor = sharedPreferences.edit()
        editor.putString("datosPMovilList", pagoMovilListJson)
        editor.apply()
        llamarPagoMovil()
    }

    private fun borrarPagoMovil(position: Int) {
        val gson = Gson()
        val sharedPreferences: SharedPreferences =
            requireContext().getSharedPreferences("MyPreferencesPMovil", AppCompatActivity.MODE_PRIVATE)
        val pagoMovilJson = sharedPreferences.getString("datosPMovilList", null)
        val pagoMovilList: MutableList<DatosPMovilModel> = if (pagoMovilJson != null) {
            gson.fromJson(pagoMovilJson, object : TypeToken<MutableList<DatosPMovilModel>>() {}.type)
        } else {
            mutableListOf()
        }
        if (position >= 0 && position < pagoMovilList.size) {
            pagoMovilList.removeAt(position)
            val pagoMovilListJson = gson.toJson(pagoMovilList)
            val editor = sharedPreferences.edit()
            editor.putString("datosPMovilList", pagoMovilListJson)
            editor.commit()
            llamarPagoMovil()
        }
    }

    private fun mostrarDatosaEditar(pagomovil: DatosPMovilModel, position: Int) {
        binding.linearLayoutDatosInfo.visibility = View.VISIBLE
        binding.recyPagoMovil.visibility = View.GONE
        binding.emptyStateView.visibility = View.GONE // Aseguramos ocultar el empty state al editar

        binding.btnGuardar.text = "Actualizar"
        binding.txtNombre.setText(pagomovil.nombre)
        binding.txtTlf.setText(pagomovil.tlf)
        binding.txtCedula.setText(pagomovil.cedula!!.substring(1))
        rutaImagenSeleccionada = pagomovil.imagen
        if (!pagomovil.imagen.isNullOrEmpty()) {
            Glide.with(this)
                .load(File(pagomovil.imagen))
                .placeholder(R.drawable.ic_agregar_imagen)
                .into(binding.imgLogoPersona)
        } else {
            binding.imgLogoPersona.setImageResource(R.drawable.ic_agregar_imagen)
        }
        posicion = position

        val listaBancos = resources.getStringArray(R.array.lista_bancos)
        val listaPrefijo = resources.getStringArray(R.array.lista_letras)
        val prefijo = pagomovil.cedula.first().toString()
        val prefijoCedula = listaPrefijo.indexOf(prefijo)
        val bancoPosition = listaBancos.indexOf(pagomovil.banco)
        if (prefijoCedula >= 0) binding.spinnerLetra.setSelection(prefijoCedula)
        if (bancoPosition >= 0) binding.spinnerBanco.setSelection(bancoPosition)
    }

    private fun obtenerPagoMovilList(context: Context): List<DatosPMovilModel> {
        val gson = Gson()
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferencesPMovil", AppCompatActivity.MODE_PRIVATE)
        val pagoMovilJson = sharedPreferences.getString("datosPMovilList", null)
        return if (pagoMovilJson != null) {
            gson.fromJson(pagoMovilJson, object : TypeToken<List<DatosPMovilModel>>() {}.type)
        } else {
            emptyList()
        }
    }

    private fun limpiarDatosLinearLayout() {
        binding.txtNombre.text?.clear()
        binding.txtTlf.text?.clear()
        binding.txtCedula.text?.clear()
        binding.spinnerBanco.setSelection(0)
        binding.spinnerLetra.setSelection(0)
        binding.imgLogoPersona.setImageResource(R.drawable.ic_agregar_imagen)
    }

    private fun ocultarTeclado(view: View, context: Context) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun convertToJpgAndStore(imageUri: Uri) {
        binding.apply {
            btnGuardar.isEnabled = false
            btnCancelar.isEnabled = false
        }

        lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    getCorrectlyOrientedBitmap(imageUri)
                } ?: throw Exception("No se pudo decodificar la imagen.")

                val permanentFile = withContext(Dispatchers.IO) {
                    val fileName = "pm_image_${System.currentTimeMillis()}.jpg"
                    val directory = requireContext().filesDir
                    val file = File(directory, fileName)

                    FileOutputStream(file).use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    }
                    file
                }

                rutaImagenSeleccionada = permanentFile.absolutePath

                withContext(Dispatchers.Main) {
                    binding.apply {
                        btnGuardar.isEnabled = true
                        btnCancelar.isEnabled = true
                        Glide.with(this@DatosPerFragment)
                            .load(permanentFile)
                            .into(imgLogoPersona)
                    }
                    Toast.makeText(requireContext(), "Imagen guardada exitosamente.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.apply {
                        btnGuardar.isEnabled = true
                        btnCancelar.isEnabled = true
                    }
                    Log.e("DatosPerFragment", "Error al procesar la imagen: ${e.message}", e)
                    Toast.makeText(requireContext(), "Error al procesar imagen: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    private fun getCorrectlyOrientedBitmap(uri: Uri): Bitmap? {
        val contentResolver = requireContext().contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        if (bitmap == null) return null
        val exifInputStream = contentResolver.openInputStream(uri)
        val exif = androidx.exifinterface.media.ExifInterface(exifInputStream!!)
        val orientation = exif.getAttributeInt(
            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
        )
        exifInputStream.close()
        val matrix = Matrix()
        when (orientation) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
    }

    private fun createJpgFromBitmap(bitmap: Bitmap): File {
        val cacheDir = requireContext().cacheDir
        val jpgFile = File.createTempFile("converted_", ".jpg", cacheDir)
        FileOutputStream(jpgFile).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
        }
        return jpgFile
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun validarDatos(): Pair<Boolean, String> {
        val nombre = binding.txtNombre.text.toString().trim()
        val cedula = binding.txtCedula.text.toString().trim()
        val tlf = binding.txtTlf.text.toString().trim()
        val banco = binding.spinnerBanco.selectedItem.toString()

        return when {
            nombre.isEmpty() -> {
                binding.txtNombre.error = "El nombre no puede estar vacío"
                Pair(false, "El nombre no puede estar vacío")
            }
            cedula.isEmpty() -> {
                binding.txtCedula.error = "La cédula no puede estar vacía"
                Pair(false, "La cédula no puede estar vacía")
            }
            tlf.isEmpty() -> {
                binding.txtTlf.error = "El teléfono no puede estar vacío"
                Pair(false, "El teléfono no puede estar vacío")
            }
            banco.isEmpty() -> {
                Pair(false, "El Banco no puede estar vacío")
            }
            else -> Pair(true, "")
        }
    }
}