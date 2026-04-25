package org.example.controller;

import org.example.config.DatabaseConfig;
import org.example.dao.CandidatureDAO;
import org.example.dao.OfferDAO;
import org.example.dao.ProjectDAO;
import org.example.dao.UserDAO;
import org.example.model.Offer;
import org.example.model.User;
import org.example.service.GeminiService;
import org.example.util.NavigationUtil;
import org.example.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.sql.ResultSet;
import java.util.List;
import java.util.ResourceBundle;

public class EstablishmentDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label userCountLabel;
    @FXML private Label projectCountLabel;
    @FXML private Label applicationCountLabel;
    @FXML private Label offerCountLabel;
    @FXML private BorderPane contentArea;
    @FXML private VBox homePane;

    @FXML private TextArea aiSuggestionsArea;
    @FXML private VBox estChatMessages;
    @FXML private TextField estChatInput;
    @FXML private ScrollPane estChatScroll;

    private final UserDAO userDAO = new UserDAO();
    private final ProjectDAO projectDAO = new ProjectDAO();
    private final OfferDAO offerDAO = new OfferDAO();
    private final CandidatureDAO candidatureDAO = new CandidatureDAO();
    private final GeminiService geminiService = new GeminiService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = SessionManager.getCurrentUser();
        if (user != null) welcomeLabel.setText("WELCOME " + user.getName().toUpperCase());
        loadStats();
        if (estChatMessages != null) {
            addBotMessage("Hi! I'm your SmartPFE assistant. Ask me anything or pick a suggestion below.");
            Platform.runLater(this::showEstSuggestedQuestions);
        }
    }

    private void loadStats() {
        try {
            userCountLabel.setText(String.valueOf(userDAO.findAll().size()));
            projectCountLabel.setText(String.valueOf(projectDAO.findAll().size()));
            User user = SessionManager.getCurrentUser();
            int estId = resolveEstId(user);
            List<Offer> myOffers = offerDAO.findByEstablishment(estId);
            long activeOffers = myOffers.stream()
                .filter(o -> "open".equals(o.getStatus()) || "published".equals(o.getStatus())).count();
            if (offerCountLabel != null) offerCountLabel.setText(String.valueOf(activeOffers));
            long pendingApps = myOffers.stream().mapToLong(o -> {
                try { return candidatureDAO.findByOffer(o.getId()).stream()
                    .filter(c -> "pending".equals(c.getStatus())).count();
                } catch (Exception e) { return 0; }
            }).sum();
            applicationCountLabel.setText(String.valueOf(pendingApps));
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── AI Suggestions ────────────────────────────────────────────────────────

    @FXML public void handleAISuggestions() {
        if (aiSuggestionsArea == null) return;
        aiSuggestionsArea.setText("Generating AI suggestions...");
        new Thread(() -> {
            try {
                User user = SessionManager.getCurrentUser();
                int estId = resolveEstId(user);
                List<Offer> offers = offerDAO.findByEstablishment(estId);
                long total = offers.size();
                long open = offers.stream().filter(o -> "open".equals(o.getStatus())).count();
                long pending = offers.stream().mapToLong(o -> {
                    try { return candidatureDAO.findByOffer(o.getId()).stream()
                        .filter(c -> "pending".equals(c.getStatus())).count();
                    } catch (Exception e) { return 0; }
                }).sum();
                String context = String.format(
                    "Institution has %d total offers (%d open), %d pending applications. Recent offers: %s",
                    total, open, pending,
                    offers.stream().limit(3).map(Offer::getTitle).reduce("", (a, b) -> a + ", " + b));
                String suggestions = geminiService.suggestImprovements(context,
                    "Improve offer visibility and candidate quality", "Project management, internship coordination");
                Platform.runLater(() -> aiSuggestionsArea.setText(suggestions));
            } catch (Exception e) {
                Platform.runLater(() -> aiSuggestionsArea.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    // ── Chatbot ───────────────────────────────────────────────────────────────

    @FXML public void handleEstChatSend() {
        if (estChatInput == null || estChatInput.getText().isBlank()) return;
        String msg = estChatInput.getText().trim();
        estChatInput.clear();
        addUserMessage(msg);
        new Thread(() -> {
            String reply = generateReply(msg);
            Platform.runLater(() -> { addBotMessage(reply); showEstSuggestedQuestions(); });
        }).start();
    }

    public void handleEstSuggestedQuestion(String question) {
        addUserMessage(question);
        new Thread(() -> {
            String reply = generateReply(question);
            Platform.runLater(() -> { addBotMessage(reply); showEstSuggestedQuestions(); });
        }).start();
    }

    private void showEstSuggestedQuestions() {
        if (estChatMessages == null) return;
        String[] suggestions = {"My active offers", "Pending applications", "Average AI score", "View calendar", "Statistics"};
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_RIGHT);
        for (String q : suggestions) {
            Button btn = new Button(q);
            btn.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #a12c2f; -fx-background-radius: 12; " +
                "-fx-padding: 4 10; -fx-font-size: 10px; -fx-cursor: hand; -fx-border-color: #a12c2f33; " +
                "-fx-border-radius: 12; -fx-border-width: 1;");
            btn.setOnAction(e -> handleEstSuggestedQuestion(q));
            row.getChildren().add(btn);
        }
        VBox.setMargin(row, new Insets(2, 0, 6, 0));
        estChatMessages.getChildren().add(row);
        if (estChatScroll != null) Platform.runLater(() -> estChatScroll.setVvalue(1.0));
    }

    private String generateReply(String msg) {
        String lower = msg.toLowerCase();
        User user = SessionManager.getCurrentUser();
        int estId = resolveEstId(user);

        if (lower.contains("offer") || lower.contains("active")) {
            try {
                List<Offer> offers = offerDAO.findByEstablishment(estId);
                long open = offers.stream().filter(o -> "open".equals(o.getStatus())).count();
                return "You have " + offers.size() + " total offer(s), " + open + " currently open. Go to OFFERS to manage them.";
            } catch (Exception e) { return "Go to OFFERS to manage your offers."; }
        }
        if (lower.contains("application") || lower.contains("pending") || lower.contains("candidature")) {
            try {
                List<Offer> offers = offerDAO.findByEstablishment(estId);
                long total = offers.stream().mapToLong(o -> { try { return candidatureDAO.findByOffer(o.getId()).size(); } catch (Exception e2) { return 0; } }).sum();
                long pending = offers.stream().mapToLong(o -> { try { return candidatureDAO.findByOffer(o.getId()).stream().filter(c -> "pending".equals(c.getStatus())).count(); } catch (Exception e2) { return 0; } }).sum();
                return "You have " + total + " total application(s), " + pending + " pending review. Go to APPLICATIONS.";
            } catch (Exception e) { return "Go to APPLICATIONS to review candidatures."; }
        }
        if (lower.contains("score") || lower.contains("average") || lower.contains("ai")) {
            return "Go to APPLICATIONS > Statistics to see average AI scores, conversion rates, and score distributions.";
        }
        if (lower.contains("statistic") || lower.contains("stat")) {
            return "Click 'Statistics' in APPLICATIONS to see conversion rates, score distributions, and monthly trends.";
        }
        if (lower.contains("calendar") || lower.contains("deadline")) {
            return "Go to CALENDAR to see offer deadlines and scheduled interviews on a monthly view.";
        }
        if (lower.contains("compare") || lower.contains("comparison")) {
            return "In APPLICATIONS, select 2-3 candidates (Ctrl+Click) then click 'Compare' for a side-by-side comparison.";
        }
        if (lower.contains("email") || lower.contains("notification")) {
            return "Emails are sent automatically to students on every status change (accepted, rejected, interview).";
        }
        if (lower.contains("hello") || lower.contains("hi")) {
            return "Hello " + user.getName() + "! How can I help you manage your offers and applications today?";
        }
        if (lower.contains("help")) {
            return "I can help with:\n• Offers management\n• Applications review\n• AI Matching scores\n• Statistics\n• Calendar\n• Candidate comparison";
        }
        return "Try asking about: offers, applications, AI score, statistics, calendar, or comparison.";
    }

    private void addBotMessage(String text) {
        if (estChatMessages == null) return;
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        Label avatar = new Label("🤖");
        avatar.setStyle("-fx-font-size: 14px;");
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(220);
        lbl.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #1a1a2e; -fx-background-radius: 0 10 10 10; -fx-padding: 7 10; -fx-font-size: 11px;");
        row.getChildren().addAll(avatar, lbl);
        VBox.setMargin(row, new Insets(2, 0, 2, 0));
        estChatMessages.getChildren().add(row);
        if (estChatScroll != null) Platform.runLater(() -> estChatScroll.setVvalue(1.0));
    }

    private void addUserMessage(String text) {
        if (estChatMessages == null) return;
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(220);
        lbl.setStyle("-fx-background-color: #a12c2f; -fx-text-fill: white; -fx-background-radius: 10 0 10 10; -fx-padding: 7 10; -fx-font-size: 11px;");
        row.getChildren().add(lbl);
        VBox.setMargin(row, new Insets(2, 0, 2, 0));
        estChatMessages.getChildren().add(row);
        if (estChatScroll != null) Platform.runLater(() -> estChatScroll.setVvalue(1.0));
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void load(String fxml) {
        if (contentArea.getCenter() != null) contentArea.getCenter().setVisible(false);
        contentArea.setCenter(null);
        Pane pane = NavigationUtil.loadPane(fxml);
        if (pane != null) { contentArea.setCenter(pane); pane.setVisible(true); }
    }

    @FXML public void showHome() {
        if (contentArea.getCenter() != null) contentArea.getCenter().setVisible(false);
        contentArea.setCenter(null);
        javafx.scene.Node node = homePane.getParent() instanceof ScrollPane sp ? sp : homePane;
        contentArea.setCenter(node);
        if (node != null) node.setVisible(true);
        loadStats();
    }

    @FXML public void showUsers()        { load("Users.fxml"); }
    @FXML public void showProjects()     { load("Projects.fxml"); }
    @FXML public void showOffers()       { load("EstablishmentOffers.fxml"); }
    @FXML public void showApplications() { load("EstablishmentCandidatures.fxml"); }
    @FXML public void showCalendar()     { load("OfferCalendar.fxml"); }
    @FXML public void showProfile()      { load("Users.fxml"); }

    @FXML public void handleLogout() {
        SessionManager.logout();
        NavigationUtil.navigateTo("Home.fxml");
    }

    private int resolveEstId(User user) {
        if (user.getEstablishmentId() != 0) return user.getEstablishmentId();
        try {
            ResultSet rs = DatabaseConfig.getConnection().createStatement()
                .executeQuery("SELECT establishment_id FROM users WHERE id = " + user.getId());
            if (rs.next()) return rs.getInt(1);
        } catch (Exception ignored) {}
        return user.getId();
    }
}
