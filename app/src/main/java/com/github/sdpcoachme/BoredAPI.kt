package com.example.CoachMe

import retrofit2.Call
import retrofit2.http.GET

interface BoredAPI {
    @GET("activity")
    fun getActivity(): Call<DataFormat>
}