package com.github.sdpcoachme.container

import com.github.sdpcoachme.network.RequestPoolAPI
import com.github.sdpcoachme.utility.Constants
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AppContainer {

    //Create a retrofit object that will handle the connection to the api and covert them into JSon
    private val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    //create an object that can handle the request fromm the pool of commands
    val remoteDataSource: RequestPoolAPI = retrofit.create(RequestPoolAPI::class.java)
}