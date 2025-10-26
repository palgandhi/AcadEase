package com.example.acadease.fragments;

import android.app.DatePickerDialog;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.example.acadease.R;
import com.example.acadease.adapters.AttendanceSessionAdapter;
import com.example.acadease.data.FacultyRepository;
import com.example.acadease.data.LookupRepository;
import com.example.acadease.model.Session;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class FacultyAttendanceFragment extends Fragment implements AttendanceSessionAdapter.SessionClickListener {

    private static final String TAG = "FACULTY_ATTENDANCE_LOG";

    // 1. UI elements
    private RecyclerView sessionRecyclerView;
    private EditText datePickerEt;
    private Button btnPrevDay, btnNextDay;
    private LinearLayout rosterContainer;
    private Button submitAttendanceButton;

    // 2. Repositories and State
    private FacultyRepository facultyRepository;
    private LookupRepository lookupRepository;
    private List<Session> facultySessions = new ArrayList<>();
    private List<String> currentRosterUids = new ArrayList<>();
    private Session selectedSession;
    private Calendar selectedDate;
    private String userUid;

    // 3. Formatters
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMM dd, yyyy (EEEE)", Locale.getDefault());

    public FacultyAttendanceFragment() { /* Required empty public constructor */ }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_faculty_attendance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialization
        facultyRepository = new FacultyRepository();
        lookupRepository = new LookupRepository();
        userUid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "DEFAULT_UID";

        // 1. Map UI Elements
        sessionRecyclerView = view.findViewById(R.id.attendance_session_recycler_view);
        datePickerEt = view.findViewById(R.id.attendance_date_picker_text);
        btnPrevDay = view.findViewById(R.id.btn_prev_day);
        btnNextDay = view.findViewById(R.id.btn_next_day);
        rosterContainer = view.findViewById(R.id.attendance_roster_container);
        submitAttendanceButton = view.findViewById(R.id.btn_submit_attendance);

        // Setup RecyclerView
        if (sessionRecyclerView != null) {
            sessionRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        }

        // 2. Setup Initial Date State (Today in IST)
        selectedDate = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));

        // 3. Set Listeners
        if (datePickerEt != null) datePickerEt.setOnClickListener(v -> showDatePicker());
        if (btnPrevDay != null) btnPrevDay.setOnClickListener(v -> navigateDay(-1));
        if (btnNextDay != null) btnNextDay.setOnClickListener(v -> navigateDay(1));
        if (submitAttendanceButton != null) submitAttendanceButton.setOnClickListener(v -> handleSubmitAttendance());

        // 4. Initial Load
        setRosterVisibility(false); // Start by showing the session selector list
        loadSessionsForSelectedDate();
    }

    // --- UI State Management ---

    private void setRosterVisibility(boolean isRosterVisible) {
        if (isRosterVisible) {
            // Hide session selector controls, show roster input
            if (sessionRecyclerView != null) sessionRecyclerView.setVisibility(View.GONE);
            if (rosterContainer != null) rosterContainer.setVisibility(View.VISIBLE);
            if (submitAttendanceButton != null) submitAttendanceButton.setVisibility(View.VISIBLE);
        } else {
            // Show session selector controls, hide roster input
            if (sessionRecyclerView != null) sessionRecyclerView.setVisibility(View.VISIBLE);
            if (rosterContainer != null) rosterContainer.setVisibility(View.GONE);
            if (submitAttendanceButton != null) submitAttendanceButton.setVisibility(View.GONE);

            if (rosterContainer != null) rosterContainer.removeAllViews();
        }
    }


    // --- Navigation and Date Helpers ---

    private void updateDateDisplay() {
        if (datePickerEt != null) {
            String dateStr = displayDateFormat.format(selectedDate.getTime());
            datePickerEt.setText(dateStr);
        }
    }

    private void navigateDay(int offset) {
        selectedDate.add(Calendar.DATE, offset);
        loadSessionsForSelectedDate();
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, y, m, d) -> {
                    selectedDate.set(y, m, d);
                    selectedDate.set(Calendar.HOUR_OF_DAY, 0);
                    selectedDate.set(Calendar.MINUTE, 0);
                    loadSessionsForSelectedDate();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    // --- Data Loading and Roster Orchestration ---

    private void loadSessionsForSelectedDate() {
        setRosterVisibility(false);
        updateDateDisplay();

        Calendar dayStart = (Calendar) selectedDate.clone();
        dayStart.set(Calendar.HOUR_OF_DAY, 0); dayStart.set(Calendar.MINUTE, 0); dayStart.set(Calendar.SECOND, 0);

        Calendar dayEnd = (Calendar) selectedDate.clone();
        dayEnd.set(Calendar.HOUR_OF_DAY, 23); dayEnd.set(Calendar.MINUTE, 59); dayEnd.set(Calendar.SECOND, 59);

        Timestamp start = new Timestamp(dayStart.getTime());
        Timestamp end = new Timestamp(dayEnd.getTime());

        // Fetch sessions for a single selected day
        facultyRepository.fetchScheduleSessions(userUid, "faculty", start, end, new FacultyRepository.ScheduleSessionsCallback() {
            @Override
            public void onSuccess(List<Session> sessions) {
                if (getContext() == null) return;

                facultySessions = sessions;

                if (sessions.isEmpty()) {
                    Toast.makeText(getContext(), "No classes scheduled on this date.", Toast.LENGTH_SHORT).show();
                    sessionRecyclerView.setAdapter(null);
                    return;
                }

                // Initialize the dedicated adapter for session selection
                AttendanceSessionAdapter adapter = new AttendanceSessionAdapter(requireContext(), sessions, FacultyAttendanceFragment.this);
                sessionRecyclerView.setAdapter(adapter);
            }

            @Override
            public void onFailure(Exception e) {
                if (getContext() == null) return;
                Log.e(TAG, "Failed to fetch schedule: " + e.getMessage(), e);
                Toast.makeText(getContext(), "Failed to fetch schedule: " + e.getMessage(), Toast.LENGTH_LONG).show();
                sessionRecyclerView.setAdapter(null);
            }
        });
    }

    private void loadRosterAndNames(String sessionId, String courseCode) {
        // 1. Fetch Roster UIDs for the course
        facultyRepository.fetchCourseRoster(courseCode, new FacultyRepository.RosterCallback() {
            @Override
            public void onSuccess(List<String> studentUids) {
                if (studentUids.isEmpty()) {
                    Toast.makeText(getContext(), "No students currently enrolled in this course.", Toast.LENGTH_LONG).show();
                    displayRosterInput(new HashMap<>());
                    return;
                }

                // 2. Bulk Lookup Student Names
                lookupRepository.fetchBulkStudentNames(studentUids, new LookupRepository.BulkNameCallback() {
                    @Override
                    public void onSuccess(Map<String, String> uidToNameMap) {
                        displayRosterInput(uidToNameMap);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(getContext(), "Warning: Failed to load student names.", Toast.LENGTH_LONG).show();

                        // Fallback: Display UIDs if name lookup fails
                        Map<String, String> fallbackMap = studentUids.stream()
                                .collect(Collectors.toMap(uid -> uid, uid -> "Failed Lookup (" + uid + ")"));
                        displayRosterInput(fallbackMap);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Failed to load course roster: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- UI Rendering Helpers ---

    private void displayRosterInput(Map<String, String> uidToNameMap) {
        if (rosterContainer == null || submitAttendanceButton == null) {
            Log.e(TAG, "Roster UI container is null. Cannot display roster.");
            return;
        }

        rosterContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (Map.Entry<String, String> entry : uidToNameMap.entrySet()) {
            String uid = entry.getKey();
            String displayName = entry.getValue();

            View row = inflater.inflate(R.layout.item_attendance_roster, rosterContainer, false);

            TextView studentName = row.findViewById(R.id.student_name_text);
            studentName.setText(displayName);

            RadioGroup statusGroup = row.findViewById(R.id.attendance_radio_group);
            statusGroup.setTag(uid);

            rosterContainer.addView(row);
        }

        // SWITCH VIEW STATE: Hide session list, show roster input
        setRosterVisibility(true);

        Toast.makeText(getContext(), "Roster loaded! Ready for attendance.", Toast.LENGTH_SHORT).show();
    }

    // --- Core Write Logic ---

    private void handleSubmitAttendance() {
        if (selectedSession == null) {
            Toast.makeText(getContext(), "System Error: Session not selected.", Toast.LENGTH_SHORT).show();
            return;
        }

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
        facultyRepository.recordAttendance(selectedSession.getId(), attendanceMap, new FacultyRepository.AttendanceWriteCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                // Return to the session selection view after successful submission
                setRosterVisibility(false);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Attendance submission failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Attendance Write Failed: " + e.getMessage());
            }
        });
    }

    // --- Core Roster Navigation (Implementation of the Adapter Interface) ---

    @Override
    public void onSessionClicked(String sessionId, String courseCode) {
        selectedSession = facultySessions.stream()
                .filter(s -> s.getId().equals(sessionId))
                .findFirst()
                .orElse(null);

        if (selectedSession != null) {
            loadRosterAndNames(sessionId, courseCode);
        }
    }
}