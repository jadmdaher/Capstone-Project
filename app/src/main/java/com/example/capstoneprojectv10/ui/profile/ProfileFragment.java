package com.example.capstoneprojectv10.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.capstoneprojectv10.MainActivity;
import com.example.capstoneprojectv10.R;
import com.example.capstoneprojectv10.databinding.FragmentProfileBinding;
import com.example.capstoneprojectv10.ui.authentication.LoginActivity;
import com.example.capstoneprojectv10.ui.availablerides.AvailableRideActivity;
import com.example.capstoneprojectv10.ui.newride.NewRideViewModel;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ImageView profileImage;
    ActivityResultLauncher<Intent> imagePickLauncher;
    private Uri selectedImageUri;
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String username;
    private Button btnLogout;
    private ProgressBar progressBar;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ProfileFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ProfileFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ProfileFragment newInstance(String param1, String param2) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        if(data != null && data.getData() != null){
                            selectedImageUri = data.getData();
                            Glide.with(this)
                                    .load(selectedImageUri)
                                    .apply(RequestOptions.circleCropTransform())
                                    .into(binding.profileImage);

                            // Upload image after selecting it
                            uploadImageToFirebase();
                        }
                    }
                });

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding = FragmentProfileBinding.bind(view);
        profileImage = binding.profileImage;
        progressBar = binding.progressBar;

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", getContext().MODE_PRIVATE);
        username = sharedPreferences.getString("username", null);

        profileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickLauncher.launch(intent);
        });

        if (username != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .whereEqualTo("username", username)
                    .get()
                    .addOnSuccessListener(querySnapshots -> {
                        if (!querySnapshots.isEmpty()) {
                            DocumentSnapshot document = querySnapshots.getDocuments().get(0);

                            String firstName = document.getString("first name");
                            String lastName = document.getString("last name");
                            String phone = document.getString("phone");
                            String role = document.getString("role");

                            // Now display them in your layout
                            binding.tvName.setText(firstName + " " + lastName);
                            binding.tvFirstname.setText(firstName);
                            binding.tvLastname.setText(lastName);
                            binding.tvUsername.setText("@" + username);
                            binding.tvPhoneNumber.setText(phone);
                            binding.tvRole.setText(role);
                            String imageUrl = document.getString("profileImageUrl");
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(imageUrl)
                                        .apply(RequestOptions.circleCropTransform())
                                        .into(binding.profileImage);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to load profile info.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
        }

        btnLogout = binding.btnLogout;

        // Animate the click of the button
        btnLogout.setOnTouchListener((v, event) -> {
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

        // Specify what happens when the logout button is clicked
        btnLogout.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            btnLogout.setEnabled(false);

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear(); // or remove("username")
            editor.apply();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        });
    }

    private void uploadImageToFirebase() {
        if (selectedImageUri == null || username == null) return;

        StorageReference ref = storage.getReference().child("profile_images/" + username + ".jpg");
        ref.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    // Save the image URL to Firestore
                    db.collection("users")
                            .whereEqualTo("username", username)
                            .get()
                            .addOnSuccessListener(query -> {
                                if (!query.isEmpty()) {
                                    String docId = query.getDocuments().get(0).getId();
                                    db.collection("users").document(docId).update("profileImageUrl", uri.toString());
                                    Toast.makeText(getContext(), "Profile picture updated", Toast.LENGTH_SHORT).show();
                                }
                            });
                }))
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

}