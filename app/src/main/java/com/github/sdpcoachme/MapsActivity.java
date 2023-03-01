package com.github.sdpcoachme;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.github.sdpcoachme.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private static final LatLng SATELLITE = new LatLng(46.520544, 6.567825);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);


    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.moveCamera(CameraUpdateFactory.newLatLng(
                new LatLng(46.520536,6.568318)));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(18));
        /*mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(@NonNull Marker marker) {
                LatLng markerPose = marker.getPosition();
                if (markerPose.latitude == SATELLITE.latitude
                        && markerPose.longitude == SATELLITE.longitude) {
                    Context context = getApplicationContext();
                    int duration = Toast.LENGTH_LONG;
                    CharSequence text = "latitude: " + markerPose.latitude
                            + ", longitude: " + markerPose.longitude;
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                }
            }
        });*/

        Marker satMarker = mMap.addMarker(new MarkerOptions()
                .position(SATELLITE)
                .title("Sat marker title"));
        assert satMarker != null;
        satMarker.showInfoWindow();
    }
}