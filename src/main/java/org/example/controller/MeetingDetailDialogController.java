package org.example.controller;

import org.example.dao.MeetingDAO;
import org.example.dao.MeetingReportDAO;
import org.example.model.Meeting;
import org.example.model.MeetingReport;
import org.example.service.OllamaService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class MeetingDetailDialogController {

    @FXML private Label headerSub, statusBadge;
    @FXML private Label detailProject, detailType, detailDate, detailDuration;
    @FXML private Label detailReschedule, detailLocation, detailLink, detailAgenda, detailCreatedAt;
    @FXML private Label aiSummaryLabel;
    @FXML private Button closeBtn, generateAiBtn, distanceBtn;
    @FXML private Button followUpEmailBtn;

    private final OllamaService ollamaService = new OllamaService();
    private final MeetingReportDAO reportDAO = new MeetingReportDAO();
    private final MeetingDAO meetingDAO = new MeetingDAO();
    private Meeting meeting;

    public void init(Meeting m) {
        this.meeting = m;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        headerSub.setText(m.getMeetingType() + " — " + safe(m.getProjectTitle()));
        statusBadge.setText(m.getStatus());
        String color = "CONFIRMED".equals(m.getStatus()) ? "#27ae60"
                     : "CANCELLED".equals(m.getStatus()) ? "#e74c3c" : "#f39c12";
        statusBadge.setStyle("-fx-background-color:" + color
            + ";-fx-text-fill:white;-fx-background-radius:12;-fx-padding:3 10;-fx-font-weight:bold;");

        detailProject.setText(safe(m.getProjectTitle()));
        detailType.setText(safe(m.getMeetingType()));
        detailDate.setText(m.getScheduledDate() != null ? m.getScheduledDate().format(fmt) : "—");
        detailDuration.setText(m.getDuration() + " min");
        detailReschedule.setText(String.valueOf(m.getRescheduleCount()));
        detailLocation.setText(safe(m.getLocation()));
        String link = m.getMeetingLink() != null && !m.getMeetingLink().isBlank() ? m.getMeetingLink() : null;
        detailLink.setText(link != null ? link : "—");
        if (link != null) {
            detailLink.setStyle("-fx-text-fill:#2980b9;-fx-cursor:hand;-fx-underline:true;");
            final String finalLink = link;
            detailLink.setOnMouseClicked(e -> {
                try { java.awt.Desktop.getDesktop().browse(new java.net.URI(finalLink)); }
                catch (Exception ex) { ex.printStackTrace(); }
            });
        }
        detailAgenda.setText(safe(m.getAgenda()));
        detailCreatedAt.setText(m.getCreatedAt() != null ? m.getCreatedAt().format(fmt) : "—");

        // AI Summary
        if (aiSummaryLabel != null) {
            aiSummaryLabel.setText(m.getAiSummary() != null && !m.getAiSummary().isBlank()
                ? m.getAiSummary() : "No AI summary yet. Click the button to generate one.");
        }

        // Bouton AI : vérifier si Ollama est disponible
        if (generateAiBtn != null) {
            boolean available = ollamaService.isAvailable();
            generateAiBtn.setDisable(!available);
            if (!available) {
                generateAiBtn.setText("🤖 AI (Ollama not running)");
                generateAiBtn.setStyle(generateAiBtn.getStyle()
                    + "-fx-opacity:0.5;");
            }
        }

        // Bouton Distance : désactiver si pas de lieu
        if (distanceBtn != null) {
            boolean hasLoc = m.getLocation() != null && !m.getLocation().isBlank()
                && !"Online Meeting".equals(m.getLocation());
            distanceBtn.setDisable(!hasLoc);
        }

        // Bouton Follow-up Email
        if (followUpEmailBtn != null) {
            followUpEmailBtn.setDisable(false);
        }
    }

    @FXML
    public void handleGenerateAiSummary() {
        if (meeting == null) return;

        generateAiBtn.setDisable(true);
        generateAiBtn.setText("⏳ Generating...");
        if (aiSummaryLabel != null)
            aiSummaryLabel.setText("Generating summary, please wait...");

        new Thread(() -> {
            try {
                List<MeetingReport> reports = reportDAO.findByMeeting(meeting.getId());
                if (reports.isEmpty()) {
                    Platform.runLater(() -> {
                        showAlert("No meeting report found.\nCreate a report first (📋 Reports button).");
                        resetAiBtn();
                    });
                    return;
                }

                MeetingReport latest = reports.get(0);
                String summary = ollamaService.generateMeetingSummary(meeting, latest);

                if (summary != null && !summary.isBlank()) {
                    meeting.setAiSummary(summary);
                    meetingDAO.updateAiSummary(meeting.getId(), summary);
                    Platform.runLater(() -> {
                        if (aiSummaryLabel != null) aiSummaryLabel.setText(summary);
                        generateAiBtn.setDisable(false);
                        generateAiBtn.setText("🤖 Regenerate AI Summary");
                        showInfo("AI summary generated successfully!");
                    });
                } else {
                    Platform.runLater(() -> {
                        showAlert("Failed to generate summary. Check Ollama is running.");
                        resetAiBtn();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert("Error: " + e.getMessage());
                    resetAiBtn();
                });
            }
        }).start();
    }

    private void resetAiBtn() {
        generateAiBtn.setDisable(false);
        generateAiBtn.setText("🤖 Generate AI Summary");
    }

    @FXML
    public void handleFollowUpEmail() {
        if (meeting == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MeetingFollowUpEmail.fxml"));
            Pane root = loader.load();
            MeetingFollowUpEmailController ctrl = loader.getController();
            ctrl.init(meeting);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Email de Suivi");
            stage.setScene(new Scene(root, 520, 560));
            stage.getScene().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleCalculateDistance() {
        if (meeting == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MeetingDistance.fxml"));
            Pane root = loader.load();
            MeetingDistanceController ctrl = loader.getController();
            ctrl.init(meeting);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Distance Calculator");
            stage.setScene(new Scene(root, 460, 520));
            stage.getScene().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleClose() {
        ((Stage) closeBtn.getScene().getWindow()).close();
    }

    private String safe(String s) { return s != null ? s : "—"; }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
