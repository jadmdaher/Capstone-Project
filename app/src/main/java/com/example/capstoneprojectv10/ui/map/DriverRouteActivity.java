package com.example.capstoneprojectv10.ui.map;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.capstoneprojectv10.R;
import com.example.capstoneprojectv10.databinding.ActivityDriverRouteBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class DriverRouteActivity extends AppCompatActivity implements OnMapReadyCallback {

    private LatLng originLatLng;
    private LatLng destLatLng;
    private ActivityDriverRouteBinding binding;
    private LinearLayout bottomSheet;
    private TextView tvDeparture;
    private TextView tvDestination;
    private TextView tvTime;

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
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Set ride details
        String departureName = getIntent().getStringExtra("departure_name");
        String destinationName = getIntent().getStringExtra("destination_name");
        String departureTime = getIntent().getStringExtra("departure_time");

        tvDeparture = binding.tvDeparture;
        tvDestination = binding.tvDestination;
        tvTime = binding.tvTime;

        tvDeparture.setText(" " + departureName);
        tvDestination.setText(" " + destinationName);
        tvTime.setText(" " + departureTime);

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
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        googleMap.addMarker(new MarkerOptions().position(originLatLng).title("Origin").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        googleMap.addMarker(new MarkerOptions().position(destLatLng).title("Destination").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(originLatLng, 15f));

        // Fetch and draw the route
        fetchRoute(googleMap);
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
                            .color(getResources().getColor(R.color.blue, getTheme()))
                            .width(20f)));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Finish the activity on goBack
    public  void goBack(View view){
        finish();
    }
}