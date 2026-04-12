package org.example.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Offer {
    private int id;
    private int establishmentId;
    private String establishmentName;
    private int assignedSupervisorId;
    private String title;
    private String description;
    private String objectives;
    private String requiredSkills;
    private int maxCandidates;
    private LocalDate deadline;
    private String status; // open, closed, draft
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Offer() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEstablishmentId() { return establishmentId; }
    public void setEstablishmentId(int establishmentId) { this.establishmentId = establishmentId; }

    public String getEstablishmentName() { return establishmentName; }
    public void setEstablishmentName(String establishmentName) { this.establishmentName = establishmentName; }

    public int getAssignedSupervisorId() { return assignedSupervisorId; }
    public void setAssignedSupervisorId(int assignedSupervisorId) { this.assignedSupervisorId = assignedSupervisorId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getObjectives() { return objectives; }
    public void setObjectives(String objectives) { this.objectives = objectives; }

    public String getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(String requiredSkills) { this.requiredSkills = requiredSkills; }

    public int getMaxCandidates() { return maxCandidates; }
    public void setMaxCandidates(int maxCandidates) { this.maxCandidates = maxCandidates; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() { return title; }
}
