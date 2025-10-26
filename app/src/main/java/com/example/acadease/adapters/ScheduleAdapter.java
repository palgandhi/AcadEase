package com.example.acadease.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.acadease.R;
import com.example.acadease.data.FacultyRepository;
import com.example.acadease.model.ScheduleItem;
import com.example.acadease.model.Session;
import com.example.acadease.model.User;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ScheduleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ScheduleItem> scheduledItems;
    private final Context context;
    private final FacultyRepository facultyRepository;

    private SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
    private SimpleDateFormat dayHeaderFormat = new SimpleDateFormat("MMM dd, yyyy EEEE", Locale.getDefault());


    public ScheduleAdapter(Context context, List<Session> allSessions, FacultyRepository facultyRepository) {
        this.context = context;
        this.facultyRepository = facultyRepository;
        // Group sessions immediately upon creation of the adapter
        this.scheduledItems = groupSessionsByDay(allSessions);
    }

    /**
     * Groups sessions by day, sorts them, and inserts header items (the black blocks).
     */
    private List<ScheduleItem> groupSessionsByDay(List<Session> sessions) {
        if (sessions == null || sessions.isEmpty()) return new ArrayList<>();

        // 1. Sort all sessions by time (Timestamp)
        sessions.sort(Comparator.comparing(s -> s.getSessionTime().toDate()));

        List<ScheduleItem> items = new ArrayList<>();
        String currentDay = "";

        for (Session session : sessions) {
            Date sessionDate = session.getSessionTime().toDate();

            // Format the date string to check for day change
            String sessionDayString = dayHeaderFormat.format(sessionDate);

            if (!sessionDayString.equals(currentDay)) {
                // If the day changed, add a header item (the black block)
                items.add(new ScheduleItem(sessionDayString));
                currentDay = sessionDayString;
            }

            // Add the session detail item
            items.add(new ScheduleItem(session));
        }
        return items;
    }

    @Override
    public int getItemViewType(int position) {
        return scheduledItems.get(position).type;
    }

    @Override
    public int getItemCount() {
        return scheduledItems.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == ScheduleItem.TYPE_HEADER) {
            // Use the custom header layout for the date block
            View view = inflater.inflate(R.layout.item_schedule_daily_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            // Use the standard detail layout for the session data
            View view = inflater.inflate(R.layout.item_schedule_detail, parent, false);
            return new SessionViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ScheduleItem item = scheduledItems.get(position);

        if (item.type == ScheduleItem.TYPE_HEADER) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;

            // Extract date and day from the full header text
            String fullHeader = item.headerDate;
            String datePart = fullHeader.substring(0, fullHeader.lastIndexOf(" "));
            String dayPart = fullHeader.substring(fullHeader.lastIndexOf(" ") + 1);

            headerHolder.date.setText(datePart);
            headerHolder.day.setText(dayPart);

        } else {
            SessionViewHolder sessionHolder = (SessionViewHolder) holder;
            Session session = item.session;

            // 1. Time Calculation and Display
            Date startDate = session.getSessionTime().toDate();
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
            calendar.setTime(startDate);
            calendar.add(Calendar.HOUR_OF_DAY, 1);

            String startTimeStr = timeFormat.format(startDate);
            String endTimeStr = timeFormat.format(calendar.getTime());

            sessionHolder.timeRange.setText(String.format("%s - %s", startTimeStr, endTimeStr));

            // Set initial placeholder for title and prof
            sessionHolder.courseTitle.setText(session.getCourseCode());
            sessionHolder.professorVenue.setText("Loading Professor...");

            // 2. Asynchronous Course Title Lookup
            final String courseCode = session.getCourseCode();
            facultyRepository.fetchCourseTitle(courseCode, new FacultyRepository.CourseTitleCallback() {
                @Override
                public void onSuccess(String title) {
                    sessionHolder.courseTitle.setText(title);
                }

                @Override
                public void onFailure(Exception e) {
                    sessionHolder.courseTitle.setText(courseCode + " (Lookup Error)");
                }
            });

            // 3. Asynchronous Faculty Name Lookup
            String facultyUid = session.getFacultyId();
            final String venue = session.getVenue();

            facultyRepository.fetchUserProfile(facultyUid, new FacultyRepository.UserProfileCallback() {
                @Override
                public void onSuccess(User user) {
                    String profName = user.getName();
                    sessionHolder.professorVenue.setText(String.format("Prof: %s | Class: %s",
                            profName,
                            venue));
                }

                @Override
                public void onFailure(Exception e) {
                    sessionHolder.professorVenue.setText(String.format("Prof: Lookup Failed | Class: %s", venue));
                }
            });

            // 4. Click Listener with Detailed Dialog
            sessionHolder.itemView.setOnClickListener(v -> {
                boolean isPast = session.getSessionTime().toDate().before(new Date());

                String status = isPast ? "STATUS: PAST CLASS" : "STATUS: UPCOMING CLASS";
                String currentProf = sessionHolder.professorVenue.getText().toString().split("\\|")[0].trim(); // Get current displayed prof string

                String message = String.format(
                        "Course: %s\nTime: %s - %s\nVenue: %s\nTopic: %s",
                        sessionHolder.courseTitle.getText(),
                        startTimeStr,
                        endTimeStr,
                        session.getVenue(),
                        currentProf,
                        session.getTopic() != null ? session.getTopic() : "N/A"
                );

                new AlertDialog.Builder(context)
                        .setTitle(sessionHolder.courseTitle.getText().toString())
                        .setMessage(message)
                        .setPositiveButton(isPast ? "RECORD ATTENDANCE" : "CLOSE", (dialog, which) -> {
                            if (isPast) {
                                // TODO: Navigate to the Attendance Roster input screen (next feature)
                                Toast.makeText(context, "Navigating to Roster for: " + session.getCourseCode(), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .show();
            });
        }
    }

    public static class SessionViewHolder extends RecyclerView.ViewHolder {
        public TextView timeRange, courseTitle, professorVenue;

        public SessionViewHolder(@NonNull View view) {
            super(view);
            timeRange = view.findViewById(R.id.schedule_time_range);
            courseTitle = view.findViewById(R.id.schedule_course_title);
            professorVenue = view.findViewById(R.id.schedule_professor_venue);
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public TextView date, day;

        public HeaderViewHolder(@NonNull View view) {
            super(view);
            // Mapped from item_schedule_daily_header.xml
            date = view.findViewById(R.id.schedule_daily_header_date);
            day = view.findViewById(R.id.schedule_daily_header_day);
        }
    }
}