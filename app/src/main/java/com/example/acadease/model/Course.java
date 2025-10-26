package com.example.acadease.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName; // Used to map snake_case to Java camelCase

public class Course {

    @Exclude
    private String id;

    private String courseCode;
    private String title;

    // FIX 1: Use @PropertyName to map snake_case or specific DB names
    @PropertyName("deptCode")
    private String departmentCode; // Maps to 'deptCode' in DB

    private String facultyId;

    @PropertyName("program_id")
    private String programId; // Maps to 'program_id' in DB

    private int credits;
    private String description;

    public Course() {}

    // --- Getters and Setters (Required for Firestore) ---

    @Exclude public String getId() { return id; }
    @Exclude public void setId(String id) { this.id = id; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @PropertyName("deptCode")
    public String getDepartmentCode() { return departmentCode; }
    @PropertyName("deptCode")
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getFacultyId() { return facultyId; }
    public void setFacultyId(String facultyId) { this.facultyId = facultyId; }

    @PropertyName("program_id")
    public String getProgramId() { return programId; }
    @PropertyName("program_id")
    public void setProgramId(String programId) { this.programId = programId; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; } // This expects a NUMBER in DB

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}