package com.example.acadease.data;

import android.util.Log;

import com.example.acadease.model.Announcement;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AnnouncementRepository {
    private static final String TAG = "AnnounceRepository";
    private final FirebaseFirestore db;

    public AnnouncementRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Fetches announcements visible to the currently logged-in user's role.
     */
    // Inside AnnouncementRepository.java

    public void fetchAnnouncements(AnnouncementsCallback callback) {
        db.collection("Announcements")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .whereArrayContains("targetRole", "admin")
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    // --- ADD LOGGING HERE ---
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.e(TAG, "Query successful, but NO documents returned.");
                    } else {
                        Log.i(TAG, "Query successful. Documents returned: " + queryDocumentSnapshots.size());
                    }
                    // -------------------------

                    List<Announcement> announcements = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            // Attempt to map the document
                            Announcement announcement = document.toObject(Announcement.class);
                            announcement.setId(document.getId());
                            announcements.add(announcement);
                            Log.d(TAG, "Mapped announcement: " + announcement.getTitle());
                        } catch (Exception e) {
                            // CATCH MAPPING FAILURE! If this logs, the document structure is bad.
                            Log.e(TAG, "MAPPING FAILED for document ID " + document.getId() + ": " + e.getMessage());
                        }
                    }
                    callback.onSuccess(announcements);
                })
                .addOnFailureListener(e -> {
                    // This happens only if the security rules are blocking the read completely.
                    Log.e(TAG, "QUERY FAILED with exception (Security Rule Issue): ", e);
                    callback.onFailure(e);
                });
    }

    public interface AnnouncementsCallback {
        void onSuccess(List<Announcement> announcements);
        void onFailure(Exception e);
    }
}