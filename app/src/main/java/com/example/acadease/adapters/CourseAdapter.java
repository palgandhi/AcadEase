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
import com.example.acadease.model.Course;

import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.ViewHolder> {

    private final List<Course> courseList;
    private final Context context;
    private final CourseActionListener listener;

    // Interface to communicate click events back to the Fragment
    public interface CourseActionListener {
        void onViewAssignmentsClicked(String courseCode, String courseTitle);
    }

    public CourseAdapter(Context context, List<Course> courseList, CourseActionListener listener) {
        this.context = context;
        this.courseList = courseList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course_assignment_card, parent, false);
        return new ViewHolder(view, listener, courseList);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Course course = courseList.get(position);

        String fullTitle = String.format("%s - %s", course.getCourseCode(), course.getTitle());
        String details = String.format("Code: %s | Credits: %d", course.getCourseCode(), course.getCredits());

        holder.courseTitle.setText(fullTitle);
        holder.courseDetails.setText(details);

        // Set the click handler on the button
        holder.btnViewAssignments.setOnClickListener(v -> {
            listener.onViewAssignmentsClicked(course.getCourseCode(), fullTitle);
        });
    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView courseTitle, courseDetails;
        public Button btnViewAssignments;

        public ViewHolder(View view, CourseActionListener listener, List<Course> courseList) {
            super(view);
            courseTitle = view.findViewById(R.id.course_card_title);
            courseDetails = view.findViewById(R.id.course_card_details);
            btnViewAssignments = view.findViewById(R.id.btn_view_assignments);

            // Note: The click listener is attached in onBindViewHolder with the current item data
        }
    }
}