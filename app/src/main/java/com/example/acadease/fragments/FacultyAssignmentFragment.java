package com.example.acadease.fragments;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.util.Log;

import com.example.acadease.AssignmentCreationActivity; // New activity to be launched
import com.example.acadease.R;
import com.example.acadease.data.FacultyRepository;
import com.example.acadease.model.Course;
import com.example.acadease.adapters.CourseAdapter;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.ArrayList; // Added for empty list initialization

public class FacultyAssignmentFragment extends Fragment implements CourseAdapter.CourseActionListener {

    private static final String TAG = "FACULTY_ASSIGNMENT_LOG";

    private RecyclerView courseRecyclerView;
    private ExtendedFloatingActionButton fabCreateAssignment;

    private FacultyRepository facultyRepository;
    private CourseAdapter courseAdapter;

    public FacultyAssignmentFragment() { /* Required empty public constructor */ }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "Lifecycle: onCreateView started.");
        return inflater.inflate(R.layout.activity_faculty_assignment_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        facultyRepository = new FacultyRepository();

        // 1. Map UI components
        courseRecyclerView = view.findViewById(R.id.faculty_course_recycler_view);
        fabCreateAssignment = view.findViewById(R.id.fab_create_assignment);

        courseRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // 2. Load the list of courses taught by this faculty member
        loadCoursesTaught();

        // 3. Set FAB Listener to open the Assignment Creation Activity
        fabCreateAssignment.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AssignmentCreationActivity.class);
            startActivity(intent);
        });
    }

    private void loadCoursesTaught() {
        String facultyUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "DEFAULT_UID";

        Log.d(TAG, "Loading courses taught by UID: " + facultyUid);

        facultyRepository.fetchCoursesTaught(facultyUid, new FacultyRepository.CourseListCallback() {
            @Override
            public void onSuccess(List<Course> courses) {
                if (getContext() == null) return;

                Log.d(TAG, "Course Fetch SUCCESS. Courses returned: " + courses.size());

                if (courses.isEmpty()) {
                    Toast.makeText(getContext(), "You are not assigned to any active courses.", Toast.LENGTH_LONG).show();
                    // Initialize adapter with an empty list to prevent NPE
                    courseAdapter = new CourseAdapter(requireContext(), new ArrayList<>(), FacultyAssignmentFragment.this);
                    courseRecyclerView.setAdapter(courseAdapter);
                    return;
                }

                // Initialize CourseAdapter and pass 'this' as the listener
                courseAdapter = new CourseAdapter(requireContext(), courses, FacultyAssignmentFragment.this);
                courseRecyclerView.setAdapter(courseAdapter);
            }

            @Override
            public void onFailure(Exception e) {
                if (getContext() == null) return;
                Log.e(TAG, "Course Fetch FAILURE: " + e.getMessage(), e);
                Toast.makeText(getContext(), "Failed to load courses: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh the list whenever the fragment becomes visible (e.g., returning from creation activity)
        loadCoursesTaught();
    }

    // --- IMPLEMENTATION OF COURSE ADAPTER LISTENER ---

    @Override
    public void onViewAssignmentsClicked(String courseCode, String courseTitle) {
        // This method starts the next screen: viewing the list of assignments for the selected course.

        Log.i(TAG, "Executing fragment transaction for Assignment List: " + courseCode);

        // 1. Prepare arguments
        Bundle args = new Bundle();
        args.putString("COURSE_CODE", courseCode);
        args.putString("COURSE_TITLE", courseTitle);

        // 2. Create the target fragment
        AssignmentListFragment assignmentListFragment = new AssignmentListFragment();
        assignmentListFragment.setArguments(args);

        // 3. Execute Fragment Transaction: Replace the current Assignment Management Fragment
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, assignmentListFragment)
                .addToBackStack(null) // Allows pressing the back button to return to the course list
                .commit();
    }
}