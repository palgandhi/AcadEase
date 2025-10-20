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
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScheduleManagementFragment extends Fragment {

    private EditText courseCodeEt, facultyIdEt, venueEt, startTimeEt, startDateEt, endDateEt;
    private Button createScheduleBtn;
    private LinearLayout daySelectorContainer;
    private AutoCompleteTextView typeAutoCompleteTextView; // For type selection
    private AdminRepository adminRepository;

    private final String[] weekDays = {"MON", "TUE", "WED", "THU", "FRI"};
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

        // Map the Type dropdown (assuming ID: schedule_type_spinner)
        typeAutoCompleteTextView = view.findViewById(R.id.schedule_type_spinner);

        // 2. Setup Dynamic Day Checkboxes
        setupDayCheckboxes(daySelectorContainer);

        // 3. Setup Type Dropdown
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                scheduleTypes
        );
        typeAutoCompleteTextView.setAdapter(typeAdapter);
        typeAutoCompleteTextView.setText(scheduleTypes[0], false);

        // 4. Setup Date Pickers (Clicking the EditText shows the picker)
        startDateEt.setOnClickListener(v -> showDatePicker(true));
        endDateEt.setOnClickListener(v -> showDatePicker(false));

        // 5. Set Listener
        createScheduleBtn.setOnClickListener(v -> handleCreateSchedule());
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
        String type = typeAutoCompleteTextView.getText().toString(); // Read the selected type
        List<String> daysOfWeek = getSelectedDays();

        // 1. Validation
        if (courseCode.isEmpty() || facultyId.isEmpty() || venue.isEmpty() || startTime.isEmpty() || daysOfWeek.isEmpty() || selectedStartDate == null || selectedEndDate == null || type.isEmpty()) {
            Toast.makeText(getContext(), "Please complete all fields (including dates and days).", Toast.LENGTH_LONG).show();
            return;
        }

        // 2. Data Conversion (Dates to Timestamps)
        Timestamp startTimestamp = new Timestamp(selectedStartDate);
        Timestamp endTimestamp = new Timestamp(selectedEndDate);

        // 3. Call Repository
        adminRepository.createSchedule(courseCode, facultyId, daysOfWeek, startTime, venue, type, startTimestamp, endTimestamp, new AdminRepository.RegistrationCallback() {
            @Override
            public void onSuccess(String message) {
                if (getContext() == null) return;
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

                // Clear form fields after successful creation
                courseCodeEt.setText("");
                facultyIdEt.setText("");
                venueEt.setText("");
                startTimeEt.setText("");
                // Note: Clearing dates and resetting checkboxes often requires more complex logic
            }

            @Override
            public void onFailure(Exception e) {
                if (getContext() == null) return;
                Toast.makeText(getContext(), "Schedule creation failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}