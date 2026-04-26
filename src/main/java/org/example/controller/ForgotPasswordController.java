package org.example.controller;

import org.example.dao.UserDAO;
import org.example.model.User;
import org.example.util.EmailService;
import org.example.util.NavigationUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Random;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private Label     emailError;
    @FXML private Label     feedbackLabel;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void handleSendCode() {
        hide(emailError);
        hide(feedbackLabel);

        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError(emailError, "Email is required.");
            return;
        }
        if (!email.matches("^[\\w.+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
            showError(emailError, "Enter a valid email address.");
            return;
        }

        showFeedback(feedbackLabel, "Sending code…", true);
        emailField.setDisable(true);

        new Thread(() -> {
            try {
                User user = userDAO.findByEmail(email);

                if (user == null) {
                    // Don't reveal whether email exists
                    Platform.runLater(() -> {
                        emailField.setDisable(false);
                        showFeedback(feedbackLabel,
                            "If this email is registered, a code has been sent.", true);
                    });
                    return;
                }

                // Generate 6-digit OTP
                String otp = String.format("%06d", new Random().nextInt(1000000));

                // Persist in DB
                Timestamp expiry = Timestamp.valueOf(LocalDateTime.now().plusMinutes(15));
                userDAO.saveResetToken(email, otp, expiry);

                // Send email
                EmailService.sendPasswordReset(email, user.getName(), otp);

                // Pass context to reset screen
                ResetPasswordController.pendingEmail  = email;
                ResetPasswordController.pendingUserId = user.getId();

                Platform.runLater(() -> NavigationUtil.navigateTo("ResetPassword.fxml"));

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    emailField.setDisable(false);
                    showFeedback(feedbackLabel, "Failed to send code: " + e.getMessage(), false);
                });
            }
        }).start();
    }

    @FXML
    public void goToLogin() {
        NavigationUtil.navigateTo("Login.fxml");
    }

    private void showError(Label l, String msg)  { l.setText(msg); l.setVisible(true); l.setManaged(true); }
    private void hide(Label l)                   { l.setVisible(false); l.setManaged(false); }
    private void showFeedback(Label l, String msg, boolean success) {
        l.setText(msg);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: "
                + (success ? "#16a34a" : "#dc2626") + ";");
        l.setVisible(true);
        l.setManaged(true);
    }
}
