package com.carlosv.dolaraldia.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.carlosv.dolaraldia.model.datosPMovil.DatosPMovilModel
import com.carlosv.menulateral.R
import java.io.File
import java.util.Date

class PagoMovilAdapter(
    val context: Fragment,
    var pagosMoviles: ArrayList<DatosPMovilModel>,
    private val onEditClick: (DatosPMovilModel, Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit,
    private val onCheckboxClick: (Int) -> Unit,
    // NUEVO: Callback para avisar si la lista está vacía
    private val onListStateChange: (Boolean) -> Unit
) : RecyclerView.Adapter<PagoMovilAdapter.PagoMovilAdapterViewHolder>() {

    private var expandedPosition = -1
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagoMovilAdapterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.cardview_pago_movil, parent, false)
        return PagoMovilAdapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: PagoMovilAdapterViewHolder, position: Int) {
        val pagoMovil = pagosMoviles[position]
        holder.bind(pagoMovil, position)
    }

    override fun getItemCount(): Int {
        return pagosMoviles.size
    }

    fun updatePrecioBancos(precionBancosList: List<DatosPMovilModel>?) {
        if (precionBancosList == null) {
            Log.e("ADAPTER", "precionBancosList es null")
            return
        }
        this.pagosMoviles = ArrayList(precionBancosList)

        handler.post {
            notifyDataSetChanged()
            // NUEVO: Verificamos si la lista quedó vacía y avisamos al Fragment
            onListStateChange(pagosMoviles.isEmpty())
        }
    }

    inner class PagoMovilAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val radioTipo: TextView = view.findViewById(R.id.txtTipo)
        private val textViewNombre: TextView = view.findViewById(R.id.txtNombrePM)
        private val textViewCedula: TextView = view.findViewById(R.id.txtCedulaPM)
        private val textViewTelefono: TextView = view.findViewById(R.id.txtTelPM)
        private val textViewBanco: TextView = view.findViewById(R.id.txtNombreBancoPM)
        // private val cardView: CardView = view.findViewById(R.id.cardViewPM) // No se usa, se puede comentar
        private val imgLogoBanco: ImageView = view.findViewById(R.id.imgBancosMP)
        private val imgFotoPersona: ImageView = view.findViewById(R.id.imgFotoPersona)
        private val arrowImageView: ImageView = view.findViewById(R.id.arrowImageView)
        private val detailsLayout: View = view.findViewById(R.id.detailsLayout)
        private val checkBoxPredeterminado: CheckBox = view.findViewById(R.id.checActivo)
        private val editButton: ImageView = view.findViewById(R.id.editButton)
        private val deleteButton: ImageView = view.findViewById(R.id.deleteButton)
        private val botonCopiarPagoMovil: ImageButton = view.findViewById(R.id.copiarPagoMovil)

        fun bind(pagoMovil: DatosPMovilModel, position: Int) {
            radioTipo.text = pagoMovil.tipo
            checkBoxPredeterminado.isChecked = pagoMovil.seleccionado
            textViewNombre.text = pagoMovil.nombre
            textViewCedula.text = pagoMovil.cedula.toString()
            textViewTelefono.text = pagoMovil.tlf.toString()
            textViewBanco.text = pagoMovil.banco.toString()

            // Imagen personalizada de la persona
            if (!pagoMovil.imagen.isNullOrEmpty()) {
                Glide.with(context.requireContext())
                    .load(File(pagoMovil.imagen))
                    .placeholder(R.drawable.ic_little_person)
                    .into(imgFotoPersona)
            } else {
                imgFotoPersona.setImageResource(R.drawable.ic_little_person)
            }

            // Logo del banco
            when (pagoMovil.banco) {
                "0134 - Banesco" -> imgLogoBanco.setImageResource(R.drawable.banco_banesco_png)
                "0102 - Venezuela" -> imgLogoBanco.setImageResource(R.drawable.banco_venezuela_png)
                "0108 - Banco Provincial" -> imgLogoBanco.setImageResource(R.drawable.banco_provincial_png)
                "0114 - Bancaribe" -> imgLogoBanco.setImageResource(R.drawable.bancaribe)
                "0105 - Banco Mercantil" -> imgLogoBanco.setImageResource(R.drawable.banco_mercantil_png)
                "0163 - Banco del Tesoro" -> imgLogoBanco.setImageResource(R.drawable.banco_tesoro)
                "0175 - Banco Bicentenario" -> imgLogoBanco.setImageResource(R.drawable.banco_banesco_png)
                "0191 - Banco Nacional de Crédito (BNC)" -> imgLogoBanco.setImageResource(R.drawable.banco_bnc_png)
                "0172 - Bancamiga" -> imgLogoBanco.setImageResource(R.drawable.banco_bancamiga_png)
                "0171 - Banco Activo" -> imgLogoBanco.setImageResource(R.drawable.banco_activo)
                else -> imgLogoBanco.setImageResource(R.drawable.ic_instituciones_24)
            }

            val isExpanded = position == expandedPosition
            detailsLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
            arrowImageView.setImageResource(if (isExpanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down)

            arrowImageView.setOnClickListener {
                expandedPosition = if (isExpanded) -1 else position
                handler.post { notifyDataSetChanged() }
            }

            checkBoxPredeterminado.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    onCheckboxClick(position)
                }
            }

            editButton.setOnClickListener {
                onEditClick(activado(), position)
            }

            deleteButton.setOnClickListener {
                onDeleteClick(position)
            }

            botonCopiarPagoMovil.setOnClickListener {
                copiarPagoMovil(pagoMovil)
            }
        }

        private fun activado(): DatosPMovilModel {
            val imagenActual = pagosMoviles[adapterPosition].imagen
            return DatosPMovilModel(
                seleccionado = checkBoxPredeterminado.isChecked,
                tipo = radioTipo.text.toString(),
                nombre = textViewNombre.text.toString(),
                tlf = textViewTelefono.text.toString(),
                cedula = textViewCedula.text.toString(),
                banco = textViewBanco.text.toString(),
                fecha = Date().toString(),
                imagen = imagenActual
            )
        }

        private fun copiarPagoMovil(pagoMovil: DatosPMovilModel) {
            val context = itemView.context
            val sb = StringBuilder()

            // Construimos el texto a copiar
            sb.append("Pago Móvil\n")
            sb.append("Banco: ${pagoMovil.banco}\n")
            sb.append("Cédula: ${pagoMovil.cedula}\n")
            sb.append("Teléfono: ${pagoMovil.tlf}")

            // Opcional: Agregar el nombre si existe
            // if (!pagoMovil.nombre.isNullOrEmpty()) {
            //    sb.append("\nAlias: ${pagoMovil.nombre}")
            // }

            val textoFinal = sb.toString()
            val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)

            if (clipboard == null) {
                Toast.makeText(context, "No se pudo acceder al portapapeles", Toast.LENGTH_SHORT).show()
                return
            }

            try {
                val clip = ClipData.newPlainText("Datos Pago Móvil", textoFinal)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Datos copiados al portapapeles", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("CopiadoPM", "Error al copiar texto: ${e.message}")
                Toast.makeText(context, "Error al copiar los datos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}