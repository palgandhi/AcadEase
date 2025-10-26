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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.acadease.R;
import com.example.acadease.data.FacultyRepository;
import com.example.acadease.data.LookupRepository;
import com.example.acadease.adapters.ResultsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.example.acadease.model.User;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class ExamInputFragment extends Fragment {

    private static final String TAG = "ExamInputLog";

    private RecyclerView resultsRecyclerView;
    private TextView examHeader;
    private Button btnSaveExam;

    private FacultyRepository facultyRepository;
    private LookupRepository lookupRepository;
    private ResultsAdapter resultsAdapter;

    private String courseCode;
    private String examTitle;
    private int maxPoints = 100; // Will be fetched from exam_types later

    public ExamInputFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            courseCode = getArguments().getString("COURSE_CODE");
            examTitle = getArguments().getString("EXAM_TITLE");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Layout is essentially a simplified fragment_submissions.xml
        return inflater.inflate(R.layout.activity_exam_input_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        facultyRepository = new FacultyRepository();
        lookupRepository = new LookupRepository();

        resultsRecyclerView = view.findViewById(R.id.exam_roster_recycler_view); // New ID
        examHeader = view.findViewById(R.id.exam_input_header);
        btnSaveExam = view.findViewById(R.id.btn_save_exam_scores);

        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        if (courseCode != null && examTitle != null) {
            examHeader.setText(String.format("Enter Marks for: %s (%s)", examTitle, courseCode));
            loadRosterForExam();
        } else {
            examHeader.setText("Error: Exam details missing.");
        }

        btnSaveExam.setOnClickListener(v -> handleSaveExamScores());
    }

    private void loadRosterForExam() {
        // Step 1: Fetch UIDs of all enrolled students in the course
        facultyRepository.fetchCourseRoster(courseCode, new FacultyRepository.RosterCallback() {
            @Override
            public void onSuccess(List<String> studentUids) {
                if (studentUids.isEmpty()) return;

                // Step 2: Bulk Lookup Student Profiles (Names)
                lookupRepository.fetchBulkStudentProfiles(studentUids, new LookupRepository.BulkProfileCallback() {
                    @Override
                    public void onSuccess(List<User> students) {
                        // 3. Attach Results Adapter for manual score entry
                        // NOTE: Initial grades map is empty; faculty starts entering marks.
                        resultsAdapter = new ResultsAdapter(requireContext(), students, maxPoints, lookupRepository);
                        resultsRecyclerView.setAdapter(resultsAdapter);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(getContext(), "Failed to load student roster names.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Error fetching roster.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSaveExamScores() {
        if (resultsAdapter == null) return;

        // 1. Get Grades from Adapter
        Map<String, Integer> gradesMap = resultsAdapter.getAllGrades();

        if (gradesMap.isEmpty()) {
            Toast.makeText(getContext(), "No marks have been entered.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Call Repository to save scores to the new 'exam_scores' subcollection
        facultyRepository.saveExamScores(courseCode, examTitle, maxPoints, gradesMap, new FacultyRepository.RegistrationCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                // Return to the results dashboard
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Exam score save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}