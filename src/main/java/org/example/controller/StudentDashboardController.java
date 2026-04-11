package org.example.controller;

import org.example.dao.ProjectDAO;
import org.example.dao.SprintDAO;
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

public class StudentDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label projectCountLabel;
    @FXML private Label taskCountLabel;
    @FXML private Label sprintCountLabel;
    @FXML private Label meetingCountLabel;
    @FXML private BorderPane contentArea;
    @FXML private VBox homePane;
    
    // Navigation labels
    @FXML private HBox navBar;
    @FXML private Label navHome;
    @FXML private Label navMeetings;
    @FXML private Label navOffers;
    @FXML private Label navProjects;
    @FXML private Label navDocuments;
    @FXML private Label navComments;
    @FXML private Label navProfile;

    private final ProjectDAO projectDAO = new ProjectDAO();
    private final TaskDAO taskDAO = new TaskDAO();
    private final SprintDAO sprintDAO = new SprintDAO();

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
            List<Project> projects = projectDAO.findByOwner(user.getId());
            projectCountLabel.setText(String.valueOf(projects.size()));
            taskCountLabel.setText(String.valueOf(taskDAO.findAll().size()));
            sprintCountLabel.setText(String.valueOf(sprintDAO.findAll().size()));
            meetingCountLabel.setText("0");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setActiveNav(Label activeLabel) {
        // Reset all
        navHome.setStyle("-fx-font-size: 12px; -fx-text-fill: #ccc; -fx-cursor: hand;");
        navMeetings.setStyle("-fx-font-size: 12px; -fx-text-fill: #ccc; -fx-cursor: hand;");
        navOffers.setStyle("-fx-font-size: 12px; -fx-text-fill: #ccc; -fx-cursor: hand;");
        navProjects.setStyle("-fx-font-size: 12px; -fx-text-fill: #ccc; -fx-cursor: hand;");
        navDocuments.setStyle("-fx-font-size: 12px; -fx-text-fill: #ccc; -fx-cursor: hand;");
        navComments.setStyle("-fx-font-size: 12px; -fx-text-fill: #ccc; -fx-cursor: hand;");
        navProfile.setStyle("-fx-font-size: 12px; -fx-text-fill: #ccc; -fx-cursor: hand;");
        
        // Set active
        activeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #a12c2f; -fx-cursor: hand;");
    }

    @FXML public void showHome() { 
        setActiveNav(navHome);
        contentArea.setCenter(homePane.getParent() instanceof javafx.scene.control.ScrollPane sp ? sp : homePane); 
    }
    
    @FXML public void showMeetings() { 
        setActiveNav(navMeetings);
        contentArea.setCenter(NavigationUtil.loadPane("Meetings.fxml")); 
    }
    
    @FXML public void showOffers() { 
        setActiveNav(navOffers);
        contentArea.setCenter(NavigationUtil.loadPane("Candidatures.fxml")); 
    }
    
    @FXML public void showProjects() { 
        setActiveNav(navProjects);
        try {
            javafx.scene.layout.Pane pane = NavigationUtil.loadPane("StudentProjects.fxml");
            if (pane != null) {
                contentArea.setCenter(pane);
            } else {
                System.err.println("ERROR: StudentProjects.fxml returned null!");
            }
        } catch (Exception e) {
            System.err.println("ERROR loading StudentProjects.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML public void showDocuments() { 
        setActiveNav(navDocuments);
        contentArea.setCenter(NavigationUtil.loadPane("Projects.fxml")); // TODO: créer Documents.fxml
    }
    
    @FXML public void showComments() { 
        setActiveNav(navComments);
        contentArea.setCenter(NavigationUtil.loadPane("Projects.fxml")); // TODO: créer Comments.fxml
    }
    
    @FXML public void showTasks() { 
        contentArea.setCenter(NavigationUtil.loadPane("Tasks.fxml")); 
    }
    
    @FXML public void showSprints() { 
        contentArea.setCenter(NavigationUtil.loadPane("Sprints.fxml")); 
    }
    
    @FXML public void showProfile() { 
        setActiveNav(navProfile);
        contentArea.setCenter(NavigationUtil.loadPane("Users.fxml")); 
    }

    @FXML
    public void handleLogout() {
        SessionManager.logout();
        NavigationUtil.navigateTo("Home.fxml");
    }
}
