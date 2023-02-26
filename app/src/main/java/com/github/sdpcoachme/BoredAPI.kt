package com.github.sdpcoachme

import com.github.sdpcoachme.DataFormat
import retrofit2.Call
import retrofit2.http.GET

interface BoredAPI {
    @GET("activity")
    fun getActivity(): Call<DataFormat>
}