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
import java.util.stream.Collectors;

public class UserController implements Initializable {

    // ── Optional establishment scope (set before loading this pane) ───────────
    /** When non-null, only users belonging to this establishment are shown/managed. */
    private static Integer establishmentScope = null;

    public static void setEstablishmentScope(Integer id) { establishmentScope = id; }
    public static void clearEstablishmentScope()         { establishmentScope = null; }

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String>  colName;
    @FXML private TableColumn<User, String>  colEmail;
    @FXML private TableColumn<User, String>  colRole;
    @FXML private TableColumn<User, Boolean> colActive;

    @FXML private TextField     nameField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField     phoneField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private ComboBox<String> roleFilterCombo;
    @FXML private CheckBox      activeCheck;
    @FXML private TextField     searchField;
    @FXML private Label         messageLabel;
    @FXML private Label         titleLabel;

    private final UserDAO userDAO = new UserDAO();
    private User selectedUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colActive.setCellValueFactory(new PropertyValueFactory<>("active"));

        // In establishment scope, supervisors and students are the relevant roles
        if (establishmentScope != null) {
            roleCombo.setItems(FXCollections.observableArrayList("student", "supervisor"));
            roleFilterCombo.setItems(FXCollections.observableArrayList("All", "student", "supervisor"));
            if (titleLabel != null) titleLabel.setText("My Institution's Members");
        } else {
            roleCombo.setItems(FXCollections.observableArrayList("student", "supervisor", "establishment", "admin"));
            roleFilterCombo.setItems(FXCollections.observableArrayList("All", "student", "supervisor", "establishment", "admin"));
            if (titleLabel != null) titleLabel.setText("User Management");
        }

        loadUsers();

        userTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                selectedUser = sel;
                nameField.setText(sel.getName());
                emailField.setText(sel.getEmail());
                phoneField.setText(sel.getPhone() != null ? sel.getPhone() : "");
                roleCombo.setValue(sel.getRole());
                activeCheck.setSelected(sel.isActive());
                passwordField.clear();
            }
        });
    }

    private void loadUsers() {
        try {
            List<User> users;
            if (establishmentScope != null) {
                users = userDAO.findByEstablishment(establishmentScope);
            } else {
                users = userDAO.findAll();
            }
            userTable.setItems(FXCollections.observableArrayList(users));
        } catch (Exception e) {
            showMessage("Error loading users: " + e.getMessage(), true);
        }
    }

    @FXML
    public void handleSave() {
        if (nameField.getText().trim().isEmpty() || emailField.getText().trim().isEmpty()) {
            showMessage("Name and email are required.", true);
            return;
        }
        try {
            User u = selectedUser != null ? selectedUser : new User();
            u.setName(nameField.getText().trim());
            u.setEmail(emailField.getText().trim());
            u.setPhone(phoneField.getText());
            u.setRole(roleCombo.getValue());
            u.setActive(activeCheck.isSelected());

            // Preserve establishment link when editing in scoped mode
            if (establishmentScope != null) {
                u.setEstablishmentId(establishmentScope);
            }

            if (selectedUser == null) {
                if (passwordField.getText().isEmpty()) {
                    showMessage("Password is required for new users.", true);
                    return;
                }
                u.setPassword(passwordField.getText());
                userDAO.save(u);
            } else {
                userDAO.update(u);
            }

            showMessage("User saved successfully.", false);
            handleClear();
            loadUsers();
        } catch (Exception e) {
            showMessage("Error: " + e.getMessage(), true);
        }
    }

    @FXML
    public void handleDelete() {
        if (selectedUser == null) {
            showMessage("Select a user first.", true);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete user \"" + selectedUser.getName() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    userDAO.delete(selectedUser.getId());
                    showMessage("User deleted.", false);
                    handleClear();
                    loadUsers();
                } catch (Exception e) {
                    showMessage("Error: " + e.getMessage(), true);
                }
            }
        });
    }

    @FXML
    public void handleClear() {
        selectedUser = null;
        nameField.clear();
        emailField.clear();
        passwordField.clear();
        phoneField.clear();
        roleCombo.setValue(null);
        activeCheck.setSelected(true);
        userTable.getSelectionModel().clearSelection();
        messageLabel.setText("");
    }

    @FXML
    public void handleSearch() {
        String q = searchField.getText().trim().toLowerCase();
        try {
            List<User> base = establishmentScope != null
                    ? userDAO.findByEstablishment(establishmentScope)
                    : userDAO.findAll();
            if (!q.isEmpty()) {
                base = base.stream()
                        .filter(u -> u.getName().toLowerCase().contains(q)
                                  || u.getEmail().toLowerCase().contains(q))
                        .collect(Collectors.toList());
            }
            userTable.setItems(FXCollections.observableArrayList(base));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleFilter() {
        String role = roleFilterCombo.getValue();
        try {
            List<User> users;
            if (establishmentScope != null) {
                users = userDAO.findByEstablishment(establishmentScope);
                if (role != null && !role.equals("All")) {
                    users = users.stream()
                            .filter(u -> role.equals(u.getRole()))
                            .collect(Collectors.toList());
                }
            } else {
                users = (role == null || role.equals("All"))
                        ? userDAO.findAll()
                        : userDAO.findByRole(role);
            }
            userTable.setItems(FXCollections.observableArrayList(users));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showMessage(String msg, boolean isError) {
        messageLabel.setText(msg);
        messageLabel.setStyle(isError
                ? "-fx-text-fill: #dc2626; -fx-font-weight: bold;"
                : "-fx-text-fill: #16a34a; -fx-font-weight: bold;");
    }
}
