package com.carlosv.dolaraldia.ApiServiceMsj

import com.carlosv.dolaraldia.model.FCMBody
import com.carlosv.dolaraldia.model.FCMResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface IFCMApi {

    @Headers(
        "Content-Type:application/json",
        "Authorization:key=AAAA2wXihB0:APA91bFoI7_aBicijAri0XxnZxp5o34YXbMhxZTwpbqx5mB5w1xmL-hyzZGj6A1ESA0QKfukf2yY5wAxJRz_caKCzkYqHzZbENcao6FCmfZFkFeK7BUlJ6fx4fT8p2wVwl1wLv-wa8v0"
    )

    @POST("fcm/send")
    fun send(@Body body: FCMBody): Call<FCMResponse>


}