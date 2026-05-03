package org.example.controller;

import org.example.dao.UserDAO;
import org.example.model.User;
import org.example.util.NavigationUtil;
import org.example.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill in all fields.");
            return;
        }

        try {
            User user = userDAO.authenticate(email, password);
            if (user != null) {
                SessionManager.setCurrentUser(user);
                switch (user.getRole().toLowerCase()) {
                    case "student"       -> NavigationUtil.navigateTo("StudentDashboard.fxml");
                    case "supervisor"    -> NavigationUtil.navigateTo("SupervisorDashboard.fxml");
                    case "establishment" -> NavigationUtil.navigateTo("EstablishmentDashboard.fxml");
                    default              -> NavigationUtil.navigateTo("Dashboard.fxml"); // admin
                }
            } else {
                errorLabel.setText("Incorrect email or password.");
            }
        } catch (Exception e) {
            errorLabel.setText("Database connection error.");
            e.printStackTrace();
        }
    }
}
