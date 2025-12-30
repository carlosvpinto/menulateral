package com.carlosv.dolaraldia.utils

/****
 * Project: Food
 * From: com.cursosant.food.utils
 * Created by Alain Nicolás Tello on 03/05/24 at 19:27
 * All rights reserved 2024.
 *
 * All my Udemy Courses:
 * https://www.udemy.com/user/alain-nicolas-tello/
 * And Frogames formación:
 * https://cursos.frogamesformacion.com/pages/instructor-alain-nicolas
 *
 * Coupons on my Website:
 * www.alainnicolastello.com
 ***/
object Constants {
    const val ARG_PRICE = "arg_price"
    const val PAYPAL_URL = "https://carlosvicentep.sg-host.com/"
    const val STATUS_SUCCESS = "21"
    const val JS_ANDROID = "Android"
    const val URL_BASE = "https://api.dolaraldiavzla.com/api/v1/"
    const val ENDPOINT = "tipo-cambio"

    const val BEARER_TOKEN = "Bearer 2x9Qjpxl5F8CoKK6T395KA"
    const val TOKEN_AS21 =
        "ccNt36cjSi2uOxgIMJxvt7:APA91bHqmTKTBF1pNWVLoBMJcH6ll43eeUuhS74PXIHxk7GbQ4l5-BKKoEMDJxjdOKQWEQ50Wc2AVxD8f2RHr47ELqiuw7_k4VuvxYCgZkeD6wQUGZgkVt4"
    const val URL_DESCARGA = "https://www.dolaraldiavzla.com/descarga/"

    const val PRICE_USD = "PRECIO_DOLLARS" // Clave para el precio en Dólares
    const val PRICE_BS = "PRECIO_BOLIVARES"

    //PARA USAR EN PAGOS MOVIL
    const val BANK_CODE = "0105" // Código de Banco Mercantil
    const val RIF = "J5068188172"   // RIF sin guiones ni espacios
    const val PHONE = "04243454032" // Teléfono sin guiones ni espacios
    const val EXTRA_AMOUNT_TO_PAY = "AMOUNT_TO_PAY_BS"
    const val SUBSCRIPTION_PLAN_NAME = "SUBSCRIPTION_PLAN_NAME"

    const val MOSTRAR_PRIMER_DIALOGO = 9 // El primer diálogo aparecerá en la 5ta publicidad
    const val MOSTRAR_DESPUES_NRO_VECES = 20 // Los siguientes diálogos, cada 20 publicidades

    const val DEBUG_PASSWORD =
        "12901924"     // Contraseña para acceder a la pantalla de depuración.

    // --- CLAVES PARA LA LÓGICA DEL PERMISO DE NOTIFICACIONES ---
    const val CONTEO_INICIOS_TRAS_DENEGAR_PERMISO = "conteoIniciosTrasDenegarPermiso"
    const val UMBRAL_RECORDATORIO_PERMISO = 15 // Mostrar recordatorio cada 15 inicios


    //const val AD_UNIT_ID_RECOMPENSA = "ca-app-pub-3940256099942544/5224354917" // PRUEBA DESARROLLO
    const val AD_UNIT_ID_RECOMPENSA = "ca-app-pub-3265312813580307/5061990761" //PRODUCCION
    //const val AD_UNIT_ID_OPEN: String = "ca-app-pub-3940256099942544/9257395921" //Para desarrollo y Pruebas
    const val AD_UNIT_ID_OPEN: String  = "ca-app-pub-3265312813580307/7449206999" // Admob Dolarmexico 2

    // Reducimos el tiempo de espera a 2.5 segundos (Equilibrio entre cargar anuncio y UX)
    const val MAX_WAIT_TIME = 4000L

    // para que se muestre un anuncio al volver. 20 minutos = 20 * 60 * 1000 = 1,200,000 milisegundos.
    const val MIN_BACKGROUND_TIME_MS = 120_000L

    //DESTINOS AL FRAGMNET**********************

    const val CALCULADORA = "calculadora"
    const val PAGOMOVIL = "pagomovil"
    const val BANCOS = "bancos"
    const val GRAFICOS = "graficos"
    const val PLATFORMAS = "plataformas"

}