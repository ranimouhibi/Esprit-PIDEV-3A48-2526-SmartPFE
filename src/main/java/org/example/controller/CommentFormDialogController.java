package org.example.controller;

import org.example.dao.CommentDAO;
import org.example.model.Comment;
import org.example.model.Project;
import org.example.model.User;
import org.example.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class CommentFormDialogController implements Initializable {

    @FXML private Label dialogTitle;
    @FXML private ComboBox<Project> projectCombo;
    @FXML private TextField subjectField;
    @FXML private TextArea contentField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> targetCombo;
    @FXML private ComboBox<String> importanceCombo;
    @FXML private Button saveButton;
    @FXML private Label projectError;
    @FXML private Label subjectError;
    @FXML private Label contentError;
    @FXML private Label typeError;
    @FXML private Label targetError;
    @FXML private Label importanceError;
    @FXML private Label errorLabel;

    private Stage dialogStage;
    private Comment comment;
    private List<Project> userProjects;
    private Runnable onSaveCallback;
    private final CommentDAO commentDAO = new CommentDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialize combos
        typeCombo.setItems(FXCollections.observableArrayList(
            "correction", "suggestion", "validation", "question"
        ));
        
        targetCombo.setItems(FXCollections.observableArrayList(
            "code", "design", "database", "other"
        ));
        
        importanceCombo.setItems(FXCollections.observableArrayList(
            "low", "medium", "urgent"
        ));

        // Add input validation listeners
        setupValidation();
    }

    private void setupValidation() {
        // Input restrictions only - no validation on change
        subjectField.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() > 255) {
                subjectField.setText(old);
            }
            // Allow only letters, numbers, spaces, punctuation, French accents
            if (val != null && !val.isEmpty() && !val.matches("[a-zA-Z0-9\\s\\p{Punct}\u00C0-\u017F]*")) {
                subjectField.setText(old);
            }
        });

        contentField.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() > 1000) {
                contentField.setText(old);
            }
            // Allow only letters, numbers, spaces, punctuation, French accents, newlines
            if (val != null && !val.isEmpty() && !val.matches("[a-zA-Z0-9\\s\\p{Punct}\u00C0-\u017F\\n\\r]*")) {
                contentField.setText(old);
            }
        });
    }

    private boolean validateProject() {
        if (projectCombo.getValue() == null) {
            projectError.setText("Please select a project");
            return false;
        }
        projectError.setText("");
        return true;
    }

    private boolean validateSubject() {
        String subject = subjectField.getText();
        if (subject == null || subject.trim().isEmpty()) {
            subjectError.setText("Subject is required");
            return false;
        }
        if (subject.length() < 3) {
            subjectError.setText("Subject must be at least 3 characters");
            return false;
        }
        if (subject.length() > 255) {
            subjectError.setText("Subject must be less than 255 characters");
            return false;
        }
        subjectError.setText("");
        return true;
    }

    private boolean validateContent() {
        String content = contentField.getText();
        if (content == null || content.trim().isEmpty()) {
            contentError.setText("Comment is required");
            return false;
        }
        if (content.length() < 5) {
            contentError.setText("Comment must be at least 5 characters");
            return false;
        }
        if (content.length() > 1000) {
            contentError.setText("Comment must be less than 1000 characters");
            return false;
        }
        contentError.setText("");
        return true;
    }

    private boolean validateType() {
        if (typeCombo.getValue() == null) {
            typeError.setText("Please select a comment type");
            return false;
        }
        typeError.setText("");
        return true;
    }

    private boolean validateTarget() {
        if (targetCombo.getValue() == null) {
            targetError.setText("Please select a target");
            return false;
        }
        targetError.setText("");
        return true;
    }

    private boolean validateImportance() {
        if (importanceCombo.getValue() == null) {
            importanceError.setText("Please select importance level");
            return false;
        }
        importanceError.setText("");
        return true;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
        System.out.println("=== setComment called ===");
        if (comment != null) {
            System.out.println("Comment ID: " + comment.getId());
            System.out.println("Subject: " + comment.getSubject());
            System.out.println("Content: " + comment.getContent());
            System.out.println("Type: " + comment.getCommentType());
            System.out.println("Target: " + comment.getTarget());
            System.out.println("Importance: " + comment.getImportance());
            System.out.println("Commentable ID: " + comment.getCommentableId());
            dialogTitle.setText("Edit Comment");
            saveButton.setText("Update Comment");
        }
    }

    public void setUserProjects(List<Project> projects) {
        this.userProjects = projects;
        System.out.println("=== setUserProjects called ===");
        System.out.println("Projects count: " + (projects != null ? projects.size() : 0));
        if (projects != null && !projects.isEmpty()) {
            projectCombo.setItems(FXCollections.observableArrayList(projects));
            System.out.println("Projects loaded in combo");
            // Now populate if we're editing
            if (comment != null) {
                System.out.println("Calling populateFields...");
                populateFields();
            } else {
                System.out.println("Comment is null, skipping populate");
            }
        }
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    private void populateFields() {
        System.out.println("=== populateFields called ===");
        if (comment != null && userProjects != null) {
            System.out.println("Comment and projects are not null");
            
            // Find and select project
            Project selectedProject = null;
            for (Project p : userProjects) {
                if (p.getId() == comment.getCommentableId()) {
                    selectedProject = p;
                    System.out.println("Found matching project: " + p.getTitle());
                    break;
                }
            }
            if (selectedProject != null) {
                projectCombo.setValue(selectedProject);
                projectCombo.setStyle("-fx-border-color: #10b981; -fx-border-width: 2; -fx-border-radius: 8;");
                System.out.println("Project set in combo");
            } else {
                System.out.println("No matching project found!");
            }

            // Set subject
            if (comment.getSubject() != null) {
                System.out.println("Setting subject: " + comment.getSubject());
                subjectField.setText(comment.getSubject());
                subjectField.setStyle("-fx-border-color: #10b981; -fx-border-width: 2; -fx-border-radius: 8;");
            }
            
            // Set content
            if (comment.getContent() != null) {
                System.out.println("Setting content: " + comment.getContent());
                contentField.setText(comment.getContent());
                contentField.setStyle("-fx-border-color: #10b981; -fx-border-width: 2; -fx-border-radius: 8;");
            }
            
            // Set comment type (with default)
            if (comment.getCommentType() != null) {
                System.out.println("Setting type: " + comment.getCommentType());
                typeCombo.setValue(comment.getCommentType());
            } else {
                System.out.println("Type is null, setting default: question");
                typeCombo.setValue("question");
            }
            typeCombo.setStyle("-fx-border-color: #10b981; -fx-border-width: 2; -fx-border-radius: 8;");
            
            // Set target (with default)
            if (comment.getTarget() != null) {
                System.out.println("Setting target: " + comment.getTarget());
                targetCombo.setValue(comment.getTarget());
            } else {
                System.out.println("Target is null, setting default: other");
                targetCombo.setValue("other");
            }
            targetCombo.setStyle("-fx-border-color: #10b981; -fx-border-width: 2; -fx-border-radius: 8;");
            
            // Set importance (with default)
            if (comment.getImportance() != null) {
                System.out.println("Setting importance: " + comment.getImportance());
                importanceCombo.setValue(comment.getImportance());
            } else {
                System.out.println("Importance is null, setting default: medium");
                importanceCombo.setValue("medium");
            }
            importanceCombo.setStyle("-fx-border-color: #10b981; -fx-border-width: 2; -fx-border-radius: 8;");
            
            System.out.println("=== populateFields completed ===");
        } else {
            System.out.println("ERROR: comment or userProjects is null!");
            System.out.println("comment: " + comment);
            System.out.println("userProjects: " + userProjects);
        }
    }

    @FXML
    public void handleSave() {
        System.out.println("=== handleSave called ===");
        
        // Validate all fields
        boolean valid = true;
        valid = validateProject() && valid;
        System.out.println("Project valid: " + valid);
        valid = validateSubject() && valid;
        System.out.println("Subject valid: " + valid);
        valid = validateContent() && valid;
        System.out.println("Content valid: " + valid);
        valid = validateType() && valid;
        System.out.println("Type valid: " + valid);
        valid = validateTarget() && valid;
        System.out.println("Target valid: " + valid);
        valid = validateImportance() && valid;
        System.out.println("Importance valid: " + valid);

        if (!valid) {
            System.out.println("Validation failed!");
            errorLabel.setText("Please fix the errors above");
            errorLabel.setVisible(true);
            return;
        }

        try {
            System.out.println("Saving comment...");
            User user = SessionManager.getCurrentUser();
            
            if (comment == null) {
                // Create new comment
                comment = new Comment();
                comment.setAuthorId(user.getId());
                comment.setCommentableType("project");
                System.out.println("Creating new comment");
            } else {
                System.out.println("Updating existing comment ID: " + comment.getId());
            }

            // Update fields
            comment.setCommentableId(projectCombo.getValue().getId());
            comment.setSubject(subjectField.getText().trim());
            comment.setContent(contentField.getText().trim());
            comment.setCommentType(typeCombo.getValue());
            comment.setTarget(targetCombo.getValue());
            comment.setImportance(importanceCombo.getValue());

            // Save to database
            if (comment.getId() == 0) {
                commentDAO.save(comment);
                System.out.println("Comment saved with new ID: " + comment.getId());
            } else {
                commentDAO.update(comment);
                System.out.println("Comment updated successfully");
            }

            // Callback and close
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            System.out.println("Closing dialog");
            dialogStage.close();

        } catch (Exception e) {
            System.out.println("ERROR saving comment: " + e.getMessage());
            e.printStackTrace();
            errorLabel.setText("Failed to save comment: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    @FXML
    public void handleCancel() {
        dialogStage.close();
    }
}
