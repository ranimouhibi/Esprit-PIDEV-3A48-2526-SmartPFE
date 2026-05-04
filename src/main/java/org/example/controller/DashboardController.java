package org.example.controller;

import org.example.dao.ProjectDAO;
import org.example.dao.SprintDAO;
import org.example.dao.TaskDAO;
import org.example.dao.UserDAO;
import org.example.model.User;
import org.example.util.NavigationUtil;
import org.example.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Label projectCountLabel;
    @FXML private Label sprintCountLabel;
    @FXML private Label taskCountLabel;
    @FXML private Label userCountLabel;
    @FXML private BorderPane contentArea;
    @FXML private VBox statsPane;

    // Sidebar buttons
    @FXML private Button btnProjects;
    @FXML private Button btnComments;
    @FXML private Button btnDocuments;
    @FXML private Button btnSprints;
    @FXML private Button btnTasks;
    @FXML private Button btnMeetings;
    @FXML private Button btnOffers;
    @FXML private Button btnProjectOffers;
    @FXML private Button btnCandidatures;
    @FXML private Button btnUsers;

    private static final String STYLE_INACTIVE = "-fx-background-color: transparent; -fx-text-fill: #ccc; -fx-alignment: CENTER-LEFT; -fx-cursor: hand; -fx-font-size: 13px; -fx-background-radius: 8; -fx-padding: 9 12;";
    private static final String STYLE_ACTIVE   = "-fx-background-color: #a12c2f; -fx-text-fill: white; -fx-alignment: CENTER-LEFT; -fx-cursor: hand; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 9 12;";

    private final ProjectDAO projectDAO = new ProjectDAO();
    private final SprintDAO sprintDAO = new SprintDAO();
    private final TaskDAO taskDAO = new TaskDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            welcomeLabel.setText("Hello, " + user.getName());
            roleLabel.setText("Role: " + user.getRole().toUpperCase());
        }
        loadStats();
    }

    private void loadStats() {
        try {
            projectCountLabel.setText(String.valueOf(projectDAO.findAll().size()));
            sprintCountLabel.setText(String.valueOf(sprintDAO.findAll().size()));
            taskCountLabel.setText(String.valueOf(taskDAO.findAll().size()));
            userCountLabel.setText(String.valueOf(userDAO.findAll().size()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setActiveButton(Button active) {
        Button[] all = {btnProjects, btnComments, btnDocuments, btnSprints, btnTasks, btnMeetings, btnOffers, btnProjectOffers, btnCandidatures, btnUsers};
        for (Button b : all) { b.setStyle(STYLE_INACTIVE); }
        active.setStyle(STYLE_ACTIVE);
    }

    @FXML public void showDashboard() {
        Button[] all = {btnProjects, btnComments, btnDocuments, btnSprints, btnTasks, btnMeetings, btnOffers, btnProjectOffers, btnCandidatures, btnUsers};
        for (Button b : all) b.setStyle(STYLE_INACTIVE);
        contentArea.setCenter(statsPane);
        loadStats();
    }

    @FXML public void showProjects()     { setActiveButton(btnProjects);      loadContent("Projects.fxml"); }
    @FXML public void showComments()     { setActiveButton(btnComments);      loadContent("Comments.fxml"); }
    @FXML public void showDocuments()    { setActiveButton(btnDocuments);     loadContent("Documents.fxml"); }
    @FXML public void showSprints()      { setActiveButton(btnSprints);       loadContent("Sprints.fxml"); }
    @FXML public void showTasks()        { setActiveButton(btnTasks);         loadContent("Tasks.fxml"); }
    @FXML public void showMeetings()     { setActiveButton(btnMeetings);      loadContent("Meetings.fxml"); }
    @FXML public void showOffers()       { setActiveButton(btnOffers);        loadContent("Offers.fxml"); }
    @FXML public void showProjectOffers(){ setActiveButton(btnProjectOffers); loadContent("ProjectOffers.fxml"); }
    @FXML public void showCandidatures() { setActiveButton(btnCandidatures);  loadContent("Candidatures.fxml"); }
    @FXML public void showUsers()        { setActiveButton(btnUsers);         loadContent("Users.fxml"); }
    @FXML public void showStudentOffers()  { loadContent("StudentOffers.fxml"); }
    @FXML public void showMyCandidatures() { loadContent("MyCandidatures.fxml"); }
    @FXML public void showAuditLog()       { loadContent("AuditLog.fxml"); }

    private void loadContent(String fxml) {
        // Clear old content completely
        if (contentArea.getCenter() != null) {
            contentArea.getCenter().setVisible(false);
            contentArea.setCenter(null);
        }
        
        // Load new content
        Pane pane = NavigationUtil.loadPane(fxml);
        if (pane != null) {
            contentArea.setCenter(pane);
            pane.setVisible(true);
        }
    }

    @FXML
    public void handleLogout() {
        SessionManager.logout();
        NavigationUtil.navigateTo("Login.fxml");
    }
}
