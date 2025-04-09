package com.example.capstoneprojectv10.ui.availablerides;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

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

import java.util.ArrayList;
import java.util.List;

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

    private void fetchDriverProfilesAndLoad() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (RideItem ride : rides) {
            db.collection("users")
                    .whereEqualTo("username", ride.driverName)
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (!snap.isEmpty()) {
                            DocumentSnapshot doc = snap.getDocuments().get(0);
                            ride.profileImageUrl = doc.getString("profileImageUrl");
                        }

                        // Refresh adapter after loading profiles
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
