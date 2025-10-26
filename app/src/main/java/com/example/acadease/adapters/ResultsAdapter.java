package com.example.acadease.adapters;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.acadease.R;
import com.example.acadease.data.LookupRepository;
import com.example.acadease.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.ViewHolder> {

    private final List<User> studentRoster;
    private final Context context;
    private final LookupRepository lookupRepository;

    private final Map<String, Integer> currentGradesMap;

    private final int maxPoints;

    public ResultsAdapter(Context context, List<User> studentRoster, int maxPoints, LookupRepository lookupRepository) {
        this.context = context;
        this.studentRoster = studentRoster;
        this.maxPoints = maxPoints;
        this.lookupRepository = lookupRepository;
        this.currentGradesMap = new HashMap<>(); // Map to store live grade inputs
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_results_input_row, parent, false);
        // Pass the live grades map to the ViewHolder
        return new ViewHolder(view, currentGradesMap);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User student = studentRoster.get(position);

        // 1. Student Name and ID Display
        String studentName = student.getName();
        String studentId = student.getStudentId();

        // Final Display Name
        String displayName = String.format("%s (%s)",
                studentName != null ? studentName : "N/A",
                studentId != null ? studentId : "ID N/A");

        holder.studentName.setText(displayName); // Line 61 of the original log context

        // 2. Max Points Display (Fixes the crash)
        holder.maxPointsText.setText(String.format("/ %d", maxPoints));

        // 3. Grade Input Setup
        String studentUid = student.getUid();

        // CRITICAL: Set the Document ID (Student UID) as the tag for saving the grade
        holder.gradeInputEt.setTag(studentUid);

        // Set the grade input if already present in the map (for editing/scrolling)
        if (currentGradesMap.containsKey(studentUid)) {
            holder.gradeInputEt.setText(String.valueOf(currentGradesMap.get(studentUid)));
        } else {
            holder.gradeInputEt.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return studentRoster.size();
    }

    /**
     * Public method used by the Fragment to retrieve all grades entered.
     */
    public Map<String, Integer> getAllGrades() {
        return currentGradesMap;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView studentName, maxPointsText;
        public EditText gradeInputEt;

        private final Map<String, Integer> gradesMap;

        public ViewHolder(@NonNull View view, Map<String, Integer> gradesMap) {
            super(view);
            this.gradesMap = gradesMap;

            // --- CRITICAL MAPPING FIX ---
            studentName = view.findViewById(R.id.result_student_name);
            maxPointsText = view.findViewById(R.id.max_points_text); // FIXED ID
            gradeInputEt = view.findViewById(R.id.result_score_input_et);

            // CRITICAL: Add TextWatcher to track grades as they are entered (live data binding)
            gradeInputEt.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) { }

                @Override
                public void afterTextChanged(Editable s) {
                    String gradeStr = s.toString();
                    String studentUid = (String) gradeInputEt.getTag();

                    if (studentUid != null && !gradeStr.isEmpty()) {
                        try {
                            gradesMap.put(studentUid, Integer.parseInt(gradeStr));
                        } catch (NumberFormatException e) {
                            // If faculty enters non-numeric text, ignore or log error
                            gradesMap.remove(studentUid);
                        }
                    } else if (studentUid != null) {
                        // Remove if field is cleared
                        gradesMap.remove(studentUid);
                    }
                }
            });
        }
    }
}