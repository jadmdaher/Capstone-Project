package com.example.capstoneprojectv10.ui.map;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.capstoneprojectv10.R;
import com.example.capstoneprojectv10.databinding.ActivityPassengerRouteBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;


public class PassengerRouteActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng origin, destination;
    private LinearLayout bottomSheet;
    private ActivityPassengerRouteBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        double originLat = getIntent().getDoubleExtra("origin_lat", 0);
        double originLng = getIntent().getDoubleExtra("origin_lng", 0);
        double destLat = getIntent().getDoubleExtra("dest_lat", 0);
        double destLng = getIntent().getDoubleExtra("dest_lng", 0);

        origin = new LatLng(originLat, originLng);
        destination = new LatLng(destLat, destLng);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        googleMap.addMarker(new MarkerOptions().position(origin).title("Origin"));
        googleMap.addMarker(new MarkerOptions().position(destination).title("Destination"));
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
                            .color(getResources().getColor(R.color.blue, getTheme()))
                            .width(20f)));
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

    //Specify what happens onDestroy
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //mMap.clear();
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