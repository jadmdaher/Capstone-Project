package com.example.capstoneprojectv10.ui.ridehistory;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.capstoneprojectv10.R;
import com.example.capstoneprojectv10.data.model.RideItem;
import com.example.capstoneprojectv10.ui.ridehistory.placeholder.PlaceholderContent;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 */
public class RideHistoryFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RideHistoryFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static RideHistoryFragment newInstance(int columnCount) {
        RideHistoryFragment fragment = new RideHistoryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ride_history_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            //recyclerView.setAdapter(new RideHistoryRecyclerViewAdapter(PlaceholderContent.ITEMS));
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String username = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                    .getString("username", null);

            List<RideItem> completedRides = new ArrayList<>();
            RideHistoryRecyclerViewAdapter adapter = new RideHistoryRecyclerViewAdapter(completedRides);
            recyclerView.setAdapter(adapter);

            db.collection("rides")
                    .whereEqualTo("status", "complete")
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        for (DocumentSnapshot doc : snapshots) {
                            String driver = doc.getString("driver");
                            List<String> passengers = (List<String>) doc.get("passengers");

                            if (username != null && (username.equals(driver) || (passengers != null && passengers.contains(username)))) {
                                GeoPoint origin = doc.getGeoPoint("origin");
                                GeoPoint destination = doc.getGeoPoint("destination");
                                Timestamp time = doc.getTimestamp("departureTime");
                                String rideId = doc.getId();

                                // Fetch driver's profile picture
                                db.collection("users")
                                        .whereEqualTo("username", driver)
                                        .get()
                                        .addOnSuccessListener(driverSnapshot -> {
                                            String profileUrl = null;
                                            if (!driverSnapshot.isEmpty()) {
                                                profileUrl = driverSnapshot.getDocuments().get(0).getString("profileImageUrl");
                                            }

                                            RideItem rideItem = new RideItem(
                                                    rideId,
                                                    driver,
                                                    null, // departureName
                                                    null, // destinationName
                                                    origin,
                                                    destination,
                                                    profileUrl,
                                                    time != null ? time.toDate().toString() : "",
                                                    0 // score is unused in history
                                            );

                                            completedRides.add(rideItem);
                                            adapter.notifyDataSetChanged(); // Notify on each successful fetch
                                        });
                            }
                        }
                    });
        }
        return view;
    }
}