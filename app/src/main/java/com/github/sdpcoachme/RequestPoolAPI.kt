package com.github.sdpcoachme

import retrofit2.Call
import retrofit2.http.GET

interface RequestPoolAPI {
    @GET("activity")
    fun getActivity(): Call<DataFormat>
}