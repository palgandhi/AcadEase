package com.example.acadease.data;

import android.util.Log;

import com.example.acadease.model.Announcement;
import com.example.acadease.model.Schedule;
import com.example.acadease.model.User;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class AdminRepository {
    private static final String TAG = "AdminRepository";
    private final FirebaseFirestore db;

    public AdminRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Interface to pass back results to the UI layer for all admin operations.
     */
    public interface RegistrationCallback {
        void onSuccess(String message);
        void onFailure(Exception e);
    }

    // =========================================================
    // USER MANAGEMENT FUNCTIONS
    // =========================================================

    /**
     * Creates a profile document in the 'users' collection using an external UID (pasted from Auth).
     * NOTE: Assumes the user identity (email/password) already exists in Firebase Authentication.
     */
    public void createProfileDocument(String uid, String email, String role, String firstName, String lastName, RegistrationCallback callback) {

        // 1. Validation (for client-side check)
        if (uid == null || uid.length() < 20) {
            callback.onFailure(new Exception("Invalid or incomplete UID. Must be pasted from Firebase Auth."));
            return;
        }

        // 2. Create the User profile object (POJO)
        User newUser = new User();
        newUser.setUid(uid); // The crucial synchronization point (Document ID)
        newUser.setEmail(email);
        newUser.setRole(role);
        newUser.setName(firstName);
        // Note: In a real system, you'd auto-generate a sequential ID instead of using UID.
        if (role.equals("student")) {
            newUser.setStudentId(uid);
        } else if (role.equals("faculty") || role.equals("admin")) {
            newUser.setFacultyId(uid);
        }

        // 3. Write to Firestore using the provided UID as the Document ID (Primary Key)
        db.collection("users").document(uid).set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Profile document created for UID: " + uid);
                    callback.onSuccess("Profile successfully created in Firestore for " + role + ".");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore write failed: " + e.getMessage());
                    callback.onFailure(new Exception("Firestore write failed: " + e.getMessage()));
                });
    }

    /**
     * Deletes a user profile document from Firestore.
     * CRITICAL WARNING: Requires manual deletion from Firebase Auth afterward.
     */
    public void deleteProfileDocument(String uid, RegistrationCallback callback) {

        db.collection("users").document(uid).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.w(TAG, "Profile document deleted for UID: " + uid);
                    // Return a clear alert message to the Admin UI
                    callback.onSuccess("SUCCESS: Profile deleted from Firestore. MANUAL STEP REQUIRED: Delete user identity from Firebase Auth console using UID: " + uid);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete profile: " + e.getMessage());
                    callback.onFailure(new Exception("Failed to delete profile: " + e.getMessage()));
                });
    }

    // =========================================================
    // ANNOUNCEMENTS FUNCTION
    // =========================================================

    /**
     * Creates a new Announcement document.
     */
    public void createAnnouncement(String title, String body, String imgUrl, List<String> targetRole, String category, String postedByUid, RegistrationCallback callback) {

        Announcement newAnnouncement = new Announcement();
        newAnnouncement.setTitle(title);
        newAnnouncement.setBody(body);
        newAnnouncement.setImgUrl(imgUrl);
        newAnnouncement.setTargetRole(targetRole);
        newAnnouncement.setCategory(category);
        newAnnouncement.setPostedBy(postedByUid);

        // Use a client-side timestamp (optional, but convenient for model)
        newAnnouncement.setCreatedAt(Timestamp.now());

        db.collection("Announcements").add(newAnnouncement)
                .addOnSuccessListener(documentReference -> {
                    Log.i(TAG, "Announcement created with ID: " + documentReference.getId());
                    callback.onSuccess("Announcement posted successfully!");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Announcement creation failed: " + e.getMessage());
                    callback.onFailure(new Exception("Failed to post announcement: " + e.getMessage()));
                });
    }

    // =========================================================
    // SCHEDULING FUNCTION
    // =========================================================

    /**
     * Creates a new recurring Schedule block document.
     */
    public void createSchedule(String courseCode, String facultyId, List<String> daysOfWeek, String startTime, String venue, String type, Timestamp startDate, Timestamp endDate, RegistrationCallback callback) {

        Schedule newSchedule = new Schedule();
        newSchedule.setCourseCode(courseCode);
        newSchedule.setFacultyId(facultyId);
        newSchedule.setDaysOfWeek(daysOfWeek);
        newSchedule.setStartTime(startTime);
        newSchedule.setVenue(venue);
        newSchedule.setType(type);
        newSchedule.setStartDate(startDate);
        newSchedule.setEndDate(endDate);

        db.collection("schedules").add(newSchedule)
                .addOnSuccessListener(documentReference -> {
                    Log.i(TAG, "Schedule created with ID: " + documentReference.getId());
                    callback.onSuccess("Schedule block successfully added.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Schedule creation failed: " + e.getMessage());
                    callback.onFailure(new Exception("Failed to create schedule: " + e.getMessage()));
                });
    }

    public void deleteAnnouncement(String announcementId, RegistrationCallback callback) {
        // The security rule should restrict this to Admin/Faculty.
        db.collection("Announcements").document(announcementId).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Announcement deleted: " + announcementId);
                    callback.onSuccess("Announcement deleted successfully.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete announcement: " + e.getMessage());
                    callback.onFailure(new Exception("Deletion failed. Check rules."));
                });
    }
}