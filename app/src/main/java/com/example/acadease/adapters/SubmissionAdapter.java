package com.example.acadease.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.acadease.R;
import com.example.acadease.data.LookupRepository;
import com.example.acadease.model.Submission;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class SubmissionAdapter extends RecyclerView.Adapter<SubmissionAdapter.ViewHolder> {

    private final List<Submission> submissionList;
    private final Context context;
    private final LookupRepository lookupRepository;
    private final Date assignmentDueDate; // Passed from the AssignmentListFragment

    // Map to hold student UIDs and their names asynchronously
    private Map<String, String> studentNameCache = new HashMap<>();

    public SubmissionAdapter(Context context, List<Submission> submissionList, Date assignmentDueDate, LookupRepository lookupRepository) {
        this.context = context;
        this.submissionList = submissionList;
        this.assignmentDueDate = assignmentDueDate;
        this.lookupRepository = lookupRepository;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_submission_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Submission submission = submissionList.get(position);

        // 1. Student Name Lookup
        String studentUid = submission.getStudentId();
        holder.studentName.setText("Loading...");

        // Check cache first
        if (studentNameCache.containsKey(studentUid)) {
            holder.studentName.setText(studentNameCache.get(studentUid));
        } else {
            // Fetch name asynchronously
            lookupRepository.fetchBulkStudentNames(List.of(studentUid), new LookupRepository.BulkNameCallback() {
                @Override
                public void onSuccess(Map<String, String> uidToNameMap) {
                    String name = uidToNameMap.getOrDefault(studentUid, "Student Profile Missing");
                    studentNameCache.put(studentUid, name);
                    holder.studentName.setText(name);
                }
                @Override
                public void onFailure(Exception e) {
                    holder.studentName.setText("Lookup Failed: " + studentUid.substring(0, 6) + "...");
                }
            });
        }

        // 2. Submission Status and Date Logic
        if (submission.getSubmittedAt() != null) {
            Date submittedDate = submission.getSubmittedAt().toDate();

            holder.btnDownloadFile.setVisibility(View.VISIBLE);

            if (submittedDate.after(assignmentDueDate)) {
                long diff = submittedDate.getTime() - assignmentDueDate.getTime();
                long daysLate = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
                holder.statusText.setText(String.format("Submitted (%d days late)", daysLate));
                holder.statusText.setTextColor(context.getColor(R.color.design_default_color_error));
            } else {
                holder.statusText.setText("Submitted On Time");
                holder.statusText.setTextColor(context.getColor(R.color.primary_accent)); // Use a success color
            }
        } else {
            // Not submitted (shouldn't happen if the submission document exists, but safety first)
            holder.statusText.setText("NOT SUBMITTED");
            holder.statusText.setTextColor(Color.GRAY);
            holder.btnDownloadFile.setVisibility(View.GONE);
        }

        // 3. Grading Input
        holder.maxPointsText.setText(String.format("/ %d", submission.getGrade()));
        holder.gradeInputEt.setText(String.valueOf(submission.getGrade() > 0 ? submission.getGrade() : ""));
        holder.gradeInputEt.setTag(submission.getId()); // Store the document ID for saving the grade

        // 4. Download Listener
        holder.btnDownloadFile.setOnClickListener(v -> {
            if (submission.getSubmissionUrl() != null) {
                // Open the URL in an external browser for download/view
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(submission.getSubmissionUrl()));
                context.startActivity(browserIntent);
            } else {
                Toast.makeText(context, "No file URL found for this submission.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return submissionList.size();
    }

    // Utility method to get all entered grades for Batched Update
    public Map<String, Integer> getAllGrades() {
        // This method is called by the SubmissionsFragment when the faculty clicks 'Save All Grades'
        // NOTE: A robust implementation would iterate through the RecyclerView views,
        // but for safety, the fragment will manage grade updates in a Map.
        return new HashMap<>();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView studentName, statusText, maxPointsText;
        public EditText gradeInputEt;
        public Button btnDownloadFile;

        public ViewHolder(@NonNull View view) {
            super(view);
            studentName = view.findViewById(R.id.submission_student_name);
            statusText = view.findViewById(R.id.submission_status_text);
            maxPointsText = view.findViewById(R.id.max_points_text);
            gradeInputEt = view.findViewById(R.id.grade_input_et);
            btnDownloadFile = view.findViewById(R.id.btn_download_file);
        }
    }
}