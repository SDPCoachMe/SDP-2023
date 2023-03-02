package com.github.sdpcoachme;

import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.internal.maps.zzaa;
import com.google.android.gms.internal.maps.zzad;
import com.google.android.gms.internal.maps.zzag;
import com.google.android.gms.internal.maps.zzaj;
import com.google.android.gms.internal.maps.zzl;
import com.google.android.gms.internal.maps.zzo;
import com.google.android.gms.internal.maps.zzr;
import com.google.android.gms.internal.maps.zzx;
import com.google.android.gms.maps.internal.IGoogleMapDelegate;
import com.google.android.gms.maps.internal.ILocationSourceDelegate;
import com.google.android.gms.maps.internal.IProjectionDelegate;
import com.google.android.gms.maps.internal.IUiSettingsDelegate;
import com.google.android.gms.maps.internal.zzab;
import com.google.android.gms.maps.internal.zzaf;
import com.google.android.gms.maps.internal.zzah;
import com.google.android.gms.maps.internal.zzak;
import com.google.android.gms.maps.internal.zzam;
import com.google.android.gms.maps.internal.zzao;
import com.google.android.gms.maps.internal.zzaq;
import com.google.android.gms.maps.internal.zzas;
import com.google.android.gms.maps.internal.zzau;
import com.google.android.gms.maps.internal.zzaw;
import com.google.android.gms.maps.internal.zzay;
import com.google.android.gms.maps.internal.zzba;
import com.google.android.gms.maps.internal.zzbc;
import com.google.android.gms.maps.internal.zzbe;
import com.google.android.gms.maps.internal.zzbg;
import com.google.android.gms.maps.internal.zzbi;
import com.google.android.gms.maps.internal.zzbv;
import com.google.android.gms.maps.internal.zzd;
import com.google.android.gms.maps.internal.zzi;
import com.google.android.gms.maps.internal.zzn;
import com.google.android.gms.maps.internal.zzp;
import com.google.android.gms.maps.internal.zzt;
import com.google.android.gms.maps.internal.zzv;
import com.google.android.gms.maps.internal.zzz;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;

public class RealGoogleMap implements IGoogleMapDelegate {
    @Override
    public float getMaxZoomLevel() throws RemoteException {
        return 0;
    }

    @Override
    public float getMinZoomLevel() throws RemoteException {
        return 0;
    }

    @Override
    public int getMapType() throws RemoteException {
        return 0;
    }

    @NonNull
    @Override
    public Location getMyLocation() throws RemoteException {
        return null;
    }

    @NonNull
    @Override
    public IProjectionDelegate getProjection() throws RemoteException {
        return null;
    }

    @NonNull
    @Override
    public IUiSettingsDelegate getUiSettings() throws RemoteException {
        return null;
    }

    @NonNull
    @Override
    public CameraPosition getCameraPosition() throws RemoteException {
        return null;
    }

    @Override
    public void animateCamera(@NonNull IObjectWrapper iObjectWrapper) throws RemoteException {

    }

    @Override
    public void clear() throws RemoteException {

    }

    @Override
    public void moveCamera(@NonNull IObjectWrapper iObjectWrapper) throws RemoteException {

    }

    @Override
    public void onCreate(@NonNull Bundle bundle) throws RemoteException {

    }

    @Override
    public void onDestroy() throws RemoteException {

    }

    @Override
    public void onEnterAmbient(@NonNull Bundle bundle) throws RemoteException {

    }

    @Override
    public void onExitAmbient() throws RemoteException {

    }

    @Override
    public void onLowMemory() throws RemoteException {

    }

    @Override
    public void onPause() throws RemoteException {

    }

    @Override
    public void onResume() throws RemoteException {

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle bundle) throws RemoteException {

    }

    @Override
    public void onStart() throws RemoteException {

    }

    @Override
    public void onStop() throws RemoteException {

    }

    @Override
    public void resetMinMaxZoomPreference() throws RemoteException {

    }

    @Override
    public void setBuildingsEnabled(boolean b) throws RemoteException {

    }

    @Override
    public void setContentDescription(String s) throws RemoteException {

    }

    @Override
    public void setLatLngBoundsForCameraTarget(LatLngBounds latLngBounds) throws RemoteException {

    }

    @Override
    public void setLocationSource(ILocationSourceDelegate iLocationSourceDelegate) throws RemoteException {

    }

    @Override
    public void setMapType(int i) throws RemoteException {

    }

    @Override
    public void setMaxZoomPreference(float v) throws RemoteException {

    }

    @Override
    public void setMinZoomPreference(float v) throws RemoteException {

    }

    @Override
    public void setMyLocationEnabled(boolean b) throws RemoteException {

    }

    @Override
    public void setPadding(int i, int i1, int i2, int i3) throws RemoteException {

    }

    @Override
    public void setTrafficEnabled(boolean b) throws RemoteException {

    }

    @Override
    public void setWatermarkEnabled(boolean b) throws RemoteException {

    }

    @Override
    public void stopAnimation() throws RemoteException {

    }

    @Override
    public boolean isBuildingsEnabled() throws RemoteException {
        return false;
    }

    @Override
    public boolean isIndoorEnabled() throws RemoteException {
        return false;
    }

    @Override
    public boolean isMyLocationEnabled() throws RemoteException {
        return false;
    }

    @Override
    public boolean isTrafficEnabled() throws RemoteException {
        return false;
    }

    @Override
    public boolean setIndoorEnabled(boolean b) throws RemoteException {
        return false;
    }

    @Override
    public boolean setMapStyle(MapStyleOptions mapStyleOptions) throws RemoteException {
        return false;
    }

    @Override
    public boolean useViewLifecycleWhenInFragment() throws RemoteException {
        return false;
    }

    @Override
    public zzl addCircle(CircleOptions circleOptions) throws RemoteException {
        return null;
    }

    @Override
    public zzo addGroundOverlay(GroundOverlayOptions groundOverlayOptions) throws RemoteException {
        return null;
    }

    @Override
    public zzr getFocusedBuilding() throws RemoteException {
        return null;
    }

    @Override
    public zzx getMapCapabilities() throws RemoteException {
        return null;
    }

    @Override
    public zzaa addMarker(MarkerOptions markerOptions) throws RemoteException {
        return null;
    }

    @Override
    public zzad addPolygon(PolygonOptions polygonOptions) throws RemoteException {
        return null;
    }

    @Override
    public zzag addPolyline(PolylineOptions polylineOptions) throws RemoteException {
        return null;
    }

    @Override
    public zzaj addTileOverlay(TileOverlayOptions tileOverlayOptions) throws RemoteException {
        return null;
    }

    @Override
    public void addOnMapCapabilitiesChangedListener(zzak zzak) throws RemoteException {

    }

    @Override
    public void animateCameraWithCallback(IObjectWrapper iObjectWrapper, zzd zzd) throws RemoteException {

    }

    @Override
    public void animateCameraWithDurationAndCallback(IObjectWrapper iObjectWrapper, int i, zzd zzd) throws RemoteException {

    }

    @Override
    public void getMapAsync(zzas zzas) throws RemoteException {

    }

    @Override
    public void removeOnMapCapabilitiesChangedListener(zzak zzak) throws RemoteException {

    }

    @Override
    public void setInfoWindowAdapter(zzi zzi) throws RemoteException {

    }

    @Override
    public void setOnCameraChangeListener(zzn zzn) throws RemoteException {

    }

    @Override
    public void setOnCameraIdleListener(zzp zzp) throws RemoteException {

    }

    @Override
    public void setOnCameraMoveCanceledListener(com.google.android.gms.maps.internal.zzr zzr) throws RemoteException {

    }

    @Override
    public void setOnCameraMoveListener(zzt zzt) throws RemoteException {

    }

    @Override
    public void setOnCameraMoveStartedListener(zzv zzv) throws RemoteException {

    }

    @Override
    public void setOnCircleClickListener(com.google.android.gms.maps.internal.zzx zzx) throws RemoteException {

    }

    @Override
    public void setOnGroundOverlayClickListener(zzz zzz) throws RemoteException {

    }

    @Override
    public void setOnIndoorStateChangeListener(zzab zzab) throws RemoteException {

    }

    @Override
    public void setOnInfoWindowClickListener(com.google.android.gms.maps.internal.zzad zzad) throws RemoteException {

    }

    @Override
    public void setOnInfoWindowCloseListener(zzaf zzaf) throws RemoteException {

    }

    @Override
    public void setOnInfoWindowLongClickListener(zzah zzah) throws RemoteException {

    }

    @Override
    public void setOnMapClickListener(zzam zzam) throws RemoteException {

    }

    @Override
    public void setOnMapLoadedCallback(zzao zzao) throws RemoteException {

    }

    @Override
    public void setOnMapLongClickListener(zzaq zzaq) throws RemoteException {

    }

    @Override
    public void setOnMarkerClickListener(zzau zzau) throws RemoteException {

    }

    @Override
    public void setOnMarkerDragListener(zzaw zzaw) throws RemoteException {

    }

    @Override
    public void setOnMyLocationButtonClickListener(zzay zzay) throws RemoteException {

    }

    @Override
    public void setOnMyLocationChangeListener(zzba zzba) throws RemoteException {

    }

    @Override
    public void setOnMyLocationClickListener(zzbc zzbc) throws RemoteException {

    }

    @Override
    public void setOnPoiClickListener(zzbe zzbe) throws RemoteException {

    }

    @Override
    public void setOnPolygonClickListener(zzbg zzbg) throws RemoteException {

    }

    @Override
    public void setOnPolylineClickListener(zzbi zzbi) throws RemoteException {

    }

    @Override
    public void snapshot(zzbv zzbv, IObjectWrapper iObjectWrapper) throws RemoteException {

    }

    @Override
    public void snapshotForTest(zzbv zzbv) throws RemoteException {

    }

    @Override
    public IBinder asBinder() {
        return null;
    }
}
