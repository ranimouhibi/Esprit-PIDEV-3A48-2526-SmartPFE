package org.example.controller;

import org.example.config.DatabaseConfig;
import org.example.dao.CandidatureDAO;
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
import javafx.scene.paint.Color;

import java.net.URL;
import java.sql.ResultSet;
import java.util.List;
import java.util.ResourceBundle;

public class StudentDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label projectCountLabel;
    @FXML private Label taskCountLabel;
    @FXML private Label sprintCountLabel;
    @FXML private Label meetingCountLabel;
    // Extended stats
    @FXML private Label taskInProgressLabel;
    @FXML private Label taskDoneLabel;
    @FXML private Label upcomingMeetingsLabel;
    @FXML private Label activeProjectsLabel;
    @FXML private BorderPane contentArea;
    @FXML private VBox homePane;

    // Chatbot
    @FXML private VBox chatbotPanel;
    @FXML private Button chatFab;


    @FXML private VBox chatMessages;
    @FXML private TextField chatInput;
    @FXML private ScrollPane chatScroll;

    // Navigation labels
    @FXML private HBox navBar;
    @FXML private Label navHome;
    @FXML private Label navMeetings;
    @FXML private Label navOffers;
    @FXML private Label navProjects;
    @FXML private Label navSprints;
    @FXML private Label navTasks;
    @FXML private Label navDocuments;
    @FXML private Label navApplications;
    @FXML private Label navProfile;

    private final ProjectDAO projectDAO = new ProjectDAO();
    private final TaskDAO taskDAO = new TaskDAO();
    private final SprintDAO sprintDAO = new SprintDAO();
    private final CandidatureDAO candidatureDAO = new CandidatureDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = SessionManager.getCurrentUser();
        if (user != null) welcomeLabel.setText("WELCOME " + user.getName().toUpperCase());
        loadStats();
    }

    private void loadStats() {
        try {
            User user = SessionManager.getCurrentUser();
            List<Project> projects = projectDAO.findByOwner(user.getId());
            long active = projects.stream().filter(p -> "active".equals(p.getStatus())).count();

            if (projectCountLabel != null) projectCountLabel.setText(String.valueOf(projects.size()));
            if (activeProjectsLabel != null) activeProjectsLabel.setText(String.valueOf(active));

            // Tasks in progress / done for this student
            long inProgress = 0, done = 0;
            try (ResultSet rs = DatabaseConfig.getConnection().createStatement().executeQuery(
                "SELECT status, COUNT(*) as cnt FROM tasks WHERE assigned_to_id = " + user.getId() + " GROUP BY status")) {
                while (rs.next()) {
                    String s = rs.getString("status");
                    int cnt = rs.getInt("cnt");
                    if ("in_progress".equals(s)) inProgress = cnt;
                    else if ("done".equals(s)) done = cnt;
                }
            }
            if (taskCountLabel != null) taskCountLabel.setText(String.valueOf(inProgress + done));
            if (taskInProgressLabel != null) taskInProgressLabel.setText(String.valueOf(inProgress));
            if (taskDoneLabel != null) taskDoneLabel.setText(String.valueOf(done));

            // Upcoming meetings
            long upcoming = 0;
            try (ResultSet rs = DatabaseConfig.getConnection().createStatement().executeQuery(
                "SELECT COUNT(*) FROM meetings WHERE scheduled_date >= NOW() AND status = 'scheduled'")) {
                if (rs.next()) upcoming = rs.getLong(1);
            }
            if (meetingCountLabel != null) meetingCountLabel.setText(String.valueOf(upcoming));
            if (upcomingMeetingsLabel != null) upcomingMeetingsLabel.setText(String.valueOf(upcoming));
            if (sprintCountLabel != null) sprintCountLabel.setText(String.valueOf(sprintDAO.findAll().size()));

        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Chatbot ───────────────────────────────────────────────────────────────

    @FXML public void handleChatToggle() {
        if (chatbotPanel == null) return;
        boolean show = !chatbotPanel.isVisible();
        chatbotPanel.setVisible(show);
        chatbotPanel.setManaged(show);
        if (chatFab != null) chatFab.setText(show ? "✕" : "🤖");
        if (show && chatMessages != null && chatMessages.getChildren().isEmpty()) {
            addBotMessage("Hi! I'm your SmartPFE assistant. Ask me anything or pick a suggestion below.");
            Platform.runLater(this::showSuggestedQuestions);
        }
        if (show) scrollChatToBottom();
    }

    private void setActiveNav(Label activeLabel) {
        // Reset all
        for (Label l : new Label[]{navHome, navMeetings, navOffers, navProjects,
                                    navSprints, navTasks, navDocuments, navProfile}) {
            l.setStyle("-fx-font-size: 12px; -fx-text-fill: #ccc; -fx-cursor: hand; -fx-font-weight: normal;");
        }
        // Set active
        activeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #a12c2f; -fx-cursor: hand;");
    @FXML public void handleChatSend() {
        String msg = chatInput.getText().trim();
        chatInput.clear();
        addUserMessage(msg);
        new Thread(() -> {
            String reply = generateBotReply(msg);
            Platform.runLater(() -> {
                addBotMessage(reply);
                showSuggestedQuestions();
            });
        }).start();
    }

    public void handleSuggestedQuestion(String question) {
        addUserMessage(question);
        new Thread(() -> {
            String reply = generateBotReply(question);
            Platform.runLater(() -> {
                addBotMessage(reply);
                showSuggestedQuestions();
            });
        }).start();
    }
    
    @FXML public void showMeetings() { 
        setActiveNav(navMeetings);
        if (contentArea.getCenter() != null) contentArea.getCenter().setVisible(false);
        contentArea.setCenter(null);
        System.out.println("DEBUG: Loading Meetings.fxml...");
        javafx.scene.layout.Pane pane = NavigationUtil.loadPane("Meetings.fxml");
        System.out.println("DEBUG: Pane loaded, children count: " + (pane != null ? pane.getChildren().size() : "null"));
        if (pane != null) {
            contentArea.setCenter(pane);
            pane.setVisible(true);
        } else {
            System.err.println("ERROR: Pane is null!");

    private void showSuggestedQuestions() {
        if (chatMessages == null) return;
        String[] suggestions = {
            "How many offers?",
            "My applications",
            "AI matching score",
            "Export PDF",
            "Upcoming meetings"
        };
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
        chatMessages.getChildren().add(row);
        scrollChatToBottom();
    }

    private String generateBotReply(String msg) {
        String lower = msg.toLowerCase();
        User user = SessionManager.getCurrentUser();

        if (lower.contains("offer") || lower.contains("offre") || lower.contains("how many")) {
            try {
                int count = new org.example.dao.OfferDAO().findAllOpen().size();
                return "There are currently " + count + " open offer(s) available. Go to the OFFERS section to browse and apply!";
            } catch (Exception e) { return "Go to the OFFERS section to browse available offers."; }
        }
        if (lower.contains("candidature") || lower.contains("application") || lower.contains("my application")) {
            try {
                int count = candidatureDAO.findByStudent(user.getId()).size();
                return "You have " + count + " application(s) submitted. Check MY CANDIDATURES for status updates.";
            } catch (Exception e) { return "Go to MY CANDIDATURES to track your applications."; }
        }
        if (lower.contains("project") || lower.contains("projet")) {
            try {
                int count = projectDAO.findByOwner(user.getId()).size();
                return "You have " + count + " project(s). Go to MY PROJECTS to manage them.";
            } catch (Exception e) { return "Go to MY PROJECTS to view your projects."; }
        }
    }
    
    @FXML public void showTasks() { 
        setActiveNav(navTasks);
        if (contentArea.getCenter() != null) contentArea.getCenter().setVisible(false);
        contentArea.setCenter(null);
        javafx.scene.layout.Pane pane = NavigationUtil.loadPane("Tasks.fxml");
        if (pane != null) {
            contentArea.setCenter(pane);
            pane.setVisible(true);
        if (lower.contains("task") || lower.contains("tâche")) {
            return "Go to MY PROJECTS > Tasks to see your assigned tasks and update their status.";
        }
        if (lower.contains("meeting") || lower.contains("upcoming")) {
            return "Go to MEETINGS to schedule or view upcoming meetings with your supervisor.";
        }
        if (lower.contains("cv") || lower.contains("resume")) {
            return "When applying for an offer, you can upload your CV (PDF or Word). Make sure it's up to date!";
        }
        if (lower.contains("score") || lower.contains("matching") || lower.contains("ai")) {
            return "After applying, your candidature gets an AI Matching Score based on your skills vs offer requirements. Check MY CANDIDATURES to see your score!";
        }
        if (lower.contains("pdf") || lower.contains("export")) {
            return "You can export your candidature as a PDF from MY CANDIDATURES. Click the PDF button on any application card.";
        }
        if (lower.contains("deadline") || lower.contains("calendar")) {
            return "Check offer deadlines in the OFFERS section (Calendar tab). Deadlines are shown in purple.";
        }
        if (lower.contains("hello") || lower.contains("hi") || lower.contains("bonjour")) {
            return "Hello " + user.getName() + "! How can I help you today?";
        }
        if (lower.contains("help") || lower.contains("aide")) {
            return "I can help with:\n• Offers & Applications\n• Projects & Tasks\n• Meetings\n• AI Matching Score\n• PDF Export\nJust ask!";
        }
        if (lower.contains("statistic") || lower.contains("stat")) {
            return "Check the Statistics tab in the OFFERS section to see monthly charts of your applications.";
        }
        return "I'm not sure about that. Try asking about: offers, applications, projects, tasks, meetings, or AI score.";
    }
    
    @FXML public void showSprints() { 
        setActiveNav(navSprints);

    private void addBotMessage(String text) {
        if (chatMessages == null) return;
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        Label avatar = new Label("🤖");
        avatar.setStyle("-fx-font-size: 16px;");
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(260);
        lbl.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #1a1a2e; -fx-background-radius: 0 12 12 12; " +
            "-fx-padding: 8 12; -fx-font-size: 12px;");
        row.getChildren().addAll(avatar, lbl);
        VBox.setMargin(row, new Insets(2, 0, 2, 0));
        chatMessages.getChildren().add(row);
        scrollChatToBottom();
    }

    private void addUserMessage(String text) {
        if (chatMessages == null) return;
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(260);
        lbl.setStyle("-fx-background-color: #a12c2f; -fx-text-fill: white; -fx-background-radius: 12 0 12 12; " +
            "-fx-padding: 8 12; -fx-font-size: 12px;");
        row.getChildren().add(lbl);
        VBox.setMargin(row, new Insets(2, 0, 2, 0));
        chatMessages.getChildren().add(row);
        scrollChatToBottom();
    }

    private void scrollChatToBottom() {
        if (chatScroll != null) Platform.runLater(() -> chatScroll.setVvalue(1.0));
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
        Label[] all = {navHome, navMeetings, navOffers, navProjects, navDocuments, navApplications, navProfile};
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

    @FXML public void showMeetings()       { setActiveNav(navMeetings);      load("Meetings.fxml"); }
    @FXML public void showOffers()         { setActiveNav(navOffers);        load("StudentOffers.fxml"); }
    @FXML public void showMyCandidatures() { setActiveNav(navApplications);  load("MyCandidatures.fxml"); }
    @FXML public void showProjects()       { setActiveNav(navProjects);       load("StudentProjects.fxml"); }
    @FXML public void showDocuments()      { setActiveNav(navDocuments);      load("Documents.fxml"); }
    @FXML public void showTasks()          { load("Tasks.fxml"); }
    @FXML public void showSprints()        { load("Sprints.fxml"); }
    @FXML public void showProfile()        { setActiveNav(navProfile);        load("Users.fxml"); }

    @FXML public void handleLogout() {
        SessionManager.logout();
        NavigationUtil.navigateTo("Home.fxml");
    }
}
