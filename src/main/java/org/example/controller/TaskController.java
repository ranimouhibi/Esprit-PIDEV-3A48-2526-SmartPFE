package org.example.controller;

import org.example.dao.TaskDAO;
import org.example.model.Task;
import org.example.model.User;
import org.example.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Window;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class TaskController implements Initializable {

    @FXML private TableView<Task> taskTable;
    @FXML private TableColumn<Task, Integer>   colId;
    @FXML private TableColumn<Task, String>    colTitle;
    @FXML private TableColumn<Task, String>    colStatus;
    @FXML private TableColumn<Task, String>    colPriority;
    @FXML private TableColumn<Task, String>    colProject;
    @FXML private TableColumn<Task, String>    colAssigned;
    @FXML private TextField searchField;
    @FXML private Label messageLabel;

    private final TaskDAO taskDAO = new TaskDAO();
    private Task selectedTask;
    private List<Task> allTasks = List.of();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        colProject.setCellValueFactory(new PropertyValueFactory<>("projectTitle"));
        colAssigned.setCellValueFactory(new PropertyValueFactory<>("assignedToName"));

        loadTasks();

        User current = SessionManager.getCurrentUser();
        if (current != null && !"admin".equals(current.getRole())) {
            colId.setVisible(false);
        }

        taskTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> selectedTask = sel);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, val) -> applySearch(val));
        }
    }

    private void loadTasks() {
        try {
            allTasks = taskDAO.findAll();
            taskTable.setItems(FXCollections.observableArrayList(allTasks));
        } catch (Exception e) { showMessage("Error: " + e.getMessage(), true); }
    }

    private void applySearch(String query) {
        if (query == null || query.isBlank()) {
            taskTable.setItems(FXCollections.observableArrayList(allTasks));
            return;
        }
        String q = query.toLowerCase();
        taskTable.setItems(FXCollections.observableArrayList(
            allTasks.stream().filter(t ->
                (t.getTitle() != null && t.getTitle().toLowerCase().contains(q)) ||
                (t.getDescription() != null && t.getDescription().toLowerCase().contains(q)) ||
                (t.getStatus() != null && t.getStatus().toLowerCase().contains(q)) ||
                (t.getAssignedToName() != null && t.getAssignedToName().toLowerCase().contains(q)) ||
                (t.getProjectTitle() != null && t.getProjectTitle().toLowerCase().contains(q))
            ).toList()
        ));
    }

    @FXML public void handleSearch() { if (searchField != null) applySearch(searchField.getText()); }

    @FXML
    public void handleAddDialog() {
        Window owner = taskTable.getScene().getWindow();
        if (new TaskDialog(owner, null).showAndWait()) loadTasks();
    }

    @FXML
    public void handleEditDialog() {
        if (selectedTask == null) { showMessage("Please select a task to edit.", true); return; }
        Window owner = taskTable.getScene().getWindow();
        if (new TaskDialog(owner, selectedTask).showAndWait()) { loadTasks(); selectedTask = null; }
    }

    @FXML
    public void handleDelete() {
        if (selectedTask == null) { showMessage("Please select a task.", true); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this task?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    taskDAO.delete(selectedTask.getId());
                    selectedTask = null;
                    showMessage("Task deleted.", false);
                    loadTasks();
                } catch (Exception e) { showMessage("Error: " + e.getMessage(), true); }
            }
        });
    }

    private void showMessage(String msg, boolean isError) {
        if (messageLabel != null) {
            messageLabel.setText(msg);
            messageLabel.setStyle(isError ? "-fx-text-fill: #dc2626;" : "-fx-text-fill: #16a34a;");
        }
    }
}

