package org.example.controller;

import org.example.dao.CandidatureDAO;
import org.example.model.Candidature;
import org.example.model.Offer;
import org.example.model.User;
import org.example.service.EmailNotificationService;
import org.example.util.ModernAlert;
import org.example.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class ApplyDialogController implements Initializable {

    @FXML private Label offerTitleLabel;
    @FXML private Label offerEstablishment;
    @FXML private Label offerDeadline;
    @FXML private TextArea motivationField;
    @FXML private Label motivationError;
    @FXML private Label motivationCount;
    @FXML private VBox cvDropZone;
    @FXML private Label cvIcon;
    @FXML private Label cvFileName;
    @FXML private Label cvError;
    @FXML private TextField portfolioField;
    @FXML private TextField githubField;
    @FXML private Button submitBtn;

    private Stage dialogStage;
    private Offer offer;
    private File selectedCvFile;
    private Runnable onSuccess;

    private final CandidatureDAO candidatureDAO = new CandidatureDAO();
    private final EmailNotificationService emailService = new EmailNotificationService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Character counter for motivation
        motivationField.textProperty().addListener((obs, old, val) -> {
            if (val != null) {
                int len = val.length();
                motivationCount.setText(len + " / 2000");
                if (len > 2000) motivationField.setText(old);
                motivationCount.setStyle("-fx-font-size: 10px; -fx-text-fill: " + (len < 50 ? "#e53e3e" : "#aaa") + ";");
            }
        });

        // URL validation styling
        portfolioField.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) validateUrl(portfolioField);
        });
        githubField.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) validateUrl(githubField);
        });

        // CV drop zone hover effect
        cvDropZone.setOnMouseEntered(e ->
            cvDropZone.setStyle(cvDropZone.getStyle().replace("#fafafa", "#f0f4ff")
                .replace("#ddd", "#667eea")));
        cvDropZone.setOnMouseExited(e ->
            cvDropZone.setStyle(cvDropZone.getStyle().replace("#f0f4ff", "#fafafa")
                .replace("#667eea", "#ddd")));
    }

    public void setOffer(Offer offer) {
        this.offer = offer;
        offerTitleLabel.setText(offer.getTitle());
        offerEstablishment.setText(offer.getEstablishmentName() != null ? offer.getEstablishmentName() : "");
        if (offer.getDeadline() != null) {
            offerDeadline.setText("Deadline: " + offer.getDeadline().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        } else {
            offerDeadline.setText("No deadline specified");
        }
    }

    public void setDialogStage(Stage stage) { this.dialogStage = stage; }
    public void setOnSuccess(Runnable callback) { this.onSuccess = callback; }

    @FXML
    private void handleBrowseCV() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select your CV");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
            new FileChooser.ExtensionFilter("Word Files", "*.doc", "*.docx"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = fc.showOpenDialog(dialogStage);
        if (file != null) {
            selectedCvFile = file;
            cvIcon.setText("✅");
            cvFileName.setText(file.getName());
            cvFileName.setStyle("-fx-font-size: 10px; -fx-text-fill: #22c55e; -fx-font-weight: bold;");
            cvDropZone.setStyle(cvDropZone.getStyle()
                .replace("#fafafa", "#f0fff4").replace("#ddd", "#22c55e"));
            cvError.setText("");
        }
    }

    @FXML
    private void handleSubmit() {
        if (!validate()) return;

        try {
            User user = SessionManager.getCurrentUser();
            Candidature c = new Candidature();
            c.setOfferId(offer.getId());
            c.setStudentId(user.getId());
            c.setMotivationLetter(motivationField.getText().trim());
            c.setCvPath(selectedCvFile.getName());
            c.setPortfolioUrl(portfolioField.getText().trim());
            c.setGithubUrl(githubField.getText().trim());
            candidatureDAO.save(c);

            // Async email confirmation
            new Thread(() -> {
                try { emailService.sendCandidatureConfirmation(c.getId()); } catch (Exception ignored) {}
            }).start();

            if (dialogStage != null) dialogStage.close();
            if (onSuccess != null) onSuccess.run();
            ModernAlert.show(ModernAlert.Type.SUCCESS, "Application Submitted",
                "Your application for \"" + offer.getTitle() + "\" has been submitted successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            ModernAlert.show(ModernAlert.Type.ERROR, "Error", "Failed to submit: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        if (dialogStage != null) dialogStage.close();
    }

    private boolean validate() {
        boolean valid = true;

        String motivation = motivationField.getText() != null ? motivationField.getText().trim() : "";
        if (motivation.isEmpty()) {
            motivationError.setText("Motivation letter is required");
            highlightError(motivationField);
            valid = false;
        } else if (motivation.length() < 50) {
            motivationError.setText("Minimum 50 characters (" + motivation.length() + " written)");
            highlightError(motivationField);
            valid = false;
        } else {
            motivationError.setText("");
            clearError(motivationField);
        }

        if (selectedCvFile == null) {
            cvError.setText("Please select your CV file");
            cvDropZone.setStyle(cvDropZone.getStyle().replace("#ddd", "#e53e3e"));
            valid = false;
        } else {
            cvError.setText("");
        }

        return valid;
    }

    private void validateUrl(TextField field) {
        String val = field.getText().trim();
        if (!val.isEmpty() && !val.startsWith("http://") && !val.startsWith("https://")) {
            field.setStyle(field.getStyle().replace("#e0e0e0", "#fca5a5"));
        } else {
            field.setStyle(field.getStyle().replace("#fca5a5", "#e0e0e0"));
        }
    }

    private void highlightError(TextArea field) {
        field.setStyle(field.getStyle().replace("#e0e0e0", "#fca5a5"));
    }

    private void clearError(TextArea field) {
        field.setStyle(field.getStyle().replace("#fca5a5", "#e0e0e0"));
    }
}
