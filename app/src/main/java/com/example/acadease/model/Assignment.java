package com.example.acadease.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.Timestamp;

public class Assignment {

    @Exclude
    private String id; // Document ID of the assignment

    private String courseCode;
    private String title;
    private String description;
    private String fileUrl; // URL pointing to the assignment file in Firebase Storage
    private Timestamp dueDate;
    private int maxPoints;
    private String facultyId; // UID of the creator
    private Timestamp createdAt;

    // Required No-Argument Constructor
    public Assignment() {}

    // --- Getters and Setters (Omitted for brevity, but must be complete) ---
    @Exclude public String getId() { return id; }
    @Exclude public void setId(String id) { this.id = id; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    // ... complete all other getters/setters ...

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public Timestamp getDueDate() { return dueDate; }
    public void setDueDate(Timestamp dueDate) { this.dueDate = dueDate; }

    public int getMaxPoints() { return maxPoints; }
    public void setMaxPoints(int maxPoints) { this.maxPoints = maxPoints; }

    public String getFacultyId() { return facultyId; }
    public void setFacultyId(String facultyId) { this.facultyId = facultyId; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}