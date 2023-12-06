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
//import com.carlosv.dolaraldia.R
import com.carlosv.dolaraldia.model.monedas.OtrasPaginas
import com.carlosv.menulateral.R


private var totalDollarUpdate = 0.0

class OtrasPaginasAdapter(val context: Fragment, var otrasPaginas: ArrayList<OtrasPaginas>): RecyclerView.Adapter<OtrasPaginasAdapter.OtrasPaginasAdapterViewHolder>() {



    private var itemCount: Int = 0 // variable para almacenar la cantidad de elementos en la lista

    init {
        var totalBs = 0.0
        var totalDollar= 0.0
        var totalSinVeriBs = 0.0
        var totalSinVeriBsDollar = 0.0


        Log.d("RESPUESTA", "otrasPaginas.size: ${otrasPaginas.size} ")
       // val textView = context.findViewById<TextView>(R.id.txtRespuesta)

        itemCount = otrasPaginas.size
    }
//**************************************************************

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OtrasPaginasAdapterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cardview_otras_paginas, parent, false)
        return OtrasPaginasAdapterViewHolder(view)
    }

    // ESTABLECER LA INFORMACION
    override fun onBindViewHolder(holder: OtrasPaginasAdapterViewHolder, position: Int) {
        val otrasPagina =   otrasPaginas[position] // UN SOLO HISTORIAL
        holder.textViewFecha.text = otrasPagina.last_update
        holder.textViewMontoBs.text = otrasPagina.price.toString()

        holder.textViewNombrePagina.text = otrasPagina.title

        if (otrasPagina.title == "Amazon Gift Card"){
            holder.imgLogo.setImageResource(R.drawable.amazon_gif240x240)
        }
        if (otrasPagina.title == "BCV"){
            holder.imgLogo.setImageResource(R.drawable.bcv240x240)
        }

        if (otrasPagina.title == "Binance"){
            holder.imgLogo.setImageResource(R.drawable.binance240x240)
        }
        if (otrasPagina.title == "Cripto Dólar"){
            holder.imgLogo.setImageResource(R.drawable.cripto_dolar240x240)
        }
        if (otrasPagina.title == "Dólar Today"){
            holder.imgLogo.setImageResource(R.drawable.dolar_today240x240)
        }
        if (otrasPagina.title == "EnParaleloVzla"){
            holder.imgLogo.setImageResource(R.drawable.enparalelo240x240)
        }
        if (otrasPagina.title == "Paypal"){
            holder.imgLogo.setImageResource(R.drawable.paypal240x240)
        }
        if (otrasPagina.title == "Skrill"){
            holder.imgLogo.setImageResource(R.drawable.skrill240x240)
        }
        if (otrasPagina.title == "Uphold"){
            holder.imgLogo.setImageResource(R.drawable.uphold240x240)
        }

        // holder.itemView.setOnClickListener { goToDetail(pagoMovil?.id!!) } //para no llamar al activity al gacer click
    }


    // EL TAMAñO DE LA LISTA QUE VAMOS A MOSTRAR
    override fun getItemCount(): Int {
        return otrasPaginas.size
    }
    fun updatePrecioPaginas(pagosRealizadosList: List<OtrasPaginas> ){
        Log.d("RESPUESTA", " DENTRO OTRAS PAGINAS ADAPTER pagosRealizadosList $pagosRealizadosList ")
        this.otrasPaginas = pagosRealizadosList as ArrayList<OtrasPaginas>
        notifyDataSetChanged()
    }

    class OtrasPaginasAdapterViewHolder(view: View): RecyclerView.ViewHolder(view) {

        val textViewFecha: TextView
        val textViewMontoBs: TextView
        val textViewNombrePagina: TextView
        val cardView: CardView // Nueva referencia a la CardView
        val imgLogo: ImageView

        init {
            textViewFecha = view.findViewById(R.id.txtFechaActualizacion)
            textViewMontoBs = view.findViewById(R.id.txtPrecioBs)
            imgLogo = view.findViewById(R.id.imgPagina)
            textViewNombrePagina = view.findViewById(R.id.txtNombrePagina)
            cardView = view.findViewById(R.id.cardView) // Inicializar la referencia a la CardView


        }
    }

}