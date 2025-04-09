package com.example.capstoneprojectv10.ui.newride;

import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.capstoneprojectv10.R;
import com.example.capstoneprojectv10.data.model.Ride;
import com.example.capstoneprojectv10.data.model.RideItem;
import com.example.capstoneprojectv10.databinding.FragmentNewRideBinding;
import com.example.capstoneprojectv10.ui.availablerides.AvailableRideActivity;
import com.example.capstoneprojectv10.ui.map.DriverRouteActivity;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class NewRideFragment extends Fragment {

    public static NewRideFragment newInstance() {
        return new NewRideFragment();
    }

    private NewRideViewModel mViewModel;
    private FragmentNewRideBinding binding;
    private ScrollView driverView;
    private ScrollView passengerView;
    EditText etDepartureLocation;
    EditText etDestinationLocation;
    EditText etDepartureTime;
    Button btnNewRide;
    private PlacesClient placesClient;
    private LatLng departureLatLng;
    private LatLng destinationLatLng;
    private Timestamp departureTime;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_ride, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding = FragmentNewRideBinding.bind(view);

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("username", null);

        mViewModel = new ViewModelProvider(this).get(NewRideViewModel.class);

        etDepartureLocation = view.findViewById(R.id.et_departure_location);
        etDestinationLocation = view.findViewById(R.id.et_destination_location);
        etDepartureTime = view.findViewById(R.id.et_departure_time);
        btnNewRide = view.findViewById(R.id.btn_new_ride);
        placesClient = Places.createClient(requireContext());

        // Using Places API to autocomplete location fields
        setupAutocomplete(etDepartureLocation);
        setupAutocomplete(etDestinationLocation);

        // Animate the click of the button
        btnNewRide.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
            }
            return false; // Let the click still happen
        });

        // Specify what happens when the new ride button is clicked
        btnNewRide.setOnClickListener(v -> {
            String departure = etDepartureLocation.getText().toString().trim();
            String arrival = etDestinationLocation.getText().toString().trim();
            String stringDepartureTime = etDepartureTime.getText().toString().trim();

            if (departure.isEmpty() || arrival.isEmpty() || stringDepartureTime.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (username != null) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("users")
                        .whereEqualTo("username", username)
                        .get()
                        .addOnSuccessListener(querySnapshots -> {
                            if (!querySnapshots.isEmpty()) {
                                DocumentSnapshot document = querySnapshots.getDocuments().get(0);
                                String role = document.getString("role");

                                // describe what you want the button click to do here
                                if(role.equalsIgnoreCase("driver") || role.equalsIgnoreCase("admin")){
                                    Timestamp departureTime = Timestamp.now(); // Replace with actual selected time

                                    Map<String, Object> ride = new HashMap<>();
                                    ride.put("driver", username);
                                    ride.put("departureTime", departureTime);
                                    ride.put("passengers", new ArrayList<>());
                                    ride.put("status", "issued");

                                    // Fetch origin
                                    getPlaceIdFromName(departure, originPlaceId -> {
                                        if (originPlaceId != null) {
                                            fetchPlaceLatLng(originPlaceId, originLatLng -> {
                                                if (originLatLng != null) {
                                                    GeoPoint originGeo = new GeoPoint(originLatLng.latitude, originLatLng.longitude);
                                                    ride.put("origin", originGeo);

                                                    // Fetch destination after origin is ready
                                                    getPlaceIdFromName(arrival, destPlaceId -> {
                                                        if (destPlaceId != null) {
                                                            fetchPlaceLatLng(destPlaceId, destLatLng -> {
                                                                if (destLatLng != null) {
                                                                    GeoPoint destGeo = new GeoPoint(destLatLng.latitude, destLatLng.longitude);
                                                                    ride.put("destination", destGeo);

                                                                    // All ready – now save to Firestore
                                                                    db.collection("rides")
                                                                            .add(ride)
                                                                            .addOnSuccessListener(docRef -> {
                                                                                Log.d("Firestore", "Ride saved: " + docRef.getId());
                                                                                Toast.makeText(getContext(), "Ride created", Toast.LENGTH_SHORT).show();

                                                                                // Optional: Pass info to next fragment
                                                                                Bundle args = new Bundle();
                                                                                args.putDouble("origin_lat", originLatLng.latitude);
                                                                                args.putDouble("origin_lng", originLatLng.longitude);
                                                                                args.putDouble("dest_lat", destLatLng.latitude);
                                                                                args.putDouble("dest_lng", destLatLng.longitude);
                                                                                /*NavHostFragment.findNavController(this)
                                                                                        .navigate(R.id.action_newRideFragment_to_availableRidesFragment, args);*/
                                                                                Intent intent = new Intent(getContext(), DriverRouteActivity.class);
                                                                                intent.putExtras(args);
                                                                                intent.putExtra("departure_name", departure);
                                                                                intent.putExtra("destination_name", arrival);
                                                                                intent.putExtra("departure_time", stringDepartureTime);
                                                                                startActivity(intent);
                                                                            })
                                                                            .addOnFailureListener(e -> {
                                                                                Log.e("Firestore", "Error saving ride: " + e.getMessage());
                                                                                Toast.makeText(getContext(), "Failed to save ride", Toast.LENGTH_SHORT).show();
                                                                            });
                                                                }
                                                            });
                                                        }
                                                    });
                                                }
                                            });
                                        }
                                    });
                                } else if (role.equalsIgnoreCase("passenger")) {
                                    // Retrieve all available rides in the firebase database
                                    db.collection("rides")
                                            .whereIn("status", Arrays.asList("issued", "in progress"))
                                            .get()
                                            .addOnSuccessListener(ridesSnapshot -> {
                                                if (!ridesSnapshot.isEmpty()) {
                                                    /*List<Map<String, Object>> rides = new ArrayList<>();
                                                    for (DocumentSnapshot doc : ridesSnapshot.getDocuments()) {
                                                        rides.add(doc.getData());
                                                    }*/

                                                    ArrayList<RideItem> rides = new ArrayList<>();

                                                    for (DocumentSnapshot doc : ridesSnapshot.getDocuments()) {
                                                        String driver = doc.getString("driver");
                                                        GeoPoint origin = doc.getGeoPoint("origin");
                                                        GeoPoint destination = doc.getGeoPoint("destination");
                                                        Timestamp time = doc.getTimestamp("departureTime");

                                                        // You'll fetch the driver's info in AvailableRideActivity (cleaner & less nested)

                                                        RideItem rideItem = new RideItem(driver, null, null, origin, destination, null, time.toDate().toString());
                                                        rides.add(rideItem);
                                                    }

                                                    // ✅ For now, just log the results
                                                    Log.d("PassengerRides", "Total rides found: " + rides.size());
                                                    for (RideItem ride : rides) {
                                                        Log.d("Ride", ride.toString());
                                                    }

                                                    Toast.makeText(getContext(), "Found " + rides.size() + " rides", Toast.LENGTH_SHORT).show();

                                                    // 🔄 Later, you can pass these to another activity or fragment
                                                    // For now, just start the same AvailableRideActivity with no filters
                                                    Intent intent = new Intent(getContext(), AvailableRideActivity.class);
                                                    intent.putExtra("rideList", new Gson().toJson(rides));
                                                    startActivity(intent);
                                                } else {
                                                    Toast.makeText(getContext(), "No rides available", Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("PassengerRides", "Failed to fetch rides", e);
                                                Toast.makeText(getContext(), "Failed to load rides", Toast.LENGTH_SHORT).show();
                                            });
                                }
                            }
                        })
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load role", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void setupAutocomplete(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!Places.isInitialized() || s.length() < 3) return;

                FindAutocompletePredictionsRequest request =
                        FindAutocompletePredictionsRequest.builder()
                                .setQuery(s.toString())
                                .build();

                placesClient.findAutocompletePredictions(request)
                        .addOnSuccessListener(response -> {
                            List<String> suggestions = new ArrayList<>();
                            for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                                suggestions.add(prediction.getFullText(null).toString());
                            }

                            // Show suggestions in dropdown below EditText
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                    requireContext(),
                                    android.R.layout.simple_dropdown_item_1line,
                                    suggestions
                            );

                            // Convert to AutoCompleteTextView on-the-fly
                            if (editText instanceof AutoCompleteTextView) {
                                ((AutoCompleteTextView) editText).setAdapter(adapter);
                                ((AutoCompleteTextView) editText).showDropDown();
                            }
                        });
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void getPlaceIdFromName(String placeName, Consumer<String> callback) {
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(placeName)
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    if (!response.getAutocompletePredictions().isEmpty()) {
                        AutocompletePrediction prediction = response.getAutocompletePredictions().get(0); // Top result
                        String placeId = prediction.getPlaceId();
                        callback.accept(placeId);
                    } else {
                        Log.e("Places", "No predictions found");
                        callback.accept(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Places", "Error getting predictions: " + e.getMessage());
                    callback.accept(null);
                });
    }

    private void fetchPlaceLatLng(String placeId, Consumer<LatLng> callback) {
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG);

        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();

        placesClient.fetchPlace(request)
                .addOnSuccessListener(response -> {
                    Place place = response.getPlace();
                    callback.accept(place.getLatLng());
                })
                .addOnFailureListener(e -> {
                    Log.e("Place", "Failed to fetch place: " + e.getMessage());
                    callback.accept(null); // or handle error
                });
    }
}