package org.example.controller;

import org.example.dao.UserDAO;
import org.example.model.User;
import org.example.util.NavigationUtil;
import org.example.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        try {
            User user = userDAO.authenticate(email, password);
            if (user != null) {
                SessionManager.setCurrentUser(user);
                NavigationUtil.navigateTo("Dashboard.fxml");
            } else {
                errorLabel.setText("Email ou mot de passe incorrect.");
            }
        } catch (Exception e) {
            errorLabel.setText("Erreur de connexion à la base de données.");
            e.printStackTrace();
        }
    }
}
