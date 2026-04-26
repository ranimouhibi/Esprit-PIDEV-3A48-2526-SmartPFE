package org.example.controller;

import org.example.dao.UserDAO;
import org.example.model.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class UserController implements Initializable {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, Boolean> colActive;

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private ComboBox<String> roleFilterCombo;
    @FXML private CheckBox activeCheck;
    @FXML private TextField searchField;
    @FXML private Label messageLabel;

    private final UserDAO userDAO = new UserDAO();
    private User selectedUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colActive.setCellValueFactory(new PropertyValueFactory<>("active"));

        roleCombo.setItems(FXCollections.observableArrayList("student", "supervisor", "establishment", "admin"));
        roleFilterCombo.setItems(FXCollections.observableArrayList("Tous", "student", "supervisor", "establishment", "admin"));

        loadUsers();

        userTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                selectedUser = sel;
                nameField.setText(sel.getName());
                emailField.setText(sel.getEmail());
                phoneField.setText(sel.getPhone());
                roleCombo.setValue(sel.getRole());
                activeCheck.setSelected(sel.isActive());
                passwordField.clear();
            }
        });
    }

    private void loadUsers() {
        try { userTable.setItems(FXCollections.observableArrayList(userDAO.findAll())); }
        catch (Exception e) { showMessage("Erreur: " + e.getMessage(), true); }
    }

    @FXML
    public void handleSave() {
        if (nameField.getText().trim().isEmpty() || emailField.getText().trim().isEmpty()) {
            showMessage("Nom et email sont obligatoires.", true);
            return;
        }
        try {
            User u = selectedUser != null ? selectedUser : new User();
            u.setName(nameField.getText().trim());
            u.setEmail(emailField.getText().trim());
            u.setPhone(phoneField.getText());
            u.setRole(roleCombo.getValue());
            u.setActive(activeCheck.isSelected());

            if (selectedUser == null) {
                if (passwordField.getText().isEmpty()) { showMessage("Mot de passe obligatoire.", true); return; }
                u.setPassword(passwordField.getText());
                userDAO.save(u);
            } else {
                userDAO.update(u);
            }

            showMessage("Utilisateur sauvegardé.", false);
            handleClear();
            loadUsers();
        } catch (Exception e) { showMessage("Erreur: " + e.getMessage(), true); }
    }

    @FXML
    public void handleDelete() {
        if (selectedUser == null) { showMessage("Sélectionnez un utilisateur.", true); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cet utilisateur?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    userDAO.delete(selectedUser.getId());
                    showMessage("Utilisateur supprimé.", false);
                    handleClear();
                    loadUsers();
                } catch (Exception e) { showMessage("Erreur: " + e.getMessage(), true); }
            }
        });
    }

    @FXML
    public void handleClear() {
        selectedUser = null;
        nameField.clear(); emailField.clear(); passwordField.clear(); phoneField.clear();
        roleCombo.setValue(null); activeCheck.setSelected(true);
        userTable.getSelectionModel().clearSelection();
    }

    @FXML
    public void handleSearch() {
        String q = searchField.getText().trim().toLowerCase();
        try {
            List<User> all = userDAO.findAll();
            if (!q.isEmpty()) all = all.stream().filter(u -> u.getName().toLowerCase().contains(q) || u.getEmail().toLowerCase().contains(q)).toList();
            userTable.setItems(FXCollections.observableArrayList(all));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void handleFilter() {
        String role = roleFilterCombo.getValue();
        try {
            List<User> users = (role == null || role.equals("Tous")) ? userDAO.findAll() : userDAO.findByRole(role);
            userTable.setItems(FXCollections.observableArrayList(users));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showMessage(String msg, boolean isError) {
        messageLabel.setText(msg);
        messageLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
}
