package org.example.model;

import java.time.LocalDateTime;

public class Project {
    private int id;
    private String title;
    private String description;
    private String projectType;
    private String status;
    private String joinCode;
    private int ownerId;
    private String ownerName;
    private int supervisorId;
    private String supervisorName;
    private LocalDateTime createdAt;

    public Project() {}

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getProjectType() { return projectType; }
    public void setProjectType(String projectType) { this.projectType = projectType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getJoinCode() { return joinCode; }
    public void setJoinCode(String joinCode) { this.joinCode = joinCode; }

    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public int getSupervisorId() { return supervisorId; }
    public void setSupervisorId(int supervisorId) { this.supervisorId = supervisorId; }

    public String getSupervisorName() { return supervisorName; }
    public void setSupervisorName(String supervisorName) { this.supervisorName = supervisorName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return title; }
}
