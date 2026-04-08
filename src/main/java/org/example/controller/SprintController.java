package org.example.controller;

import org.example.dao.ProjectDAO;
import org.example.dao.SprintDAO;
import org.example.model.Project;
import org.example.model.Sprint;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class SprintController implements Initializable {

    @FXML private TableView<Sprint> sprintTable;
    @FXML private TableColumn<Sprint, Integer> colId;
    @FXML private TableColumn<Sprint, Integer> colNumber;
    @FXML private TableColumn<Sprint, String> colName;
    @FXML private TableColumn<Sprint, String> colStatus;
    @FXML private TableColumn<Sprint, String> colProject;
    @FXML private TableColumn<Sprint, LocalDate> colStart;
    @FXML private TableColumn<Sprint, LocalDate> colEnd;

    @FXML private ComboBox<Project> projectCombo;
    @FXML private TextField nameField;
    @FXML private TextArea goalField;
    @FXML private Spinner<Integer> numberSpinner;
    @FXML private ComboBox<String> statusCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextArea retrospectiveField;
    @FXML private Label messageLabel;

    private final SprintDAO sprintDAO = new SprintDAO();
    private final ProjectDAO projectDAO = new ProjectDAO();
    private Sprint selectedSprint;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNumber.setCellValueFactory(new PropertyValueFactory<>("sprintNumber"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colProject.setCellValueFactory(new PropertyValueFactory<>("projectTitle"));
        colStart.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        colEnd.setCellValueFactory(new PropertyValueFactory<>("endDate"));

        statusCombo.setItems(FXCollections.observableArrayList("planned", "active", "closed"));
        numberSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1));

        try { projectCombo.setItems(FXCollections.observableArrayList(projectDAO.findAll())); }
        catch (Exception e) { e.printStackTrace(); }

        loadSprints();

        sprintTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                selectedSprint = sel;
                nameField.setText(sel.getName());
                goalField.setText(sel.getGoal());
                numberSpinner.getValueFactory().setValue(sel.getSprintNumber());
                statusCombo.setValue(sel.getStatus());
                startDatePicker.setValue(sel.getStartDate());
                endDatePicker.setValue(sel.getEndDate());
                retrospectiveField.setText(sel.getRetrospective());
            }
        });
    }

    private void loadSprints() {
        try { sprintTable.setItems(FXCollections.observableArrayList(sprintDAO.findAll())); }
        catch (Exception e) { showMessage("Erreur: " + e.getMessage(), true); }
    }

    @FXML
    public void handleSave() {
        if (nameField.getText().trim().isEmpty() || projectCombo.getValue() == null) {
            showMessage("Nom et projet sont obligatoires.", true);
            return;
        }
        try {
            Sprint s = selectedSprint != null ? selectedSprint : new Sprint();
            s.setProjectId(projectCombo.getValue().getId());
            s.setName(nameField.getText().trim());
            s.setGoal(goalField.getText());
            s.setSprintNumber(numberSpinner.getValue());
            s.setStatus(statusCombo.getValue());
            s.setStartDate(startDatePicker.getValue());
            s.setEndDate(endDatePicker.getValue());
            s.setRetrospective(retrospectiveField.getText());

            if (selectedSprint == null) sprintDAO.save(s);
            else sprintDAO.update(s);

            showMessage("Sprint sauvegardé.", false);
            handleClear();
            loadSprints();
        } catch (Exception e) { showMessage("Erreur: " + e.getMessage(), true); }
    }

    @FXML
    public void handleDelete() {
        if (selectedSprint == null) { showMessage("Sélectionnez un sprint.", true); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce sprint?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    sprintDAO.delete(selectedSprint.getId());
                    showMessage("Sprint supprimé.", false);
                    handleClear();
                    loadSprints();
                } catch (Exception e) { showMessage("Erreur: " + e.getMessage(), true); }
            }
        });
    }

    @FXML
    public void handleClear() {
        selectedSprint = null;
        nameField.clear();
        goalField.clear();
        retrospectiveField.clear();
        statusCombo.setValue(null);
        projectCombo.setValue(null);
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        sprintTable.getSelectionModel().clearSelection();
    }

    private void showMessage(String msg, boolean isError) {
        messageLabel.setText(msg);
        messageLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
}
