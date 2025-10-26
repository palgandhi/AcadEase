package com.example.acadease.data;

import android.util.Log;

import com.example.acadease.model.Announcement;
import com.example.acadease.model.Schedule;
import com.example.acadease.model.User;
import com.example.acadease.utils.ScheduleUtility;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AdminRepository {
    private static final String TAG = "AdminRepository";
    private final FirebaseFirestore db;

    // Collection Name Constants (CRITICAL: Match database case exactly)
    private final String USERS_COLLECTION = "users";
    private final String ENROLLMENTS_COLLECTION = "Enrollments";
    private final String PROGRAMS_COLLECTION = "programs"; // Used for program lookup
    private final String ANNOUNCEMENTS_COLLECTION = "Announcements";
    private final String SCHEDULES_COLLECTION = "schedules";
    private final String SESSIONS_COLLECTION = "sessions";

    public AdminRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // =========================================================
    // CALLBACK INTERFACE
    // =========================================================

    public interface RegistrationCallback {
        void onSuccess(String message);
        void onFailure(Exception e);
    }

    // =========================================================
    // 1. USER REGISTRATION & ENROLLMENT (Program-Based Batched Write)
    // =========================================================

    /**
     * Creates a profile document in 'users' and then fetches course codes from the
     * specified program to create enrollment records in a batched write.
     */
    public void createProfileAndEnroll(String uid, String email, String role, String firstName, String lastName, String mobile, String customId, String imageUrl, String programId, RegistrationCallback callback) {

        // 1. ASYNCHRONOUS STEP: Fetch the required course codes from the Program blueprint.
        if (!role.equals("student")) {
            // Non-students bypass enrollment lookup. Proceed directly to profile write.
            executeBatchWrite(uid, email, role, firstName, lastName, mobile, customId, imageUrl, null, callback);
            return;
        }

        // Ensure student has selected a program before lookup
        if (programId == null || programId.isEmpty()) {
            callback.onFailure(new Exception("Student registration requires a program ID."));
            return;
        }

        db.collection(PROGRAMS_COLLECTION).document(programId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // CRITICAL: Cast the array field to List<String>
                        List<String> courseCodes = (List<String>) documentSnapshot.get("defaultCourseCodes");

                        if (courseCodes == null || courseCodes.isEmpty()) {
                            callback.onFailure(new Exception("Program " + programId + " has no course codes defined. Cannot enroll."));
                            return;
                        }

                        // Step 2: Execute the profile and enrollment write using the retrieved list.
                        executeBatchWrite(uid, email, role, firstName, lastName, mobile, customId, imageUrl, courseCodes, callback);
                    } else {
                        callback.onFailure(new Exception("Program ID not found: Cannot enroll student."));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to retrieve program data: ", e);
                    callback.onFailure(new Exception("Failed to retrieve program data: " + e.getMessage()));
                });
    }

    public void createSchedule(Schedule scheduleBlueprint, List<Map<String, Object>> sessions, RegistrationCallback callback) {

        // Step A: Save the Schedule Document first (The parent blueprint)
        db.collection(SCHEDULES_COLLECTION).add(scheduleBlueprint)
                .addOnSuccessListener(documentReference -> {
                    String scheduleId = documentReference.getId();

                    // Step B: Bulk write the generated sessions
                    // CRITICAL: Call the Utility to generate the sessions using the corrected logic
                    List<Map<String, Object>> generatedSessions = ScheduleUtility.generateSessions(scheduleBlueprint, scheduleId);

                    writeSessionsToFirestore(generatedSessions, scheduleId, callback);
                })
                .addOnFailureListener(e -> callback.onFailure(new Exception("Schedule blueprint save failed: " + e.getMessage())));
    }

    /**
     * Executes the combined Batched Write for User Profile and Enrollments.
     */
    private void executeBatchWrite(String uid, String email, String role, String firstName, String lastName, String mobile, String customId, String imageUrl, List<String> courseCodes, RegistrationCallback callback) {

        User newUser = new User();
        // Set all user fields...
        newUser.setUid(uid); newUser.setEmail(email); newUser.setRole(role);
        newUser.setName(firstName);
        newUser.setProfileImageUrl(imageUrl); newUser.setCreatedAt(Timestamp.now());

        // Set Contact Info
        Map<String, String> contactInfo = new HashMap<>();
        contactInfo.put("mobile", mobile); newUser.setContactInfo(contactInfo);

        // Set Custom ID
        if (role.equals("student")) { newUser.setStudentId(customId); }
        else if (role.equals("faculty") || role.equals("admin")) { newUser.setFacultyId(customId); }

        WriteBatch batch = db.batch();
        DocumentReference userRef = db.collection(USERS_COLLECTION).document(uid);
        batch.set(userRef, newUser);

        // Step B: Create Enrollment Documents (Only if courseCodes is provided and not empty)
        if (courseCodes != null && !courseCodes.isEmpty()) {
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            for (String courseCode : courseCodes) {
                DocumentReference enrollmentRef = db.collection(ENROLLMENTS_COLLECTION).document();
                Map<String, Object> enrollmentData = new HashMap<>();
                enrollmentData.put("studentId", uid);
                enrollmentData.put("courseCode", courseCode);
                enrollmentData.put("academicYear", currentYear);
                enrollmentData.put("status", "active");

                batch.set(enrollmentRef, enrollmentData);
            }
        }

        // 3. Commit the Batch
        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess("Profile and Enrollments successfully created for " + firstName + "."))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Batch commit failed: ", e);
                    callback.onFailure(new Exception("Batch commit failed: " + e.getMessage()));
                });
    }

    // =========================================================
    // 2. ANNOUNCEMENTS CRUD (Simplified to Auth Check)
    // =========================================================

    public void createAnnouncement(String title, String body, String imgUrl, List<String> targetRole, String category, String postedByUid, RegistrationCallback callback) {
        Announcement newAnnouncement = new Announcement();
        newAnnouncement.setTitle(title);
        newAnnouncement.setBody(body);
        newAnnouncement.setImgUrl(imgUrl);
        newAnnouncement.setTargetRole(targetRole);
        newAnnouncement.setCategory(category);
        newAnnouncement.setPostedBy(postedByUid);
        newAnnouncement.setCreatedAt(Timestamp.now());

        db.collection(ANNOUNCEMENTS_COLLECTION).add(newAnnouncement)
                .addOnSuccessListener(documentReference -> callback.onSuccess("Announcement posted successfully!"))
                .addOnFailureListener(e -> callback.onFailure(new Exception("Failed to post announcement: " + e.getMessage())));
    }

    public void deleteAnnouncement(String announcementId, RegistrationCallback callback) {
        db.collection(ANNOUNCEMENTS_COLLECTION).document(announcementId).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess("Announcement deleted successfully."))
                .addOnFailureListener(e -> callback.onFailure(new Exception("Deletion failed. Check rules.")));
    }


    // =========================================================
    // 3. SCHEDULING (Creation and Session Bulk Write)
    // =========================================================


    private void writeSessionsToFirestore(List<Map<String, Object>> sessions, String scheduleId, RegistrationCallback callback) {
        WriteBatch batch = db.batch();

        for (Map<String, Object> session : sessions) {
            DocumentReference sessionRef = db.collection(SESSIONS_COLLECTION).document();
            batch.set(sessionRef, session);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess(String.format("Schedule created and %d sessions generated!", sessions.size())))
                .addOnFailureListener(e -> callback.onFailure(new Exception("Session bulk write failed. Integrity compromised: " + e.getMessage())));
    }

    // =========================================================
    // 4. GENERAL USER DELETION
    // =========================================================

    public void deleteProfileDocument(String uid, RegistrationCallback callback) {
        db.collection(USERS_COLLECTION).document(uid).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess("SUCCESS: Profile deleted from Firestore. MANUAL STEP REQUIRED: Delete user identity from Firebase Auth console using UID: " + uid))
                .addOnFailureListener(e -> callback.onFailure(new Exception("Failed to delete profile: " + e.getMessage())));
    }
}