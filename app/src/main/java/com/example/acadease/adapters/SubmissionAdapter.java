package com.example.acadease.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
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
    private final int maxPoints;

    // Map to hold student UIDs and their names asynchronously
    private Map<String, String> studentNameCache = new HashMap<>();
    // Map to hold grades keyed by studentId
    private final Map<String, Integer> gradesMap = new HashMap<>();

    public SubmissionAdapter(Context context, List<Submission> submissionList, Date assignmentDueDate, int maxPoints, LookupRepository lookupRepository) {
        this.context = context;
        this.submissionList = submissionList;
        this.assignmentDueDate = assignmentDueDate;
        this.maxPoints = maxPoints;
        this.lookupRepository = lookupRepository;
    }

    // Optimization: allow fragment to preload names in bulk
    public void setPreloadedNameCache(Map<String, String> cache) {
        if (cache != null) {
            this.studentNameCache.putAll(cache);
        }
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

            if (assignmentDueDate != null && submittedDate.after(assignmentDueDate)) {
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
        holder.maxPointsText.setText(String.format("/ %d", maxPoints));
        Integer existing = submission.getGrade() > 0 ? submission.getGrade() : gradesMap.get(studentUid);
        holder.gradeInputEt.setText(existing != null ? String.valueOf(existing) : "");
        holder.gradeInputEt.setTag(submission.getId()); // Store the document ID for saving the grade

        // Remove previous watcher if any
        if (holder.textWatcher != null) {
            holder.gradeInputEt.removeTextChangedListener(holder.textWatcher);
        }
        // Add watcher to update grades map
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String text = s.toString().trim();
                if (text.isEmpty()) {
                    gradesMap.remove(studentUid);
                    return;
                }
                try {
                    int val = Integer.parseInt(text);
                    if (val < 0) val = 0;
                    if (val > maxPoints) val = maxPoints;
                    gradesMap.put(studentUid, val);
                } catch (NumberFormatException e) {
                    // ignore invalid
                }
            }
        };
        holder.gradeInputEt.addTextChangedListener(watcher);
        holder.textWatcher = watcher;

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
        return new HashMap<>(gradesMap);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView studentName, statusText, maxPointsText;
        public EditText gradeInputEt;
        public Button btnDownloadFile;
        public TextWatcher textWatcher;

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