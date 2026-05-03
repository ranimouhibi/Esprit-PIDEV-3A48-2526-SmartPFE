package org.example.controller;

import org.example.dao.UserDAO;
import org.example.model.User;
import org.example.util.NavigationUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ResetPasswordController {

    public static String pendingEmail  = null;
    public static int    pendingUserId = -1;

    @FXML private TextField     otpField;
    @FXML private PasswordField newPassField;
    @FXML private PasswordField confirmPassField;
    @FXML private Label         otpError;
    @FXML private Label         newPassError;
    @FXML private Label         confirmPassError;
    @FXML private Label         feedbackLabel;
    @FXML private Label         instructionLabel;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        if (pendingEmail != null) {
            instructionLabel.setText(
                "A 6-digit code was sent to " + pendingEmail
                + ". Enter it below along with your new password.");
        }
    }

    @FXML
    public void handleResetPassword() {
        clearErrors();
        hide(feedbackLabel);

        String otp     = otpField.getText().trim();
        String newPass = newPassField.getText();
        String confirm = confirmPassField.getText();
        boolean valid  = true;

        if (otp.isEmpty()) {
            showError(otpError, "Reset code is required."); valid = false;
        } else if (!otp.matches("\\d{6}")) {
            showError(otpError, "Code must be exactly 6 digits."); valid = false;
        }
        if (newPass.isEmpty()) {
            showError(newPassError, "New password is required."); valid = false;
        } else if (newPass.length() < 8) {
            showError(newPassError, "Password must be at least 8 characters."); valid = false;
        } else if (!newPass.matches(".*[A-Z].*")) {
            showError(newPassError, "Must contain at least one uppercase letter."); valid = false;
        } else if (!newPass.matches(".*[0-9].*")) {
            showError(newPassError, "Must contain at least one digit."); valid = false;
        }
        if (confirm.isEmpty()) {
            showError(confirmPassError, "Please confirm your password."); valid = false;
        } else if (!newPass.equals(confirm)) {
            showError(confirmPassError, "Passwords do not match."); valid = false;
        }

        if (!valid) return;

        try {
            User user = userDAO.findByResetToken(otp);

            if (user == null) {
                showError(otpError, "Invalid or expired code. Please request a new one.");
                return;
            }

            if (pendingEmail != null && !pendingEmail.equalsIgnoreCase(user.getEmail())) {
                showError(otpError, "Code does not match the requested email.");
                return;
            }

            userDAO.updatePassword(user.getId(), newPass);
            try { userDAO.clearResetToken(user.getId()); } catch (Exception ignored) {}

            pendingEmail  = null;
            pendingUserId = -1;

            showFeedback(feedbackLabel, "Password reset successfully! Redirecting to login…", true);

            new Thread(() -> {
                try { Thread.sleep(1800); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> NavigationUtil.navigateTo("Login.fxml"));
            }).start();

        } catch (Exception e) {
            showFeedback(feedbackLabel, "Error: " + e.getMessage(), false);
            e.printStackTrace();
        }
    }

    @FXML
    public void goToLogin() {
        pendingEmail = null; pendingUserId = -1;
        NavigationUtil.navigateTo("Login.fxml");
    }

    private void showError(Label l, String msg) { l.setText(msg); l.setVisible(true); l.setManaged(true); }
    private void clearErrors() {
        for (Label l : new Label[]{otpError, newPassError, confirmPassError})
            { l.setText(""); l.setVisible(false); l.setManaged(false); }
    }
    private void hide(Label l) { l.setVisible(false); l.setManaged(false); }
    private void showFeedback(Label l, String msg, boolean success) {
        l.setText(msg);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: "
                + (success ? "#16a34a" : "#dc2626") + ";");
        l.setVisible(true);
        l.setManaged(true);
    }
}
