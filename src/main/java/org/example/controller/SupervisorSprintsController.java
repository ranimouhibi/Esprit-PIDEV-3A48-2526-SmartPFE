package org.example.controller;

import org.example.dao.ProjectDAO;
import org.example.dao.SprintDAO;
import org.example.dao.TaskDAO;
import org.example.model.Project;
import org.example.model.Sprint;
import org.example.model.Task;
import org.example.model.User;
import org.example.service.SprintAIService;
import org.example.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class SupervisorSprintsController implements Initializable {

    @FXML private ComboBox<Project> projectCombo;
    @FXML private TableView<Sprint> sprintTable;
    @FXML private TableColumn<Sprint, String> colNum;
    @FXML private TableColumn<Sprint, String> colName;
    @FXML private TableColumn<Sprint, String> colStatus;
    @FXML private TableColumn<Sprint, String> colDates;

    // AI Assignment panel
    @FXML private VBox aiPanel;
    @FXML private Label aiSprintLabel;
    @FXML private VBox aiRecommendationsBox;
    @FXML private Label aiStatusLabel;

    private final ProjectDAO projectDAO = new ProjectDAO();
    private final SprintDAO sprintDAO = new SprintDAO();
    private final SprintAIService aiService = new SprintAIService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colNum.setCellValueFactory(d -> new SimpleStringProperty("#" + d.getValue().getSprintNumber()));
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus() != null ? d.getValue().getStatus() : "-"));
        colDates.setCellValueFactory(d -> {
            Sprint s = d.getValue();
            String start = s.getStartDate() != null ? s.getStartDate().toString() : "?";
            String end = s.getEndDate() != null ? s.getEndDate().toString() : "?";
            return new SimpleStringProperty(start + " → " + end);
        });

        projectCombo.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Project p) { return p == null ? "" : p.getTitle(); }
            public Project fromString(String s) { return null; }
        });
        projectCombo.valueProperty().addListener((obs, o, sel) -> { if (sel != null) loadSprints(sel); });

        sprintTable.getSelectionModel().selectedItemProperty().addListener((obs, o, sel) -> {
            if (sel != null) showAIPanel(sel);
        });

        loadProjects();
    }

    private void loadProjects() {
        try {
            User user = SessionManager.getCurrentUser();
            List<Project> projects = projectDAO.findBySupervisor(user.getId());
            projectCombo.setItems(FXCollections.observableArrayList(projects));
            if (!projects.isEmpty()) projectCombo.setValue(projects.get(0));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadSprints(Project p) {
        try {
            sprintTable.setItems(FXCollections.observableArrayList(sprintDAO.findByProject(p.getId())));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAIPanel(Sprint sprint) {
        if (aiPanel == null) return;
        aiSprintLabel.setText("Sprint: " + sprint.getName() + " (#" + sprint.getSprintNumber() + ")");
        aiRecommendationsBox.getChildren().clear();
        Label loading = new Label("Click 'Get AI Recommendations' to analyze student skills.");
        loading.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
        aiRecommendationsBox.getChildren().add(loading);
    }

    @FXML public void handleAIAssign() {
        Sprint sprint = sprintTable.getSelectionModel().getSelectedItem();
        Project project = projectCombo.getValue();
        if (sprint == null || project == null) {
            if (aiStatusLabel != null) aiStatusLabel.setText("Select a project and sprint first.");
            return;
        }
        if (aiStatusLabel != null) aiStatusLabel.setText("Analyzing student skills...");
        aiRecommendationsBox.getChildren().clear();
        Label loading = new Label("🤖 Calculating AI recommendations...");
        loading.setStyle("-fx-text-fill: #667eea; -fx-font-size: 12px;");
        aiRecommendationsBox.getChildren().add(loading);

        new Thread(() -> {
            try {
                List<SprintAIService.StudentRecommendation> recs =
                    aiService.recommendStudentsForSprint(sprint.getId(), project.getId());
                Platform.runLater(() -> {
                    aiRecommendationsBox.getChildren().clear();
                    if (recs.isEmpty()) {
                        aiRecommendationsBox.getChildren().add(new Label("No students found."));
                        return;
                    }
                    int rank = 1;
                    for (SprintAIService.StudentRecommendation rec : recs) {
                        VBox card = buildRecommendationCard(rec, rank++);
                        aiRecommendationsBox.getChildren().add(card);
                    }
                    if (aiStatusLabel != null) aiStatusLabel.setText("Top " + recs.size() + " recommendations ready.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (aiStatusLabel != null) aiStatusLabel.setText("Error: " + e.getMessage());
                });
            }
        }).start();
    }

    private VBox buildRecommendationCard(SprintAIService.StudentRecommendation rec, int rank) {
        VBox card = new VBox(6);
        String borderColor = rank == 1 ? "#28a745" : rank == 2 ? "#ffc107" : "#667eea";
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 12; " +
            "-fx-border-color: " + borderColor + "; -fx-border-radius: 10; -fx-border-width: 2; " +
            "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,2);");
        VBox.setMargin(card, new Insets(0, 0, 8, 0));

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        String medal = rank == 1 ? "🥇" : rank == 2 ? "🥈" : "🥉";
        Label rankLbl = new Label(medal + " #" + rank);
        rankLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Label name = new Label(rec.student().getName());
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label score = new Label(String.format("%.1f%%", rec.score()));
        score.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + borderColor + ";");
        header.getChildren().addAll(rankLbl, name, sp, score);

        // Reason
        Label reason = new Label(rec.reason());
        reason.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        reason.setWrapText(true);

        // Matched skills chips
        if (!rec.matchedSkills().isEmpty()) {
            FlowPane chips = new FlowPane(4, 4);
            for (String skill : rec.matchedSkills()) {
                Label chip = new Label(skill);
                chip.setStyle("-fx-background-color: #28a74522; -fx-text-fill: #28a745; " +
                    "-fx-background-radius: 8; -fx-padding: 2 8; -fx-font-size: 10px;");
                chips.getChildren().add(chip);
            }
            card.getChildren().addAll(header, reason, chips);
        } else {
            card.getChildren().addAll(header, reason);
        }

        return card;
    }
}
