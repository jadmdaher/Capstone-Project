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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.capstoneprojectv10.R;
import com.example.capstoneprojectv10.data.model.Ride;
import com.example.capstoneprojectv10.ui.availablerides.AvailableRideActivity;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

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
    EditText etDepartureLocation;
    EditText etDestinationLocation;
    EditText etDepartureTime;
    Button btnSearch;
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

        mViewModel = new ViewModelProvider(this).get(NewRideViewModel.class);

        etDepartureLocation = view.findViewById(R.id.et_departure_location);
        etDestinationLocation = view.findViewById(R.id.et_destination_location);
        etDepartureTime = view.findViewById(R.id.et_departure_time);
        btnSearch = view.findViewById(R.id.btn_search);
        placesClient = Places.createClient(requireContext());

        // Using Places API to autocomplete location fields
        setupAutocomplete(etDepartureLocation);
        setupAutocomplete(etDestinationLocation);

        btnSearch.setOnClickListener(v -> {
            String departure = etDepartureLocation.getText().toString().trim();
            String arrival = etDestinationLocation.getText().toString().trim();
            String stringDepartureTime = etDepartureTime.getText().toString().trim();

            if (departure.isEmpty() || arrival.isEmpty() || stringDepartureTime.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String username = sharedPreferences.getString("username", null);
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
                                            FirebaseFirestore.getInstance().collection("rides")
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
                                                        NavHostFragment.findNavController(this)
                                                                .navigate(R.id.action_newRideFragment_to_mapsFragment, args);
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