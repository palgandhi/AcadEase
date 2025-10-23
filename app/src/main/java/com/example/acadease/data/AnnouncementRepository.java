package com.example.acadease.data;

import android.util.Log;
import com.example.acadease.model.Announcement;
import com.example.acadease.model.Schedule;
import com.example.acadease.model.User;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class AnnouncementRepository {
    private static final String TAG = "AnnounceRepository";
    private final FirebaseFirestore db;
    private final CollectionReference announcementsRef;

    // NOTE: This array is required for the HomeFragment's load logic.
    private final String[] validRoles = {"admin", "faculty", "student", "all"};

    public AnnouncementRepository() {
        this.db = FirebaseFirestore.getInstance();
        // CRITICAL: Must match the database case exactly
        this.announcementsRef = db.collection("Announcements");
    }

    // --- CALLBACK INTERFACES ---
    public interface AnnouncementsCallback {
        void onSuccess(List<Announcement> announcements);

        void onFailure(Exception e);
    }

    public interface NameCallback {
        void onSuccess(String name);
    }

    /**
     * Fetches announcements visible to the currently logged-in user's role (Admin views all).
     */
    // Inside AnnouncementRepository.java

// NOTE: We keep the original fetchAnnouncements for backwards compatibility
// but make it call the new filtered method with the "All" default.

    /**
     * Public method to fetch announcements with filtering.
     * This is the method the HomeFragment must call when a button is clicked.
     */
    public void fetchAnnouncements(String filterCategory, AnnouncementsCallback callback) {

        // --- OPTIMIZATION: Filter by Last 7 Days ---
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7); // Set calendar to 7 days ago
        Timestamp sevenDaysAgo = new Timestamp(calendar.getTime());

        // Start with the base query: ordered by creation date, descending
        Query query = announcementsRef.orderBy("createdAt", Query.Direction.DESCENDING);

        // 1. Filter by Time (Applies to all loads)
        query = query.whereGreaterThan("createdAt", sevenDaysAgo);

        // 2. Filter by Category (Conditional, based on button click)
        if (filterCategory != null && !filterCategory.equals("All")) {
            // NOTE: Category names should be stored in lowercase in the database (e.g., "academic")
            query = query.whereEqualTo("category", filterCategory.toLowerCase());
        }

        // Execute the query
        query.limit(50) // Limits the result size
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Announcement> announcements = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Announcement announcement = document.toObject(Announcement.class);
                            announcement.setId(document.getId());
                            announcements.add(announcement);
                        } catch (Exception e) {
                            Log.e(TAG, "MAPPING FAILED for document ID " + document.getId(), e);
                        }
                    }
                    callback.onSuccess(announcements);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "QUERY FAILED with exception:", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Default method for initial load (calls the filtered method with "All").
     */
    public void fetchAnnouncements(AnnouncementsCallback callback) {
        fetchAnnouncements("All", callback);
    }

    /**
     * Fetches only the user's name (or a default) for a given UID.
     */
    public void fetchUserName(String uid, NameCallback callback) {

        // CRITICAL ROBUSTNESS CHECK: If the UID is null or obviously invalid, FAIL GRACEFULLY.
        if (uid == null || uid.isEmpty() || uid.length() < 20 || uid.toLowerCase().contains("user")) {
            Log.e(TAG, "Corrupted UID found in announcement document: " + uid);
            callback.onSuccess("Corrupted Poster Data");
            return;
        }

        // This line is syntactically correct, assuming 'uid' is a clean string.
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        callback.onSuccess(name != null && !name.isEmpty() ? name : "Admin/Faculty");
                    } else {
                        callback.onSuccess("Deleted User");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "User lookup failed for UID: " + uid, e);
                    callback.onSuccess("Lookup Failed");
                });
    }
}
