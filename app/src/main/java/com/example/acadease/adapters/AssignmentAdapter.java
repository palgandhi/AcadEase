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
import com.example.acadease.model.Assignment;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AssignmentAdapter extends RecyclerView.Adapter<AssignmentAdapter.ViewHolder> {

    private final List<Assignment> assignmentList;
    private final Context context;
    private final SubmissionListener listener;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    // Interface to communicate click events back to the Fragment
    public interface SubmissionListener {
        void onViewSubmissionsClicked(String assignmentId, String assignmentTitle);
    }

    public AssignmentAdapter(Context context, List<Assignment> assignmentList, SubmissionListener listener) {
        this.context = context;
        this.assignmentList = assignmentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Assume item_assignment_card.xml is the list item layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_assignment_card, parent, false);
        return new ViewHolder(view, listener, assignmentList);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Assignment assignment = assignmentList.get(position);

        Date dueDate = assignment.getDueDate() != null ? assignment.getDueDate().toDate() : null;
        String dueDateStr = dueDate != null ? dateFormat.format(dueDate) : "N/A";

        // Check if the assignment is overdue
        boolean isOverdue = dueDate != null && dueDate.before(new Date());

        // 1. Bind Assignment Details
        holder.assignmentTitle.setText(assignment.getTitle());
        holder.assignmentPoints.setText(String.format("Max Points: %d", assignment.getMaxPoints()));

        // 2. Bind Due Date and Status
        holder.assignmentDueDate.setText(String.format("Due: %s", dueDateStr));

        if (isOverdue) {
            holder.assignmentStatus.setText("OVERDUE");
            holder.assignmentStatus.setTextColor(context.getColor(R.color.design_default_color_error));
        } else {
            holder.assignmentStatus.setText("Active");
            holder.assignmentStatus.setTextColor(context.getColor(R.color.primary_accent));
        }


        // 3. Set the click handler on the button
        holder.btnViewSubmissions.setOnClickListener(v -> {
            listener.onViewSubmissionsClicked(assignment.getId(), assignment.getTitle());
        });
    }

    @Override
    public int getItemCount() {
        return assignmentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView assignmentTitle, assignmentPoints, assignmentDueDate, assignmentStatus;
        public Button btnViewSubmissions;

        public ViewHolder(View view, SubmissionListener listener, List<Assignment> assignmentList) {
            super(view);
            // Assuming item_assignment_card.xml has these IDs:
            assignmentTitle = view.findViewById(R.id.assignment_card_title);
            assignmentPoints = view.findViewById(R.id.assignment_card_points);
            assignmentDueDate = view.findViewById(R.id.assignment_card_due_date);
            assignmentStatus = view.findViewById(R.id.assignment_card_status);
            btnViewSubmissions = view.findViewById(R.id.btn_view_submissions);
        }
    }
}