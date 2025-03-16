package com.example.capstoneprojectv10.ui.newride;

import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.capstoneprojectv10.MainActivity;
import com.example.capstoneprojectv10.R;
import com.example.capstoneprojectv10.ui.availablerides.AvailableRideActivity;

public class NewRideFragment extends Fragment {

    public static NewRideFragment newInstance() {
        return new NewRideFragment();
    }

    private NewRideViewModel mViewModel;
    EditText etDepartureLocation;
    EditText etArrivalLocation;
    EditText etDepartureTime;
    Button btnSearch;

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
        etArrivalLocation = view.findViewById(R.id.et_arrival_location);
        etDepartureTime = view.findViewById(R.id.et_departure_time);
        btnSearch = view.findViewById(R.id.btn_search);

        btnSearch.setOnClickListener(v -> {
            String departure = etDepartureLocation.getText().toString().trim();
            String arrival = etArrivalLocation.getText().toString().trim();
            String departureTime = etDepartureTime.getText().toString().trim();

            if (departure.isEmpty() || arrival.isEmpty() || departureTime.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            } else {
                /*NavController navController = Navigation.findNavController(view);
                Bundle args = new Bundle();
                args.putString("departure", departure);
                args.putString("arrival", arrival);
                navController.navigate(R.id.action_newRideFragment_to_availableRidesFragment, args);
                Intent intent = new Intent(getActivity(), AvailableRideActivity.class);
                intent.putExtra("DEPARTURE_LOCATION", departure);
                intent.putExtra("ARRIVAL_LOCATION", arrival);
                intent.putExtra("DEPARTURE_TIME", departureTime);
                startActivity(intent);*/

                // Create Intent to open AvailableRideActivity
                Intent intent = new Intent(getActivity(), AvailableRideActivity.class);
                intent.putExtra("DEPARTURE_LOCATION", departure);
                intent.putExtra("ARRIVAL_LOCATION", arrival);
                intent.putExtra("DEPARTURE_TIME", departureTime);
                startActivity(intent);
            }

            // Create Intent to open SearchResultsActivity
            /*Intent intent = new Intent(getActivity(), MainActivity.class);

            // Pass data to the new activity
            intent.putExtra("DEPARTURE_LOCATION", departure);
            intent.putExtra("ARRIVAL_LOCATION", arrival);
            intent.putExtra("DEPARTURE_TIME", departureTime);

            startActivity(intent); // Start the new activity*/

            // Create Intent to open AvailableRideActivity
            //CANNOT use Intent for fragment, AvailableRides will have to be a new Activity.
        });
    }


}