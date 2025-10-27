package com.example.acadease.fragments.student;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.acadease.R;
import com.example.acadease.data.StudentRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentAssignmentsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView countersText;
    private TextView emptyState;

    private StudentRepository repo;
    private String uid;

    public StudentAssignmentsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_assignments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new StudentRepository();
        uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        recyclerView = view.findViewById(R.id.assignments_recycler);
        progressBar = view.findViewById(R.id.assignments_progress);
        countersText = view.findViewById(R.id.assignments_counters);
        emptyState = view.findViewById(R.id.assignments_empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadAssignments();
    }

    private void loadAssignments() {
        showLoading(true);
        repo.fetchEnrolledCourseCodes(uid, new StudentRepository.EnrollmentsCallback() {
            @Override
            public void onSuccess(List<String> courseCodes) {
                if (!isAdded()) return;
                if (courseCodes.isEmpty()) { showEmpty("No enrolled courses."); return; }
                repo.fetchAssignmentsForCourses(courseCodes, new StudentRepository.AssignmentsCallback() {
                    @Override
                    public void onSuccess(List<DocumentSnapshot> assignments) {
                        if (!isAdded()) return;
                        // Build models
                        List<AssignmentItem> items = new ArrayList<>();
                        List<com.google.android.gms.tasks.Task<?>> tasks = new ArrayList<>();
                        for (DocumentSnapshot ds : assignments) {
                            AssignmentItem item = new AssignmentItem();
                            item.courseCode = ds.getReference().getParent().getParent().getId();
                            item.assignmentId = ds.getId();
                            item.title = ds.getString("title");
                            Timestamp due = ds.getTimestamp("dueDate");
                            item.dueDate = due != null ? due.toDate() : null;
                            items.add(item);

                            // Prefetch submission status for counters and ordering
                            DocumentReference subRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("Courses").document(item.courseCode)
                                    .collection("assignments").document(item.assignmentId)
                                    .collection("submissions").document(uid);
                            tasks.add(subRef.get().addOnSuccessListener(doc -> {
                                item.submitted = doc.exists();
                                item.submissionUrl = doc.getString("submissionUrl");
                                item.overdue = (item.dueDate != null && new Date().after(item.dueDate) && !item.submitted);
                            }));
                        }

                        com.google.android.gms.tasks.Tasks.whenAllComplete(tasks)
                                .addOnSuccessListener(v -> {
                                    if (!isAdded()) return;
                                    // Compute counters
                                    int completed = 0, pending = 0, overdue = 0;
                                    for (AssignmentItem it : items) {
                                        if (it.submitted) completed++;
                                        else if (it.overdue) overdue++;
                                        else pending++;
                                    }
                                    countersText.setText(String.format(Locale.getDefault(), "Pending: %d   Completed: %d   Overdue: %d", pending, completed, overdue));

                                    // Sort: Overdue > Pending > Completed; within buckets, by dueDate ascending
                                    Collections.sort(items, (a, b) -> {
                                        int rankA = a.submitted ? 2 : (a.overdue ? 0 : 1);
                                        int rankB = b.submitted ? 2 : (b.overdue ? 0 : 1);
                                        if (rankA != rankB) return Integer.compare(rankA, rankB);
                                        long ta = a.dueDate != null ? a.dueDate.getTime() : Long.MAX_VALUE;
                                        long tb = b.dueDate != null ? b.dueDate.getTime() : Long.MAX_VALUE;
                                        return Long.compare(ta, tb);
                                    });

                                    showLoading(false);
                                    recyclerView.setAdapter(new AssignmentsAdapter(items));
                                })
                                .addOnFailureListener(e -> {
                                    if (!isAdded()) return;
                                    showLoading(false);
                                    recyclerView.setAdapter(new AssignmentsAdapter(items));
                                });
                    }
                    @Override
                    public void onFailure(Exception e) {
                        if (!isAdded()) return;
                        showEmpty("Failed to load assignments.");
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

    static class AssignmentItem {
        String courseCode;
        String assignmentId;
        String title;
        Date dueDate;
        boolean submitted;
        boolean overdue;
        String submissionUrl;
    }

    // ---- File upload handling ----
    private AssignmentItem pendingUploadItem;
    private final ActivityResultLauncher<String[]> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null || pendingUploadItem == null || getContext() == null) {
                    return;
                }
                uploadSubmissionFile(pendingUploadItem, uri);
            });

    private void startFilePick(AssignmentItem item) {
        this.pendingUploadItem = item;
        // Allow PDFs and common docs/images
        String[] types = new String[]{"application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "image/*"};
        filePickerLauncher.launch(types);
    }

    private void uploadSubmissionFile(AssignmentItem item, Uri uri) {
        try {
            // Persist permission for future access while upload runs
            requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) { Toast.makeText(requireContext(), "Not authenticated", Toast.LENGTH_SHORT).show(); return; }

        // Build a storage path: submissions/{course}/{assignment}/{uid}/{timestamp}.bin
        String filename = "file_" + System.currentTimeMillis();
        StorageReference ref = FirebaseStorage.getInstance().getReference()
                .child("submissions")
                .child(item.courseCode)
                .child(item.assignmentId)
                .child(uid)
                .child(filename);

        progressBar.setVisibility(View.VISIBLE);
        ref.putFile(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) { throw task.getException(); }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    // Write/overwrite submission doc
                    DocumentReference subRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("Courses").document(item.courseCode)
                            .collection("assignments").document(item.assignmentId)
                            .collection("submissions").document(uid);
                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("studentId", uid);
                    data.put("courseCode", item.courseCode);
                    data.put("assignmentId", item.assignmentId);
                    data.put("submissionUrl", downloadUri.toString());
                    data.put("submittedAt", Timestamp.now());
                    subRef.set(data)
                            .addOnSuccessListener(v -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Submitted", Toast.LENGTH_SHORT).show();
                                // Refresh list to update counters/status
                                loadAssignments();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Failed to save submission", Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_LONG).show();
                });
    }

    private class AssignmentsAdapter extends RecyclerView.Adapter<AssignmentsAdapter.VH> {
        private final List<AssignmentItem> items;
        AssignmentsAdapter(List<AssignmentItem> items) { this.items = items; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_assignment_row, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            AssignmentItem it = items.get(position);
            h.title.setText(it.title != null ? it.title : it.assignmentId);
            h.course.setText(it.courseCode);
            if (it.dueDate != null) {
                h.due.setText(android.text.format.DateFormat.format("MMM dd, yyyy", it.dueDate));
            } else {
                h.due.setText("-");
            }
            if (it.submitted) {
                h.status.setText("Completed");
                h.submitBtn.setText("View");
                h.submitBtn.setEnabled(true);
                h.submitBtn.setOnClickListener(v -> {
                    String url = it.submissionUrl;
                    if (url != null && !url.isEmpty()) {
                        try {
                            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url));
                            startActivity(intent);
                        } catch (Exception ex) {
                            Toast.makeText(requireContext(), "Invalid URL", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                h.status.setText(it.overdue ? "Overdue" : "Pending");
                h.submitBtn.setText("Upload");
                h.submitBtn.setEnabled(true);
                h.submitBtn.setOnClickListener(v -> startFilePick(it));
            }
        }
        @Override public int getItemCount() { return items.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView title, course, due, status;
            Button submitBtn;
            VH(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.assign_title);
                course = itemView.findViewById(R.id.assign_course);
                due = itemView.findViewById(R.id.assign_due);
                status = itemView.findViewById(R.id.assign_status);
                submitBtn = itemView.findViewById(R.id.assign_submit_btn);
            }
        }
    }

    private void promptSubmitUrl(AssignmentItem it) {
        EditText input = new EditText(requireContext());
        input.setHint("Paste submission URL");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        new AlertDialog.Builder(requireContext())
                .setTitle("Submit Assignment")
                .setView(input)
                .setPositiveButton("Submit", (d, w) -> {
                    String url = input.getText().toString().trim();
                    if (url.isEmpty()) {
                        Toast.makeText(requireContext(), "URL required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    repo.submitAssignmentUrl(it.courseCode, it.assignmentId, uid, url, new StudentRepository.SubmissionWriteCallback() {
                        @Override public void onSuccess() {
                            Toast.makeText(requireContext(), "Submitted", Toast.LENGTH_SHORT).show();
                            loadAssignments();
                        }
                        @Override public void onFailure(Exception e) {
                            Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
