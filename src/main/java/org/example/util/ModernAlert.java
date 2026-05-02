package org.example.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Modern styled alert dialogs replacing the default JavaFX Alert.
 */
public class ModernAlert {

    public enum Type { INFO, SUCCESS, WARNING, ERROR, CONFIRM }

    private static final String PRIMARY = "#a12c2f";
    private static final String SUCCESS_COLOR = "#28a745";
    private static final String WARNING_COLOR = "#ffc107";
    private static final String ERROR_COLOR   = "#dc3545";
    private static final String INFO_COLOR    = "#667eea";

    /** Show a simple message dialog (OK button only). */
    public static void show(Type type, String title, String message) {
        Stage stage = buildStage();
        VBox root = buildRoot(type, title, message);

        Button ok = buildButton("OK", accentColor(type), "white");
        ok.setOnAction(e -> stage.close());
        ok.setPrefWidth(100);

        HBox buttons = new HBox(ok);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(0, 24, 20, 24));

        root.getChildren().add(buttons);
        stage.setScene(new Scene(root));
        stage.showAndWait();
    }

    /** Show a confirmation dialog. Returns true if user clicked Confirm/Yes. */
    public static boolean confirm(String title, String message) {
        Stage stage = buildStage();
        VBox root = buildRoot(Type.CONFIRM, title, message);
        AtomicBoolean result = new AtomicBoolean(false);

        Button cancel = buildButton("Cancel", "#f0f0f0", "#333");
        cancel.setOnAction(e -> stage.close());

        Button confirm = buildButton("Confirm", PRIMARY, "white");
        confirm.setOnAction(e -> { result.set(true); stage.close(); });

        HBox buttons = new HBox(12, cancel, confirm);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(0, 24, 20, 24));

        root.getChildren().add(buttons);
        stage.setScene(new Scene(root));
        stage.showAndWait();
        return result.get();
    }

    /** Show a delete confirmation dialog. Returns true if user confirmed. */
    public static boolean confirmDelete(String itemName) {
        Stage stage = buildStage();
        VBox root = buildRoot(Type.ERROR, "Delete Confirmation",
            "Are you sure you want to delete\n\"" + itemName + "\"?\n\nThis action cannot be undone.");
        AtomicBoolean result = new AtomicBoolean(false);

        Button cancel = buildButton("Cancel", "#f0f0f0", "#333");
        cancel.setOnAction(e -> stage.close());

        Button delete = buildButton("Delete", ERROR_COLOR, "white");
        delete.setOnAction(e -> { result.set(true); stage.close(); });

        HBox buttons = new HBox(12, cancel, delete);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(0, 24, 20, 24));

        root.getChildren().add(buttons);
        stage.setScene(new Scene(root));
        stage.showAndWait();
        return result.get();
    }

    // ── Builders ──────────────────────────────────────────────────────────────

    private static Stage buildStage() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setResizable(false);
        return stage;
    }

    private static VBox buildRoot(Type type, String title, String message) {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: white; -fx-background-radius: 14; " +
            "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.25),20,0,0,6);");
        root.setPrefWidth(400);

        // Colored top bar
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(16, 20, 16, 20));
        topBar.setStyle("-fx-background-color: " + accentColor(type) +
            "; -fx-background-radius: 14 14 0 0;");

        Label icon = new Label(icon(type));
        icon.setStyle("-fx-font-size: 20px;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");

        topBar.getChildren().addAll(icon, titleLbl);

        // Message
        Label msgLbl = new Label(message);
        msgLbl.setWrapText(true);
        msgLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #444;");
        msgLbl.setPadding(new Insets(20, 24, 16, 24));
        msgLbl.setMaxWidth(360);

        root.getChildren().addAll(topBar, msgLbl);
        return root;
    }

    private static Button buildButton(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg +
            "; -fx-background-radius: 8; -fx-padding: 9 22; -fx-font-size: 13px; " +
            "-fx-cursor: hand; -fx-font-weight: bold;");
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle() + "-fx-opacity: 0.88;"));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle().replace("-fx-opacity: 0.88;", "")));
        return btn;
    }

    private static String accentColor(Type type) {
        return switch (type) {
            case SUCCESS -> SUCCESS_COLOR;
            case WARNING -> WARNING_COLOR;
            case ERROR   -> ERROR_COLOR;
            case CONFIRM -> PRIMARY;
            default      -> INFO_COLOR;
        };
    }

    private static String icon(Type type) {
        return switch (type) {
            case SUCCESS -> "✅";
            case WARNING -> "⚠️";
            case ERROR   -> "🗑";
            case CONFIRM -> "❓";
            default      -> "ℹ️";
        };
    }
}
