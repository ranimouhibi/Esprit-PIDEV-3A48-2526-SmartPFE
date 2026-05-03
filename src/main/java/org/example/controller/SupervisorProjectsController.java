package org.example.controller;

import org.example.config.DatabaseConfig;
import org.example.dao.ProjectDAO;
import org.example.dao.SprintDAO;
import org.example.dao.TaskDAO;
import org.example.dao.UserDAO;
import org.example.model.Project;
import org.example.model.Sprint;
import org.example.model.Task;
import org.example.model.User;
import org.example.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.ResourceBundle;

public class SupervisorProjectsController implements Initializable {

    @FXML private TableView<Project> projectTable;
    @FXML private TableColumn<Project, String> colTitle;
    @FXML private TableColumn<Project, String> colStatus;
    @FXML private TableColumn<Project, String> colOwner;
    @FXML private TableColumn<Project, String> colType;

    @FXML private VBox detailPanel;
    @FXML private Label detailTitle;
    @FXML private Label detailStatus;
    @FXML private Label detailOwner;
    @FXML private Label detailDesc;
    @FXML private VBox membersBox;
    @FXML private VBox burndownBox;
    @FXML private VBox sprintsBox;

    private final ProjectDAO projectDAO = new ProjectDAO();
    private final SprintDAO sprintDAO = new SprintDAO();
    private final TaskDAO taskDAO = new TaskDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colTitle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus() != null ? d.getValue().getStatus() : "-"));
        colOwner.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getOwnerName() != null ? d.getValue().getOwnerName() : "-"));
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProjectType() != null ? d.getValue().getProjectType() : "-"));

        projectTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) showProjectDetail(sel);
        });

        loadProjects();
    }

    private void loadProjects() {
        try {
            User user = SessionManager.getCurrentUser();
            List<Project> projects = projectDAO.findBySupervisor(user.getId());
            projectTable.setItems(FXCollections.observableArrayList(projects));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showProjectDetail(Project p) {
        detailTitle.setText(p.getTitle());
        detailStatus.setText("Status: " + (p.getStatus() != null ? p.getStatus().toUpperCase() : "-"));
        detailOwner.setText("Owner: " + (p.getOwnerName() != null ? p.getOwnerName() : "-"));
        detailDesc.setText(p.getDescription() != null ? p.getDescription() : "No description.");

        loadMembers(p);
        loadBurndown(p);
        loadSprintSummary(p);
    }

    private void loadMembers(Project p) {
        membersBox.getChildren().clear();
        Label title = new Label("Team Members");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1a1a2e;");
        membersBox.getChildren().add(title);
        try {
            String sql = "SELECT u.name, u.role, u.email FROM project_members pm " +
                         "JOIN users u ON pm.user_id = u.id WHERE pm.project_id = ?";
            try (java.sql.PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
                ps.setInt(1, p.getId());
                ResultSet rs = ps.executeQuery();
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);
                    Label avatar = new Label("👤");
                    Label name = new Label(rs.getString("name"));
                    name.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                    Label role = new Label("(" + rs.getString("role") + ")");
                    role.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
                    row.getChildren().addAll(avatar, name, role);
                    membersBox.getChildren().add(row);
                }
                if (!found) {
                    // Fallback: show owner
                    User owner = userDAO.findById(p.getOwnerId());
                    if (owner != null) {
                        HBox row = new HBox(8);
                        row.setAlignment(Pos.CENTER_LEFT);
                        row.getChildren().addAll(new Label("👤"), new Label(owner.getName()), new Label("(owner)"));
                        membersBox.getChildren().add(row);
                    }
                }
            }
        } catch (Exception e) {
            membersBox.getChildren().add(new Label("Could not load members."));
        }
    }

    private void loadBurndown(Project p) {
        burndownBox.getChildren().clear();
        Label title = new Label("Burndown Chart — Sprint Progress");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1a1a2e;");
        burndownBox.getChildren().add(title);

        try {
            List<Sprint> sprints = sprintDAO.findByProject(p.getId());
            if (sprints.isEmpty()) {
                burndownBox.getChildren().add(new Label("No sprints yet."));
                return;
            }

            // Simple bar-based burndown per sprint
            for (Sprint sprint : sprints) {
                List<Task> tasks = taskDAO.findBySprint(sprint.getId());
                int total = tasks.size();
                long done = tasks.stream().filter(t -> "done".equals(t.getStatus())).count();
                long inProgress = tasks.stream().filter(t -> "in_progress".equals(t.getStatus())).count();
                long todo = tasks.stream().filter(t -> "todo".equals(t.getStatus())).count();

                VBox sprintBar = new VBox(4);
                sprintBar.setStyle("-fx-background-color: #f8f9ff; -fx-background-radius: 8; -fx-padding: 8;");

                HBox header = new HBox(8);
                header.setAlignment(Pos.CENTER_LEFT);
                Label sprintName = new Label(sprint.getName());
                sprintName.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
                Label statusBadge = new Label(sprint.getStatus() != null ? sprint.getStatus().toUpperCase() : "");
                String badgeColor = "active".equals(sprint.getStatus()) ? "#28a745" : "closed".equals(sprint.getStatus()) ? "#6c757d" : "#ffc107";
                statusBadge.setStyle("-fx-background-color: " + badgeColor + "22; -fx-text-fill: " + badgeColor +
                    "; -fx-background-radius: 4; -fx-padding: 2 6; -fx-font-size: 10px;");
                header.getChildren().addAll(sprintName, statusBadge);

                // Progress bar
                double pct = total > 0 ? (double) done / total : 0;
                HBox barRow = new HBox(4);
                barRow.setAlignment(Pos.CENTER_LEFT);

                // Stacked bar: done (green) + in_progress (yellow) + todo (gray)
                HBox stackedBar = new HBox(0);
                stackedBar.setPrefHeight(14);
                stackedBar.setMaxWidth(Double.MAX_VALUE);
                stackedBar.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 7;");
                HBox.setHgrow(stackedBar, Priority.ALWAYS);

                if (total > 0) {
                    double doneW = (double) done / total;
                    double ipW = (double) inProgress / total;
                    if (doneW > 0) {
                        Region doneBar = new Region();
                        doneBar.setPrefHeight(14);
                        doneBar.setPrefWidth(doneW * 200);
                        doneBar.setStyle("-fx-background-color: #28a745; -fx-background-radius: 7 0 0 7;");
                        stackedBar.getChildren().add(doneBar);
                    }
                    if (ipW > 0) {
                        Region ipBar = new Region();
                        ipBar.setPrefHeight(14);
                        ipBar.setPrefWidth(ipW * 200);
                        ipBar.setStyle("-fx-background-color: #ffc107;");
                        stackedBar.getChildren().add(ipBar);
                    }
                }

                Label pctLabel = new Label(String.format("%.0f%% done (%d/%d tasks)", pct * 100, done, total));
                pctLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");

                barRow.getChildren().addAll(stackedBar, pctLabel);
                sprintBar.getChildren().addAll(header, barRow);
                VBox.setMargin(sprintBar, new Insets(0, 0, 6, 0));
                burndownBox.getChildren().add(sprintBar);
            }
        } catch (Exception e) {
            burndownBox.getChildren().add(new Label("Error loading burndown: " + e.getMessage()));
        }
    }

    private void loadSprintSummary(Project p) {
        sprintsBox.getChildren().clear();
        Label title = new Label("Sprint Summary");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1a1a2e;");
        sprintsBox.getChildren().add(title);
        try {
            List<Sprint> sprints = sprintDAO.findByProject(p.getId());
            for (Sprint s : sprints) {
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                Label num = new Label("#" + s.getSprintNumber());
                num.setStyle("-fx-font-weight: bold; -fx-text-fill: #a12c2f; -fx-font-size: 12px;");
                Label name = new Label(s.getName());
                name.setStyle("-fx-font-size: 12px;");
                String dates = (s.getStartDate() != null ? s.getStartDate().toString() : "?") +
                               " → " + (s.getEndDate() != null ? s.getEndDate().toString() : "?");
                Label dateLbl = new Label(dates);
                dateLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");
                row.getChildren().addAll(num, name, dateLbl);
                sprintsBox.getChildren().add(row);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
