package org.example.controller;

import org.example.dao.DocumentDAO;
import org.example.model.Document;
import org.example.model.Project;
import org.example.model.User;
import org.example.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ProjectDocumentsDialogController implements Initializable {

    @FXML private Label projectNameLabel;
    @FXML private Button uploadBtn;
    @FXML private VBox uploadFormContainer;
    @FXML private TextField filePathField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextField descriptionField;
    @FXML private Label fileError;
    @FXML private Label categoryError;
    @FXML private ComboBox<String> filterCategory;
    @FXML private Label countLabel;
    @FXML private VBox docsContainer;

    private Stage dialogStage;
    private Project project;
    private boolean canUpload = false; // true for student, false for supervisor (read-only)
    private final DocumentDAO documentDAO = new DocumentDAO();
    private List<Document> allDocs = new ArrayList<>();
    private File selectedFile = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        categoryCombo.setItems(FXCollections.observableArrayList(
            "proposal", "report", "presentation", "code", "other"
        ));

        filterCategory.setItems(FXCollections.observableArrayList(
            "All Categories", "proposal", "report", "presentation", "code", "other"
        ));
        filterCategory.setValue("All Categories");
        filterCategory.valueProperty().addListener((obs, old, val) -> applyFilter());
    }

    public void setDialogStage(Stage stage) { this.dialogStage = stage; }

    public void setProject(Project project, boolean canUpload) {
        this.project = project;
        this.canUpload = canUpload;
        projectNameLabel.setText(project.getTitle());

        // Hide upload button for supervisors (read-only)
        if (!canUpload) {
            uploadBtn.setVisible(false);
            uploadBtn.setManaged(false);
        }

        loadDocuments();
    }

    private void loadDocuments() {
        try {
            allDocs = documentDAO.findByProject(project.getId());
            applyFilter();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyFilter() {
        String cat = filterCategory.getValue();
        List<Document> filtered = new ArrayList<>();
        for (Document d : allDocs) {
            if (cat == null || cat.equals("All Categories") || cat.equals(d.getCategory())) {
                filtered.add(d);
            }
        }
        countLabel.setText(filtered.size() + " document(s)");
        displayDocuments(filtered);
    }

    @FXML public void handleReset() {
        filterCategory.setValue("All Categories");
    }

    private void displayDocuments(List<Document> docs) {
        docsContainer.getChildren().clear();

        if (docs.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(javafx.geometry.Pos.CENTER);
            empty.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e0e0e0; -fx-border-radius: 12; -fx-border-width: 2; -fx-border-style: dashed;");
            empty.setPrefHeight(120);
            empty.setPadding(new Insets(24));
            Label lbl = new Label("No documents uploaded yet");
            lbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #999;");
            empty.getChildren().add(lbl);
            docsContainer.getChildren().add(empty);
            return;
        }

        for (Document doc : docs) {
            docsContainer.getChildren().add(createDocCard(doc));
        }
    }

    private HBox createDocCard(Document doc) {
        HBox card = new HBox(14);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2); -fx-border-color: #f0f0f0; -fx-border-radius: 10; -fx-border-width: 1;");
        card.setPadding(new Insets(14));

        // File icon based on type
        Label icon = new Label(getFileIcon(doc.getFileType()));
        icon.setStyle("-fx-font-size: 28px; -fx-min-width: 40;");

        // Info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLabel = new Label(doc.getFilename());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e1e1e;");
        nameLabel.setWrapText(true);

        HBox meta = new HBox(10);
        meta.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        if (doc.getCategory() != null) {
            Label catLabel = new Label(doc.getCategory().toUpperCase());
            catLabel.setStyle("-fx-background-color: #a12c2f22; -fx-text-fill: #a12c2f; -fx-font-size: 9px; -fx-font-weight: bold; -fx-background-radius: 4; -fx-padding: 2 7;");
            meta.getChildren().add(catLabel);
        }

        Label uploaderLabel = new Label("By: " + (doc.getUploaderName() != null ? doc.getUploaderName() : "Unknown"));
        uploaderLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");

        Label dateLabel = new Label(doc.getUploadedAt() != null
            ? doc.getUploadedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
        dateLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10px;");

        meta.getChildren().addAll(uploaderLabel, dateLabel);

        if (doc.getDescription() != null && !doc.getDescription().isEmpty()) {
            Label desc = new Label(doc.getDescription());
            desc.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
            desc.setWrapText(true);
            info.getChildren().addAll(nameLabel, meta, desc);
        } else {
            info.getChildren().addAll(nameLabel, meta);
        }

        card.getChildren().addAll(icon, info);

        // Delete button only for uploader
        User currentUser = SessionManager.getCurrentUser();
        if (canUpload && currentUser != null && currentUser.getId() == doc.getUploadedById()) {
            Button deleteBtn = new Button("Delete");
            deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 6; -fx-padding: 6 12; -fx-cursor: hand;");
            deleteBtn.setOnAction(e -> handleDeleteDoc(doc));
            card.getChildren().add(deleteBtn);
        }

        return card;
    }

    private String getFileIcon(String fileType) {
        if (fileType == null) return "[F]";
        String ft = fileType.toLowerCase();
        if (ft.contains("pdf")) return "[PDF]";
        if (ft.contains("word") || ft.contains("doc")) return "[DOC]";
        if (ft.contains("excel") || ft.contains("sheet") || ft.contains("xls")) return "[XLS]";
        if (ft.contains("image") || ft.contains("png") || ft.contains("jpg")) return "[IMG]";
        if (ft.contains("zip") || ft.contains("rar")) return "[ZIP]";
        if (ft.contains("text") || ft.contains("txt")) return "[TXT]";
        return "[FILE]";
    }

    // ── Upload Form ───────────────────────────────────────────────────────────

    @FXML public void handleToggleForm() {
        boolean visible = uploadFormContainer.isVisible();
        if (!visible) {
            selectedFile = null;
            filePathField.clear();
            categoryCombo.setValue(null);
            descriptionField.clear();
            fileError.setText("");
            categoryError.setText("");
            uploadFormContainer.setVisible(true);
            uploadFormContainer.setManaged(true);
            uploadBtn.setText("Cancel");
        } else {
            hideForm();
        }
    }

    @FXML public void handleCancelUpload() { hideForm(); }

    private void hideForm() {
        uploadFormContainer.setVisible(false);
        uploadFormContainer.setManaged(false);
        uploadBtn.setText("+ Upload Document");
        selectedFile = null;
    }

    @FXML public void handleBrowseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Document");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("All Files", "*.*"),
            new FileChooser.ExtensionFilter("PDF", "*.pdf"),
            new FileChooser.ExtensionFilter("Word", "*.doc", "*.docx"),
            new FileChooser.ExtensionFilter("Excel", "*.xls", "*.xlsx"),
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"),
            new FileChooser.ExtensionFilter("Text", "*.txt")
        );

        File file = fileChooser.showOpenDialog(dialogStage);
        if (file != null) {
            selectedFile = file;
            filePathField.setText(file.getAbsolutePath());
            fileError.setText("");
        }
    }

    @FXML public void handleUpload() {
        boolean valid = true;

        if (selectedFile == null) {
            fileError.setText("Please select a file");
            valid = false;
        } else {
            fileError.setText("");
        }

        if (categoryCombo.getValue() == null) {
            categoryError.setText("Please select a category");
            valid = false;
        } else {
            categoryError.setText("");
        }

        if (!valid) return;

        try {
            User user = SessionManager.getCurrentUser();

            // Determine file type from extension
            String filename = selectedFile.getName();
            String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase() : "";
            String fileType = getFileTypeFromExt(ext);

            Document doc = new Document();
            doc.setProjectId(project.getId());
            doc.setUploadedById(user.getId());
            doc.setFilename(filename);                    // Only filename
            doc.setFilePath(filename);                    // Store only filename, NOT full path
            doc.setFileType(fileType);
            doc.setCategory(categoryCombo.getValue());
            doc.setDescription(descriptionField.getText().trim());
            doc.setVersion(1);

            documentDAO.save(doc);
            hideForm();
            loadDocuments();
            showAlert("Success", "Document uploaded successfully!", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to upload: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private String getFileTypeFromExt(String ext) {
        switch (ext) {
            case "pdf": return "application/pdf";
            case "doc": case "docx": return "application/msword";
            case "xls": case "xlsx": return "application/excel";
            case "png": return "image/png";
            case "jpg": case "jpeg": return "image/jpeg";
            case "txt": return "text/plain";
            case "zip": return "application/zip";
            default: return "application/octet-stream";
        }
    }

    private void handleDeleteDoc(Document doc) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Document");
        confirm.setHeaderText("Delete \"" + doc.getFilename() + "\"?");
        confirm.setContentText("This action cannot be undone.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                documentDAO.delete(doc.getId());
                loadDocuments();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to delete: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML public void handleClose() { dialogStage.close(); }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
