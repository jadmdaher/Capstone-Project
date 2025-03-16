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
import com.example.capstoneprojectv10.databinding.ActivityAvailableRideListBinding;
import com.example.capstoneprojectv10.databinding.RecyclerviewAvailableRideTitleBinding;
import com.example.capstoneprojectv10.ui.availablerides.placeholder.PlaceholderContent;

public class AvailableRideActivity extends AppCompatActivity {

    private ActivityAvailableRideListBinding activityBinding;
    private RecyclerviewAvailableRideTitleBinding titleBinding;
    private Button btnBack;
    private RecyclerView recyclerView;
    private AvailableRideRecyclerViewAdapter adapter;

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

        adapter = new AvailableRideRecyclerViewAdapter(PlaceholderContent.ITEMS);
        recyclerView.setAdapter(adapter);

        btnBack = titleBinding.btnBack;
        btnBack.setOnClickListener(this::goBack);
    }

    public void goBack(View view) {
        finish(); // Closes the activity when the back button is clicked
    }
}
