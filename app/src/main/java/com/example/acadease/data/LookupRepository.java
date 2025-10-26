package com.example.acadease.data;

import com.example.acadease.model.User; // CRITICAL: Import the User model
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LookupRepository {
    private final FirebaseFirestore db;

    // Collection Name Constants (CRITICAL: Match database case exactly)
    private final String PROGRAMS_COLLECTION = "programs";
    private final String USERS_COLLECTION = "users";
    private final String COURSES_COLLECTION = "Courses";

    public LookupRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // =========================================================
    // CALLBACK INTERFACES
    // =========================================================

    public interface LookupListCallback {
        void onSuccess(List<String> suggestions);
        void onFailure(Exception e);
    }

    public interface BulkNameCallback {
        void onSuccess(Map<String, String> uidToNameMap);
        void onFailure(Exception e);
    }

    public interface CourseTitleCallback {
        void onSuccess(String title);
        void onFailure(Exception e);
    }

    /**
     * CRITICAL: NEW INTERFACE to return a list of full User objects.
     */
    public interface BulkProfileCallback {
        void onSuccess(List<User> userProfiles);
        void onFailure(Exception e);
    }


    // =========================================================
    // LOOKUP METHODS
    // =========================================================

    /**
     * Fetches all existing Program Codes (Document IDs) for the student enrollment dropdown.
     */
    public void fetchProgramCodes(LookupListCallback callback) {
        db.collection(PROGRAMS_COLLECTION)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> programCodes = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        programCodes.add(document.getId());
                    }
                    callback.onSuccess(programCodes);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches names for a bulk list of student UIDs.
     */
    public void fetchBulkStudentNames(List<String> studentUids, BulkNameCallback callback) {
        if (studentUids == null || studentUids.isEmpty()) {
            callback.onSuccess(new HashMap<>());
            return;
        }

        db.collection(USERS_COLLECTION)
                .whereIn(FieldPath.documentId(), studentUids)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, String> uidToNameMap = new HashMap<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String fullName = document.getString("name");

                        if (fullName == null || fullName.isEmpty()) {
                            fullName = "Unknown Student (Profile Error)";
                        }

                        uidToNameMap.put(document.getId(), fullName);
                    }
                    callback.onSuccess(uidToNameMap);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * NEW METHOD: Fetches a list of full User profiles for the grading roster.
     */
    public void fetchBulkStudentProfiles(List<String> studentUids, BulkProfileCallback callback) {
        if (studentUids == null || studentUids.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        db.collection(USERS_COLLECTION)
                .whereIn(FieldPath.documentId(), studentUids)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> userProfiles = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // Map the document to the full User POJO
                        User user = document.toObject(User.class);
                        user.setUid(document.getId()); // Set the UID
                        userProfiles.add(user);
                    }
                    callback.onSuccess(userProfiles);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // NOTE: fetchAllCourseCodes and fetchAllFacultyUids remain as implemented.
}