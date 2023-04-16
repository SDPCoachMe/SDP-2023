package com.github.sdpcoachme.map

import android.Manifest.permission.ACCESS_FINE_LOCATION
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.github.sdpcoachme.map.MapActivity.TestTags.Companion.MAP
import com.github.sdpcoachme.data.MapState
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import com.google.android.gms.maps.model.LatLng
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for the Composable MapView. Unfortunately, the GoogleMap api for Jetpack Compose
 * does not provide testTags for Marker.
 */
@RunWith(AndroidJUnit4::class)
class MapTest {

    private val random = LatLng(42.0,42.0)

    @get: Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        ACCESS_FINE_LOCATION
    )

    private fun mapExistsAndShowsUpOnLocation(location: LatLng?) {
        composeTestRule.setContent {
            CoachMeTheme() {
                Map(
                    modifier = Modifier.fillMaxWidth(),
                    mapState = MapState(lastKnownLocation = location)
                )
            }
        }
        composeTestRule.onRoot().onChild().assertExists().assert(hasTestTag(MAP))
        composeTestRule.onNodeWithTag(MAP).assertExists().assertIsDisplayed()
    }

    @Test
    fun mapExistsAndShowsUpWithoutLocation() {
        mapExistsAndShowsUpOnLocation(location = null)
    }

    @Test
    fun mapExistsAndShowsUpWithLocation() {
        mapExistsAndShowsUpOnLocation(location = random)
    }

}