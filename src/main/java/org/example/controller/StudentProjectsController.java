package org.example.controller;

import org.example.dao.CommentDAO;
import org.example.dao.ProjectDAO;
import org.example.model.Project;
import org.example.model.User;
import org.example.service.ProjectService;
import org.example.util.PDFExporter;
import org.example.util.SessionManager;
import org.example.util.QRCodeUtil;
import org.example.util.ExcelExporter;
import org.example.util.ChartUtil;
import org.example.util.ChartDialog;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import org.example.util.ModernAlert;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.Desktop;
import java.io.File;
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
    
    // Stats
    @FXML private Label statTotal;
    @FXML private Label statIndividual;
    @FXML private Label statTeam;
    @FXML private Label statInProgress;

    private final ProjectDAO projectDAO = new ProjectDAO();
    private final CommentDAO commentDAO = new CommentDAO();
    private final ProjectService projectService = new ProjectService();
    private List<Project> allProjects;
    private List<Project> filteredProjects;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filterType.setItems(FXCollections.observableArrayList("All Types", "individual", "team"));
        filterType.setValue("All Types");
        
        filterStatus.setItems(FXCollections.observableArrayList("All Status", "created", "waiting_supervisor", "supervised", 
            "waiting_validation", "in_progress", "suspended", "finished", "archived"));
        filterStatus.setValue("All Status");
        
        loadProjects();
    }

    private void loadProjects() {
        try {
            User user = SessionManager.getCurrentUser();
            // Load all projects where user is owner OR member
            allProjects = projectDAO.findByUserProjects(user.getId());
            updateStats();
            displayProjects(allProjects);
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

        if (statTotal != null) statTotal.setText(String.valueOf(total));
        if (statIndividual != null) statIndividual.setText(String.valueOf(individual));
        if (statTeam != null) statTeam.setText(String.valueOf(team));
        if (statInProgress != null) statInProgress.setText(String.valueOf(inProgress));
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
        
        Button commentsBtn = new Button("💬 Comments");
        commentsBtn.setStyle("-fx-background-color: #5b7fc4; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 10; -fx-cursor: hand;");
        // Show comment count on button
        try {
            int count = commentDAO.findByProject(project.getId()).size();
            commentsBtn.setText("💬 Com. (" + count + ")");
        } catch (Exception ignored) {}
        commentsBtn.setOnAction(e -> handleViewComments(project));

        Button docsBtn = new Button("📄 Docs");
        docsBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 10; -fx-cursor: hand;");
        docsBtn.setOnAction(e -> handleViewDocuments(project));
        
        Button editBtn = new Button("✏️ Edit");
        editBtn.setStyle("-fx-background-color: #4a5568; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 10; -fx-cursor: hand;");
        editBtn.setOnAction(e -> handleEditProject(project));
        
        Button deleteBtn = new Button("🗑️ Delete");
        deleteBtn.setStyle("-fx-background-color: #a94442; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 10; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> handleDeleteProject(project));
        
        actions.getChildren().addAll(commentsBtn, docsBtn, editBtn, deleteBtn);
        
        // 🆕 Deuxième ligne d'actions (AI & Historique)
        HBox aiActions = new HBox(8);
        aiActions.setAlignment(javafx.geometry.Pos.CENTER);
        
        Button aiBtn = new Button("🤖 AI Suggest");
        aiBtn.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 10; -fx-cursor: hand;");
        aiBtn.setOnAction(e -> handleAISuggestions(project));
        
        Button historyBtn = new Button("📜 History");
        historyBtn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 10; -fx-cursor: hand;");
        historyBtn.setOnAction(e -> handleViewHistory(project));
        
        aiActions.getChildren().addAll(aiBtn, historyBtn);

        // 🆕 Troisième ligne d'actions (QR Code & Excel)
        HBox exportActions = new HBox(8);
        exportActions.setAlignment(javafx.geometry.Pos.CENTER);
        
        // QR Code button (only for team projects)
        if ("team".equalsIgnoreCase(project.getProjectType())) {
            Button qrBtn = new Button("📱 QR Code");
            qrBtn.setStyle("-fx-background-color: #4a5568; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 10; -fx-cursor: hand;");
            qrBtn.setOnAction(e -> handleGenerateQRCode(project));
            exportActions.getChildren().add(qrBtn);
        }
        
        Button excelBtn = new Button("📊 Export");
        excelBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 10; -fx-cursor: hand;");
        excelBtn.setOnAction(e -> handleExportProjectExcel(project));
        
        Button chartBtn = new Button("📈 Stats");
        chartBtn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 10; -fx-cursor: hand;");
        chartBtn.setOnAction(e -> handleGenerateCharts(project));
        
        exportActions.getChildren().addAll(excelBtn, chartBtn);

        // Add all elements to card
        card.getChildren().addAll(header, title, description);
        if (joinCodeBox != null) {
            card.getChildren().add(joinCodeBox);
        }
        card.getChildren().addAll(supervisorBox, dateLabel, sep, actions, aiActions, exportActions);
        
        return card;
    }

    private String getStatusColor(String status) {
        if (status == null) return "#888";
        String s = status.toLowerCase();
        if (s.equals("in_progress")) return "#22c55e";
        if (s.equals("supervised") || s.equals("waiting_validation")) return "#3b82f6";
        if (s.equals("finished")) return "#10b981";
        if (s.equals("archived")) return "#888";
        if (s.equals("suspended")) return "#ef4444";
        if (s.equals("waiting_supervisor")) return "#f59e0b";
        return "#6b7280";
    }

    @FXML
    public void handleAddProject() {
        openProjectDialog(null);
    }

    private void handleEditProject(Project project) {
        openProjectDialog(project);
    }

    private void handleViewDocuments(Project project) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProjectDocumentsDialog.fxml"));
            VBox dialogContent = loader.load();
            ProjectDocumentsDialogController controller = loader.getController();
            controller.setProject(project, true); // true = student can upload

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(StageStyle.UNDECORATED);
            
            Scene scene = new Scene(dialogContent);
            scene.setFill(javafx.scene.paint.Color.WHITE); // Force white background
            dialog.setScene(scene);
            
            controller.setDialogStage(dialog);
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open documents: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleViewComments(Project project) {
        try {
            // Count comments for badge
            int count = 0;
            try { count = commentDAO.findByProject(project.getId()).size(); } catch (Exception ignored) {}

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProjectCommentsDialog.fxml"));
            VBox dialogContent = loader.load();

            ProjectCommentsDialogController controller = loader.getController();
            controller.setProject(project);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(StageStyle.UNDECORATED);
            
            Scene scene = new Scene(dialogContent);
            scene.setFill(javafx.scene.paint.Color.WHITE); // Force white background
            dialog.setScene(scene);

            controller.setDialogStage(dialog);
            dialog.showAndWait();

            // Refresh cards after closing (comment count may have changed)
            loadProjects();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open comments: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void openProjectDialog(Project project) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProjectFormDialog.fxml"));
            VBox dialogContent = loader.load();
            
            ProjectFormDialogController controller = loader.getController();
            controller.setProject(project);
            
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(StageStyle.UNDECORATED);
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
        
        filteredProjects = allProjects.stream()
            .filter(p -> query.isEmpty() || p.getTitle().toLowerCase().contains(query))
            .filter(p -> typeFilter.equals("All Types") || p.getProjectType().equals(typeFilter))
            .filter(p -> statusFilter.equals("All Status") || p.getStatus().equals(statusFilter))
            .toList();
        
        displayProjects(filteredProjects);
    }

    @FXML
    public void handleReset() {
        searchField.clear();
        filterType.setValue("All Types");
        filterStatus.setValue("All Status");
        displayProjects(allProjects);
    }

    @FXML
    public void handleExportPDF() {
        List<Project> toExport = filteredProjects != null && !filteredProjects.isEmpty() ? filteredProjects : allProjects;
        
        if (toExport.isEmpty()) {
            showAlert("Warning", "No projects to export", Alert.AlertType.WARNING);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF Report");
        fileChooser.setInitialFileName("my_projects_report.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        
        File file = fileChooser.showSaveDialog(projectsContainer.getScene().getWindow());
        if (file != null) {
            try {
                PDFExporter.exportProjects(toExport, file);
                showAlert("Success", "PDF exported successfully!", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to export PDF: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    public void handleExportExcel() {
        List<Project> toExport = filteredProjects != null && !filteredProjects.isEmpty() ? filteredProjects : allProjects;
        
        if (toExport.isEmpty()) {
            showAlert("Warning", "No projects to export", Alert.AlertType.WARNING);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export to Excel");
        fileChooser.setInitialFileName("my_projects.xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        
        File file = fileChooser.showSaveDialog(projectsContainer.getScene().getWindow());
        if (file != null) {
            try {
                ExcelExporter.exportProjects(toExport, file);
                
                // Open Excel file
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                }
                
                showAlert("Success", 
                    "✅ Excel export successful!\n\n" +
                    "File: " + file.getName() + "\n" +
                    "Projects exported: " + toExport.size(), 
                    Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Excel export failed: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
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

    // ═══════════════════════════════════════════════════════════════════════
    // 🆕 MÉTIERS - AI SUGGESTIONS & HISTORY
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Show AI suggestions for a project
     */
    private void handleAISuggestions(Project project) {
        try {
            String suggestions = projectService.getAISuggestions(
                project.getTitle(),
                project.getDescription() != null ? project.getDescription() : ""
            );
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("💡 AI Suggestions");
            alert.setHeaderText("Suggestions to improve: " + project.getTitle());
            
            TextArea textArea = new TextArea(suggestions);
            textArea.setWrapText(true);
            textArea.setEditable(false);
            textArea.setPrefRowCount(10);
            textArea.setPrefColumnCount(50);
            
            alert.getDialogPane().setContent(textArea);
            alert.showAndWait();
            
        } catch (Exception e) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Error");
            error.setHeaderText("Unable to generate suggestions");
            error.setContentText(e.getMessage());
            error.showAndWait();
        }
    }

    /**
     * Show full project history with comments
     */
    private void handleViewHistory(Project project) {
        try {
            StringBuilder historyText = new StringBuilder();
            historyText.append("📜 Full History: ").append(project.getTitle()).append("\n\n");
            
            historyText.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            historyText.append("📅 PROJECT CREATION\n");
            historyText.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
            
            if (project.getCreatedAt() != null) {
                historyText.append("📅 Date: ").append(project.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
                historyText.append("👤 Created by: ").append(project.getOwnerName() != null ? project.getOwnerName() : "Owner").append("\n");
                historyText.append("📌 Title: ").append(project.getTitle()).append("\n");
                historyText.append("📂 Type: ").append(project.getProjectType() != null ? project.getProjectType() : "N/A").append("\n");
                historyText.append("🎯 Initial status: ").append(project.getStatus() != null ? project.getStatus() : "created").append("\n");
                
                if (project.getDescription() != null && !project.getDescription().isEmpty()) {
                    historyText.append("📝 Description: ").append(project.getDescription()).append("\n");
                }
                historyText.append("\n");
            }
            
            // Load and display comments
            try {
                List<org.example.model.Comment> comments = commentDAO.findByProject(project.getId());
                
                if (!comments.isEmpty()) {
                    historyText.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                    historyText.append("💬 COMMENTS & ACTIVITY (").append(comments.size()).append(")\n");
                    historyText.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
                    
                    comments.sort((a, b) -> {
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });
                    
                    int count = 1;
                    for (org.example.model.Comment comment : comments) {
                        historyText.append(count++).append(". ");
                        
                        if (comment.getCreatedAt() != null) {
                            historyText.append("📅 ").append(comment.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                        }
                        historyText.append(" - 👤 ").append(comment.getAuthorName() != null ? comment.getAuthorName() : "User").append("\n");
                        
                        historyText.append("   🏷️ Type: ").append(comment.getCommentType() != null ? comment.getCommentType().toUpperCase() : "COMMENT");
                        if (comment.getImportance() != null) {
                            String emoji = comment.getImportance().equals("urgent") ? "🔴" :
                                          comment.getImportance().equals("medium") ? "🟡" : "🟢";
                            historyText.append(" | ").append(emoji).append(" ").append(comment.getImportance().toUpperCase());
                        }
                        if (comment.getTarget() != null) {
                            historyText.append(" | 🎯 ").append(comment.getTarget());
                        }
                        historyText.append("\n");
                        
                        if (comment.getSubject() != null && !comment.getSubject().isEmpty()) {
                            historyText.append("   📌 ").append(comment.getSubject()).append("\n");
                        }
                        
                        if (comment.getContent() != null && !comment.getContent().isEmpty()) {
                            String content = comment.getContent();
                            if (content.length() > 150) content = content.substring(0, 150) + "...";
                            historyText.append("   💬 ").append(content).append("\n");
                        }
                        
                        historyText.append("\n");
                    }
                } else {
                    historyText.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                    historyText.append("💬 COMMENTS & ACTIVITY\n");
                    historyText.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
                    historyText.append("No comments yet for this project.\n\n");
                }
            } catch (Exception e) {
                historyText.append("\n⚠️ Unable to load comments\n\n");
            }
            
            // Last modification
            if (project.getUpdatedAt() != null && !project.getUpdatedAt().equals(project.getCreatedAt())) {
                historyText.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                historyText.append("🔄 LAST MODIFICATION\n");
                historyText.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
                historyText.append("📅 Date: ").append(project.getUpdatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
                historyText.append("🎯 Current status: ").append(project.getStatus() != null ? project.getStatus() : "N/A").append("\n");
                if (project.getSupervisorName() != null) {
                    historyText.append("👨‍🏫 Supervisor: ").append(project.getSupervisorName()).append("\n");
                }
                historyText.append("\n");
            }
            
            // Current state
            historyText.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            historyText.append("📊 CURRENT PROJECT STATE\n");
            historyText.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
            historyText.append("📌 Title: ").append(project.getTitle()).append("\n");
            historyText.append("📂 Type: ").append(project.getProjectType() != null ? project.getProjectType() : "N/A").append("\n");
            historyText.append("🎯 Status: ").append(project.getStatus() != null ? project.getStatus() : "N/A").append("\n");
            if (project.getSupervisorName() != null) {
                historyText.append("👨‍🏫 Supervisor: ").append(project.getSupervisorName()).append("\n");
            }
            if (project.getJoinCode() != null && "team".equalsIgnoreCase(project.getProjectType())) {
                historyText.append("🔑 Join Code: ").append(project.getJoinCode()).append("\n");
            }
            if (project.getAiSuggestions() != null && !project.getAiSuggestions().trim().isEmpty()) {
                historyText.append("\n💡 Saved AI Suggestions:\n");
                String suggestions = project.getAiSuggestions();
                if (suggestions.length() > 200) suggestions = suggestions.substring(0, 200) + "...";
                historyText.append(suggestions).append("\n");
            }
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("📜 Project History");
            alert.setHeaderText("Project: " + project.getTitle());
            
            TextArea textArea = new TextArea(historyText.toString());
            textArea.setWrapText(true);
            textArea.setEditable(false);
            textArea.setPrefRowCount(25);
            textArea.setPrefColumnCount(70);
            textArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 11px;");
            
            alert.getDialogPane().setContent(textArea);
            alert.getDialogPane().setPrefWidth(800);
            alert.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Error");
            error.setHeaderText("Unable to load history");
            error.setContentText(e.getMessage());
            error.showAndWait();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🆕 NOUVELLES APIs - QR CODE, EXCEL, CHARTS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Generate a QR Code for a team project
     */
    private void handleGenerateQRCode(Project project) {
        try {
            // Generate the QR code
            String qrPath = QRCodeUtil.generateProjectQRCode(project.getId(), project.getTitle());
            
            if (qrPath != null) {
                // Display QR code in a JavaFX Dialog (instead of gallery)
                QRCodeUtil.showQRCodeDialog(qrPath, project.getTitle());
            } else {
                showAlert("Error", "Failed to generate QR code", Alert.AlertType.ERROR);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Error generating QR Code:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Export a project to Excel with its comments
     */
    private void handleExportProjectExcel(Project project) {
        try {
            List<org.example.model.Comment> comments = commentDAO.findByProject(project.getId());
            List<org.example.model.Document> documents = new org.example.dao.DocumentDAO().findByProject(project.getId());
            
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export to Excel");
            fileChooser.setInitialFileName("project_" + project.getId() + "_" + project.getTitle().replaceAll("[^a-zA-Z0-9]", "_") + ".xlsx");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            
            File file = fileChooser.showSaveDialog(projectsContainer.getScene().getWindow());
            if (file != null) {
                ExcelExporter.exportProjectStatistics(project, comments, documents, file);
                
                // Open Excel file
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                }
                
                showAlert("Success", 
                    "✅ Excel export successful!\n\n" +
                    "File: " + file.getName() + "\n\n" +
                    "Content:\n" +
                    "• Project information\n" +
                    "• Statistics\n" +
                    "• " + comments.size() + " comments\n" +
                    "• " + documents.size() + " documents", 
                    Alert.AlertType.INFORMATION);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Error exporting to Excel:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Générer des graphiques pour un projet
     */
    private void handleGenerateCharts(Project project) {
        try {
            List<org.example.model.Comment> comments = commentDAO.findByProject(project.getId());
            
            if (comments.isEmpty()) {
                showAlert("Information", 
                    "No comments available to generate charts.\n\n" +
                    "Add comments to the project to see statistics.", 
                    Alert.AlertType.INFORMATION);
                return;
            }
            
            // Display charts in a JavaFX Dialog (instead of Chrome)
            ChartDialog.showCommentStatistics(comments, project.getTitle());
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Error generating charts:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}