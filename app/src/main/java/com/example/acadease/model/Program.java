package com.example.acadease.model;

import com.google.firebase.firestore.Exclude;
import java.util.List;
import java.util.Map;

public class Program {

    @Exclude
    private String id;

    private String name;
    private String departmentCode;

    // CRITICAL FIX: Map key is the semester string (e.g., "sem1", "sem3"),
    // and the value is the array of course codes for that semester.
    private Map<String, List<String>> semesterCourses;

    public Program() {}

    // --- Getters and Setters ---

    @Exclude public String getId() { return id; }
    @Exclude public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    // PROGRESSION FIELD
    public Map<String, List<String>> getSemesterCourses() { return semesterCourses; }
    public void setSemesterCourses(Map<String, List<String>> semesterCourses) { this.semesterCourses = semesterCourses; }
}