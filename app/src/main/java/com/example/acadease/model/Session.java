package com.example.acadease.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.Timestamp;
import java.util.Map;

public class Session {

    @Exclude
    private String id; // Document ID (e.g., 3zxLStDYRL8KGyGronbM)

    private String scheduleId;   // Links to the recurrence blueprint
    private String courseCode;   // e.g., "CS101"
    private String facultyId;    // e.g., "J7KS3bmdQ5fg2e4ivs1aXzQJdru1"
    private Timestamp sessionTime; // CRITICAL: Date and time of the specific class meeting
    private String venue;        // Room number, e.g., "A 303"
    private String type;         // e.g., "lecture"
    private String topic;        // Optional: What was covered (for past sessions)

    // Required No-Argument Constructor
    public Session() {}

    // --- Getters and Setters (Required for Firestore) ---

    @Exclude public String getId() { return id; }
    @Exclude public void setId(String id) { this.id = id; }

    public String getScheduleId() { return scheduleId; }
    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getFacultyId() { return facultyId; }
    public void setFacultyId(String facultyId) { this.facultyId = facultyId; }

    public Timestamp getSessionTime() { return sessionTime; }
    public void setSessionTime(Timestamp sessionTime) { this.sessionTime = sessionTime; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
}