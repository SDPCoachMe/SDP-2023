package com.github.sdpcoachme

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

class MapsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CoachMeTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    MapPerspective(this)
                }
            }
        }
    }
}

@Composable
fun MapPerspective(context: Context) {
    val satellite = LatLng(46.520544, 6.567825)
    val campus = LatLng(46.520536,6.568318)
    val camStartPosition = rememberCameraPositionState() {
        position = CameraPosition.fromLatLngZoom(campus, 15f)
    }

    var markerClickCnt by remember { mutableStateOf(0) }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = camStartPosition,
        properties = MapProperties(
            maxZoomPreference = 20f,
            minZoomPreference = 10f,
            mapType = MapType.SATELLITE
        )
    ) {
        Marker(
            state = MarkerState(position = satellite),
            title = "Satellite",
            snippet = "Marker at Satellite",
            onClick = {
                if (++markerClickCnt >= 5) {
                    Toast.makeText(context, "You clicked ${markerClickCnt} times", Toast.LENGTH_SHORT).show()
                }
                false
            },
            onInfoWindowClick = { _ ->
                val toast = Toast.makeText(context,"Latitude: ${satellite.latitude}, Longitude: ${satellite.longitude}",Toast.LENGTH_LONG)
                toast.show()
            }
        )
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CoachMeTheme {
        MapPerspective(LocalContext.current)
    }
}