package com.example.capstoneprojectv10.ui.map;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.example.capstoneprojectv10.MainActivity;
import com.example.capstoneprojectv10.R;
import com.example.capstoneprojectv10.databinding.ActivityDriverRouteBinding;
import com.example.capstoneprojectv10.utils.LocationTracker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class DriverRouteActivity extends AppCompatActivity implements OnMapReadyCallback {

    private LatLng originLatLng;
    private LatLng destLatLng;
    private ActivityDriverRouteBinding binding;
    private LinearLayout bottomSheet;
    private TextView tvDeparture;
    private TextView tvDestination;
    private Button btnCompleteRide;
    private LocationTracker locationTracker;
    private final Map<String, Marker> userMarkers = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        binding = ActivityDriverRouteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //setContentView(R.layout.activity_driver_route);

        bottomSheet = binding.bottomSheet;
        BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight(120);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        // Set ride details
        String departureName = getIntent().getStringExtra("departure_name");
        String destinationName = getIntent().getStringExtra("destination_name");

        tvDeparture = binding.tvDeparture;
        tvDestination = binding.tvDestination;
        btnCompleteRide = binding.btnCompleteRide;

        tvDeparture.setText(" " + departureName);
        tvDestination.setText(" " + destinationName);

        double originLat = getIntent().getDoubleExtra("origin_lat", 0);
        double originLng = getIntent().getDoubleExtra("origin_lng", 0);
        double destLat = getIntent().getDoubleExtra("dest_lat", 0);
        double destLng = getIntent().getDoubleExtra("dest_lng", 0);

        originLatLng = new LatLng(originLat, originLng);
        destLatLng = new LatLng(destLat, destLng);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null)
            mapFragment.getMapAsync(this);

        String driverUsername = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("username", null);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("ride_requests")
                .whereEqualTo("driverUsername", driverUsername)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null || snapshots.isEmpty()) return;

                    for (DocumentSnapshot snapshot : snapshots.getDocuments()) {
                        String passenger = snapshot.getString("passengerUsername");

                        new AlertDialog.Builder(this)
                                .setTitle("Ride Request")
                                .setMessage("Passenger " + passenger + " wants to join your ride.")
                                .setPositiveButton("Accept", (dialog, which) -> {
                                    db.collection("ride_requests").document(snapshot.getId())
                                            .update("status", "accepted")
                                            .addOnSuccessListener(unused -> {
                                                String rideId = snapshot.getString("rideId");
                                                String passengerUsername = snapshot.getString("passengerUsername");

                                                if (rideId != null && passengerUsername != null) {
                                                    db.collection("rides").document(rideId)
                                                            .get()
                                                            .addOnSuccessListener(rideDoc -> {
                                                                List<String> currentPassengers = (List<String>) rideDoc.get("passengers");

                                                                if (currentPassengers == null || !currentPassengers.contains(passengerUsername)) {
                                                                    db.collection("rides").document(rideId)
                                                                            .update("passengers", FieldValue.arrayUnion(passengerUsername))
                                                                            .addOnSuccessListener(v -> {
                                                                                Log.d("RideUpdate", "Passenger added to ride.");
                                                                            });
                                                                } else {
                                                                    Log.d("RideUpdate", "Passenger already in ride, skipping.");
                                                                }
                                                            });
                                                }
                                            });
                                })

                                .setNegativeButton("Reject", (dialog, which) -> {
                                    db.collection("ride_requests").document(snapshot.getId())
                                            .update("status", "rejected");
                                })
                                .setCancelable(false)
                                .show();
                    }
                });

        // Retrieve rideId from intent
        String rideId = getIntent().getStringExtra("rideId");
        String username = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("username", null);
        locationTracker = new LocationTracker(this, rideId, username);
        locationTracker.startTracking();

        btnCompleteRide.setOnClickListener(v -> {
            if (rideId != null) {
                db.collection("rides").document(rideId)
                        .update("status", "complete")
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(this, "Ride complete!", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(this, MainActivity.class); // or HomeActivity if you have one
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish(); // Optional redundancy for extra safety
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to complete ride.", Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(this, "Ride ID not found.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        googleMap.addMarker(new MarkerOptions().position(originLatLng).title("Origin").icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromVectorDrawable(R.drawable.marker_circle_green))));
        googleMap.addMarker(new MarkerOptions().position(destLatLng).title("Destination").icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromVectorDrawable(R.drawable.marker_circle_red))));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(originLatLng, 15f));

        // Fetch and draw the route
        fetchRoute(googleMap);

        String rideId = getIntent().getStringExtra("rideId");
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("rides").document(rideId)
                .collection("locations")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String user = doc.getId();
                        Double lat = doc.getDouble("latitude");
                        Double lng = doc.getDouble("longitude");
                        if (lat == null || lng == null) continue;

                        LatLng userLocation = new LatLng(lat, lng);

                        db.collection("users")
                                .whereEqualTo("username", user)
                                .get()
                                .addOnSuccessListener(querySnapshot  -> {
                                    String role = querySnapshot.getDocuments().get(0).getString("role");
                                    int iconRes = "driver".equalsIgnoreCase(role) ? R.drawable.marker_car : R.drawable.marker_circle_purple;
                                    BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(getBitmapFromVectorDrawable(iconRes));

                                    if (userMarkers.containsKey(user)) {
                                        userMarkers.get(user).setPosition(userLocation);
                                    } else {
                                        Marker marker = googleMap.addMarker(
                                                new MarkerOptions()
                                                        .position(userLocation)
                                                        .title("@" + user)
                                                        .icon(icon)
                                        );
                                        userMarkers.put(user, marker);
                                    }
                                });
                    }
                });
    }

    private void fetchRoute(GoogleMap map) {
        new Thread(() -> {
            try {
                String apiKey = "AIzaSyASboo4rxLoC4QkA9ZeH5yWI4flQi_hXxU";
                String urlStr = "https://maps.googleapis.com/maps/api/directions/json?origin="
                        + originLatLng.latitude + "," + originLatLng.longitude
                        + "&destination=" + destLatLng.latitude + "," + destLatLng.longitude
                        + "&key=" + apiKey;

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    json.append(line);
                reader.close();

                JSONObject response = new JSONObject(json.toString());
                JSONArray routes = response.getJSONArray("routes");
                if (routes.length() > 0) {
                    JSONObject overviewPolyline = routes.getJSONObject(0).getJSONObject("overview_polyline");
                    String points = overviewPolyline.getString("points");

                    List<LatLng> decodedPath = PolyUtil.decode(points);

                    runOnUiThread(() -> map.addPolyline(new PolylineOptions()
                            .addAll(decodedPath)
                            .color(Color.LTGRAY)
                            .width(30f)
                            .startCap(new RoundCap())
                            .endCap(new RoundCap())
                            .jointType(JointType.ROUND)));

                    runOnUiThread(() -> map.addPolyline(new PolylineOptions()
                            .addAll(decodedPath)
                            .color(getResources().getColor(R.color.blue, getTheme()))
                            .width(20f)
                            .startCap(new RoundCap())
                            .endCap(new RoundCap())
                            .jointType(JointType.ROUND)));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private Bitmap getBitmapFromVectorDrawable(@DrawableRes int drawableId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(this, drawableId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(
                vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    // Finish the activity on goBack
    public  void goBack(View view){
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationTracker != null) {
            locationTracker.stopTracking();
        }
        finish();
    }
}