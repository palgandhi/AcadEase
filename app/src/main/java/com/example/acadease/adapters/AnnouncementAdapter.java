package com.example.acadease.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.acadease.R;
import com.example.acadease.data.AnnouncementRepository; // Needed for lookup
import com.example.acadease.model.Announcement;
import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {

    private final List<Announcement> announcementList;
    private final Context context;
    private final OnAnnouncementActionListener listener;
    private final AnnouncementRepository announcementRepository; // CRITICAL: Repository instance

    public interface OnAnnouncementActionListener {
        void onDeleteClicked(String announcementId, int position);
    }

    public AnnouncementAdapter(Context context, List<Announcement> announcementList, OnAnnouncementActionListener listener, AnnouncementRepository announcementRepository) {
        this.context = context;
        this.announcementList = announcementList;
        this.listener = listener;
        this.announcementRepository = announcementRepository; // Initialize repository
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

        // 1. Body and Title Binding (Using title for the main snippet)
        holder.title.setText(announcement.getBody() != null ? announcement.getBody() : "NO BODY TEXT AVAILABLE");

        // 2. Poster Name Lookup (CRITICAL ASYNCHRONOUS CALL)
        String postedByUid = announcement.getPostedBy();
        if (postedByUid != null) {
            holder.poster.setText("Loading..."); // Set placeholder immediately

            announcementRepository.fetchUserName(postedByUid, new AnnouncementRepository.NameCallback() {
                @Override
                public void onSuccess(String name) {
                    holder.poster.setText(name); // Set actual name when data returns
                }
            });
        } else {
            holder.poster.setText("System Post");
        }

        // 3. Category Null Check
        String categoryText = announcement.getCategory();
        if (categoryText != null && !categoryText.isEmpty()) {
            holder.category.setText(categoryText.toUpperCase());
        } else {
            holder.category.setText("GENERAL");
        }

        // 4. Target Roles Null Check
        List<String> roles = announcement.getTargetRole();
        if (roles != null && !roles.isEmpty()) {
            String targetRoles = "Target: " + String.join(", ", roles);
            holder.target.setText(targetRoles);
        } else {
            holder.target.setText("Target: All Authenticated Users");
        }

        // 5. Timestamp
        holder.timestamp.setText(formatTimestamp(announcement.getCreatedAt()));

        // 6. Set Item Click Listener (Optional)
        // holder.itemView.setOnClickListener(...)
    }

    @Override
    public int getItemCount() {
        return announcementList.size();
    }

    public void removeItem(int position) {
        announcementList.remove(position);
        notifyItemRemoved(position);
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "N/A";
        Date date = timestamp.toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.US);
        return sdf.format(date);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView title, category, target, timestamp, poster;

        public ViewHolder(@NonNull View view, OnAnnouncementActionListener listener, List<Announcement> announcementList) {
            super(view);

            // Map the views from item_announcement_card.xml
            title = view.findViewById(R.id.card_announcement_title);
            category = view.findViewById(R.id.card_announcement_category);
            poster = view.findViewById(R.id.card_announcement_poster); // Mapped the poster name field
            timestamp = view.findViewById(R.id.card_announcement_timestamp);

            // NOTE: Since the target TextView ID was ambiguous, we are using the category TextView
            // for the actual target list output in the Java logic for now.
            target = view.findViewById(R.id.card_announcement_category);

            // Delete Logic
            view.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    new AlertDialog.Builder(view.getContext())
                            .setTitle("Confirm Deletion")
                            .setMessage("Are you sure you want to delete this announcement: '" + announcementList.get(position).getTitle() + "'? \n\nTHIS ACTION IS IRREVERSIBLE.")
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