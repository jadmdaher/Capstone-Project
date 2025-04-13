package com.example.capstoneprojectv10.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;

import android.location.Location;

import java.util.HashMap;
import java.util.Map;

public class LocationTracker {

    private final FusedLocationProviderClient fusedLocationClient;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String username;
    private final String rideId;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final long intervalMillis = 2500; // update every 2.5 seconds
    private boolean tracking = false;

    public LocationTracker(Context context, String rideId, String username) {
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.rideId = rideId;
        this.username = username;
    }

    public void startTracking() {
        tracking = true;
        handler.post(updateTask);
    }

    public void stopTracking() {
        tracking = false;
        handler.removeCallbacks(updateTask);
    }

    private final Runnable updateTask = new Runnable() {
        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            if (!tracking) return;

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            uploadLocation(location);
                        }
                    });

            handler.postDelayed(this, intervalMillis);
        }
    };

    private void uploadLocation(Location location) {
        Map<String, Object> locData = new HashMap<>();
        locData.put("latitude", location.getLatitude());
        locData.put("longitude", location.getLongitude());

        db.collection("rides").document(rideId)
                .collection("locations")
                .document(username)
                .set(locData);
    }
}