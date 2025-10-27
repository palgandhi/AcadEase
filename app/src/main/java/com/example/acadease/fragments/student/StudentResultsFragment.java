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
import com.example.acadease.data.StudentRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentResultsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyState;
    private TextView sgpaBadge;

    private StudentRepository repo;
    private String uid;

    public StudentResultsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_results, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new StudentRepository();
        uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        recyclerView = view.findViewById(R.id.results_recycler);
        progressBar = view.findViewById(R.id.results_progress);
        emptyState = view.findViewById(R.id.results_empty);
        sgpaBadge = view.findViewById(R.id.sgpa_badge_text);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadResults();
    }

    private void loadResults() {
        showLoading(true);
        repo.fetchEnrolledCourseCodes(uid, new StudentRepository.EnrollmentsCallback() {
            @Override
            public void onSuccess(List<String> courseCodes) {
                if (!isAdded()) return;
                if (courseCodes.isEmpty()) { showEmpty("No enrolled courses."); return; }
                repo.fetchExamScores(uid, courseCodes, new StudentRepository.ExamScoresCallback() {
                    @Override
                    public void onSuccess(List<StudentRepository.ExamScore> scores) {
                        if (!isAdded()) return;
                        if (scores.isEmpty()) { showEmpty("No results available yet."); return; }
                        // Compute SGPA using course credits. Fetch meta first
                        repo.fetchCourseMetaByCodes(courseCodes, new StudentRepository.CourseMetaCallback() {
                            @Override
                            public void onSuccess(Map<String, StudentRepository.CourseMeta> metaByCode) {
                                if (!isAdded()) return;
                                showLoading(false);
                                recyclerView.setAdapter(new ResultsAdapter(scores));
                                double sgpa = computeSgpa(scores, metaByCode);
                                sgpaBadge.setText(String.format(Locale.getDefault(), "%.2f", sgpa));
                            }
                            @Override
                            public void onFailure(Exception e) {
                                if (!isAdded()) return;
                                showLoading(false);
                                recyclerView.setAdapter(new ResultsAdapter(scores));
                                double sgpa = computeSgpa(scores, new HashMap<>());
                                sgpaBadge.setText(String.format(Locale.getDefault(), "%.2f", sgpa));
                            }
                        });
                    }
                    @Override
                    public void onFailure(Exception e) {
                        if (!isAdded()) return;
                        showEmpty("Failed to load results.");
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

    private double computeSgpa(List<StudentRepository.ExamScore> scores, Map<String, StudentRepository.CourseMeta> metaByCode) {
        // Aggregate course weighted percent = sum(obtained)/sum(max) per course
        Map<String, double[]> byCourse = new HashMap<>(); // [obtainedSum, maxSum]
        for (StudentRepository.ExamScore s : scores) {
            double[] arr = byCourse.computeIfAbsent(s.courseCode, k -> new double[]{0,0});
            arr[0] += s.obtained;
            arr[1] += s.maxPoints;
        }
        double totalWeighted = 0;
        double totalCredits = 0;
        for (Map.Entry<String, double[]> e : byCourse.entrySet()) {
            String code = e.getKey();
            double obtained = e.getValue()[0];
            double max = Math.max(e.getValue()[1], 1.0);
            double percent = (obtained * 100.0) / max;
            // Simple grade mapping: GPA ~ percentage/10 capped to 10
            double gradePoint = Math.min(10.0, Math.max(0.0, percent / 10.0));
            int credits = metaByCode.get(code) != null ? metaByCode.get(code).credits : 3;
            totalWeighted += gradePoint * credits;
            totalCredits += credits;
        }
        if (totalCredits == 0) return 0.0;
        return totalWeighted / totalCredits;
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

    private static class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.VH> {
        private final List<StudentRepository.ExamScore> items;
        ResultsAdapter(List<StudentRepository.ExamScore> items) { this.items = items; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_result_row, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            StudentRepository.ExamScore s = items.get(position);
            h.colCourse.setText(s.courseCode);
            h.colExam.setText(s.examTitle);
            h.colMarks.setText(String.format(Locale.getDefault(), "%.0f/%.0f", s.obtained, s.maxPoints));
            h.colPercent.setText(String.format(Locale.getDefault(), "%.1f%%", s.percentage));
        }
        @Override public int getItemCount() { return items.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView colCourse, colExam, colMarks, colPercent;
            VH(@NonNull View itemView) {
                super(itemView);
                colCourse = itemView.findViewById(R.id.col_course);
                colExam = itemView.findViewById(R.id.col_exam);
                colMarks = itemView.findViewById(R.id.col_marks);
                colPercent = itemView.findViewById(R.id.col_percent);
            }
        }
    }
}
