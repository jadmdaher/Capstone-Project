package com.example.capstoneprojectv10.data.repository;

import com.example.capstoneprojectv10.data.model.Ride;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RideRepository {
    private final DatabaseReference rideRef;

    public RideRepository() {
        rideRef = FirebaseDatabase.getInstance().getReference("rides");
    }

    public void addRide(Ride ride) {
        rideRef.child(ride.getRideId()).setValue(ride);
    }

    public DatabaseReference getAvailableRides() {
        return rideRef;
    }
}