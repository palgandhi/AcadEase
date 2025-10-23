package com.example.acadease.model;

import com.google.firebase.firestore.Exclude;

// NOTE: No List import needed as only Strings and int are used directly

public class Course {

    @Exclude // Document ID is the courseCode
    private String id;

    // Core identification
    private String courseCode;
    private String title;

    // Foreign Keys / Links
    private String departmentCode; // Links to departments/{deptCode}
    private String facultyId;      // Primary instructor UID
    private String programId;      // Links to programs/{programId} (CRITICAL new link)

    // Details
    private int credits;
    private String description;

    // Required No-Argument Constructor for Firestore
    public Course() {}

    // --- Getters and Setters (Required for Firestore) ---

    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getFacultyId() { return facultyId; }
    public void setFacultyId(String facultyId) { this.facultyId = facultyId; }

    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}