package org.example.controller;

import org.example.dao.CandidatureDAO;
import org.example.dao.CandidatureNoteDAO;
import org.example.dao.MatchingScoreDAO;
import org.example.model.Candidature;
import org.example.model.MatchingScore;
import org.example.model.User;
import org.example.service.PDFExportService;
import org.example.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class MyCandidaturesController implements Initializable {

    @FXML private VBox candidaturesContainer;
    @FXML private ComboBox<String> filterStatus;
    @FXML private Label statTotal;
    @FXML private Label statPending;
    @FXML private Label statAccepted;
    @FXML private Label statRejected;

    // Edit form
    @FXML private VBox editFormContainer;
    @FXML private Label editOfferTitle;
    @FXML private TextArea motivationField;
    @FXML private TextField cvPathField;
    @FXML private TextField portfolioField;
    @FXML private TextField githubField;
    @FXML private Label motivationError;
    @FXML private Label cvError;

    private final CandidatureDAO candidatureDAO = new CandidatureDAO();
    private final MatchingScoreDAO matchingScoreDAO = new MatchingScoreDAO();
    private final CandidatureNoteDAO noteDAO = new CandidatureNoteDAO();
    private final PDFExportService pdfService = new PDFExportService();
    private List<Candidature> allCandidatures = new ArrayList<>();
    private Candidature editingCandidature = null;
    private File selectedCvFile = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filterStatus.setItems(FXCollections.observableArrayList("All", "pending", "accepted", "rejected"));
        filterStatus.setValue("All");
        filterStatus.valueProperty().addListener((obs, o, v) -> applyFilter());

        motivationField.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() > 2000) motivationField.setText(old);
            if (val != null && !val.isEmpty() && !val.matches("[a-zA-Z0-9\\s\\p{Punct}\u00C0-\u017F\\n\\r]*"))
                motivationField.setText(old);
        });

        loadCandidatures();
    }

    private void loadCandidatures() {
        try {
            User user = SessionManager.getCurrentUser();
            allCandidatures = candidatureDAO.findByStudent(user.getId());
            updateStats();
            applyFilter();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateStats() {
        statTotal.setText(String.valueOf(allCandidatures.size()));
        long pending = allCandidatures.stream().filter(c -> "pending".equals(c.getStatus())).count();
        long accepted = allCandidatures.stream().filter(c -> "accepted".equals(c.getStatus())).count();
        long rejected = allCandidatures.stream().filter(c -> "rejected".equals(c.getStatus())).count();
        statPending.setText(String.valueOf(pending));
        statAccepted.setText(String.valueOf(accepted));
        statRejected.setText(String.valueOf(rejected));
    }

    private void applyFilter() {
        String status = filterStatus.getValue();
        List<Candidature> filtered = new ArrayList<>();
        for (Candidature c : allCandidatures) {
            if ("All".equals(status) || status.equals(c.getStatus())) filtered.add(c);
        }
        displayCandidatures(filtered);
    }

    private void displayCandidatures(List<Candidature> list) {
        candidaturesContainer.getChildren().clear();
        if (list.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(Pos.CENTER);
            empty.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e0e0e0; -fx-border-radius: 12; -fx-border-width: 2; -fx-border-style: dashed;");
            empty.setPrefHeight(150);
            empty.setPadding(new Insets(30));
            Label lbl = new Label("No applications yet. Go to Offers to apply!");
            lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #999;");
            empty.getChildren().add(lbl);
            candidaturesContainer.getChildren().add(empty);
            return;
        }
        for (Candidature c : list) candidaturesContainer.getChildren().add(createCard(c));
    }

    private VBox createCard(Candidature c) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2); -fx-border-color: #f0f0f0; -fx-border-radius: 12; -fx-border-width: 1;");
        card.setPadding(new Insets(16));

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label offerLabel = new Label(c.getOfferTitle() != null ? c.getOfferTitle() : "Unknown Offer");
        offerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e1e1e;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String statusColor = getStatusColor(c.getStatus());
        String statusIcon = getStatusIcon(c.getStatus());
        Label statusLabel = new Label(statusIcon + " " + (c.getStatus() != null ? c.getStatus().toUpperCase() : "PENDING"));
        statusLabel.setStyle("-fx-background-color: " + statusColor + "22; -fx-text-fill: " + statusColor + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 4 10;");
        header.getChildren().addAll(offerLabel, spacer, statusLabel);

        Label dateLabel = new Label("Applied: " + (c.getCreatedAt() != null ? c.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : ""));
        dateLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10px;");

        // Files info
        HBox files = new HBox(12);
        files.setAlignment(Pos.CENTER_LEFT);
        if (c.getCvPath() != null && !c.getCvPath().isEmpty()) {
            Label cv = new Label("CV: " + c.getCvPath());
            cv.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 11px;");
            files.getChildren().add(cv);
        }
        if (c.getPortfolioUrl() != null && !c.getPortfolioUrl().isEmpty()) {
            Label portfolio = new Label("Portfolio: " + c.getPortfolioUrl());
            portfolio.setStyle("-fx-text-fill: #8b5cf6; -fx-font-size: 11px;");
            files.getChildren().add(portfolio);
        }

        card.getChildren().addAll(header, dateLabel, files);

        // Feedback from establishment
        if (c.getFeedback() != null && !c.getFeedback().isEmpty()) {
            VBox feedbackBox = new VBox(4);
            feedbackBox.setStyle("-fx-background-color: " + getStatusColor(c.getStatus()) + "11; -fx-background-radius: 8; -fx-padding: 10;");
            Label fbTitle = new Label("Feedback from establishment:");
            fbTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #666;");
            Label fbContent = new Label(c.getFeedback());
            fbContent.setStyle("-fx-text-fill: #444; -fx-font-size: 12px;");
            fbContent.setWrapText(true);
            feedbackBox.getChildren().addAll(fbTitle, fbContent);
            card.getChildren().add(feedbackBox);
        }

        // AI Score panel
        try {
            MatchingScore ms = matchingScoreDAO.findByCandidature(c.getId());
            if (ms != null) {
                VBox scoreBox = new VBox(6);
                scoreBox.setStyle("-fx-background-color: #f8f9ff; -fx-background-radius: 8; -fx-padding: 10; -fx-border-color: #667eea33; -fx-border-radius: 8; -fx-border-width: 1;");

                HBox scoreHeader = new HBox(8);
                scoreHeader.setAlignment(Pos.CENTER_LEFT);
                Label scoreTitle = new Label("🤖 AI Matching Score");
                scoreTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #1a1a2e;");
                Label scoreVal = new Label(String.format("%.1f%%", ms.getScore()));
                scoreVal.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + ms.getMatchLevelColor() + ";");
                Label levelBadge = new Label(ms.getMatchLevel() != null ? ms.getMatchLevel().toUpperCase() : "");
                levelBadge.setStyle("-fx-background-color: " + ms.getMatchLevelColor() + "; -fx-text-fill: white; -fx-background-radius: 4; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: bold;");
                Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
                scoreHeader.getChildren().addAll(scoreTitle, sp2, scoreVal, levelBadge);
                scoreBox.getChildren().add(scoreHeader);

                // Sub-scores
                GridPane grid = new GridPane();
                grid.setHgap(8); grid.setVgap(4);
                addScoreRow(grid, "Skills", ms.getSkillsScore(), 0);
                addScoreRow(grid, "Description", ms.getDescriptionScore(), 1);
                addScoreRow(grid, "Keywords", ms.getKeywordsScore(), 2);
                scoreBox.getChildren().add(grid);

                // Matched skills
                if (!ms.getMatchedSkills().isEmpty()) {
                    FlowPane matched = new FlowPane(4, 4);
                    for (String skill : ms.getMatchedSkills()) {
                        Label chip = new Label(skill);
                        chip.setStyle("-fx-background-color: #28a74522; -fx-text-fill: #28a745; -fx-background-radius: 8; -fx-padding: 2 8; -fx-font-size: 10px;");
                        matched.getChildren().add(chip);
                    }
                    scoreBox.getChildren().add(matched);
                }
                // Missing skills
                if (!ms.getMissingSkills().isEmpty()) {
                    FlowPane missing = new FlowPane(4, 4);
                    for (String skill : ms.getMissingSkills().subList(0, Math.min(5, ms.getMissingSkills().size()))) {
                        Label chip = new Label(skill);
                        chip.setStyle("-fx-background-color: #dc354522; -fx-text-fill: #dc3545; -fx-background-radius: 8; -fx-padding: 2 8; -fx-font-size: 10px;");
                        missing.getChildren().add(chip);
                    }
                    scoreBox.getChildren().add(missing);
                }
                card.getChildren().add(scoreBox);
            }
        } catch (Exception ignored) {}

        // Actions
        Separator sep = new Separator();
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        // PDF export button (always visible)
        Button pdfBtn = new Button("📄 PDF");
        pdfBtn.setStyle("-fx-background-color: #a12c2f; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 6; -fx-padding: 6 12; -fx-cursor: hand;");
        pdfBtn.setOnAction(e -> exportPDF(c));
        actions.getChildren().add(pdfBtn);

        if ("pending".equals(c.getStatus())) {
            Button editBtn = new Button("Edit");
            editBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 6; -fx-padding: 6 12; -fx-cursor: hand;");
            editBtn.setOnAction(e -> openEditForm(c));

            Button deleteBtn = new Button("Withdraw");
            deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 6; -fx-padding: 6 12; -fx-cursor: hand;");
            deleteBtn.setOnAction(e -> handleDelete(c));
            actions.getChildren().addAll(editBtn, deleteBtn);
        }

        card.getChildren().addAll(sep, actions);
        return card;
    }

    private void openEditForm(Candidature c) {
        editingCandidature = c;
        editOfferTitle.setText("Edit application for: " + c.getOfferTitle());
        motivationField.setText(c.getMotivationLetter() != null ? c.getMotivationLetter() : "");
        cvPathField.setText(c.getCvPath() != null ? c.getCvPath() : "");
        portfolioField.setText(c.getPortfolioUrl() != null ? c.getPortfolioUrl() : "");
        githubField.setText(c.getGithubUrl() != null ? c.getGithubUrl() : "");
        motivationError.setText("");
        cvError.setText("");
        selectedCvFile = null;
        editFormContainer.setVisible(true);
        editFormContainer.setManaged(true);
    }

    @FXML public void handleBrowseCV() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select CV");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
            new FileChooser.ExtensionFilter("Word Files", "*.doc", "*.docx")
        );
        File file = fc.showOpenDialog(candidaturesContainer.getScene().getWindow());
        if (file != null) {
            selectedCvFile = file;
            cvPathField.setText(file.getName());
        }
    }

    @FXML public void handleCancelEdit() {
        editFormContainer.setVisible(false);
        editFormContainer.setManaged(false);
        editingCandidature = null;
        selectedCvFile = null;
    }

    @FXML public void handleSaveEdit() {
        boolean valid = true;
        if (motivationField.getText() == null || motivationField.getText().trim().length() < 50) {
            motivationError.setText("Minimum 50 characters required"); valid = false;
        } else { motivationError.setText(""); }

        if (!valid) return;

        try {
            editingCandidature.setMotivationLetter(motivationField.getText().trim());
            if (selectedCvFile != null) editingCandidature.setCvPath(selectedCvFile.getName());
            editingCandidature.setPortfolioUrl(portfolioField.getText().trim());
            editingCandidature.setGithubUrl(githubField.getText().trim());
            candidatureDAO.update(editingCandidature);
            handleCancelEdit();
            loadCandidatures();
        } catch (Exception e) {
            e.printStackTrace();
            motivationError.setText("Error: " + e.getMessage());
        }
    }

    private void handleDelete(Candidature c) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Withdraw Application");
        confirm.setHeaderText("Withdraw your application for \"" + c.getOfferTitle() + "\"?");
        confirm.setContentText("This action cannot be undone.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                candidatureDAO.delete(c.getId());
                loadCandidatures();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getStatusColor(String status) {
        if (status == null) return "#888";
        switch (status) {
            case "accepted": return "#22c55e";
            case "rejected": return "#ef4444";
            default: return "#f59e0b";
        }
    }

    private String getStatusIcon(String status) {
        if (status == null) return "[?]";
        switch (status) {
            case "accepted": return "[OK]";
            case "rejected": return "[X]";
            default: return "[...]";
        }
    }

    private void addScoreRow(GridPane grid, String label, double value, int row) {
        javafx.scene.control.Label lbl = new javafx.scene.control.Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        javafx.scene.control.ProgressBar pb = new javafx.scene.control.ProgressBar(value / 100.0);
        pb.setPrefWidth(120);
        String color = value >= 80 ? "#28a745" : value >= 60 ? "#ffc107" : value >= 40 ? "#fd7e14" : "#dc3545";
        pb.setStyle("-fx-accent: " + color + ";");
        javafx.scene.control.Label val = new javafx.scene.control.Label(String.format("%.0f%%", value));
        val.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        grid.add(lbl, 0, row);
        grid.add(pb, 1, row);
        grid.add(val, 2, row);
    }

    private void exportPDF(Candidature c) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Save PDF");
        fc.setInitialFileName("candidature_" + c.getId() + ".pdf");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fc.showSaveDialog(candidaturesContainer.getScene().getWindow());
        if (file == null) return;
        try {
            MatchingScore ms = matchingScoreDAO.findByCandidature(c.getId());
            java.util.List<org.example.model.CandidatureNote> notes = noteDAO.findByCandidature(c.getId());
            pdfService.exportCandidature(c, ms, notes, file.getAbsolutePath());
            new Alert(Alert.AlertType.INFORMATION, "PDF exported:\n" + file.getAbsolutePath()).showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "PDF error: " + e.getMessage()).showAndWait();
        }
    }
}
