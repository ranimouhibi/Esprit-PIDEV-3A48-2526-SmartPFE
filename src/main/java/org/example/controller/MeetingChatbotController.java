package org.example.controller;

import org.example.dao.MeetingDAO;
import org.example.model.Meeting;
import org.example.service.OllamaService;
import org.example.util.SessionManager;
import org.example.model.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class MeetingChatbotController {

    @FXML private VBox chatContainer;
    @FXML private ScrollPane chatScroll;
    @FXML private TextField questionField;
    @FXML private Button sendBtn;
    @FXML private Button closeBtn;
    @FXML private Label statusLabel;

    private final OllamaService ollamaService = new OllamaService();
    private final MeetingDAO meetingDAO = new MeetingDAO();
    private String meetingsContext;

    @FXML
    public void initialize() {
        buildContext();
        addBotMessage("Bonjour ! Je suis votre assistant meetings. Posez-moi des questions sur vos meetings, par exemple :\n- Combien de meetings sont confirmés ?\n- Quel est le prochain meeting ?\n- Qui a participé au dernier meeting ?");
        questionField.setOnAction(e -> handleSend());
    }

    private void buildContext() {
        try {
            User user = SessionManager.getCurrentUser();
            List<Meeting> meetings;
            if (user != null && "ADMIN".equalsIgnoreCase(user.getRole())) {
                meetings = meetingDAO.findAll(null, "ALL", "ALL", "scheduledDate", "ASC", 1, 200);
            } else if (user != null) {
                meetings = meetingDAO.findByUser(user.getId());
            } else {
                meetings = meetingDAO.findAll(null, "ALL", "ALL", "scheduledDate", "ASC", 1, 200);
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            StringBuilder sb = new StringBuilder();
            sb.append("Total meetings: ").append(meetings.size()).append("\n");
            long confirmed = meetings.stream().filter(m -> "CONFIRMED".equals(m.getStatus())).count();
            long pending   = meetings.stream().filter(m -> "PENDING".equals(m.getStatus())).count();
            long cancelled = meetings.stream().filter(m -> "CANCELLED".equals(m.getStatus())).count();
            sb.append("Confirmed: ").append(confirmed)
              .append(", Pending: ").append(pending)
              .append(", Cancelled: ").append(cancelled).append("\n\n");

            for (Meeting m : meetings) {
                sb.append("- ID:").append(m.getId())
                  .append(" | Project:").append(safe(m.getProjectTitle()))
                  .append(" | Type:").append(safe(m.getMeetingType()))
                  .append(" | Status:").append(safe(m.getStatus()))
                  .append(" | Date:").append(m.getScheduledDate() != null ? m.getScheduledDate().format(fmt) : "N/A")
                  .append(" | Location:").append(safe(m.getLocation()))
                  .append(" | Duration:").append(m.getDuration()).append("min")
                  .append("\n");
            }
            meetingsContext = sb.toString();
        } catch (Exception e) {
            meetingsContext = "No meeting data available.";
        }
    }

    @FXML
    public void handleSend() {
        String question = questionField.getText().trim();
        if (question.isEmpty()) return;
        if (!ollamaService.isAvailable()) {
            addBotMessage("Ollama n'est pas disponible. Veuillez démarrer Ollama sur localhost:11434.");
            return;
        }

        questionField.clear();
        addUserMessage(question);
        sendBtn.setDisable(true);
        statusLabel.setText("En train de répondre...");

        new Thread(() -> {
            String answer = ollamaService.chat(question, meetingsContext);
            Platform.runLater(() -> {
                addBotMessage(answer);
                sendBtn.setDisable(false);
                statusLabel.setText("");
            });
        }).start();
    }

    private void addUserMessage(String text) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(320);
        lbl.setStyle("-fx-background-color:#a12c2f;-fx-text-fill:white;-fx-padding:10 14;"
            + "-fx-background-radius:12 12 2 12;-fx-font-size:13px;");
        HBox row = new HBox(lbl);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(4, 8, 4, 40));
        chatContainer.getChildren().add(row);
        scrollToBottom();
    }

    private void addBotMessage(String text) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(320);
        lbl.setStyle("-fx-background-color:#2a2a3e;-fx-text-fill:#e2e8f0;-fx-padding:10 14;"
            + "-fx-background-radius:12 12 12 2;-fx-font-size:13px;");
        HBox row = new HBox(lbl);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 40, 4, 8));
        chatContainer.getChildren().add(row);
        scrollToBottom();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> chatScroll.setVvalue(1.0));
    }

    private String safe(String s) { return s != null ? s : ""; }

    @FXML
    public void handleClose() {
        ((Stage) closeBtn.getScene().getWindow()).close();
    }
}
