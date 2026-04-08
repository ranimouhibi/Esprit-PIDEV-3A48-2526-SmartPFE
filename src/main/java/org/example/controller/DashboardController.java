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
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

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

    private final ProjectDAO projectDAO = new ProjectDAO();
    private final SprintDAO sprintDAO = new SprintDAO();
    private final TaskDAO taskDAO = new TaskDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            welcomeLabel.setText("Bonjour, " + user.getName());
            roleLabel.setText("Rôle: " + user.getRole().toUpperCase());
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

    @FXML public void showProjects() { loadContent("Projects.fxml"); }
    @FXML public void showSprints() { loadContent("Sprints.fxml"); }
    @FXML public void showTasks() { loadContent("Tasks.fxml"); }
    @FXML public void showUsers() { loadContent("Users.fxml"); }
    @FXML public void showMeetings() { loadContent("Meetings.fxml"); }
    @FXML public void showCandidatures() { loadContent("Candidatures.fxml"); }

    private void loadContent(String fxml) {
        Pane pane = NavigationUtil.loadPane(fxml);
        contentArea.setCenter(pane);
    }

    @FXML
    public void handleLogout() {
        SessionManager.logout();
        NavigationUtil.navigateTo("Login.fxml");
    }
}
