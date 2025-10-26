package com.example.acadease.model;

public class ScheduleItem {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_SESSION = 1;

    public int type;
    public String headerDate;   // Used if TYPE_HEADER (e.g., "Sep 22, 2025 Monday")
    public Session session;     // Used if TYPE_SESSION

    // Constructor for a Date Header
    public ScheduleItem(String headerDate) {
        this.type = TYPE_HEADER;
        this.headerDate = headerDate;
    }

    // Constructor for a Class Session
    public ScheduleItem(Session session) {
        this.type = TYPE_SESSION;
        this.session = session;
    }
}