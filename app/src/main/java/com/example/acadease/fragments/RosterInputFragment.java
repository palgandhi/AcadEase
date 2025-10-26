package com.example.acadease.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.example.acadease.R;
import com.example.acadease.data.FacultyRepository;
import com.example.acadease.data.LookupRepository;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RosterInputFragment extends Fragment {

    private static final String TAG = "ROSTER_INPUT_LOGIC";

    // UI elements
    private TextView sessionDetailsTextView;
    private LinearLayout rosterContainer; // Container for dynamic student attendance inputs
    private Button submitAttendanceButton;

    // Repositories and State
    private FacultyRepository facultyRepository;
    private LookupRepository lookupRepository;
    private String sessionId;
    private String courseCode;
    private List<String> currentRosterUids = new ArrayList<>(); // UIDs of students in the selected course

    public RosterInputFragment() { /* Required empty public constructor */ }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // CRITICAL: Retrieve the session ID passed from the previous fragment
        if (getArguments() != null) {
            sessionId = getArguments().getString("SESSION_ID");
            courseCode = getArguments().getString("COURSE_CODE");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflates the dedicated roster input layout
        return inflater.inflate(R.layout.activity_roster_input_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        facultyRepository = new FacultyRepository();
        lookupRepository = new LookupRepository();

        // 1. Map UI Elements
        sessionDetailsTextView = view.findViewById(R.id.roster_session_details);
        rosterContainer = view.findViewById(R.id.attendance_roster_container);
        submitAttendanceButton = view.findViewById(R.id.btn_submit_attendance);

        if (sessionId == null) {
            sessionDetailsTextView.setText("Error: Session ID is missing.");
            return;
        }

        sessionDetailsTextView.setText(String.format("Course: %s | Session ID: %s", courseCode, sessionId));

        // 2. Load the Roster (UIDs -> Names)
        loadRosterAndNames();

        // 3. Set Listener
        if (submitAttendanceButton != null) {
            submitAttendanceButton.setOnClickListener(v -> handleSubmitAttendance());
        }
    }

    // --- Data Loading and Display ---

    private void loadRosterAndNames() {
        // Step A: Fetch Roster UIDs for the course
        facultyRepository.fetchCourseRoster(courseCode, new FacultyRepository.RosterCallback() {
            @Override
            public void onSuccess(List<String> studentUids) {
                currentRosterUids = studentUids;
                if (studentUids.isEmpty()) {
                    Toast.makeText(getContext(), "No students currently enrolled in this course.", Toast.LENGTH_LONG).show();
                    displayRosterInput(new HashMap<>());
                    return;
                }

                // Step B: Bulk Lookup Student Names
                lookupRepository.fetchBulkStudentNames(studentUids, new LookupRepository.BulkNameCallback() {
                    @Override
                    public void onSuccess(Map<String, String> uidToNameMap) {
                        Log.i(TAG, "Roster Names fetched successfully. Rendering UI.");
                        displayRosterInput(uidToNameMap);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Bulk Name Lookup FAILED: " + e.getMessage(), e);
                        // Fallback: Display UIDs if name lookup fails
                        Map<String, String> fallbackMap = studentUids.stream()
                                .collect(Collectors.toMap(uid -> uid, uid -> "Failed Lookup (" + uid + ")"));
                        displayRosterInput(fallbackMap);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Failed to load course roster UIDs: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Dynamically creates the list of student rows with Radio Buttons.
     */
    private void displayRosterInput(Map<String, String> uidToNameMap) {
        if (rosterContainer == null) {
            Log.e(TAG, "FATAL: Roster UI container is null. Cannot display roster.");
            return;
        }

        rosterContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (Map.Entry<String, String> entry : uidToNameMap.entrySet()) {
            String uid = entry.getKey();
            String displayName = entry.getValue();

            // Inflate item_attendance_roster layout for each student row
            View row = inflater.inflate(R.layout.item_attendance_roster, rosterContainer, false);

            TextView studentName = row.findViewById(R.id.student_name_text);
            studentName.setText(displayName);

            RadioGroup statusGroup = row.findViewById(R.id.attendance_radio_group);
            statusGroup.setTag(uid);

            rosterContainer.addView(row);
        }

        Toast.makeText(getContext(), "Roster ready for marking.", Toast.LENGTH_SHORT).show();
    }

    // --- Core Write Logic ---

    private void handleSubmitAttendance() {
        // 1. Gather Attendance Data (Map<Student UID, Status String>)
        Map<String, String> attendanceMap = new HashMap<>();

        for (int i = 0; i < rosterContainer.getChildCount(); i++) {
            View row = rosterContainer.getChildAt(i);
            RadioGroup statusGroup = row.findViewById(R.id.attendance_radio_group);

            String studentUid = (String) statusGroup.getTag();
            int selectedId = statusGroup.getCheckedRadioButtonId();

            if (selectedId != -1) {
                RadioButton selectedButton = row.findViewById(selectedId);
                String status = (String) selectedButton.getTag();
                attendanceMap.put(studentUid, status);
            } else {
                Toast.makeText(getContext(), "Error: Please mark attendance for all students.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Final Validation check
        if (attendanceMap.size() != currentRosterUids.size()) {
            Toast.makeText(getContext(), "Attendance must be marked for all students.", Toast.LENGTH_LONG).show();
            return;
        }

        // 2. Call Repository to perform Batched Write
        facultyRepository.recordAttendance(sessionId, attendanceMap, new FacultyRepository.AttendanceWriteCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                // CRITICAL: Return to the previous fragment (Session Selector) on success
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Attendance submission failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Attendance Write Failed: " + e.getMessage());
            }
        });
    }
}