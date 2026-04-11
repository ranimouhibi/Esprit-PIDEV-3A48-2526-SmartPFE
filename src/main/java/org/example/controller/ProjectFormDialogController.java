package org.example.controller;

import org.example.dao.ProjectDAO;
import org.example.dao.UserDAO;
import org.example.model.Project;
import org.example.model.User;
import org.example.util.SessionManager;
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
                // TODO: Set establishment_id when available in User model
            }

            project.setTitle(titleField.getText().trim());
            project.setDescription(descriptionField.getText().trim());
            project.setProjectType(typeCombo.getValue());
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
            } else {
                projectDAO.update(project);
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
        
        titleError.setText("");
        titleField.setStyle("-fx-border-color: #22c55e; -fx-border-width: 2; -fx-border-radius: 8;");
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
