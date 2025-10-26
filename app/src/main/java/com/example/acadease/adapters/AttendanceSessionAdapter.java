package com.example.acadease.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.acadease.R;
import com.example.acadease.data.FacultyRepository;
import com.example.acadease.model.Session;
import com.example.acadease.model.User;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class AttendanceSessionAdapter extends RecyclerView.Adapter<AttendanceSessionAdapter.ViewHolder> {

    private final List<Session> sessionList;
    private final Context context;
    private final SessionClickListener listener;
    private final FacultyRepository facultyRepository;

    private SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    // Interface defined in the Fragment (FacultyAttendanceFragment)
    public interface SessionClickListener {
        void onSessionClicked(String sessionId, String courseCode);
    }

    public AttendanceSessionAdapter(Context context, List<Session> sessionList, SessionClickListener listener) {
        this.context = context;
        this.sessionList = sessionList;
        this.listener = listener;
        // CRITICAL: Initialize the repository instance
        this.facultyRepository = new FacultyRepository();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Reuse the item_schedule_detail layout for the card style
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule_detail, parent, false);
        return new ViewHolder(view, listener, sessionList);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Session session = sessionList.get(position);

        Date sessionDate = session.getSessionTime().toDate();
        Date currentTime = new Date();

        // 1. Time Display
        String timeStr = timeFormat.format(sessionDate);

        // Final End Time (Assumes 1-hour class)
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        calendar.setTime(sessionDate);
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        String endTimeStr = timeFormat.format(calendar.getTime());

        holder.timeRange.setText(String.format("%s - %s", timeStr, endTimeStr));

        // Set initial placeholders while async lookups happen
        holder.courseTitle.setText("Loading Course...");
        holder.professorVenue.setText("Loading Professor...");

        // 2. Asynchronous Course Title Lookup
        final String courseCode = session.getCourseCode();
        facultyRepository.fetchCourseTitle(courseCode, new FacultyRepository.CourseTitleCallback() {
            @Override
            public void onSuccess(String title) {
                holder.courseTitle.setText(title);
            }
            @Override public void onFailure(Exception e) { holder.courseTitle.setText(courseCode + " (Error)"); }
        });

        // 3. Asynchronous Faculty Name Lookup
        String facultyUid = session.getFacultyId();
        facultyRepository.fetchUserProfile(facultyUid, new FacultyRepository.UserProfileCallback() {
            @Override
            public void onSuccess(User user) {
                String profName = user.getName() != null ? user.getName() : "Faculty";

                holder.professorVenue.setText(String.format("Prof: %s | Venue: %s",
                        profName,
                        session.getVenue()));
            }
            @Override
            public void onFailure(Exception e) {
                holder.professorVenue.setText("Prof: Lookup Failed | Venue: " + session.getVenue());
            }
        });

        // 4. Status Check (Determine if we show LOG or MODIFY button)
        boolean isPast = sessionDate.before(currentTime);

        if (isPast) {
            holder.actionButton.setText("LOG ATTENDANCE");
            holder.actionButton.setVisibility(View.VISIBLE);
        } else {
            holder.actionButton.setText("UPCOMING");
            holder.actionButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return sessionList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView timeRange, courseTitle, professorVenue;
        public Button actionButton; // The Log/Modify Button

        public ViewHolder(View view, SessionClickListener listener, List<Session> sessionList) {
            super(view);
            // Mapped elements from item_schedule_detail.xml (repurposed)
            timeRange = view.findViewById(R.id.schedule_time_range);
            courseTitle = view.findViewById(R.id.schedule_course_title);
            professorVenue = view.findViewById(R.id.schedule_professor_venue);
            actionButton = view.findViewById(R.id.action_button); // The button added to the card

            // Set the listener on the ACTION BUTTON
            if (actionButton != null) {
                actionButton.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        Session session = sessionList.get(position);
                        listener.onSessionClicked(session.getId(), session.getCourseCode()); // Triggers Fragment Navigation
                    }
                });
            }
        }
    }
}