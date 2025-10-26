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

import com.example.acadease.R;
import com.example.acadease.data.FacultyRepository;
import com.example.acadease.data.LookupRepository;
import com.example.acadease.adapters.ResultsAdapter;
import com.example.acadease.model.Assignment;
import com.example.acadease.model.Course;
import com.example.acadease.model.User;
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

    private FacultyRepository facultyRepository;
    private LookupRepository lookupRepository;
    private ResultsAdapter resultsAdapter;

    private String selectedCourseCode;
    private String selectedExamType;
    private int maxExamPoints = 100; // Default or fetched max points
    private List<User> currentCourseRoster = new ArrayList<>();
    private final String[] examTypes = {"Mid-Semester", "Final Theory", "Lab Practical"};
    private List<Course> coursesTaught = new ArrayList<>();
    private List<Assignment> courseAssignments = new ArrayList<>();

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
        // maxPointsDisplay = view.findViewById(R.id.max_points_display_tv); // Assuming this is mapped

        // Setup RecyclerView
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // 2. Setup Dropdowns
        setupDropdown(examTypeDropdown, examTypes);
        setupCourseDropdown();

        // 3. Set Listeners
        courseDropdown.setOnItemClickListener((adapterView, v, position, id) -> {
            String selection = (String) adapterView.getItemAtPosition(position);
            selectedCourseCode = selection.split(" - ")[0].trim();
            Log.d(TAG, "Course selected: " + selectedCourseCode);
            // Load the roster for the selected course
            loadRosterForGrading(selectedCourseCode);
        });

        examTypeDropdown.setOnItemClickListener((adapterView, v, position, id) -> {
            selectedExamType = (String) adapterView.getItemAtPosition(position);
            // NOTE: In a real app, you would load max points for the selected exam type here.
            // For now, we assume maxPoints is 100.
        });

        btnSaveResults.setOnClickListener(v -> handleSaveExamResults());
        btnUploadFinalGrade.setOnClickListener(v -> handleUploadFinalGrade());
    }

    // --- DROPDOWN SETUP ---

    private void setupDropdown(AutoCompleteTextView view, String[] items) {
        if (view == null || items == null || items.length == 0) {
            // CRITICAL FIX: Add check for empty array and return immediately
            // This prevents the ArrayIndexOutOfBoundsException
            view.setHint("No items available"); // Set a helpful hint
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                items
        );
        view.setAdapter(adapter);
        view.setText(items[0], false); // Sets the default selection safely
    }

    public void setupCourseDropdown() {
        String facultyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Step 1: Fetch all courses taught by the faculty
        facultyRepository.fetchCoursesTaught(facultyUid, new FacultyRepository.CourseListCallback() {
            @Override
            public void onSuccess(List<Course> courses) {
                coursesTaught = courses;

                // Step 2: Fetch all defined exam types
                facultyRepository.fetchExamTypes(new FacultyRepository.ExamTypeCallback() {
                    @Override
                    public void onSuccess(List<String> examTitles) {
                        // Step 3: Combine and display the dropdowns
                        populateGradingDropdowns(courses, examTitles);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(getContext(), "Failed to load exam types.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Failed to load assigned courses.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateGradingDropdowns(List<Course> courses, List<String> examTitles) {
        // 1. Create Dropdown 1: Course Selection (Remains the same)
        List<String> courseDisplayList = courses.stream()
                .map(c -> String.format("%s - %s", c.getCourseCode(), c.getTitle()))
                .collect(Collectors.toList());

        // Setup the course dropdown (as before)

        // 2. Create Dropdown 2: Exam Type Selection (Dynamic List)
        String[] examArray = examTitles.toArray(new String[0]);
        setupDropdown(examTypeDropdown, examArray);

        // NOTE: The logic to combine Assignments and Exams into a SINGLE dropdown
        // is highly complex. For simplicity, we keep the two functions separate:
        // A) Grade Assignments (using AssignmentListFragment)
        // B) Grade Manual Exams (using this fragment's manual input)

        // If you prefer the unified list, it needs a dedicated model:
        // [CS101 - Mid-Term Exam (EXAM)], [CS101 - Report 1 (ASSIGNMENT)]
    }

    // --- ROSTER LOADING AND GRADING ---

    private void loadRosterForGrading(String courseCode) {
        // Step 1: Fetch UIDs of all enrolled students
        facultyRepository.fetchCourseRoster(courseCode, new FacultyRepository.RosterCallback() {
            @Override
            public void onSuccess(List<String> studentUids) {
                if (studentUids.isEmpty()) {
                    Toast.makeText(getContext(), "Course has no enrolled students.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Step 2: Bulk Lookup Student Profiles (Full User POJO needed)
                lookupRepository.fetchBulkStudentProfiles(studentUids, new LookupRepository.BulkProfileCallback() {
                    @Override
                    public void onSuccess(List<User> students) {
                        currentCourseRoster = students;

                        // 3. Attach Adapter
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

    private void handleSaveExamResults() {
        if (selectedCourseCode == null || selectedExamType == null || resultsAdapter == null) {
            Toast.makeText(getContext(), "Please select a Course and Exam Type first.", Toast.LENGTH_LONG).show();
            return;
        }

        // 1. Get Grades from Adapter
        Map<String, Integer> gradesMap = resultsAdapter.getAllGrades();

        if (gradesMap.isEmpty()) {
            Toast.makeText(getContext(), "No marks have been entered.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Call Repository to save grades to the exam_scores subcollection
        facultyRepository.saveExamScores(selectedCourseCode, selectedExamType, maxExamPoints, gradesMap, new FacultyRepository.RegistrationCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                // Clear adapter data or refresh view if necessary
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Exam score save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleUploadFinalGrade() {
        if (selectedCourseCode == null) {
            Toast.makeText(getContext(), "Please select a Course first.", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(getContext(), "Final Grade Upload TBD (Logic too complex for client-side demo).", Toast.LENGTH_LONG).show();

        // NOTE: This feature remains conceptual as it requires complex weighted average calculation.
    }
}