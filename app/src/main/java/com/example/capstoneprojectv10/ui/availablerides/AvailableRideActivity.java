package com.example.capstoneprojectv10.ui.availablerides;

import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.capstoneprojectv10.R;
import com.example.capstoneprojectv10.data.model.RideItem;
import com.example.capstoneprojectv10.databinding.ActivityAvailableRideListBinding;
import com.example.capstoneprojectv10.databinding.RecyclerviewAvailableRideTitleBinding;
import com.example.capstoneprojectv10.ui.availablerides.placeholder.PlaceholderContent;
import com.example.capstoneprojectv10.ui.map.DriverRouteActivity;
import com.example.capstoneprojectv10.ui.map.PassengerRouteActivity;
import com.google.common.reflect.TypeToken;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AvailableRideActivity extends AppCompatActivity {

    private ActivityAvailableRideListBinding activityBinding;
    private RecyclerviewAvailableRideTitleBinding titleBinding;
    private Button btnBack;
    private RecyclerView recyclerView;
    private AvailableRideRecyclerViewAdapter adapter;
    private List<RideItem> rides = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        activityBinding = ActivityAvailableRideListBinding.inflate(getLayoutInflater());
        titleBinding = RecyclerviewAvailableRideTitleBinding.inflate(getLayoutInflater());
        setContentView(activityBinding.getRoot());

        recyclerView = activityBinding.availableRidesList;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //adapter = new AvailableRideRecyclerViewAdapter(PlaceholderContent.ITEMS);
        //recyclerView.setAdapter(adapter);

        btnBack = titleBinding.btnBack;
        btnBack.setOnClickListener(this::goBack);

        // Parse passed JSON
        String json = getIntent().getStringExtra("rideList");
        if (json != null) {
            rides = new Gson().fromJson(json, new TypeToken<List<RideItem>>() {}.getType());

            // Now fetch driver profile pictures
            fetchDriverProfilesAndLoad();
        }
    }

    public void goBack(View view) {
        finish(); // Closes the activity when the back button is clicked
    }

    /*private void fetchDriverProfilesAndLoad() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Geocoder geocoder = new Geocoder(this);

        for (RideItem ride : rides) {
            db.collection("users")
                    .whereEqualTo("username", ride.driverName)
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (!snap.isEmpty()) {
                            DocumentSnapshot doc = snap.getDocuments().get(0);
                            ride.profileImageUrl = doc.getString("profileImageUrl");
                        }

                        try {
                            // Get departure name
                            List<Address> originAddresses = geocoder.getFromLocation(
                                    ride.origin.getLatitude(),
                                    ride.origin.getLongitude(),
                                    1
                            );
                            if (!originAddresses.isEmpty()) {
                                Address originAddress = originAddresses.get(0);
                                ride.departureName = originAddress.getAddressLine(0); // Full address
                            }

                            // Get destination name
                            List<Address> destAddresses = geocoder.getFromLocation(
                                    ride.destination.getLatitude(),
                                    ride.destination.getLongitude(),
                                    1
                            );
                            if (!destAddresses.isEmpty()) {
                                Address destAddress = destAddresses.get(0);
                                ride.destinationName = destAddress.getAddressLine(0); // Full address
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            ride.departureName = ride.origin.getLatitude() + ", " + ride.origin.getLongitude();
                            ride.destinationName = ride.destination.getLatitude() + ", " + ride.destination.getLongitude();
                        }

                        // Refresh adapter after loading profiles and addresses
                        adapter = new AvailableRideRecyclerViewAdapter(rides, rideItem -> {
                            Bundle args = new Bundle();
                            args.putDouble("origin_lat", rideItem.origin.getLatitude());
                            args.putDouble("origin_lng", rideItem.origin.getLongitude());
                            args.putDouble("dest_lat", rideItem.destination.getLatitude());
                            args.putDouble("dest_lng", rideItem.destination.getLongitude());

                            Intent intent = new Intent(this, PassengerRouteActivity.class);
                            intent.putExtras(args);
                            intent.putExtra("departure_name", rideItem.departureName);
                            intent.putExtra("destination_name", rideItem.destinationName);
                            intent.putExtra("departure_date", rideItem.rideDate);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        });
                        recyclerView.setAdapter(adapter);
                    });
        }
    }*/

    private void fetchDriverProfilesAndLoad() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Geocoder geocoder = new Geocoder(this);

        // Initialize adapter ONCE
        adapter = new AvailableRideRecyclerViewAdapter(rides, rideItem -> {
            if (rideItem.origin != null && rideItem.destination != null) {
                Bundle args = new Bundle();
                args.putDouble("origin_lat", rideItem.origin.getLatitude());
                args.putDouble("origin_lng", rideItem.origin.getLongitude());
                args.putDouble("dest_lat", rideItem.destination.getLatitude());
                args.putDouble("dest_lng", rideItem.destination.getLongitude());

                /*Intent intent = new Intent(this, PassengerRouteActivity.class);
                intent.putExtras(args);
                intent.putExtra("departure_name", rideItem.departureName);
                intent.putExtra("destination_name", rideItem.destinationName);
                intent.putExtra("departure_date", rideItem.rideDate);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);*/

                String rideId = rideItem.rideId; // if you have a rideId field, or null
                String passengerUsername = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("username", null);
                String driverUsername = rideItem.driverName;

                Map<String, Object> request = new HashMap<>();
                request.put("rideId", rideId != null ? rideId : "unknown");
                request.put("passengerUsername", passengerUsername);
                request.put("driverUsername", driverUsername);
                request.put("status", "pending");
                request.put("timestamp", System.currentTimeMillis());

                db.collection("ride_requests")
                        .add(request)
                        .addOnSuccessListener(docRef -> {
                            Toast.makeText(this, "Waiting for driver response...", Toast.LENGTH_SHORT).show();

                            // Start listening for response
                            db.collection("ride_requests").document(docRef.getId())
                                    .addSnapshotListener((snapshot, error) -> {
                                        if (error != null || snapshot == null || !snapshot.exists()) return;
                                        String status = snapshot.getString("status");
                                        if ("accepted".equals(status)) {
                                            Intent intent = new Intent(this, PassengerRouteActivity.class);
                                            intent.putExtras(args);
                                            intent.putExtra("rideId", rideItem.rideId);
                                            startActivity(intent);
                                        } else if ("rejected".equals(status)) {
                                            Toast.makeText(this, "Request rejected. Please try another ride.", Toast.LENGTH_LONG).show();
                                        }
                                    });
                        });
            }
        });

        recyclerView.setAdapter(adapter); // Set adapter only ONCE

        for (RideItem ride : rides) {
            db.collection("users")
                    .whereEqualTo("username", ride.driverName)
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (!snap.isEmpty()) {
                            DocumentSnapshot doc = snap.getDocuments().get(0);
                            ride.profileImageUrl = doc.getString("profileImageUrl");
                            adapter.notifyDataSetChanged(); // Update when image is fetched
                        }

                        try {
                            // Get departure name
                            List<Address> originAddresses = geocoder.getFromLocation(
                                    ride.origin.getLatitude(),
                                    ride.origin.getLongitude(),
                                    1
                            );
                            if (!originAddresses.isEmpty()) {
                                Address originAddress = originAddresses.get(0);
                                ride.departureName = originAddress.getAddressLine(0);
                                adapter.notifyDataSetChanged(); // Update after address fetch
                            }

                            // Get destination name
                            List<Address> destAddresses = geocoder.getFromLocation(
                                    ride.destination.getLatitude(),
                                    ride.destination.getLongitude(),
                                    1
                            );
                            if (!destAddresses.isEmpty()) {
                                Address destAddress = destAddresses.get(0);
                                ride.destinationName = destAddress.getAddressLine(0);
                                adapter.notifyDataSetChanged(); // Update after address fetch
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            ride.departureName = ride.origin.getLatitude() + ", " + ride.origin.getLongitude();
                            ride.destinationName = ride.destination.getLatitude() + ", " + ride.destination.getLongitude();
                            adapter.notifyDataSetChanged(); // Update fallback
                        }
                    });
        }
    }

    //Specify what happens onDestroy
    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
    }

    // Specify what happens onPause
    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    //Specify what happens onStop
    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }
}
