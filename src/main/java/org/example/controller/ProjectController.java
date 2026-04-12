package org.example.controller;

import org.example.dao.ProjectDAO;
import org.example.model.Project;
import org.example.util.PDFExporter;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ProjectController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> filterStatus;
    @FXML private ComboBox<String> sortCombo;
    @FXML private FlowPane projectsContainer;
    
    // Stats
    @FXML private Label statTotal;
    @FXML private Label statIndividual;
    @FXML private Label statTeam;
    @FXML private Label statInProgress;
    @FXML private Label statFinished;

    private final ProjectDAO projectDAO = new ProjectDAO();
    private List<Project> allProjects;
    private List<Project> filteredProjects;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filterType.setItems(FXCollections.observableArrayList("All Types", "individual", "team"));
        filterType.setValue("All Types");
        
        filterStatus.setItems(FXCollections.observableArrayList("All Status", "created", "waiting_supervisor", "supervised", 
            "waiting_validation", "in_progress", "suspended", "finished", "archived"));
        filterStatus.setValue("All Status");
        
        sortCombo.setItems(FXCollections.observableArrayList(
            "Date: Newest first", "Date: Oldest first", 
            "Title: A to Z", "Title: Z to A", 
            "Status: A to Z", "Status: Z to A"
        ));
        sortCombo.setValue("Date: Newest first");
        
        // Live listeners
        searchField.textProperty().addListener((obs, old, val) -> applyFilters());
        filterType.valueProperty().addListener((obs, old, val) -> applyFilters());
        filterStatus.valueProperty().addListener((obs, old, val) -> applyFilters());
        sortCombo.valueProperty().addListener((obs, old, val) -> applyFilters());
        
        loadProjects();
    }

    private void loadProjects() {
        try {
            allProjects = projectDAO.findAll();
            updateStats();
            applyFilters();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load projects: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void updateStats() {
        int total = allProjects.size();
        long individual = allProjects.stream().filter(p -> "individual".equals(p.getProjectType())).count();
        long team = allProjects.stream().filter(p -> "team".equals(p.getProjectType())).count();
        long inProgress = allProjects.stream().filter(p -> "in_progress".equals(p.getStatus())).count();
        long finished = allProjects.stream().filter(p -> "finished".equals(p.getStatus())).count();

        statTotal.setText(String.valueOf(total));
        statIndividual.setText(String.valueOf(individual));
        statTeam.setText(String.valueOf(team));
        statInProgress.setText(String.valueOf(inProgress));
        statFinished.setText(String.valueOf(finished));
    }

    private void applyFilters() {
        String query = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String typeFilter = filterType.getValue();
        String statusFilter = filterStatus.getValue();
        String sort = sortCombo.getValue();
        
        filteredProjects = new ArrayList<>();
        for (Project p : allProjects) {
            boolean matchQuery = query.isEmpty() || 
                (p.getTitle() != null && p.getTitle().toLowerCase().contains(query)) ||
                (p.getDescription() != null && p.getDescription().toLowerCase().contains(query));
            boolean matchType = typeFilter.equals("All Types") || 
                (p.getProjectType() != null && p.getProjectType().equals(typeFilter));
            boolean matchStatus = statusFilter.equals("All Status") || 
                (p.getStatus() != null && p.getStatus().equals(statusFilter));
            
            if (matchQuery && matchType && matchStatus) {
                filteredProjects.add(p);
            }
        }
        
        // Sort
        if (sort != null) {
            switch (sort) {
                case "Date: Oldest first":
                    filteredProjects.sort((a, b) -> {
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return a.getCreatedAt().compareTo(b.getCreatedAt());
                    });
                    break;
                case "Title: A to Z":
                    filteredProjects.sort((a, b) -> {
                        String ta = a.getTitle() != null ? a.getTitle() : "";
                        String tb = b.getTitle() != null ? b.getTitle() : "";
                        return ta.compareToIgnoreCase(tb);
                    });
                    break;
                case "Title: Z to A":
                    filteredProjects.sort((a, b) -> {
                        String ta = a.getTitle() != null ? a.getTitle() : "";
                        String tb = b.getTitle() != null ? b.getTitle() : "";
                        return tb.compareToIgnoreCase(ta);
                    });
                    break;
                case "Status: A to Z":
                    filteredProjects.sort((a, b) -> {
                        String sa = a.getStatus() != null ? a.getStatus() : "";
                        String sb = b.getStatus() != null ? b.getStatus() : "";
                        return sa.compareTo(sb);
                    });
                    break;
                case "Status: Z to A":
                    filteredProjects.sort((a, b) -> {
                        String sa = a.getStatus() != null ? a.getStatus() : "";
                        String sb = b.getStatus() != null ? b.getStatus() : "";
                        return sb.compareTo(sa);
                    });
                    break;
                default: // Date: Newest first
                    filteredProjects.sort((a, b) -> {
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });
                    break;
            }
        }
        
        displayProjects(filteredProjects);
    }

    @FXML public void handleSearch() { applyFilters(); }

    @FXML
    public void handleReset() {
        searchField.clear();
        filterType.setValue("All Types");
        filterStatus.setValue("All Status");
        sortCombo.setValue("Date: Newest first");
    }

    @FXML
    public void handleExportPDF() {
        if (filteredProjects == null || filteredProjects.isEmpty()) {
            showAlert("Warning", "No projects to export", Alert.AlertType.WARNING);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF Report");
        fileChooser.setInitialFileName("projects_report.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        
        File file = fileChooser.showSaveDialog(projectsContainer.getScene().getWindow());
        if (file != null) {
            try {
                PDFExporter.exportProjects(filteredProjects, file);
                showAlert("Success", "PDF exported successfully!", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to export PDF: " + e.getMessage(), Alert.AlertType.ERROR);
            }
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
        
        Label title = new Label("No projects found");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #666;");
        
        Label subtitle = new Label("No projects match your search criteria");
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

        // Owner
        HBox ownerBox = new HBox(6);
        ownerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label ownerIcon = new Label("[O]");
        ownerIcon.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #667eea;");
        Label ownerLabel = new Label("Owner: " + (project.getOwnerName() != null ? project.getOwnerName() : "N/A"));
        ownerLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        ownerBox.getChildren().addAll(ownerIcon, ownerLabel);

        // Supervisor
        HBox supervisorBox = new HBox(6);
        supervisorBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label supervisorIcon = new Label("[S]");
        supervisorIcon.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #a12c2f;");
        Label supervisorLabel = new Label("Supervisor: " + (project.getSupervisorName() != null ? project.getSupervisorName() : "No supervisor"));
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
        
        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 6; -fx-padding: 6 14; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> handleDeleteProject(project));
        
        actions.getChildren().add(deleteBtn);

        card.getChildren().addAll(header, title, description, ownerBox, supervisorBox, dateLabel, sep, actions);
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

    private void handleDeleteProject(Project project) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Project");
        confirm.setHeaderText("Delete \"" + project.getTitle() + "\"?");
        confirm.setContentText("This action cannot be undone. All related data will be deleted.");
        
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

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
