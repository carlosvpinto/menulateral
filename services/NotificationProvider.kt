package com.carlosv.dolaraldia.services

import com.carlosv.dolaraldia.ApiServiceMsj.IFCMApi
import com.carlosv.dolaraldia.ApiServiceMsj.RetrofitClientMsj
import com.carlosv.dolaraldia.model.FCMBody
import com.carlosv.dolaraldia.model.FCMResponse
import com.carlosv.dolaraldia.ui.home.HomeFragment
import retrofit2.Call


class NotificationProvider {
    private val URL = "https://fcm.googleapis.com"

    fun sendNotification(body: FCMBody): Call<FCMResponse> {
        return RetrofitClientMsj.getClient(URL).create(IFCMApi::class.java).send(body)
    }

}