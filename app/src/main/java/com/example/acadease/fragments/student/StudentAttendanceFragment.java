package com.example.acadease.fragments.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.acadease.R;
import com.example.acadease.ProfileActivity;
import com.example.acadease.data.StudentRepository;
import com.google.firebase.auth.FirebaseAuth;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentAttendanceFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyState;

    private StudentRepository repo;
    private String uid;
    private Map<String, StudentRepository.CourseMeta> metaByCode = new java.util.HashMap<>();

    public StudentAttendanceFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_attendance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new StudentRepository();
        uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        recyclerView = view.findViewById(R.id.attendance_courses_recycler);
        progressBar = view.findViewById(R.id.attendance_progress);
        emptyState = view.findViewById(R.id.attendance_empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Wire profile icon click
        View profileIcon = view.findViewById(R.id.profile_icon);
        if (profileIcon != null) {
            Log.d("StudentAttendance", "Setting up profile icon click listener");
            profileIcon.setOnClickListener(v -> {
                Log.d("StudentAttendance", "Profile icon clicked!");
                startActivity(new Intent(requireContext(), ProfileActivity.class));
            });
        } else {
            Log.e("StudentAttendance", "Profile icon not found in view hierarchy!");
        }

        loadAttendanceStats();
    }

    private void loadAttendanceStats() {
        showLoading(true);
        repo.fetchEnrolledCourseCodes(uid, new StudentRepository.EnrollmentsCallback() {
            @Override
            public void onSuccess(List<String> courseCodes) {
                if (!isAdded()) return;
                if (courseCodes.isEmpty()) {
                    showEmpty("You're not enrolled in any courses.");
                    return;
                }
                repo.fetchCourseMetaByCodes(courseCodes, new StudentRepository.CourseMetaCallback() {
                    @Override public void onSuccess(Map<String, StudentRepository.CourseMeta> meta) {
                        metaByCode.clear(); metaByCode.putAll(meta);
                        repo.fetchAttendanceStats(uid, courseCodes, new StudentRepository.AttendanceStatsCallback() {
                            @Override
                            public void onSuccess(Map<String, StudentRepository.CourseAttendance> stats) {
                                if (!isAdded()) return;
                                showLoading(false);
                                if (stats.isEmpty()) {
                                    showEmpty("No attendance records yet.");
                                    return;
                                }
                                recyclerView.setAdapter(new CourseAttendanceAdapter(new ArrayList<>(stats.entrySet())));
                            }
                            @Override
                            public void onFailure(Exception e) {
                                if (!isAdded()) return;
                                showEmpty("Failed to load attendance.");
                            }
                        });
                    }
                    @Override public void onFailure(Exception e) {
                        // Proceed without meta
                        repo.fetchAttendanceStats(uid, courseCodes, new StudentRepository.AttendanceStatsCallback() {
                            @Override public void onSuccess(Map<String, StudentRepository.CourseAttendance> stats) {
                                if (!isAdded()) return;
                                showLoading(false);
                                if (stats.isEmpty()) { showEmpty("No attendance records yet."); return; }
                                recyclerView.setAdapter(new CourseAttendanceAdapter(new ArrayList<>(stats.entrySet())));
                            }
                            @Override public void onFailure(Exception e2) { if (!isAdded()) return; showEmpty("Failed to load attendance."); }
                        });
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) return;
                showEmpty("Failed to load enrollments.");
            }
        });
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(loading ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(View.GONE);
    }

    private void showEmpty(String message) {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        emptyState.setText(message);
    }

    private class CourseAttendanceAdapter extends RecyclerView.Adapter<CourseAttendanceAdapter.VH> {
        private final List<Map.Entry<String, StudentRepository.CourseAttendance>> items;
        CourseAttendanceAdapter(List<Map.Entry<String, StudentRepository.CourseAttendance>> items) { this.items = items; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_course_attendance_card, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            Map.Entry<String, StudentRepository.CourseAttendance> e = items.get(position);
            String courseCode = e.getKey();
            StudentRepository.CourseAttendance ca = e.getValue();
            int total = Math.max(ca.totalSessions, 0);
            int attended = Math.max(ca.attendedSessions, 0);
            int percent = total > 0 ? Math.round(attended * 100f / total) : 0;
            String title = metaByCode.get(courseCode) != null && metaByCode.get(courseCode).title != null ? metaByCode.get(courseCode).title : courseCode;
            h.courseTitle.setText(title);
            h.statsText.setText(String.format(Locale.getDefault(), "%d/%d sessions", attended, total));
            h.percentText.setText(String.format(Locale.getDefault(), "%d%%", percent));
            h.itemView.setOnClickListener(v -> openCourseDetail(courseCode));
        }
        @Override public int getItemCount() { return items.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView courseTitle, statsText, percentText;
            VH(@NonNull View itemView) {
                super(itemView);
                courseTitle = itemView.findViewById(R.id.att_course_title);
                statsText = itemView.findViewById(R.id.att_course_stats);
                percentText = itemView.findViewById(R.id.att_course_percent);
            }
        }
    }

    private void openCourseDetail(String courseCode) {
        // Simple inline sheet: replace with a dedicated fragment if you prefer
        // For now, show a bottom sheet-like dialog with sessions and statuses
        repo.fetchAttendanceSessionsForCourse(uid, courseCode, new StudentRepository.CourseAttendanceSessionsCallback() {
            @Override
            public void onSuccess(List<StudentRepository.SessionWithStatus> list) {
                if (!isAdded()) return;
                androidx.appcompat.app.AlertDialog.Builder b = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
                View root = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_attendance_course_sessions, null, false);
                RecyclerView rv = root.findViewById(R.id.att_course_sessions_recycler);
                rv.setLayoutManager(new LinearLayoutManager(requireContext()));
                rv.setAdapter(new RecyclerView.Adapter<SessionVH>() {
                    @NonNull @Override public SessionVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session_status_row, parent, false);
                        return new SessionVH(v);
                    }
                    @Override public void onBindViewHolder(@NonNull SessionVH h, int position) {
                        StudentRepository.SessionWithStatus s = list.get(position);
                        h.dateText.setText(android.text.format.DateFormat.format("MMM dd, yyyy  h:mm a", s.session.getSessionTime().toDate()));
                        String st = s.status == null ? "-" : s.status.substring(0, 1).toUpperCase() + s.status.substring(1);
                        h.statusText.setText(st);
                    }
                    @Override public int getItemCount() { return list.size(); }
                });
                b.setTitle(courseCode).setView(root).setPositiveButton("Close", (d, w) -> d.dismiss()).show();
            }
            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Failed to load course sessions", Toast.LENGTH_LONG).show();
            }
        });
    }

    static class SessionVH extends RecyclerView.ViewHolder {
        TextView dateText, statusText;
        SessionVH(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.session_date_text);
            statusText = itemView.findViewById(R.id.session_status_text);
        }
    }
}
