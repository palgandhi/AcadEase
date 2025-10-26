package com.example.acadease.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;

import com.example.acadease.R;
import com.example.acadease.data.FacultyRepository;
import com.example.acadease.data.LookupRepository;
import com.example.acadease.adapters.ResultsAdapter;
import com.example.acadease.model.Course;
import com.example.acadease.model.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class FacultyResultsFragment extends Fragment {

    private static final String TAG = "FACULTY_RESULTS_LOG";

    private RecyclerView resultsRecyclerView;
    private AutoCompleteTextView courseDropdown, examTypeDropdown;
    private Button btnSaveResults, btnUploadFinalGrade;
    private TextView maxPointsDisplay;
    private FloatingActionButton fabCreateExamType;

    private FacultyRepository facultyRepository;
    private LookupRepository lookupRepository;
    private ResultsAdapter resultsAdapter;

    private String selectedCourseCode;
    private String selectedExamType;
    private int maxExamPoints = 100; // Will be updated by fetchExamMaxPoints
    private List<User> currentCourseRoster = new ArrayList<>();

    public FacultyResultsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_faculty_results, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        facultyRepository = new FacultyRepository();
        lookupRepository = new LookupRepository();

        // 1. Map UI
        resultsRecyclerView = view.findViewById(R.id.results_roster_recycler_view);
        courseDropdown = view.findViewById(R.id.results_course_dropdown);
        examTypeDropdown = view.findViewById(R.id.results_exam_type_dropdown);
        btnSaveResults = view.findViewById(R.id.btn_save_course_results);
        btnUploadFinalGrade = view.findViewById(R.id.btn_upload_final_grade);
        maxPointsDisplay = view.findViewById(R.id.max_points_display_tv);
        fabCreateExamType = view.findViewById(R.id.fab_create_exam_type);


        // Setup RecyclerView
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // 2. Setup Dropdowns
        setupCourseDropdown(); // Loads courses and sets initial listener

        // 3. Set Listeners
        courseDropdown.setOnItemClickListener((adapterView, v, position, id) -> {
            String selection = (String) adapterView.getItemAtPosition(position);
            selectedCourseCode = selection.split(" - ")[0].trim();
            Log.d(TAG, "Course selected: " + selectedCourseCode);
            loadRosterForGrading(selectedCourseCode); // Load students when course is selected
        });

        examTypeDropdown.setOnItemClickListener((adapterView, v, position, id) -> {
            selectedExamType = (String) adapterView.getItemAtPosition(position);
            fetchAndSetMaxPoints(selectedExamType);
        });

        btnSaveResults.setOnClickListener(v -> handleSaveExamResults());
        btnUploadFinalGrade.setOnClickListener(v -> handleUploadFinalGrade());

        // FAB Listener to create a new exam type
        if (fabCreateExamType != null) {
            fabCreateExamType.setOnClickListener(v -> showCreateExamDialog());
        }
    }

    // --- DROPDOWN SETUP HELPERS ---

    private void setupDropdown(AutoCompleteTextView view, String[] items) {
        // CRITICAL FIX: Handles empty array gracefully
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
        view.setText(items[0], false); // Set default selection
    }

    public void setupCourseDropdown() {
        String facultyUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "DEFAULT_UID";

        facultyRepository.fetchCoursesTaught(facultyUid, new FacultyRepository.CourseListCallback() {
            @Override
            public void onSuccess(List<Course> courses) {
                if (getContext() == null || courses.isEmpty()) {
                    Toast.makeText(getContext(), "No assigned courses found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<String> courseDisplayList = courses.stream()
                        .map(c -> String.format("%s - %s", c.getCourseCode(), c.getTitle()))
                        .collect(Collectors.toList());

                setupDropdown(courseDropdown, courseDisplayList.toArray(new String[0]));

                selectedCourseCode = courses.get(0).getCourseCode();

                fetchAndSetupExamTypeDropdown();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Failed to load assigned courses.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchAndSetupExamTypeDropdown() {
        if (selectedCourseCode == null) return;

        lookupRepository.fetchExamTypeTitles(selectedCourseCode, new LookupRepository.ExamTypeTitlesCallback() {
            @Override
            public void onSuccess(List<String> examTitles) {
                if (getContext() == null) return;

                String[] examArray = examTitles.toArray(new String[0]);
                setupDropdown(examTypeDropdown, examArray);

                if (examArray.length > 0) {
                    selectedExamType = examArray[0];
                    fetchAndSetMaxPoints(selectedExamType);
                } else {
                    selectedExamType = null;
                    maxExamPoints = 0;
                    if (maxPointsDisplay != null) maxPointsDisplay.setText("Max: N/A");
                    Toast.makeText(getContext(), "No exam types defined for this course.", Toast.LENGTH_LONG).show();
                }

                // Load the student roster after both course and exam type are known
                loadRosterForGrading(selectedCourseCode);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Failed to load exam categories.", Toast.LENGTH_SHORT).show();
                setupDropdown(examTypeDropdown, new String[]{});
            }
        });
    }

    private void fetchAndSetMaxPoints(String examTitle) {
        if (selectedCourseCode == null || examTitle == null) return;

        facultyRepository.fetchExamMaxPoints(selectedCourseCode, examTitle, new FacultyRepository.ExamDetailCallback() {
            @Override
            public void onSuccess(int points) {
                maxExamPoints = points;
                if (maxPointsDisplay != null) maxPointsDisplay.setText("Max: " + maxExamPoints);
                if (resultsAdapter != null) resultsAdapter.notifyDataSetChanged();
            }
            @Override
            public void onFailure(Exception e) {
                maxExamPoints = 0;
                if (maxPointsDisplay != null) maxPointsDisplay.setText("Max: N/A");
                Toast.makeText(getContext(), "Max Points for exam not found in DB.", Toast.LENGTH_SHORT).show();
                if (resultsAdapter != null) resultsAdapter.notifyDataSetChanged();
            }
        });
    }


    // --- ROSTER LOADING ---

    private void loadRosterForGrading(String courseCode) {
        // Step 1: Fetch UIDs of all enrolled students
        facultyRepository.fetchCourseRoster(courseCode, new FacultyRepository.RosterCallback() {
            @Override
            public void onSuccess(List<String> studentUids) {
                if (studentUids.isEmpty()) {
                    Toast.makeText(getContext(), "Course has no enrolled students.", Toast.LENGTH_SHORT).show();
                    resultsRecyclerView.setAdapter(null);
                    currentCourseRoster = new ArrayList<>();
                    return;
                }

                // Step 2: Bulk Lookup Student Profiles
                lookupRepository.fetchBulkStudentProfiles(studentUids, new LookupRepository.BulkProfileCallback() {
                    @Override
                    public void onSuccess(List<User> students) {
                        currentCourseRoster = students;

                        // 3. Attach Adapter
                        // NOTE: Adapter now uses the current maxExamPoints value
                        resultsAdapter = new ResultsAdapter(requireContext(), students, maxExamPoints, lookupRepository);
                        resultsRecyclerView.setAdapter(resultsAdapter);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(getContext(), "Failed to load student names for roster.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Error fetching roster.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- GRADING AND SAVE LOGIC ---

    private void handleSaveExamResults() {
        if (selectedCourseCode == null || selectedExamType == null || resultsAdapter == null) {
            Toast.makeText(getContext(), "Please select a Course and Exam Type first.", Toast.LENGTH_LONG).show();
            return;
        }

        if (maxExamPoints == 0) {
            Toast.makeText(getContext(), "Max Marks are 0. Cannot save scores.", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Integer> gradesMap = resultsAdapter.getAllGrades();

        if (gradesMap.isEmpty()) {
            Toast.makeText(getContext(), "No marks have been entered.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Call Repository to save grades to the exam_scores subcollection
        facultyRepository.saveExamScores(selectedCourseCode, selectedExamType, maxExamPoints, gradesMap, new FacultyRepository.RegistrationCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                loadRosterForGrading(selectedCourseCode); // Refresh roster
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Exam score save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Exam score save failed: " + e.getMessage());
            }
        });
    }

    private void handleUploadFinalGrade() {
        if (selectedCourseCode == null) {
            Toast.makeText(getContext(), "Please select a Course first.", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(getContext(), "Final Grade Upload initiated (Concept Only).", Toast.LENGTH_LONG).show();
    }

    // --- DIALOG LOGIC ---

    private void showCreateExamDialog() {
        if (selectedCourseCode == null) {
            Toast.makeText(getContext(), "Please select a Course first.", Toast.LENGTH_LONG).show();
            return;
        }

        // Inflate the custom dialog layout
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_exam, null);

        final EditText examNameEt = dialogView.findViewById(R.id.dialog_exam_name_et);
        final EditText maxMarksEt = dialogView.findViewById(R.id.dialog_max_marks_et);

        new AlertDialog.Builder(requireContext())
                .setTitle("Define New Exam Category for " + selectedCourseCode)
                .setView(dialogView)
                .setPositiveButton("CREATE", (dialog, which) -> {
                    String examName = examNameEt.getText().toString().trim();
                    String maxMarksStr = maxMarksEt.getText().toString().trim();

                    if (examName.isEmpty() || maxMarksStr.isEmpty()) {
                        Toast.makeText(getContext(), "Both fields are required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int maxMarks;
                    try {
                        maxMarks = Integer.parseInt(maxMarksStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "Max Marks must be a number.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Call repository to save the new exam type
                    facultyRepository.addNewExamType(selectedCourseCode, examName, maxMarks, new FacultyRepository.RegistrationCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                            // Refresh the exam types dropdown after creation
                            fetchAndSetupExamTypeDropdown();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(getContext(), "Error creating exam: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }
}