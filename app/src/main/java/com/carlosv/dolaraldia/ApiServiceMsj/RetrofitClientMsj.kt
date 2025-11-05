package com.carlosv.dolaraldia.ApiServiceMsj

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitClientMsj {
    companion object {
        fun getClient(url: String): Retrofit {
            val retrofit = Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit
        }
    }

}