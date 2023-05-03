package com.github.sdpcoachme.location.provider

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.location.MapActivity
import com.google.android.gms.maps.model.LatLng
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CompletableFuture

/**
 * Test class for the FusedLocationProviderTest. Setting cannot be handled here, so most of the
 * flow stays private to the LocationProvider. Some tests here just verify that the function are
 * launched and run without errors.
 */
@RunWith(AndroidJUnit4::class)
class FusedLocationProviderTest {

    private val mockIntent = Intent(ApplicationProvider.getApplicationContext(), MapActivity::class.java)
    private val mockUserInfo = CompletableFuture<UserInfo>()

    @Test
    fun initGivesNullLocation() {
        ActivityScenario.launch<ComponentActivity>(mockIntent).use { scenario ->
            scenario.onActivity {
                val fusedLocationProvider = FusedLocationProvider()
                fusedLocationProvider.init(it, mockUserInfo)
                assertThat(fusedLocationProvider.getLastLocation().value, `is`(null as LatLng?))
            }
        }
    }

    @Test
    fun locationIsPermittedWorks() {
        ActivityScenario.launch<ComponentActivity>(mockIntent).use { scenario ->
            scenario.onActivity {
                val fusedLocationProvider = FusedLocationProvider()
                fusedLocationProvider.init(it, mockUserInfo)
                assertThat(fusedLocationProvider.locationIsPermitted(), `is`(true))
            }
        }
    }

    @Test
    fun requestPermissionWorks() {
        ActivityScenario.launch<ComponentActivity>(mockIntent).use { scenario ->
            scenario.onActivity {
                val fusedLocationProvider = FusedLocationProvider()
                fusedLocationProvider.init(it, mockUserInfo)
                fusedLocationProvider.requestPermission()
            }
        }
    }

    @Test
    fun checkLocationSettingWorks() {
        ActivityScenario.launch<ComponentActivity>(mockIntent).use { scenario ->
            scenario.onActivity {
                val fusedLocationProvider = FusedLocationProvider()
                fusedLocationProvider.init(it, mockUserInfo)
                fusedLocationProvider.checkLocationSetting()
            }
        }
    }

    @Test
    fun getLastLocationRetrievesLastLocation() {
        ActivityScenario.launch<ComponentActivity>(mockIntent).use { scenario ->
            scenario.onActivity {
                val fusedLocationProvider = FusedLocationProvider()
                fusedLocationProvider.init(it, mockUserInfo)
                fusedLocationProvider.getLastLocation()
            }
        }
    }
}