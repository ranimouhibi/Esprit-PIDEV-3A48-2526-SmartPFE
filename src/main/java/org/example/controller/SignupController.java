package org.example.controller;

import org.example.dao.UserDAO;
import org.example.model.User;
import org.example.util.NavigationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SignupController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private Label nameError;
    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Label roleError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;
    @FXML private Label globalError;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        roleCombo.getItems().addAll("student", "supervisor", "establishment");
    }

    @FXML
    public void handleSignup() {
        clearErrors();

        String name    = nameField.getText().trim();
        String email   = emailField.getText().trim();
        String phone   = phoneField.getText().trim();
        String role    = roleCombo.getValue();
        String pass    = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        boolean valid = true;

        // Name validation
        if (name.isEmpty()) {
            showError(nameError, "Full name is required.");
            valid = false;
        } else if (name.length() < 3) {
            showError(nameError, "Name must be at least 3 characters.");
            valid = false;
        } else if (!name.matches("[\\p{L} .'-]+")) {
            showError(nameError, "Name contains invalid characters.");
            valid = false;
        }

        // Email validation
        if (email.isEmpty()) {
            showError(emailError, "Email is required.");
            valid = false;
        } else if (!email.matches("^[\\w.+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
            showError(emailError, "Enter a valid email address.");
            valid = false;
        } else {
            try {
                if (userDAO.emailExists(email)) {
                    showError(emailError, "This email is already registered.");
                    valid = false;
                }
            } catch (Exception e) {
                showGlobalError("Database error while checking email.");
                return;
            }
        }

        // Phone validation
        if (phone.isEmpty()) {
            showError(phoneError, "Phone number is required.");
            valid = false;
        } else if (!phone.matches("^[+]?[0-9\\s\\-]{8,15}$")) {
            showError(phoneError, "Enter a valid phone number.");
            valid = false;
        }

        // Role validation
        if (role == null || role.isEmpty()) {
            showError(roleError, "Please select a role.");
            valid = false;
        }

        // Password validation
        if (pass.isEmpty()) {
            showError(passwordError, "Password is required.");
            valid = false;
        } else if (pass.length() < 8) {
            showError(passwordError, "Password must be at least 8 characters.");
            valid = false;
        } else if (!pass.matches(".*[A-Z].*")) {
            showError(passwordError, "Password must contain at least one uppercase letter.");
            valid = false;
        } else if (!pass.matches(".*[0-9].*")) {
            showError(passwordError, "Password must contain at least one digit.");
            valid = false;
        }

        // Confirm password
        if (confirm.isEmpty()) {
            showError(confirmPasswordError, "Please confirm your password.");
            valid = false;
        } else if (!pass.equals(confirm)) {
            showError(confirmPasswordError, "Passwords do not match.");
            valid = false;
        }

        if (!valid) return;

        try {
            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPhone(phone);
            user.setRole(role);
            user.setPassword(pass);
            user.setActive(true);

            userDAO.save(user);
            NavigationUtil.navigateTo("Login.fxml");
        } catch (Exception e) {
            showGlobalError("Failed to create account. Please try again.");
            e.printStackTrace();
        }
    }

    @FXML
    public void goToLogin() {
        NavigationUtil.navigateTo("Login.fxml");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void clearErrors() {
        for (Label lbl : new Label[]{nameError, emailError, phoneError, roleError,
                                      passwordError, confirmPasswordError, globalError}) {
            lbl.setText("");
            lbl.setVisible(false);
            lbl.setManaged(false);
        }
    }

    private void clearForm() {
        nameField.clear();
        emailField.clear();
        phoneField.clear();
        phoneField.clear();
        roleCombo.setValue(null);
        passwordField.clear();
        confirmPasswordField.clear();
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void showGlobalError(String message) {
        globalError.setText(message);
        globalError.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px; -fx-font-weight: bold;");
        globalError.setVisible(true);
        globalError.setManaged(true);
    }

    private void showGlobalSuccess(String message) {
        globalError.setText(message);
        globalError.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 12px; -fx-font-weight: bold;");
        globalError.setVisible(true);
        globalError.setManaged(true);
    }
}
