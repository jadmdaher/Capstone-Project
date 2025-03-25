package com.example.capstoneprojectv10.data.repository;

import com.example.capstoneprojectv10.data.model.User;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class UserRepository {
    private final DatabaseReference userRef;

    public UserRepository() {
        userRef = FirebaseDatabase.getInstance().getReference("users");
    }

    public void addUser(User user) {
        userRef.child(user.getUsername()).setValue(user);
    }

    public DatabaseReference getUser(String userId) {
        return userRef.child(userId);
    }
}