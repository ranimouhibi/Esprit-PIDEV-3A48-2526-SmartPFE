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
import org.example.model.Project;
import org.example.model.Sprint;
import org.example.model.Task;
import org.example.util.SessionManager;

import java.time.LocalDate;

public class SprintDialog {

    private static final int NAME_MIN = 3;
    private static final int NAME_MAX = 60;
    private static final int DESC_MAX = 300;

    private final Stage stage;
    private final Sprint sprint;
    private boolean confirmed = false;

    private final ComboBox<Project> projectCombo = new ComboBox<>();
    private final TextField         nameField     = new TextField();
    private final TextArea          goalField     = new TextArea();
    private final DatePicker        startPicker   = new DatePicker();
    private final DatePicker        endPicker     = new DatePicker();
    private final ComboBox<String>  statusCombo   = new ComboBox<>();

    private final Label errProject = errLabel();
    private final Label errName    = errLabel();
    private final Label errDesc    = errLabel();
    private final Label errStart   = errLabel();
    private final Label errEnd     = errLabel();
    private final Label errStatus  = errLabel();
    private final Label errGeneral = errLabel();

    public SprintDialog(Window owner, Sprint existing) {
        this.sprint = existing != null ? existing : new Sprint();
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(existing == null ? "New Sprint" : "Edit Sprint");
        stage.setResizable(false);
        stage.setScene(new Scene(buildLayout(), 460, 560));
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
        if (sprint.getId() == 0) {
            statusCombo.setItems(FXCollections.observableArrayList("planned", "active"));
        } else {
            switch (sprint.getStatus() != null ? sprint.getStatus() : "planned") {
                case "planned" -> statusCombo.setItems(FXCollections.observableArrayList("planned", "active", "closed"));
                case "active"  -> statusCombo.setItems(FXCollections.observableArrayList("active", "closed"));
                default        -> statusCombo.setItems(FXCollections.observableArrayList("closed"));
            }
        }

        String inputStyle = "-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 7 10;";
        projectCombo.setMaxWidth(Double.MAX_VALUE); projectCombo.setStyle(inputStyle);
        nameField.setPromptText("Between " + NAME_MIN + " and " + NAME_MAX + " characters");
        nameField.setStyle(inputStyle);
        goalField.setPromptText("Optional description (max " + DESC_MAX + " characters)");
        goalField.setPrefRowCount(3); goalField.setWrapText(true); goalField.setStyle(inputStyle);
        startPicker.setMaxWidth(Double.MAX_VALUE); startPicker.setStyle(inputStyle);
        endPicker.setMaxWidth(Double.MAX_VALUE);   endPicker.setStyle(inputStyle);

        // Disable past dates in the calendar popup
        javafx.util.Callback<javafx.scene.control.DatePicker, javafx.scene.control.DateCell> pastDayFactory =
            dp -> new javafx.scene.control.DateCell() {
                @Override public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    if (date.isBefore(LocalDate.now())) { setDisable(true); setStyle("-fx-background-color: #f3f4f6;"); }
                }
            };
        startPicker.setDayCellFactory(pastDayFactory);
        endPicker.setDayCellFactory(pastDayFactory);
        statusCombo.setMaxWidth(Double.MAX_VALUE); statusCombo.setStyle(inputStyle);

        Button confirmBtn = new Button("Confirm");
        confirmBtn.setStyle("-fx-background-color: #a12c2f; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 9 24; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13px;");
        confirmBtn.setOnAction(e -> handleConfirm());

        try {
            var user = SessionManager.getCurrentUser();
            var projects = (user != null)
                ? new ProjectDAO().findForUser(user.getId(), user.getRole())
                : new ProjectDAO().findAll();
            projectCombo.setItems(FXCollections.observableArrayList(projects));
            if (projects.isEmpty()) {
                showErr(errGeneral, "You have no projects. Please create a project first.");
                confirmBtn.setDisable(true);
            }
        } catch (Exception e) {
            showErr(errGeneral, "Unable to load projects.");
            confirmBtn.setDisable(true);
        }

        if (sprint.getId() != 0) {
            nameField.setText(sprint.getName());
            goalField.setText(sprint.getGoal() != null ? sprint.getGoal() : "");
            startPicker.setValue(sprint.getStartDate());
            endPicker.setValue(sprint.getEndDate());
            statusCombo.setValue(sprint.getStatus());
            projectCombo.getItems().stream()
                .filter(p -> p.getId() == sprint.getProjectId())
                .findFirst().ifPresent(projectCombo::setValue);
        }

        nameField.textProperty().addListener((o, old, val) -> validateName(val));
        goalField.textProperty().addListener((o, old, val) -> validateDesc(val));
        startPicker.valueProperty().addListener((o, old, val) -> { clearErr(errStart); markValid(startPicker); validateDates(); });
        endPicker.valueProperty().addListener((o, old, val)   -> { clearErr(errEnd);   markValid(endPicker);   validateDates(); });
        projectCombo.valueProperty().addListener((o, old, val) -> { if (val != null) { clearErr(errProject); markValid(projectCombo); } });
        statusCombo.valueProperty().addListener((o, old, val)  -> { if (val != null) { clearErr(errStatus);  markValid(statusCombo);  } });

        VBox form = new VBox(4);
        form.setPadding(new Insets(20));
        form.getChildren().addAll(
            fieldLabel("Project *"),                                                    projectCombo, errProject,
            fieldLabel("Sprint Name *  (" + NAME_MIN + "-" + NAME_MAX + " chars)"),    nameField,    errName,
            fieldLabel("Description  (max " + DESC_MAX + " chars)"),                   goalField,    errDesc,
            fieldLabel("Start Date *"),                                                 startPicker,  errStart,
            fieldLabel("End Date *"),                                                   endPicker,    errEnd,
            fieldLabel("Status *"),                                                     statusCombo,  errStatus,
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

    private boolean validateName(String val) {
        String v = val == null ? "" : val.trim();
        if (v.isEmpty())           { showErr(errName, "Sprint name is required.");                                    markInvalid(nameField); return false; }
        if (v.length() < NAME_MIN) { showErr(errName, "Name must be at least " + NAME_MIN + " characters.");          markInvalid(nameField); return false; }
        if (v.length() > NAME_MAX) { showErr(errName, "Name cannot exceed " + NAME_MAX + " characters.");             markInvalid(nameField); return false; }
        clearErr(errName); markValid(nameField); return true;
    }

    private boolean validateDesc(String val) {
        String v = val == null ? "" : val;
        if (v.length() > DESC_MAX) {
            showErr(errDesc, "Description cannot exceed " + DESC_MAX + " characters (" + v.length() + "/" + DESC_MAX + ").");
            markInvalid(goalField); return false;
        }
        clearErr(errDesc); markValid(goalField); return true;
    }

    private boolean validateDates() {
        LocalDate today = LocalDate.now();
        LocalDate start = startPicker.getValue();
        LocalDate end   = endPicker.getValue();
        boolean ok = true;

        if (start == null) {
            showErr(errStart, "Start date is required."); markInvalid(startPicker); ok = false;
        } else if (start.isBefore(today)) {
            showErr(errStart, "Start date cannot be in the past."); markInvalid(startPicker); ok = false;
        } else {
            clearErr(errStart); markValid(startPicker);
        }

        if (end == null) {
            showErr(errEnd, "End date is required."); markInvalid(endPicker); ok = false;
        } else if (end.isBefore(today)) {
            showErr(errEnd, "End date cannot be in the past."); markInvalid(endPicker); ok = false;
        } else if (start != null && end != null && !end.isAfter(start)) {
            showErr(errEnd, "End date must be after the start date."); markInvalid(endPicker); ok = false;
        } else {
            clearErr(errEnd); markValid(endPicker);
        }

        return ok;
    }

    private boolean validateAll() {
        boolean ok = true;
        if (projectCombo.getValue() == null) { showErr(errProject, "Project is required."); markInvalid(projectCombo); ok = false; }
        if (!validateName(nameField.getText())) ok = false;
        if (!validateDesc(goalField.getText()))  ok = false;
        if (!validateDates())                    ok = false;
        if (statusCombo.getValue() == null)  { showErr(errStatus, "Status is required."); markInvalid(statusCombo); ok = false; }
        return ok;
    }

    private void handleConfirm() {
        clearErr(errGeneral);
        if (!validateAll()) return;
        sprint.setProjectId(projectCombo.getValue().getId());
        sprint.setName(nameField.getText().trim());
        sprint.setGoal(goalField.getText().trim());
        sprint.setStartDate(startPicker.getValue());
        sprint.setEndDate(endPicker.getValue());
        sprint.setStatus(statusCombo.getValue());
        try {
            SprintDAO dao = new SprintDAO();
            if (dao.hasOverlap(sprint.getProjectId(), sprint.getStartDate(), sprint.getEndDate(), sprint.getId())) {
                showErr(errEnd, "These dates overlap with an existing sprint in this project.");
                markInvalid(startPicker); markInvalid(endPicker);
                return;
            }
            if (sprint.getId() == 0) dao.save(sprint);
            else dao.update(sprint);
            // When a sprint is closed, mark all its tasks as done
            if ("closed".equals(sprint.getStatus())) {
                TaskDAO taskDAO = new TaskDAO();
                for (Task t : taskDAO.findBySprint(sprint.getId())) {
                    if (!"done".equals(t.getStatus())) {
                        t.setStatus("done");
                        taskDAO.update(t);
                    }
                }
            }
            confirmed = true;
            stage.close();
        } catch (Exception ex) {
            showErr(errGeneral, "Error: " + ex.getMessage());
        }
    }

    public boolean showAndWait() {
        stage.showAndWait();
        return confirmed;
    }
}
