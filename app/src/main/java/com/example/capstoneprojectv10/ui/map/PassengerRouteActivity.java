package com.example.capstoneprojectv10.ui.map;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.example.capstoneprojectv10.ui.riderating.RateRideActivity;
import com.example.capstoneprojectv10.R;
import com.example.capstoneprojectv10.databinding.ActivityPassengerRouteBinding;
import com.example.capstoneprojectv10.utils.LocationTracker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.ButtCap;
import com.google.android.gms.maps.model.Cap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.firestore.DocumentSnapshot;
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


public class PassengerRouteActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng origin, destination;
    private LinearLayout bottomSheet;
    private ActivityPassengerRouteBinding binding;
    private TextView tvDeparture;
    private TextView tvDestination;
    private LocationTracker locationTracker;
    private final Map<String, Marker> userMarkers = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkLocationPermission();

        getSupportActionBar().hide();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        binding = ActivityPassengerRouteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Always fetch fresh Intent values
        updateRouteFromIntent(getIntent());

        bottomSheet = binding.bottomSheet;
        BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight(120);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        // Set ride details
        String departureName = getIntent().getStringExtra("departure_name");
        String destinationName = getIntent().getStringExtra("destination_name");

        tvDeparture = binding.tvDeparture;
        tvDestination = binding.tvDestination;

        tvDeparture.setText(" " + departureName);
        tvDestination.setText(" " + destinationName);

        double originLat = getIntent().getDoubleExtra("origin_lat", 0);
        double originLng = getIntent().getDoubleExtra("origin_lng", 0);
        double destLat = getIntent().getDoubleExtra("dest_lat", 0);
        double destLng = getIntent().getDoubleExtra("dest_lng", 0);

        origin = new LatLng(originLat, originLng);
        destination = new LatLng(destLat, destLng);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        String rideId = getIntent().getStringExtra("rideId");
        String username = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("username", null);
        locationTracker = new LocationTracker(this, rideId, username);
        locationTracker.startTracking();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (rideId != null) {
            db.collection("rides").document(rideId)
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null || snapshot == null || !snapshot.exists()) return;

                        String status = snapshot.getString("status");
                        if ("complete".equalsIgnoreCase(status)) {
                            Toast.makeText(this, "Ride complete!", Toast.LENGTH_SHORT).show();

                            Intent rateIntent = new Intent(this, RateRideActivity.class);
                            rateIntent.putExtra("rideId", rideId);
                            startActivity(rateIntent);
                            rateIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(rateIntent);
                        }
                    });
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        googleMap.addMarker(new MarkerOptions().position(origin).title("Origin").icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromVectorDrawable(R.drawable.marker_circle_green))));
        googleMap.addMarker(new MarkerOptions().position(destination).title("Destination").icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromVectorDrawable(R.drawable.marker_circle_red))));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origin, 15f));

        // Fetch and draw the route
        fetchRoute(googleMap);

        // Move camera to show both points
        /*LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(origin);
        builder.include(destination);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));

        // Draw a polyline between the points
        googleMap.addPolyline(new PolylineOptions().add(origin, destination).width(10).color(Color.BLUE));*/

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
                        + origin.latitude + "," + origin.longitude
                        + "&destination=" + destination.latitude + "," + destination.longitude
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Important to update the internal reference
        updateRouteFromIntent(intent); // You can extract logic here
    }

    private void updateRouteFromIntent(Intent intent) {
        double originLat = intent.getDoubleExtra("originLat", 0);
        double originLng = intent.getDoubleExtra("originLng", 0);
        double destLat = intent.getDoubleExtra("destLat", 0);
        double destLng = intent.getDoubleExtra("destLng", 0);

        origin = new LatLng(originLat, originLng);
        destination = new LatLng(destLat, destLng);

        if (mMap != null) {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(origin).title("Origin"));
            mMap.addMarker(new MarkerOptions().position(destination).title("Destination"));

            LatLngBounds bounds = new LatLngBounds.Builder().include(origin).include(destination).build();
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

            mMap.addPolyline(new PolylineOptions().add(origin, destination).width(10).color(Color.BLUE));
        }
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

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Location permission denied. Location features may not work properly.", Toast.LENGTH_LONG).show();
            }
        }
    }

    //Specify what happens onDestroy
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationTracker != null) {
            locationTracker.stopTracking();
        }
        finish();
    }

    // Specify what happens onPause
    /*@Override
    protected void onPause() {
        super.onPause();
        mMap.clear();
        finish();
    }*/

    //Specify what happens onStop
   /* @Override
    protected void onStop() {
        super.onStop();
        mMap.clear();
        finish();
    }*/

    /*@Override
    public void onBackPressed(){
        super.onBackPressed();
    }*/
}