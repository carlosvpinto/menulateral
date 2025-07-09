package com.carlosv.dolaraldia.ui.home

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.Global.putString
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.carlosv.dolaraldia.adapter.PagoMovilAdapter

import com.carlosv.dolaraldia.model.datosPMovil.DatosPMovilModel
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentDatosPerBinding
import com.carlosv.menulateral.databinding.FragmentHomeBinding
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.util.Date
import java.util.Locale



private var _binding: FragmentDatosPerBinding? = null
private val binding get() = _binding ?: throw IllegalStateException("Binding is null")

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private var spBancos: Spinner? = null
private  var adapterPM: PagoMovilAdapter?=null
private var pagomovilActivo: DatosPMovilModel?= null
private var pagomovilActivoList: MutableList<DatosPMovilModel?> = mutableListOf()
private var posicion : Int?=null

private var pagosMovils= ArrayList<DatosPMovilModel>()
private val TAG = "PERSONAL"

private lateinit var sharedPref: SharedPreferences

class DatosPerFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var spBancos: Spinner
    private var bancoSeleccionado= ""
    private var isExpanded = false



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
    ):  View? {

        _binding = FragmentDatosPerBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val view = inflater.inflate(R.layout.fragment_datos_per, container, false)
        val linearLayoutManager = LinearLayoutManager(requireContext())
        binding.recyPagoMovil.layoutManager = linearLayoutManager


        sharedPref =  requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // Obtener el array de bancos desde los recursos
        val listaBancos = resources.getStringArray(R.array.lista_bancos)

        val listaLetras = resources.getStringArray(R.array.lista_letras)
        llamarPagoMovil()

        binding.btnCancelar.setOnClickListener {
            binding.btnGuardar.text= "Agregar"
            binding.btnCancelar.visibility= View.GONE
            binding.linearLayoutDatosInfo.visibility = View.GONE
            binding.recyPagoMovil.visibility= View.VISIBLE
        }

        binding.btnGuardar.setOnClickListener {
            if (binding.btnGuardar.text== "Agregar"){

                binding.linearLayoutDatosInfo.visibility = View.VISIBLE
                limpiarDatosLinearLayout()
                binding.btnGuardar.text= "Guardar"
                binding.btnCancelar.visibility= View.GONE
                binding.btnCancelar.visibility= View.VISIBLE
                binding.recyPagoMovil.visibility= View.GONE
                return@setOnClickListener
            }


            if(binding.btnGuardar.text== "Guardar" ){

                Log.d(TAG, "onCreateView: Guardar")
                guardarPagoMovil(requireContext())
                llamarPagoMovil()
                return@setOnClickListener

            }
            if (binding.btnGuardar.text=="Actualizar"){

                editarPagoMovil()
                llamarPagoMovil()
                return@setOnClickListener
            }
            }


        val adaptadorSp: ArrayAdapter<String> = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listaBancos
        )
        val adaptadorLetras: ArrayAdapter<String> = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listaLetras
        )

        binding.spinnerLetra.adapter = adaptadorLetras

        binding.spinnerBanco.adapter = adaptadorSp

        // Establecer un listener para manejar la selección del spinner
        binding.spinnerBanco.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                bancoSeleccionado = parent.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                Toast.makeText(requireContext(), "Debe selecionar el banco", Toast.LENGTH_SHORT).show()
            }
        }

            return root
    }
    private fun chequeaLenguajeSistema(): String {
        val currentLocale = Locale.getDefault()
        return when (currentLocale.language) {
            "en" -> "Select Bank"
            "es" -> "Seleccione el Banco"
            "fr" -> "Sélectionner une banque"
            // Puedes agregar más casos según sea necesario
            else -> "El sistema está en otro idioma: ${currentLocale.displayLanguage}"
        }
    }


    private fun validarDatos(): Pair<Boolean, String> {
        val nombre = binding.txtNombre.text.toString().trim()
        val cedula = binding.txtCedula.text.toString().trim()
        val tlf = binding.txtTlf.text.toString().trim()
        val banco = binding.spinnerBanco.selectedItem.toString()
        Log.d(TAG, "validarDatos: obtenerLenguajeSistema ${chequeaLenguajeSistema()}")
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
               // binding.spinnerBanco.error = "El teléfono no puede estar vacío"
                Pair(false, "El Banco no puede estar vacío")
            }
            banco == chequeaLenguajeSistema() -> {
                Pair(false, chequeaLenguajeSistema())
            }
            else -> Pair(true, "")
        }
    }



    private fun llamarPagoMovil() {
        val pagoMovilList = obtenerPagoMovilList(requireContext())
        pagosMovils.clear()

        if (!pagoMovilList.isNullOrEmpty()) {
            pagosMovils.addAll(pagoMovilList)
        }

        adapterPM = PagoMovilAdapter(this@DatosPerFragment, pagosMovils, ::mostrarDatosaEditar, ::borrarPagoMovil, ::actualizarPredeterminado)
        binding.recyPagoMovil.adapter = adapterPM
        adapterPM?.updatePrecioBancos(pagoMovilList)

        Log.d(TAG, "onCreateView: VACIO!!  pagoMovilList $pagoMovilList")
    }


    private fun guardarPagoMovil(context: Context) {
        var tipo = ""

        val (isValid, errorMessage) = validarDatos()
        if (isValid) {
            binding.btnGuardar.text= "Agregar"
            // Datos válidos, continuar con el procesamiento
            if (binding.rdioPagoMovil.isChecked) {
                tipo = binding.rdioPagoMovil.text.toString()
            }
            if (binding.rdioTransferencia.isChecked) {
                tipo = binding.rdioTransferencia.text.toString()
            }

            val pagoMovil = DatosPMovilModel(
                seleccionado = true,
                tipo = tipo,
                nombre = binding.txtNombre.text.toString(),
                tlf = binding.txtTlf.text.toString(),
                cedula = binding.spinnerLetra.selectedItem.toString()+ binding.txtCedula.text.toString(),
                banco = bancoSeleccionado,
                fecha = Date().toString()
            )

            val gson = Gson()
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences("MyPreferencesPMovil", AppCompatActivity.MODE_PRIVATE)

            // Leer la lista existente de pagoMovil desde SharedPreferences
            val pagoMovilJson = sharedPreferences.getString("datosPMovilList", null)
            val pagoMovilList: MutableList<DatosPMovilModel> = if (pagoMovilJson != null) {
                gson.fromJson(pagoMovilJson, object : TypeToken<MutableList<DatosPMovilModel>>() {}.type)
            } else {
                mutableListOf()
            }

            // Desactivar el campo seleccionado en todos los elementos existentes
            for (i in pagoMovilList.indices) {
                pagoMovilList[i].seleccionado = false
            }

            // Agregar el nuevo pagoMovil a la lista
            pagoMovilList.add(pagoMovil)

            // Serializar la lista actualizada a JSON
            val pagoMovilListJson = gson.toJson(pagoMovilList)

            // Guardar la lista actualizada en SharedPreferences
            val editor = sharedPreferences.edit()
            editor.putString("datosPMovilList", pagoMovilListJson)
            editor.apply()
            binding.btnGuardar.text= "Agregar"
            binding.linearLayoutDatosInfo.visibility = View.GONE
            binding.btnCancelar.visibility= View.GONE
            binding.recyPagoMovil.visibility= View.VISIBLE
            ocultarTeclado(binding.btnGuardar, requireContext())
        } else {
            // Datos inválidos, mostrar el error
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

        // Desactivar todos los elementos
        for (i in pagoMovilList.indices) {
            pagoMovilList[i].seleccionado = false
        }

        // Activar solo el seleccionado
        pagoMovilList[position].seleccionado = true

        // Guardar la lista actualizada en SharedPreferences
        val pagoMovilListJson = gson.toJson(pagoMovilList)
        val editor = sharedPreferences.edit()
        editor.putString("datosPMovilList", pagoMovilListJson)
        editor.apply()

        // Actualizar la lista en la UI
        llamarPagoMovil()
    }

    // Función para editar un pagoMovil
    private fun editarPagoMovil() {

        val (isValid, errorMessage) = validarDatos()
        if (isValid) {
            // Datos válidos, continuar con el procesamiento

            binding.btnGuardar.text= "Agregar"
            binding.linearLayoutDatosInfo.visibility= View.GONE
            binding.recyPagoMovil.visibility= View.VISIBLE
            val gson = Gson()
            val sharedPreferences: SharedPreferences =
                requireContext().getSharedPreferences("MyPreferencesPMovil", AppCompatActivity.MODE_PRIVATE)

            // Leer la lista existente de pagoMovil desde SharedPreferences
            val pagoMovilJson = sharedPreferences.getString("datosPMovilList", null)
            val pagoMovilList: MutableList<DatosPMovilModel> = if (pagoMovilJson != null) {
                gson.fromJson(pagoMovilJson, object : TypeToken<MutableList<DatosPMovilModel>>() {}.type)
            } else {
                mutableListOf()
            }
            val pagoMovil = pagoMovilActivado()
            // Editar el pagoMovil en la lista
            pagoMovilList[posicion!!] = pagoMovil

            // Serializar la lista actualizada a JSON
            val pagoMovilListJson = gson.toJson(pagoMovilList)

            // Guardar la lista actualizada en SharedPreferences
            val editor = sharedPreferences.edit()
            editor.putString("datosPMovilList", pagoMovilListJson)
            editor.apply()

            // Actualizar la lista en la UI
            llamarPagoMovil()
            ocultarTeclado(binding.btnGuardar, requireContext())

        }else{
            // Datos inválidos, mostrar el error
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()

        }

    }

    private fun actualiazarPagoMovil(){
        val gson = Gson()
        val sharedPreferences: SharedPreferences =
            requireContext().getSharedPreferences("MyPreferencesPMovil", AppCompatActivity.MODE_PRIVATE)

        // Leer la lista existente de pagoMovil desde SharedPreferences
        val pagoMovilJson = sharedPreferences.getString("datosPMovilList", null)
        val pagoMovilList: MutableList<DatosPMovilModel> = if (pagoMovilJson != null) {
            gson.fromJson(pagoMovilJson, object : TypeToken<MutableList<DatosPMovilModel>>() {}.type)
        } else {
            mutableListOf()
        }

        // Editar el pagoMovil en la lista
        pagoMovilList[posicion!!] = pagoMovilActivado()

        // Serializar la lista actualizada a JSON
        val pagoMovilListJson = gson.toJson(pagomovilActivoList)

        // Guardar la lista actualizada en SharedPreferences
        val editor = sharedPreferences.edit()
        editor.putString("datosPMovilList", pagoMovilListJson)
        editor.apply()

        // Actualizar la lista en la UI
        llamarPagoMovil()
    }

    private fun pagoMovilActivado():DatosPMovilModel{

        return DatosPMovilModel(
            seleccionado = binding.rdioPagoMovil.isChecked,
            tipo =binding.rdioPagoMovil.text.toString(),
            nombre = binding.txtNombre.text.toString(),
            tlf =  binding.txtTlf.text.toString(),
            cedula = binding.spinnerLetra.selectedItem.toString()+binding.txtCedula.text.toString(),
            banco = binding.spinnerBanco.selectedItem.toString(),
            fecha = Date().toString()
        )
    }

    private fun mostrarDatosaEditar(pagomovil: DatosPMovilModel, position: Int) {
        binding.linearLayoutDatosInfo.visibility = View.VISIBLE
        binding.recyPagoMovil.visibility = View.GONE
        binding.btnGuardar.text = "Actualizar"
        binding.txtNombre.setText(pagomovil.nombre)
        binding.txtTlf.setText(pagomovil.tlf)
        // Verifica si texto en Pagomovul.cedula tiene una letra en su primer caracter
        val cedula = pagomovil.cedula
        if (cedula!!.isNotEmpty() && cedula[0].isLetter()) {
            binding.txtCedula.setText(cedula.substring(1))
        } else {
            binding.txtCedula.setText(cedula)
        }
        binding.txtCedula.setText(pagomovil.cedula.substring(1))
        pagomovilActivo = pagomovil
        posicion = position


        // Obtener el array de bancos desde los recursos
        val listaBancos = resources.getStringArray(R.array.lista_bancos)
        val listaPrefijo = resources.getStringArray(R.array.lista_letras)
        val prefijo = pagomovil.cedula!!.first().toString()

        // Encontrar la posición del valor en el Spinner
        val prefijoCedula= listaPrefijo.indexOf(prefijo)
        val bancoPosition = listaBancos.indexOf(pagomovil.banco)

        if (prefijoCedula>=0){
            binding.spinnerLetra.setSelection(prefijoCedula)
        }

        if (bancoPosition >= 0) {
            binding.spinnerBanco.setSelection(bancoPosition)
        } else {
            // Valor no encontrado, puedes manejar esto como prefieras
            Log.d(TAG, "mostrarDatosaEditar: Banco no encontrado en el Spinner")
        }
    }


    // Función para borrar un pagoMovil
    private fun borrarPagoMovil(position: Int) {
        val gson = Gson()
        val sharedPreferences: SharedPreferences =
            requireContext().getSharedPreferences("MyPreferencesPMovil", AppCompatActivity.MODE_PRIVATE)

        // Leer la lista existente de pagoMovil desde SharedPreferences
        val pagoMovilJson = sharedPreferences.getString("datosPMovilList", null)
        val pagoMovilList: MutableList<DatosPMovilModel> = if (pagoMovilJson != null) {
            gson.fromJson(pagoMovilJson, object : TypeToken<MutableList<DatosPMovilModel>>() {}.type)
        } else {
            mutableListOf()
        }

        // Validar que el índice está dentro de los límites de la lista
        if (position >= 0 && position < pagoMovilList.size) {
            // Borrar el pagoMovil de la lista
            pagoMovilList.removeAt(position)

            // Serializar la lista actualizada a JSON
            val pagoMovilListJson = gson.toJson(pagoMovilList)

            // Guardar la lista actualizada en SharedPreferences
            val editor = sharedPreferences.edit()
            editor.putString("datosPMovilList", pagoMovilListJson)

            // Usar commit() en lugar de apply() para esperar a que los datos se guarden
            val success = editor.commit()

            if (success) {
                // Actualizar la lista en la UI
                llamarPagoMovil()
            } else {
                // Manejar el error en caso de que la operación de guardado falle
                Log.e("borrarPagoMovil", "Error al guardar los datos actualizados en SharedPreferences")
            }
        } else {
            // Manejar el caso donde el índice está fuera de los límites de la lista
            Log.e("borrarPagoMovil", "Índice fuera de los límites: $position, tamaño de la lista: ${pagoMovilList.size}")
        }
    }



    // Define una función para recuperar la respuesta de SharedPreferences
    private fun obtenerPagoMovilList(context: Context): List<DatosPMovilModel> {
        val gson = Gson()
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyPreferencesPMovil", AppCompatActivity.MODE_PRIVATE)

        // Leer la lista existente de pagoMovil desde SharedPreferences
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
    }
    private fun ocultarTeclado(view: View, context: Context) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }



    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            DatosPerFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }




}