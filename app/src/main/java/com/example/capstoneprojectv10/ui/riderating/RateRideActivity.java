package com.example.capstoneprojectv10.ui.riderating;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.example.capstoneprojectv10.MainActivity;
import com.example.capstoneprojectv10.R;
import com.example.capstoneprojectv10.databinding.ActivityRateRideBinding;
import com.google.firebase.firestore.FirebaseFirestore;

public class RateRideActivity extends AppCompatActivity {

    private RatingBar ratingBar;
    private Button btnSubmit;
    private String rideId;
    private String username;
    private ActivityRateRideBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        binding = ActivityRateRideBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        rideId = getIntent().getStringExtra("rideId");
        username = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("username", null);

        ratingBar = binding.ratingBar;
        btnSubmit = binding.btnSubmitRating;

        ratingBar.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.rating_star_yellow)));


        btnSubmit.setOnClickListener(v -> {
            float rating = ratingBar.getRating();

            if (rating < 1) {
                Toast.makeText(this, "Please select at least 1 star.", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("rides").document(rideId)
                    .update("ratings." + username, rating)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Thank you for rating!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to submit rating.", Toast.LENGTH_SHORT).show();
                    });
        });
    }
}