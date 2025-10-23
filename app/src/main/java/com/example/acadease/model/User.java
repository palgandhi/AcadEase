package com.example.acadease.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.Timestamp;

import java.util.Map;
import java.util.HashMap;

public class User {

    // 1. PRIMARY KEY (Document ID) - Stored in Firebase Auth
    @Exclude
    private String uid;

    // 2. Profile Details (From Registration Form)
    private String email;
    private String role; // 'student', 'faculty', or 'admin'

    // We use separate fields for name to match the input form and keep data segregated
    private String Name;

    // 3. Identification Numbers (Used for lookups/reporting, distinct from UID)
    private String studentId;
    private String facultyId;

    // 4. Nested Contact Info (Map is flexible for phone, address, etc.)
    private Map<String, String> contactInfo;

    // 5. File URL and Timestamp
    private String profileImageUrl;
    private Timestamp createdAt;

    // 6. REQUIRED: Public No-Argument Constructor for Firestore Deserialization
    public User() {
        // Initialize map to avoid NullPointerException on read/write if empty
        this.contactInfo = new HashMap<>();
    }


    // Excluded UID Getter/Setter
    @Exclude
    public String getUid() { return uid; }
    @Exclude
    public void setUid(String uid) { this.uid = uid; }

    // Core Data Getters/Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getName() { return Name; }
    public void setName(String Name) { this.Name = Name; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getFacultyId() { return facultyId; }
    public void setFacultyId(String facultyId) { this.facultyId = facultyId; }

    // Contact Info (Map<String, String>)
    public Map<String, String> getContactInfo() { return contactInfo; }
    public void setContactInfo(Map<String, String> contactInfo) { this.contactInfo = contactInfo; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}