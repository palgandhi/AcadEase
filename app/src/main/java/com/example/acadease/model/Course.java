package com.example.acadease.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

public class Course {

    @Exclude
    private String id;

    private String courseCode;
    private String title;

    // Foreign Keys / Links
    @PropertyName("deptCode")
    private String departmentCode; // Maps to 'deptCode' in DB

    private String facultyId;

    @PropertyName("program_id")
    private String programId;

    private int credits;
    private String description;

    // NEW FIELD: Links this course to the semester it is taught in (stored as number in Firestore)
    private int semesterTaughtIn;

    public Course() {}

    // --- Getters and Setters ---

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

    // PROGRESSION FIELD
    public int getSemesterTaughtIn() { return semesterTaughtIn; }
    public void setSemesterTaughtIn(int semesterTaughtIn) { this.semesterTaughtIn = semesterTaughtIn; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}