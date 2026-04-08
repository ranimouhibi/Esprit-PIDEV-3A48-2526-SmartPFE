package org.example.model;

import java.time.LocalDateTime;

public class Meeting {
    private int id;
    private int projectId;
    private String projectTitle;
    private String title;
    private String description;
    private String meetingType;
    private String location;
    private String meetingLink;
    private String status;
    private LocalDateTime scheduledDate;
    private LocalDateTime createdAt;

    public Meeting() {}

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMeetingType() { return meetingType; }
    public void setMeetingType(String meetingType) { this.meetingType = meetingType; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDateTime scheduledDate) { this.scheduledDate = scheduledDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return title; }
}
