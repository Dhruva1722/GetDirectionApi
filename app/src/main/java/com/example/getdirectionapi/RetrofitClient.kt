package com.example.getdirectionapi

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

class RetrofitClient {

    private var  BASE_URL = "https://trueway-directions2.p.rapidapi.com/"

//    private var BASE_URL = "https://api.thecatapi.com/"


    fun create(): DirectionsApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(DirectionsApiService::class.java)
    }
}

