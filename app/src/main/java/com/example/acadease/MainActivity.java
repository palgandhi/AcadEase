package com.example.acadease;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.acadease.data.UserRepository;
import com.example.acadease.model.User;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private UserRepository userRepository;

    // UI elements (assuming simple layout with two fields and one button)
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Assuming your layout file is activity_main.xml

        // 1. Initialize the Repository
        userRepository = new UserRepository();

        // 2. Map UI elements (IDs will vary based on your XML, use correct IDs)
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginButton = findViewById(R.id.login_button);

        // 3. Set the Login button listener
        loginButton.setOnClickListener(v -> handleLogin());

        // Optional: Check if a user is already authenticated
//        checkCurrentAuthStatus();
    }

    private void checkCurrentAuthStatus() {
        if (userRepository.getCurrentFirebaseUser() != null) {
            // User is already logged in, skip login screen
            String uid = userRepository.getCurrentFirebaseUser().getUid();
            Log.i(TAG, "User already logged in. UID: " + uid);

            // Immediately fetch profile data and proceed to next screen
            userRepository.fetchUserProfile(uid, new UserRepository.LoginCallback() {
                @Override
                public void onSuccess(User user) {
                    Toast.makeText(MainActivity.this, "Welcome back, " + user.getName() + "!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Auto-login complete. Role: " + user.getRole());

                    // Role-based navigation (same as handleLogin)
                    Intent intent;
                    String role = user.getRole() != null ? user.getRole().toLowerCase() : "";

                    if ("student".equals(role)) {
                        intent = new Intent(MainActivity.this, StudentDashboardActivity.class);
                    } else if ("faculty".equals(role)) {
                        intent = new Intent(MainActivity.this, FacultyDashboardActivity.class);
                    } else if ("admin".equals(role)) {
                        intent = new Intent(MainActivity.this, AdminDashboardActivity.class);
                    } else {
                        Toast.makeText(MainActivity.this, "Error: Unknown role assigned.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    intent.putExtra("USER_UID", user.getUid());
                    intent.putExtra("USER_ROLE", user.getRole());
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Auto-login failed to retrieve profile: " + e.getMessage());
                    // User stays on login screen
                }
            });
        }
    }

    private void handleLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and password cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Call the core login logic from the Repository
        userRepository.loginUser(email, password, new UserRepository.LoginCallback() {
            @Override
            public void onSuccess(User user) {
                Toast.makeText(MainActivity.this, "Login Successful! Role: " + user.getRole(), Toast.LENGTH_LONG).show();
                Log.i(TAG, "Login complete. User role is: " + user.getRole());

                // --- CRITICAL ROLE-BASED NAVIGATION ---
                Intent intent;
                String role = user.getRole();

                if ("student".equals(role.toLowerCase())) {
                    intent = new Intent(MainActivity.this, StudentDashboardActivity.class);
                } else if ("faculty".equals(role.toLowerCase())) {
                    intent = new Intent(MainActivity.this, FacultyDashboardActivity.class);
                } else if ("admin".equals(role.toLowerCase())) {
                    intent = new Intent(MainActivity.this, AdminDashboardActivity.class);
                } else {
                    // Should not happen if roles are strictly enforced
                    Toast.makeText(MainActivity.this, "Error: Unknown role assigned.", Toast.LENGTH_LONG).show();
                    return;
                }

                // Pass the User object or critical UID/Role to the next activity
                intent.putExtra("USER_UID", user.getUid());
                intent.putExtra("USER_ROLE", user.getRole());

                startActivity(intent);
                finish(); // Close the login activity so the user cannot navigate back
            }

            @Override
            public void onFailure(Exception e) {
                // Failed Login
                Toast.makeText(MainActivity.this, "Login Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Login attempt failure: " + e.getMessage());
            }
        });
    }
}