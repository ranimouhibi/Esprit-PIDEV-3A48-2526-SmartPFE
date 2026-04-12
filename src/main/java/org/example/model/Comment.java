package org.example.model;

import java.time.LocalDateTime;

public class Comment {
    private int id;
    private int authorId;
    private String commentableType;
    private int commentableId;
    private String content;
    private LocalDateTime createdAt;
    private String subject;
    private String commentType;
    private String target;
    private String importance;
    private String reference;
    private Integer parentId;
    private String sentiment;
    
    // Display fields
    private String authorName;
    private String projectTitle;

    public Comment() {}

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }

    public String getCommentableType() { return commentableType; }
    public void setCommentableType(String commentableType) { this.commentableType = commentableType; }

    public int getCommentableId() { return commentableId; }
    public void setCommentableId(int commentableId) { this.commentableId = commentableId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getCommentType() { return commentType; }
    public void setCommentType(String commentType) { this.commentType = commentType; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getImportance() { return importance; }
    public void setImportance(String importance) { this.importance = importance; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }

    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }
}
