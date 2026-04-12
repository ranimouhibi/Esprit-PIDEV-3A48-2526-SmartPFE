package org.example.controller;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.example.dao.ProjectDAO;
import org.example.dao.SprintDAO;
import org.example.dao.TaskDAO;
import org.example.dao.UserDAO;
import org.example.model.Project;
import org.example.model.Sprint;
import org.example.model.Task;
import org.example.model.User;
import org.example.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class TaskDialog {

    private static final int TITLE_MIN = 3;
    private static final int TITLE_MAX = 100;
    private static final int DESC_MAX  = 500;

    private static final User ALL_TEAM = new User(0, "", "all", "Entire Team");

    private final Stage stage;
    private final Task  task;
    private boolean confirmed = false;

    private final TextField        titleField       = new TextField();
    private final TextArea         descriptionField = new TextArea();
    private final ComboBox<String> priorityCombo    = new ComboBox<>();
    private final ComboBox<String> statusCombo      = new ComboBox<>();
    private final ComboBox<Sprint> sprintCombo      = new ComboBox<>();
    private final ComboBox<User>   assigneeCombo    = new ComboBox<>();

    private final Label errTitle    = errLabel();
    private final Label errDesc     = errLabel();
    private final Label errPriority = errLabel();
    private final Label errStatus   = errLabel();
    private final Label errSprint   = errLabel();
    private final Label errGeneral  = errLabel();

    private List<Project> userProjects = new ArrayList<>();

    public TaskDialog(Window owner, Task existing) {
        this.task = existing != null ? existing : new Task();
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(existing == null ? "New Task" : "Edit Task");
        stage.setResizable(false);
        stage.setScene(new Scene(buildLayout(), 460, 580));
    }

    private static Label errLabel() {
        Label l = new Label();
        l.setTextFill(Color.web("#dc2626"));
        l.setStyle("-fx-font-size: 11px;");
        l.setWrapText(true);
        l.setVisible(false);
        l.setManaged(false);
        return l;
    }

    private void showErr(Label l, String msg) { l.setText(msg); l.setVisible(true);  l.setManaged(true);  }
    private void clearErr(Label l)            { l.setText("");  l.setVisible(false); l.setManaged(false); }
    private void markInvalid(Control c) { c.setStyle(c.getStyle() + "; -fx-border-color: #dc2626; -fx-border-width: 2; -fx-border-radius: 8;"); }
    private void markValid(Control c)   { c.setStyle(c.getStyle().replace("; -fx-border-color: #dc2626; -fx-border-width: 2; -fx-border-radius: 8;", "")); }

    private VBox buildLayout() {
        String inputStyle = "-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 7 10;";

        titleField.setPromptText("Between " + TITLE_MIN + " and " + TITLE_MAX + " characters");
        titleField.setStyle(inputStyle);
        descriptionField.setPromptText("Optional (max " + DESC_MAX + " characters)");
        descriptionField.setPrefRowCount(3); descriptionField.setWrapText(true); descriptionField.setStyle(inputStyle);

        priorityCombo.setItems(FXCollections.observableArrayList("low", "medium", "high", "critical"));
        priorityCombo.setMaxWidth(Double.MAX_VALUE); priorityCombo.setStyle(inputStyle);
        sprintCombo.setMaxWidth(Double.MAX_VALUE);   sprintCombo.setStyle(inputStyle);
        assigneeCombo.setMaxWidth(Double.MAX_VALUE); assigneeCombo.setStyle(inputStyle);

        if (task.getId() == 0) {
            statusCombo.setItems(FXCollections.observableArrayList("todo"));
        } else {
            switch (task.getStatus() != null ? task.getStatus() : "todo") {
                case "todo"        -> statusCombo.setItems(FXCollections.observableArrayList("todo", "in_progress"));
                case "in_progress" -> statusCombo.setItems(FXCollections.observableArrayList("in_progress", "done"));
                default            -> statusCombo.setItems(FXCollections.observableArrayList(task.getStatus()));
            }
        }
        statusCombo.setMaxWidth(Double.MAX_VALUE); statusCombo.setStyle(inputStyle);

        Button confirmBtn = new Button("Confirm");
        confirmBtn.setStyle("-fx-background-color: #a12c2f; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 9 24; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13px;");
        confirmBtn.setOnAction(e -> handleConfirm());

        try {
            var user = SessionManager.getCurrentUser();
            List<Sprint> sprints = new ArrayList<>();
            if (user != null) {
                userProjects = new ProjectDAO().findForUser(user.getId(), user.getRole());
                if (userProjects.isEmpty()) {
                    showErr(errGeneral, "You have no projects. Please create a project first.");
                    confirmBtn.setDisable(true);
                } else {
                    SprintDAO sprintDAO = new SprintDAO();
                    for (Project p : userProjects) sprints.addAll(sprintDAO.findByProject(p.getId()));
                    if (sprints.isEmpty()) {
                        showErr(errGeneral, "No sprints found. Please create a sprint first.");
                        confirmBtn.setDisable(true);
                    }
                }
            } else {
                sprints = new SprintDAO().findAll();
            }
            sprintCombo.setItems(FXCollections.observableArrayList(sprints));
        } catch (Exception e) {
            showErr(errGeneral, "Unable to load sprints.");
            confirmBtn.setDisable(true);
        }

        sprintCombo.setOnAction(e -> refreshAssignees());
        boolean canAssign = SessionManager.getCurrentUser() != null &&
            !("student".equals(SessionManager.getCurrentUser().getRole()));

        Label assignLabel = fieldLabel("Assign To");
        assignLabel.setVisible(canAssign);
        assignLabel.setManaged(canAssign);
        assigneeCombo.setVisible(canAssign);
        assigneeCombo.setManaged(canAssign);

        if (task.getId() != 0) {
            titleField.setText(task.getTitle());
            descriptionField.setText(task.getDescription() != null ? task.getDescription() : "");
            priorityCombo.setValue(task.getPriority());
            statusCombo.setValue(task.getStatus());
            if (task.getSprintId() != null) {
                sprintCombo.getItems().stream()
                    .filter(s -> s.getId() == task.getSprintId())
                    .findFirst().ifPresent(s -> {
                        sprintCombo.setValue(s);
                        refreshAssignees();
                        if (task.getAssignedToId() != null) {
                            assigneeCombo.getItems().stream()
                                .filter(u -> u.getId() == task.getAssignedToId())
                                .findFirst().ifPresent(assigneeCombo::setValue);
                        }
                    });
            }
        }

        titleField.textProperty().addListener((o, old, val) -> validateTitle(val));
        descriptionField.textProperty().addListener((o, old, val) -> validateDesc(val));
        priorityCombo.valueProperty().addListener((o, old, val) -> { if (val != null) { clearErr(errPriority); markValid(priorityCombo); } });
        statusCombo.valueProperty().addListener((o, old, val)   -> { if (val != null) { clearErr(errStatus);   markValid(statusCombo);   } });
        sprintCombo.valueProperty().addListener((o, old, val)   -> { if (val != null) { clearErr(errSprint);   markValid(sprintCombo);   } });

        VBox form = new VBox(4);
        form.setPadding(new Insets(20));
        form.getChildren().addAll(
            fieldLabel("Title *  (" + TITLE_MIN + "-" + TITLE_MAX + " chars)"), titleField,       errTitle,
            fieldLabel("Description  (max " + DESC_MAX + " chars)"),            descriptionField, errDesc,
            fieldLabel("Priority *"),                                            priorityCombo,    errPriority,
            fieldLabel("Status *"),                                              statusCombo,      errStatus,
            fieldLabel("Sprint *"),                                              sprintCombo,      errSprint,
            assignLabel,                                                         assigneeCombo,
            errGeneral
        );

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #b0b8c1; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 9 24; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13px;");
        cancelBtn.setOnAction(e -> stage.close());

        HBox buttons = new HBox(10, confirmBtn, cancelBtn);
        buttons.setPadding(new Insets(4, 20, 20, 20));

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        return new VBox(scroll, buttons);
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #374151; -fx-padding: 6 0 2 0;");
        return l;
    }

    private void refreshAssignees() {
        Sprint selected = sprintCombo.getValue();
        if (selected == null) return;
        try {
            List<User> members = new ArrayList<>();
            for (Project p : userProjects) {
                if (p.getId() == selected.getProjectId()) {
                    members = new ProjectDAO().findProjectMembers(p.getId());
                    break;
                }
            }
            if (members.isEmpty()) members.addAll(new UserDAO().findByRole("student"));
            List<User> options = new ArrayList<>();
            options.add(ALL_TEAM);
            options.addAll(members);
            assigneeCombo.setItems(FXCollections.observableArrayList(options));
        } catch (Exception e) {
            showErr(errGeneral, "Unable to load team members.");
        }
    }

    private boolean validateTitle(String val) {
        String v = val == null ? "" : val.trim();
        if (v.isEmpty())            { showErr(errTitle, "Title is required.");                                    markInvalid(titleField); return false; }
        if (v.length() < TITLE_MIN) { showErr(errTitle, "Title must be at least " + TITLE_MIN + " characters."); markInvalid(titleField); return false; }
        if (v.length() > TITLE_MAX) { showErr(errTitle, "Title cannot exceed " + TITLE_MAX + " characters.");    markInvalid(titleField); return false; }
        clearErr(errTitle); markValid(titleField); return true;
    }

    private boolean validateDesc(String val) {
        String v = val == null ? "" : val;
        if (v.length() > DESC_MAX) {
            showErr(errDesc, "Description cannot exceed " + DESC_MAX + " characters (" + v.length() + "/" + DESC_MAX + ").");
            markInvalid(descriptionField); return false;
        }
        clearErr(errDesc); markValid(descriptionField); return true;
    }

    private boolean validateAll() {
        boolean ok = true;
        if (!validateTitle(titleField.getText()))      ok = false;
        if (!validateDesc(descriptionField.getText())) ok = false;
        if (priorityCombo.getValue() == null) { showErr(errPriority, "Priority is required."); markInvalid(priorityCombo); ok = false; }
        if (statusCombo.getValue() == null)   { showErr(errStatus,   "Status is required.");   markInvalid(statusCombo);   ok = false; }
        if (sprintCombo.getValue() == null)   { showErr(errSprint,   "Sprint is required.");   markInvalid(sprintCombo);   ok = false; }
        return ok;
    }

    private void handleConfirm() {
        clearErr(errGeneral);
        if (!validateAll()) return;

        Sprint sprint = sprintCombo.getValue();
        User assignee = assigneeCombo.getValue();

        task.setTitle(titleField.getText().trim());
        task.setDescription(descriptionField.getText().trim());
        task.setPriority(priorityCombo.getValue());
        task.setStatus(statusCombo.getValue());
        task.setSprintId(sprint.getId());
        task.setProjectId(sprint.getProjectId());

        try {
            TaskDAO dao = new TaskDAO();
            boolean isStudent = SessionManager.getCurrentUser() != null &&
                "student".equals(SessionManager.getCurrentUser().getRole());
            // Students always assign to entire team; supervisors use the combo selection
            boolean assignAll = isStudent || (assignee == null || assignee == ALL_TEAM);

            if (task.getId() != 0) {
                if (!assignAll) task.setAssignedToId(assignee.getId());
                dao.update(task);
            } else if (assignAll && assigneeCombo.getItems().size() > 1) {
                for (User member : assigneeCombo.getItems()) {
                    if (member == ALL_TEAM) continue;
                    Task t = cloneTask();
                    t.setAssignedToId(member.getId());
                    dao.save(t);
                }
            } else {
                if (!assignAll) task.setAssignedToId(assignee.getId());
                dao.save(task);
            }
            confirmed = true;
            stage.close();
        } catch (Exception ex) {
            showErr(errGeneral, "Error: " + ex.getMessage());
        }
    }

    private Task cloneTask() {
        Task t = new Task();
        t.setTitle(task.getTitle()); t.setDescription(task.getDescription());
        t.setPriority(task.getPriority()); t.setStatus(task.getStatus());
        t.setSprintId(task.getSprintId()); t.setProjectId(task.getProjectId());
        return t;
    }

    public boolean showAndWait() {
        stage.showAndWait();
        return confirmed;
    }
}
