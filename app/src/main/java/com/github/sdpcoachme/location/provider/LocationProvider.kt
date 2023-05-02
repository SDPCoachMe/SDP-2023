package com.github.sdpcoachme.location.provider

import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import com.github.sdpcoachme.data.UserInfo
import com.google.android.gms.maps.model.LatLng
import java.util.concurrent.CompletableFuture

interface LocationProvider {

    fun checkLocationSetting()

    fun locationIsPermitted(): Boolean

    fun requestPermission()

    fun getLastLocation(): MutableState<LatLng?>

    fun init(context: ComponentActivity, userInfo: CompletableFuture<UserInfo>)
}