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
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.example.acadease.R;
import com.example.acadease.data.FacultyRepository;
import com.example.acadease.model.Assignment;
import com.example.acadease.adapters.AssignmentAdapter;

import java.util.List;
import java.util.ArrayList;

public class AssignmentListFragment extends Fragment implements AssignmentAdapter.SubmissionListener {

    private static final String TAG = "AssignmentListLog";

    private RecyclerView assignmentRecyclerView;
    private TextView courseTitleHeader;

    private FacultyRepository facultyRepository;
    private String courseCode;
    private String courseTitle;
    private AssignmentAdapter assignmentAdapter;

    public AssignmentListFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // CRITICAL: Retrieve arguments passed from the Course Card click
        if (getArguments() != null) {
            courseCode = getArguments().getString("COURSE_CODE");
            courseTitle = getArguments().getString("COURSE_TITLE");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Assume fragment_assignment_list.xml is the correct layout
        return inflater.inflate(R.layout.activity_assignment_list_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        facultyRepository = new FacultyRepository();

        assignmentRecyclerView = view.findViewById(R.id.assignments_recycler_view);
        courseTitleHeader = view.findViewById(R.id.assignment_list_header);

        assignmentRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        if (courseCode != null) {
            courseTitleHeader.setText(String.format("Assignments for: %s", courseTitle));
            loadAssignments();
        } else {
            courseTitleHeader.setText("Error: Course Not Selected");
            Toast.makeText(getContext(), "Course code is missing.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadAssignments() {
        // Fetch assignments for this specific course code
        facultyRepository.fetchAssignmentsByCourse(courseCode, new FacultyRepository.AssignmentListCallback() {
            @Override
            public void onSuccess(List<Assignment> assignments) {
                if (getContext() == null) return;

                Log.d(TAG, "Assignments fetched: " + assignments.size());

                if (assignments.isEmpty()) {
                    Toast.makeText(getContext(), "No assignments posted yet.", Toast.LENGTH_SHORT).show();
                }

                // Initialize adapter
                assignmentAdapter = new AssignmentAdapter(requireContext(), assignments, AssignmentListFragment.this);
                assignmentRecyclerView.setAdapter(assignmentAdapter);
            }

            @Override
            public void onFailure(Exception e) {
                if (getContext() == null) return;
                Log.e(TAG, "Failed to load assignments: " + e.getMessage());
                Toast.makeText(getContext(), "Error loading assignments.", Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- Implementation of AssignmentAdapter.SubmissionListener ---

    @Override
    public void onViewSubmissionsClicked(String assignmentId, String assignmentTitle) {
        Log.i(TAG, "Executing fragment transaction for Submissions List: " + assignmentTitle);

        // 1. Prepare arguments
        Bundle args = new Bundle();
        args.putString("ASSIGNMENT_ID", assignmentId);
        // CRITICAL: We pass the course code too, as the repository needs it to look up submissions
        args.putString("COURSE_CODE", courseCode);

        // 2. Create the target fragment
        // NOTE: This assumes SubmissionsFragment.java is created and ready.
        SubmissionFragment submissionsFragment = new SubmissionFragment();
        submissionsFragment.setArguments(args);

        // 3. Execute Fragment Transaction: Replace the current list fragment
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, submissionsFragment)
                .addToBackStack(null) // Allows pressing the back button to return to the assignment list
                .commit();

        Toast.makeText(getContext(), "Loading Submissions for: " + assignmentTitle, Toast.LENGTH_SHORT).show();
    }
}