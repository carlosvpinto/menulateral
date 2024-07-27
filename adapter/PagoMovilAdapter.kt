package com.carlosv.dolaraldia.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.carlosv.dolaraldia.model.datosPMovil.DatosPMovilModel
import com.carlosv.menulateral.R
import java.util.Date
// Importar Handler y Looper
import android.os.Handler
import android.os.Looper

class PagoMovilAdapter(
    val context: Fragment,
    var pagosMoviles: ArrayList<DatosPMovilModel>,
    private val onEditClick: (DatosPMovilModel, Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit,
    private val onCheckboxClick: (Int) -> Unit
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

        Log.d("ADAPTER", "DENTRO updatePrecioBancos precionBancosList $precionBancosList")
        this.pagosMoviles = ArrayList(precionBancosList)

        // Usar Handler para llamar notifyDataSetChanged() en el hilo principal
        handler.post {
            notifyDataSetChanged()
        }
    }


    inner class PagoMovilAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val radioTipo: TextView= view.findViewById(R.id.txtTipo)
        private val textViewNombre: TextView = view.findViewById(R.id.txtNombrePM)
        private val textViewCedula: TextView = view.findViewById(R.id.txtCedulaPM)
        private val textViewTelefono: TextView = view.findViewById(R.id.txtTelPM)
        private val textViewBanco: TextView = view.findViewById(R.id.txtNombreBancoPM)
        private val cardView: CardView = view.findViewById(R.id.cardViewPM)
        private val imgLogo: ImageView = view.findViewById(R.id.imgBancosMP)
        private val arrowImageView: ImageView = view.findViewById(R.id.arrowImageView)
        private val detailsLayout: View = view.findViewById(R.id.detailsLayout)
        private val checkBoxPredeterminado: CheckBox = view.findViewById(R.id.checActivo)
        private val editButton: ImageView = view.findViewById(R.id.editButton)
        private val deleteButton: ImageView = view.findViewById(R.id.deleteButton)

        fun bind(pagoMovil: DatosPMovilModel, position: Int) {
            radioTipo.text= pagoMovil.tipo
            checkBoxPredeterminado.isChecked= pagoMovil.seleccionado
            textViewNombre.text = pagoMovil.nombre
            textViewCedula.text = pagoMovil.cedula.toString()
            textViewTelefono.text = pagoMovil.tlf.toString()
            textViewBanco.text = pagoMovil.banco.toString()



            if (pagoMovil.banco == "0134 - Banesco") {
                imgLogo.setImageResource(R.drawable.banco_banesco_png)
            }
            if (pagoMovil.banco == "0102 - Venezuela") {
                imgLogo.setImageResource(R.drawable.banco_venezuela_png)
            }
            if (pagoMovil.banco ==  "0108 - Banco Provincial") {
                imgLogo.setImageResource(R.drawable.banco_provincial_png)
            }
            if (pagoMovil.banco == "0114 - Bancaribe") {
                imgLogo.setImageResource(R.drawable.bancaribe)
            }
            if (pagoMovil.banco ==  "0105 - Banco Mercantil") {
                imgLogo.setImageResource(R.drawable.banco_mercantil_png)
            }
            if (pagoMovil.banco ==  "0163 - Banco del Tesoro") {
                imgLogo.setImageResource(R.drawable.banco_tesoro)
            }
            if (pagoMovil.banco ==  "0175 - Banco Bicentenario") {
                imgLogo.setImageResource(R.drawable.banco_banesco_png)
            }
            if (pagoMovil.banco ==  "0191 - Banco Nacional de Crédito (BNC)") {
                imgLogo.setImageResource(R.drawable.banco_bnc_png)
            }
            if (pagoMovil.banco ==   "0172 - Bancamiga") {
                imgLogo.setImageResource(R.drawable.banco_bancamiga_png)
            }
            if (pagoMovil.banco ==   "0171 - Banco Activo") {
                imgLogo.setImageResource(R.drawable.banco_activo)
            }


            val isExpanded = position == expandedPosition
            detailsLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
            arrowImageView.setImageResource(if (isExpanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down)

            arrowImageView.setOnClickListener {
                expandedPosition = if (isExpanded) -1 else position
                // Usar Handler para llamar notifyDataSetChanged()
                handler.post {
                    notifyDataSetChanged()
                }
            }

            // Lógica para manejar el evento de clic en el CheckBox

            checkBoxPredeterminado.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    onCheckboxClick(position)
                }
            }
//            checkBox.setOnCheckedChangeListener { _, isChecked ->
//
//                Log.d("ADAPTER", "CheckBox at position $position is ${if (isChecked) "checked" else "unchecked"}")
//            }

            editButton.setOnClickListener {


                onEditClick(activado(), position)
            }

            deleteButton.setOnClickListener {
                onDeleteClick(position)
            }
        }
        private fun activado():DatosPMovilModel{

            return DatosPMovilModel(
                    seleccionado = checkBoxPredeterminado.isChecked,
                  //  tipo =  radioTipo.text.toString(),
                    nombre =textViewNombre.text.toString(),
                    tlf =  textViewTelefono.text.toString(),
                    cedula =  textViewCedula.text.toString(),
                    banco = textViewBanco.text.toString(),
                    fecha = Date().toString()
            )
        }

    }
}
