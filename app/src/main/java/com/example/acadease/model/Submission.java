package com.example.acadease.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.Timestamp;

public class Submission {

    // 1. IDENTIFIERS (Document ID is the Student's UID)
    @Exclude
    private String id;

    private String studentId;
    private String assignmentId;
    private String courseCode;

    // 2. SUBMISSION DETAILS
    private String submissionUrl; // Link to the file in Firebase Storage
    private Timestamp submittedAt;

    // 3. GRADING DETAILS
    private int grade; // The score awarded (e.g., 45 out of 50)
    private String gradedBy; // Faculty UID who awarded the grade
    private Timestamp gradedAt;

    // Required No-Argument Constructor for Firestore Deserialization
    public Submission() {}

    // --- Getters and Setters (Required for Firestore Mapping) ---

    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getAssignmentId() { return assignmentId; }
    public void setAssignmentId(String assignmentId) { this.assignmentId = assignmentId; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getSubmissionUrl() { return submissionUrl; }
    public void setSubmissionUrl(String submissionUrl) { this.submissionUrl = submissionUrl; }

    public Timestamp getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Timestamp submittedAt) { this.submittedAt = submittedAt; }

    public int getGrade() { return grade; }
    public void setGrade(int grade) { this.grade = grade; }

    public String getGradedBy() { return gradedBy; }
    public void setGradedBy(String gradedBy) { this.gradedBy = gradedBy; }

    public Timestamp getGradedAt() { return gradedAt; }
    public void setGradedAt(Timestamp gradedAt) { this.gradedAt = gradedAt; }
}