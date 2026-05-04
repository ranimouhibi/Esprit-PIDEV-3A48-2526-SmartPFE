package org.example.controller;

import org.example.dao.UserDAO;
import org.example.model.User;
import org.example.util.LocalSessionStore;
import org.example.util.NavigationUtil;
import org.example.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.mindrot.jbcrypt.BCrypt;

public class LoginController {

    // ── Password panel ────────────────────────────────────────────────────────
    @FXML private VBox      passwordPanel;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox  rememberMeCheck;
    @FXML private Label     errorLabel;
    @FXML private Button    switchToPinBtn;

    // ── PIN panel ─────────────────────────────────────────────────────────────
    @FXML private VBox   pinPanel;
    @FXML private Label  pinAvatarLabel;
    @FXML private Label  pinUserName;
    @FXML private HBox   pinDots;
    @FXML private Label  pinErrorLabel;

    private final StringBuilder pinBuffer = new StringBuilder();
    private static final int PIN_LENGTH = 6;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        int lastUserId = LocalSessionStore.loadLastUserId();
        if (lastUserId != -1 && LocalSessionStore.loadPinHash(lastUserId) != null) {
            switchToPinBtn.setVisible(true);
            switchToPinBtn.setManaged(true);
            try {
                User u = userDAO.findById(lastUserId);
                if (u != null) {
                    emailField.setText(u.getEmail());
                    pinAvatarLabel.setText(String.valueOf(u.getName().charAt(0)).toUpperCase());
                    pinUserName.setText(u.getName());
                }
            } catch (Exception ignored) {}
        }
    }

    // ── Password login ────────────────────────────────────────────────────────

    @FXML
    public void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        // Both fields empty → jump to PIN panel if a PIN is available
        if (email.isEmpty() && password.isEmpty()) {
            int lastUserId = LocalSessionStore.loadLastUserId();
            if (lastUserId != -1 && LocalSessionStore.loadPinHash(lastUserId) != null) {
                showPinPanel();
                return;
            }
            showError("Please fill in all fields.");
            return;
        }
        if (email.isEmpty())   { showError("Email is required.");    highlight(emailField);    return; }
        if (!email.matches("^[\\w.+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
            showError("Enter a valid email address."); highlight(emailField); return;
        }
        if (password.isEmpty()) { showError("Password is required."); highlight(passwordField); return; }
        if (password.length() < 8) { showError("Password must be at least 8 characters."); highlight(passwordField); return; }

        clearHighlights();

        try {
            User user = userDAO.authenticate(email, password);
            if (user != null) {
                completeLogin(user, rememberMeCheck.isSelected());
                SessionManager.setCurrentUser(user);
                switch (user.getRole().toLowerCase()) {
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

    // ── PIN login ─────────────────────────────────────────────────────────────

    @FXML
    public void pinKey(javafx.event.ActionEvent e) {
        if (pinBuffer.length() >= PIN_LENGTH) return;
        String digit = ((Button) e.getSource()).getText();
        pinBuffer.append(digit);
        updatePinDots();
        // Auto-submit when full
        if (pinBuffer.length() == PIN_LENGTH) {
            verifyPin();
        }
    }

    @FXML
    public void pinBackspace() {
        if (pinBuffer.length() > 0) {
            pinBuffer.deleteCharAt(pinBuffer.length() - 1);
            updatePinDots();
            hidePinError();
        }
    }

    @FXML
    public void pinConfirm() {
        if (pinBuffer.length() == 0) return;
        verifyPin();
    }

    private void verifyPin() {
        int lastUserId = LocalSessionStore.loadLastUserId();
        if (lastUserId == -1) {
            showPinError("No saved user. Please use your password.");
            return;
        }
        String storedHash = LocalSessionStore.loadPinHash(lastUserId);
        if (storedHash == null) {
            showPinError("No PIN set. Please use your password.");
            return;
        }
        if (BCrypt.checkpw(pinBuffer.toString(), storedHash)) {
            try {
                User user = userDAO.findById(lastUserId);
                if (user != null && user.isActive()) {
                    completeLogin(user, true);
                } else {
                    showPinError("Account not found or inactive.");
                }
            } catch (Exception ex) {
                showPinError("Database error.");
                ex.printStackTrace();
            }
        } else {
            showPinError("Incorrect PIN. Try again.");
            pinBuffer.setLength(0);
            updatePinDots();
        }
    }

    private void updatePinDots() {
        int filled = pinBuffer.length();
        for (int i = 0; i < pinDots.getChildren().size(); i++) {
            StackPane dot = (StackPane) pinDots.getChildren().get(i);
            dot.setStyle(i < filled
                ? "-fx-background-color: #a12c2f; -fx-background-radius: 9;"
                : "-fx-background-color: #ddd; -fx-background-radius: 9;");
        }
    }

    private void showPinError(String msg) {
        pinErrorLabel.setText(msg);
        pinErrorLabel.setVisible(true);
        pinErrorLabel.setManaged(true);
    }

    private void hidePinError() {
        pinErrorLabel.setVisible(false);
        pinErrorLabel.setManaged(false);
    }

    // ── Panel switching ───────────────────────────────────────────────────────

    @FXML
    public void showPinPanel() {
        passwordPanel.setVisible(false);
        passwordPanel.setManaged(false);
        pinPanel.setVisible(true);
        pinPanel.setManaged(true);
        pinBuffer.setLength(0);
        updatePinDots();
        hidePinError();
    }

    @FXML
    public void showPasswordPanel() {
        pinPanel.setVisible(false);
        pinPanel.setManaged(false);
        passwordPanel.setVisible(true);
        passwordPanel.setManaged(true);
        errorLabel.setText("");
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    public void goToSignup() { NavigationUtil.navigateTo("Signup.fxml"); }

    @FXML
    public void goToForgotPassword() { NavigationUtil.navigateTo("ForgotPassword.fxml"); }

    // ── Shared post-login logic ───────────────────────────────────────────────

    private void completeLogin(User user, boolean remember) {
        SessionManager.setCurrentUser(user);

        // Always persist last user for PIN login (independent of remember-me)
        LocalSessionStore.saveLastUser(user.getId());

        if (remember) {
            try {
                String token = LocalSessionStore.generateToken();
                userDAO.saveRememberToken(user.getId(), token);
                LocalSessionStore.saveSession(user.getId(), token);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            LocalSessionStore.clearSession();
            try { userDAO.clearRememberToken(user.getId()); } catch (Exception ignored) {}
        }

        switch (user.getRole()) {
            case "student"       -> NavigationUtil.navigateTo("StudentDashboard.fxml");
            case "supervisor"    -> NavigationUtil.navigateTo("SupervisorDashboard.fxml");
            case "establishment" -> NavigationUtil.navigateTo("EstablishmentDashboard.fxml");
            default              -> NavigationUtil.navigateTo("Dashboard.fxml");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showError(String message) { errorLabel.setText(message); }

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
