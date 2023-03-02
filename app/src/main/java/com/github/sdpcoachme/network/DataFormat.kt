package com.github.sdpcoachme.network

data class DataFormat(
        val activity: String,
        val type : String,
        val participants : Int,
        val price : Double,
        val link : String,
        val key: Int,
        val accessibility : Double
)
