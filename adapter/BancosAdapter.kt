package com.carlosv.dolaraldia.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.carlosv.dolaraldia.model.monedas.OtrasPaginas
import com.carlosv.dolaraldia.ui.bancos.BancoModelAdap
import com.carlosv.menulateral.R

class BancosAdapter(val context: Fragment, var otrosBancos: ArrayList<BancoModelAdap>): RecyclerView.Adapter<BancosAdapter.OtrosBancosAdapterViewHolder>() {



    private var itemCount: Int = 0 // variable para almacenar la cantidad de elementos en la lista

    init {
        var totalBs = 0.0
        var totalDollar= 0.0
        var totalSinVeriBs = 0.0
        var totalSinVeriBsDollar = 0.0


        Log.d("RESPUESTA", "otrasPaginas.size: ${otrosBancos.size} ")
       // val textView = context.findViewById<TextView>(R.id.txtRespuesta)

        itemCount = otrosBancos.size
    }
//**************************************************************

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OtrosBancosAdapterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.cardview_otras_bancos, parent, false)
        return OtrosBancosAdapterViewHolder(view)
    }

    // ESTABLECER LA INFORMACION
    override fun onBindViewHolder(holder: OtrosBancosAdapterViewHolder, position: Int) {
        val otroBanco =   otrosBancos[position] // UN SOLO HISTORIAL
        holder.textViewFechaActu.text = otroBanco.last_update
        holder.textViewMontoBs.text = otroBanco.precio.toString()
        holder.textViewNombreBanco.text = otroBanco.nombre
        holder.textViewVariacion.text = otroBanco.diferencia.toString()
        holder.imgflecha.setImageResource(R.drawable.ic_flechaverde)


        if (otroBanco.nombre == "bancamiga"){
            holder.imgLogo.setImageResource(R.drawable.banco_bancamiga_png)
        }
        if (otroBanco.nombre == "BCV"){
            holder.imgLogo.setImageResource(R.drawable.bcv240x240)
        }

        if (otroBanco.nombre == "banesco"){
            holder.imgLogo.setImageResource(R.drawable.banco_banesco_png)
        }
        if (otroBanco.nombre == "bbva_provincial"){
            holder.imgLogo.setImageResource(R.drawable.banco_provincial_png)
        }
        if (otroBanco.nombre == "bnc"){
            holder.imgLogo.setImageResource(R.drawable.banco_bnc_png)
        }
        if (otroBanco.nombre == "mercantil"){
            holder.imgLogo.setImageResource(R.drawable.banco_mercantil_png)
        }


        // holder.itemView.setOnClickListener { goToDetail(pagoMovil?.id!!) } //para no llamar al activity al gacer click
    }


    // EL TAMAñO DE LA LISTA QUE VAMOS A MOSTRAR
    override fun getItemCount(): Int {
        return otrosBancos.size
    }
    fun updatePrecioBancos(precionBancosList: List<BancoModelAdap> ){
        Log.d("RESPUESTA", " DENTRO updatePrecioBancos precionBancosList $precionBancosList ")
        this.otrosBancos = precionBancosList as ArrayList<BancoModelAdap>
        notifyDataSetChanged()
    }

    class OtrosBancosAdapterViewHolder(view: View): RecyclerView.ViewHolder(view) {

        val textViewFechaActu: TextView
        val textViewMontoBs: TextView
        val textViewNombreBanco: TextView
        val textViewVariacion: TextView
        val cardView: CardView // Nueva referencia a la CardView
        val imgLogo: ImageView
        val imgflecha: ImageView

        init {
            textViewFechaActu = view.findViewById(R.id.txtFechaActualizacionBanco)
            textViewMontoBs = view.findViewById(R.id.txtPrecioBsBanco)
            textViewVariacion = view.findViewById(R.id.txtVariacion)
            imgLogo = view.findViewById(R.id.imgBancos)
            textViewNombreBanco = view.findViewById(R.id.txtNombreBanco)
            cardView = view.findViewById(R.id.cardView) // Inicializar la referencia a la CardView
            imgflecha= view.findViewById(R.id.imgfechabanco)


        }
    }

}