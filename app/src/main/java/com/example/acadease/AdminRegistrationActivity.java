package com.example.acadease;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.AutoCompleteTextView; // Correct import for the modern Spinner UI

import com.example.acadease.data.AdminRepository;

public class AdminRegistrationActivity extends AppCompatActivity {
    private static final String TAG = "AdminRegActivity";

    // UI components
    private EditText uidEditText, emailEditText, firstNameEditText, lastNameEditText;
    // CORRECT TYPE: AutoCompleteTextView is used for the Exposed Dropdown Menu
    private AutoCompleteTextView roleAutoCompleteTextView;
    private Button registerButton;

    private AdminRepository adminRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_registration);

        // Initialize AdminRepository
        adminRepository = new AdminRepository();

        // 1. Map UI components (using final IDs from the XML)
        uidEditText = findViewById(R.id.reg_uid_edit_text);
        emailEditText = findViewById(R.id.reg_email_edit_text);
        firstNameEditText = findViewById(R.id.reg_first_name_edit_text);
        lastNameEditText = findViewById(R.id.reg_last_name_edit_text);
        // FIX: Map the ID to the correct Material component type
        roleAutoCompleteTextView = findViewById(R.id.reg_role_spinner);
        registerButton = findViewById(R.id.reg_register_button);

        // 2. Setup Role Dropdown (Exposed Dropdown Menu)
        String[] roles = {"student", "faculty", "admin"};
        // Use a standard simple dropdown item layout
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                roles
        );

        // Attach the adapter to the AutoCompleteTextView
        roleAutoCompleteTextView.setAdapter(adapter);

        // Optional: Set the default text and ensure the list shows on focus
        roleAutoCompleteTextView.setText(roles[0], false);

        // 3. Set Registration Listener
        registerButton.setOnClickListener(v -> handleRegistration());
    }

    private void handleRegistration() {
        // 1. Capture Data
        String uid = uidEditText.getText().toString().trim(); // CRITICAL: The pasted Auth UID
        String email = emailEditText.getText().toString().trim();
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String role = roleAutoCompleteTextView.getText().toString(); // Read the selected text

        // 2. Validation
        if (uid.isEmpty() || email.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || role.isEmpty()) {
            Toast.makeText(this, "All fields, including Auth UID, are required.", Toast.LENGTH_LONG).show();
            return;
        }
        if (uid.length() < 20) {
            Toast.makeText(this, "UID appears incomplete. Must be 28 characters.", Toast.LENGTH_LONG).show();
            return;
        }

        // 3. Core Firestore Profile Creation
        // NOTE: This assumes the user was manually created in Firebase Auth already.
        adminRepository.createProfileDocument(uid, email, role, firstName, lastName, new AdminRepository.RegistrationCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(AdminRegistrationActivity.this, "SUCCESS: " + message, Toast.LENGTH_LONG).show();
                Log.i(TAG, message);
                // Clear fields on successful profile creation
                uidEditText.setText("");
                emailEditText.setText("");
                firstNameEditText.setText("");
                lastNameEditText.setText("");
            }

            @Override
            public void onFailure(Exception e) {
                // Failure often means PERMISSION_DENIED (rules) or network issue.
                Toast.makeText(AdminRegistrationActivity.this, "Profile Creation Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Registration Failure: " + e.getMessage());
            }
        });
    }
}