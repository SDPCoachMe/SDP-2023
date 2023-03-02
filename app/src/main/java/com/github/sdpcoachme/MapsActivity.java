package com.github.sdpcoachme;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.github.sdpcoachme.databinding.ActivityMapsBinding;

/**
 This class represents the MapsActivity that displays a Google Map.
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;

    // The coordinates of sat
    private static final LatLng SATELLITE = new LatLng(46.520544, 6.567825);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        setCamToEPFL(mMap);

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(@NonNull Marker marker) {
                LatLng markerPose = marker.getPosition();
                if (markerPose.latitude == SATELLITE.latitude
                        && markerPose.longitude == SATELLITE.longitude) {
                    Context context = getApplicationContext();
                    int duration = Toast.LENGTH_SHORT;
                    CharSequence text = "latitude: " + markerPose.latitude
                            + ", longitude: " + markerPose.longitude;
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                }
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                Integer cCount = (Integer) marker.getTag();
                assert cCount != null;
                marker.setTag(++cCount);
                if (cCount >= 5) {
                    Toast.makeText(getApplicationContext(),
                            marker.getTitle() + " clicked " + cCount + " times. Open info window",
                            Toast.LENGTH_LONG).show();
                }
                return false;
            }
        });

        // Limit the map zoom
        mMap.setMaxZoomPreference(20);
        mMap.setMinZoomPreference(10);

        Marker satMarker = mMap.addMarker(new MarkerOptions()
                .position(SATELLITE)
                .title("Satellite"));
        assert satMarker != null;
        satMarker.setTag(0);
    }

    /**
     Sets the camera position to the EPFL area
     @param mMap the GoogleMap object representing the map
     */
    private void setCamToEPFL(GoogleMap mMap) {
        mMap.moveCamera(CameraUpdateFactory.newLatLng(
                new LatLng(46.520536,6.568318)));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15));
    }
}