package org.example.model;

import java.time.LocalDateTime;

public class CandidatureNote {
    private int id;
    private int candidatureId;
    private int authorId;
    private String authorName;
    private String content;
    private int rating; // 1-5
    private boolean isPrivate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CandidatureNote() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCandidatureId() { return candidatureId; }
    public void setCandidatureId(int candidatureId) { this.candidatureId = candidatureId; }

    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean aPrivate) { isPrivate = aPrivate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getStars() {
        return "★".repeat(rating) + "☆".repeat(5 - rating);
    }
}
