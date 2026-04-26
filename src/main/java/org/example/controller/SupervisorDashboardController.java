package org.example.controller;

import org.example.config.DatabaseConfig;
import org.example.dao.ProjectDAO;
import org.example.dao.SprintDAO;
import org.example.dao.TaskDAO;
import org.example.model.Project;
import org.example.model.User;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.ResourceBundle;

public class SupervisorDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label projectCountLabel;
    @FXML private Label taskCountLabel;
    @FXML private Label meetingCountLabel;
    @FXML private Label pendingRequestsLabel;
    @FXML private BorderPane contentArea;
    @FXML private VBox homePane;

    // Chatbot
    @FXML private VBox supChatMessages;
    @FXML private TextField supChatInput;
    @FXML private ScrollPane supChatScroll;
    @FXML private VBox supChatPanel;
    @FXML private Button supChatFab;



    // Navigation labels
    @FXML private HBox navBar;
    @FXML private Label navHome;
    @FXML private Label navMeetings;
    @FXML private Label navProjects;
    @FXML private Label navSprints;
    @FXML private Label navTasks;
    @FXML private Label navDocuments;
    @FXML private Label navComments;
    @FXML private Label navCandidatures;
    @FXML private Label navProfile;

    private final ProjectDAO projectDAO = new ProjectDAO();
    private final TaskDAO taskDAO = new TaskDAO();
    private final SprintDAO sprintDAO = new SprintDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = SessionManager.getCurrentUser();
        if (user != null) welcomeLabel.setText("WELCOME " + user.getName().toUpperCase());
        loadStats();
    }

    @FXML public void handleSupChatToggle() {
        if (supChatPanel == null) return;
        boolean show = !supChatPanel.isVisible();
        supChatPanel.setVisible(show);
        supChatPanel.setManaged(show);
        if (supChatFab != null) supChatFab.setText(show ? "✕" : "🤖");
        if (show && supChatMessages != null && supChatMessages.getChildren().isEmpty()) {
            addBotMessage("Hi! I'm your SmartPFE assistant. Ask me anything or pick a suggestion below.");
            Platform.runLater(this::showSuggestedQuestions);
        }
        if (show && supChatScroll != null) Platform.runLater(() -> supChatScroll.setVvalue(1.0));
    }

    private void loadStats() {
        try {
            User user = SessionManager.getCurrentUser();
            List<Project> projects = projectDAO.findBySupervisor(user.getId());
            projectCountLabel.setText(String.valueOf(projects.size()));

            // Tasks for supervised projects
            long taskCount = projects.stream().mapToLong(p -> {
                try { return taskDAO.findByProject(p.getId()).size(); } catch (Exception e) { return 0; }
            }).sum();
            taskCountLabel.setText(String.valueOf(taskCount));

            // Upcoming meetings
            long meetings = 0;
            try (ResultSet rs = DatabaseConfig.getConnection().createStatement().executeQuery(
                "SELECT COUNT(*) FROM meetings WHERE scheduled_date >= NOW() AND status = 'scheduled'")) {
                if (rs.next()) meetings = rs.getLong(1);
            }
            meetingCountLabel.setText(String.valueOf(meetings));

            // Pending supervision requests
            long pending = 0;
            try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM supervision_requests WHERE supervisor_id = ? AND status = 'pending'")) {
                ps.setInt(1, user.getId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) pending = rs.getLong(1);
            } catch (Exception ignored) {}
            if (pendingRequestsLabel != null) pendingRequestsLabel.setText(String.valueOf(pending));

        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Chatbot ───────────────────────────────────────────────────────────────

    @FXML public void handleSupChatSend() {
        if (supChatInput == null || supChatInput.getText().isBlank()) return;
        String msg = supChatInput.getText().trim();
        supChatInput.clear();
        addUserMessage(msg);
        new Thread(() -> {
            String reply = generateReply(msg);
            Platform.runLater(() -> { addBotMessage(reply); showSuggestedQuestions(); });
        }).start();
    }

    public void handleSuggestedQuestion(String question) {
        addUserMessage(question);
        new Thread(() -> {
            String reply = generateReply(question);
            Platform.runLater(() -> { addBotMessage(reply); showSuggestedQuestions(); });
        }).start();
    }

    private void showSuggestedQuestions() {
        if (supChatMessages == null) return;
        String[] suggestions = {"My projects", "Pending requests", "Review candidatures", "AI matching score", "Upcoming meetings"};
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_RIGHT);
        for (String q : suggestions) {
            Button btn = new Button(q);
            btn.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #a12c2f; -fx-background-radius: 12; " +
                "-fx-padding: 4 10; -fx-font-size: 10px; -fx-cursor: hand; -fx-border-color: #a12c2f33; " +
                "-fx-border-radius: 12; -fx-border-width: 1;");
            btn.setOnAction(e -> handleSuggestedQuestion(q));
            row.getChildren().add(btn);
        }
        VBox.setMargin(row, new Insets(2, 0, 6, 0));
        supChatMessages.getChildren().add(row);
        if (supChatScroll != null) Platform.runLater(() -> supChatScroll.setVvalue(1.0));
    }

    private String generateReply(String msg) {
        String lower = msg.toLowerCase();
        User user = SessionManager.getCurrentUser();
        try {
            if (lower.contains("project") || lower.contains("supervised")) {
                int count = projectDAO.findBySupervisor(user.getId()).size();
                return "You are supervising " + count + " project(s). Go to MY PROJECTS to view them.";
            }
            if (lower.contains("request") || lower.contains("pending")) {
                return "Check your pending supervision requests in the dashboard stats.";
            }
            if (lower.contains("candidature") || lower.contains("application")) {
                return "Go to CANDIDATURES to review student applications, see AI matching scores, add notes, and make decisions.";
            }
            if (lower.contains("score") || lower.contains("ai") || lower.contains("matching")) {
                return "In CANDIDATURES, select a candidature and click 'Calculate AI Score' to see the matching score, matched/missing skills, and recommendations.";
            }
            if (lower.contains("compare")) {
                return "In CANDIDATURES, select 2-3 students (Ctrl+Click) and click 'Compare' for a side-by-side comparison.";
            }
            if (lower.contains("meeting")) {
                return "Go to MEETINGS to schedule and manage meetings with your students.";
            }
            if (lower.contains("task")) {
                return "Go to TASKS to review and manage student tasks.";
            }
            if (lower.contains("hello") || lower.contains("hi")) {
                return "Hello " + user.getName() + "! How can I help you today?";
            }
            if (lower.contains("help")) {
                return "I can help with:\n• Candidatures & AI scores\n• Supervised projects\n• Meetings\n• Tasks\n• Supervision requests";
            }
        } catch (Exception e) { return "Error: " + e.getMessage(); }
        return "Try asking about: candidatures, AI score, projects, meetings, or tasks.";
    }

    private void addBotMessage(String text) {
        if (supChatMessages == null) return;
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        Label avatar = new Label("🤖");
        avatar.setStyle("-fx-font-size: 14px;");
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(240);
        lbl.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #1a1a2e; -fx-background-radius: 0 10 10 10; -fx-padding: 7 10; -fx-font-size: 11px;");
        row.getChildren().addAll(avatar, lbl);
        VBox.setMargin(row, new Insets(2, 0, 2, 0));
        supChatMessages.getChildren().add(row);
        if (supChatScroll != null) Platform.runLater(() -> supChatScroll.setVvalue(1.0));
    }

    private void addUserMessage(String text) {
        if (supChatMessages == null) return;
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(240);
        lbl.setStyle("-fx-background-color: #a12c2f; -fx-text-fill: white; -fx-background-radius: 10 0 10 10; -fx-padding: 7 10; -fx-font-size: 11px;");
        row.getChildren().add(lbl);
        VBox.setMargin(row, new Insets(2, 0, 2, 0));
        supChatMessages.getChildren().add(row);
        if (supChatScroll != null) Platform.runLater(() -> supChatScroll.setVvalue(1.0));
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void load(String fxml) {
        if (contentArea.getCenter() != null) contentArea.getCenter().setVisible(false);
        contentArea.setCenter(null);
        javafx.scene.layout.Pane pane = NavigationUtil.loadPane(fxml);
        if (pane != null) {
            contentArea.setCenter(pane);
            pane.setVisible(true);
        }
    }

    private void setActiveNav(Label activeLabel) {
        Label[] all = {navHome, navMeetings, navProjects, navSprints, navComments, navDocuments, navTasks, navCandidatures, navProfile};
        for (Label l : all) if (l != null) l.setStyle("-fx-font-size: 12px; -fx-text-fill: #ccc; -fx-cursor: hand;");
        if (activeLabel != null) activeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #a12c2f; -fx-cursor: hand;");
    }

    @FXML public void showHome() {
        setActiveNav(navHome);
        if (contentArea.getCenter() != null) contentArea.getCenter().setVisible(false);
        javafx.scene.Node node = homePane.getParent() instanceof ScrollPane sp ? sp : homePane;
        if (node instanceof ScrollPane sp) {
            sp.setPrefHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
            sp.setMaxHeight(Double.MAX_VALUE);
        }
        contentArea.setCenter(node);
        if (node != null) node.setVisible(true);
        loadStats();
    }

    @FXML public void showMeetings()     { setActiveNav(navMeetings);     load("Meetings.fxml"); }
    @FXML public void showProjects()     { setActiveNav(navProjects);     load("Projects.fxml"); }
    @FXML public void showSprints()      { if (navSprints != null) setActiveNav(navSprints); load("Sprints.fxml"); }
    @FXML public void showTasks()        { setActiveNav(navTasks);        load("Tasks.fxml"); }
    @FXML public void showDocuments()    { setActiveNav(navDocuments);    load("Documents.fxml"); }
    @FXML public void showComments()     { setActiveNav(navComments);     load("Comments.fxml"); }
    @FXML public void showCandidatures() { setActiveNav(navCandidatures); load("SupervisorCandidatures.fxml"); }
    @FXML public void showProfile()      { setActiveNav(navProfile);      load("Users.fxml"); }

    @FXML public void handleLogout() {
        SessionManager.logout();
        NavigationUtil.navigateTo("Home.fxml");
    }
}
