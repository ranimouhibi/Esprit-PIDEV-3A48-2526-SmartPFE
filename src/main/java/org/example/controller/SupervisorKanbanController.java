package org.example.controller;

import org.example.dao.ProjectDAO;
import org.example.dao.TaskDAO;
import org.example.model.Project;
import org.example.model.Task;
import org.example.model.User;
import org.example.util.SessionManager;
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

public class SupervisorKanbanController implements Initializable {

    @FXML private ComboBox<Project> projectCombo;
    @FXML private VBox todoCol;
    @FXML private VBox inProgressCol;
    @FXML private VBox reviewCol;
    @FXML private VBox doneCol;
    @FXML private Label todoCount;
    @FXML private Label inProgressCount;
    @FXML private Label reviewCount;
    @FXML private Label doneCount;
    @FXML private Label statusLabel;

    private final ProjectDAO projectDAO = new ProjectDAO();
    private final TaskDAO taskDAO = new TaskDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        projectCombo.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Project p) { return p == null ? "" : p.getTitle(); }
            public Project fromString(String s) { return null; }
        });
        projectCombo.valueProperty().addListener((obs, o, sel) -> { if (sel != null) loadKanban(sel); });

        try {
            User user = SessionManager.getCurrentUser();
            List<Project> projects = projectDAO.findBySupervisor(user.getId());
            projectCombo.setItems(FXCollections.observableArrayList(projects));
            if (!projects.isEmpty()) projectCombo.setValue(projects.get(0));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadKanban(Project p) {
        clearColumns();
        try {
            List<Task> tasks = taskDAO.findByProject(p.getId());
            for (Task t : tasks) {
                VBox card = buildTaskCard(t);
                switch (t.getStatus() != null ? t.getStatus() : "todo") {
                    case "in_progress" -> inProgressCol.getChildren().add(card);
                    case "review"      -> reviewCol.getChildren().add(card);
                    case "done"        -> doneCol.getChildren().add(card);
                    default            -> todoCol.getChildren().add(card);
                }
            }
            updateCounts(tasks);
        } catch (Exception e) {
            if (statusLabel != null) statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void clearColumns() {
        todoCol.getChildren().clear();
        inProgressCol.getChildren().clear();
        reviewCol.getChildren().clear();
        doneCol.getChildren().clear();
    }

    private void updateCounts(List<Task> tasks) {
        long todo = tasks.stream().filter(t -> "todo".equals(t.getStatus()) || t.getStatus() == null).count();
        long ip   = tasks.stream().filter(t -> "in_progress".equals(t.getStatus())).count();
        long rev  = tasks.stream().filter(t -> "review".equals(t.getStatus())).count();
        long done = tasks.stream().filter(t -> "done".equals(t.getStatus())).count();
        if (todoCount != null) todoCount.setText(String.valueOf(todo));
        if (inProgressCount != null) inProgressCount.setText(String.valueOf(ip));
        if (reviewCount != null) reviewCount.setText(String.valueOf(rev));
        if (doneCount != null) doneCount.setText(String.valueOf(done));
    }

    private VBox buildTaskCard(Task t) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
            "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),6,0,0,2); " +
            "-fx-border-color: " + priorityColor(t.getPriority()) + "44; -fx-border-radius: 8; -fx-border-width: 1;");
        VBox.setMargin(card, new Insets(0, 0, 8, 0));

        // Title
        Label title = new Label(t.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #1a1a2e;");
        title.setWrapText(true);

        // Priority badge
        HBox meta = new HBox(6);
        meta.setAlignment(Pos.CENTER_LEFT);
        if (t.getPriority() != null) {
            Label prio = new Label(t.getPriority().toUpperCase());
            prio.setStyle("-fx-background-color: " + priorityColor(t.getPriority()) + "22; " +
                "-fx-text-fill: " + priorityColor(t.getPriority()) + "; " +
                "-fx-background-radius: 4; -fx-padding: 2 6; -fx-font-size: 9px; -fx-font-weight: bold;");
            meta.getChildren().add(prio);
        }
        if (t.getStoryPoints() > 0) {
            Label sp = new Label(t.getStoryPoints() + " pts");
            sp.setStyle("-fx-background-color: #667eea22; -fx-text-fill: #667eea; -fx-background-radius: 4; -fx-padding: 2 6; -fx-font-size: 9px;");
            meta.getChildren().add(sp);
        }
        if (t.isBlocked()) {
            Label blocked = new Label("🚫 BLOCKED");
            blocked.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 9px; -fx-font-weight: bold;");
            meta.getChildren().add(blocked);
        }

        card.getChildren().addAll(title, meta);

        // Assigned to
        if (t.getAssignedToName() != null) {
            Label assigned = new Label("👤 " + t.getAssignedToName());
            assigned.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");
            card.getChildren().add(assigned);
        }

        // Deadline
        if (t.getDeadline() != null) {
            boolean overdue = t.getDeadline().isBefore(java.time.LocalDate.now()) && !"done".equals(t.getStatus());
            Label deadline = new Label("📅 " + t.getDeadline());
            deadline.setStyle("-fx-font-size: 10px; -fx-text-fill: " + (overdue ? "#dc3545" : "#888") + ";" +
                (overdue ? " -fx-font-weight: bold;" : ""));
            card.getChildren().add(deadline);
        }

        // Move buttons
        HBox actions = new HBox(4);
        actions.setAlignment(Pos.CENTER_RIGHT);
        String[] statuses = {"todo", "in_progress", "review", "done"};
        String[] labels = {"Todo", "In Progress", "Review", "Done"};
        for (int i = 0; i < statuses.length; i++) {
            if (!statuses[i].equals(t.getStatus())) {
                Button btn = new Button(labels[i]);
                btn.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #333; -fx-background-radius: 4; " +
                    "-fx-padding: 3 8; -fx-font-size: 9px; -fx-cursor: hand;");
                final String newStatus = statuses[i];
                btn.setOnAction(e -> moveTask(t, newStatus));
                actions.getChildren().add(btn);
            }
        }
        card.getChildren().add(actions);
        return card;
    }

    private void moveTask(Task t, String newStatus) {
        try {
            t.setStatus(newStatus);
            taskDAO.update(t);
            loadKanban(projectCombo.getValue());
        } catch (Exception e) {
            if (statusLabel != null) statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private String priorityColor(String priority) {
        if (priority == null) return "#888";
        return switch (priority) {
            case "critical" -> "#dc3545";
            case "high"     -> "#fd7e14";
            case "medium"   -> "#ffc107";
            default         -> "#28a745";
        };
    }
}
