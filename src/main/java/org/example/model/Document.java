package org.example.model;

import java.time.LocalDateTime;

public class Document {
    private int id;
    private int projectId;
    private int uploadedById;
    private String filename;
    private String filePath;
    private String fileType;
    private String category;
    private String description;
    private int version;
    private LocalDateTime uploadedAt;

    // Display fields
    private String projectTitle;
    private String uploaderName;

    public Document() { this.version = 1; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public int getUploadedById() { return uploadedById; }
    public void setUploadedById(int uploadedById) { this.uploadedById = uploadedById; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }

    public String getUploaderName() { return uploaderName; }
    public void setUploaderName(String uploaderName) { this.uploaderName = uploaderName; }

    @Override
    public String toString() { return filename; }
}
