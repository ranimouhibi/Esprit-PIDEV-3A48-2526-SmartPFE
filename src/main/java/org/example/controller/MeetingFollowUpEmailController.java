package org.example.controller;

import org.example.dao.MeetingParticipantDAO;
import org.example.dao.MeetingReportDAO;
import org.example.model.Meeting;
import org.example.model.MeetingReport;
import org.example.model.User;
import org.example.service.EmailService;
import org.example.service.OllamaService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;

public class MeetingFollowUpEmailController {

    @FXML private Label meetingTitleLabel;
    @FXML private TextArea emailBodyArea;
    @FXML private Button generateBtn;
    @FXML private Button sendBtn;
    @FXML private Button closeBtn;
    @FXML private Label statusLabel;

    private final OllamaService ollamaService = new OllamaService();
    private final EmailService emailService = new EmailService();
    private final MeetingReportDAO reportDAO = new MeetingReportDAO();
    private final MeetingParticipantDAO participantDAO = new MeetingParticipantDAO();

    private Meeting meeting;

    public void init(Meeting m) {
        this.meeting = m;
        meetingTitleLabel.setText("Meeting : " + safe(m.getProjectTitle())
            + " — " + (m.getScheduledDate() != null
                ? m.getScheduledDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : ""));
        sendBtn.setDisable(true);

        if (!ollamaService.isAvailable()) {
            statusLabel.setText("⚠ Ollama non disponible — vous pouvez écrire l'email manuellement.");
            statusLabel.setStyle("-fx-text-fill:#f59e0b;-fx-font-size:11px;");
            generateBtn.setDisable(true);
        }
    }

    @FXML
    public void handleGenerate() {
        generateBtn.setDisable(true);
        statusLabel.setText("Génération en cours...");
        statusLabel.setStyle("-fx-text-fill:#8b8fa8;-fx-font-size:11px;");

        new Thread(() -> {
            try {
                List<MeetingReport> reports = reportDAO.findByMeeting(meeting.getId());
                MeetingReport report = reports.isEmpty() ? null : reports.get(0);
                String email = ollamaService.generateFollowUpEmail(meeting, report);

                Platform.runLater(() -> {
                    if (email != null && !email.isBlank()) {
                        emailBodyArea.setText(email);
                        sendBtn.setDisable(!emailService.isEnabled());
                        statusLabel.setText("✓ Email généré. Vous pouvez le modifier avant envoi.");
                        statusLabel.setStyle("-fx-text-fill:#22c55e;-fx-font-size:11px;");
                    } else {
                        statusLabel.setText("Échec de la génération. Écrivez l'email manuellement.");
                        statusLabel.setStyle("-fx-text-fill:#ef4444;-fx-font-size:11px;");
                    }
                    generateBtn.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Erreur : " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill:#ef4444;-fx-font-size:11px;");
                    generateBtn.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    public void handleSend() {
        String body = emailBodyArea.getText().trim();
        if (body.isEmpty()) {
            statusLabel.setText("L'email est vide.");
            return;
        }
        sendBtn.setDisable(true);
        statusLabel.setText("Envoi en cours...");

        new Thread(() -> {
            try {
                List<User> recipients = participantDAO.findParticipantsAndSupervisor(meeting.getId());
                if (recipients.isEmpty()) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Aucun participant trouvé pour ce meeting.");
                        statusLabel.setStyle("-fx-text-fill:#f59e0b;-fx-font-size:11px;");
                        sendBtn.setDisable(false);
                    });
                    return;
                }
                emailService.sendFollowUpEmail(meeting, recipients, body);
                Platform.runLater(() -> {
                    statusLabel.setText("✓ Email envoyé à " + recipients.size() + " participant(s).");
                    statusLabel.setStyle("-fx-text-fill:#22c55e;-fx-font-size:11px;");
                    sendBtn.setDisable(true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Erreur envoi : " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill:#ef4444;-fx-font-size:11px;");
                    sendBtn.setDisable(false);
                });
            }
        }).start();
    }

    private String safe(String s) { return s != null ? s : ""; }

    @FXML
    public void handleClose() {
        ((Stage) closeBtn.getScene().getWindow()).close();
    }
}
