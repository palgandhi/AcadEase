package com.example.acadease.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.acadease.R;
import com.example.acadease.data.AnnouncementRepository;
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
    private final AnnouncementRepository announcementRepository;
    private final boolean canDelete;

    public interface OnAnnouncementActionListener {
        void onDeleteClicked(String announcementId, int position);
    }

    public AnnouncementAdapter(Context context, List<Announcement> announcementList, OnAnnouncementActionListener listener, AnnouncementRepository announcementRepository, boolean canDelete) {
        this.context = context;
        this.announcementList = announcementList;
        this.listener = listener;
        this.announcementRepository = announcementRepository;
        this.canDelete = canDelete;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_announcement_card, parent, false);
        return new ViewHolder(view, listener, announcementList, announcementRepository, canDelete);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Announcement announcement = announcementList.get(position);

        // 1. Body and Title Binding
        holder.title.setText(announcement.getBody() != null ? announcement.getBody() : "NO BODY TEXT AVAILABLE");

        // 2. Poster Name Lookup (Asynchronous)
        String postedByUid = announcement.getPostedBy();
        if (postedByUid != null) {
            holder.poster.setText("Loading...");

            announcementRepository.fetchUserName(postedByUid, new AnnouncementRepository.NameCallback() {
                @Override
                public void onSuccess(String name) {
                    holder.poster.setText(name);
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

        // 4. Target roles display removed per request; keep category chip only

        // 5. Timestamp
        holder.timestamp.setText(formatTimestamp(announcement.getCreatedAt()));
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

    // Inside AnnouncementAdapter.java

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // TextViews
        public TextView title, category, timestamp, poster;
        // Icons
        public ImageView iconLike, iconShare;

        // Local state tracker for the like button (false = outlined, true = filled/red)
        private boolean isLiked = false;

        public ViewHolder(@NonNull View view, OnAnnouncementActionListener listener, List<Announcement> announcementList, AnnouncementRepository announcementRepository, boolean canDelete) {
            super(view);

            // --- MAPPING VIEWS ---
            title = view.findViewById(R.id.card_announcement_title);
            category = view.findViewById(R.id.card_announcement_category);
            poster = view.findViewById(R.id.card_announcement_poster);
            timestamp = view.findViewById(R.id.card_announcement_timestamp);

            // No separate target display anymore

            iconLike = view.findViewById(R.id.icon_like);
            iconShare = view.findViewById(R.id.icon_share);

            // --- ACTION LISTENERS ---

            // 1. LIKE Functionality (Client-Side State Toggle)
            iconLike.setOnClickListener(v -> {
                isLiked = !isLiked; // Toggle the state

                Context context = view.getContext();

                if (isLiked) {
                    // Change icon to filled heart and tint it red
                    iconLike.setImageResource(R.drawable.ic_favorite_filled);
                    iconLike.setColorFilter(context.getColor(R.color.design_default_color_error));
                    Toast.makeText(context, "Liked!", Toast.LENGTH_SHORT).show();
                } else {
                    // Change icon back to outlined heart and tint it dark/light
                    iconLike.setImageResource(R.drawable.ic_favorite_border);
                    iconLike.setColorFilter(context.getColor(R.color.text_light));
                    Toast.makeText(context, "Unliked.", Toast.LENGTH_SHORT).show();
                }
            });

            // 2. Share Functionality (Android Native Intent)
            iconShare.setOnClickListener(v -> {
                String shareText = "Check out this announcement from AcadEase: " + announcementList.get(getAdapterPosition()).getTitle();
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                view.getContext().startActivity(Intent.createChooser(shareIntent, "Share Announcement"));
            });

            // 3. DELETION LOGIC (Long Press) - enabled only when canDelete is true (Admin only)
            if (canDelete) {
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
            } else {
                view.setOnLongClickListener(null);
            }
        }
    }
}