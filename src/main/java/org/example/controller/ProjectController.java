package org.example.controller;

import org.example.dao.ProjectDAO;
import org.example.dao.UserDAO;
import org.example.model.Project;
import org.example.model.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ProjectController implements Initializable {

    @FXML private TableView<Project> projectTable;
    @FXML private TableColumn<Project, Integer> colId;
    @FXML private TableColumn<Project, String> colTitle;
    @FXML private TableColumn<Project, String> colType;
    @FXML private TableColumn<Project, String> colStatus;
    @FXML private TableColumn<Project, String> colOwner;
    @FXML private TableColumn<Project, String> colSupervisor;

    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private ComboBox<User> ownerCombo;
    @FXML private ComboBox<User> supervisorCombo;
    @FXML private TextField searchField;
    @FXML private Label messageLabel;

    private final ProjectDAO projectDAO = new ProjectDAO();
    private final UserDAO userDAO = new UserDAO();
    private Project selectedProject;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        typeCombo.setItems(FXCollections.observableArrayList("pfe", "stage", "projet_libre"));
        statusCombo.setItems(FXCollections.observableArrayList("created", "active", "completed", "archived"));
        loadUsers();
        loadProjects();

        projectTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) populateForm(selected);
        });
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colType.setCellValueFactory(new PropertyValueFactory<>("projectType"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colOwner.setCellValueFactory(new PropertyValueFactory<>("ownerName"));
        colSupervisor.setCellValueFactory(new PropertyValueFactory<>("supervisorName"));
    }

    private void loadUsers() {
        try {
            List<User> students = userDAO.findByRole("student");
            List<User> supervisors = userDAO.findByRole("supervisor");
            ownerCombo.setItems(FXCollections.observableArrayList(students));
            supervisorCombo.setItems(FXCollections.observableArrayList(supervisors));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadProjects() {
        try {
            projectTable.setItems(FXCollections.observableArrayList(projectDAO.findAll()));
        } catch (Exception e) { showMessage("Erreur chargement: " + e.getMessage(), true); }
    }

    private void populateForm(Project p) {
        selectedProject = p;
        titleField.setText(p.getTitle());
        descriptionField.setText(p.getDescription());
        typeCombo.setValue(p.getProjectType());
        statusCombo.setValue(p.getStatus());
    }

    @FXML
    public void handleSave() {
        if (titleField.getText().trim().isEmpty()) {
            showMessage("Le titre est obligatoire.", true);
            return;
        }
        try {
            Project p = selectedProject != null ? selectedProject : new Project();
            p.setTitle(titleField.getText().trim());
            p.setDescription(descriptionField.getText());
            p.setProjectType(typeCombo.getValue());
            p.setStatus(statusCombo.getValue());
            if (ownerCombo.getValue() != null) p.setOwnerId(ownerCombo.getValue().getId());
            if (supervisorCombo.getValue() != null) p.setSupervisorId(supervisorCombo.getValue().getId());

            if (selectedProject == null) projectDAO.save(p);
            else projectDAO.update(p);

            showMessage("Projet sauvegardé avec succès.", false);
            handleClear();
            loadProjects();
        } catch (Exception e) { showMessage("Erreur: " + e.getMessage(), true); }
    }

    @FXML
    public void handleDelete() {
        if (selectedProject == null) { showMessage("Sélectionnez un projet.", true); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce projet?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    projectDAO.delete(selectedProject.getId());
                    showMessage("Projet supprimé.", false);
                    handleClear();
                    loadProjects();
                } catch (Exception e) { showMessage("Erreur: " + e.getMessage(), true); }
            }
        });
    }

    @FXML
    public void handleClear() {
        selectedProject = null;
        titleField.clear();
        descriptionField.clear();
        typeCombo.setValue(null);
        statusCombo.setValue(null);
        ownerCombo.setValue(null);
        supervisorCombo.setValue(null);
        projectTable.getSelectionModel().clearSelection();
    }

    @FXML
    public void handleSearch() {
        String query = searchField.getText().trim().toLowerCase();
        try {
            List<Project> all = projectDAO.findAll();
            if (!query.isEmpty()) {
                all = all.stream().filter(p -> p.getTitle().toLowerCase().contains(query)).toList();
            }
            projectTable.setItems(FXCollections.observableArrayList(all));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showMessage(String msg, boolean isError) {
        messageLabel.setText(msg);
        messageLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
}
