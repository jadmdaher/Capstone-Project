package com.example.capstoneprojectv10.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class Ride {
    /* A ride has the following attributes:
    *  - departureLocation: String
    *  - arrivalLocation: String
    *  - departureTime: String ---> Time is optional because we are assuming only current rides will
    *                               be displayed, whereas completed rides will not be displayed.
    *  - driver: User ---> The driver of the ride
    *  - passengers: List<User> ---> The passengers of the ride
    *  - capacity: int ---> The maximum number of passengers the ride can take
    *  - isCompleted: boolean ---> True if the ride is completed, false if the ride is not completed.
    *  - isAvailable: boolean ---> True if the ride is available, false if the ride is not available.
    *  - isBooked: boolean ---> True if the ride is booked, false if the ride is not booked.
    * */

    private String rideId;
    private String driverId;
    private GeoPoint departureLocation;
    private GeoPoint currentLocation;
    private GeoPoint destinationLocation;
    private Timestamp departureTime;
    private User driver;
    private List<User> passengers;
    private int capacity;
    private boolean isComplete;
    private String status;

    // Constructor
    public Ride(GeoPoint departureLocation, GeoPoint currentLocation, GeoPoint destinationLocation, Timestamp departureTime, User driver, List<User> passengers, int capacity, boolean isCompleted) {
        this.departureLocation = departureLocation;
        this.currentLocation = currentLocation;
        this.destinationLocation = destinationLocation;
        this.departureTime = departureTime;
        this.driver = driver;
        this.passengers = passengers;
        this.capacity = capacity;
        this.isComplete = isCompleted;
    }

    public Ride(String driverId, GeoPoint departureLocation, GeoPoint destinationLocation, Timestamp departureTime, String status) {
        this.driverId = driverId;
        this.departureLocation = departureLocation;
        this.destinationLocation = destinationLocation;
        this.departureTime = departureTime;
        this.status = status;
    }

    // rideId getter and setter
    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    // departureLocation getter and setter
    public GeoPoint getDepartureLocation() {
        return departureLocation;
    }

    public void setDepartureLocation(GeoPoint departureLocation) {
        this.departureLocation = departureLocation;
    }

    // currentLocation getter and setter
    public GeoPoint getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(GeoPoint currentLocation) {
        this.currentLocation = currentLocation;
    }

    // arrivalLocation getter and setter
    public GeoPoint getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(GeoPoint destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    // departureTime getter and setter
    public Timestamp getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(Timestamp departureTime) {
        this.departureTime = departureTime;
    }

    // driver getter and setter
    public User getDriver() {
        return driver;
    }

    public void setDriver(User driver) {
        this.driver = driver;
    }

    // passengers getter and setter
    public List<User> getPassengers() {
        return passengers;
    }

    public void setPassengers(List<User> passengers) {
        this.passengers = passengers;
    }

    // capacity getter and setter
    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    // isCompleted getter and setter
    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean isCompleted) {
        this.isComplete = isCompleted;
    }

    // Ride Matching Algorithm
    public boolean isMatch(Ride ride) {
        return this.departureLocation.equals(ride.getDepartureLocation()) &&
                this.destinationLocation.equals(ride.getDestinationLocation()) &&
                this.departureTime.equals(ride.getDepartureTime());
    }

    public ArrayList<User> ridePassengers = new ArrayList<User>();
    public Ride newRide = new Ride(driverId, departureLocation, destinationLocation, departureTime, status);
    public ArrayList<Ride> availableRides = new ArrayList<Ride>();

    /* Add a ride to the available rides list (available rides are rides that are ongoing; i.e: not
    *                                          completed and/or capacity is not full)
    * */
    public void addRide(Ride ride) {
        availableRides.add(ride);
    }

    // Iterate over the available rides list and get the valid rides that match the user's ride
    public ArrayList<Ride> getValidRides(Ride newRide) {
        this.newRide = newRide;
        for (Ride r : availableRides) {
            if (r.isMatch(newRide)) {
                availableRides.add(r);
                // Lines 117 and 118 are commented out because they were automatically generated
                // by the IDE.
                // Add the driver of the ride to the ridePassengers list.
                //ridePassengers.add(r.getDriver());
            }
        }

        return availableRides;
    }
}
