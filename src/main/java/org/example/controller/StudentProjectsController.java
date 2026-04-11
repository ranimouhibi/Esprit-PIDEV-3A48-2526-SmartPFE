package org.example.controller;

import org.example.dao.ProjectDAO;
import org.example.model.Project;
import org.example.model.User;
import org.example.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class StudentProjectsController implements Initializable {

    @FXML private TextField searchField;
    @FXML private TextField joinCodeInput;
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> filterStatus;
    @FXML private FlowPane projectsContainer;

    private final ProjectDAO projectDAO = new ProjectDAO();
    private List<Project> allProjects;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filterType.getItems().addAll("All Types", "individual", "team");
        filterType.setValue("All Types");
        
        filterStatus.getItems().addAll("All Status", "created", "waiting_supervisor", "supervised", 
            "waiting_validation", "in_progress", "suspended", "finished", "archived");
        filterStatus.setValue("All Status");
        
        loadProjects();
    }

    private void loadProjects() {
        try {
            User user = SessionManager.getCurrentUser();
            // Load all projects where user is owner OR member
            allProjects = projectDAO.findByUserProjects(user.getId());
            displayProjects(allProjects);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load projects: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void displayProjects(List<Project> projects) {
        projectsContainer.getChildren().clear();
        
        if (projects.isEmpty()) {
            VBox emptyState = createEmptyState();
            projectsContainer.getChildren().add(emptyState);
            return;
        }

        for (Project project : projects) {
            VBox card = createProjectCard(project);
            projectsContainer.getChildren().add(card);
        }
    }

    private VBox createEmptyState() {
        VBox empty = new VBox(12);
        empty.setAlignment(javafx.geometry.Pos.CENTER);
        empty.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e0e0e0; -fx-border-radius: 12; -fx-border-width: 2; -fx-border-style: dashed;");
        empty.setPrefSize(400, 200);
        empty.setPadding(new Insets(30));
        
        Label icon = new Label("[EMPTY]");
        icon.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #999;");
        
        Label title = new Label("No projects yet");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #666;");
        
        Label subtitle = new Label("Click 'New Project' to create your first project");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #999;");
        
        empty.getChildren().addAll(icon, title, subtitle);
        return empty;
    }

    private VBox createProjectCard(Project project) {
        VBox card = new VBox(14);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 3); -fx-border-color: #f0f0f0; -fx-border-radius: 14; -fx-border-width: 1;");
        card.setPrefWidth(340);
        card.setPadding(new Insets(20));

        // Header
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label typeLabel = new Label(project.getProjectType() != null ? project.getProjectType().toUpperCase() : "N/A");
        typeLabel.setStyle("-fx-background-color: #a12c2f22; -fx-text-fill: #a12c2f; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 4 10;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label statusLabel = new Label("• " + (project.getStatus() != null ? project.getStatus() : "created"));
        String statusColor = getStatusColor(project.getStatus());
        statusLabel.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        
        header.getChildren().addAll(typeLabel, spacer, statusLabel);

        // Title
        Label title = new Label(project.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #1e1e1e;");
        title.setWrapText(true);
        title.setMaxWidth(300);

        // Description
        Label description = new Label(project.getDescription() != null && !project.getDescription().isEmpty() 
            ? project.getDescription() : "No description");
        description.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
        description.setWrapText(true);
        description.setMaxWidth(300);
        description.setMaxHeight(60);

        // Join Code (only for team projects)
        VBox joinCodeBox = null;
        if ("team".equalsIgnoreCase(project.getProjectType()) && project.getJoinCode() != null) {
            joinCodeBox = new VBox(4);
            joinCodeBox.setStyle("-fx-background-color: #f0f9ff; -fx-background-radius: 8; -fx-padding: 10 12; -fx-border-color: #3b82f6; -fx-border-width: 1; -fx-border-radius: 8;");
            
            Label joinLabel = new Label("Join Code for Team:");
            joinLabel.setStyle("-fx-text-fill: #1e40af; -fx-font-size: 10px; -fx-font-weight: bold;");
            
            Label codeLabel = new Label(project.getJoinCode());
            codeLabel.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: 'Courier New';");
            
            joinCodeBox.getChildren().addAll(joinLabel, codeLabel);
        }

        // Supervisor
        HBox supervisorBox = new HBox(6);
        supervisorBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label supervisorIcon = new Label("[S]");
        supervisorIcon.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #a12c2f;");
        Label supervisorLabel = new Label(project.getSupervisorName() != null ? project.getSupervisorName() : "No supervisor");
        supervisorLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        supervisorBox.getChildren().addAll(supervisorIcon, supervisorLabel);

        // Date
        Label dateLabel = new Label("Created: " + (project.getCreatedAt() != null 
            ? project.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A"));
        dateLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");

        Separator sep = new Separator();

        // Actions
        HBox actions = new HBox(8);
        actions.setAlignment(javafx.geometry.Pos.CENTER);
        
        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 6; -fx-padding: 6 14; -fx-cursor: hand;");
        editBtn.setOnAction(e -> handleEditProject(project));
        
        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 6; -fx-padding: 6 14; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> handleDeleteProject(project));
        
        actions.getChildren().addAll(editBtn, deleteBtn);

        // Add all elements to card
        card.getChildren().addAll(header, title, description);
        if (joinCodeBox != null) {
            card.getChildren().add(joinCodeBox);
        }
        card.getChildren().addAll(supervisorBox, dateLabel, sep, actions);
        
        return card;
    }

    private String getStatusColor(String status) {
        if (status == null) return "#888";
        return switch (status.toLowerCase()) {
            case "in_progress" -> "#22c55e";
            case "supervised", "waiting_validation" -> "#3b82f6";
            case "finished" -> "#10b981";
            case "archived" -> "#888";
            case "suspended" -> "#ef4444";
            case "waiting_supervisor" -> "#f59e0b";
            default -> "#6b7280";
        };
    }

    @FXML
    public void handleAddProject() {
        openProjectDialog(null);
    }

    private void handleEditProject(Project project) {
        openProjectDialog(project);
    }

    private void openProjectDialog(Project project) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProjectFormDialog.fxml"));
            VBox dialogContent = loader.load();
            
            ProjectFormDialogController controller = loader.getController();
            controller.setProject(project);
            
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(StageStyle.TRANSPARENT);
            dialog.setScene(new Scene(dialogContent, 520, 650));
            
            controller.setDialogStage(dialog);
            controller.setOnSaveCallback(this::loadProjects);
            
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open dialog: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleDeleteProject(Project project) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Project");
        confirm.setHeaderText("Delete \"" + project.getTitle() + "\"?");
        confirm.setContentText("This action cannot be undone.");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                projectDAO.delete(project.getId());
                showAlert("Success", "Project deleted successfully", Alert.AlertType.INFORMATION);
                loadProjects();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to delete project: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    public void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        String typeFilter = filterType.getValue();
        String statusFilter = filterStatus.getValue();
        
        List<Project> filtered = allProjects.stream()
            .filter(p -> query.isEmpty() || p.getTitle().toLowerCase().contains(query))
            .filter(p -> typeFilter.equals("All Types") || p.getProjectType().equals(typeFilter))
            .filter(p -> statusFilter.equals("All Status") || p.getStatus().equals(statusFilter))
            .toList();
        
        displayProjects(filtered);
    }

    @FXML
    public void handleReset() {
        searchField.clear();
        filterType.setValue("All Types");
        filterStatus.setValue("All Status");
        displayProjects(allProjects);
    }

    @FXML
    public void handleJoinProject() {
        String code = joinCodeInput.getText().trim().toUpperCase();
        
        if (code.isEmpty()) {
            showAlert("Error", "Please enter a join code", Alert.AlertType.WARNING);
            return;
        }

        try {
            User currentUser = SessionManager.getCurrentUser();
            Project project = projectDAO.findByJoinCode(code);
            
            if (project == null) {
                showAlert("Error", "Invalid join code. Project not found.", Alert.AlertType.ERROR);
                return;
            }
            
            if (!"team".equalsIgnoreCase(project.getProjectType())) {
                showAlert("Error", "This project is not a team project.", Alert.AlertType.ERROR);
                return;
            }
            
            if (project.getOwnerId() == currentUser.getId()) {
                showAlert("Info", "You are already the owner of this project.", Alert.AlertType.INFORMATION);
                return;
            }
            
            // Check if already a member
            if (projectDAO.isMember(project.getId(), currentUser.getId())) {
                showAlert("Info", "You are already a member of this project.", Alert.AlertType.INFORMATION);
                return;
            }
            
            // Add user as member
            projectDAO.addMember(project.getId(), currentUser.getId());
            
            showAlert("Success", "Join request sent! Waiting for project owner approval.\nProject: " + project.getTitle(), Alert.AlertType.INFORMATION);
            joinCodeInput.clear();
            loadProjects();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to join project: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
