package com.example.acadease.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.Timestamp;

import java.util.Map;
import java.util.HashMap;

public class User {

    @Exclude
    private String uid;

    // Profile Details
    private String email;
    private String role;
    private String firstName;

    // Identification & Progression (CRITICAL FIXES)
    private String studentId;
    private String facultyId;
    private int currentSemester; // NEW: Tracks academic level (e.g., 1, 3, 5)

    // Contact & Files
    private Map<String, String> contactInfo;
    private String profileImageUrl;
    private Timestamp createdAt;

    public User() {
        this.contactInfo = new HashMap<>();
    }

    // --- Getters and Setters ---

    @Exclude public String getUid() { return uid; }
    @Exclude public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getName() { return firstName; }
    public void setName(String firstName) { this.firstName = firstName; }


    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getFacultyId() { return facultyId; }
    public void setFacultyId(String facultyId) { this.facultyId = facultyId; }

    // PROGRESSION FIELD
    public int getCurrentSemester() { return currentSemester; }
    public void setCurrentSemester(int currentSemester) { this.currentSemester = currentSemester; }

    public Map<String, String> getContactInfo() { return contactInfo; }
    public void setContactInfo(Map<String, String> contactInfo) { this.contactInfo = contactInfo; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}