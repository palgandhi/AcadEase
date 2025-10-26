package com.example.acadease.fragments;

import android.app.DatePickerDialog;
import android.content.Context;
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
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import com.example.acadease.R;
import com.example.acadease.adapters.ScheduleAdapter;
import com.example.acadease.data.FacultyRepository;
import com.example.acadease.model.Session;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.function.Function;

public class ScheduleFragment extends Fragment {

    // UI Elements
    private AutoCompleteTextView weekDropdown, yearDropdown;
    private TableLayout calendarGrid;
    private RecyclerView detailList;
    private TextView dailyHeaderDate, dailyHeaderDay;
    private TextView datePickerText;
    private Button btnPrevWeek, btnNextWeek;

    // Repositories
    private FacultyRepository facultyRepository;

    // Date State Management
    private String userRole = "faculty";
    private String userUid;
    private Calendar currentWeekStart; // Used for query start
    private Calendar currentWeekEnd;   // Used for query end
    private Calendar selectedDetailDay;

    // Formatters (IST-aware)
    private SimpleDateFormat weekRangeFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
    private SimpleDateFormat dayHeaderDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private SimpleDateFormat dayHeaderDayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
    private SimpleDateFormat timeFormatHourMinute = new SimpleDateFormat("h:mm a", Locale.getDefault());

    public ScheduleFragment() { /* Required empty public constructor */ }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_schedule_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialization
        facultyRepository = new FacultyRepository();
        userUid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "DEFAULT_UID";

        // 1. Map UI Elements
        calendarGrid = view.findViewById(R.id.schedule_calendar_grid);
        detailList = view.findViewById(R.id.schedule_detail_list);
        dailyHeaderDate = view.findViewById(R.id.schedule_daily_header_date);
        dailyHeaderDay = view.findViewById(R.id.schedule_daily_header_day);

        datePickerText = view.findViewById(R.id.schedule_date_picker_text);
        btnPrevWeek = view.findViewById(R.id.btn_prev_week);
        btnNextWeek = view.findViewById(R.id.btn_next_week);


        detailList.setLayoutManager(new LinearLayoutManager(requireContext()));

        // 2. Setup Initial Date Range (Current Week)
        setupInitialWeekRange();
        selectedDetailDay = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));

        // 3. Set Listeners
        datePickerText.setOnClickListener(v -> showDatePicker());
        btnPrevWeek.setOnClickListener(v -> navigateWeek(-1));
        btnNextWeek.setOnClickListener(v -> navigateWeek(1));

        // 4. Initial Load
        loadScheduleData();

        updateDetailHeader(selectedDetailDay);
    }

    // --- Date Management Logic ---

    private void setupInitialWeekRange() {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));

        currentWeekStart = (Calendar) now.clone();
        currentWeekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        currentWeekStart.set(Calendar.HOUR_OF_DAY, 0); currentWeekStart.set(Calendar.MINUTE, 0); currentWeekStart.set(Calendar.SECOND, 0);

        currentWeekEnd = (Calendar) currentWeekStart.clone();
        currentWeekEnd.add(Calendar.DATE, 6);
        currentWeekEnd.set(Calendar.HOUR_OF_DAY, 23); currentWeekEnd.set(Calendar.MINUTE, 59); currentWeekEnd.set(Calendar.SECOND, 59);

        updateWeekRangeDisplay();
    }

    private void navigateWeek(int offset) {
        currentWeekStart.add(Calendar.DATE, offset * 7);
        currentWeekEnd.add(Calendar.DATE, offset * 7);

        updateWeekRangeDisplay();
        loadScheduleData();
    }

    private void updateWeekRangeDisplay() {
        String startStr = weekRangeFormat.format(currentWeekStart.getTime());
        String endStr = weekRangeFormat.format(currentWeekEnd.getTime());
        String yearStr = new SimpleDateFormat("yyyy", Locale.getDefault()).format(currentWeekStart.getTime());

        datePickerText.setText(String.format("%s - %s, %s", startStr, endStr, yearStr));
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, y, m, d) -> {
                    Calendar selectedDay = Calendar.getInstance();
                    selectedDay.set(y, m, d);
                    selectedDay.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));

                    // Reset the current week range based on the selected day
                    currentWeekStart = (Calendar) selectedDay.clone();
                    currentWeekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    currentWeekStart.set(Calendar.HOUR_OF_DAY, 0); currentWeekStart.set(Calendar.MINUTE, 0); currentWeekStart.set(Calendar.SECOND, 0);

                    currentWeekEnd = (Calendar) currentWeekStart.clone();
                    currentWeekEnd.add(Calendar.DATE, 6);
                    currentWeekEnd.set(Calendar.HOUR_OF_DAY, 23); currentWeekEnd.set(Calendar.MINUTE, 59); currentWeekEnd.set(Calendar.SECOND, 59);

                    updateWeekRangeDisplay();
                    loadScheduleData();
                },
                currentWeekStart.get(Calendar.YEAR),
                currentWeekStart.get(Calendar.MONTH),
                currentWeekStart.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateDetailHeader(Calendar day) {
        if (dailyHeaderDate != null) {
            dailyHeaderDate.setText(dayHeaderDateFormat.format(day.getTime()));
        }
        if (dailyHeaderDay != null) {
            dailyHeaderDay.setText(dayHeaderDayFormat.format(day.getTime()));
        }
    }

    // --- Data Loading and Filtering ---

    private void loadScheduleData() {
        Timestamp start = new Timestamp(currentWeekStart.getTime());
        Timestamp end = new Timestamp(currentWeekEnd.getTime());

        Toast.makeText(requireContext(), "Fetching schedule for week...", Toast.LENGTH_SHORT).show();

        facultyRepository.fetchScheduleSessions(userUid, userRole, start, end, new FacultyRepository.ScheduleSessionsCallback() {
            @Override
            public void onSuccess(List<Session> sessions) {
                if (getContext() == null) return;

                if (sessions.isEmpty()) {
                    Toast.makeText(requireContext(), "No classes scheduled for this week.", Toast.LENGTH_LONG).show();
                    detailList.setAdapter(new ScheduleAdapter(requireContext(), new ArrayList<>(), facultyRepository));
                } else {
                    // 1. Draw the full Calendar Grid
                    drawCalendarGrid(sessions);

                    // 2. CRITICAL FIX: The detail list must always show the full week's sessions.
                    // The adapter handles the grouping/headers.
                    ScheduleAdapter adapter = new ScheduleAdapter(requireContext(), sessions, facultyRepository);
                    detailList.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (getContext() == null) return;
                Toast.makeText(requireContext(), "Failed to load schedule: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Helper function to create a TableHeader TextView (Moved outside drawCalendarGrid)
    private TextView createHeaderCell(LayoutInflater inflater, Context context, String text) {
        TextView tv = (TextView) inflater.inflate(R.layout.item_schedule_header_cell, null);
        tv.setText(text);
        tv.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        return tv;
    }

    /**
     * Dynamically draws the full weekly calendar grid (TableLayout).
     */
    private void drawCalendarGrid(List<Session> sessions) {
        if (calendarGrid == null) return;
        calendarGrid.removeAllViews();

        // Time slots extended from 8 AM to 7 PM
        List<String> timeSlots = List.of("8:00 AM", "9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM", "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM", "6:00 PM", "7:00 PM");
        String[] days = {"MON", "TUE", "WED", "THUR", "FRI", "SAT", "SUN"};

        final Context context = requireContext();
        final LayoutInflater inflater = LayoutInflater.from(context);

        // Map sessions grouped by DayOfWeek and TimeSlot string
        Map<Integer, Map<String, Session>> gridMap = sessions.stream()
                .collect(Collectors.groupingBy(session -> {
                    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
                    cal.setTime(session.getSessionTime().toDate());
                    return cal.get(Calendar.DAY_OF_WEEK);
                }, Collectors.toMap(session ->
                                timeFormatHourMinute.format(session.getSessionTime().toDate()),
                        session -> session,
                        (existing, replacement) -> existing
                )));

        // --- 1. Create Header Row (TIME, MON, TUE, ...) ---
        TableRow headerRow = new TableRow(context);
        headerRow.addView(createHeaderCell(inflater, context, "TIME"));
        for (String day : days) { headerRow.addView(createHeaderCell(inflater, context, day)); }
        calendarGrid.addView(headerRow);

        // --- 2. Create Content Rows (Populating the Grid) ---
        for (String timeSlot : timeSlots) {
            TableRow row = new TableRow(context);

            // Add Time Slot Label (Fixed Left Column)
            TextView timeLabel = (TextView) inflater.inflate(R.layout.item_schedule_time_cell, null);
            timeLabel.setText(timeSlot);
            row.addView(timeLabel);

            // Iterate through 7 days of the week (j=0 is MON, j=6 is SUN)
            for (int j = 0; j < 7; j++) {
                final TextView classCell = (TextView) inflater.inflate(R.layout.item_schedule_class_cell, null);
                int calendarDay = (j + Calendar.MONDAY - 1) % 7 + 1;

                // Calculate the specific date for this cell
                final Calendar cellDate = (Calendar) currentWeekStart.clone();
                cellDate.add(Calendar.DATE, j);
                cellDate.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));

                Map<String, Session> sessionsForDay = gridMap.getOrDefault(calendarDay, Map.of());
                Session matchingSession = sessionsForDay.get(timeSlot);

                if (matchingSession != null) {
                    final String courseCode = matchingSession.getCourseCode();
                    final String venue = matchingSession.getVenue();

                    // Set initial text to code while loading the title
                    classCell.setText(String.format("%s\n%s", courseCode, venue));
                    classCell.setBackgroundColor(context.getColor(R.color.primary_dark_grey));
                    classCell.setTextColor(context.getColor(R.color.text_light));

                    // Asynchronously fetch and display the full title (CRITICAL for readability)
                    facultyRepository.fetchCourseTitle(courseCode, new FacultyRepository.CourseTitleCallback() {
                        @Override
                        public void onSuccess(String title) {
                            // Update the cell with the full title and small venue text
                            classCell.setText(String.format("%s\n%s", title, venue));
                        }
                        @Override
                        public void onFailure(Exception e) {
                            // Keep the course code if lookup fails
                            classCell.setText(String.format("%s\n%s", courseCode, venue));
                        }
                    });

                    // Set the click listener to update the detail list below
                    classCell.setOnClickListener(v -> {
                        // CRITICAL: When a grid cell is clicked, we scroll the detail list to the corresponding day header.
                        // Since scrolling to a dynamic position is complex, we'll keep the toast for functionality demo.
                        Toast.makeText(context, "Clicked class on: " + dayHeaderDateFormat.format(cellDate.getTime()), Toast.LENGTH_SHORT).show();
                    });
                } else {
                    // Empty Slot
                    classCell.setText("----");
                    classCell.setBackgroundColor(context.getColor(android.R.color.white));
                }
                row.addView(classCell);
            }
            calendarGrid.addView(row);
        }
    }
}