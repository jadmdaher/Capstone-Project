package com.example.capstoneprojectv10.ui.authentication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.window.OnBackInvokedDispatcher;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.capstoneprojectv10.MainActivity;
import com.example.capstoneprojectv10.R;
import com.example.capstoneprojectv10.databinding.ActivityLoginBinding;
import com.example.capstoneprojectv10.ui.availablerides.AvailableRideActivity;

import org.w3c.dom.Text;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private EditText usernameEditText;
    private EditText passwordEditText;
    private TextView signUpLinkText;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        EdgeToEdge.enable(this);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        /*getSupportActionBar().hide();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);*/

        usernameEditText = binding.etUsername;
        passwordEditText = binding.etPassword;
        signUpLinkText = binding.tvSignUpLink;
        loginButton = binding.btnLogin;

        // ClickListner for login button
        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            // Finish the LoginActivity
            //finish();
        });

        // ClickListener for login text
        signUpLinkText.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignUpActivity.class);
            startActivity(intent);
            // Finish the LoginActivity
            //finish();
        });
    }

    public void goBack(View view) {
        finish(); // Closes the activity when the back button is clicked
    }

    /*@NonNull
    @Override
    public OnBackInvokedDispatcher getOnBackInvokedDispatcher() {
        return super.getOnBackInvokedDispatcher();
    }*/
}