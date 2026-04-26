package org.example.controller;

import org.example.dao.CandidatureDAO;
import org.example.dao.CandidatureNoteDAO;
import org.example.dao.MatchingScoreDAO;
import org.example.model.Candidature;
import org.example.model.CandidatureNote;
import org.example.model.MatchingScore;
import org.example.service.AIMatchingService;
import org.example.service.EmailNotificationService;
import org.example.service.GeminiService;
import org.example.service.PDFExportService;
import org.example.util.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import org.example.util.ModernAlert;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.scene.paint.Color;

import java.util.List;

public class CandidatureDetailController {

    // Info labels
    @FXML private Label lblStudent;
    @FXML private Label lblOffer;
    @FXML private Label lblStatus;
    @FXML private Label lblDate;
    @FXML private TextArea motivationArea;
    @FXML private Label lblCvPath;
    @FXML private Label lblPortfolio;

    // AI Score panel
    @FXML private Label lblOverallScore;
    @FXML private ProgressBar pbOverall;
    @FXML private ProgressBar pbSkills;
    @FXML private ProgressBar pbDescription;
    @FXML private ProgressBar pbKeywords;
    @FXML private Label lblSkillsScore;
    @FXML private Label lblDescScore;
    @FXML private Label lblKeywordsScore;
    @FXML private Label lblMatchLevel;
    @FXML private FlowPane matchedSkillsPane;
    @FXML private FlowPane missingSkillsPane;
    @FXML private TextArea recommendationsArea;
    @FXML private Label lblAiStatus;

    // Status update
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextArea feedbackArea;

    // Notes
    @FXML private VBox notesContainer;
    @FXML private TextArea noteContentArea;
    @FXML private Spinner<Integer> ratingSpinner;
    @FXML private CheckBox privateCheck;
    @FXML private Label notesMessage;

    private final CandidatureDAO candidatureDAO = new CandidatureDAO();
    private final MatchingScoreDAO matchingScoreDAO = new MatchingScoreDAO();
    private final CandidatureNoteDAO noteDAO = new CandidatureNoteDAO();
    private final AIMatchingService aiService = new AIMatchingService();
    private final EmailNotificationService emailService = new EmailNotificationService();
    private final GeminiService geminiService = new GeminiService();
    private final PDFExportService pdfService = new PDFExportService();
    private Candidature currentCandidature;

    public void loadCandidature(Candidature c) {
        this.currentCandidature = c;

        lblStudent.setText(c.getStudentName());
        lblOffer.setText(c.getOfferTitle());
        lblStatus.setText(c.getStatus());
        lblDate.setText(c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate().toString() : "-");
        motivationArea.setText(c.getMotivationLetter() != null ? c.getMotivationLetter() : "");
        lblCvPath.setText(c.getCvPath() != null ? c.getCvPath() : "-");
        lblPortfolio.setText(c.getPortfolioUrl() != null ? c.getPortfolioUrl() : "-");

        statusCombo.setItems(FXCollections.observableArrayList("pending", "accepted", "rejected", "interview"));
        statusCombo.setValue(c.getStatus());
        feedbackArea.setText(c.getFeedback() != null ? c.getFeedback() : "");

        ratingSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 3));

        // Load existing AI score
        try {
            MatchingScore ms = matchingScoreDAO.findByCandidature(c.getId());
            if (ms != null) displayScore(ms);
            else lblAiStatus.setText("No score calculated yet. Click 'Calculate AI Score'.");
        } catch (Exception e) { e.printStackTrace(); }

        loadNotes();
    }

    @FXML
    public void handleCalculateScore() {
        lblAiStatus.setText("Calculating...");
        new Thread(() -> {
            try {
                MatchingScore ms = aiService.calculateMatchingScore(currentCandidature.getId());
                Platform.runLater(() -> {
                    displayScore(ms);
                    lblAiStatus.setText("Score calculated successfully.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> lblAiStatus.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    private void displayScore(MatchingScore ms) {
        double score = ms.getScore();
        lblOverallScore.setText(String.format("%.1f%%", score));
        pbOverall.setProgress(score / 100.0);
        pbSkills.setProgress(ms.getSkillsScore() / 100.0);
        pbDescription.setProgress(ms.getDescriptionScore() / 100.0);
        pbKeywords.setProgress(ms.getKeywordsScore() / 100.0);
        lblSkillsScore.setText(String.format("%.1f%%", ms.getSkillsScore()));
        lblDescScore.setText(String.format("%.1f%%", ms.getDescriptionScore()));
        lblKeywordsScore.setText(String.format("%.1f%%", ms.getKeywordsScore()));

        String level = ms.getMatchLevel() != null ? ms.getMatchLevel() : "low";
        lblMatchLevel.setText(level.toUpperCase());
        lblMatchLevel.setStyle("-fx-background-color: " + ms.getMatchLevelColor() +
            "; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 3 10; -fx-font-weight: bold;");

        // Color progress bars
        colorProgressBar(pbOverall, score);
        colorProgressBar(pbSkills, ms.getSkillsScore());
        colorProgressBar(pbDescription, ms.getDescriptionScore());
        colorProgressBar(pbKeywords, ms.getKeywordsScore());

        // Matched skills chips
        matchedSkillsPane.getChildren().clear();
        for (String skill : ms.getMatchedSkills()) {
            Label chip = createChip(skill, "#28a745", "white");
            matchedSkillsPane.getChildren().add(chip);
        }

        // Missing skills chips
        missingSkillsPane.getChildren().clear();
        for (String skill : ms.getMissingSkills()) {
            Label chip = createChip(skill, "#dc3545", "white");
            missingSkillsPane.getChildren().add(chip);
        }

        recommendationsArea.setText(ms.getRecommendations() != null ? ms.getRecommendations() : "");
    }

    private void colorProgressBar(ProgressBar pb, double value) {
        String color;
        if (value >= 80) color = "#28a745";
        else if (value >= 60) color = "#ffc107";
        else if (value >= 40) color = "#fd7e14";
        else color = "#dc3545";
        pb.setStyle("-fx-accent: " + color + ";");
    }

    private Label createChip(String text, String bg, String fg) {
        Label chip = new Label(text);
        chip.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg +
            "; -fx-background-radius: 12; -fx-padding: 3 10; -fx-font-size: 11px;");
        FlowPane.setMargin(chip, new Insets(2));
        return chip;
    }

    @FXML
    public void handleUpdateStatus() {
        try {
            candidatureDAO.updateStatus(currentCandidature.getId(), statusCombo.getValue(), feedbackArea.getText());
            lblStatus.setText(statusCombo.getValue());
            // Send email notification
            emailService.sendStatusChange(currentCandidature.getId(), statusCombo.getValue(), feedbackArea.getText());
            ModernAlert.show(ModernAlert.Type.INFO, "Information", "Status updated. Email notification sent.");
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    public void handleExportPDF() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save PDF Report");
        fc.setInitialFileName("candidature_" + currentCandidature.getId() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        java.io.File file = fc.showSaveDialog(null);
        if (file == null) return;
        try {
            MatchingScore ms = matchingScoreDAO.findByCandidature(currentCandidature.getId());
            java.util.List<CandidatureNote> notes = noteDAO.findByCandidature(currentCandidature.getId());
            pdfService.exportCandidature(currentCandidature, ms, notes, file.getAbsolutePath());
            new Alert(Alert.AlertType.INFORMATION, "PDF exported to:\n" + file.getAbsolutePath()).showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "PDF export failed: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    public void handleGeminiSuggestions() {
        lblAiStatus.setText("Getting AI suggestions...");
        new Thread(() -> {
            try {
                String suggestions = geminiService.suggestImprovements(
                    currentCandidature.getMotivationLetter() != null ? currentCandidature.getMotivationLetter() : "",
                    "", ""
                );
                Platform.runLater(() -> {
                    recommendationsArea.setText(suggestions);
                    lblAiStatus.setText("Gemini suggestions loaded.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> lblAiStatus.setText("Gemini error: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void handleAddNote() {
        if (noteContentArea.getText().trim().isEmpty()) {
            notesMessage.setText("Note content is required.");
            notesMessage.setStyle("-fx-text-fill: red;");
            return;
        }
        try {
            CandidatureNote note = new CandidatureNote();
            note.setCandidatureId(currentCandidature.getId());
            note.setAuthorId(SessionManager.getCurrentUser().getId());
            note.setContent(noteContentArea.getText().trim());
            note.setRating(ratingSpinner.getValue());
            note.setPrivate(privateCheck.isSelected());
            noteDAO.save(note);
            noteContentArea.clear();
            ratingSpinner.getValueFactory().setValue(3);
            privateCheck.setSelected(false);
            notesMessage.setText("Note added.");
            notesMessage.setStyle("-fx-text-fill: green;");
            loadNotes();
        } catch (Exception e) {
            notesMessage.setText("Error: " + e.getMessage());
            notesMessage.setStyle("-fx-text-fill: red;");
        }
    }

    private void loadNotes() {
        notesContainer.getChildren().clear();
        try {
            List<CandidatureNote> notes = noteDAO.findByCandidature(currentCandidature.getId());
            for (CandidatureNote note : notes) {
                VBox card = buildNoteCard(note);
                notesContainer.getChildren().add(card);
            }
            if (notes.isEmpty()) {
                Label empty = new Label("No notes yet.");
                empty.setStyle("-fx-text-fill: #999;");
                notesContainer.getChildren().add(empty);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private VBox buildNoteCard(CandidatureNote note) {
        VBox card = new VBox(4);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2); -fx-padding: 10;");

        HBox header = new HBox(8);
        Label author = new Label(note.getAuthorName() != null ? note.getAuthorName() : "Unknown");
        author.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        Label stars = new Label(note.getStars());
        stars.setStyle("-fx-text-fill: #ffc107;");
        Label date = new Label(note.getCreatedAt() != null ? note.getCreatedAt().toLocalDate().toString() : "");
        date.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
        if (note.isPrivate()) {
            Label priv = new Label("🔒 Private");
            priv.setStyle("-fx-text-fill: #888; -fx-font-size: 10px;");
            header.getChildren().addAll(author, stars, date, priv);
        } else {
            header.getChildren().addAll(author, stars, date);
        }

        Label content = new Label(note.getContent());
        content.setWrapText(true);
        content.setStyle("-fx-font-size: 12px; -fx-text-fill: #333;");

        Button del = new Button("Delete");
        del.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 11px; -fx-padding: 2 8;");
        del.setOnAction(e -> {
            try { noteDAO.delete(note.getId()); loadNotes(); }
            catch (Exception ex) { ex.printStackTrace(); }
        });

        card.getChildren().addAll(header, content, del);
        VBox.setMargin(card, new Insets(0, 0, 8, 0));
        return card;
    }
}
