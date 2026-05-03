package org.example.util;

import org.example.dao.ProjectDAO;
import org.example.model.User;
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

import java.util.*;

public class AIAssignmentDialog {

    public static Optional<User> run(Window owner, int projectId, String taskTitle, String taskDesc) {
        try {
            List<User> members = new ProjectDAO().findProjectMembers(projectId);
            List<User> students = members.stream()
                .filter(u -> "student".equals(u.getRole()))
                .toList();

            if (students.size() == 1) {
                System.out.println("[AI] 1 student — auto-assigning to: " + students.get(0).getName());
                return Optional.of(students.get(0));
            }
            if (students.isEmpty()) return Optional.empty();

            TaskAssignmentAI.AssignmentResult result =
                TaskAssignmentAI.recommend(students, taskTitle, taskDesc);

            if (result == null) {
                System.out.println("[AI] No skills detected — manual assignment");
                return Optional.empty();
            }

            return showRecommendationDialog(owner, result, students);

        } catch (Exception e) {
            System.err.println("[AI] AIAssignmentDialog error: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private static Optional<User> showRecommendationDialog(Window owner,
                                                             TaskAssignmentAI.AssignmentResult result,
                                                             List<User> students) {
        Stage stage = new Stage();
        if (owner != null) stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("AI Task Assignment");
        stage.setResizable(false);

        final User[] chosen   = {result.recommended()};
        final boolean[] entire = {false};

        User other = students.stream()
            .filter(u -> u.getId() != result.recommended().getId())
            .findFirst().orElse(null);

        // ── Root ─────────────────────────────────────────────────────────────
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f4f6fb;");

        // ── Header bar ───────────────────────────────────────────────────────
        HBox headerBar = new HBox(10);
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setPadding(new Insets(18, 24, 18, 24));
        headerBar.setStyle("-fx-background-color: #1a1a2e;");
        Label icon = new Label("🤖");
        icon.setStyle("-fx-font-size: 20px;");
        Label headerLbl = new Label("AI Task Assignment");
        headerLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        headerLbl.setTextFill(Color.WHITE);
        headerBar.getChildren().addAll(icon, headerLbl);

        // ── Body ─────────────────────────────────────────────────────────────
        VBox body = new VBox(14);
        body.setPadding(new Insets(20, 24, 20, 24));

        // Reason box
        Label reasonLbl = new Label("💡 " + result.reason());
        reasonLbl.setWrapText(true);
        reasonLbl.setMaxWidth(Double.MAX_VALUE);
        reasonLbl.setStyle("-fx-background-color: #eef2ff; -fx-background-radius: 8; "
            + "-fx-padding: 10 14; -fx-text-fill: #3730a3; -fx-font-size: 12px;");

        // Student cards
        VBox cardsBox = new VBox(10);
        cardsBox.getChildren().add(studentCard(result.recommended(), true));
        if (other != null) cardsBox.getChildren().add(studentCard(other, false));

        // ── Action buttons (vertical stack, full width) ───────────────────
        VBox btnBox = new VBox(8);
        btnBox.setPadding(new Insets(4, 0, 0, 0));

        Button acceptBtn = makeBtn("✓  Accept — assign to " + result.recommended().getName(),
            "#10b981", true);
        acceptBtn.setOnAction(e -> { chosen[0] = result.recommended(); stage.close(); });

        btnBox.getChildren().add(acceptBtn);

        if (other != null) {
            Button otherBtn = makeBtn("↩  Assign to " + other.getName() + " instead",
                "#667eea", false);
            User finalOther = other;
            otherBtn.setOnAction(e -> { chosen[0] = finalOther; stage.close(); });
            btnBox.getChildren().add(otherBtn);
        }

        Button teamBtn = makeBtn("👥  Assign to Entire Team", "#f59e0b", false);
        teamBtn.setOnAction(e -> { entire[0] = true; chosen[0] = null; stage.close(); });
        btnBox.getChildren().add(teamBtn);

        body.getChildren().addAll(reasonLbl, cardsBox, btnBox);

        root.getChildren().addAll(headerBar, body);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        stage.setScene(new Scene(scroll, 460, 420));
        stage.showAndWait();

        if (entire[0]) return Optional.empty();
        return Optional.ofNullable(chosen[0]);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static HBox studentCard(User student, boolean recommended) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 14, 12, 14));
        card.setMaxWidth(Double.MAX_VALUE);
        if (recommended) {
            card.setStyle("-fx-background-color: white; -fx-background-radius: 10; "
                + "-fx-border-color: #10b981; -fx-border-width: 2; -fx-border-radius: 10;");
        } else {
            card.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 10; "
                + "-fx-border-color: #e5e7eb; -fx-border-width: 1; -fx-border-radius: 10;");
        }

        // Avatar circle
        Label avatar = new Label(student.getName() != null
            ? String.valueOf(student.getName().charAt(0)).toUpperCase() : "?");
        avatar.setMinSize(38, 38);
        avatar.setMaxSize(38, 38);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-background-color: " + (recommended ? "#10b981" : "#667eea") + "; "
            + "-fx-background-radius: 19; -fx-text-fill: white; "
            + "-fx-font-weight: bold; -fx-font-size: 15px;");

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(student.getName() != null ? student.getName() : student.getEmail());
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        name.setTextFill(Color.web("#1a1a2e"));

        String skillsText = student.getSkills() != null && !student.getSkills().isBlank()
            ? student.getSkills() : "No skills listed";
        Label skills = new Label("🔧 " + skillsText);
        skills.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
        skills.setWrapText(true);

        info.getChildren().addAll(name, skills);

        card.getChildren().addAll(avatar, info);

        if (recommended) {
            Label badge = new Label("⭐ Recommended");
            badge.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; "
                + "-fx-background-radius: 6; -fx-padding: 3 8; -fx-font-size: 10px; -fx-font-weight: bold;");
            card.getChildren().add(badge);
        }

        return card;
    }

    private static Button makeBtn(String text, String color, boolean primary) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; "
            + "-fx-background-radius: 8; -fx-padding: 11 16; "
            + "-fx-font-weight: bold; -fx-font-size: 12px; -fx-cursor: hand;");
        return btn;
    }
}
