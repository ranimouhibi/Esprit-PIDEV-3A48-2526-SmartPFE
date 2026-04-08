package org.example.controller;

import org.example.dao.ProjectDAO;
import org.example.dao.SprintDAO;
import org.example.dao.TaskDAO;
import org.example.dao.UserDAO;
import org.example.model.Project;
import org.example.model.Sprint;
import org.example.model.Task;
import org.example.model.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class TaskController implements Initializable {

    @FXML private TableView<Task> taskTable;
    @FXML private TableColumn<Task, Integer> colId;
    @FXML private TableColumn<Task, String> colTitle;
    @FXML private TableColumn<Task, String> colStatus;
    @FXML private TableColumn<Task, String> colPriority;
    @FXML private TableColumn<Task, String> colProject;
    @FXML private TableColumn<Task, String> colAssigned;
    @FXML private TableColumn<Task, LocalDate> colDeadline;

    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private ComboBox<String> priorityCombo;
    @FXML private ComboBox<Project> projectCombo;
    @FXML private ComboBox<Sprint> sprintCombo;
    @FXML private ComboBox<User> assignedCombo;
    @FXML private Spinner<Integer> storyPointsSpinner;
    @FXML private DatePicker deadlinePicker;
    @FXML private CheckBox blockedCheck;
    @FXML private TextField searchField;
    @FXML private Label messageLabel;

    private final TaskDAO taskDAO = new TaskDAO();
    private final ProjectDAO projectDAO = new ProjectDAO();
    private final SprintDAO sprintDAO = new SprintDAO();
    private final UserDAO userDAO = new UserDAO();
    private Task selectedTask;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        statusCombo.setItems(FXCollections.observableArrayList("todo", "in_progress", "review", "done"));
        priorityCombo.setItems(FXCollections.observableArrayList("low", "medium", "high", "critical"));
        storyPointsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 1));
        loadProjects();
        loadUsers();
        loadTasks();

        projectCombo.setOnAction(e -> {
            Project p = projectCombo.getValue();
            if (p != null) loadSprintsForProject(p.getId());
        });

        taskTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) populateForm(sel);
        });
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        colProject.setCellValueFactory(new PropertyValueFactory<>("projectTitle"));
        colAssigned.setCellValueFactory(new PropertyValueFactory<>("assignedToName"));
        colDeadline.setCellValueFactory(new PropertyValueFactory<>("deadline"));
    }

    private void loadProjects() {
        try { projectCombo.setItems(FXCollections.observableArrayList(projectDAO.findAll())); }
        catch (Exception e) { e.printStackTrace(); }
    }

    private void loadSprintsForProject(int projectId) {
        try { sprintCombo.setItems(FXCollections.observableArrayList(sprintDAO.findByProject(projectId))); }
        catch (Exception e) { e.printStackTrace(); }
    }

    private void loadUsers() {
        try { assignedCombo.setItems(FXCollections.observableArrayList(userDAO.findAll())); }
        catch (Exception e) { e.printStackTrace(); }
    }

    private void loadTasks() {
        try { taskTable.setItems(FXCollections.observableArrayList(taskDAO.findAll())); }
        catch (Exception e) { showMessage("Erreur: " + e.getMessage(), true); }
    }

    private void populateForm(Task t) {
        selectedTask = t;
        titleField.setText(t.getTitle());
        descriptionField.setText(t.getDescription());
        statusCombo.setValue(t.getStatus());
        priorityCombo.setValue(t.getPriority());
        storyPointsSpinner.getValueFactory().setValue(t.getStoryPoints());
        deadlinePicker.setValue(t.getDeadline());
        blockedCheck.setSelected(t.isBlocked());
    }

    @FXML
    public void handleSave() {
        if (titleField.getText().trim().isEmpty() || projectCombo.getValue() == null) {
            showMessage("Titre et projet sont obligatoires.", true);
            return;
        }
        try {
            Task t = selectedTask != null ? selectedTask : new Task();
            t.setTitle(titleField.getText().trim());
            t.setDescription(descriptionField.getText());
            t.setStatus(statusCombo.getValue());
            t.setPriority(priorityCombo.getValue());
            t.setProjectId(projectCombo.getValue().getId());
            if (sprintCombo.getValue() != null) t.setSprintId(sprintCombo.getValue().getId());
            if (assignedCombo.getValue() != null) t.setAssignedToId(assignedCombo.getValue().getId());
            t.setStoryPoints(storyPointsSpinner.getValue());
            t.setDeadline(deadlinePicker.getValue());
            t.setBlocked(blockedCheck.isSelected());

            if (selectedTask == null) taskDAO.save(t);
            else taskDAO.update(t);

            showMessage("Tâche sauvegardée.", false);
            handleClear();
            loadTasks();
        } catch (Exception e) { showMessage("Erreur: " + e.getMessage(), true); }
    }

    @FXML
    public void handleDelete() {
        if (selectedTask == null) { showMessage("Sélectionnez une tâche.", true); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cette tâche?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    taskDAO.delete(selectedTask.getId());
                    showMessage("Tâche supprimée.", false);
                    handleClear();
                    loadTasks();
                } catch (Exception e) { showMessage("Erreur: " + e.getMessage(), true); }
            }
        });
    }

    @FXML
    public void handleClear() {
        selectedTask = null;
        titleField.clear();
        descriptionField.clear();
        statusCombo.setValue(null);
        priorityCombo.setValue(null);
        projectCombo.setValue(null);
        sprintCombo.setValue(null);
        assignedCombo.setValue(null);
        deadlinePicker.setValue(null);
        blockedCheck.setSelected(false);
        taskTable.getSelectionModel().clearSelection();
    }

    @FXML
    public void handleSearch() {
        String query = searchField.getText().trim().toLowerCase();
        try {
            List<Task> all = taskDAO.findAll();
            if (!query.isEmpty()) all = all.stream().filter(t -> t.getTitle().toLowerCase().contains(query)).toList();
            taskTable.setItems(FXCollections.observableArrayList(all));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showMessage(String msg, boolean isError) {
        messageLabel.setText(msg);
        messageLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
}
