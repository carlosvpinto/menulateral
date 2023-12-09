package com.carlosv.dolaraldia.ui.bancos

import com.carlosv.dolaraldia.model.monedas.Airtm
import com.carlosv.dolaraldia.model.monedas.AmazonGiftCard
import com.google.gson.annotations.SerializedName

data class Monitors(
    var airtm: Airtm,
    var amazon_gift_card: AmazonGiftCard,
    var bancamiga: Bancamiga,
    var banco_de_venezuela: BancoDeVenezuela,
    var banesco: Banesco,
    var bbva_provincial: BbvaProvincial,
    var bcv: Bcv,
    var binance: Binance,
    var bnc: Bnc,
    @SerializedName("cambios_r&a")  var cambios_ra: CambiosRa,
    var dolar_em: DolarEm,
    var dolartoday: Dolartoday,
    @SerializedName("dolartoday_(btc)") var dolartoday_btc: DolartodayBtc,
    var el_dorado: ElDorado,
    var enparalelovzla: Enparalelovzla,
    var italcambio: Italcambio,
    var mercantil: Mercantil,
    var mkambio: Mkambio,
    var monitor_dolar_venezuela: MonitorDolarVenezuela,
    var monitor_dolar_vzla: MonitorDolarVzla,
    var otras_instituciones: OtrasInstituciones,
    var paypal: Paypal,
    var petro: Petro,
    var remesas_zoom: RemesasZoom,
    var skrill: Skrill,
    var syklo: Syklo,
    var yadio: Yadio,
    var zinli: Zinli
)