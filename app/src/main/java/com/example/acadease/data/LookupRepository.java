package com.example.acadease.data;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class LookupRepository {
    private final FirebaseFirestore db;
    private final String PROGRAMS_COLLECTION = "programs";
    private final String USERS_COLLECTION = "users";
    private final String COURSES_COLLECTION = "Courses"; // Used for course search/list

    public LookupRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface LookupListCallback {
        void onSuccess(List<String> suggestions);
        void onFailure(Exception e);
    }

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
     * Fetches all existing Course Codes (Document IDs) for general auto-suggestion/search.
     */
    public void fetchAllCourseCodes(LookupListCallback callback) {
        db.collection(COURSES_COLLECTION)
                .limit(100)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> courseCodes = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        courseCodes.add(document.getId());
                    }
                    callback.onSuccess(courseCodes);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches UIDs of all Faculty members for assignment/schedule purposes.
     */
    public void fetchAllFacultyUids(LookupListCallback callback) {
        db.collection(USERS_COLLECTION)
                .whereEqualTo("role", "faculty")
                .limit(100)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> facultyUids = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        facultyUids.add(document.getId()); // The UID is the Document ID
                    }
                    callback.onSuccess(facultyUids);
                })
                .addOnFailureListener(callback::onFailure);
    }
}