package com.example.acadease.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.Timestamp;

// We do NOT import java.util.Map because contactInfo is a String/Number in your database

public class User {

    // 1. PRIMARY KEY (Document ID)
    @Exclude
    private String uid;

    // 2. Fields Matching Firestore Document (Case-Sensitive)

    // Note: This field is a single String in your database, not a Map.
    private String contactInfo;

    private Timestamp createdAt;
    private String email;
    private String facultyId;
    private String name; // NOTE: Mapped as 'name' (lowercase)
    private String profileImgUrl; // NOTE: Mapped as 'profileImgUrl' (CamelCase)
    private String role;
    private String studentId;

    // 3. REQUIRED: Public No-Argument Constructor
    public User() {
    }

    // --- Getters and Setters (Required for Firestore Mapping) ---

    // Excluded UID Getter/Setter
    @Exclude public String getUid() { return uid; }
    @Exclude public void setUid(String uid) { this.uid = uid; }

    // Core Data Getters/Setters

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFacultyId() { return facultyId; }
    public void setFacultyId(String facultyId) { this.facultyId = facultyId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProfileImgUrl() { return profileImgUrl; }
    public void setProfileImgUrl(String profileImgUrl) { this.profileImgUrl = profileImgUrl; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
}