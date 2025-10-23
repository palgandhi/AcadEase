package com.example.acadease.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.acadease.R;
import com.example.acadease.data.AdminRepository;
import com.example.acadease.model.Schedule;
import com.example.acadease.utils.ScheduleUtility; // Utility to generate session dates
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class ScheduleManagementFragment extends Fragment {

    private EditText courseCodeEt, facultyIdEt, venueEt, startTimeEt, startDateEt, endDateEt;
    private Button createScheduleBtn;
    private LinearLayout daySelectorContainer;
    private AutoCompleteTextView typeAutoCompleteTextView;

    private AdminRepository adminRepository;

    private final String[] weekDays = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
    private final String[] scheduleTypes = {"lecture", "lab", "tutorial"};

    private Date selectedStartDate, selectedEndDate;

    public ScheduleManagementFragment() { /* Required empty public constructor */ }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adminRepository = new AdminRepository();

        // 1. Map UI Elements
        courseCodeEt = view.findViewById(R.id.schedule_course_code_edit_text);
        facultyIdEt = view.findViewById(R.id.schedule_faculty_id_edit_text);
        venueEt = view.findViewById(R.id.schedule_venue_edit_text);
        startTimeEt = view.findViewById(R.id.schedule_start_time_edit_text);
        startDateEt = view.findViewById(R.id.schedule_start_date_edit_text);
        endDateEt = view.findViewById(R.id.schedule_end_date_edit_text);
        createScheduleBtn = view.findViewById(R.id.btn_create_schedule);
        daySelectorContainer = view.findViewById(R.id.schedule_day_selector_container);
        typeAutoCompleteTextView = view.findViewById(R.id.schedule_type_spinner);

        // 2. Setup Dynamic Day Checkboxes
        setupDayCheckboxes(daySelectorContainer);

        // 3. Setup Type Dropdown
        setupTypeDropdown();

        // 4. Setup Date Pickers
        startDateEt.setOnClickListener(v -> showDatePicker(true));
        endDateEt.setOnClickListener(v -> showDatePicker(false));

        // 5. Set Listener
        createScheduleBtn.setOnClickListener(v -> handleCreateSchedule());
    }

    private void setupTypeDropdown() {
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                scheduleTypes
        );
        typeAutoCompleteTextView.setAdapter(typeAdapter);
        typeAutoCompleteTextView.setText(scheduleTypes[0], false);
    }

    private void setupDayCheckboxes(LinearLayout container) {
        for (String day : weekDays) {
            CheckBox cb = new CheckBox(getContext());
            cb.setText(day);
            cb.setTag(day);
            container.addView(cb);
        }
    }

    private void showDatePicker(boolean isStartDate) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, y, m, d) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(y, m, d);

                    // Set time fields to start/end of day for accurate Firestore Timestamp
                    if (isStartDate) {
                        selectedDate.set(Calendar.HOUR_OF_DAY, 0);
                        selectedDate.set(Calendar.MINUTE, 0);
                    } else {
                        selectedDate.set(Calendar.HOUR_OF_DAY, 23);
                        selectedDate.set(Calendar.MINUTE, 59);
                    }

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);

                    if (isStartDate) {
                        selectedStartDate = selectedDate.getTime();
                        startDateEt.setText(sdf.format(selectedStartDate));
                    } else {
                        selectedEndDate = selectedDate.getTime();
                        endDateEt.setText(sdf.format(selectedEndDate));
                    }
                }, year, month, day);
        datePickerDialog.show();
    }

    private List<String> getSelectedDays() {
        List<String> selectedDays = new ArrayList<>();
        for (int i = 0; i < daySelectorContainer.getChildCount(); i++) {
            View child = daySelectorContainer.getChildAt(i);
            if (child instanceof CheckBox) {
                CheckBox cb = (CheckBox) child;
                if (cb.isChecked()) {
                    selectedDays.add(cb.getTag().toString());
                }
            }
        }
        return selectedDays;
    }

    private void handleCreateSchedule() {
        String courseCode = courseCodeEt.getText().toString().trim();
        String facultyId = facultyIdEt.getText().toString().trim();
        String venue = venueEt.getText().toString().trim();
        String startTime = startTimeEt.getText().toString().trim();
        String type = typeAutoCompleteTextView.getText().toString();
        List<String> daysOfWeek = getSelectedDays();

        // 1. Validation and Integrity Check
        if (courseCode.isEmpty() || facultyId.isEmpty() || venue.isEmpty() || startTime.isEmpty() || daysOfWeek.isEmpty() || selectedStartDate == null || selectedEndDate == null) {
            Toast.makeText(getContext(), "Please complete all fields.", Toast.LENGTH_LONG).show();
            return;
        }
        if (selectedStartDate.after(selectedEndDate)) {
            Toast.makeText(getContext(), "Start date must be before end date.", Toast.LENGTH_LONG).show();
            return;
        }

        // 2. Data Conversion
        Timestamp startTimestamp = new Timestamp(selectedStartDate);
        Timestamp endTimestamp = new Timestamp(selectedEndDate);

        // 3. Create Schedule POJO (The Recurrence Blueprint)
        Schedule scheduleBlueprint = new Schedule();
        scheduleBlueprint.setCourseCode(courseCode);
        scheduleBlueprint.setFacultyId(facultyId);
        scheduleBlueprint.setDaysOfWeek(daysOfWeek);
        scheduleBlueprint.setStartTime(startTime);
        scheduleBlueprint.setVenue(venue);
        scheduleBlueprint.setType(type);
        scheduleBlueprint.setStartDate(startTimestamp);
        scheduleBlueprint.setEndDate(endTimestamp);

        // 4. Save the Recurrence Blueprint and Generate Sessions
        // We use a transaction or batch write for integrity (conceptually).

        // Step 4a: Save the Schedule Document first (The parent)
        FirebaseFirestore.getInstance().collection("schedules").add(scheduleBlueprint)
                .addOnSuccessListener(documentReference -> {
                    String scheduleId = documentReference.getId();

                    // Step 4b: Generate all individual sessions based on the blueprint
                    List<Map<String, Object>> sessions = ScheduleUtility.generateSessions(scheduleBlueprint, scheduleId);

                    // Step 4c: Bulk write the generated sessions to the 'sessions' collection
                    writeSessionsToFirestore(sessions, scheduleId);

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Schedule save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Executes the bulk write operation for all generated sessions.
     * In a real system, this should be a Batched Write for integrity.
     */
    private void writeSessionsToFirestore(List<Map<String, Object>> sessions, String scheduleId) {
        // We will loop and write each session document individually for simplicity,
        // but note that Batch Writes are preferred for true atomicity.

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (Map<String, Object> session : sessions) {
            db.collection("sessions").add(session);
            // Error handling and final success is often managed by a transaction wrapper,
            // which is too complex for this client-side demo.
        }

        Toast.makeText(getContext(),
                String.format("Schedule created and %d sessions generated!", sessions.size()),
                Toast.LENGTH_LONG).show();

        // Clear UI fields after completion (Good UX)
        courseCodeEt.setText("");
        facultyIdEt.setText("");
        venueEt.setText("");
        startTimeEt.setText("");
    }
}