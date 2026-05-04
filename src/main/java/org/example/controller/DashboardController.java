package org.example.controller;

import org.example.dao.ProjectDAO;
import org.example.dao.SprintDAO;
import org.example.dao.TaskDAO;
import org.example.dao.UserDAO;
import org.example.dao.CommentDAO;
import org.example.dao.DocumentDAO;
import org.example.model.User;
import org.example.model.Project;
import org.example.model.Comment;
import org.example.util.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.application.Platform;

import java.net.URL;
import java.util.ResourceBundle;
import java.io.File;
import java.util.List;
import java.awt.Desktop;

public class DashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Label projectCountLabel;
    @FXML private Label sprintCountLabel;
    @FXML private Label taskCountLabel;
    @FXML private Label userCountLabel;
    @FXML private BorderPane contentArea;
    @FXML private VBox statsPane;

    // Sidebar buttons
    @FXML private Button btnProjects;
    @FXML private Button btnComments;
    @FXML private Button btnDocuments;
    @FXML private Button btnSprints;
    @FXML private Button btnTasks;
    @FXML private Button btnMeetings;
    @FXML private Button btnCandidatures;
    @FXML private Button btnUsers;
    @FXML private Button btnAuditLog;

    private static final String STYLE_INACTIVE = "-fx-background-color: transparent; -fx-text-fill: #ccc; -fx-alignment: CENTER-LEFT; -fx-cursor: hand; -fx-font-size: 13px; -fx-background-radius: 8; -fx-padding: 9 12;";
    private static final String STYLE_ACTIVE   = "-fx-background-color: #a12c2f; -fx-text-fill: white; -fx-alignment: CENTER-LEFT; -fx-cursor: hand; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 9 12;";

    private final ProjectDAO projectDAO = new ProjectDAO();
    private final SprintDAO sprintDAO = new SprintDAO();
    private final TaskDAO taskDAO = new TaskDAO();
    private final UserDAO userDAO = new UserDAO();
    private final CommentDAO commentDAO = new CommentDAO();
    private final DocumentDAO documentDAO = new DocumentDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            welcomeLabel.setText("Hello, " + user.getName());
            roleLabel.setText("Role: " + user.getRole().toUpperCase());
        }
        loadStats();
    }

    private void loadStats() {
        try {
            projectCountLabel.setText(String.valueOf(projectDAO.findAll().size()));
            sprintCountLabel.setText(String.valueOf(sprintDAO.findAll().size()));
            taskCountLabel.setText(String.valueOf(taskDAO.findAll().size()));
            userCountLabel.setText(String.valueOf(userDAO.findAll().size()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setActiveButton(Button active) {
        Button[] all = {btnProjects, btnComments, btnDocuments, btnSprints, btnTasks, btnMeetings, btnCandidatures, btnUsers, btnAuditLog};
        for (Button b : all) {
            b.setStyle(STYLE_INACTIVE);
        }
        active.setStyle(STYLE_ACTIVE);
    }

    @FXML public void showDashboard() {
        Button[] all = {btnProjects, btnComments, btnDocuments, btnSprints, btnTasks, btnMeetings, btnCandidatures, btnUsers, btnAuditLog};
        for (Button b : all) b.setStyle(STYLE_INACTIVE);
        contentArea.setCenter(statsPane);
        loadStats();
    }

    @FXML public void showProjects()     { setActiveButton(btnProjects);     loadContent("Projects.fxml"); }
    @FXML public void showComments()     { setActiveButton(btnComments);     loadContent("Comments.fxml"); }
    @FXML public void showDocuments()    { setActiveButton(btnDocuments);    loadContent("Documents.fxml"); }
    @FXML public void showSprints()      { setActiveButton(btnSprints);      loadContent("Sprints.fxml"); }
    @FXML public void showTasks()        { setActiveButton(btnTasks);        loadContent("Tasks.fxml"); }
    @FXML public void showMeetings()     { setActiveButton(btnMeetings);     loadContent("Meetings.fxml"); }
    @FXML public void showCandidatures() { setActiveButton(btnCandidatures); loadContent("Candidatures.fxml"); }
    @FXML public void showUsers()        { setActiveButton(btnUsers);        loadContent("Users.fxml"); }
    @FXML public void showAuditLog()     { setActiveButton(btnAuditLog);     loadContent("AuditLog.fxml"); }

    private void loadContent(String fxml) {
        // Clear old content completely
        if (contentArea.getCenter() != null) {
            contentArea.getCenter().setVisible(false);
            contentArea.setCenter(null);
        }
        
        // Load new content
        Pane pane = NavigationUtil.loadPane(fxml);
        if (pane != null) {
            contentArea.setCenter(pane);
            pane.setVisible(true);
        }
    }

    @FXML
    public void handleLogout() {
        SessionManager.logout();
        NavigationUtil.navigateTo("Login.fxml");
    }

    // ========== MÉTHODES DE TEST DES APIs ==========

    /**
     * Tester les notifications desktop
     */
    @FXML
    public void testNotification() {
        System.out.println("\n🧪 TEST: Notification Desktop");
        
        try {
            if (!NotificationUtil.isSupported()) {
                showAlert("Notification", "⚠️ Les notifications ne sont pas supportées sur ce système", Alert.AlertType.WARNING);
                return;
            }

            // Test de différents types de notifications
            NotificationUtil.showInfo("Test SmartPFE", "✅ Notification d'information");
            
            Thread.sleep(1000);
            NotificationUtil.showSuccess("Test SmartPFE", "🎉 Notification de succès");
            
            Thread.sleep(1000);
            NotificationUtil.showWarning("Test SmartPFE", "⚠️ Notification d'avertissement");
            
            Thread.sleep(1000);
            NotificationUtil.Notifications.projectCreated("Projet Test");
            
            showAlert("Notification", "✅ Notifications envoyées avec succès!\nVérifiez votre barre de notifications système.", Alert.AlertType.INFORMATION);
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "❌ Erreur lors du test des notifications:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Tester la génération de QR Code
     */
    @FXML
    public void testQRCode() {
        System.out.println("\n🧪 TEST: QR Code");
        
        try {
            // Générer un QR code de test
            String qrPath = QRCodeUtil.generateProjectQRCode(999, "Projet Test SmartPFE");
            
            if (qrPath != null) {
                File qrFile = new File(qrPath);
                if (qrFile.exists()) {
                    // Ouvrir le QR code généré
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(qrFile);
                    }
                    
                    showAlert("QR Code", 
                        "✅ QR Code généré avec succès!\n\n" +
                        "Fichier: " + qrPath + "\n\n" +
                        "Le QR code a été ouvert automatiquement.\n" +
                        "Scannez-le avec votre smartphone!", 
                        Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Erreur", "❌ Le fichier QR code n'a pas été créé", Alert.AlertType.ERROR);
                }
            } else {
                showAlert("Erreur", "❌ Échec de la génération du QR code", Alert.AlertType.ERROR);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "❌ Erreur lors du test du QR Code:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Tester Text-to-Speech
     */
    @FXML
    public void testTTS() {
        System.out.println("\n🧪 TEST: Text-to-Speech");
        
        try {
            if (!TextToSpeechUtil.isAvailable()) {
                showAlert("Text-to-Speech", 
                    "⚠️ Text-to-Speech non disponible sur ce système\n\n" +
                    "Moteur: " + TextToSpeechUtil.getTTSEngine(), 
                    Alert.AlertType.WARNING);
                return;
            }

            showAlert("Text-to-Speech", 
                "🔊 Lecture en cours...\n\n" +
                "Moteur: " + TextToSpeechUtil.getTTSEngine() + "\n\n" +
                "Écoutez le message vocal!", 
                Alert.AlertType.INFORMATION);

            // Lancer la lecture de manière asynchrone
            new Thread(() -> {
                TextToSpeechUtil.speak("Bonjour, bienvenue dans SmartPFE. Ceci est un test de synthèse vocale.");
                
                Platform.runLater(() -> {
                    NotificationUtil.showSuccess("Text-to-Speech", "✅ Lecture terminée");
                });
            }).start();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "❌ Erreur lors du test TTS:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Tester toutes les APIs en une fois
     */
    @FXML
    public void testAllAPIs() {
        System.out.println("\n🚀 TEST COMPLET: Toutes les APIs");
        
        try {
            StringBuilder results = new StringBuilder();
            results.append("=== RÉSULTATS DES TESTS ===\n\n");

            // 1. Test Notifications
            results.append("1️⃣ Notifications Desktop: ");
            if (NotificationUtil.isSupported()) {
                NotificationUtil.showInfo("Test Complet", "🧪 Test de toutes les APIs en cours...");
                results.append("✅ OK\n");
            } else {
                results.append("⚠️ Non supporté\n");
            }

            // 2. Test QR Code
            results.append("2️⃣ QR Code (ZXing): ");
            String qrPath = QRCodeUtil.generateProjectQRCode(888, "Test Complet");
            if (qrPath != null && new File(qrPath).exists()) {
                results.append("✅ OK\n");
            } else {
                results.append("❌ Échec\n");
            }

            // 3. Test Excel Export
            results.append("3️⃣ Excel Export (Apache POI): ");
            try {
                List<Project> projects = projectDAO.findAll();
                if (!projects.isEmpty()) {
                    File excelFile = new File("exports/test_projects.xlsx");
                    excelFile.getParentFile().mkdirs();
                    ExcelExporter.exportProjects(projects, excelFile);
                    results.append("✅ OK\n");
                } else {
                    results.append("⚠️ Pas de données\n");
                }
            } catch (Exception e) {
                results.append("❌ Échec\n");
            }

            // 4. Test Text-to-Speech
            results.append("4️⃣ Text-to-Speech: ");
            if (TextToSpeechUtil.isAvailable()) {
                results.append("✅ OK (Moteur: " + TextToSpeechUtil.getTTSEngine() + ")\n");
            } else {
                results.append("⚠️ Non disponible\n");
            }

            // 5. Test Google Charts
            results.append("5️⃣ Google Charts: ");
            try {
                List<Project> projects = projectDAO.findAll();
                List<Comment> comments = commentDAO.findAll();
                if (!projects.isEmpty() && !comments.isEmpty()) {
                    String chartPath = ChartUtil.generateDashboard(projects, comments);
                    if (chartPath != null && new File(chartPath).exists()) {
                        results.append("✅ OK\n");
                        
                        // Ouvrir le dashboard
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().open(new File(chartPath));
                        }
                    } else {
                        results.append("❌ Échec\n");
                    }
                } else {
                    results.append("⚠️ Pas de données\n");
                }
            } catch (Exception e) {
                results.append("❌ Échec\n");
            }

            results.append("\n📊 Fichiers générés:\n");
            results.append("  • qrcodes/\n");
            results.append("  • exports/\n");
            results.append("  • charts/\n");

            // Afficher les résultats
            showAlert("Test Complet", results.toString(), Alert.AlertType.INFORMATION);

            // Notification finale
            NotificationUtil.showSuccess("Test Complet", "✅ Tous les tests sont terminés!");

            // Lire le résultat
            if (TextToSpeechUtil.isAvailable()) {
                new Thread(() -> {
                    TextToSpeechUtil.speak("Tests terminés. Toutes les APIs ont été testées avec succès.");
                }).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "❌ Erreur lors du test complet:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Afficher une alerte JavaFX
     */
    private void showAlert(String title, String content, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}
