package org.example.controller;

import org.example.dao.CommentDAO;
import org.example.model.Comment;
import org.example.util.PDFExporter;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import org.example.util.ModernAlert;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class CommentController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> filterImportance;
    @FXML private ComboBox<String> sortCombo;
    @FXML private FlowPane commentsContainer;
    
    // Stats
    @FXML private Label statTotal;
    @FXML private Label statUrgent;
    @FXML private Label statMedium;
    @FXML private Label statLow;

    private final CommentDAO commentDAO = new CommentDAO();
    private List<Comment> allComments;
    private List<Comment> filteredComments;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filterType.setItems(FXCollections.observableArrayList("All Types", "correction", "suggestion", "validation", "question"));
        filterType.setValue("All Types");
        
        filterImportance.setItems(FXCollections.observableArrayList("All", "urgent", "medium", "low"));
        filterImportance.setValue("All");
        
        sortCombo.setItems(FXCollections.observableArrayList(
            "Date: Newest first", "Date: Oldest first", 
            "Importance: High to Low", "Importance: Low to High",
            "Type: A to Z", "Type: Z to A"
        ));
        sortCombo.setValue("Date: Newest first");
        
        // Live listeners
        searchField.textProperty().addListener((obs, old, val) -> applyFilters());
        filterType.valueProperty().addListener((obs, old, val) -> applyFilters());
        filterImportance.valueProperty().addListener((obs, old, val) -> applyFilters());
        sortCombo.valueProperty().addListener((obs, old, val) -> applyFilters());
        
        loadComments();
    }

    private void loadComments() {
        try {
            allComments = commentDAO.findAll();
            updateStats();
            applyFilters();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load comments: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void updateStats() {
        int total = allComments.size();
        long urgent = allComments.stream().filter(c -> "urgent".equals(c.getImportance())).count();
        long medium = allComments.stream().filter(c -> "medium".equals(c.getImportance())).count();
        long low = allComments.stream().filter(c -> "low".equals(c.getImportance())).count();

        statTotal.setText(String.valueOf(total));
        statUrgent.setText(String.valueOf(urgent));
        statMedium.setText(String.valueOf(medium));
        statLow.setText(String.valueOf(low));
    }

    private void applyFilters() {
        String query = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String typeFilter = filterType.getValue();
        String importanceFilter = filterImportance.getValue();
        String sort = sortCombo.getValue();
        
        filteredComments = new ArrayList<>();
        for (Comment c : allComments) {
            boolean matchQuery = query.isEmpty() || 
                (c.getContent() != null && c.getContent().toLowerCase().contains(query)) ||
                (c.getSubject() != null && c.getSubject().toLowerCase().contains(query));
            boolean matchType = typeFilter.equals("All Types") || 
                (c.getCommentType() != null && c.getCommentType().equals(typeFilter));
            boolean matchImportance = importanceFilter.equals("All") || 
                (c.getImportance() != null && c.getImportance().equals(importanceFilter));
            
            if (matchQuery && matchType && matchImportance) {
                filteredComments.add(c);
            }
        }
        
        // Sort
        if (sort != null) {
            switch (sort) {
                case "Date: Oldest first":
                    filteredComments.sort((a, b) -> {
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return a.getCreatedAt().compareTo(b.getCreatedAt());
                    });
                    break;
                case "Importance: High to Low":
                    filteredComments.sort((a, b) -> importanceOrder(a.getImportance()) - importanceOrder(b.getImportance()));
                    break;
                case "Importance: Low to High":
                    filteredComments.sort((a, b) -> importanceOrder(b.getImportance()) - importanceOrder(a.getImportance()));
                    break;
                case "Type: A to Z":
                    filteredComments.sort((a, b) -> {
                        String ta = a.getCommentType() != null ? a.getCommentType() : "";
                        String tb = b.getCommentType() != null ? b.getCommentType() : "";
                        return ta.compareTo(tb);
                    });
                    break;
                case "Type: Z to A":
                    filteredComments.sort((a, b) -> {
                        String ta = a.getCommentType() != null ? a.getCommentType() : "";
                        String tb = b.getCommentType() != null ? b.getCommentType() : "";
                        return tb.compareTo(ta);
                    });
                    break;
                default: // Date: Newest first
                    filteredComments.sort((a, b) -> {
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });
                    break;
            }
        }
        
        displayComments(filteredComments);
    }

    private int importanceOrder(String imp) {
        if ("urgent".equals(imp)) return 0;
        if ("medium".equals(imp)) return 1;
        return 2;
    }

    @FXML public void handleSearch() { applyFilters(); }

    @FXML
    public void handleReset() {
        searchField.clear();
        filterType.setValue("All Types");
        filterImportance.setValue("All");
        sortCombo.setValue("Date: Newest first");
    }

    @FXML
    public void handleExportPDF() {
        if (filteredComments == null || filteredComments.isEmpty()) {
            showAlert("Warning", "No comments to export", Alert.AlertType.WARNING);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF Report");
        fileChooser.setInitialFileName("comments_report.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        
        File file = fileChooser.showSaveDialog(commentsContainer.getScene().getWindow());
        if (file != null) {
            try {
                PDFExporter.exportComments(filteredComments, file);
                showAlert("Success", "PDF exported successfully!", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to export PDF: " + e.getMessage(), Alert.AlertType.ERROR);
            }
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
        
        Label title = new Label("No comments found");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #666;");
        
        Label subtitle = new Label("No comments match your search criteria");
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
        content.setMaxHeight(80);

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

        // Author
        HBox authorBox = new HBox(6);
        authorBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label authorIcon = new Label("[A]");
        authorIcon.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #667eea;");
        Label authorLabel = new Label("By: " + (comment.getAuthorName() != null ? comment.getAuthorName() : "Unknown"));
        authorLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        authorBox.getChildren().addAll(authorIcon, authorLabel);

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
        
        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 6; -fx-padding: 6 14; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> handleDeleteComment(comment));
        
        actions.getChildren().add(deleteBtn);

        card.getChildren().addAll(header, content, authorBox, projectBox, sep, actions);
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
        ModernAlert.Type mType = (type == Alert.AlertType.ERROR) ? ModernAlert.Type.ERROR :
                                 (type == Alert.AlertType.WARNING) ? ModernAlert.Type.WARNING :
                                 ModernAlert.Type.INFO;
        ModernAlert.show(mType, title, message);
    }
}
