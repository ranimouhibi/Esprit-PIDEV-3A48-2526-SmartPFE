package org.example.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.example.model.Task;

import java.time.format.DateTimeFormatter;

public class TaskDetailsDialog {
    private final Stage dialog;
    private final Task task;

    public TaskDetailsDialog(Window owner, Task task) {
        this.task = task;
        this.dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Task Details");
        dialog.setScene(new Scene(buildContent(), 550, 600));
    }

    private VBox buildContent() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #f9fafb;");

        // Header with title and status
        VBox header = new VBox(12);
        Label titleLabel = new Label(task.getTitle());
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        titleLabel.setTextFill(Color.web("#1a1a2e"));
        titleLabel.setWrapText(true);

        HBox badges = new HBox(10);
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.getChildren().addAll(statusBadge(task.getStatus()), priorityBadge(task.getPriority()));

        header.getChildren().addAll(titleLabel, badges);

        // Details section
        VBox details = new VBox(16);
        details.setPadding(new Insets(20));
        details.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);");

        // Check if current user is a student
        org.example.model.User currentUser = org.example.util.SessionManager.getCurrentUser();
        boolean isStudent = currentUser != null && "student".equals(currentUser.getRole());

        details.getChildren().addAll(
            detailRow("Description", task.getDescription() != null ? task.getDescription() : "No description"),
            new Separator(),
            detailRow("Project", task.getProjectTitle() != null ? task.getProjectTitle() : "N/A"),
            detailRow("Sprint", task.getSprintName() != null ? task.getSprintName() : "Not assigned to sprint"),
            new Separator()
        );

        // Only show "Assigned To" for non-students (supervisors/admins)
        if (!isStudent) {
            details.getChildren().addAll(
                detailRow("Assigned To", task.getAssignedToName() != null ? task.getAssignedToName() : "Unassigned"),
                new Separator()
            );
        }

        details.getChildren().add(
            detailRow("Created", task.getCreatedAt() != null ? 
                task.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) : "N/A")
        );

        // Close button
        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10 24; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());
        closeBtn.setPrefWidth(120);

        HBox buttonBox = new HBox(closeBtn);
        buttonBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(header, details, buttonBox);
        return root;
    }

    private VBox detailRow(String label, String value) {
        VBox row = new VBox(6);
        Label labelText = new Label(label);
        labelText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        labelText.setTextFill(Color.web("#6b7280"));

        Label valueText = new Label(value);
        valueText.setFont(Font.font("Segoe UI", 13));
        valueText.setTextFill(Color.web("#1a1a2e"));
        valueText.setWrapText(true);

        row.getChildren().addAll(labelText, valueText);
        return row;
    }

    private Label statusBadge(String status) {
        String color = "done".equals(status) ? "#10b981" : "in_progress".equals(status) ? "#3b82f6" : "#f59e0b";
        Label badge = new Label(status != null ? status.replace("_", " ").toUpperCase() : "");
        badge.setStyle("-fx-background-color:" + color + "; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 4 10; -fx-font-size: 11px; -fx-font-weight: bold;");
        return badge;
    }

    private Label priorityBadge(String priority) {
        String color = "critical".equals(priority) ? "#dc2626" : "high".equals(priority) ? "#f97316" : "medium".equals(priority) ? "#eab308" : "#6b7280";
        Label badge = new Label(priority != null ? priority.toUpperCase() : "");
        badge.setStyle("-fx-background-color:" + color + "22; -fx-text-fill:" + color + "; -fx-background-radius: 6; -fx-padding: 4 10; -fx-font-size: 11px; -fx-font-weight: bold;");
        return badge;
    }

    public void show() {
        dialog.show();
    }
}
