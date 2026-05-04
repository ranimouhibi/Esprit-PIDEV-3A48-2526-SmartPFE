package org.example.controller;

import org.example.dao.UserDAO;
import org.example.model.User;
import org.example.util.NavigationUtil;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

public class SignupController {

    // ── Step 1 ────────────────────────────────────────────────────────────────
    @FXML private VBox stepRolePane;
    @FXML private VBox cardEstablishment;
    @FXML private VBox cardSupervisor;
    @FXML private VBox cardStudent;
    @FXML private Label roleCardError;

    // ── Step 2 ────────────────────────────────────────────────────────────────
    @FXML private VBox stepFormPane;
    @FXML private Label formRoleLabel;

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;

    @FXML private VBox establishmentBox;
    @FXML private ComboBox<String> establishmentCombo;
    @FXML private Label establishmentError;

    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private Label nameError;
    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;
    @FXML private Label globalError;
    @FXML private Button signupButton;

    // ── State ─────────────────────────────────────────────────────────────────
    private String selectedRole = null;
    /** Maps display name → User id for establishments */
    private java.util.Map<String, Integer> establishmentMap = new java.util.LinkedHashMap<>();

    private static final String CARD_DEFAULT =
            "-fx-background-color: #f9f9f9; -fx-background-radius: 16; " +
            "-fx-border-color: #ddd; -fx-border-radius: 16; -fx-border-width: 2; -fx-cursor: hand;";
    private static final String CARD_SELECTED =
            "-fx-background-color: #fff5f5; -fx-background-radius: 16; " +
            "-fx-border-color: #a12c2f; -fx-border-radius: 16; -fx-border-width: 2.5; -fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(161,44,47,0.18), 10, 0, 0, 3);";

    private final UserDAO userDAO = new UserDAO();

    // ── Step 1 handlers ───────────────────────────────────────────────────────

    @FXML
    public void selectEstablishment() {
        selectedRole = "establishment";
        highlightCard(cardEstablishment);
    }

    @FXML
    public void selectSupervisor() {
        selectedRole = "supervisor";
        highlightCard(cardSupervisor);
    }

    @FXML
    public void selectStudent() {
        selectedRole = "student";
        highlightCard(cardStudent);
    }

    private void highlightCard(VBox selected) {
        cardEstablishment.setStyle(CARD_DEFAULT);
        cardSupervisor.setStyle(CARD_DEFAULT);
        cardStudent.setStyle(CARD_DEFAULT);
        selected.setStyle(CARD_SELECTED);
        roleCardError.setVisible(false);
        roleCardError.setManaged(false);
    }

    @FXML
    public void handleRoleNext() {
        if (selectedRole == null) {
            roleCardError.setText("Please select a role to continue.");
            roleCardError.setVisible(true);
            roleCardError.setManaged(true);
            return;
        }
        // Transition to step 2
        stepRolePane.setVisible(false);
        stepRolePane.setManaged(false);

        formRoleLabel.setText("Signing up as " + capitalize(selectedRole));

        // Show / hide establishment selector
        boolean needsEstablishment = selectedRole.equals("supervisor") || selectedRole.equals("student");
        establishmentBox.setVisible(needsEstablishment);
        establishmentBox.setManaged(needsEstablishment);

        if (needsEstablishment) {
            loadEstablishments();
        }

        stepFormPane.setVisible(true);
        stepFormPane.setManaged(true);
    }

    private void loadEstablishments() {
        establishmentCombo.getItems().clear();
        establishmentMap.clear();
        try {
            List<User> establishments = userDAO.findByRole("establishment");
            for (User e : establishments) {
                String display = e.getName();
                establishmentMap.put(display, e.getId());
                establishmentCombo.getItems().add(display);
            }
            if (establishments.isEmpty()) {
                establishmentCombo.setPromptText("No establishments registered yet");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            establishmentCombo.setPromptText("Failed to load establishments");
        }
    }

    // ── Step 2 handlers ───────────────────────────────────────────────────────

    @FXML
    public void goBackToRoleStep() {
        stepFormPane.setVisible(false);
        stepFormPane.setManaged(false);
        stepRolePane.setVisible(true);
        stepRolePane.setManaged(true);
        clearErrors();
    }

    @FXML
    public void handleSignup() {
        clearErrors();

        String name    = nameField.getText().trim();
        String email   = emailField.getText().trim();
        String phone   = phoneField.getText().trim();
        String pass    = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        boolean valid = true;

        // Name
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

        // Email
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

        // Phone
        if (phone.isEmpty()) {
            showError(phoneError, "Phone number is required.");
            valid = false;
        } else if (!phone.matches("^[+]?[0-9\\s\\-]{8,15}$")) {
            showError(phoneError, "Enter a valid phone number.");
            valid = false;
        }

        // Establishment (required for supervisor / student)
        Integer establishmentId = null;
        if (selectedRole.equals("supervisor") || selectedRole.equals("student")) {
            String selectedEstablishment = establishmentCombo.getValue();
            if (selectedEstablishment == null || selectedEstablishment.isEmpty()) {
                showError(establishmentError, "Please select an establishment.");
                valid = false;
            } else {
                establishmentId = establishmentMap.get(selectedEstablishment);
            }
        }

        // Password
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
            user.setRole(selectedRole);
            user.setPassword(pass);
            user.setActive(true);
            user.setEstablishmentId(establishmentId);

            userDAO.save(user);

            // Show success message then navigate to login after 2 seconds
            signupButton.setDisable(true);
            showGlobalSuccess("✓ Account created successfully! Redirecting to login...");
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(e -> NavigationUtil.navigateTo("Login.fxml"));
            pause.play();
        } catch (Exception e) {
            showGlobalError("Failed to create account. Please try again.");
            e.printStackTrace();
        }
    }

    @FXML
    public void goToLogin() {
        NavigationUtil.navigateTo("Login.fxml");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void clearErrors() {
        Label[] labels = {nameError, emailError, phoneError, establishmentError,
                          passwordError, confirmPasswordError, globalError};
        for (Label lbl : labels) {
            lbl.setText("");
            lbl.setVisible(false);
            lbl.setManaged(false);
        }
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

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
