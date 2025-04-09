package com.example.capstoneprojectv10.data.model;

import com.google.firebase.firestore.GeoPoint;

public class RideItem {
    public String driverName;
    public String departureName;
    public String destinationName;
    public GeoPoint origin;
    public GeoPoint destination;
    public String profileImageUrl;
    public String rideDate;

    public RideItem(String driverName, String departureName, String destinationName, GeoPoint origin, GeoPoint destination, String profileImageUrl, String rideDate) {
        this.driverName = driverName;
        this.departureName = departureName;
        this.destinationName = destinationName;
        this.origin = origin;
        this.destination = destination;
        this.profileImageUrl = profileImageUrl;
        this.rideDate = rideDate;
    }
}