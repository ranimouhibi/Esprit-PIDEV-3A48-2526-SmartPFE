package org.example.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import org.example.util.ModernAlert;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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

    private static final String C_BG      = "#f4f6fb";
    private static final String C_CARD    = "white";
    private static final String C_PRIMARY = "#a12c2f";
    private static final String C_PLANNED = "#f59e0b";
    private static final String C_ACTIVE  = "#3b82f6";
    private static final String C_CLOSED  = "#6b7280";
    private static final String C_DONE    = "#10b981";
    private static final String C_ORANGE  = "#f97316";
    private static final String C_DARK    = "#1a1a2e";
    private static final String C_MUTED   = "#6b7280";

    public SprintStatsController(Window owner) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Sprint Statistics");
        stage.setWidth(860);
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
            ModernAlert.show(ModernAlert.Type.ERROR, "Error", "Load error: " + e.getMessage());
            return;
        }
        stage.setScene(new Scene(buildLayout()));
        stage.show();
    }

    // ── Root ─────────────────────────────────────────────────────────────────

    private BorderPane buildLayout() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + C_BG + ";");
        root.setTop(buildHeader());

        VBox body = new VBox(20);
        body.setPadding(new Insets(20, 28, 28, 28));
        body.getChildren().addAll(
            buildKpiRow(),
            buildTaskBreakdownSection(),
            buildSprintProgressSection()
        );

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        root.setCenter(scroll);
        return root;
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 28, 0, 28));

        VBox titles = new VBox(2);
        Label title = new Label("Sprint Statistics");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.web(C_DARK));
        Label sub = new Label(sprints.size() + " sprints  ·  " + scopedTasks().size() + " tasks");
        sub.setStyle("-fx-text-fill: " + C_MUTED + "; -fx-font-size: 13px;");
        titles.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button exportBtn = new Button("⬇  Export PDF");
        exportBtn.setStyle("-fx-background-color: " + C_PRIMARY + "; -fx-text-fill: white; "
            + "-fx-background-radius: 8; -fx-padding: 9 20; -fx-font-weight: bold; -fx-cursor: hand;");
        exportBtn.setOnAction(e -> handleExport());

        header.getChildren().addAll(titles, spacer, exportBtn);
        return header;
    }

    // ── KPI cards ─────────────────────────────────────────────────────────────

    private HBox buildKpiRow() {
        List<Task> tasks = scopedTasks();
        long done   = tasks.stream().filter(t -> "done".equals(t.getStatus())).count();
        long inProg = tasks.stream().filter(t -> "in_progress".equals(t.getStatus())).count();
        long todo   = tasks.stream().filter(t -> "todo".equals(t.getStatus())).count();
        long active = sprints.stream().filter(s -> "active".equals(s.getStatus())).count();

        HBox row = new HBox(14);
        row.getChildren().addAll(
            kpiCard("📋", String.valueOf(sprints.size()), "Total Sprints",  C_PRIMARY),
            kpiCard("🔵", String.valueOf(active),         "Active Sprints", C_ACTIVE),
            kpiCard("✅", String.valueOf(done),            "Tasks Done",     C_DONE),
            kpiCard("⏳", String.valueOf(inProg),          "In Progress",    C_ORANGE),
            kpiCard("📝", String.valueOf(todo),            "To Do",          C_PLANNED)
        );
        for (var node : row.getChildren()) HBox.setHgrow(node, Priority.ALWAYS);
        return row;
    }

    private VBox kpiCard(String icon, String value, String label, String accent) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 14; "
            + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 10, 0, 0, 2); "
            + "-fx-border-color: " + accent + "; -fx-border-width: 0 0 0 4; -fx-border-radius: 0 0 0 14;");
        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 18px;");
        Label valLbl = new Label(value);
        valLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        valLbl.setTextFill(Color.web(accent));
        top.getChildren().addAll(iconLbl, valLbl);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: " + C_MUTED + "; -fx-font-size: 12px;");
        card.getChildren().addAll(top, lbl);
        return card;
    }

    // ── Task breakdown (replaces charts) ─────────────────────────────────────

    private VBox buildTaskBreakdownSection() {
        List<Task> tasks = scopedTasks();
        int total = tasks.size();

        VBox card = sectionCard("Task Breakdown");
        if (total == 0) {
            card.getChildren().add(mutedLabel("No tasks yet."));
            return card;
        }

        long done   = tasks.stream().filter(t -> "done".equals(t.getStatus())).count();
        long inProg = tasks.stream().filter(t -> "in_progress".equals(t.getStatus())).count();
        long todo   = tasks.stream().filter(t -> "todo".equals(t.getStatus())).count();

        card.getChildren().addAll(
            statBar("Done",        done,   total, C_DONE),
            statBar("In Progress", inProg, total, C_ACTIVE),
            statBar("To Do",       todo,   total, C_PLANNED)
        );

        // Sprint status breakdown
        Label sprintTitle = new Label("Sprint Status");
        sprintTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        sprintTitle.setTextFill(Color.web(C_DARK));
        sprintTitle.setPadding(new Insets(12, 0, 4, 0));
        card.getChildren().add(sprintTitle);

        int totalSprints = sprints.size();
        long planned = sprints.stream().filter(s -> "planned".equals(s.getStatus())).count();
        long active  = sprints.stream().filter(s -> "active".equals(s.getStatus())).count();
        long closed  = sprints.stream().filter(s -> "closed".equals(s.getStatus())).count();

        card.getChildren().addAll(
            statBar("Planned", planned, totalSprints, C_PLANNED),
            statBar("Active",  active,  totalSprints, C_ACTIVE),
            statBar("Closed",  closed,  totalSprints, C_CLOSED)
        );

        return card;
    }

    /** A labeled progress bar row: "Label   ████░░░░  12 (60%)" */
    private HBox statBar(String label, long count, int total, String color) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 0, 4, 0));

        Label nameLbl = new Label(label);
        nameLbl.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px;");
        nameLbl.setMinWidth(90);

        double pct = total == 0 ? 0 : (count * 100.0 / total);
        ProgressBar bar = new ProgressBar(pct / 100.0);
        bar.setPrefHeight(10);
        bar.setPrefWidth(300);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setStyle("-fx-accent: " + color + ";");
        HBox.setHgrow(bar, Priority.ALWAYS);

        Label countLbl = new Label(count + "  (" + String.format("%.0f", pct) + "%)");
        countLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: bold; -fx-min-width: 70;");

        row.getChildren().addAll(nameLbl, bar, countLbl);
        return row;
    }

    // ── Sprint progress cards ─────────────────────────────────────────────────

    private VBox buildSprintProgressSection() {
        VBox section = new VBox(12);
        Label title = new Label("Sprint Progress");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        title.setTextFill(Color.web(C_DARK));
        section.getChildren().add(title);

        if (sprints.isEmpty()) {
            section.getChildren().add(mutedLabel("No sprints to display."));
            return section;
        }
        for (Sprint sprint : sprints) section.getChildren().add(buildSprintCard(sprint));
        return section;
    }

    private VBox buildSprintCard(Sprint sprint) {
        List<Task> sprintTasks = allTasks.stream()
            .filter(t -> Objects.equals(t.getSprintId(), sprint.getId())).toList();
        int total   = sprintTasks.size();
        long done   = sprintTasks.stream().filter(t -> "done".equals(t.getStatus())).count();
        long inProg = sprintTasks.stream().filter(t -> "in_progress".equals(t.getStatus())).count();
        long todo   = sprintTasks.stream().filter(t -> "todo".equals(t.getStatus())).count();
        double pct  = total == 0 ? 0 : (done * 100.0 / total);

        VBox card = new VBox(10);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 14; "
            + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");

        // Header
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLbl = new Label(sprint.getName());
        nameLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        nameLbl.setTextFill(Color.web(C_DARK));
        HBox.setHgrow(nameLbl, Priority.ALWAYS);
        headerRow.getChildren().add(nameLbl);
        if (sprint.getProjectTitle() != null) {
            Label projLbl = new Label("📁 " + sprint.getProjectTitle());
            projLbl.setStyle("-fx-text-fill: " + C_MUTED + "; -fx-font-size: 11px;");
            headerRow.getChildren().add(projLbl);
        }
        Label badge = new Label(sprint.getStatus().toUpperCase());
        badge.setStyle("-fx-background-color: " + statusColor(sprint.getStatus()) + "; "
            + "-fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 3 10; "
            + "-fx-font-size: 10px; -fx-font-weight: bold;");
        headerRow.getChildren().add(badge);

        // Progress bar
        HBox progressRow = new HBox(10);
        progressRow.setAlignment(Pos.CENTER_LEFT);
        ProgressBar bar = new ProgressBar(pct / 100.0);
        bar.setPrefHeight(10);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setStyle("-fx-accent: " + progressColor(pct) + ";");
        HBox.setHgrow(bar, Priority.ALWAYS);
        Label pctLbl = new Label(String.format("%.0f%%", pct));
        pctLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        pctLbl.setTextFill(Color.web(progressColor(pct)));
        pctLbl.setMinWidth(40);
        progressRow.getChildren().addAll(bar, pctLbl);

        // Mini counters
        HBox counters = new HBox(10);
        counters.setAlignment(Pos.CENTER_LEFT);
        counters.getChildren().addAll(
            miniCounter("✅ Done",        done,   C_DONE),
            miniCounter("⏳ In Progress", inProg, C_ACTIVE),
            miniCounter("📝 To Do",       todo,   C_PLANNED),
            miniCounter("📦 Total",       total,  C_MUTED)
        );

        card.getChildren().addAll(headerRow, progressRow, counters);

        // Collapsible task list
        if (!sprintTasks.isEmpty()) {
            TitledPane taskPane = new TitledPane("Tasks (" + total + ")", buildTaskList(sprintTasks));
            taskPane.setExpanded(false);
            taskPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; "
                + "-fx-font-size: 12px; -fx-text-fill: " + C_MUTED + ";");
            card.getChildren().add(taskPane);
        }
        return card;
    }

    private VBox buildTaskList(List<Task> tasks) {
        VBox list = new VBox(4);
        list.setPadding(new Insets(4, 0, 0, 0));
        for (Task t : tasks) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 8, 6, 8));
            row.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 8;");

            Label dot = new Label("●");
            dot.setStyle("-fx-text-fill: " + taskStatusColor(t.getStatus()) + "; -fx-font-size: 10px;");

            Label titleLbl = new Label(t.getTitle());
            titleLbl.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px;");
            HBox.setHgrow(titleLbl, Priority.ALWAYS);

            String priorityColor = switch (t.getPriority() != null ? t.getPriority() : "") {
                case "critical" -> "#dc2626"; case "high" -> "#f97316";
                case "medium"   -> "#eab308"; default     -> C_MUTED;
            };
            Label priorityLbl = new Label(t.getPriority() != null ? t.getPriority().toUpperCase() : "—");
            priorityLbl.setStyle("-fx-text-fill: " + priorityColor + "; -fx-font-size: 10px; "
                + "-fx-font-weight: bold; -fx-min-width: 55;");

            Label statusLbl = new Label(t.getStatus() != null ? t.getStatus().replace("_", " ") : "—");
            statusLbl.setStyle("-fx-background-color: " + taskStatusColor(t.getStatus()) + "22; "
                + "-fx-text-fill: " + taskStatusColor(t.getStatus()) + "; "
                + "-fx-background-radius: 4; -fx-padding: 2 7; -fx-font-size: 10px; -fx-font-weight: bold;");

            row.getChildren().addAll(dot, titleLbl, priorityLbl, statusLbl);
            list.getChildren().add(row);
        }
        return list;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private VBox sectionCard(String title) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 14; "
            + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 10, 0, 0, 2);");
        Label lbl = new Label(title);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lbl.setTextFill(Color.web(C_DARK));
        card.getChildren().add(lbl);
        return card;
    }

    private HBox miniCounter(String label, long count, String color) {
        HBox box = new HBox(5);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(4, 10, 4, 10));
        box.setStyle("-fx-background-color: " + color + "18; -fx-background-radius: 8;");
        Label val = new Label(String.valueOf(count));
        val.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        val.setTextFill(Color.web(color));
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px;");
        box.getChildren().addAll(val, lbl);
        return box;
    }

    private Label mutedLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + C_MUTED + "; -fx-font-size: 13px;");
        return l;
    }

    private List<Task> scopedTasks() {
        Set<Integer> ids = sprints.stream().map(Sprint::getId).collect(Collectors.toSet());
        return allTasks.stream().filter(t -> ids.contains(t.getSprintId())).toList();
    }

    private String statusColor(String s) {
        return switch (s != null ? s : "") {
            case "active" -> C_ACTIVE; case "closed" -> C_CLOSED; default -> C_PLANNED;
        };
    }
    private String progressColor(double pct) {
        if (pct >= 75) return C_DONE; if (pct >= 40) return C_ACTIVE; return C_PLANNED;
    }
    private String taskStatusColor(String s) {
        return switch (s != null ? s : "") {
            case "done" -> C_DONE; case "in_progress" -> C_ACTIVE; default -> C_PLANNED;
        };
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
                ModernAlert.show(ModernAlert.Type.INFO, "Info", "PDF exported successfully.");
            } catch (Exception e) {
                ModernAlert.show(ModernAlert.Type.ERROR, "Error", "Export error: " + e.getMessage());
            }
        }
    }
}
