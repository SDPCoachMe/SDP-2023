package com.github.sdpcoachme.location

import com.github.sdpcoachme.data.UserLocation

class UserLocationSamples {
    companion object {
        val PARIS = UserLocation(
            placeId = "ChIJD7fiBh9u5kcRYJSMaMOCCwQ",
            address = "Paris, France",
            latitude = 48.856614,
            longitude = 2.3522219
        )
        val LONDON = UserLocation(
            placeId = "ChIJdd4hrwug2EcRmSrV3Vo6llI",
            address = "London, UK",
            latitude = 51.5073509,
            longitude = -0.1277583
        )
        val NEW_YORK = UserLocation(
            placeId = "ChIJOwg_06VPwokRYv534QaPC8g",
            address = "New York, NY, USA",
            latitude = 40.7127281,
            longitude = -74.0060152
        )
        val TOKYO = UserLocation(
            placeId = "ChIJ51cu8IcbXWARiRtXIothAS4",
            address = "Tokyo, Japan",
            latitude = 35.6894875,
            longitude = 139.6917064
        )
        val SYDNEY = UserLocation(
            placeId = "ChIJP3Sa8ziYEmsRUKgyFmh9AQM",
            address = "Sydney, Australia",
            latitude = -33.8688197,
            longitude = 151.2092955
        )
        val LAUSANNE = UserLocation(
            placeId = "ChIJ5aeJzT4pjEcRXu7iysk_F-s",
            address = "Lausanne, Switzerland",
            latitude = 46.5196535,
            longitude = 6.6335972
        )
    }
}