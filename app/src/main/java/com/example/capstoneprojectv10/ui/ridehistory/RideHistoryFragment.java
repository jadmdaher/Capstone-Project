package com.example.capstoneprojectv10.ui.ridehistory;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
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
import com.example.capstoneprojectv10.ui.map.DriverRouteActivity;
import com.example.capstoneprojectv10.ui.map.PassengerRouteActivity;
import com.example.capstoneprojectv10.ui.ridedetails.RideDetailsActivity;
import com.example.capstoneprojectv10.ui.ridehistory.placeholder.PlaceholderContent;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
            RideHistoryRecyclerViewAdapter adapter = new RideHistoryRecyclerViewAdapter(completedRides, ride -> {
                if ("in progress".equalsIgnoreCase(ride.status)) {
                    if (username.equals(ride.driverName)) {
                        Intent intent = new Intent(requireContext(), DriverRouteActivity.class);
                        intent.putExtra("rideId", ride.rideId);
                        intent.putExtra("origin_lat", ride.origin.getLatitude());
                        intent.putExtra("origin_lng", ride.origin.getLongitude());
                        intent.putExtra("dest_lat", ride.destination.getLatitude());
                        intent.putExtra("dest_lng", ride.destination.getLongitude());

                        Geocoder geocoder = new Geocoder(this.getContext());
                        try {
                            List<Address> originAddresses = geocoder.getFromLocation(ride.origin.getLatitude(), ride.origin.getLongitude(), 1);
                            List<Address> destinationAddresses = geocoder.getFromLocation(ride.destination.getLatitude(), ride.destination.getLongitude(), 1);

                            if (!originAddresses.isEmpty()) {
                                String originAddress = originAddresses.get(0).getAddressLine(0);
                                intent.putExtra("departure_name", originAddress);
                            } else {
                                intent.putExtra("departure_name", "" + ride.origin.getLatitude() + ", " + ride.origin.getLongitude());
                            }

                            if (!destinationAddresses.isEmpty()) {
                                String destAddress = destinationAddresses.get(0).getAddressLine(0);
                                intent.putExtra("destination_name", destAddress);
                            } else {
                                intent.putExtra("destination_name", "" + ride.destination.getLatitude() + ", " + ride.destination.getLongitude());
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            intent.putExtra("departure_name", "" + ride.origin.getLatitude() + ", " + ride.origin.getLongitude());
                            intent.putExtra("destination_name", "" + ride.destination.getLatitude() + ", " + ride.destination.getLongitude());
                        }

                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(requireContext(), PassengerRouteActivity.class);
                        intent.putExtra("rideId", ride.rideId);
                        intent.putExtra("origin_lat", ride.origin.getLatitude());
                        intent.putExtra("origin_lng", ride.origin.getLongitude());
                        intent.putExtra("dest_lat", ride.destination.getLatitude());
                        intent.putExtra("dest_lng", ride.destination.getLongitude());

                        Geocoder geocoder = new Geocoder(this.getContext());
                        try {
                            List<Address> originAddresses = geocoder.getFromLocation(ride.origin.getLatitude(), ride.origin.getLongitude(), 1);
                            List<Address> destinationAddresses = geocoder.getFromLocation(ride.destination.getLatitude(), ride.destination.getLongitude(), 1);

                            if (!originAddresses.isEmpty()) {
                                String originAddress = originAddresses.get(0).getAddressLine(0);
                                intent.putExtra("departure_name", originAddress);
                            } else {
                                intent.putExtra("departure_name", "" + ride.origin.getLatitude() + ", " + ride.origin.getLongitude());
                            }

                            if (!destinationAddresses.isEmpty()) {
                                String destAddress = destinationAddresses.get(0).getAddressLine(0);
                                intent.putExtra("destination_name", destAddress);
                            } else {
                                intent.putExtra("destination_name", "" + ride.destination.getLatitude() + ", " + ride.destination.getLongitude());
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            intent.putExtra("departure_name", "" + ride.origin.getLatitude() + ", " + ride.origin.getLongitude());
                            intent.putExtra("destination_name", "" + ride.destination.getLatitude() + ", " + ride.destination.getLongitude());
                        }

                        startActivity(intent);
                    }
                } else {
                    // Completed ride
                    Intent intent = new Intent(requireContext(), RideDetailsActivity.class);
                    intent.putExtra("rideId", ride.rideId);
                    intent.putExtra("driver", ride.driverName);
                    intent.putExtra("rideDate", ride.rideDate);
                    intent.putExtra("origin_lat", ride.origin.getLatitude());
                    intent.putExtra("origin_lng", ride.origin.getLongitude());
                    intent.putExtra("destination_lat", ride.destination.getLatitude());
                    intent.putExtra("destination_lng", ride.destination.getLongitude());
                    intent.putExtra("profileImageUrl", ride.profileImageUrl);
                    startActivity(intent);
                }
            });

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
                                String status = doc.getString("status");

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
                                                    status,
                                                    0 // score is unused in history
                                            );

                                            //completedRides.add(rideItem);
                                            //adapter.notifyDataSetChanged(); // Notify on each successful fetch
                                            completedRides.add(rideItem);
                                            completedRides.sort((r1, r2) -> {
                                                try {
                                                    SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
                                                    Date d1 = sdf.parse(r1.rideDate);
                                                    Date d2 = sdf.parse(r2.rideDate);
                                                    return d2.compareTo(d1); // newest first
                                                } catch (Exception e) {
                                                    return 0;
                                                }
                                            });
                                            adapter.notifyDataSetChanged();
                                        });
                            }
                        }
                    });
            db.collection("rides")
                    .whereEqualTo("status", "in progress")
                    .get()
                    .addOnSuccessListener(inProgressRides -> {
                        for (DocumentSnapshot doc : inProgressRides) {
                            String driver = doc.getString("driver");
                            List<String> passengers = (List<String>) doc.get("passengers");

                            if (username != null && (username.equals(driver) || (passengers != null && passengers.contains(username)))) {
                                GeoPoint origin = doc.getGeoPoint("origin");
                                GeoPoint destination = doc.getGeoPoint("destination");
                                Timestamp time = doc.getTimestamp("departureTime");
                                String rideId = doc.getId();
                                String status = doc.getString("status");

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
                                                    null,
                                                    null,
                                                    origin,
                                                    destination,
                                                    profileUrl,
                                                    time != null ? time.toDate().toString() : "",
                                                    status,
                                                    0
                                            );

                                            completedRides.add(rideItem); // include current ride in the list
                                            completedRides.sort((r1, r2) -> {
                                                try {
                                                    SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
                                                    Date d1 = sdf.parse(r1.rideDate);
                                                    Date d2 = sdf.parse(r2.rideDate);
                                                    return d2.compareTo(d1);
                                                } catch (Exception e) {
                                                    return 0;
                                                }
                                            });
                                            adapter.notifyDataSetChanged();
                                        });
                            }
                        }
                    });
        }
        return view;
    }
}