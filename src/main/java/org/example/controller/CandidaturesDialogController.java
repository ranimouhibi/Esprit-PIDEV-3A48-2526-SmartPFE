package org.example.controller;

import org.example.dao.CandidatureDAO;
import org.example.model.Candidature;
import org.example.model.Offer;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class CandidaturesDialogController implements Initializable {

    @FXML private Label offerTitleLabel;
    @FXML private Label statTotal;
    @FXML private Label statPending;
    @FXML private Label statAccepted;
    @FXML private Label statRejected;
    @FXML private ComboBox<String> filterStatus;
    @FXML private VBox candidaturesContainer;

    // Feedback form
    @FXML private VBox feedbackFormContainer;
    @FXML private Label feedbackCandidateName;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextArea feedbackField;
    @FXML private Label feedbackError;

    private Stage dialogStage;
    private Offer offer;
    private final CandidatureDAO candidatureDAO = new CandidatureDAO();
    private List<Candidature> allCandidatures = new ArrayList<>();
    private Candidature reviewingCandidature = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filterStatus.setItems(FXCollections.observableArrayList("All", "pending", "accepted", "rejected"));
        filterStatus.setValue("All");
        filterStatus.valueProperty().addListener((obs, o, v) -> applyFilter());

        statusCombo.setItems(FXCollections.observableArrayList("accepted", "rejected"));

        // Input restriction for feedback
        feedbackField.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() > 500) feedbackField.setText(old);
            if (val != null && !val.isEmpty() && !val.matches("[a-zA-Z0-9\\s\\p{Punct}\u00C0-\u017F\\n\\r]*"))
                feedbackField.setText(old);
        });
    }

    public void setDialogStage(Stage stage) { this.dialogStage = stage; }

    public void setOffer(Offer offer) {
        this.offer = offer;
        offerTitleLabel.setText(offer.getTitle());
        loadCandidatures();
    }

    private void loadCandidatures() {
        try {
            allCandidatures = candidatureDAO.findByOffer(offer.getId());
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
            Label lbl = new Label("No candidatures yet");
            lbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #999; -fx-padding: 30;");
            candidaturesContainer.getChildren().add(lbl);
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
        Label nameLabel = new Label(c.getStudentName() != null ? c.getStudentName() : "Unknown");
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e1e1e;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String statusColor = getStatusColor(c.getStatus());
        Label statusLabel = new Label(c.getStatus() != null ? c.getStatus().toUpperCase() : "PENDING");
        statusLabel.setStyle("-fx-background-color: " + statusColor + "22; -fx-text-fill: " + statusColor + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 4 10;");
        header.getChildren().addAll(nameLabel, spacer, statusLabel);

        Label dateLabel = new Label(c.getCreatedAt() != null ? c.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
        dateLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10px;");

        // CV and Portfolio
        HBox links = new HBox(12);
        links.setAlignment(Pos.CENTER_LEFT);
        if (c.getCvPath() != null && !c.getCvPath().isEmpty()) {
            Label cv = new Label("CV: " + c.getCvPath());
            cv.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 11px;");
            links.getChildren().add(cv);
        }
        if (c.getPortfolioUrl() != null && !c.getPortfolioUrl().isEmpty()) {
            Label portfolio = new Label("Portfolio: " + c.getPortfolioUrl());
            portfolio.setStyle("-fx-text-fill: #8b5cf6; -fx-font-size: 11px;");
            links.getChildren().add(portfolio);
        }

        // Motivation letter preview
        if (c.getMotivationLetter() != null && !c.getMotivationLetter().isEmpty()) {
            Label motiv = new Label(c.getMotivationLetter().length() > 120
                ? c.getMotivationLetter().substring(0, 120) + "..." : c.getMotivationLetter());
            motiv.setStyle("-fx-text-fill: #555; -fx-font-size: 11px;");
            motiv.setWrapText(true);
            card.getChildren().addAll(header, dateLabel, links, motiv);
        } else {
            card.getChildren().addAll(header, dateLabel, links);
        }

        // Feedback if exists
        if (c.getFeedback() != null && !c.getFeedback().isEmpty()) {
            Label fb = new Label("Feedback: " + c.getFeedback());
            fb.setStyle("-fx-text-fill: #666; -fx-font-size: 11px; -fx-font-style: italic;");
            fb.setWrapText(true);
            card.getChildren().add(fb);
        }

        // Actions (only for pending)
        if ("pending".equals(c.getStatus())) {
            Separator sep = new Separator();
            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_RIGHT);

            Button reviewBtn = new Button("Review");
            reviewBtn.setStyle("-fx-background-color: #a12c2f; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 6; -fx-padding: 6 14; -fx-cursor: hand;");
            reviewBtn.setOnAction(e -> openFeedbackForm(c));
            actions.getChildren().add(reviewBtn);
            card.getChildren().addAll(sep, actions);
        }

        return card;
    }

    private void openFeedbackForm(Candidature c) {
        reviewingCandidature = c;
        feedbackCandidateName.setText("Reviewing: " + c.getStudentName());
        statusCombo.setValue(null);
        feedbackField.clear();
        feedbackError.setText("");
        feedbackFormContainer.setVisible(true);
        feedbackFormContainer.setManaged(true);
    }

    @FXML public void handleCancelFeedback() {
        feedbackFormContainer.setVisible(false);
        feedbackFormContainer.setManaged(false);
        reviewingCandidature = null;
    }

    @FXML public void handleSubmitFeedback() {
        if (statusCombo.getValue() == null) {
            feedbackError.setText("Please select a decision");
            return;
        }
        feedbackError.setText("");

        try {
            candidatureDAO.updateStatus(
                reviewingCandidature.getId(),
                statusCombo.getValue(),
                feedbackField.getText().trim()
            );
            handleCancelFeedback();
            loadCandidatures();
        } catch (Exception e) {
            e.printStackTrace();
            feedbackError.setText("Error: " + e.getMessage());
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

    @FXML public void handleClose() { dialogStage.close(); }
}
