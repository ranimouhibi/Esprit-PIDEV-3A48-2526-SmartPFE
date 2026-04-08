package org.example.model;

import java.time.LocalDateTime;

public class Candidature {
    private int id;
    private int offerId;
    private String offerTitle;
    private int studentId;
    private String studentName;
    private String motivationLetter;
    private String cvPath;
    private String status; // pending, accepted, rejected
    private String feedback;
    private LocalDateTime createdAt;

    public Candidature() {}

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOfferId() { return offerId; }
    public void setOfferId(int offerId) { this.offerId = offerId; }

    public String getOfferTitle() { return offerTitle; }
    public void setOfferTitle(String offerTitle) { this.offerTitle = offerTitle; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getMotivationLetter() { return motivationLetter; }
    public void setMotivationLetter(String motivationLetter) { this.motivationLetter = motivationLetter; }

    public String getCvPath() { return cvPath; }
    public void setCvPath(String cvPath) { this.cvPath = cvPath; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return studentName + " → " + offerTitle; }
}
