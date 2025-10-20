package com.example.acadease.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.Timestamp;
import java.util.List;

public class Announcement {

    @Exclude
    private String id; // Critical for deletion/editing

    private String title;
    private String body;
    private String imgUrl;
    private String postedBy;
    private List<String> targetRole;
    private String category;
    private Timestamp createdAt;

    public Announcement() {}

    // --- Getters and Setters ---

    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getImgUrl() { return imgUrl; }
    public void setImgUrl(String imgUrl) { this.imgUrl = imgUrl; }

    public String getPostedBy() { return postedBy; }
    public void setPostedBy(String postedBy) { this.postedBy = postedBy; }

    public List<String> getTargetRole() { return targetRole; }
    public void setTargetRole(List<String> targetRole) { this.targetRole = targetRole; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}