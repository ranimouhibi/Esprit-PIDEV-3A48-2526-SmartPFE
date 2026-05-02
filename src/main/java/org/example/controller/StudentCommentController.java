package org.example.controller;

import org.example.dao.CommentDAO;
import org.example.dao.ProjectDAO;
import org.example.model.Comment;
import org.example.model.Project;
import org.example.model.User;
import org.example.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import org.example.util.ModernAlert;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class StudentCommentController implements Initializable {

    @FXML private ComboBox<Project> projectFilter;
    @FXML private FlowPane commentsContainer;

    private final CommentDAO commentDAO = new CommentDAO();
    private final ProjectDAO projectDAO = new ProjectDAO();
    private List<Comment> allComments;
    private List<Project> userProjects;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadUserProjects();
        loadComments();
    }

    private void loadUserProjects() {
        try {
            User user = SessionManager.getCurrentUser();
            userProjects = projectDAO.findByUserProjects(user.getId());
            projectFilter.setItems(FXCollections.observableArrayList(userProjects));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadComments() {
        try {
            User user = SessionManager.getCurrentUser();
            allComments = commentDAO.findByAuthor(user.getId());
            displayComments(allComments);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load comments: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void displayComments(List<Comment> comments) {
        commentsContainer.getChildren().clear();
        
        if (comments.isEmpty()) {
            VBox emptyState = createEmptyState();
            commentsContainer.getChildren().add(emptyState);
            return;
        }

        for (Comment comment : comments) {
            VBox card = createCommentCard(comment);
            commentsContainer.getChildren().add(card);
        }
    }

    private VBox createEmptyState() {
        VBox empty = new VBox(12);
        empty.setAlignment(javafx.geometry.Pos.CENTER);
        empty.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e0e0e0; -fx-border-radius: 12; -fx-border-width: 2; -fx-border-style: dashed;");
        empty.setPrefSize(400, 200);
        empty.setPadding(new Insets(30));
        
        Label icon = new Label("[EMPTY]");
        icon.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #999;");
        
        Label title = new Label("No comments yet");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #666;");
        
        Label subtitle = new Label("Click 'New Comment' to add your first comment");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #999;");
        
        empty.getChildren().addAll(icon, title, subtitle);
        return empty;
    }

    private VBox createCommentCard(Comment comment) {
        VBox card = new VBox(14);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 3); -fx-border-color: #f0f0f0; -fx-border-radius: 14; -fx-border-width: 1;");
        card.setPrefWidth(340);
        card.setPadding(new Insets(20));

        // Header with Type and Importance
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label typeLabel = new Label(comment.getCommentType() != null ? comment.getCommentType().toUpperCase() : "COMMENT");
        typeLabel.setStyle("-fx-background-color: #3b82f622; -fx-text-fill: #3b82f6; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 4 10;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label importanceLabel = new Label(comment.getImportance() != null ? comment.getImportance().toUpperCase() : "");
        String importanceColor = getImportanceColor(comment.getImportance());
        importanceLabel.setStyle("-fx-background-color: " + importanceColor + "22; -fx-text-fill: " + importanceColor + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 4 10;");
        
        Label dateLabel = new Label(comment.getCreatedAt() != null 
            ? comment.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) 
            : "");
        dateLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
        
        header.getChildren().addAll(typeLabel, spacer, importanceLabel, dateLabel);

        // Subject
        if (comment.getSubject() != null && !comment.getSubject().isEmpty()) {
            Label subject = new Label(comment.getSubject());
            subject.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #1e1e1e;");
            subject.setWrapText(true);
            subject.setMaxWidth(300);
            card.getChildren().add(subject);
        }

        // Content
        Label content = new Label(comment.getContent() != null ? comment.getContent() : "No content");
        content.setStyle("-fx-text-fill: #666; -fx-font-size: 13px;");
        content.setWrapText(true);
        content.setMaxWidth(300);
        content.setMaxHeight(100);

        // Target
        if (comment.getTarget() != null && !comment.getTarget().isEmpty()) {
            HBox targetBox = new HBox(6);
            targetBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label targetIcon = new Label("[T]");
            targetIcon.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #8b5cf6;");
            Label targetLabel = new Label("Target: " + comment.getTarget());
            targetLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
            targetBox.getChildren().addAll(targetIcon, targetLabel);
            card.getChildren().add(targetBox);
        }

        // Project
        HBox projectBox = new HBox(6);
        projectBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label projectIcon = new Label("[P]");
        projectIcon.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #a12c2f;");
        Label projectLabel = new Label("Project: " + (comment.getProjectTitle() != null ? comment.getProjectTitle() : "N/A"));
        projectLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        projectBox.getChildren().addAll(projectIcon, projectLabel);

        Separator sep = new Separator();

        // Actions
        HBox actions = new HBox(8);
        actions.setAlignment(javafx.geometry.Pos.CENTER);
        
        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 6; -fx-padding: 6 14; -fx-cursor: hand;");
        editBtn.setOnAction(e -> handleEditComment(comment));
        
        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 6; -fx-padding: 6 14; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> handleDeleteComment(comment));
        
        actions.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(header, content, projectBox, sep, actions);
        return card;
    }

    private String getImportanceColor(String importance) {
        if (importance == null) return "#888888";
        String imp = importance.toLowerCase();
        if (imp.equals("urgent")) {
            return "#ef4444";
        } else if (imp.equals("medium")) {
            return "#f59e0b";
        } else {
            return "#10b981";
        }
    }

    @FXML
    public void handleAddComment() {
        openCommentDialog(null);
    }

    private void handleEditComment(Comment comment) {
        openCommentDialog(comment);
    }

    private void openCommentDialog(Comment comment) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CommentFormDialog.fxml"));
            VBox dialogContent = loader.load();
            
            CommentFormDialogController controller = loader.getController();
            
            // IMPORTANT: Set comment FIRST, then projects (which will trigger populate)
            controller.setComment(comment);
            controller.setUserProjects(userProjects);
            
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(StageStyle.UNDECORATED);
            dialog.setScene(new Scene(dialogContent, 520, 720));
            
            controller.setDialogStage(dialog);
            controller.setOnSaveCallback(this::loadComments);
            
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open dialog: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void handleFilter() {
        if (projectFilter.getValue() == null) {
            displayComments(allComments);
            return;
        }
        
        int projectId = projectFilter.getValue().getId();
        List<Comment> filtered = new ArrayList<>();
        for (Comment c : allComments) {
            if (c.getCommentableId() == projectId) {
                filtered.add(c);
            }
        }
        
        displayComments(filtered);
    }

    @FXML
    public void handleReset() {
        projectFilter.setValue(null);
        displayComments(allComments);
    }

    private void handleDeleteComment(Comment comment) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Comment");
        confirm.setHeaderText("Delete this comment?");
        confirm.setContentText("This action cannot be undone.");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                commentDAO.delete(comment.getId());
                showAlert("Success", "Comment deleted successfully", Alert.AlertType.INFORMATION);
                loadComments();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to delete comment: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
