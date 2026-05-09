package org.example.controller;

import org.example.dao.UserDAO;
import org.example.model.User;
import org.example.util.AIService;
import org.example.util.CloudinaryService;
import org.example.util.LocalSessionStore;
import org.example.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import org.mindrot.jbcrypt.BCrypt;

import java.io.File;
import java.time.format.DateTimeFormatter;

public class UserProfileController {

    // ── Header ────────────────────────────────────────────────────────────────
    @FXML private Label headerName;
    @FXML private Label headerEmail;
    @FXML private Label headerRole;
    @FXML private Label headerMember;

    // ── Avatar ────────────────────────────────────────────────────────────────
    @FXML private StackPane avatarStack;
    @FXML private ImageView avatarImage;
    @FXML private Label     avatarInitials;
    @FXML private StackPane previewStack;
    @FXML private ImageView previewImage;
    @FXML private Label     previewInitials;
    @FXML private Button    removePicBtn;
    @FXML private Label     picFeedback;

    // ── Personal info ─────────────────────────────────────────────────────────
    @FXML private Label     infoSubtitle;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private Label     nameError;
    @FXML private Label     emailError;
    @FXML private Label     phoneError;
    @FXML private Label     infoFeedback;

    // ── Skills & Experience ───────────────────────────────────────────────────
    @FXML private VBox      skillsBox;
    @FXML private TextField skillsField;
    @FXML private VBox      experienceBox;
    @FXML private TextArea  experienceField;
    @FXML private VBox      formationsBox;
    @FXML private TextArea  formationsField;

    // ── Bio ───────────────────────────────────────────────────────────────────
    @FXML private TextArea  bioField;
    @FXML private Button    generateBioBtn;
    @FXML private Label     bioStatus;

    // ── Role-specific extra field ─────────────────────────────────────────────
    @FXML private VBox      extraFieldBox;
    @FXML private Label     extraFieldLabel;
    @FXML private TextField extraField;

    // ── Change password ───────────────────────────────────────────────────────
    @FXML private PasswordField currentPassField;
    @FXML private PasswordField newPassField;
    @FXML private PasswordField confirmPassField;
    @FXML private Label         currentPassError;
    @FXML private Label         newPassError;
    @FXML private Label         confirmPassError;
    @FXML private Label         passFeedback;

    // ── PIN card ──────────────────────────────────────────────────────────────
    @FXML private PasswordField pinField;
    @FXML private PasswordField pinConfirmField;
    @FXML private Label         pinStatusLabel;
    @FXML private Label         pinFeedback;
    @FXML private Button        removePinBtn;

    // ── State ─────────────────────────────────────────────────────────────────
    private final UserDAO userDAO = new UserDAO();
    private File    pendingImageFile     = null;
    private boolean removePicturePending = false;

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        headerName.setText(user.getName());
        headerEmail.setText(user.getEmail());
        headerRole.setText(capitalize(user.getRole()));
        if (user.getCreatedAt() != null) {
            headerMember.setText("Member since "
                + user.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM yyyy")));
        }

        nameField.setText(user.getName());
        emailField.setText(user.getEmail());
        phoneField.setText(user.getPhone() != null ? user.getPhone() : "");
        bioField.setText(user.getBio() != null ? user.getBio() : "");

        switch (user.getRole()) {
            case "supervisor" -> {
                infoSubtitle.setText("Update your name, email, phone and contact details");
                headerRole.setStyle(headerRole.getStyle().replace("#a12c2f", "#2563eb"));
                skillsField.setText(user.getSkills() != null ? user.getSkills() : "");
                experienceField.setText(user.getExperience() != null ? user.getExperience() : "");
                formationsField.setText(user.getFormations() != null ? user.getFormations() : "");
            }
            case "establishment" -> {
                infoSubtitle.setText("Update your institution's contact information");
                extraFieldBox.setVisible(true);
                extraFieldBox.setManaged(true);
                extraFieldLabel.setText("Institution / Organisation Name");
                extraField.setPromptText("e.g. University of Algiers");
                extraField.setText(user.getName());
                nameField.setPromptText("Contact person full name");
                skillsBox.setVisible(false);      skillsBox.setManaged(false);
                experienceBox.setVisible(false);  experienceBox.setManaged(false);
                formationsBox.setVisible(false);  formationsBox.setManaged(false);
            }
        }

        loadAvatarFromUrl(user.getProfilePicture(), user);
        avatarImage.setClip(new Circle(50, 50, 50));
        previewImage.setClip(new Circle(40, 40, 40));

        refreshPinStatus(user);
    }

    // ── AI Bio Generator ──────────────────────────────────────────────────────

    @FXML
    public void handleGenerateBio() {
        User user = SessionManager.getCurrentUser();
        String name = nameField.getText().trim();
        if (name.isEmpty()) name = user.getName();

        // Institution: from extraField for establishment, or from linked establishment for supervisor
        String institution = null;
        if ("establishment".equals(user.getRole()) && extraField != null) {
            institution = extraField.getText().trim();
        } else if ("supervisor".equals(user.getRole()) && user.getEstablishmentId() != null) {
            try {
                User est = userDAO.findById(user.getEstablishmentId());
                if (est != null) institution = est.getName();
            } catch (Exception ignored) {}
        }

        String skills     = skillsBox.isVisible()     ? skillsField.getText().trim()     : null;
        String experience = experienceBox.isVisible()  ? experienceField.getText().trim() : null;
        String formations = formationsBox.isVisible()  ? formationsField.getText().trim() : null;

        generateBioBtn.setDisable(true);
        generateBioBtn.setText("⏳ Generating…");
        showBioStatus("Calling AI, please wait…", true);

        final String fn = name;
        final String fi = institution;
        final String fs = (skills != null && !skills.isEmpty()) ? skills : null;
        final String fe = (experience != null && !experience.isEmpty()) ? experience : null;
        final String ff = (formations != null && !formations.isEmpty()) ? formations : null;

        new Thread(() -> {
            try {
                String bio = AIService.generateBio(fn, user.getRole(), fi, fs, fe, ff);
                Platform.runLater(() -> {
                    bioField.setText(bio);
                    generateBioBtn.setDisable(false);
                    generateBioBtn.setText("✨ Generate with AI");
                    showBioStatus("Bio generated! Review and click Save Changes to keep it.", true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    generateBioBtn.setDisable(false);
                    generateBioBtn.setText("✨ Generate with AI");
                    showBioStatus("Failed: " + e.getMessage(), false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void showBioStatus(String msg, boolean success) {
        bioStatus.setText(msg);
        bioStatus.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: "
                + (success ? "#7c3aed" : "#dc2626") + ";");
        bioStatus.setVisible(true);
        bioStatus.setManaged(true);
    }

    // ── Profile Picture ───────────────────────────────────────────────────────

    @FXML
    public void chooseProfilePicture() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Profile Picture");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        File file = chooser.showOpenDialog(avatarStack.getScene().getWindow());
        if (file == null) return;
        if (file.length() > 5 * 1024 * 1024) {
            showFeedback(picFeedback, "Image must be smaller than 5 MB.", false);
            return;
        }
        pendingImageFile     = file;
        removePicturePending = false;
        setAvatarImage(new Image(file.toURI().toString()));
        removePicBtn.setVisible(true);
        removePicBtn.setManaged(true);
        showFeedback(picFeedback, "Photo selected — click Save Changes to upload.", true);
    }

    @FXML
    public void removeProfilePicture() {
        pendingImageFile     = null;
        removePicturePending = true;
        clearAvatarToInitials(SessionManager.getCurrentUser());
        removePicBtn.setVisible(false);
        removePicBtn.setManaged(false);
        showFeedback(picFeedback, "Photo will be removed on Save.", true);
    }

    // ── Save Personal Info ────────────────────────────────────────────────────

    @FXML
    public void handleSaveInfo() {
        clearErrors(nameError, emailError, phoneError);
        hide(infoFeedback);

        String name  = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        boolean valid = true;

        if (name.isEmpty()) {
            showError(nameError, "Name is required."); valid = false;
        } else if (name.length() < 3) {
            showError(nameError, "Name must be at least 3 characters."); valid = false;
        } else if (!name.matches("[\\p{L} .,'&()-]+")) {
            showError(nameError, "Name contains invalid characters."); valid = false;
        }

        if (email.isEmpty()) {
            showError(emailError, "Email is required."); valid = false;
        } else if (!email.matches("^[\\w.+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
            showError(emailError, "Enter a valid email address."); valid = false;
        } else {
            try {
                User current = SessionManager.getCurrentUser();
                if (!email.equalsIgnoreCase(current.getEmail())
                        && userDAO.emailExistsForOther(email, current.getId())) {
                    showError(emailError, "This email is already in use."); valid = false;
                }
            } catch (Exception e) {
                showFeedback(infoFeedback, "Database error: " + e.getMessage(), false);
                return;
            }
        }

        if (phone.isEmpty()) {
            showError(phoneError, "Phone number is required."); valid = false;
        } else if (!phone.matches("^[+]?[0-9\\s\\-]{8,15}$")) {
            showError(phoneError, "Enter a valid phone number."); valid = false;
        }

        if (!valid) return;

        showFeedback(infoFeedback, "Saving…", true);

        new Thread(() -> {
            try {
                User user = SessionManager.getCurrentUser();
                user.setName(name);
                user.setEmail(email);
                user.setPhone(phone);
                user.setBio(bioField.getText().trim());
                if (skillsBox.isVisible()) user.setSkills(skillsField.getText().trim());
                if (experienceBox.isVisible()) user.setExperience(experienceField.getText().trim());
                if (formationsBox.isVisible()) user.setFormations(formationsField.getText().trim());

                if (pendingImageFile != null) {
                    Platform.runLater(() -> showFeedback(infoFeedback, "Uploading photo…", true));
                    String url = CloudinaryService.uploadImage(pendingImageFile, "profile_pictures");
                    user.setProfilePicture(url);
                    pendingImageFile = null;
                } else if (removePicturePending) {
                    user.setProfilePicture(null);
                    removePicturePending = false;
                }

                userDAO.updateProfile(user);
                SessionManager.setCurrentUser(user);

                Platform.runLater(() -> {
                    headerName.setText(user.getName());
                    headerEmail.setText(user.getEmail());
                    loadAvatarFromUrl(user.getProfilePicture(), user);
                    removePicBtn.setVisible(user.getProfilePicture() != null);
                    removePicBtn.setManaged(user.getProfilePicture() != null);
                    hide(picFeedback);
                    showFeedback(infoFeedback, "Profile updated successfully!", true);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                    showFeedback(infoFeedback, "Failed to save: " + e.getMessage(), false));
            }
        }).start();
    }

    // ── Change Password ───────────────────────────────────────────────────────

    @FXML
    public void handleChangePassword() {
        clearErrors(currentPassError, newPassError, confirmPassError);
        hide(passFeedback);

        String current = currentPassField.getText();
        String newPass  = newPassField.getText();
        String confirm  = confirmPassField.getText();
        boolean valid   = true;

        if (current.isEmpty()) {
            showError(currentPassError, "Current password is required."); valid = false;
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
            showError(confirmPassError, "Please confirm your new password."); valid = false;
        } else if (!newPass.equals(confirm)) {
            showError(confirmPassError, "Passwords do not match."); valid = false;
        }

        if (!valid) return;

        try {
            User user = SessionManager.getCurrentUser();
            String stored = userDAO.findById(user.getId()).getPassword().replace("$2y$", "$2a$");
            if (!BCrypt.checkpw(current, stored)) {
                showError(currentPassError, "Current password is incorrect.");
                return;
            }
            userDAO.updatePassword(user.getId(), newPass);
            currentPassField.clear();
            newPassField.clear();
            confirmPassField.clear();
            showFeedback(passFeedback, "Password changed successfully!", true);
        } catch (Exception e) {
            showFeedback(passFeedback, "Failed to update password: " + e.getMessage(), false);
            e.printStackTrace();
        }
    }

    // ── PIN management ────────────────────────────────────────────────────────

    private void refreshPinStatus(User user) {
        String hash = LocalSessionStore.loadPinHash(user.getId());
        boolean hasPin = hash != null;
        pinStatusLabel.setText(hasPin ? "✓ PIN is set — you can use it at login" : "No PIN set");
        pinStatusLabel.setStyle(hasPin
            ? "-fx-font-size: 11px; -fx-text-fill: #16a34a;"
            : "-fx-font-size: 11px; -fx-text-fill: #888;");
        removePinBtn.setVisible(hasPin);
        removePinBtn.setManaged(hasPin);
    }

    @FXML
    public void handleSavePin() {
        hide(pinFeedback);
        String pin     = pinField.getText();
        String confirm = pinConfirmField.getText();
        if (pin.isEmpty())          { showPinFeedback("Enter a PIN.", false); return; }
        if (!pin.matches("\\d{6}")) { showPinFeedback("PIN must be exactly 6 digits.", false); return; }
        if (!pin.equals(confirm))   { showPinFeedback("PINs do not match.", false); return; }

        User user = SessionManager.getCurrentUser();
        LocalSessionStore.savePin(user.getId(), BCrypt.hashpw(pin, BCrypt.gensalt()));
        pinField.clear();
        pinConfirmField.clear();
        showPinFeedback("PIN saved! You can now use it at login.", true);
        refreshPinStatus(user);
    }

    @FXML
    public void handleRemovePin() {
        User user = SessionManager.getCurrentUser();
        LocalSessionStore.clearPin(user.getId());
        showPinFeedback("PIN removed.", true);
        refreshPinStatus(user);
    }

    private void showPinFeedback(String msg, boolean success) {
        showFeedback(pinFeedback, msg, success);
    }

    // ── Avatar helpers ────────────────────────────────────────────────────────

    private void loadAvatarFromUrl(String url, User user) {
        if (url != null && !url.isBlank()) {
            new Thread(() -> {
                try {
                    Image img = new Image(url, true);
                    Platform.runLater(() -> setAvatarImage(img));
                } catch (Exception e) {
                    Platform.runLater(() -> clearAvatarToInitials(user));
                }
            }).start();
        } else {
            clearAvatarToInitials(user);
        }
    }

    private void setAvatarImage(Image img) {
        avatarImage.setImage(img);
        avatarImage.setVisible(true);    avatarImage.setManaged(true);
        avatarInitials.setVisible(false); avatarInitials.setManaged(false);
        previewImage.setImage(img);
        previewImage.setVisible(true);   previewImage.setManaged(true);
        previewInitials.setVisible(false); previewInitials.setManaged(false);
    }

    private void clearAvatarToInitials(User user) {
        String initials = (user != null && user.getName() != null && !user.getName().isEmpty())
                ? String.valueOf(user.getName().charAt(0)).toUpperCase() : "?";
        avatarInitials.setText(initials);
        avatarInitials.setVisible(true);  avatarInitials.setManaged(true);
        avatarImage.setVisible(false);    avatarImage.setManaged(false);
        previewInitials.setText(initials);
        previewInitials.setVisible(true); previewInitials.setManaged(true);
        previewImage.setVisible(false);   previewImage.setManaged(false);
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void showError(Label l, String msg)  { l.setText(msg); l.setVisible(true); l.setManaged(true); }
    private void clearErrors(Label... labels)    { for (Label l : labels) { l.setText(""); l.setVisible(false); l.setManaged(false); } }
    private void hide(Label l)                   { l.setVisible(false); l.setManaged(false); }

    private void showFeedback(Label label, String msg, boolean success) {
        label.setText(msg);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: "
                + (success ? "#16a34a" : "#dc2626") + ";");
        label.setVisible(true);
        label.setManaged(true);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
