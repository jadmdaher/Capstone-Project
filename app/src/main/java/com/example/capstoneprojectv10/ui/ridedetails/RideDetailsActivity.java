package com.example.capstoneprojectv10.ui.ridedetails;

import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.bumptech.glide.Glide;
import com.example.capstoneprojectv10.R;
import com.example.capstoneprojectv10.databinding.ActivityRideDetailsBinding;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RideDetailsActivity extends AppCompatActivity {

    private TextView tvDriver, tvDate, tvOrigin, tvDestination, tvPassengers, tvRatings;
    private ImageView ivProfile;
    private ActivityRideDetailsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        binding = ActivityRideDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tvDriver = binding.tvDriver;
        tvDate = binding.tvDate;
        tvOrigin = binding.tvOrigin;
        tvDestination = binding.tvDestination;
        tvPassengers = binding.tvPassengers;
        tvRatings = binding.tvRatings;
        ivProfile = binding.ivProfile;

        String driver = getIntent().getStringExtra("driver");
        String rideDate = getIntent().getStringExtra("rideDate");
        String rideId = getIntent().getStringExtra("rideId");
        double originLat = getIntent().getDoubleExtra("origin_lat", 0);
        double originLng = getIntent().getDoubleExtra("origin_lng", 0);
        double destLat = getIntent().getDoubleExtra("destination_lat", 0);
        double destLng = getIntent().getDoubleExtra("destination_lng", 0);
        String profileUrl = getIntent().getStringExtra("profileImageUrl");

        tvDriver.setText("@" + driver);

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
            Date date = inputFormat.parse(rideDate);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            tvDate.setText(outputFormat.format(date));
        } catch (Exception e) {
            tvDate.setText("" + rideDate);
        }

        //tvDate.setText("" + rideDate);

        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> originAddresses = geocoder.getFromLocation(originLat, originLng, 1);
            List<Address> destinationAddresses = geocoder.getFromLocation(destLat, destLng, 1);

            if (!originAddresses.isEmpty()) {
                String originAddress = originAddresses.get(0).getAddressLine(0);
                tvOrigin.setText("" + originAddress);
            } else {
                tvOrigin.setText("" + originLat + ", " + originLng);
            }

            if (!destinationAddresses.isEmpty()) {
                String destAddress = destinationAddresses.get(0).getAddressLine(0);
                tvDestination.setText("" + destAddress);
            } else {
                tvDestination.setText("" + destLat + ", " + destLng);
            }

        } catch (Exception e) {
            e.printStackTrace();
            tvOrigin.setText("" + originLat + ", " + originLng);
            tvDestination.setText("" + destLat + ", " + destLng);
        }

        //tvOrigin.setText("" + originLat + ", " + originLng);
        //tvDestination.setText("" + destLat + ", " + destLng);

        if (rideId != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("rides").document(rideId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            List<String> passengers = (List<String>) snapshot.get("passengers");

                            if (passengers != null && !passengers.isEmpty()) {
                                StringBuilder formatted = new StringBuilder();
                                for (String p : passengers) {
                                    formatted.append("@").append(p).append("\n");
                                }
                                tvPassengers.setText(formatted.toString().trim());
                            } else {
                                tvPassengers.setText("No passengers");
                            }

                            Map<String, Object> ratingsMap = (Map<String, Object>) snapshot.get("ratings");

                            if (ratingsMap != null && !ratingsMap.isEmpty()) {
                                StringBuilder sb = new StringBuilder();
                                for (Map.Entry<String, Object> entry : ratingsMap.entrySet()) {
                                    sb.append("@").append(entry.getKey()).append(": ").append(entry.getValue().toString()).append("★");
                                    sb.append("\n");
                                }
                                tvRatings.setText(sb.toString().trim());
                            } else {
                                tvRatings.setText("No ratings submitted");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        tvPassengers.setText("Unable to load passengers");
                    });
        }

        if (profileUrl != null && !profileUrl.isEmpty()) {
            Glide.with(this)
                    .load(profileUrl)
                    .circleCrop()
                    .into(ivProfile);
        } else {
            ivProfile.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }
}