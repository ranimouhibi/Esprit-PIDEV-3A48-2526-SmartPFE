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
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        // Input validation
        if (email.isEmpty() && password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }
        if (email.isEmpty()) {
            showError("Email is required.");
            highlight(emailField);
            return;
        }
        if (!email.matches("^[\\w.+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
            showError("Enter a valid email address.");
            highlight(emailField);
            return;
        }
        if (password.isEmpty()) {
            showError("Password is required.");
            highlight(passwordField);
            return;
        }
        if (password.length() < 8) {
            showError("Password must be at least 8 characters.");
            highlight(passwordField);
            return;
        }

        clearHighlights();

        try {
            User user = userDAO.authenticate(email, password);
            if (user != null) {
                SessionManager.setCurrentUser(user);
                switch (user.getRole()) {
                    case "student"       -> NavigationUtil.navigateTo("StudentDashboard.fxml");
                    case "supervisor"    -> NavigationUtil.navigateTo("SupervisorDashboard.fxml");
                    case "establishment" -> NavigationUtil.navigateTo("EstablishmentDashboard.fxml");
                    default              -> NavigationUtil.navigateTo("Dashboard.fxml");
                }
            } else {
                showError("Incorrect email or password.");
                highlight(emailField);
                highlight(passwordField);
            }
        } catch (Exception e) {
            showError("Database connection error.");
            e.printStackTrace();
        }
    }

    @FXML
    public void goToSignup() {
        NavigationUtil.navigateTo("Signup.fxml");
    }

    @FXML
    public void goToForgotPassword() {
        NavigationUtil.navigateTo("ForgotPassword.fxml");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private void highlight(Control field) {
        field.setStyle(field.getStyle().replace("#ddd", "#dc2626"));
    }

    private void clearHighlights() {
        String base = "-fx-pref-height: 44px; -fx-background-radius: 22; -fx-border-radius: 22; "
                    + "-fx-border-color: #ddd; -fx-border-width: 1.5; -fx-background-color: #f9f9f9; "
                    + "-fx-padding: 0 18; -fx-font-size: 13px;";
        emailField.setStyle(base);
        passwordField.setStyle(base);
        errorLabel.setText("");
    }
}
