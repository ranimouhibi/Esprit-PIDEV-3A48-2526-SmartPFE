package org.example.controller;

import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.view.CalendarView;
import org.example.dao.ProjectDAO;
import org.example.dao.SprintDAO;
import org.example.dao.TaskDAO;
import org.example.model.Sprint;
import org.example.model.Task;
import org.example.model.User;
import org.example.util.EmailService;
import org.example.util.PDFExporter;
import org.example.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.example.util.ModernAlert;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class SprintController implements Initializable {

    @FXML private FlowPane cardPane;
    @FXML private TextField searchField;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private Label messageLabel;

    private final SprintDAO sprintDAO = new SprintDAO();
    private List<Sprint> allSprints = List.of();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadSprints();
        if (searchField != null)
            searchField.textProperty().addListener((o, old, v) -> applyFilters());
    }

    private void loadSprints() {
        try {
            org.example.model.User current = SessionManager.getCurrentUser();
            if (current != null && !"admin".equalsIgnoreCase(current.getRole())) {
                allSprints = sprintDAO.findForUser(current.getId(), current.getRole());
            } else {
                allSprints = sprintDAO.findAll();
            }
            renderCards(allSprints);
        } catch (Exception e) { showMessage("Error: " + e.getMessage(), true); }
    }

    // ── Card rendering ────────────────────────────────────────────────────────

    private void renderCards(List<Sprint> sprints) {
        if (cardPane == null) return;
        cardPane.getChildren().clear();
        if (sprints.isEmpty()) {
            Label empty = new Label("No sprints found.");
            empty.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14px;");
            cardPane.getChildren().add(empty);
            return;
        }
        for (Sprint s : sprints) cardPane.getChildren().add(buildSprintCard(s));
    }

    private VBox buildSprintCard(Sprint sprint) {
        VBox card = new VBox(8);
        card.setPrefWidth(280);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
            + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3); -fx-cursor: hand;");

        // Header: name + status badge
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(sprint.getName());
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        nameLabel.setTextFill(Color.web("#1a1a2e"));
        nameLabel.setWrapText(true);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);
        header.getChildren().addAll(nameLabel, statusBadge(sprint.getStatus()));

        Label project = new Label("📁 " + (sprint.getProjectTitle() != null ? sprint.getProjectTitle() : "—"));
        project.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String start = sprint.getStartDate() != null ? sprint.getStartDate().format(fmt) : "—";
        String end   = sprint.getEndDate()   != null ? sprint.getEndDate().format(fmt)   : "—";
        Label dates = new Label("📅 " + start + " → " + end);
        dates.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

        Label hint = new Label("Double-click for details");
        hint.setStyle("-fx-text-fill: #c4c4c4; -fx-font-size: 10px; -fx-font-style: italic;");

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button editBtn = actionBtn("Edit", "#667eea");
        Button delBtn  = actionBtn("Delete", "#ef4444");

        editBtn.setOnAction(e -> {
            if ("closed".equals(sprint.getStatus())) { showMessage("Closed sprints cannot be edited.", true); return; }
            Window owner = cardPane.getScene().getWindow();
            if (new SprintDialog(owner, sprint).showAndWait()) {
                try {
                    // Re-fetch from DB to get latest data (projectId, projectTitle, dates)
                    Sprint refreshed = sprintDAO.findById(sprint.getId());
                    if (refreshed != null) notifySprintAction("updated", refreshed);
                } catch (Exception ex) {
                    System.err.println("[SPRINT NOTIFY] Could not refresh sprint: " + ex.getMessage());
                }
                loadSprints();
            }
        });
        delBtn.setOnAction(e -> handleDeleteSprint(sprint));
        actions.getChildren().addAll(editBtn, delBtn);

        card.getChildren().addAll(header, project, dates, hint, new Separator(), actions);

        // Double-click → show detail popup
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) showSprintDetail(sprint);
        });

        return card;
    }

    private void showSprintDetail(Sprint sprint) {
        Stage detail = new Stage();
        detail.initOwner(cardPane.getScene().getWindow());
        detail.initModality(Modality.APPLICATION_MODAL);
        detail.setTitle("Sprint Details — " + sprint.getName());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        VBox root = new VBox(12);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #f4f6fb;");
        root.setPrefWidth(420);

        Label title = new Label(sprint.getName());
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#1a1a2e"));

        root.getChildren().addAll(
            title,
            statusBadge(sprint.getStatus()),
            detailRow("Project",  sprint.getProjectTitle() != null ? sprint.getProjectTitle() : "—"),
            detailRow("Start",    sprint.getStartDate() != null ? sprint.getStartDate().format(fmt) : "—"),
            detailRow("End",      sprint.getEndDate()   != null ? sprint.getEndDate().format(fmt)   : "—"),
            detailRow("Goal",     sprint.getGoal() != null && !sprint.getGoal().isBlank() ? sprint.getGoal() : "—")
        );

        // Tasks in this sprint
        try {
            List<Task> tasks = new TaskDAO().findBySprint(sprint.getId());
            if (!tasks.isEmpty()) {
                Label tasksTitle = new Label("Tasks (" + tasks.size() + ")");
                tasksTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
                tasksTitle.setStyle("-fx-text-fill: #374151; -fx-padding: 8 0 4 0;");
                root.getChildren().add(tasksTitle);
                for (Task t : tasks) {
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);
                    Label tName = new Label("• " + t.getTitle());
                    tName.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px;");
                    HBox.setHgrow(tName, Priority.ALWAYS);
                    row.getChildren().addAll(tName, statusBadge(t.getStatus()));
                    root.getChildren().add(row);
                }
            }
        } catch (Exception ignored) {}

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #a12c2f; -fx-text-fill: white; -fx-background-radius: 8; "
            + "-fx-padding: 8 24; -fx-font-weight: bold; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> detail.close());
        root.getChildren().add(closeBtn);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        detail.setScene(new Scene(scroll, 440, 520));
        detail.show();
    }

    private HBox detailRow(String label, String value) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 8 12;");
        Label lbl = new Label(label + ":");
        lbl.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px; -fx-min-width: 80;");
        Label val = new Label(value);
        val.setStyle("-fx-text-fill: #1a1a2e; -fx-font-size: 12px;");
        val.setWrapText(true);
        row.getChildren().addAll(lbl, val);
        return row;
    }

    // ── Calendar popup (CalendarFX) ───────────────────────────────────────────

    @FXML
    public void handleCalendar() {
        Stage calStage = new Stage();
        calStage.initOwner(cardPane.getScene().getWindow());
        calStage.initModality(Modality.APPLICATION_MODAL);
        calStage.setTitle("Sprint Calendar");

        // Build one CalendarFX Calendar per sprint status
        Calendar activeCal  = new Calendar("Active Sprints");
        Calendar plannedCal = new Calendar("Planned Sprints");
        Calendar closedCal  = new Calendar("Closed Sprints");

        activeCal.setStyle(Calendar.Style.STYLE2);   // green
        plannedCal.setStyle(Calendar.Style.STYLE3);  // blue/purple
        closedCal.setStyle(Calendar.Style.STYLE5);   // grey

        // Map each sprint to a CalendarFX Entry
        for (Sprint s : allSprints) {
            if (s.getStartDate() == null || s.getEndDate() == null) continue;

            Entry<Sprint> entry = new Entry<>(s.getName());
            entry.setInterval(
                s.getStartDate().atTime(LocalTime.MIN),
                s.getEndDate().atTime(LocalTime.MAX)
            );
            entry.setFullDay(true);
            entry.setUserObject(s);

            // Click on entry → show sprint tasks popup
            entry.setRecurrenceRule(null);

            switch (s.getStatus() != null ? s.getStatus() : "") {
                case "active" -> activeCal.addEntry(entry);
                case "closed" -> closedCal.addEntry(entry);
                default       -> plannedCal.addEntry(entry);
            }
        }

        CalendarSource source = new CalendarSource("Sprints");
        source.getCalendars().addAll(activeCal, plannedCal, closedCal);

        CalendarView calendarView = new CalendarView();
        calendarView.getCalendarSources().setAll(source);
        calendarView.setRequestedTime(LocalTime.now());
        calendarView.showMonthPage(); // default to month view

        // On entry click → show sprint tasks popup
        calendarView.setEntryDetailsCallback(param -> {
            Entry<?> entry = param.getEntry();
            if (entry.getUserObject() instanceof Sprint sprint) {
                javafx.application.Platform.runLater(() ->
                    showSprintTasksPopup(sprint, calStage));
            }
            return null;
        });

        Scene scene = new Scene(calendarView, 900, 650);
        calStage.setScene(scene);
        calStage.show();
    }

    private void showSprintTasksPopup(Sprint sprint, Window owner) {
        Stage popup = new Stage();
        popup.initOwner(owner);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Sprint: " + sprint.getName());

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f4f6fb;");
        root.setPrefWidth(460);

        Label title = new Label(sprint.getName());
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#1a1a2e"));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Label dates = new Label("📅 "
            + (sprint.getStartDate() != null ? sprint.getStartDate().format(fmt) : "—")
            + " → "
            + (sprint.getEndDate() != null ? sprint.getEndDate().format(fmt) : "—"));
        dates.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);
        header.getChildren().add(title);
        if (!"closed".equals(sprint.getStatus())) {
            Button newTaskBtn = new Button("+ New Task");
            newTaskBtn.setStyle("-fx-background-color: #a12c2f; -fx-text-fill: white; "
                + "-fx-background-radius: 8; -fx-padding: 7 16; -fx-font-weight: bold; -fx-cursor: hand;");
            newTaskBtn.setOnAction(e -> {
                if (new TaskDialog(popup, null, sprint).showAndWait()) {
                    popup.close();
                    showSprintTasksPopup(sprint, owner);
                }
            });
            header.getChildren().add(newTaskBtn);
        }

        root.getChildren().addAll(header, dates, statusBadge(sprint.getStatus()), new Separator());

        try {
            List<Task> tasks = new TaskDAO().findBySprint(sprint.getId());
            User current = SessionManager.getCurrentUser();
            boolean isStudent = current != null && "student".equals(current.getRole());
            
            if (tasks.isEmpty()) {
                Label empty = new Label("No tasks in this sprint yet.");
                empty.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 13px;");
                root.getChildren().add(empty);
            } else {
                for (Task t : tasks) {
                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(8, 12, 8, 12));
                    row.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
                    Label tName = new Label(t.getTitle());
                    tName.setStyle("-fx-font-size: 12px; -fx-text-fill: #1a1a2e;");
                    HBox.setHgrow(tName, Priority.ALWAYS);
                    
                    // Only show assigned field for supervisors
                    if (isStudent) {
                        row.getChildren().addAll(tName, statusBadge(t.getStatus()), priorityBadge(t.getPriority()));
                    } else {
                        Label assigned = new Label("👤 " + (t.getAssignedToName() != null ? t.getAssignedToName() : "—"));
                        assigned.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");
                        row.getChildren().addAll(tName, assigned, statusBadge(t.getStatus()), priorityBadge(t.getPriority()));
                    }
                    root.getChildren().add(row);
                }
            }
        } catch (Exception ignored) {}

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; "
            + "-fx-background-radius: 8; -fx-padding: 7 20; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> popup.close());
        root.getChildren().add(closeBtn);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        popup.setScene(new Scene(scroll, 480, 500));
        popup.show();
    }

    private Label priorityBadge(String priority) {
        String color = switch (priority != null ? priority : "") {
            case "critical" -> "#dc2626";
            case "high"     -> "#f97316";
            case "medium"   -> "#eab308";
            default         -> "#6b7280";
        };
        Label b = new Label(priority != null ? priority.toUpperCase() : "—");
        b.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color + "; "
            + "-fx-background-radius: 4; -fx-padding: 1 6; -fx-font-size: 10px; -fx-font-weight: bold;");
        return b;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Label statusBadge(String status) {
        String color = switch (status != null ? status : "") {
            case "active"      -> "#10b981";
            case "closed"      -> "#6b7280";
            case "done"        -> "#10b981";
            case "in_progress" -> "#3b82f6";
            default            -> "#f59e0b";
        };
        Label badge = new Label(status != null ? status.replace("_", " ").toUpperCase() : "—");
        badge.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; "
            + "-fx-background-radius: 6; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: bold;");
        return badge;
    }

    private Button actionBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; "
            + "-fx-background-radius: 6; -fx-padding: 5 12; -fx-font-size: 11px; -fx-cursor: hand;");
        return btn;
    }

    private void handleDeleteSprint(Sprint sprint) {
        User current = SessionManager.getCurrentUser();
        if (current != null && "supervisor".equals(current.getRole()) && !"planned".equals(sprint.getStatus())) {
            showMessage("Only 'planned' sprints can be deleted.", true); return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete sprint '" + sprint.getName() + "'? All tasks will also be deleted.",
            ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    notifySprintAction("deleted", sprint);
                    sprintDAO.delete(sprint.getId());
                    showMessage("Sprint deleted.", false);
                    loadSprints();
                } catch (Exception e) { showMessage("Error: " + e.getMessage(), true); }
            }
        });
    @FXML
    public void handleDelete() {
        if (selectedSprint == null) { showMessage("Sélectionnez un sprint.", true); return; }
        if (ModernAlert.confirmDelete(selectedSprint.getName())) {
            try {
                sprintDAO.delete(selectedSprint.getId());
                showMessage("Sprint supprimé.", false);
                handleClear();
                loadSprints();
            } catch (Exception e) { showMessage("Erreur: " + e.getMessage(), true); }
        }
    }

    // ── Filters ───────────────────────────────────────────────────────────────

    private void applyFilters() {
        String query = searchField != null ? searchField.getText() : "";
        LocalDate from = fromDate != null ? fromDate.getValue() : null;
        LocalDate to   = toDate   != null ? toDate.getValue()   : null;

        List<Sprint> result = new java.util.ArrayList<>(allSprints);
        if (query != null && !query.isBlank()) {
            String q = query.toLowerCase();
            result = result.stream().filter(s ->
                (s.getName() != null && s.getName().toLowerCase().contains(q)) ||
                (s.getStatus() != null && s.getStatus().toLowerCase().contains(q)) ||
                (s.getProjectTitle() != null && s.getProjectTitle().toLowerCase().contains(q))
            ).toList();
        }
        if (from != null || to != null) {
            result = result.stream().filter(s -> {
                if (from != null && s.getEndDate()   != null && s.getEndDate().isBefore(from))   return false;
                if (to   != null && s.getStartDate() != null && s.getStartDate().isAfter(to))    return false;
                return true;
            }).toList();
        }
        renderCards(result);
    }

    @FXML public void handleSort()        { applyFilters(); }
    @FXML public void handleClearFilter() {
        if (fromDate != null) fromDate.setValue(null);
        if (toDate   != null) toDate.setValue(null);
        if (searchField != null) searchField.clear();
        applyFilters();
    }
    @FXML public void handleSearch() { applyFilters(); }

    @FXML
    public void handleAddDialog() {
        Window owner = cardPane.getScene().getWindow();
        Sprint newSprint = new Sprint();
        SprintDialog dialog = new SprintDialog(owner, null);
        if (dialog.showAndWait()) {
            try {
                // Fetch the most recently created sprint to get full data including projectId
                List<Sprint> updated = sprintDAO.findAll();
                if (!updated.isEmpty()) {
                    notifySprintAction("created", updated.get(0));
                }
            } catch (Exception ignored) {}
            loadSprints();
        }
    }

    @FXML
    public void handleStats() {
        Window owner = cardPane.getScene().getWindow();
        new SprintStatsController(owner).show();
    }

    @FXML
    public void handleExportPdf() {
        try {
            User user = SessionManager.getCurrentUser();
            if (user == null) { showMessage("No user session.", true); return; }

            var projectDAO = new ProjectDAO();
            var taskDAO    = new TaskDAO();

            // Load projects scoped to the current user's role
            List<org.example.model.Project> projects = projectDAO.findForUser(user.getId(), user.getRole());

            FileChooser fc = new FileChooser();
            fc.setTitle("Save PDF Report");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            fc.setInitialFileName("sprint_report_" + user.getName().replaceAll("\\s+", "_") + ".pdf");
            File file = fc.showSaveDialog(cardPane.getScene().getWindow());
            if (file == null) return;

            String reportTitle = "supervisor".equalsIgnoreCase(user.getRole())
                ? "SmartPFE — Supervisor Sprint Report"
                : "SmartPFE — Student Sprint Report";

            PDFExporter.exportFullReport(
                reportTitle,
                user.getName(),
                projects,
                projectId -> { try { return sprintDAO.findByProject(projectId); } catch (Exception e) { return List.of(); } },
                sprintId  -> { try { return taskDAO.findBySprint(sprintId);     } catch (Exception e) { return List.of(); } },
                file
            );
            showMessage("PDF exported successfully.", false);
        } catch (Exception e) {
            showMessage("Export error: " + e.getMessage(), true);
        }
    }

    // ── Task Email (from sprint popup) ────────────────────────────────────────

    private void notifyTaskAction(String action, Task task) {
        try {
            User current = SessionManager.getCurrentUser();
            String role = current != null ? current.getRole() : "";
            System.out.println("[TASK NOTIFY] action=" + action + " role=" + role
                + " taskId=" + task.getId() + " assignedToId=" + task.getAssignedToId());

            if ("student".equals(role)) {
                // Student → notify supervisor
                if (task.getProjectId() == 0) { System.err.println("[TASK NOTIFY] Skipped: projectId=0"); return; }
                org.example.model.Project project = new ProjectDAO().findById(task.getProjectId());
                if (project == null || project.getSupervisorId() <= 0) { System.err.println("[TASK NOTIFY] Skipped: no supervisor"); return; }
                org.example.dao.UserDAO uDao = new org.example.dao.UserDAO();
                org.example.model.User supervisor = uDao.findById(project.getSupervisorId());
                if (supervisor == null || supervisor.getEmail() == null) { System.err.println("[TASK NOTIFY] Skipped: supervisor has no email"); return; }
                String creatorName = current.getName() != null ? current.getName() : current.getEmail();
                String html = EmailService.taskSupervisorTemplate(action, task.getTitle(),
                    task.getSprintName() != null ? task.getSprintName() : "—",
                    task.getPriority() != null ? task.getPriority() : "—",
                    creatorName,
                    supervisor.getName() != null ? supervisor.getName() : supervisor.getEmail());
                EmailService.sendAsync(supervisor.getEmail(), "Task " + action + " by student: " + task.getTitle(), html);
                System.out.println("[TASK NOTIFY] Notified supervisor: " + supervisor.getEmail());
            } else {
                // Supervisor → notify assigned student only
                if (task.getAssignedToId() == null) { System.err.println("[TASK NOTIFY] Skipped: no assignee"); return; }
                org.example.dao.UserDAO uDao = new org.example.dao.UserDAO();
                org.example.model.User u = uDao.findById(task.getAssignedToId());
                if (u == null || u.getEmail() == null) { System.err.println("[TASK NOTIFY] Skipped: assignee has no email"); return; }
                String html = EmailService.taskTemplate(action, task.getTitle(),
                    task.getSprintName() != null ? task.getSprintName() : "—",
                    task.getPriority() != null ? task.getPriority() : "—",
                    u.getName() != null ? u.getName() : u.getEmail());
                EmailService.sendAsync(u.getEmail(), "Task " + action + ": " + task.getTitle(), html);
                System.out.println("[TASK NOTIFY] Notified student: " + u.getEmail());
            }
        } catch (Exception e) {
            System.err.println("[TASK NOTIFY] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Email ─────────────────────────────────────────────────────────────────

    private void notifySprintAction(String action, Sprint sprint) {
        try {
            User current = SessionManager.getCurrentUser();
            String role = current != null ? current.getRole() : "";
            ProjectDAO projectDAO = new ProjectDAO();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String start = sprint.getStartDate() != null ? sprint.getStartDate().format(fmt) : "—";
            String end   = sprint.getEndDate()   != null ? sprint.getEndDate().format(fmt)   : "—";
            String projectName = sprint.getProjectTitle() != null ? sprint.getProjectTitle() : "";

            if ("student".equals(role)) {
                // Student created/updated sprint → notify the supervisor
                org.example.model.Project project = projectDAO.findById(sprint.getProjectId());
                if (project != null && project.getSupervisorId() > 0) {
                    User supervisor = new org.example.dao.UserDAO().findById(project.getSupervisorId());
                    if (supervisor != null && supervisor.getEmail() != null) {
                        String creatorName = current.getName() != null ? current.getName() : current.getEmail();
                        String html = EmailService.sprintSupervisorTemplate(action, sprint.getName(),
                            projectName, start, end, creatorName,
                            supervisor.getName() != null ? supervisor.getName() : supervisor.getEmail());
                        EmailService.sendAsync(supervisor.getEmail(),
                            "Sprint " + action + " by student: " + sprint.getName(), html);
                        System.out.println("[SPRINT NOTIFY] Notified supervisor: " + supervisor.getEmail());
                    }
                }
            } else {
                // Supervisor (or admin) created/updated sprint → notify all students in the project
                List<User> members = projectDAO.findProjectMembers(sprint.getProjectId());
                String creatorName = current != null && current.getName() != null ? current.getName() : "Your supervisor";
                for (User u : members) {
                    if (u.getEmail() != null && "student".equals(u.getRole())) {
                        String html = EmailService.sprintTemplate(action, sprint.getName(),
                            projectName, start, end,
                            u.getName() != null ? u.getName() : u.getEmail());
                        EmailService.sendAsync(u.getEmail(), "Sprint " + action + ": " + sprint.getName(), html);
                        System.out.println("[SPRINT NOTIFY] Notified student: " + u.getEmail());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[SPRINT NOTIFY] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showMessage(String msg, boolean isError) {
        if (messageLabel != null) {
            messageLabel.setText(msg);
            messageLabel.setStyle(isError ? "-fx-text-fill: #dc2626;" : "-fx-text-fill: #16a34a;");
        }
    }
}
