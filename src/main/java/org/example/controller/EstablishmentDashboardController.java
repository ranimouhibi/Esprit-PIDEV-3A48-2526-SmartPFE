package org.example.controller;

import org.example.dao.ProjectDAO;
import org.example.dao.UserDAO;
import org.example.model.User;
import org.example.util.NavigationUtil;
import org.example.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class EstablishmentDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label userCountLabel;
    @FXML private Label projectCountLabel;
    @FXML private Label applicationCountLabel;
    @FXML private BorderPane contentArea;
    @FXML private VBox homePane;

    private final UserDAO userDAO = new UserDAO();
    private final ProjectDAO projectDAO = new ProjectDAO();

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
            userCountLabel.setText(String.valueOf(userDAO.findAll().size()));
            projectCountLabel.setText(String.valueOf(projectDAO.findAll().size()));
            applicationCountLabel.setText("0");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML public void showHome() { 
        if (contentArea.getCenter() != null) contentArea.getCenter().setVisible(false);
        contentArea.setCenter(null);
        javafx.scene.Node node = homePane.getParent() instanceof javafx.scene.control.ScrollPane sp ? sp : homePane;
        contentArea.setCenter(node);
        if (node != null) node.setVisible(true);
    }
    
    @FXML public void showUsers() { 
        if (contentArea.getCenter() != null) contentArea.getCenter().setVisible(false);
        contentArea.setCenter(null);
        javafx.scene.layout.Pane pane = NavigationUtil.loadPane("Users.fxml");
        if (pane != null) {
            contentArea.setCenter(pane);
            pane.setVisible(true);
        }
    }
    
    @FXML public void showProjects() { 
        if (contentArea.getCenter() != null) contentArea.getCenter().setVisible(false);
        contentArea.setCenter(null);
        javafx.scene.layout.Pane pane = NavigationUtil.loadPane("Projects.fxml");
        if (pane != null) {
            contentArea.setCenter(pane);
            pane.setVisible(true);
        }
    }
    
    @FXML public void showApplications() { 
        if (contentArea.getCenter() != null) contentArea.getCenter().setVisible(false);
        contentArea.setCenter(null);
        javafx.scene.layout.Pane pane = NavigationUtil.loadPane("Candidatures.fxml");
        if (pane != null) {
            contentArea.setCenter(pane);
            pane.setVisible(true);
        }
    }
    
    @FXML public void showProfile() { 
        if (contentArea.getCenter() != null) contentArea.getCenter().setVisible(false);
        contentArea.setCenter(null);
        javafx.scene.layout.Pane pane = NavigationUtil.loadPane("UserProfile.fxml");
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
