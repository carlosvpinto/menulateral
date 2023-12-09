package com.carlosv.dolaraldia.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
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
       // holder.imgflecha.setImageResource(R.drawable.ic_flechaverde)
        Log.d("ADAPTER", " otroBanco.nombre ${otroBanco.nombre} ")

        if (otroBanco.nombre == "Bancamiga"){
            holder.imgLogo.setImageResource(R.drawable.banco_bancamiga_png)
            val color = otroBanco.Color
            mostrarFlecha(color,holder)
        }
        if (otroBanco.nombre == "BCV"){
            holder.imgLogo.setImageResource(R.drawable.bcv240x240)
            val color = otroBanco.Color
            mostrarFlecha(color,holder)
        }

        if (otroBanco.nombre == "Banesco"){
            holder.imgLogo.setImageResource(R.drawable.banco_banesco_png)
            val color = otroBanco.Color
            mostrarFlecha(color,holder)
        }
        if (otroBanco.nombre == "Banco Provincial"){
            holder.imgLogo.setImageResource(R.drawable.banco_provincial_png)
            val color = otroBanco.Color
            mostrarFlecha(color,holder)
        }
        if (otroBanco.nombre == "BNC"){
            holder.imgLogo.setImageResource(R.drawable.banco_bnc_png)
            val color = otroBanco.Color
            mostrarFlecha(color,holder)
        }
        if (otroBanco.nombre == "Mercantil"){
            holder.imgLogo.setImageResource(R.drawable.banco_mercantil_png)
            val color = otroBanco.Color
            mostrarFlecha(color,holder)
        }

        if (otroBanco.nombre == "banco_de_venezuela" ||otroBanco.nombre == "Banco de Venezuela"){
            holder.imgLogo.setImageResource(R.drawable.banco_venezuela_png)
            val color = otroBanco.Color
            mostrarFlecha(color,holder)
        }
        if (otroBanco.nombre == "zinli"){
            holder.imgLogo.setImageResource(R.drawable.banco_zinli_png)
            val color = otroBanco.Color
            mostrarFlecha(color,holder)
        }
        if (otroBanco.nombre == "italcambio"){
            holder.imgLogo.setImageResource(R.drawable.banco_italcambio_png)
            val color = otroBanco.Color
            mostrarFlecha(color,holder)
        }
        if (otroBanco.nombre == "remesas_zoom"){
            holder.imgLogo.setImageResource(R.drawable.banco_remesas_zoom)
            val color = otroBanco.Color
            mostrarFlecha(color,holder)
        }


        // holder.itemView.setOnClickListener { goToDetail(pagoMovil?.id!!) } //para no llamar al activity al gacer click
    }
    fun mostrarFlecha(color:String,holder: OtrosBancosAdapterViewHolder){
        val contexto = holder.itemView.context
        if (color== "green"){
            holder.imgflecha.setImageResource(R.drawable.ic_flechaverde)
            holder.textViewVariacion.setTextColor(ContextCompat.getColor(contexto,R.color.green))
        }
        if (color== "red"){
            holder.imgflecha.setImageResource(R.drawable.ic_flecha_roja)
            holder.textViewVariacion.setTextColor(ContextCompat.getColor(contexto,R.color.red))
        }
        if (color== "neutral"){
            holder.imgflecha.setImageResource(R.drawable.ic_flecha_igual)
            holder.textViewVariacion.setTextColor(ContextCompat.getColor(contexto,R.color.black))
        }
    }


    // EL TAMAñO DE LA LISTA QUE VAMOS A MOSTRAR
    override fun getItemCount(): Int {
        return otrosBancos.size
    }
    fun updatePrecioBancos(precionBancosList: List<BancoModelAdap> ){
        Log.d("ADAPTER", " DENTRO updatePrecioBancos precionBancosList $precionBancosList ")
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