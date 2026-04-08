package org.example.util;

import org.example.Main;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;

public class NavigationUtil {

    public static void navigateTo(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationUtil.class.getResource("/fxml/" + fxmlFile));
            Scene scene = new Scene(loader.load());
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
            e.printStackTrace();
            return new Pane();
        }
    }
}
