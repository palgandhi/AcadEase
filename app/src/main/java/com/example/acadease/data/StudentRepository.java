package com.example.acadease.data;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.acadease.model.Session;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class StudentRepository {

    private static final String TAG = "StudentRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Executor bg = Executors.newSingleThreadExecutor();

    // -------- Enrollments --------
    public interface EnrollmentsCallback {
        void onSuccess(List<String> courseCodes);
        void onFailure(Exception e);
    }

    // -------- Attendance: per-course detailed sessions --------
    public static class SessionWithStatus {
        public Session session;
        public String status; // present/absent/null
    }

    public interface CourseAttendanceSessionsCallback {
        void onSuccess(List<SessionWithStatus> list);
        void onFailure(Exception e);
    }

    public void fetchAttendanceSessionsForCourse(@NonNull String studentUid, @NonNull String courseCode, @NonNull CourseAttendanceSessionsCallback cb) {
        // 1) Fetch sessions for course
        db.collection("sessions")
                .whereEqualTo("courseCode", courseCode)
                .get()
                .addOnSuccessListener(qs -> {
                    List<SessionWithStatus> out = new ArrayList<>();
                    List<com.google.android.gms.tasks.Task<?>> tasks = new ArrayList<>();
                    for (DocumentSnapshot ds : qs.getDocuments()) {
                        Session s = ds.toObject(Session.class);
                        if (s == null) continue;
                        s.setId(ds.getId());
                        SessionWithStatus sw = new SessionWithStatus();
                        sw.session = s;
                        out.add(sw);
                        // For each session, read its attendance subcollection
                        tasks.add(ds.getReference().collection("attendance").get()
                                .addOnSuccessListener(attQs -> {
                                    for (DocumentSnapshot aDoc : attQs.getDocuments()) {
                                        // Two options supported: documentId == studentUid OR field studentId == studentUid
                                        boolean match = aDoc.getId().equals(studentUid);
                                        if (!match) {
                                            String sid = aDoc.getString("studentId");
                                            match = (sid != null && sid.equals(studentUid));
                                        }
                                        if (match) {
                                            sw.status = String.valueOf(aDoc.getString("status"));
                                            break;
                                        }
                                    }
                                }));
                    }
                    Tasks.whenAllComplete(tasks).addOnSuccessListener(v -> {
                                // Sort locally by sessionTime if available
                                out.sort((a, b) -> {
                                    if (a.session.getSessionTime() == null || b.session.getSessionTime() == null) return 0;
                                    return a.session.getSessionTime().toDate().compareTo(b.session.getSessionTime().toDate());
                                });
                                cb.onSuccess(out);
                            })
                            .addOnFailureListener(cb::onFailure);
                })
                .addOnFailureListener(cb::onFailure);
    }

    public void fetchEnrolledCourseCodes(@NonNull String studentUid, @NonNull EnrollmentsCallback cb) {
        db.collection("Enrollments")
                .whereEqualTo("studentId", studentUid)
                .get()
                .addOnSuccessListener(qs -> {
                    List<String> codes = new ArrayList<>();
                    for (QueryDocumentSnapshot d : qs) {
                        String code = d.getString("courseCode");
                        if (code != null) codes.add(code);
                    }
                    cb.onSuccess(codes);
                })
                .addOnFailureListener(cb::onFailure);
    }

    // -------- Sessions (Weekly) --------
    public interface SessionsCallback {
        void onSuccess(List<Session> sessions);
        void onFailure(Exception e);
    }

    public void fetchWeeklySessions(@NonNull String studentUid, @NonNull Timestamp start, @NonNull Timestamp end, @NonNull SessionsCallback cb) {
        fetchEnrolledCourseCodes(studentUid, new EnrollmentsCallback() {
            @Override public void onSuccess(List<String> courseCodes) {
                if (courseCodes.isEmpty()) { cb.onSuccess(new ArrayList<>()); return; }
                // Firestore 'in' supports up to 10 items; chunk if needed
                List<Session> all = new ArrayList<>();
                List<List<String>> chunks = chunk(courseCodes, 10);
                List<com.google.android.gms.tasks.Task<?>> tasks = new ArrayList<>();
                for (List<String> chunk : chunks) {
                    tasks.add(db.collection("sessions")
                            .whereIn("courseCode", chunk)
                            .whereGreaterThanOrEqualTo("sessionTime", start)
                            .whereLessThanOrEqualTo("sessionTime", end)
                            .get()
                            .addOnSuccessListener(qs -> {
                                for (DocumentSnapshot ds : qs.getDocuments()) {
                                    Session s = ds.toObject(Session.class);
                                    if (s != null) { s.setId(ds.getId()); all.add(s); }
                                }
                            }));
                }
                Tasks.whenAllComplete(tasks).addOnSuccessListener(v -> cb.onSuccess(all))
                        .addOnFailureListener(e -> {
                            // Fallback path for missing index: run without date filters and filter locally
                            fetchWeeklySessionsNoIndex(studentUid, start, end, cb);
                        });
            }
            @Override public void onFailure(Exception e) { cb.onFailure(e); }
        });
    }

    public void fetchWeeklySessionsNoIndex(@NonNull String studentUid, @NonNull Timestamp start, @NonNull Timestamp end, @NonNull SessionsCallback cb) {
        fetchEnrolledCourseCodes(studentUid, new EnrollmentsCallback() {
            @Override public void onSuccess(List<String> courseCodes) {
                if (courseCodes.isEmpty()) { cb.onSuccess(new ArrayList<>()); return; }
                List<Session> all = new ArrayList<>();
                List<List<String>> chunks = chunk(courseCodes, 10);
                List<com.google.android.gms.tasks.Task<?>> tasks = new ArrayList<>();
                for (List<String> chunk : chunks) {
                    tasks.add(db.collection("sessions")
                            .whereIn("courseCode", chunk)
                            .get()
                            .addOnSuccessListener(qs -> {
                                for (DocumentSnapshot ds : qs.getDocuments()) {
                                    Session s = ds.toObject(Session.class);
                                    if (s != null && s.getSessionTime() != null) {
                                        java.util.Date d = s.getSessionTime().toDate();
                                        if (!d.before(start.toDate()) && !d.after(end.toDate())) {
                                            s.setId(ds.getId());
                                            all.add(s);
                                        }
                                    }
                                }
                            }));
                }
                Tasks.whenAllComplete(tasks).addOnSuccessListener(v -> {
                    // Sort by sessionTime
                    all.sort((a, b) -> a.getSessionTime().toDate().compareTo(b.getSessionTime().toDate()));
                    cb.onSuccess(all);
                }).addOnFailureListener(cb::onFailure);
            }
            @Override public void onFailure(Exception e) { cb.onFailure(e); }
        });
    }

    // -------- Attendance --------
    public interface AttendanceStatsCallback {
        void onSuccess(Map<String, CourseAttendance> stats);
        void onFailure(Exception e);
    }

    public static class CourseAttendance {
        public int totalSessions;
        public int attendedSessions;
    }

    public void fetchAttendanceStats(@NonNull String studentUid, @NonNull List<String> courseCodes, @NonNull AttendanceStatsCallback cb) {
        if (courseCodes.isEmpty()) { cb.onSuccess(new HashMap<>()); return; }
        Map<String, CourseAttendance> out = new HashMap<>();
        List<com.google.android.gms.tasks.Task<?>> courseTasks = new ArrayList<>();
        for (String code : courseCodes) {
            out.put(code, new CourseAttendance());
            com.google.android.gms.tasks.Task<?> courseTask = db.collection("sessions")
                    .whereEqualTo("courseCode", code)
                    .get()
                    .onSuccessTask(qs -> {
                        java.util.Date now = new java.util.Date();
                        List<com.google.android.gms.tasks.Task<?>> attTasks = new ArrayList<>();
                        for (DocumentSnapshot ds : qs.getDocuments()) {
                            CourseAttendance ca = out.get(code);
                            Timestamp st = ds.getTimestamp("sessionTime");
                            boolean isPast = (st != null && !st.toDate().after(now));
                            attTasks.add(ds.getReference().collection("attendance").get()
                                    .addOnSuccessListener(attQs -> {
                                        if (isPast || !attQs.isEmpty()) {
                                            ca.totalSessions += 1;
                                        }
                                        for (DocumentSnapshot aDoc : attQs.getDocuments()) {
                                            boolean match = aDoc.getId().equals(studentUid);
                                            if (!match) {
                                                String sid = aDoc.getString("studentId");
                                                match = (sid != null && sid.equals(studentUid));
                                            }
                                            if (!match) {
                                                Map<String, Object> entries = (Map<String, Object>) aDoc.get("entries");
                                                if (entries != null && entries.containsKey(studentUid)) {
                                                    Object v = entries.get(studentUid);
                                                    if (v != null && "present".equalsIgnoreCase(String.valueOf(v))) {
                                                        ca.attendedSessions += 1;
                                                    }
                                                    break;
                                                }
                                            }
                                            if (match) {
                                                Object raw = aDoc.get("status");
                                                boolean isPresent = false;
                                                if (raw instanceof String) {
                                                    String s = ((String) raw).trim();
                                                    isPresent = s.equalsIgnoreCase("present") || s.equalsIgnoreCase("p") || s.equals("1");
                                                } else if (raw instanceof Boolean) {
                                                    isPresent = (Boolean) raw;
                                                } else if (raw instanceof Number) {
                                                    isPresent = ((Number) raw).intValue() == 1;
                                                }
                                                if (isPresent) {
                                                    ca.attendedSessions += 1;
                                                }
                                                break;
                                            }
                                        }
                                    }));
                        }
                        return com.google.android.gms.tasks.Tasks.whenAllComplete(attTasks);
                    });
            courseTasks.add(courseTask);
        }
        com.google.android.gms.tasks.Tasks.whenAllComplete(courseTasks)
                .addOnSuccessListener(v -> cb.onSuccess(out))
                .addOnFailureListener(cb::onFailure);
    }

    // -------- Results --------
    public interface ExamScoresCallback {
        void onSuccess(List<ExamScore> scores);
        void onFailure(Exception e);
    }

    public static class ExamScore {
        public String courseCode;
        public String examTitle;
        public double obtained;
        public double maxPoints;
        public double percentage;
    }

    public void fetchExamScores(@NonNull String studentUid, @NonNull List<String> courseCodes, @NonNull ExamScoresCallback cb) {
        if (courseCodes.isEmpty()) { cb.onSuccess(new ArrayList<>()); return; }
        List<ExamScore> list = new ArrayList<>();
        List<com.google.android.gms.tasks.Task<?>> tasks = new ArrayList<>();
        for (String code : courseCodes) {
            tasks.add(db.collection("Courses").document(code).collection("exam_scores")
                    .get()
                    .addOnSuccessListener(qs -> {
                        for (DocumentSnapshot ds : qs.getDocuments()) {
                            String examTitle = ds.getId();
                            Map<String, Object> scores = (Map<String, Object>) ds.get("scores");
                            Number max = (Number) ds.get("maxPoints");
                            if (scores != null && scores.containsKey(studentUid) && max != null) {
                                Number got = (Number) scores.get(studentUid);
                                ExamScore es = new ExamScore();
                                es.courseCode = code;
                                es.examTitle = examTitle;
                                es.obtained = got.doubleValue();
                                es.maxPoints = max.doubleValue();
                                es.percentage = es.maxPoints > 0 ? (es.obtained * 100.0 / es.maxPoints) : 0.0;
                                list.add(es);
                            }
                        }
                    }));
        }
        Tasks.whenAllComplete(tasks).addOnSuccessListener(v -> cb.onSuccess(list))
                .addOnFailureListener(cb::onFailure);
    }

    // -------- Assignments --------
    public interface AssignmentsCallback {
        void onSuccess(List<DocumentSnapshot> assignments);
        void onFailure(Exception e);
    }

    public void fetchAssignmentsForCourses(@NonNull List<String> courseCodes, @NonNull AssignmentsCallback cb) {
        if (courseCodes.isEmpty()) { cb.onSuccess(new ArrayList<>()); return; }
        List<DocumentSnapshot> result = new ArrayList<>();
        List<com.google.android.gms.tasks.Task<?>> tasks = new ArrayList<>();
        for (String code : courseCodes) {
            tasks.add(db.collection("Courses").document(code).collection("assignments").get()
                    .addOnSuccessListener(qs -> result.addAll(qs.getDocuments())));
        }
        Tasks.whenAllComplete(tasks).addOnSuccessListener(v -> cb.onSuccess(result))
                .addOnFailureListener(cb::onFailure);
    }

    public interface SubmissionWriteCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public void submitAssignmentUrl(@NonNull String courseCode, @NonNull String assignmentId, @NonNull String studentUid, @NonNull String submissionUrl, @NonNull SubmissionWriteCallback cb) {
        Map<String, Object> data = new HashMap<>();
        data.put("submissionUrl", submissionUrl);
        data.put("submittedAt", Timestamp.now());
        db.collection("Courses").document(courseCode)
                .collection("assignments").document(assignmentId)
                .collection("submissions").document(studentUid)
                .set(data)
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onFailure);
    }

    // -------- Course Meta (title, credits) --------
    public static class CourseMeta {
        public String courseCode;
        public String title;
        public int credits;
    }

    public interface CourseMetaCallback {
        void onSuccess(Map<String, CourseMeta> metaByCode);
        void onFailure(Exception e);
    }

    public void fetchCourseMetaByCodes(@NonNull List<String> courseCodes, @NonNull CourseMetaCallback cb) {
        if (courseCodes.isEmpty()) { cb.onSuccess(new HashMap<>()); return; }
        Map<String, CourseMeta> out = new HashMap<>();
        List<com.google.android.gms.tasks.Task<?>> tasks = new ArrayList<>();
        List<List<String>> chunks = chunk(courseCodes, 10);
        for (List<String> chunk : chunks) {
            tasks.add(db.collection("Courses")
                    .whereIn("courseCode", chunk)
                    .get()
                    .addOnSuccessListener(qs -> {
                        for (DocumentSnapshot ds : qs.getDocuments()) {
                            CourseMeta cm = new CourseMeta();
                            cm.courseCode = ds.getString("courseCode");
                            cm.title = ds.getString("title");
                            Number cr = (Number) ds.get("credits");
                            cm.credits = cr != null ? cr.intValue() : 0;
                            if (cm.courseCode != null) out.put(cm.courseCode, cm);
                        }
                    }));
        }
        Tasks.whenAllComplete(tasks).addOnSuccessListener(v -> cb.onSuccess(out))
                .addOnFailureListener(cb::onFailure);
    }

    // -------- Helpers --------
    private static <T> List<List<T>> chunk(List<T> src, int size) {
        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < src.size(); i += size) {
            out.add(src.subList(i, Math.min(src.size(), i + size)));
        }
        return out;
    }
}
