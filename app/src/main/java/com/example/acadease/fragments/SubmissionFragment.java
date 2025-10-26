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

import java.util.ArrayList;
import java.util.List;

public class SubmissionFragment extends Fragment {

    private static final String TAG = "SubmissionsLog";

    private RecyclerView submissionsRecyclerView;
    private TextView assignmentHeader;
    private Button btnSaveGrades;

    private FacultyRepository facultyRepository;
    private LookupRepository lookupRepository;

    private String assignmentId;
    private String courseCode; // Will need to be passed if necessary

    public SubmissionFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            assignmentId = getArguments().getString("ASSIGNMENT_ID");
            // NOTE: For full safety, courseCode should also be retrieved here.
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

        if (assignmentId != null) {
            assignmentHeader.setText("Submissions for: " + assignmentId);
            loadSubmissions();
        } else {
            assignmentHeader.setText("Error: Assignment Missing");
        }

        btnSaveGrades.setOnClickListener(v -> handleSaveGrades());
    }

    private void loadSubmissions() {
        // Assumes courseCode is retrieved or passed. We will hardcode for now.
        // NOTE: This must be updated to fetch assignments/submissions correctly.
        String dummyCourseCode = "CS101";

        facultyRepository.fetchSubmissions(dummyCourseCode, assignmentId, new FacultyRepository.SubmissionListCallback() {
            @Override
            public void onSuccess(List<Submission> submissions) {
                if (getContext() == null) return;

                Log.d(TAG, "Submissions fetched: " + submissions.size());

                if (submissions.isEmpty()) {
                    Toast.makeText(getContext(), "No student submissions yet.", Toast.LENGTH_SHORT).show();
                }

                // TODO: Initialize SubmissionsAdapter and pass the list
                // SubmissionAdapter adapter = new SubmissionAdapter(getContext(), submissions, lookupRepository);
                // submissionsRecyclerView.setAdapter(adapter);
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
        // Logic to iterate over RecyclerView items, collect grades, and perform a Batched Write
        Toast.makeText(getContext(), "Saving grades... (Logic TBD)", Toast.LENGTH_SHORT).show();
        // TODO: Finalize logic to update grades in the database
    }
}