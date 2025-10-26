package com.example.acadease;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.DatePickerDialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.acadease.data.FacultyRepository;
import com.example.acadease.data.StorageRepository;
import com.example.acadease.model.Assignment;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class AssignmentCreationActivity extends AppCompatActivity {

    private static final String TAG = "AssignmentCreation";

    private EditText courseCodeEt, titleEt, descriptionEt, maxPointsEt, dueDateEt;
    private Button btnSelectFile, btnPostAssignment;
    private TextView fileStatusTv;

    private FacultyRepository facultyRepository;
    private StorageRepository storageRepository;

    private Uri selectedFileUri;
    private Date selectedDueDate;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());


    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedFileUri = uri;
                    fileStatusTv.setText("File Selected: " + uri.getLastPathSegment());
                    fileStatusTv.setVisibility(View.VISIBLE);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivity_assignment_creation); // New layout needed

        facultyRepository = new FacultyRepository();
        storageRepository = new StorageRepository();

        // 1. Map UI Elements
        courseCodeEt = findViewById(R.id.assign_course_code_et);
        titleEt = findViewById(R.id.assign_title_et);
        descriptionEt = findViewById(R.id.assign_description_et);
        maxPointsEt = findViewById(R.id.assign_max_points_et);
        dueDateEt = findViewById(R.id.assign_due_date_et);
        btnSelectFile = findViewById(R.id.btn_select_file);
        btnPostAssignment = findViewById(R.id.btn_post_assignment);
        fileStatusTv = findViewById(R.id.file_status_tv);

        // Pre-fill course code if passed from the FacultyAssignmentFragment (optional)
        String passedCourseCode = getIntent().getStringExtra("COURSE_CODE");
        if (passedCourseCode != null) {
            courseCodeEt.setText(passedCourseCode);
            courseCodeEt.setEnabled(false); // Prevent changes
        }

        // 2. Set Listeners
        btnSelectFile.setOnClickListener(v -> filePickerLauncher.launch("*/*")); // Allows selection of any file
        dueDateEt.setOnClickListener(v -> showDatePicker());
        btnPostAssignment.setOnClickListener(v -> handlePostAssignment());
    }

    private void handlePostAssignment() {
        String courseCode = courseCodeEt.getText().toString().trim();
        String title = titleEt.getText().toString().trim();
        String description = descriptionEt.getText().toString().trim();
        String maxPointsStr = maxPointsEt.getText().toString().trim();
        int maxPoints = maxPointsStr.isEmpty() ? 0 : Integer.parseInt(maxPointsStr);

        String facultyUid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        // 1. Validation
        if (courseCode.isEmpty() || title.isEmpty() || maxPoints == 0 || selectedDueDate == null) {
            Toast.makeText(this, "Course, Title, Points, and Due Date are required.", Toast.LENGTH_LONG).show();
            return;
        }

        // 2. File Upload OR Direct Write
        if (selectedFileUri != null) {
            uploadFileAndCreateAssignment(courseCode, title, description, maxPoints, facultyUid);
        } else {
            // Write assignment document without a file URL
            createAssignmentDocument(courseCode, title, description, null, maxPoints, facultyUid);
        }
    }

    private void uploadFileAndCreateAssignment(String courseCode, String title, String description, int maxPoints, String facultyUid) {
        Toast.makeText(this, "Uploading assignment file...", Toast.LENGTH_SHORT).show();

        // 1. Define storage path for the assignment file
        String filePath = String.format("assignments/%s/%s/%s",
                courseCode,
                System.currentTimeMillis(), // Unique path component
                selectedFileUri.getLastPathSegment());

        storageRepository.uploadFile(selectedFileUri, filePath, new StorageRepository.UploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                // 2. File uploaded, now create the Firestore document
                createAssignmentDocument(courseCode, title, description, downloadUrl, maxPoints, facultyUid);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AssignmentCreationActivity.this, "File upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    Calendar selectedDateCal = Calendar.getInstance();
                    selectedDateCal.set(y, m, d);

                    // Set time to end of day (23:59:59) for due date validity
                    selectedDateCal.set(Calendar.HOUR_OF_DAY, 23);
                    selectedDateCal.set(Calendar.MINUTE, 59);
                    selectedDateCal.set(Calendar.SECOND, 59);

                    selectedDueDate = selectedDateCal.getTime(); // Save the Date object

                    // Format for display
                    SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    dueDateEt.setText(displayFormat.format(selectedDueDate));
                },
                year, month, day
        );
        datePickerDialog.show();
    }


    private void createAssignmentDocument(String courseCode, String title, String description, String fileUrl, int maxPoints, String facultyUid) {
        Assignment assignment = new Assignment();
        assignment.setCourseCode(courseCode);
        assignment.setTitle(title);
        assignment.setDescription(description);
        assignment.setFileUrl(fileUrl);
        assignment.setDueDate(new Timestamp(selectedDueDate));
        assignment.setMaxPoints(maxPoints);
        assignment.setFacultyId(facultyUid);

        facultyRepository.createAssignment(assignment, courseCode, new FacultyRepository.RegistrationCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(AssignmentCreationActivity.this, message, Toast.LENGTH_LONG).show();
                finish(); // Close activity on success
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AssignmentCreationActivity.this, "Assignment post failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}