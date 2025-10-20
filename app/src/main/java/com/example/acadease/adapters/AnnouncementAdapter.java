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
import com.example.acadease.model.Announcement;
import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {

    private final List<Announcement> announcementList;
    private final Context context;
    private final OnAnnouncementActionListener listener; // CRITICAL: Listener for deletion

    // Interface to communicate back to the Fragment
    public interface OnAnnouncementActionListener {
        void onDeleteClicked(String announcementId, int position);
    }

    public AnnouncementAdapter(Context context, List<Announcement> announcementList, OnAnnouncementActionListener listener) {
        this.context = context;
        this.announcementList = announcementList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_announcement_card, parent, false);
        return new ViewHolder(view, listener, announcementList);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Announcement announcement = announcementList.get(position);

        holder.title.setText(announcement.getTitle() != null ? announcement.getTitle() : "NO TITLE");
        holder.body.setText(announcement.getBody() != null ? announcement.getBody() : "No content available.");

        // Category Null Check
        String categoryText = announcement.getCategory();
        if (categoryText != null && !categoryText.isEmpty()) {
            holder.category.setText(categoryText.toUpperCase());
        } else {
            holder.category.setText("GENERAL");
        }

        // Target Roles Null Check
        List<String> roles = announcement.getTargetRole();
        if (roles != null && !roles.isEmpty()) {
            String targetRoles = "Target: " + String.join(", ", roles);
            holder.target.setText(targetRoles);
        } else {
            holder.target.setText("Target: All Authenticated Users");
        }

        holder.timestamp.setText(formatTimestamp(announcement.getCreatedAt()));
    }

    @Override
    public int getItemCount() {
        return announcementList.size();
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "N/A";
        Date date = timestamp.toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.US);
        return sdf.format(date);
    }

    // Utility method called by the Fragment on successful repository delete
    public void removeItem(int position) {
        announcementList.remove(position);
        notifyItemRemoved(position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView title, body, category, target, timestamp;

        public ViewHolder(View view, OnAnnouncementActionListener listener, List<Announcement> announcementList) {
            super(view);
            title = view.findViewById(R.id.card_announcement_title);
            body = view.findViewById(R.id.card_announcement_body);
            category = view.findViewById(R.id.card_announcement_category);
            target = view.findViewById(R.id.card_announcement_target);
            timestamp = view.findViewById(R.id.card_announcement_timestamp);

            // Set Long Click Listener for Deletion
            view.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    // Confirmation dialog is best practice for destructive actions
                    new AlertDialog.Builder(view.getContext())
                            .setTitle("Confirm Deletion")
                            .setMessage("Are you sure you want to delete this announcement: '" + announcementList.get(position).getTitle() + "'?")
                            .setPositiveButton("DELETE", (dialog, which) -> {
                                String docId = announcementList.get(position).getId();
                                listener.onDeleteClicked(docId, position);
                            })
                            .setNegativeButton("CANCEL", null)
                            .show();
                    return true;
                }
                return false;
            });
        }
    }
}