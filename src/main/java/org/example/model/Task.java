package org.example.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Task {
    private int id;
    private int projectId;
    private String projectTitle;
    private Integer sprintId;
    private String sprintName;
    private String title;
    private String description;
    private String status; // todo, in_progress, done
    private String priority; // low, medium, high, critical
    private int storyPoints;
    private Integer assignedToId;
    private String assignedToName;
    private boolean isBlocked;
    private String blockerDescription;
    private LocalDate deadline;
    private LocalDateTime createdAt;

    public Task() {}

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }

    public Integer getSprintId() { return sprintId; }
    public void setSprintId(Integer sprintId) { this.sprintId = sprintId; }

    public String getSprintName() { return sprintName; }
    public void setSprintName(String sprintName) { this.sprintName = sprintName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public int getStoryPoints() { return storyPoints; }
    public void setStoryPoints(int storyPoints) { this.storyPoints = storyPoints; }

    public Integer getAssignedToId() { return assignedToId; }
    public void setAssignedToId(Integer assignedToId) { this.assignedToId = assignedToId; }

    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }

    public boolean isBlocked() { return isBlocked; }
    public void setBlocked(boolean blocked) { isBlocked = blocked; }

    public String getBlockerDescription() { return blockerDescription; }
    public void setBlockerDescription(String blockerDescription) { this.blockerDescription = blockerDescription; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return title; }
}
