package com.example.capstoneprojectv10.data.model;

import java.util.List;

public class User {
    /*A user can be either a passenger or a driver and has the following attributes:
    *  - username: String
    *  - phoneNumber: String
    *  - email: String ---> might not use this attribute
    *  - password: String
    *  - rides: List<Ride> ---> The rides the user has been a part of.
    *  - isDriver: boolean ---> True if the user is a driver, false if the user is a passenger.
    *  - isPassenger: boolean ---> True if the user is a passenger, false if the user is a driver.
    *
    * The following attributes are for the user's ride status (optional):
    *  - isAvailable: boolean ---> True if the user is available to drive, false if the user is not available.
    *  - isBooked: boolean ---> True if the user has booked a ride, false if the user has not booked a ride.
    *  - isCompleted: boolean ---> True if the ride is completed, false if the ride is not completed.
    *
    * The Attributes below are for the user's account status (optional):
    *  - isVerified: boolean ---> True if the user has verified their account, false if the user has not verified their account.
    *  - isBlocked: boolean ---> True if the user has been blocked, false if the user has not been blocked.
    *  - isSuspended: boolean ---> True if the user has been suspended, false if the user has not been suspended.
    *  - isDeleted: boolean ---> True if the user has been deleted, false if the user has not been deleted.
    *  - isReported: boolean ---> True if the user has been reported, false if the user has not been reported.
    *  - isBanned: boolean ---> True if the user has been banned, false if the user has not been banned.
    *  - isUnbanned: boolean ---> True if the user has been unbanned, false if the user has not been unbanned.
    * */

    private String username;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String password;
    private List<Ride> rides;
    private String role;

    // Constructor
    public User(String username, String firstName, String lastName, String phoneNumber, String password, List<Ride> rides, String role) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.rides = rides;
        this.role = role;
    }

    // username getter and setter
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // firstName getter and setter
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    // lastName getter and setter
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    // phoneNumber getter and setter
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    // password getter and setter
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // rides getter and setter
    public List<Ride> getRides() {
        return rides;
    }

    public void setRides(List<Ride> rides) {
        this.rides = rides;
    }

    // role getter and setter
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        if (role.equalsIgnoreCase("driver")) {
            this.role = "driver";
        } else if (role.equalsIgnoreCase("passenger")) {
            this.role = "passenger";
        } else if (role.equalsIgnoreCase("admin")) {
            this.role = "admin";
        }
    }
}
