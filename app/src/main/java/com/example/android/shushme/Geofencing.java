package com.example.android.shushme;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;

import java.util.ArrayList;
import java.util.List;

public class Geofencing implements ResultCallback<Status> {
    private String TAG = Geofencing.class.getName();

    private Context context;
    private GoogleApiClient client;
    private List<Geofence> geofences;
    private PendingIntent geoIntent;

    public Geofencing(Context context, GoogleApiClient client) {
        this.context = context;
        this.client = client;

        geofences = new ArrayList<>();
        geoIntent = null;
    }

    public void updateGeofencesList(PlaceBuffer buffer) {
        geofences = new ArrayList<>();

        if (buffer == null || buffer.getCount() == 0) return;

        for (Place place : buffer) {
            String id = place.getId();
            double lat = place.getLatLng().latitude;
            double log = place.getLatLng().longitude;

            Geofence geofence = new Geofence.Builder()
                    .setRequestId(id)
                    .setExpirationDuration(86400000)
                    .setCircularRegion(lat, log, 50)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();

            geofences.add(geofence);
        }
    }

    public void registerAllGeofences() {
        if (client == null || !client.isConnected() || geofences == null || geofences.size() == 0) {
            return;
        }

        GeofencingRequest request = getGeofencingRequest();
        PendingIntent intent = getGeofencePendingIntent();

        try {
            LocationServices.GeofencingApi
                    .addGeofences(client, request, intent)
                    .setResultCallback(this);
        } catch (SecurityException ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    public void unregisterAllGeofences() {
        if (client == null || !client.isConnected()) {
            return;
        }

        PendingIntent intent = getGeofencePendingIntent();

        try {
            LocationServices.GeofencingApi
                    .removeGeofences(client, intent)
                    .setResultCallback(this);
        } catch (SecurityException ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofences);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if (geoIntent != null) {
            return geoIntent;
        }

        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        geoIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return geoIntent;
    }

    @Override
    public void onResult(@NonNull Status status) {
        Log.e(TAG, String.format("onResult [Geofencing]: %s", status));
    }
}
