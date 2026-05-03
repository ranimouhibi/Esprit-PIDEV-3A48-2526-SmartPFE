package org.example.controller;

import org.example.dao.MatchingScoreDAO;
import org.example.model.Candidature;
import org.example.model.MatchingScore;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;

import java.util.List;

public class CandidatureCompareController {

    @FXML private HBox compareContainer;

    private final MatchingScoreDAO matchingScoreDAO = new MatchingScoreDAO();

    public void loadCandidatures(List<Candidature> candidatures) {
        compareContainer.getChildren().clear();

        // Find best score
        double bestScore = candidatures.stream().mapToDouble(c -> {
            try {
                MatchingScore ms = matchingScoreDAO.findByCandidature(c.getId());
                return ms != null ? ms.getScore() : 0;
            } catch (Exception e) { return 0; }
        }).max().orElse(0);

        for (Candidature c : candidatures) {
            MatchingScore ms = null;
            try { ms = matchingScoreDAO.findByCandidature(c.getId()); } catch (Exception e) { e.printStackTrace(); }
            VBox col = buildColumn(c, ms, ms != null && ms.getScore() == bestScore && bestScore > 0);
            HBox.setHgrow(col, Priority.ALWAYS);
            compareContainer.getChildren().add(col);
        }
    }

    private VBox buildColumn(Candidature c, MatchingScore ms, boolean isBest) {
        VBox col = new VBox(10);
        col.setPadding(new Insets(16));
        col.setStyle("-fx-background-color: " + (isBest ? "#f0fff4" : "white") +
            "; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);" +
            (isBest ? " -fx-border-color: #28a745; -fx-border-radius: 12; -fx-border-width: 2;" : ""));

        // Header
        if (isBest) {
            Label best = new Label("⭐ BEST MATCH");
            best.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold; -fx-font-size: 11px;");
            col.getChildren().add(best);
        }

        Label name = new Label(c.getStudentName());
        name.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        Label status = new Label("Status: " + c.getStatus());
        status.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        col.getChildren().addAll(name, status);

        if (ms != null) {
            // Overall score
            Label scoreLabel = new Label(String.format("Overall: %.1f%%", ms.getScore()));
            scoreLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + ms.getMatchLevelColor() + ";");

            Label levelLabel = new Label(ms.getMatchLevel() != null ? ms.getMatchLevel().toUpperCase() : "");
            levelLabel.setStyle("-fx-background-color: " + ms.getMatchLevelColor() +
                "; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 3 10; -fx-font-weight: bold;");

            col.getChildren().addAll(scoreLabel, levelLabel);

            // Score bars
            col.getChildren().add(buildScoreRow("Skills", ms.getSkillsScore()));
            col.getChildren().add(buildScoreRow("Description", ms.getDescriptionScore()));
            col.getChildren().add(buildScoreRow("Keywords", ms.getKeywordsScore()));

            // Matched skills
            if (!ms.getMatchedSkills().isEmpty()) {
                Label ml = new Label("Matched Skills:");
                ml.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
                col.getChildren().add(ml);
                FlowPane fp = new FlowPane(4, 4);
                for (String s : ms.getMatchedSkills()) {
                    Label chip = new Label(s);
                    chip.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 11px;");
                    fp.getChildren().add(chip);
                }
                col.getChildren().add(fp);
            }

            // Missing skills
            if (!ms.getMissingSkills().isEmpty()) {
                Label ml = new Label("Missing Skills:");
                ml.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
                col.getChildren().add(ml);
                FlowPane fp = new FlowPane(4, 4);
                for (String s : ms.getMissingSkills().subList(0, Math.min(5, ms.getMissingSkills().size()))) {
                    Label chip = new Label(s);
                    chip.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 11px;");
                    fp.getChildren().add(chip);
                }
                col.getChildren().add(fp);
            }
        } else {
            Label noScore = new Label("No AI score calculated");
            noScore.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
            col.getChildren().add(noScore);
        }

        return col;
    }

    private VBox buildScoreRow(String label, double value) {
        VBox box = new VBox(2);
        Label lbl = new Label(label + ": " + String.format("%.1f%%", value));
        lbl.setStyle("-fx-font-size: 12px;");
        ProgressBar pb = new ProgressBar(value / 100.0);
        pb.setMaxWidth(Double.MAX_VALUE);
        String color = value >= 80 ? "#28a745" : value >= 60 ? "#ffc107" : value >= 40 ? "#fd7e14" : "#dc3545";
        pb.setStyle("-fx-accent: " + color + ";");
        box.getChildren().addAll(lbl, pb);
        return box;
    }
}
