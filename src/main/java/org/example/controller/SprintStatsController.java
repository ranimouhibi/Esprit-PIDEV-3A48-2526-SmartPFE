package org.example.controller;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.example.dao.ProjectDAO;
import org.example.dao.SprintDAO;
import org.example.dao.TaskDAO;
import org.example.model.Sprint;
import org.example.model.Task;
import org.example.util.PDFExporter;
import org.example.util.SessionManager;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class SprintStatsController {

    private final Stage stage;
    private List<Sprint> sprints;
    private List<Task> allTasks;

    public SprintStatsController(Window owner) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Sprint Statistics");
        stage.setWidth(950);
        stage.setHeight(700);
        stage.setResizable(true);
    }

    public void show() {
        try {
            var user = SessionManager.getCurrentUser();
            var projectDAO = new ProjectDAO();
            var projects = (user != null)
                ? projectDAO.findForUser(user.getId(), user.getRole())
                : projectDAO.findAll();
            sprints = new ArrayList<>();
            for (var p : projects) sprints.addAll(new SprintDAO().findByProject(p.getId()));
            allTasks = new TaskDAO().findAll();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Load error: " + e.getMessage()).showAndWait();
            return;
        }
        stage.setScene(new Scene(buildLayout()));
        stage.show();
    }

    private BorderPane buildLayout() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f4f6fb;");
        root.setPadding(new Insets(24, 28, 24, 28));

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(16);
        header.setPadding(new Insets(0, 0, 16, 0));
        Label title = new Label("Sprint Statistics");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button exportBtn = new Button("Export PDF");
        exportBtn.setStyle("-fx-background-color: #a12c2f; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 18; -fx-font-weight: bold; -fx-cursor: hand;");
        exportBtn.setOnAction(e -> handleExport());
        header.getChildren().addAll(title, spacer, exportBtn);
        root.setTop(header);

        long totalSprints = sprints.size();
        long totalTasks   = allTasks.stream().filter(t -> sprintIds().contains(t.getSprintId())).count();

        HBox cards = new HBox(12);
        cards.setPadding(new Insets(0, 0, 16, 0));
        cards.getChildren().addAll(
            statCard("Sprints",     String.valueOf(totalSprints),          "#667eea"),
            statCard("Tasks",       String.valueOf(totalTasks),            "#a12c2f"),
            statCard("To Do",       String.valueOf(countByStatus("todo")), "#f59e0b"),
            statCard("In Progress", String.valueOf(countByStatus("in_progress")), "#3b82f6"),
            statCard("Done",        String.valueOf(countByStatus("done")), "#10b981")
        );

        HBox charts = new HBox(16);
        charts.setPrefHeight(260);
        charts.getChildren().addAll(buildStatusPieChart(), buildStudentBarChart());
        HBox.setHgrow(charts.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(charts.getChildren().get(1), Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        scroll.setContent(new VBox(12, cards, charts, buildSprintProgressTable()));
        root.setCenter(scroll);
        return root;
    }

    private Set<Integer> sprintIds() {
        return sprints.stream().map(Sprint::getId).collect(Collectors.toSet());
    }

    private long countByStatus(String status) {
        Set<Integer> ids = sprintIds();
        return allTasks.stream().filter(t -> ids.contains(t.getSprintId()) && status.equals(t.getStatus())).count();
    }

    private VBox statCard(String label, String value, String color) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 12;");
        card.setPrefWidth(160);
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.9);");
        card.getChildren().addAll(val, lbl);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private PieChart buildStatusPieChart() {
        PieChart chart = new PieChart(FXCollections.observableArrayList(
            new PieChart.Data("To Do",       countByStatus("todo")),
            new PieChart.Data("In Progress", countByStatus("in_progress")),
            new PieChart.Data("Done",        countByStatus("done"))
        ));
        chart.setTitle("Tasks by Status");
        chart.setStyle("-fx-background-color: white; -fx-background-radius: 12;");
        chart.setPrefHeight(260);
        return chart;
    }

    private BarChart<String, Number> buildStudentBarChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Student"); yAxis.setLabel("Tasks");
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Tasks per Student");
        chart.setStyle("-fx-background-color: white; -fx-background-radius: 12;");
        chart.setPrefHeight(260);
        chart.setLegendVisible(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Set<Integer> ids = sprintIds();
        allTasks.stream().filter(t -> ids.contains(t.getSprintId()))
            .collect(Collectors.groupingBy(
                t -> t.getAssignedToName() != null ? t.getAssignedToName() : "Unassigned",
                Collectors.counting()))
            .forEach((name, count) -> series.getData().add(new XYChart.Data<>(name, count)));
        chart.getData().add(series);
        return chart;
    }

    private VBox buildSprintProgressTable() {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 8, 0, 0, 2);");
        Label title = new Label("Sprint Progress");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        box.getChildren().add(title);

        for (Sprint sprint : sprints) {
            List<Task> sprintTasks = allTasks.stream()
                .filter(t -> Objects.equals(t.getSprintId(), sprint.getId())).toList();
            int total = sprintTasks.size();
            long done = sprintTasks.stream().filter(t -> "done".equals(t.getStatus())).count();
            double pct = total == 0 ? 0 : (done * 100.0 / total);

            VBox sprintBox = new VBox(4);
            sprintBox.setPadding(new Insets(8, 0, 8, 0));
            sprintBox.setStyle("-fx-border-color: transparent transparent #f3f4f6 transparent;");

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            Label nameLabel = new Label(sprint.getName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151; -fx-min-width: 160;");
            Label pctLabel = new Label(String.format("%.0f%%  (%d/%d tasks)", pct, done, total));
            pctLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");
            Label badge = new Label(sprint.getStatus());
            badge.setStyle("-fx-background-color: " + statusColor(sprint.getStatus()) +
                "; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 2 8; -fx-font-size: 11px;");
            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
            row.getChildren().addAll(nameLabel, badge, sp, pctLabel);

            ProgressBar bar = new ProgressBar(pct / 100.0);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.setPrefHeight(8);
            bar.setStyle("-fx-accent: " + progressColor(pct) + ";");
            sprintBox.getChildren().addAll(row, bar);

            if (!sprintTasks.isEmpty()) {
                GridPane grid = new GridPane();
                grid.setHgap(12); grid.setVgap(4);
                grid.setPadding(new Insets(6, 0, 0, 12));
                int r = 0;
                for (Task t : sprintTasks) {
                    Label tTitle = new Label("• " + t.getTitle());
                    tTitle.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px; -fx-min-width: 200;");
                    Label tStatus = new Label(t.getStatus());
                    tStatus.setStyle("-fx-text-fill: " + taskStatusColor(t.getStatus()) + "; -fx-font-size: 11px; -fx-min-width: 80;");
                    Label tAssigned = new Label(t.getAssignedToName() != null ? t.getAssignedToName() : "—");
                    tAssigned.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
                    grid.add(tTitle, 0, r); grid.add(tStatus, 1, r); grid.add(tAssigned, 2, r); r++;
                }
                sprintBox.getChildren().add(grid);
            }
            box.getChildren().add(sprintBox);
        }
        return box;
    }

    private String statusColor(String s) {
        return switch (s != null ? s : "") { case "active" -> "#10b981"; case "closed" -> "#6b7280"; default -> "#f59e0b"; };
    }
    private String progressColor(double pct) {
        if (pct >= 75) return "#10b981"; if (pct >= 40) return "#3b82f6"; return "#f59e0b";
    }
    private String taskStatusColor(String s) {
        return switch (s != null ? s : "") { case "done" -> "#10b981"; case "in_progress" -> "#3b82f6"; default -> "#f59e0b"; };
    }

    private void handleExport() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save PDF Report");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName("sprint_report.pdf");
        File file = fc.showSaveDialog(stage);
        if (file != null) {
            try {
                PDFExporter.export(sprints, allTasks, file);
                new Alert(Alert.AlertType.INFORMATION, "PDF exported successfully.").showAndWait();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Export error: " + e.getMessage()).showAndWait();
            }
        }
    }
}
