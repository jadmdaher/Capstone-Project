package com.example.capstoneprojectv10.ui.newride;

import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
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
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
    private static final double DETOUR_RADIUS_KM = 5.0;

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

            if (departure.isEmpty() || arrival.isEmpty()) {
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

                                    db.collection("rides")
                                            .whereEqualTo("driver", username)
                                            .whereEqualTo("status", "in progress")
                                            .get()
                                            .addOnSuccessListener(existingRides -> {
                                                if (!existingRides.isEmpty()) {
                                                    Toast.makeText(getContext(), "You already have a ride in progress.", Toast.LENGTH_LONG).show();
                                                    return;
                                                }

                                                // No ride in progress — safe to continue creating a new ride

                                                ride.put("driver", username);
                                                ride.put("departureTime", Timestamp.now()); // or your actual selected time
                                                ride.put("passengers", new ArrayList<>());
                                                ride.put("status", "in progress");

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
                                                                                            intent.putExtra("rideId", docRef.getId());
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
                                            });

                                } else if (role.equalsIgnoreCase("passenger")) {
                                    getPlaceIdFromName(departure, passengerOriginId -> {
                                        if (passengerOriginId != null) {
                                            fetchPlaceLatLng(passengerOriginId, passengerOrigin -> {
                                                if (passengerOrigin != null) {
                                                    getPlaceIdFromName(arrival, passengerDestId -> {
                                                        if (passengerDestId != null) {
                                                            fetchPlaceLatLng(passengerDestId, passengerDestination -> {
                                                                if (passengerDestination != null) {
                                                                    fetchDirectionsPoints(passengerOrigin, passengerDestination, passengerRoute -> {
                                                                        if (passengerRoute.isEmpty()) {
                                                                            Toast.makeText(getContext(), "Failed to fetch your route", Toast.LENGTH_SHORT).show();
                                                                            return;
                                                                        }

                                                                        db.collection("rides")
                                                                                .whereIn("status", Arrays.asList("issued", "in progress"))
                                                                                .get()
                                                                                .addOnSuccessListener(ridesSnapshot -> {
                                                                                    boolean alreadyInRide = false;

                                                                                    for (DocumentSnapshot rideDoc : ridesSnapshot) {
                                                                                        List<String> passengers = (List<String>) rideDoc.get("passengers");
                                                                                        if (passengers != null && passengers.contains(username)) {
                                                                                            alreadyInRide = true;
                                                                                            break;
                                                                                        }
                                                                                    }

                                                                                    if (alreadyInRide) {
                                                                                        Toast.makeText(getContext(), "You are already in a ride.", Toast.LENGTH_LONG).show();
                                                                                    } else {
                                                                                        // Continue with normal ride search logic
                                                                                        ArrayList<RideItem> matchedRides = new ArrayList<>();
                                                                                        //ArrayList<Pair<RideItem, Double>> matchedRidesWithScores = new ArrayList<>();
                                                                                        List<DocumentSnapshot> rideDocs = ridesSnapshot.getDocuments();
                                                                                        int totalRides = rideDocs.size();
                                                                                        int[] processedCount = {0};

                                                                                        for (DocumentSnapshot doc : rideDocs) {
                                                                                            GeoPoint originGeo = doc.getGeoPoint("origin");
                                                                                            GeoPoint destinationGeo = doc.getGeoPoint("destination");
                                                                                            List<String> passengers = (List<String>) doc.get("passengers");

                                                                                            if (passengers != null && passengers.size() >= 4) {
                                                                                                continue; // Skip this ride
                                                                                            }

                                                                                            if (originGeo == null || destinationGeo == null) {
                                                                                                processedCount[0]++;
                                                                                                continue;
                                                                                            }

                                                                                            LatLng driverOrigin = new LatLng(originGeo.getLatitude(), originGeo.getLongitude());
                                                                                            LatLng driverDest = new LatLng(destinationGeo.getLatitude(), destinationGeo.getLongitude());

                                                                                            fetchDirectionsPoints(driverOrigin, driverDest, driverRoute -> {
                                                                                                //processedCount[0]++;
                                                                                                if (driverRoute.isEmpty()) return;

                                                                                                double matchPercent = calculateRouteMatchPercentage(driverRoute, passengerRoute);
                                                                                                boolean directionValid = isPassengerDirectionValid(driverRoute, passengerRoute, 0.5);
                                                                                                Log.d("MatchPercent", matchPercent + "");
                                                                                                Log.d("DirectionValid", directionValid + "");

                                                                                            /*if (matchPercent >= 50.0 && directionValid) {
                                                                                                String driver = doc.getString("driver");
                                                                                                Timestamp time = doc.getTimestamp("departureTime");
                                                                                                RideItem rideItem = new RideItem(driver, null, null, originGeo, destinationGeo, null, time.toDate().toString());
                                                                                                matchedRides.add(rideItem);
                                                                                            }*/

                                                                                                if (directionValid) {
                                                                                                    String driverUsername = doc.getString("driver");

                                                                                                    db.collection("users")
                                                                                                            .whereEqualTo("username", driverUsername)
                                                                                                            .get()
                                                                                                            .addOnSuccessListener(driverQuery -> {
                                                                                                                if (!driverQuery.isEmpty()) {
                                                                                                                    DocumentSnapshot driverDoc = driverQuery.getDocuments().get(0);

                                                                                                                    db.collection("users").whereEqualTo("username", username).get()
                                                                                                                            .addOnSuccessListener(passengerQuery -> {
                                                                                                                                if (!passengerQuery.isEmpty()) {
                                                                                                                                    DocumentSnapshot passengerDoc = passengerQuery.getDocuments().get(0);

                                                                                                                                    double preferenceScore = calculatePreferenceScore(passengerDoc, driverDoc);
                                                                                                                                    double matchScore = (matchPercent * 0.6) + (preferenceScore * 0.4);
                                                                                                                                    boolean test = matchScore >= 60.0;
                                                                                                                                    Log.d("MatchScore", "Overlap Percent = " + matchPercent + ", Preference Score = " + preferenceScore + ", Match Score = " + matchScore + ", Boolean = " + test + ", Driver = " + driverUsername);

                                                                                                                                    if (matchScore >= 60.0) {
                                                                                                                                        String rideId = doc.getId();
                                                                                                                                        String status = doc.getString("status");
                                                                                                                                        Timestamp time = doc.getTimestamp("departureTime");
                                                                                                                                        //RideItem rideItem = new RideItem(driverUsername, null, null, doc.getGeoPoint("origin"), doc.getGeoPoint("destination"), null, time.toDate().toString());
                                                                                                                                        RideItem rideItem = new RideItem(rideId, driverUsername, null, null, originGeo, destinationGeo, null, time.toDate().toString(), status, matchScore);
                                                                                                                                        matchedRides.add(rideItem);
                                                                                                                                        //matchedRidesWithScores.add(new Pair<>(rideItem, matchScore));
                                                                                                                                    }
                                                                                                                                }
                                                                                                                            });
                                                                                                                }
                                                                                                            });
                                                                                                }

                                                                                            /*if (processedCount[0] == totalRides) {
                                                                                                requireActivity().runOnUiThread(() -> {
                                                                                                    if (!matchedRides.isEmpty()) {
                                                                                                        //Collections.sort(matchedRidesWithScores, (a, b) -> Double.compare(b.second, a.second));

                                                                                                        //ArrayList<RideItem> sortedMatchedRides = new ArrayList<>();
                                                                                                        //for (Pair<RideItem, Double> pair : matchedRidesWithScores) {
                                                                                                        //    sortedMatchedRides.add(pair.first);
                                                                                                        //}

                                                                                                        Collections.sort(matchedRides, (a, b) -> Double.compare(b.score, a.score));

                                                                                                        Intent intent = new Intent(getContext(), AvailableRideActivity.class);
                                                                                                        intent.putExtra("rideList", new Gson().toJson(matchedRides));
                                                                                                        startActivity(intent);
                                                                                                        Toast.makeText(getContext(), "Found " + matchedRides.size() + " ride(s)", Toast.LENGTH_SHORT).show();
                                                                                                    } else if(matchedRides.size() == 0) {
                                                                                                        Toast.makeText(getContext(), "No rides available", Toast.LENGTH_SHORT).show();
                                                                                                    }
                                                                                                });
                                                                                            }*/
                                                                                            });
                                                                                            // here
                                                                                            processedCount[0]++;
                                                                                            if (processedCount[0] == totalRides) {
                                                                                                requireActivity().runOnUiThread(() -> {
                                                                                                /*if (!matchedRides.isEmpty()) {
                                                                                                    //Collections.sort(matchedRidesWithScores, (a, b) -> Double.compare(b.second, a.second));

                                                                                                    //ArrayList<RideItem> sortedMatchedRides = new ArrayList<>();
                                                                                                    //for (Pair<RideItem, Double> pair : matchedRidesWithScores) {
                                                                                                    //    sortedMatchedRides.add(pair.first);
                                                                                                    //}

                                                                                                    Collections.sort(matchedRides, (a, b) -> Double.compare(b.score, a.score));

                                                                                                    Intent intent = new Intent(getContext(), AvailableRideActivity.class);
                                                                                                    intent.putExtra("rideList", new Gson().toJson(matchedRides));
                                                                                                    startActivity(intent);
                                                                                                    Toast.makeText(getContext(), "Found " + matchedRides.size() + " ride(s)", Toast.LENGTH_SHORT).show();
                                                                                                } else if(matchedRides.size() == 0) {
                                                                                                    Toast.makeText(getContext(), "No rides available", Toast.LENGTH_SHORT).show();
                                                                                                }*/
                                                                                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                                                                                        if (!matchedRides.isEmpty()) {
                                                                                                            Collections.sort(matchedRides, (a, b) -> Double.compare(b.score, a.score));
                                                                                                            Intent intent = new Intent(getContext(), AvailableRideActivity.class);
                                                                                                            intent.putExtra("rideList", new Gson().toJson(matchedRides));
                                                                                                            startActivity(intent);
                                                                                                            Toast.makeText(getContext(), "Found " + matchedRides.size() + " ride(s)", Toast.LENGTH_SHORT).show();
                                                                                                        } else {
                                                                                                            Toast.makeText(getContext(), "No rides available", Toast.LENGTH_SHORT).show();
                                                                                                        }
                                                                                                    }, 15000); // 15-second delay — enough for inner async stuff to (likely) finish
                                                                                                });
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                });
                                                                    });
                                                                }
                                                            });
                                                        }
                                                    });
                                                }
                                            });
                                        }
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

    private void fetchDirectionsPoints(LatLng origin, LatLng destination, Consumer<List<LatLng>> callback) {
        String apiKey = "AIzaSyASboo4rxLoC4QkA9ZeH5yWI4flQi_hXxU";
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&key=" + apiKey;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("DirectionsAPI", "Request failed: " + e.getMessage());
                callback.accept(new ArrayList<>());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.accept(new ArrayList<>());
                    return;
                }

                String body = response.body().string();
                try {
                    JSONObject json = new JSONObject(body);
                    JSONArray routes = json.getJSONArray("routes");
                    if (routes.length() > 0) {
                        String polyline = routes.getJSONObject(0)
                                .getJSONObject("overview_polyline")
                                .getString("points");
                        List<LatLng> decoded = PolyUtil.decode(polyline);
                        callback.accept(decoded);
                    } else {
                        callback.accept(new ArrayList<>());
                    }
                } catch (JSONException e) {
                    Log.e("DirectionsAPI", "Failed to parse JSON: " + e.getMessage());
                    callback.accept(new ArrayList<>());
                }
            }
        });
    }

    private double calculateRouteMatchPercentage(List<LatLng> driverPoints, List<LatLng> passengerPoints) {
        int matchCount = 0;
        double threshold = 0.5; // in km

        for (LatLng passengerPoint : passengerPoints) {
            for (LatLng driverPoint : driverPoints) {
                double distance = distanceBetween(passengerPoint, driverPoint);
                if (distance < threshold) {
                    matchCount++;
                    break;
                }
            }
        }

        return 100.0 * matchCount / passengerPoints.size();
    }

    private double distanceBetween(LatLng a, LatLng b) {
        float[] result = new float[1];
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, result);
        return result[0] / 1000.0;
    }

    private boolean isPassengerDirectionValid(List<LatLng> driverRoute, List<LatLng> passengerRoute, double thresholdKm) {
        LatLng passengerStart = passengerRoute.get(0);
        LatLng passengerEnd = passengerRoute.get(passengerRoute.size() - 1);

        LatLng driverStart = driverRoute.get(0);
        LatLng driverEnd = driverRoute.get(driverRoute.size() - 1);

        int passengerOriginInDriver = findNearestIndex(driverRoute, passengerStart, DETOUR_RADIUS_KM);
        int passengerDestInDriver = findNearestIndex(driverRoute, passengerEnd, DETOUR_RADIUS_KM);
        int passengerOriginInPassenger = findNearestIndex(passengerRoute, passengerStart, DETOUR_RADIUS_KM);
        int passengerDestInPassenger = findNearestIndex(passengerRoute, passengerEnd, DETOUR_RADIUS_KM);

        int driverOriginInPassenger = findNearestIndex(passengerRoute, driverStart, DETOUR_RADIUS_KM);
        int driverDestInPassenger = findNearestIndex(passengerRoute, driverEnd, DETOUR_RADIUS_KM);
        int driverOriginInDriver = findNearestIndex(driverRoute, driverStart, DETOUR_RADIUS_KM);
        int driverDestInDriver = findNearestIndex(driverRoute, driverEnd, DETOUR_RADIUS_KM);

        // Case A: Full match (driver covers both start and end in correct direction)
        if (passengerOriginInDriver != -1 && passengerDestInDriver != -1) {
            return passengerOriginInDriver < passengerDestInDriver;
        }

        // Case B: Partial match — passenger's origin is on the driver's route,
        // and driver is going forward along the passenger’s intended path
        if (passengerOriginInDriver != -1 && passengerDestInDriver == -1) {
            Log.d("DriverDestInPassenger", driverDestInPassenger + "");
            boolean testBoolean = driverDestInPassenger > passengerOriginInPassenger;
            Log.d("DriverDestInPassenger>PassengerOriginInDriver", testBoolean + "");
            Log.d("PassengerOriginInDriver", passengerOriginInDriver + "");
            return driverDestInPassenger != -1 && driverDestInPassenger > passengerOriginInPassenger;
        }

        return false;
    }

    private int findNearestIndex(List<LatLng> route, LatLng point, double thresholdKm) {
        for (int i = 0; i < route.size(); i++) {
            if (distanceBetween(route.get(i), point) <= thresholdKm) {
                return i;
            }
        }
        return -1;
    }

    private double calculatePreferenceScore(Map<String, Object> passengerPrefs, Map<String, Object> driverPrefs) {
        int matches = 0;
        int total = 0;

        for (String key : passengerPrefs.keySet()) {
            Object passengerVal = passengerPrefs.get(key);
            Object driverVal = driverPrefs.get(key);
            if (passengerVal != null && driverVal != null) {
                total++;
                if (passengerVal.toString().equalsIgnoreCase(driverVal.toString())) {
                    matches++;
                }
            }
        }

        return total > 0 ? ((double) matches / total) * 100.0 : 0;
    }

    private double calculatePreferenceScore(DocumentSnapshot passengerDoc, DocumentSnapshot driverDoc) {
        int matches = 0;
        int total = 0;

        // 1. Same gender preference
        String passengerSameGenderPref = passengerDoc.getString("same gender preference");
        String driverSameGenderPref = driverDoc.getString("same gender preference");
        String passengerGender = passengerDoc.getString("gender");
        String driverGender = driverDoc.getString("gender");
        if (passengerSameGenderPref != null && driverSameGenderPref != null && passengerGender != null && driverGender != null) {
            total++;
            if (passengerSameGenderPref.equalsIgnoreCase("Yes") || driverSameGenderPref.equalsIgnoreCase("Yes")){
                if (driverGender.equalsIgnoreCase(passengerGender)){
                    matches++;
                }
            } else if (passengerSameGenderPref.equalsIgnoreCase("No") && driverSameGenderPref.equalsIgnoreCase("No")) {
                matches++;
            }
        }

        // 2. Smoking preference
        String smokingPref = passengerDoc.getString("smoking preference");
        String driverSmokingPref = driverDoc.getString("smoking preference");
        if (smokingPref != null && driverSmokingPref != null) {
            total++;
            if (smokingPref.equalsIgnoreCase(driverSmokingPref)) {
                matches++;
            }
        }

        // 3. Music preference
        String musicPref = passengerDoc.getString("music preference");
        String driverMusicPref = driverDoc.getString("music preference");
        if (musicPref != null && driverMusicPref != null) {
            total++;
            if (musicPref.equalsIgnoreCase(driverMusicPref)) {
                matches++;
            }
        }

        return total > 0 ? ((double) matches / total) * 100.0 : 0;
    }
}