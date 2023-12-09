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
import com.carlosv.dolaraldia.ui.bancos.BancoModelAdap
import com.carlosv.menulateral.R

class OtrasPaginasAdapter(val context: Fragment, var otrosBancos: ArrayList<BancoModelAdap>): RecyclerView.Adapter<OtrasPaginasAdapter.OtrosBancosAdapterViewHolder>() {



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

        if (otroBanco.nombre == "airtm"){
            holder.imgLogo.setImageResource(R.drawable.logo_airtm_png)
            val color = otroBanco.Color
            mostrarFlecha(color,holder)
        }
        if (otroBanco.nombre == "zinli"){
            holder.imgLogo.setImageResource(R.drawable.banco_zinli_png)
            val color = otroBanco.Color
            mostrarFlecha(color,holder)
        }
        if (otroBanco.nombre == "amazon_gift_card"){
            holder.imgLogo.setImageResource(R.drawable.amazon_gif240x240)
            val color = otroBanco.Color
            mostrarFlecha(color,holder)
        }

        if (otroBanco.nombre == "binance"){
            holder.imgLogo.setImageResource(R.drawable.binance240x240)
            val color = otroBanco.Color
            mostrarFlecha(color,holder)
        }
        if (otroBanco.nombre == "cambios_r&a"){
            holder.imgLogo.setImageResource(R.drawable.logo_rya_png)
            val color = otroBanco.Color
            mostrarFlecha(color,holder)
        }

        if (otroBanco.nombre == "dolartoday"){
            holder.imgLogo.setImageResource(R.drawable.dolar_today240x240)
            val color = otroBanco.Color
            mostrarFlecha(color,holder)
        }

        if (otroBanco.nombre == "el_dorado"){
            holder.imgLogo.setImageResource(R.drawable.logo_eldorado_png)
            val color = otroBanco.Color
            mostrarFlecha(color,holder)
        }

        if (otroBanco.nombre == "mkambio"){
            holder.imgLogo.setImageResource(R.drawable.logo_mkambio_png)
            val color = otroBanco.Color
            mostrarFlecha(color,holder)
        }
        if (otroBanco.nombre == "paypal"){
            holder.imgLogo.setImageResource(R.drawable.paypal240x240)
            val color = otroBanco.Color
            mostrarFlecha(color,holder)
        }
        if (otroBanco.nombre == "monitor_dolar_venezuela"){
            holder.imgLogo.setImageResource(R.drawable.logo_monitordolarvenezuela_png)
            val color = otroBanco.Color
            mostrarFlecha(color,holder)
        }
        if (otroBanco.nombre == "petro"){
            holder.imgLogo.setImageResource(R.drawable.logo_petro_png)
            val color = otroBanco.Color
            mostrarFlecha(color,holder)
        }
        if (otroBanco.nombre == "skrill"){
            holder.imgLogo.setImageResource(R.drawable.skrill240x240)
            val color = otroBanco.Color
            mostrarFlecha(color,holder)
        }

        if (otroBanco.nombre == "yadio"){
            holder.imgLogo.setImageResource(R.drawable.logo_yadio_png)
            val color = otroBanco.Color
            mostrarFlecha(color,holder)
        }

        if (otroBanco.nombre == "syklo"){
            holder.imgLogo.setImageResource(R.drawable.logo_syklo_png)
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