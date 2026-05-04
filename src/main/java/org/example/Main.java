package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.dao.UserDAO;
import org.example.model.User;
import org.example.util.LocalSessionStore;
import org.example.util.SessionManager;

public class Main extends Application {

    public static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        stage.setTitle("SmartPFE Desktop");
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.setResizable(true);

        // ── Auto-login: check for a saved session token ───────────────────────
        String startFxml = tryAutoLogin();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + startFxml));
        Scene scene = new Scene(loader.load(), 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Attempts to restore a previous session from disk.
     * Returns the FXML to open: the appropriate dashboard if successful,
     * or "Home.fxml" if no valid session exists.
     */
    private String tryAutoLogin() {
        try {
            LocalSessionStore.SavedSession saved = LocalSessionStore.loadSession();
            if (saved == null) return "Home.fxml";

            UserDAO dao = new UserDAO();
            User user = dao.findByRememberToken(saved.token());
            if (user == null || user.getId() != saved.userId()) {
                LocalSessionStore.clearSession();
                return "Home.fxml";
            }

            // Valid token — restore session
            SessionManager.setCurrentUser(user);
            return switch (user.getRole()) {
                case "student"       -> "StudentDashboard.fxml";
                case "supervisor"    -> "SupervisorDashboard.fxml";
                case "establishment" -> "EstablishmentDashboard.fxml";
                default              -> "Dashboard.fxml";
            };
        } catch (Exception e) {
            e.printStackTrace();
            LocalSessionStore.clearSession();
            return "Home.fxml";
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
