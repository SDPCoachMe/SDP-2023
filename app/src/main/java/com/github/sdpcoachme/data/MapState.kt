package com.github.sdpcoachme.data

import com.google.android.gms.maps.model.LatLng

/**
 * Holds the last known user location as LatLng type.
 */
data class MapState(val lastKnownLocation: LatLng?)
