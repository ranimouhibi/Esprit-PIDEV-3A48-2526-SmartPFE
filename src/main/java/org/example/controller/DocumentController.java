package org.example.controller;

import org.example.dao.DocumentDAO;
import org.example.model.Document;
import org.example.service.DocumentService;
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

public class DocumentController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCategory;
    @FXML private ComboBox<String> sortCombo;
    @FXML private VBox docsContainer;
    @FXML private Label countLabel;
    
    // Stats
    @FXML private Label statTotal;
    @FXML private Label statProposals;
    @FXML private Label statReports;
    @FXML private Label statPresentations;

    private final DocumentDAO documentDAO = new DocumentDAO();
    private final DocumentService documentService = new DocumentService();
    private List<Document> allDocs = new ArrayList<>();
    private List<Document> filteredDocs = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filterCategory.setItems(FXCollections.observableArrayList(
            "All Categories", "proposal", "report", "presentation", "code", "other"
        ));
        filterCategory.setValue("All Categories");
        
        sortCombo.setItems(FXCollections.observableArrayList(
            "Date: Newest first", "Date: Oldest first", 
            "Filename: A to Z", "Filename: Z to A",
            "Category: A to Z", "Category: Z to A"
        ));
        sortCombo.setValue("Date: Newest first");
        
        // Live listeners
        searchField.textProperty().addListener((obs, old, val) -> applyFilters());
        filterCategory.valueProperty().addListener((obs, old, val) -> applyFilters());
        sortCombo.valueProperty().addListener((obs, old, val) -> applyFilters());
        
        loadDocuments();
    }

    private void loadDocuments() {
        try {
            allDocs = documentDAO.findAll();
            updateStats();
            applyFilters();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStats() {
        int total = allDocs.size();
        long proposals = allDocs.stream().filter(d -> "proposal".equals(d.getCategory())).count();
        long reports = allDocs.stream().filter(d -> "report".equals(d.getCategory())).count();
        long presentations = allDocs.stream().filter(d -> "presentation".equals(d.getCategory())).count();

        statTotal.setText(String.valueOf(total));
        statProposals.setText(String.valueOf(proposals));
        statReports.setText(String.valueOf(reports));
        statPresentations.setText(String.valueOf(presentations));
    }

    private void applyFilters() {
        String query = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String cat = filterCategory.getValue();
        String sort = sortCombo.getValue();

        filteredDocs = new ArrayList<>();
        for (Document d : allDocs) {
            boolean matchQuery = query.isEmpty()
                || (d.getFilename() != null && d.getFilename().toLowerCase().contains(query))
                || (d.getProjectTitle() != null && d.getProjectTitle().toLowerCase().contains(query));
            boolean matchCat = cat == null || cat.equals("All Categories") || cat.equals(d.getCategory());
            if (matchQuery && matchCat) filteredDocs.add(d);
        }

        // Sort
        if (sort != null) {
            switch (sort) {
                case "Date: Oldest first":
                    filteredDocs.sort((a, b) -> {
                        if (a.getUploadedAt() == null) return 1;
                        if (b.getUploadedAt() == null) return -1;
                        return a.getUploadedAt().compareTo(b.getUploadedAt());
                    });
                    break;
                case "Filename: A to Z":
                    filteredDocs.sort((a, b) -> {
                        String fa = a.getFilename() != null ? a.getFilename() : "";
                        String fb = b.getFilename() != null ? b.getFilename() : "";
                        return fa.compareToIgnoreCase(fb);
                    });
                    break;
                case "Filename: Z to A":
                    filteredDocs.sort((a, b) -> {
                        String fa = a.getFilename() != null ? a.getFilename() : "";
                        String fb = b.getFilename() != null ? b.getFilename() : "";
                        return fb.compareToIgnoreCase(fa);
                    });
                    break;
                case "Category: A to Z":
                    filteredDocs.sort((a, b) -> {
                        String ca = a.getCategory() != null ? a.getCategory() : "";
                        String cb = b.getCategory() != null ? b.getCategory() : "";
                        return ca.compareTo(cb);
                    });
                    break;
                case "Category: Z to A":
                    filteredDocs.sort((a, b) -> {
                        String ca = a.getCategory() != null ? a.getCategory() : "";
                        String cb = b.getCategory() != null ? b.getCategory() : "";
                        return cb.compareTo(ca);
                    });
                    break;
                default: // Date: Newest first
                    filteredDocs.sort((a, b) -> {
                        if (a.getUploadedAt() == null) return 1;
                        if (b.getUploadedAt() == null) return -1;
                        return b.getUploadedAt().compareTo(a.getUploadedAt());
                    });
                    break;
            }
        }

        countLabel.setText(filteredDocs.size() + " document(s)");
        displayDocuments(filteredDocs);
    }

    @FXML public void handleSearch() { applyFilters(); }

    @FXML public void handleReset() {
        searchField.clear();
        filterCategory.setValue("All Categories");
        sortCombo.setValue("Date: Newest first");
    }

    @FXML
    public void handleExportPDF() {
        if (filteredDocs == null || filteredDocs.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setContentText("No documents to export");
            alert.showAndWait();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF Report");
        fileChooser.setInitialFileName("documents_report.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        
        File file = fileChooser.showSaveDialog(docsContainer.getScene().getWindow());
        if (file != null) {
            try {
                PDFExporter.exportDocuments(filteredDocs, file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setContentText("PDF exported successfully!");
                alert.showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Failed to export PDF: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void displayDocuments(List<Document> docs) {
        docsContainer.getChildren().clear();

        if (docs.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(javafx.geometry.Pos.CENTER);
            empty.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e0e0e0; -fx-border-radius: 12; -fx-border-width: 2; -fx-border-style: dashed;");
            empty.setPrefHeight(150);
            empty.setPadding(new Insets(30));
            Label lbl = new Label("No documents found");
            lbl.setStyle("-fx-font-size: 16px; -fx-text-fill: #999;");
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
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2); -fx-border-color: #f0f0f0; -fx-border-radius: 10; -fx-border-width: 1;");
        card.setPadding(new Insets(14));

        // File icon
        Label icon = new Label(getFileIcon(doc.getFileType()));
        icon.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #667eea; -fx-min-width: 50;");

        // Info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLabel = new Label(doc.getFilename());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e1e1e;");

        HBox meta = new HBox(12);
        meta.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        if (doc.getCategory() != null) {
            Label catLabel = new Label(doc.getCategory().toUpperCase());
            catLabel.setStyle("-fx-background-color: #a12c2f22; -fx-text-fill: #a12c2f; -fx-font-size: 9px; -fx-font-weight: bold; -fx-background-radius: 4; -fx-padding: 2 7;");
            meta.getChildren().add(catLabel);
        }

        Label projectLabel = new Label("Project: " + (doc.getProjectTitle() != null ? doc.getProjectTitle() : "N/A"));
        projectLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 11px;");

        Label uploaderLabel = new Label("By: " + (doc.getUploaderName() != null ? doc.getUploaderName() : "Unknown"));
        uploaderLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");

        Label dateLabel = new Label(doc.getUploadedAt() != null
            ? doc.getUploadedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
        dateLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10px;");

        meta.getChildren().addAll(projectLabel, uploaderLabel, dateLabel);
        info.getChildren().addAll(nameLabel, meta);

        if (doc.getDescription() != null && !doc.getDescription().isEmpty()) {
            Label desc = new Label(doc.getDescription());
            desc.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
            info.getChildren().add(desc);
        }

        // 🆕 Bouton AI Résumé
        VBox actions = new VBox(8);
        
        Button aiSummaryBtn = new Button("📝 AI Summary");
        aiSummaryBtn.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 6; -fx-padding: 6 12; -fx-cursor: hand;");
        aiSummaryBtn.setOnAction(e -> handleGenerateAISummary(doc));

        // Delete button (admin)
        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 6; -fx-padding: 6 12; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> handleDelete(doc));

        actions.getChildren().addAll(aiSummaryBtn, deleteBtn);

        card.getChildren().addAll(icon, info, actions);
        return card;
    }

    private String getFileIcon(String fileType) {
        if (fileType == null) return "[FILE]";
        String ft = fileType.toLowerCase();
        if (ft.contains("pdf")) return "[PDF]";
        if (ft.contains("word") || ft.contains("doc")) return "[DOC]";
        if (ft.contains("excel") || ft.contains("xls")) return "[XLS]";
        if (ft.contains("image") || ft.contains("png") || ft.contains("jpg")) return "[IMG]";
        if (ft.contains("zip")) return "[ZIP]";
        if (ft.contains("text")) return "[TXT]";
        return "[FILE]";
    }

    private void handleDelete(Document doc) {
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
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setContentText("Failed to delete: " + e.getMessage());
                err.showAndWait();
            }
        }
    }

    /**
     * Générer un résumé AI pour un document
     */
    private void handleGenerateAISummary(Document doc) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("AI Summary");
        confirm.setHeaderText("Generate AI Summary");
        confirm.setContentText("This will analyze the document content and generate an AI-powered summary. Continue?");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Afficher un indicateur de chargement
                Alert loading = new Alert(Alert.AlertType.INFORMATION);
                loading.setTitle("Processing");
                loading.setHeaderText("Generating AI Summary...");
                loading.setContentText("Please wait while the AI analyzes the document.");
                loading.show();
                
                // Générer le résumé
                String summary = documentService.generateAISummary(doc.getId());
                
                loading.close();
                
                // Afficher le résumé
                Alert summaryAlert = new Alert(Alert.AlertType.INFORMATION);
                summaryAlert.setTitle("AI Summary");
                summaryAlert.setHeaderText("Summary for: " + doc.getFilename());
                
                TextArea textArea = new TextArea(summary);
                textArea.setWrapText(true);
                textArea.setEditable(false);
                textArea.setPrefRowCount(12);
                
                VBox vbox = new VBox(10);
                vbox.setPadding(new Insets(10));
                vbox.getChildren().addAll(
                    new Label("AI-Generated Summary:"),
                    textArea,
                    new Label("This summary has been saved to the document description.")
                );
                
                summaryAlert.getDialogPane().setContent(vbox);
                summaryAlert.showAndWait();
                
                // Recharger les documents pour afficher le résumé mis à jour
                loadDocuments();
                
            } catch (Exception e) {
                e.printStackTrace();
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Error");
                err.setHeaderText("Failed to generate AI summary");
                err.setContentText("Error: " + e.getMessage() + "\n\nMake sure:\n" +
                    "1. The document file exists\n" +
                    "2. OpenAI API key is configured in AIUtil.java\n" +
                    "3. The document contains readable text content");
                err.showAndWait();
            }
        }
    }
}
