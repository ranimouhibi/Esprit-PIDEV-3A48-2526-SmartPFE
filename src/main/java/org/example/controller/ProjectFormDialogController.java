package org.example.controller;

import org.example.dao.ProjectDAO;
import org.example.dao.UserDAO;
import org.example.model.Project;
import org.example.model.User;
import org.example.util.MailUtil;
import org.example.util.SessionManager;
import org.example.util.NotificationUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

public class ProjectFormDialogController implements Initializable {

    @FXML private Label dialogTitle;
    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField joinCodeField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private ComboBox<User> supervisorCombo;
    @FXML private Label titleError;
    @FXML private Label typeError;
    @FXML private Label descriptionError;
    @FXML private Label joinCodeError;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;

    private Stage dialogStage;
    private Project project;
    private Runnable onSaveCallback;
    private final ProjectDAO projectDAO = new ProjectDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Project types: individual, team
        typeCombo.setItems(FXCollections.observableArrayList("individual", "team"));
        
        // Status values from DB
        statusCombo.setItems(FXCollections.observableArrayList(
            "created", "waiting_supervisor", "supervised", 
            "waiting_validation", "in_progress", "suspended", 
            "finished", "archived"
        ));
        statusCombo.setValue("created");
        
        loadSupervisors();
        
        // Real-time validation
        titleField.textProperty().addListener((obs, old, newVal) -> validateTitle());
        typeCombo.valueProperty().addListener((obs, old, newVal) -> validateType());
        descriptionField.textProperty().addListener((obs, old, newVal) -> validateDescription());
        joinCodeField.textProperty().addListener((obs, old, newVal) -> validateJoinCode());
        
        // Input restrictions
        setupInputRestrictions();
    }

    private void setupInputRestrictions() {
        // Title: letters, numbers, spaces, and common punctuation only
        titleField.textProperty().addListener((obs, old, newVal) -> {
            if (newVal != null && !newVal.matches("[a-zA-Z0-9\\s\\-_.,!?()àâäéèêëïîôùûüÿçÀÂÄÉÈÊËÏÎÔÙÛÜŸÇ]*")) {
                titleField.setText(old);
            }
        });
        
        // Description: letters, numbers, spaces, and common punctuation
        descriptionField.textProperty().addListener((obs, old, newVal) -> {
            if (newVal != null && !newVal.matches("[a-zA-Z0-9\\s\\-_.,!?()\\n\\ràâäéèêëïîôùûüÿçÀÂÄÉÈÊËÏÎÔÙÛÜŸÇ]*")) {
                descriptionField.setText(old);
            }
        });
        
        // Join Code: uppercase letters and numbers only, max 10 chars
        joinCodeField.textProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                // Convert to uppercase and remove invalid chars
                String filtered = newVal.toUpperCase().replaceAll("[^A-Z0-9]", "");
                if (filtered.length() > 10) {
                    filtered = filtered.substring(0, 10);
                }
                if (!newVal.equals(filtered)) {
                    joinCodeField.setText(filtered);
                }
            }
        });
    }

    private void loadSupervisors() {
        try {
            User currentUser = SessionManager.getCurrentUser();
            List<User> supervisors = userDAO.findByRole("supervisor");
            
            // TODO: Filter by same establishment when establishment_id is available
            
            supervisorCombo.setItems(FXCollections.observableArrayList(supervisors));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setProject(Project project) {
        this.project = project;
        if (project != null) {
            dialogTitle.setText("Edit Project");
            saveButton.setText("Save Changes");
            populateForm();
        }
    }

    private void populateForm() {
        titleField.setText(project.getTitle());
        descriptionField.setText(project.getDescription());
        typeCombo.setValue(project.getProjectType());
        joinCodeField.setText(project.getJoinCode());
        statusCombo.setValue(project.getStatus());
        
        if (project.getSupervisorId() > 0) {
            supervisorCombo.getItems().stream()
                .filter(s -> s.getId() == project.getSupervisorId())
                .findFirst()
                .ifPresent(supervisorCombo::setValue);
        }
    }

    @FXML
    public void handleSave() {
        if (!validateForm()) {
            return;
        }

        try {
            User currentUser = SessionManager.getCurrentUser();
            
            if (project == null) {
                project = new Project();
                project.setOwnerId(currentUser.getId());
            }

            String title = titleField.getText().trim();
            String type  = typeCombo.getValue();
            int excludeId = project.getId(); // 0 for new, existing id for edit

            // ── Uniqueness check ──────────────────────────────────────────
            if (projectDAO.existsDuplicate(title, type, currentUser.getId(), excludeId)) {
                titleError.setText("! A project with the same title and type already exists today");
                titleField.setStyle("-fx-border-color: #dc2626; -fx-border-width: 2; -fx-border-radius: 8;");
                showError("Duplicate project: a project named \"" + title + "\" (" + type + ") was already created today.");
                return;
            }

            project.setTitle(title);
            project.setDescription(descriptionField.getText().trim());
            project.setProjectType(type);
            project.setStatus(statusCombo.getValue());
            
            // Join code
            String joinCode = joinCodeField.getText().trim();
            if (joinCode.isEmpty()) {
                joinCode = generateJoinCode();
            }
            project.setJoinCode(joinCode);
            
            // Supervisor
            if (supervisorCombo.getValue() != null) {
                project.setSupervisorId(supervisorCombo.getValue().getId());
            }

            if (project.getId() == 0) {
                projectDAO.save(project);

                // 🔔 Notification Desktop : Projet créé
                NotificationUtil.Notifications.projectCreated(project.getTitle());

                // 📧 Send emails: to supervisor + to owner (same as EmailService.php)
                User owner = SessionManager.getCurrentUser();
                // Re-fetch project to get supervisor email populated by DAO
                Project savedProject = projectDAO.findById(project.getId());
                if (savedProject != null) {
                    MailUtil.sendProjectCreatedEmail(savedProject, owner);
                }
            } else {
                String oldStatus = projectDAO.findById(project.getId()) != null
                    ? projectDAO.findById(project.getId()).getStatus() : null;
                projectDAO.update(project);

                // 🔔 Notification Desktop : Projet mis à jour
                NotificationUtil.Notifications.projectUpdated(project.getTitle());

                // 📧 Send status-change email if status changed
                if (oldStatus != null && !oldStatus.equals(project.getStatus())) {
                    User owner = SessionManager.getCurrentUser();
                    MailUtil.sendProjectStatusChangedEmail(project, owner, oldStatus);
                }
            }

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            
            dialogStage.close();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to save project: " + e.getMessage());
        }
    }

    private String generateJoinCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    private boolean validateForm() {
        boolean valid = true;
        
        if (!validateTitle()) valid = false;
        if (!validateType()) valid = false;
        if (!validateDescription()) valid = false;
        if (!validateJoinCode()) valid = false;
        
        return valid;
    }

    private boolean validateTitle() {
        String title = titleField.getText().trim();
        
        if (title.isEmpty()) {
            titleError.setText("! Title is required");
            titleField.setStyle("-fx-border-color: #dc2626; -fx-border-width: 2; -fx-border-radius: 8;");
            return false;
        }
        
        if (title.length() < 3) {
            titleError.setText("! Title must be at least 3 characters");
            titleField.setStyle("-fx-border-color: #dc2626; -fx-border-width: 2; -fx-border-radius: 8;");
            return false;
        }
        
        if (title.length() > 255) {
            titleError.setText("! Title is too long (max 255 characters)");
            titleField.setStyle("-fx-border-color: #dc2626; -fx-border-width: 2; -fx-border-radius: 8;");
            return false;
        }
        
        // Check for valid characters
        if (!title.matches("[a-zA-Z0-9\\s\\-_.,!?()àâäéèêëïîôùûüÿçÀÂÄÉÈÊËÏÎÔÙÛÜŸÇ]+")) {
            titleError.setText("! Title contains invalid characters");
            titleField.setStyle("-fx-border-color: #dc2626; -fx-border-width: 2; -fx-border-radius: 8;");
            return false;
        }
        
        titleError.setText("");
        titleField.setStyle("-fx-border-color: #22c55e; -fx-border-width: 2; -fx-border-radius: 8;");
        return true;
    }

    private boolean validateDescription() {
        String description = descriptionField.getText().trim();
        
        // Description is optional, but if provided, validate it
        if (!description.isEmpty()) {
            if (description.length() > 1000) {
                descriptionError.setText("! Description is too long (max 1000 characters)");
                descriptionField.setStyle("-fx-border-color: #dc2626; -fx-border-width: 2; -fx-border-radius: 8;");
                return false;
            }
            
            // Check for valid characters
            if (!description.matches("[a-zA-Z0-9\\s\\-_.,!?()\\n\\ràâäéèêëïîôùûüÿçÀÂÄÉÈÊËÏÎÔÙÛÜŸÇ]+")) {
                descriptionError.setText("! Description contains invalid characters");
                descriptionField.setStyle("-fx-border-color: #dc2626; -fx-border-width: 2; -fx-border-radius: 8;");
                return false;
            }
        }
        
        descriptionError.setText("");
        descriptionField.setStyle("-fx-border-color: #22c55e; -fx-border-width: 2; -fx-border-radius: 8;");
        return true;
    }

    private boolean validateJoinCode() {
        String code = joinCodeField.getText().trim();
        
        // Join code is optional (will be auto-generated if empty)
        if (!code.isEmpty()) {
            if (code.length() < 4) {
                joinCodeError.setText("! Join code must be at least 4 characters");
                joinCodeField.setStyle("-fx-border-color: #dc2626; -fx-border-width: 2; -fx-border-radius: 8;");
                return false;
            }
            
            if (code.length() > 10) {
                joinCodeError.setText("! Join code is too long (max 10 characters)");
                joinCodeField.setStyle("-fx-border-color: #dc2626; -fx-border-width: 2; -fx-border-radius: 8;");
                return false;
            }
            
            // Only uppercase letters and numbers
            if (!code.matches("[A-Z0-9]+")) {
                joinCodeError.setText("! Join code must contain only letters and numbers");
                joinCodeField.setStyle("-fx-border-color: #dc2626; -fx-border-width: 2; -fx-border-radius: 8;");
                return false;
            }
        }
        
        joinCodeError.setText("");
        joinCodeField.setStyle("-fx-border-color: #22c55e; -fx-border-width: 2; -fx-border-radius: 8;");
        return true;
    }

    private boolean validateType() {
        if (typeCombo.getValue() == null || typeCombo.getValue().isEmpty()) {
            typeError.setText("! Project type is required");
            typeCombo.setStyle("-fx-border-color: #dc2626; -fx-border-width: 2;");
            return false;
        }
        
        typeError.setText("");
        typeCombo.setStyle("-fx-border-color: #22c55e; -fx-border-width: 2;");
        return true;
    }

    private void showError(String message) {
        errorLabel.setText("! " + message);
        errorLabel.setVisible(true);
    }

    @FXML
    public void handleCancel() {
        dialogStage.close();
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }
}
