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

    @FXML public void showHome() { contentArea.setCenter(homePane.getParent() instanceof javafx.scene.control.ScrollPane sp ? sp : homePane); }
    @FXML public void showUsers() { contentArea.setCenter(NavigationUtil.loadPane("Users.fxml")); }
    @FXML public void showProjects() { contentArea.setCenter(NavigationUtil.loadPane("Projects.fxml")); }
    @FXML public void showApplications() { contentArea.setCenter(NavigationUtil.loadPane("Candidatures.fxml")); }
    @FXML public void showProfile() { contentArea.setCenter(NavigationUtil.loadPane("Users.fxml")); }

    @FXML
    public void handleLogout() {
        SessionManager.logout();
        NavigationUtil.navigateTo("Home.fxml");
    }
}
