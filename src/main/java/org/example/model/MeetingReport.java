package org.example.model;

import java.time.LocalDateTime;

public class MeetingReport {
    private int id;
    private int meetingId;
    private int createdById;
    private String createdByName;
    private String projectTitle;
    private String meetingType;
    private LocalDateTime meetingDate;
    private String discussionPoints;
    private String decisions;
    private String actionItems;
    private String nextSteps;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String rawMeetingText;

    public MeetingReport() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMeetingId() { return meetingId; }
    public void setMeetingId(int meetingId) { this.meetingId = meetingId; }

    public int getCreatedById() { return createdById; }
    public void setCreatedById(int createdById) { this.createdById = createdById; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }

    public String getMeetingType() { return meetingType; }
    public void setMeetingType(String meetingType) { this.meetingType = meetingType; }

    public LocalDateTime getMeetingDate() { return meetingDate; }
    public void setMeetingDate(LocalDateTime meetingDate) { this.meetingDate = meetingDate; }

    public String getDiscussionPoints() { return discussionPoints; }
    public void setDiscussionPoints(String discussionPoints) { this.discussionPoints = discussionPoints; }

    public String getDecisions() { return decisions; }
    public void setDecisions(String decisions) { this.decisions = decisions; }

    public String getActionItems() { return actionItems; }
    public void setActionItems(String actionItems) { this.actionItems = actionItems; }

    public String getNextSteps() { return nextSteps; }
    public void setNextSteps(String nextSteps) { this.nextSteps = nextSteps; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getRawMeetingText() { return rawMeetingText; }
    public void setRawMeetingText(String rawMeetingText) { this.rawMeetingText = rawMeetingText; }
}
