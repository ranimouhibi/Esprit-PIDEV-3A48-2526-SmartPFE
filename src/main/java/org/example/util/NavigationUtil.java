package org.example.util;

import org.example.Main;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class NavigationUtil {

    public static void navigateTo(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationUtil.class.getResource("/fxml/" + fxmlFile));
            
            // Garder les dimensions actuelles de la fenêtre
            double width = Main.primaryStage.getWidth();
            double height = Main.primaryStage.getHeight();
            
            // Si la fenêtre n'a pas encore de dimensions, utiliser des valeurs par défaut
            if (width == 0 || height == 0) {
                width = 1200;
                height = 750;
            }
            
            Scene scene = new Scene(loader.load(), width, height);
            scene.getStylesheets().add(NavigationUtil.class.getResource("/css/style.css").toExternalForm());
            Main.primaryStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Pane loadPane(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationUtil.class.getResource("/fxml/" + fxmlFile));
            return loader.load();
        } catch (Exception e) {
            System.err.println("=== FXML LOAD ERROR: " + fxmlFile + " ===");
            System.err.println("ERROR loading FXML: " + fxmlFile + " → " + e.getMessage());
            e.printStackTrace();
            Label err = new Label("Error loading " + fxmlFile + ":\n" + e.getMessage());
            err.setStyle("-fx-text-fill: red; -fx-padding: 20; -fx-font-size: 13px;");
            err.setWrapText(true);
            return new javafx.scene.layout.VBox(err);
        }
    }
}
