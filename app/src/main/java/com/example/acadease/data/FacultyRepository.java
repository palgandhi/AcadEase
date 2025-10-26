package com.example.acadease.data;

import android.util.Log;

import com.example.acadease.model.Session;
import com.example.acadease.model.User;
import com.example.acadease.model.Assignment;
import com.example.acadease.model.Submission;
import com.example.acadease.model.Course;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class FacultyRepository {
    private static final String TAG = "FacultyRepository";
    private final FirebaseFirestore db;

    // Collection Name Constants (CRITICAL: Match database case exactly)
    private final String SESSIONS_COLLECTION = "sessions";
    private final String ENROLLMENTS_COLLECTION = "Enrollments";
    private final String USERS_COLLECTION = "users";
    private final String COURSES_COLLECTION = "Courses";
    private final String EXAM_SCORES_SUBCOLLECTION = "exam_scores";

    private final String EXAM_TYPES_COLLECTION = "exam_types";

    public FacultyRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // =========================================================
    // CALLBACK INTERFACES
    // =========================================================

    public interface ScheduleSessionsCallback {
        void onSuccess(List<Session> sessions);
        void onFailure(Exception e);
    }

    public interface RosterCallback {
        void onSuccess(List<String> studentUids);
        void onFailure(Exception e);
    }
    public interface CourseTitleCallback {
        void onSuccess(String title);
        void onFailure(Exception e);
    }

    public interface AttendanceWriteCallback { // <--- THIS MUST BE PUBLIC
        void onSuccess(String message);
        void onFailure(Exception e);
    }

    public interface UserProfileCallback {
        void onSuccess(User user);
        void onFailure(Exception e);
    }

    public interface CourseListCallback {
        void onSuccess(List<Course> courses);
        void onFailure(Exception e);
    }

    public interface AssignmentListCallback {
        void onSuccess(List<Assignment> assignments);
        void onFailure(Exception e);
    }

    public interface SubmissionListCallback {
        void onSuccess(List<Submission> submissions);
        void onFailure(Exception e);
    }

    public interface RegistrationCallback { // Reused for general write success/failure
        void onSuccess(String message);
        void onFailure(Exception e);
    }

    // =========================================================
    // 1. SCHEDULE AND SESSION RETRIEVAL
    // =========================================================

    /**
     * Fetches all scheduled sessions within a date range, filtered by facultyId.
     */
    public void fetchScheduleSessions(String userUid, String role, Timestamp startOfRange, Timestamp endOfRange, ScheduleSessionsCallback callback) {

        if (userUid == null || userUid.isEmpty()) {
            Log.e(TAG, "User UID is null. Cannot fetch sessions.");
            callback.onFailure(new Exception("Authentication Error: User ID missing."));
            return;
        }

        CollectionReference sessionsRef = db.collection(SESSIONS_COLLECTION);

        // Filter 1: By the faculty member's UID
        Query query = sessionsRef.whereEqualTo("facultyId", userUid);

        // Filter 2 & 3: By the date range and ordering (Requires Composite Index)
        query = query.whereGreaterThanOrEqualTo("sessionTime", startOfRange)
                .whereLessThanOrEqualTo("sessionTime", endOfRange)
                .orderBy("sessionTime", Query.Direction.ASCENDING);

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Session> sessions = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Session session = document.toObject(Session.class);
                            session.setId(document.getId());
                            sessions.add(session);
                        } catch (Exception e) {
                            Log.e(TAG, "MAPPING FAILED for session document " + document.getId(), e);
                        }
                    }
                    callback.onSuccess(sessions);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch schedule sessions.", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Fetches the roster (list of student UIDs) for a specific course.
     */
    public void fetchCourseRoster(String courseCode, RosterCallback callback) {
        db.collection(ENROLLMENTS_COLLECTION)
                .whereEqualTo("courseCode", courseCode)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> studentUids = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // Extract the studentId from each enrollment document
                        String studentId = document.getString("studentId");
                        if (studentId != null) {
                            studentUids.add(studentId);
                        }
                    }
                    callback.onSuccess(studentUids);
                })
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    // =========================================================
    // 2. COURSE AND USER LOOKUPS (UI Display)
    // =========================================================


    public void fetchCoursesTaught(String facultyUid, CourseListCallback callback) {
        db.collection(COURSES_COLLECTION)
                .whereEqualTo("facultyId", facultyUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Course> courses = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Course course = document.toObject(Course.class);
                            course.setId(document.getId());
                            courses.add(course);
                        } catch (Exception e) {
                            Log.e(TAG, "MAPPING FAILED for course document " + document.getId(), e);
                        }
                    }
                    callback.onSuccess(courses);
                })
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    /**
     * Fetches the full User profile (needed for first/last name).
     */
    public void fetchUserProfile(String userUid, UserProfileCallback callback) {
        db.collection(USERS_COLLECTION).document(userUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            callback.onSuccess(user);
                            return;
                        }
                    }
                    callback.onFailure(new Exception("Profile not found or invalid."));
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches the full course title for a given course code.
     */
    public void fetchCourseTitle(String courseCode, CourseTitleCallback callback) {
        db.collection(COURSES_COLLECTION).document(courseCode).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String title = documentSnapshot.getString("title");
                        callback.onSuccess(title != null ? title : courseCode);
                    } else {
                        callback.onSuccess(courseCode); // Fallback to code if not found
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Course title lookup failed for code: " + courseCode, e);
                    callback.onSuccess(courseCode); // Fallback on failure
                });
    }

    // =========================================================
    // 3. ASSIGNMENTS AND GRADES
    // =========================================================

    /**
     * Uploads a new assignment document to the courses/{code}/assignments subcollection.
     */
    public void createAssignment(Assignment assignment, String courseCode, RegistrationCallback callback) {
        DocumentReference assignmentRef = db.collection(COURSES_COLLECTION)
                .document(courseCode)
                .collection("assignments")
                .document(); // Auto-generate ID

        assignment.setCourseCode(courseCode);
        assignment.setFacultyId(FirebaseAuth.getInstance().getCurrentUser().getUid());
        assignment.setCreatedAt(Timestamp.now());

        assignmentRef.set(assignment)
                .addOnSuccessListener(aVoid -> callback.onSuccess("Assignment " + assignment.getTitle() + " posted."))
                .addOnFailureListener(e -> callback.onFailure(new Exception("Failed to post assignment: " + e.getMessage())));
    }

    /**
     * Fetches all student submissions for a specific assignment in a course.
     */
    public void fetchSubmissions(String courseCode, String assignmentId, SubmissionListCallback callback) {
        db.collection(COURSES_COLLECTION)
                .document(courseCode)
                .collection("assignments")
                .document(assignmentId)
                .collection("submissions") // Target the nested submissions subcollection
                .orderBy("submittedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Submission> submissions = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Submission submission = document.toObject(Submission.class);
                            submission.setId(document.getId());
                            submissions.add(submission);
                        } catch (Exception e) {
                            Log.e(TAG, "MAPPING FAILED for submission document " + document.getId(), e);
                        }
                    }
                    callback.onSuccess(submissions);
                })
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    /**
     * Updates grades for multiple student submissions in a single Batched Write.
     */
    public void updateSubmissionGrades(String courseCode, String assignmentId, Map<String, Integer> gradesMap, RegistrationCallback callback) {

        WriteBatch batch = db.batch();
        Timestamp gradedTime = Timestamp.now();
        String facultyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        for (Map.Entry<String, Integer> entry : gradesMap.entrySet()) {
            String studentId = entry.getKey();
            int grade = entry.getValue();

            DocumentReference submissionRef = db.collection(COURSES_COLLECTION)
                    .document(courseCode)
                    .collection("assignments")
                    .document(assignmentId)
                    .collection("submissions")
                    .document(studentId);

            // Update the existing submission document
            Map<String, Object> updates = new HashMap<>();
            updates.put("grade", grade);
            updates.put("gradedBy", facultyUid);
            updates.put("gradedAt", gradedTime);

            batch.update(submissionRef, updates); // Use update() since submission must exist
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess("Successfully saved grades for " + gradesMap.size() + " submissions."))
                .addOnFailureListener(e -> callback.onFailure(new Exception("Grade save failed: " + e.getMessage())));
    }

    /**
     * Saves non-submission (exam/quiz) scores for all students in a single document.
     */
    public void saveExamScores(String courseCode, String examTitle, int maxPoints, Map<String, Integer> gradesMap, RegistrationCallback callback) {

        // Sanitize exam title for use as a Firestore Document ID
        String examDocumentId = examTitle.replaceAll("[^a-zA-Z0-9\\-]", "_").toLowerCase();

        DocumentReference examScoresRef = db.collection(COURSES_COLLECTION)
                .document(courseCode)
                .collection(EXAM_SCORES_SUBCOLLECTION)
                .document(examDocumentId);

        Map<String, Object> examData = new HashMap<>();
        examData.put("examTitle", examTitle);
        examData.put("maxPoints", maxPoints);
        examData.put("gradedAt", Timestamp.now());
        examData.put("scores", gradesMap); // The map of scores (UID -> Score)

        examScoresRef.set(examData)
                .addOnSuccessListener(aVoid -> callback.onSuccess("Exam scores for " + examTitle + " uploaded successfully."))
                .addOnFailureListener(e -> callback.onFailure(new Exception("Exam score upload failed: " + e.getMessage())));
    }

    public void recordAttendance(String sessionId, Map<String, String> attendanceMap, AttendanceWriteCallback callback) {

        WriteBatch batch = db.batch();
        Timestamp recordTime = Timestamp.now();

        // CRITICAL: Construct the path to the nested attendance subcollection
        CollectionReference attendanceRef = db.collection(SESSIONS_COLLECTION)
                .document(sessionId)
                .collection("attendance");

        for (Map.Entry<String, String> entry : attendanceMap.entrySet()) {
            String studentUid = entry.getKey();
            String status = entry.getValue();

            // The subcollection document ID is the student UID
            DocumentReference attendanceDocRef = attendanceRef.document(studentUid);

            Map<String, Object> data = new HashMap<>();
            data.put("studentId", studentUid);
            data.put("status", status);
            data.put("recordedAt", recordTime);

            batch.set(attendanceDocRef, data);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess("Attendance recorded for " + attendanceMap.size() + " students."))
                .addOnFailureListener(e -> callback.onFailure(new Exception("Attendance write failed: " + e.getMessage())));
    }

    public void fetchAssignmentsByCourse(String courseCode, AssignmentListCallback callback) {

        // Safety check
        if (courseCode == null || courseCode.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        db.collection(COURSES_COLLECTION)
                .document(courseCode)
                .collection("assignments") // Target the assignments subcollection
                .orderBy("dueDate", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Assignment> assignments = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Assignment assignment = document.toObject(Assignment.class);
                            assignment.setId(document.getId());
                            assignments.add(assignment);
                        } catch (Exception e) {
                            Log.e(TAG, "MAPPING FAILED for assignment document " + document.getId(), e);
                        }
                    }
                    callback.onSuccess(assignments);
                })
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    public interface ExamTypeCallback {
        void onSuccess(List<String> examTitles);
        void onFailure(Exception e);
    }

    /**
     * Fetches all defined exam types from the dedicated collection for the dropdown.
     */
    public void fetchExamTypes(ExamTypeCallback callback) {
        db.collection(EXAM_TYPES_COLLECTION)
                .orderBy(FieldPath.documentId(), Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> titles = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        titles.add(document.getId()); // The Document ID is the exam title
                    }
                    callback.onSuccess(titles);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public interface ExamDetailCallback {
        void onSuccess(int maxPoints);
        void onFailure(Exception e);
    }

    /**
     * Fetches the max points for a given exam type from the 'exam_types' collection.
     */
    public void fetchExamMaxPoints(String courseCode, String examTitle, ExamDetailCallback callback) {
        db.collection(COURSES_COLLECTION).document(courseCode)
                .collection(EXAM_TYPES_COLLECTION) // Use the nested path
                .document(examTitle).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long points = documentSnapshot.getLong("maxPoints");
                        callback.onSuccess(points != null ? points.intValue() : 0);
                    } else {
                        callback.onFailure(new Exception("Exam type not found for this course."));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Creates a new exam type document nested under a specific course.
     */
    public void addNewExamType(String courseCode, String examTitle, int maxPoints, RegistrationCallback callback) {

        String examDocumentId = examTitle.replaceAll("[^a-zA-Z0-9\\-]", "_").toLowerCase();

        // CRITICAL FIX: Target the nested subcollection path
        DocumentReference examRef = db.collection(COURSES_COLLECTION).document(courseCode)
                .collection(EXAM_TYPES_COLLECTION)
                .document(examDocumentId);

        Map<String, Object> examData = new HashMap<>();
        examData.put("maxPoints", maxPoints);
        examData.put("createdAt", Timestamp.now());

        examRef.set(examData)
                .addOnSuccessListener(aVoid -> callback.onSuccess("New exam type '" + examTitle + "' created for " + courseCode + "."))
                .addOnFailureListener(e -> callback.onFailure(new Exception("Failed to create exam type: " + e.getMessage())));
    }
}