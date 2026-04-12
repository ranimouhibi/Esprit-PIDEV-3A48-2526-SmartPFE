package org.example.model;

import java.time.LocalDateTime;

public class Project {
    private int id;
    private int ownerId;
    private int supervisorId;
    private int establishmentId;
    private String title;
    private String description;
    private String projectType;
    private String joinCode;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String aiSuggestions;
    
    // Noms pour affichage
    private String ownerName;
    private String supervisorName;

    public Project() {}

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    public int getSupervisorId() { return supervisorId; }
    public void setSupervisorId(int supervisorId) { this.supervisorId = supervisorId; }

    public int getEstablishmentId() { return establishmentId; }
    public void setEstablishmentId(int establishmentId) { this.establishmentId = establishmentId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getProjectType() { return projectType; }
    public void setProjectType(String projectType) { this.projectType = projectType; }

    public String getJoinCode() { return joinCode; }
    public void setJoinCode(String joinCode) { this.joinCode = joinCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getAiSuggestions() { return aiSuggestions; }
    public void setAiSuggestions(String aiSuggestions) { this.aiSuggestions = aiSuggestions; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getSupervisorName() { return supervisorName; }
    public void setSupervisorName(String supervisorName) { this.supervisorName = supervisorName; }

    @Override
    public String toString() { return title; }
}
