package com.example.acadease.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.acadease.R;
import com.example.acadease.data.AdminRepository;
import com.example.acadease.data.LookupRepository;
import com.example.acadease.data.StorageRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserManagementFragment extends Fragment {

    private static final String TAG = "UserMgmtFragment";

    // 1. REPOSITORY DECLARATIONS
    private AdminRepository adminRepository;
    private StorageRepository storageRepository;
    private LookupRepository lookupRepository;

    // 2. UI FIELD DECLARATIONS
    private EditText uidEt, emailEt, firstNameEt, lastNameEt;
    private EditText mobileEt, customIdEt, currentSemesterEt; // currentSemesterEt is NEW
    private AutoCompleteTextView roleAcTv, programAcTv, courseSearchAcTv;
    private LinearLayout courseSelectionContainer;
    private Button regRegisterButton, btnSelectImage, deleteButton;
    private EditText deleteUidEditText;

    // 3. CONSTANTS AND STATE
    private final String[] roles = {"student", "faculty", "admin"};
    private Uri selectedImageUri;

    // 4. CRITICAL: Activity Result Launcher
    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Toast.makeText(requireContext(), "Image selected: " + uri.getLastPathSegment(), Toast.LENGTH_SHORT).show();
                }
            }
    );

    public UserManagementFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize Repositories
        adminRepository = new AdminRepository();
        storageRepository = new StorageRepository();
        lookupRepository = new LookupRepository();

        // 2. Map ALL UI components
        uidEt = view.findViewById(R.id.reg_uid_edit_text);
        emailEt = view.findViewById(R.id.reg_email_edit_text);
        firstNameEt = view.findViewById(R.id.reg_first_name_edit_text);
        lastNameEt = view.findViewById(R.id.reg_last_name_edit_text);
        mobileEt = view.findViewById(R.id.reg_mobile_edit_text);
        customIdEt = view.findViewById(R.id.reg_custom_id_edit_text);
        currentSemesterEt = view.findViewById(R.id.reg_current_semester_et); // Mapped NEW FIELD

        roleAcTv = view.findViewById(R.id.reg_role_spinner);
        programAcTv = view.findViewById(R.id.reg_program_spinner);
        courseSearchAcTv = view.findViewById(R.id.reg_course_search_edit_text);
        courseSelectionContainer = view.findViewById(R.id.course_selection_container);

        regRegisterButton = view.findViewById(R.id.reg_register_button);
        btnSelectImage = view.findViewById(R.id.btn_select_image);

        deleteUidEditText = view.findViewById(R.id.delete_uid_edit_text);
        deleteButton = view.findViewById(R.id.btn_delete_user);

        // 3. Setup Logic
        if (roleAcTv != null) {
            setupDropdown(roleAcTv, roles);
        }
        setupProgramAutocomplete();

        // 4. Set Listeners
        if (btnSelectImage != null) btnSelectImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        if (regRegisterButton != null) regRegisterButton.setOnClickListener(v -> handleRegistration());
        if (deleteButton != null) deleteButton.setOnClickListener(v -> handleDelete());
    }

    // --- HELPER METHODS ---

    private void setupDropdown(AutoCompleteTextView view, String[] items) {
        if (view == null || items == null || items.length == 0) {
            if (view != null) view.setHint("No items available");
            if (view != null) view.setEnabled(false);
            return;
        }

        view.setEnabled(true);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                items
        );
        view.setAdapter(adapter);
        view.setText(items[0], false);
    }

    private void setupProgramAutocomplete() {
        lookupRepository.fetchProgramCodes(new LookupRepository.LookupListCallback() {
            @Override
            public void onSuccess(List<String> programCodes) {
                if (getContext() == null) return;

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        programCodes
                );
                programAcTv.setAdapter(adapter);

                if (programCodes.isEmpty()) {
                    programAcTv.setHint("No programs found");
                } else {
                    programAcTv.setText(programCodes.get(0), false);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Failed to load academic programs.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<String> getSelectedCourseCodes() {
        List<String> selectedCodes = new ArrayList<>();
        // This is now obsolete, as enrollment uses Program ID.
        // It remains here for previous checkbox structure logic reference.
        return selectedCodes;
    }

    // --- MAIN ACTION HANDLERS ---

    private void handleRegistration() {
        // 1. Capture Data (Declare all variables as FINAL)
        final String uid = uidEt.getText().toString().trim();
        final String email = emailEt.getText().toString().trim();
        final String firstName = firstNameEt.getText().toString().trim();
        final String lastName = lastNameEt.getText().toString().trim();
        final String role = roleAcTv.getText().toString();
        final String mobile = mobileEt.getText().toString().trim();
        final String customId = customIdEt.getText().toString().trim();

        // Capture and Parse Semester
        final String semesterStr = currentSemesterEt.getText().toString().trim();
        final String programId = "student".equals(role) ? programAcTv.getText().toString() : null;

        // 2. Validation (Basic checks)
        if (uid.isEmpty() || email.isEmpty() || role.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || customId.isEmpty()) {
            Toast.makeText(getContext(), "UID, Custom ID, Email, and Name are required.", Toast.LENGTH_LONG).show();
            return;
        }

        // Student-specific validation
        int semester = 0;
        if ("student".equals(role)) {
            if (programId == null || programId.isEmpty()) {
                Toast.makeText(getContext(), "Student requires selecting an academic program.", Toast.LENGTH_LONG).show();
                return;
            }
            try {
                semester = Integer.parseInt(semesterStr);
                if (semester <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Student requires a valid Semester number (1, 2, 3...).", Toast.LENGTH_LONG).show();
                return;
            }
        }

        // CRITICAL FIX: The local variable 'semester' used in the lambda must be final.
        final int finalSemester = semester;

        // 3. Execute token refresh before proceeding with registration
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "Admin not authenticated. Please log in again.", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseAuth.getInstance().getCurrentUser().getIdToken(true)
                .addOnSuccessListener(result -> {
                    // Token refreshed successfully. Proceed with the actual registration logic.
                    if (selectedImageUri != null) {
                        // Now passing the final version of the variables
                        uploadImageAndRegister(uid, email, role, firstName, lastName, mobile, customId, programId, finalSemester, selectedImageUri);
                    } else {
                        registerProfile(uid, email, role, firstName, lastName, mobile, customId, null, programId, finalSemester);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Admin verification failed. Try logging out and back in.", Toast.LENGTH_LONG).show();
                });
    }

    private void uploadImageAndRegister(String uid, String email, String role, String firstName, String lastName, String mobile, String customId, String programId, int semester, Uri imageUri) {
        Toast.makeText(getContext(), "Uploading profile image...", Toast.LENGTH_SHORT).show();

        String filePath = String.format("profiles/%s/profile.jpg", uid);

        storageRepository.uploadFile(imageUri, filePath, new StorageRepository.UploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                registerProfile(uid, email, role, firstName, lastName, mobile, customId, downloadUrl, programId, semester);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void registerProfile(String uid, String email, String role, String firstName, String lastName, String mobile, String customId, String imageUrl, String programId, int semester) {
        // Perform Batched Write (Profile + Enrollments)
        adminRepository.createProfileAndEnroll(
                uid, email, role, firstName, lastName, mobile, customId, imageUrl, programId, semester,
                new AdminRepository.RegistrationCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(getContext(), "SUCCESS: " + message, Toast.LENGTH_LONG).show();
                        // Clear fields after success
                        uidEt.setText(""); emailEt.setText(""); firstNameEt.setText(""); lastNameEt.setText("");
                        mobileEt.setText(""); customIdEt.setText(""); currentSemesterEt.setText("");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(getContext(), "Registration Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Batch Registration Failure:", e);
                    }
                }
        );
    }

    private void handleDelete() {
        String uidToDelete = deleteUidEditText.getText().toString().trim();

        if (uidToDelete.isEmpty()) {
            Toast.makeText(getContext(), "UID is required to delete a profile.", Toast.LENGTH_SHORT).show();
            return;
        }

        adminRepository.deleteProfileDocument(uidToDelete, new AdminRepository.RegistrationCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
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