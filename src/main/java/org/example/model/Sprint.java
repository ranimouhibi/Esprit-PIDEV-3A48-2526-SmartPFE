package org.example.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Sprint {
    private int id;
    private int projectId;
    private String projectTitle;
    private String name;
    private String goal;
    private int sprintNumber;
    private String status; // planned, active, closed
    private LocalDate startDate;
    private LocalDate endDate;
    private String retrospective;
    private LocalDateTime createdAt;

    public Sprint() {}

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public int getSprintNumber() { return sprintNumber; }
    public void setSprintNumber(int sprintNumber) { this.sprintNumber = sprintNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getRetrospective() { return retrospective; }
    public void setRetrospective(String retrospective) { this.retrospective = retrospective; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return "Sprint #" + sprintNumber + ": " + name; }
}
