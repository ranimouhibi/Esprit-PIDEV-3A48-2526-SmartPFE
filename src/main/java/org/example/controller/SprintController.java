package org.example.controller;

import org.example.dao.ProjectDAO;
import org.example.dao.SprintDAO;
import org.example.dao.TaskDAO;
import org.example.model.Sprint;
import org.example.model.Task;
import org.example.model.User;
import org.example.util.PDFExporter;
import org.example.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class SprintController implements Initializable {

    @FXML private TableView<Sprint> sprintTable;
    @FXML private TableColumn<Sprint, Integer>   colId;
    @FXML private TableColumn<Sprint, Integer>   colNumber;
    @FXML private TableColumn<Sprint, String>    colName;
    @FXML private TableColumn<Sprint, String>    colStatus;
    @FXML private TableColumn<Sprint, String>    colProject;
    @FXML private TableColumn<Sprint, LocalDate> colStart;
    @FXML private TableColumn<Sprint, LocalDate> colEnd;
    @FXML private TextField searchField;
    @FXML private Label messageLabel;

    private final SprintDAO sprintDAO = new SprintDAO();
    private Sprint selectedSprint;
    private List<Sprint> allSprints = List.of();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNumber.setCellValueFactory(new PropertyValueFactory<>("sprintNumber"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colProject.setCellValueFactory(new PropertyValueFactory<>("projectTitle"));
        colStart.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        colEnd.setCellValueFactory(new PropertyValueFactory<>("endDate"));

        loadSprints();

        User current = SessionManager.getCurrentUser();
        if (current != null && !"admin".equals(current.getRole())) {
            colId.setVisible(false);
            colNumber.setVisible(false);
        }

        sprintTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> selectedSprint = sel);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, val) -> applySearch(val));
        }
    }

    private void loadSprints() {
        try {
            allSprints = sprintDAO.findAll();
            sprintTable.setItems(FXCollections.observableArrayList(allSprints));
        } catch (Exception e) { showMessage("Error: " + e.getMessage(), true); }
    }

    private void applySearch(String query) {
        if (query == null || query.isBlank()) {
            sprintTable.setItems(FXCollections.observableArrayList(allSprints));
            return;
        }
        String q = query.toLowerCase();
        sprintTable.setItems(FXCollections.observableArrayList(
            allSprints.stream().filter(s ->
                (s.getName() != null && s.getName().toLowerCase().contains(q)) ||
                (s.getStatus() != null && s.getStatus().toLowerCase().contains(q)) ||
                (s.getProjectTitle() != null && s.getProjectTitle().toLowerCase().contains(q))
            ).toList()
        ));
    }

    @FXML public void handleSearch() { if (searchField != null) applySearch(searchField.getText()); }

    @FXML
    public void handleAddDialog() {
        Window owner = sprintTable.getScene().getWindow();
        if (new SprintDialog(owner, null).showAndWait()) loadSprints();
    }

    @FXML
    public void handleEditDialog() {
        if (selectedSprint == null) { showMessage("Please select a sprint to edit.", true); return; }
        if ("closed".equals(selectedSprint.getStatus())) { showMessage("This sprint is closed and cannot be edited.", true); return; }
        Window owner = sprintTable.getScene().getWindow();
        if (new SprintDialog(owner, selectedSprint).showAndWait()) { loadSprints(); selectedSprint = null; }
    }

    @FXML
    public void handleDelete() {
        if (selectedSprint == null) { showMessage("Please select a sprint.", true); return; }
        User current = SessionManager.getCurrentUser();
        if (current != null && "supervisor".equals(current.getRole()) && !"planned".equals(selectedSprint.getStatus())) {
            showMessage("Only sprints with 'planned' status can be deleted.", true);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete this sprint? All associated tasks will also be deleted.",
            ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    sprintDAO.delete(selectedSprint.getId());
                    selectedSprint = null;
                    showMessage("Sprint deleted.", false);
                    loadSprints();
                } catch (Exception e) { showMessage("Error: " + e.getMessage(), true); }
            }
        });
    }

    @FXML
    public void handleStats() {
        Window owner = sprintTable.getScene().getWindow();
        new SprintStatsController(owner).show();
    }

    @FXML
    public void handleExportPdf() {
        try {
            User user = SessionManager.getCurrentUser();
            var projectDAO = new ProjectDAO();
            var projects = (user != null)
                ? projectDAO.findForUser(user.getId(), user.getRole())
                : projectDAO.findAll();

            List<Sprint> sprints = new java.util.ArrayList<>();
            for (var p : projects) sprints.addAll(sprintDAO.findByProject(p.getId()));
            List<Task> tasks = new TaskDAO().findAll();

            FileChooser fc = new FileChooser();
            fc.setTitle("Save PDF Report");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            fc.setInitialFileName("sprint_report.pdf");
            File file = fc.showSaveDialog(sprintTable.getScene().getWindow());
            if (file != null) {
                PDFExporter.export(sprints, tasks, file);
                showMessage("PDF exported successfully.", false);
            }
        } catch (Exception e) { showMessage("Export error: " + e.getMessage(), true); }
    }

    private void showMessage(String msg, boolean isError) {
        if (messageLabel != null) {
            messageLabel.setText(msg);
            messageLabel.setStyle(isError ? "-fx-text-fill: #dc2626;" : "-fx-text-fill: #16a34a;");
        }
    }
}
