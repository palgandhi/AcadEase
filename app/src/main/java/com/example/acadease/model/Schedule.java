package com.example.acadease.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.Timestamp;
import java.util.List;

public class Schedule {

    @Exclude // Document ID is auto-generated in Firestore, used locally.
    private String id;

    // Links to other collections
    private String courseCode; // Links to courses/{courseCode}
    private String facultyId;  // Links to users/{UID}

    // Scheduling details
    private List<String> daysOfWeek; // e.g., ["MON", "TUE", "THU"]
    private String startTime;      // e.g., "10:00"
    private String venue;          // e.g., "Room 501"
    private String type;           // e.g., "lecture", "lab"

    // Duration of the schedule block
    private Timestamp startDate;    // Start of the recurring block (semester start)
    private Timestamp endDate;      // End of the recurring block (semester end)

    // Required No-Argument Constructor
    public Schedule() {}

    // --- Getters and Setters (Required for Firestore) ---

    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getFacultyId() { return facultyId; }
    public void setFacultyId(String facultyId) { this.facultyId = facultyId; }

    public List<String> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(List<String> daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Timestamp getStartDate() { return startDate; }
    public void setStartDate(Timestamp startDate) { this.startDate = startDate; }

    public Timestamp getEndDate() { return endDate; }
    public void setEndDate(Timestamp endDate) { this.endDate = endDate; }
}