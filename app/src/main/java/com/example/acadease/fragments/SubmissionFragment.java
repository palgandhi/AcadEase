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
import android.util.Log;

import com.example.acadease.R;
import com.example.acadease.data.FacultyRepository;
import com.example.acadease.data.LookupRepository;
import com.example.acadease.model.Submission;
import com.example.acadease.adapters.SubmissionAdapter; // New adapter needed
import com.example.acadease.model.Assignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Map;

public class SubmissionFragment extends Fragment {

    private static final String TAG = "SubmissionsLog";

    private RecyclerView submissionsRecyclerView;
    private TextView assignmentHeader;
    private Button btnSaveGrades;

    private FacultyRepository facultyRepository;
    private LookupRepository lookupRepository;

    private String assignmentId;
    private String courseCode; // Passed from previous screen

    private Date assignmentDueDate;
    private int maxPoints;
    private SubmissionAdapter adapter;

    public SubmissionFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            assignmentId = getArguments().getString("ASSIGNMENT_ID");
            courseCode = getArguments().getString("COURSE_CODE");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // New layout needed: fragment_submissions.xml
        return inflater.inflate(R.layout.activity_submission_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        facultyRepository = new FacultyRepository();
        lookupRepository = new LookupRepository();

        submissionsRecyclerView = view.findViewById(R.id.submissions_recycler_view);
        assignmentHeader = view.findViewById(R.id.submissions_header);
        btnSaveGrades = view.findViewById(R.id.btn_save_grades);

        submissionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        if (assignmentId != null && courseCode != null) {
            assignmentHeader.setText("Loading submissions...");
            fetchAssignmentAndLoad();
        } else {
            assignmentHeader.setText("Error: Missing assignment or course.");
        }

        btnSaveGrades.setOnClickListener(v -> handleSaveGrades());
    }

    private void fetchAssignmentAndLoad() {
        facultyRepository.fetchAssignmentDetails(courseCode, assignmentId, new FacultyRepository.AssignmentDetailCallback() {
            @Override
            public void onSuccess(Assignment assignment) {
                assignmentDueDate = assignment.getDueDate() != null ? assignment.getDueDate().toDate() : null;
                maxPoints = assignment.getMaxPoints();

                String header = String.format("Submissions for: %s | Max Points: %d", assignment.getTitle(), maxPoints);
                assignmentHeader.setText(header);

                loadSubmissions();
            }

            @Override
            public void onFailure(Exception e) {
                assignmentHeader.setText("Failed to load assignment details.");
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadSubmissions() {
        facultyRepository.fetchSubmissions(courseCode, assignmentId, new FacultyRepository.SubmissionListCallback() {
            @Override
            public void onSuccess(List<Submission> submissions) {
                if (getContext() == null) return;

                Log.d(TAG, "Submissions fetched: " + submissions.size());

                if (submissions.isEmpty()) {
                    Toast.makeText(getContext(), "No student submissions yet.", Toast.LENGTH_SHORT).show();
                }

                adapter = new SubmissionAdapter(requireContext(), submissions, assignmentDueDate, maxPoints, lookupRepository);
                submissionsRecyclerView.setAdapter(adapter);
            }

            @Override
            public void onFailure(Exception e) {
                if (getContext() == null) return;
                Log.e(TAG, "Failed to load submissions: " + e.getMessage());
                Toast.makeText(getContext(), "Error loading submissions.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleSaveGrades() {
        if (adapter == null) {
            Toast.makeText(getContext(), "No submissions to grade.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Integer> gradesMap = adapter.getAllGrades();
        if (gradesMap.isEmpty()) {
            Toast.makeText(getContext(), "No grades entered.", Toast.LENGTH_SHORT).show();
            return;
        }

        facultyRepository.updateSubmissionGrades(courseCode, assignmentId, gradesMap, new FacultyRepository.RegistrationCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                // Refresh list to reflect graded marks
                loadSubmissions();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Failed to save grades: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}