package com.example.acadease.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.acadease.R;
import com.example.acadease.data.AdminRepository;

public class UserManagementFragment extends Fragment {

    private static final String TAG = "UserMgmtFragment";
    private AdminRepository adminRepository;

    // UI elements for Registration
    private EditText regUidEditText, regEmailEditText, regFirstNameEditText, regLastNameEditText;
    private AutoCompleteTextView roleAutoCompleteTextView;
    private Button regRegisterButton;

    // UI elements for Deletion
    private EditText deleteUidEditText;
    private Button deleteButton;

    public UserManagementFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adminRepository = new AdminRepository();

        // 1. Map Registration UI Components (Using IDs from the included layout)
        regUidEditText = view.findViewById(R.id.reg_uid_edit_text);
        regEmailEditText = view.findViewById(R.id.reg_email_edit_text);
        regFirstNameEditText = view.findViewById(R.id.reg_first_name_edit_text);
        regLastNameEditText = view.findViewById(R.id.reg_last_name_edit_text);
        roleAutoCompleteTextView = view.findViewById(R.id.reg_role_spinner);
        regRegisterButton = view.findViewById(R.id.reg_register_button);

        // 2. Map Deletion UI Components
        deleteUidEditText = view.findViewById(R.id.delete_uid_edit_text);
        deleteButton = view.findViewById(R.id.btn_delete_user);

        // 3. Setup Role Dropdown
        String[] roles = {"student", "faculty", "admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                roles
        );
        roleAutoCompleteTextView.setAdapter(adapter);
        roleAutoCompleteTextView.setText(roles[0], false); // Set default

        // 4. Set Listeners
        regRegisterButton.setOnClickListener(v -> handleRegistration());
        deleteButton.setOnClickListener(v -> handleDelete());
    }

    private void handleRegistration() {
        String uid = regUidEditText.getText().toString().trim();
        String email = regEmailEditText.getText().toString().trim();
        String firstName = regFirstNameEditText.getText().toString().trim();
        String lastName = regLastNameEditText.getText().toString().trim();
        String role = roleAutoCompleteTextView.getText().toString();

        if (uid.isEmpty() || email.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || role.isEmpty()) {
            Toast.makeText(getContext(), "All fields, including Auth UID, are required.", Toast.LENGTH_LONG).show();
            return;
        }

        // Call the repository method to create the Firestore profile
        adminRepository.createProfileDocument(uid, email, role, firstName, lastName, new AdminRepository.RegistrationCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(getContext(), "SUCCESS: " + message, Toast.LENGTH_LONG).show();
                // Clear fields
                regUidEditText.setText("");
                regEmailEditText.setText("");
                regFirstNameEditText.setText("");
                regLastNameEditText.setText("");
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Profile Creation Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Registration Failure: " + e.getMessage());
            }
        });
    }

    private void handleDelete() {
        String uidToDelete = deleteUidEditText.getText().toString().trim();

        if (uidToDelete.isEmpty()) {
            Toast.makeText(getContext(), "UID is required to delete a profile.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Call the repository method to delete the Firestore profile
        adminRepository.deleteProfileDocument(uidToDelete, new AdminRepository.RegistrationCallback() {
            @Override
            public void onSuccess(String message) {
                // The message includes the warning about manual Auth deletion
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                Log.w(TAG, message);
                deleteUidEditText.setText("");
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Deletion Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Deletion Failure: " + e.getMessage());
            }
        });
    }
}