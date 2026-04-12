package org.example.controller;

import org.example.dao.ProjectDAO;
import org.example.dao.TaskDAO;
import org.example.model.Project;
import org.example.model.User;
import org.example.util.NavigationUtil;
import org.example.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class SupervisorDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label projectCountLabel;
    @FXML private Label taskCountLabel;
    @FXML private Label meetingCountLabel;
    @FXML private BorderPane contentArea;
    @FXML private VBox homePane;
    
    // Navigation labels
    @FXML private HBox navBar;
    @FXML private Label navHome;
    @FXML private Label navMeetings;
    @FXML private Label navProjects;
    @FXML private Label navTasks;
    @FXML private Label navDocuments;
    @FXML private Label navComments;
    @FXML private Label navProfile;

    private final ProjectDAO projectDAO = new ProjectDAO();
    private final TaskDAO taskDAO = new TaskDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            welcomeLabel.setText("WELCOME " + user.getName().toUpperCase());
        }
        loadStats();
    }

    private void loadStats() {
        try {
            User user = SessionManager.getCurrentUser();
            List<Project> projects = projectDAO.findBySupervisor(user.getId());
            projectCountLabel.setText(String.valueOf(projects.size()));
            taskCountLabel.setText(String.valueOf(taskDAO.findAll().size()));
            meetingCountLabel.setText("0");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setActiveNav(Label activeLabel) {
        navHome.setStyle("-fx-font-size: 12px; -fx-text-fill: #ccc; -fx-cursor: hand;");
        navMeetings.setStyle("-fx-font-size: 12px; -fx-text-fill: #ccc; -fx-cursor: hand;");
        navProjects.setStyle("-fx-font-size: 12px; -fx-text-fill: #ccc; -fx-cursor: hand;");
        navComments.setStyle("-fx-font-size: 12px; -fx-text-fill: #ccc; -fx-cursor: hand;");
        navDocuments.setStyle("-fx-font-size: 12px; -fx-text-fill: #ccc; -fx-cursor: hand;");
        navTasks.setStyle("-fx-font-size: 12px; -fx-text-fill: #ccc; -fx-cursor: hand;");
        navProfile.setStyle("-fx-font-size: 12px; -fx-text-fill: #ccc; -fx-cursor: hand;");
        
        activeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #a12c2f; -fx-cursor: hand;");
    }

    @FXML public void showHome() { 
        setActiveNav(navHome);
        if (contentArea.getCenter() != null) contentArea.getCenter().setVisible(false);
        contentArea.setCenter(null);
        javafx.scene.Node node = homePane.getParent() instanceof javafx.scene.control.ScrollPane sp ? sp : homePane;
        contentArea.setCenter(node);
        if (node != null) node.setVisible(true);
    }
    
    @FXML public void showMeetings() { 
        setActiveNav(navMeetings);
        if (contentArea.getCenter() != null) contentArea.getCenter().setVisible(false);
        contentArea.setCenter(null);
        javafx.scene.layout.Pane pane = NavigationUtil.loadPane("Meetings.fxml");
        if (pane != null) {
            contentArea.setCenter(pane);
            pane.setVisible(true);
        }
    }
    
    @FXML public void showProjects() { 
        setActiveNav(navProjects);
        if (contentArea.getCenter() != null) contentArea.getCenter().setVisible(false);
        contentArea.setCenter(null);
        javafx.scene.layout.Pane pane = NavigationUtil.loadPane("Projects.fxml");
        if (pane != null) {
            contentArea.setCenter(pane);
            pane.setVisible(true);
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
        }
    }
    
    @FXML public void showDocuments() { 
        setActiveNav(navDocuments);
        if (contentArea.getCenter() != null) contentArea.getCenter().setVisible(false);
        contentArea.setCenter(null);
        javafx.scene.layout.Pane pane = NavigationUtil.loadPane("Documents.fxml");
        if (pane != null) {
            contentArea.setCenter(pane);
            pane.setVisible(true);
        }
    }
    
    @FXML public void showComments() { 
        setActiveNav(navComments);
        if (contentArea.getCenter() != null) contentArea.getCenter().setVisible(false);
        contentArea.setCenter(null);
        javafx.scene.layout.Pane pane = NavigationUtil.loadPane("Comments.fxml");
        if (pane != null) {
            contentArea.setCenter(pane);
            pane.setVisible(true);
        }
    }
    
    @FXML public void showProfile() { 
        setActiveNav(navProfile);
        if (contentArea.getCenter() != null) contentArea.getCenter().setVisible(false);
        contentArea.setCenter(null);
        javafx.scene.layout.Pane pane = NavigationUtil.loadPane("Users.fxml");
        if (pane != null) {
            contentArea.setCenter(pane);
            pane.setVisible(true);
        }
    }

    @FXML
    public void handleLogout() {
        SessionManager.logout();
        NavigationUtil.navigateTo("Home.fxml");
    }
}
