package org.example.util;

import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import org.example.model.Comment;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dialog to display statistics charts in JavaFX
 * Everything stays in the app instead of opening Chrome
 */
public class ChartDialog {

    /**
     * Display comment statistics in a JavaFX Dialog
     */
    public static void showCommentStatistics(List<Comment> comments, String projectTitle) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("📊 Statistics - " + projectTitle);

        // Main container
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f5f5f5;");

        // Title
        Label titleLabel = new Label("📊 Comment Statistics");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #333;");

        // Subtitle
        Label subtitleLabel = new Label(projectTitle);
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        // Container for charts
        HBox chartsContainer = new HBox(20);
        chartsContainer.setAlignment(Pos.CENTER);

        // Chart 1: Comments by Type (PieChart)
        PieChart typeChart = createCommentsByTypeChart(comments);
        VBox typeChartBox = createChartBox(typeChart, "Comments by Type");

        // Chart 2: Comments by Importance (BarChart)
        BarChart<String, Number> importanceChart = createCommentsByImportanceChart(comments);
        VBox importanceChartBox = createChartBox(importanceChart, "Comments by Importance");

        chartsContainer.getChildren().addAll(typeChartBox, importanceChartBox);

        // Text statistics
        VBox statsBox = createStatsBox(comments);

        // Close button
        Button closeButton = new Button("Close");
        closeButton.setStyle(
            "-fx-background-color: #4a5568; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-padding: 12 24; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 14px;"
        );
        closeButton.setOnAction(e -> dialog.close());

        HBox buttonBox = new HBox(closeButton);
        buttonBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(titleLabel, subtitleLabel, chartsContainer, statsBox, buttonBox);

        Scene scene = new Scene(root, 1000, 700);
        dialog.setScene(scene);
        dialog.show();
    }

    /**
     * Create a PieChart for comments by type
     */
    private static PieChart createCommentsByTypeChart(List<Comment> comments) {
        PieChart chart = new PieChart();
        chart.setTitle("");
        chart.setLegendVisible(true);

        // Count comments by type
        Map<String, Long> countByType = comments.stream()
            .collect(Collectors.groupingBy(
                c -> c.getCommentType() != null ? c.getCommentType() : "Other",
                Collectors.counting()
            ));

        // Add data to chart
        countByType.forEach((type, count) -> {
            PieChart.Data data = new PieChart.Data(
                capitalize(type) + " (" + count + ")", 
                count
            );
            chart.getData().add(data);
        });

        // Style
        chart.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        chart.setPrefSize(450, 400);

        return chart;
    }

    /**
     * Create a BarChart for comments by importance
     */
    private static BarChart<String, Number> createCommentsByImportanceChart(List<Comment> comments) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Importance");
        
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Count");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("");
        chart.setLegendVisible(false);

        // Count comments by importance
        Map<String, Long> countByImportance = comments.stream()
            .collect(Collectors.groupingBy(
                c -> c.getImportance() != null ? c.getImportance() : "Undefined",
                Collectors.counting()
            ));

        // Create data series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Comments");

        countByImportance.forEach((importance, count) -> {
            series.getData().add(new XYChart.Data<>(capitalize(importance), count));
        });

        chart.getData().add(series);

        // Style
        chart.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        chart.setPrefSize(450, 400);

        return chart;
    }

    /**
     * Create a box for a chart with title
     */
    private static VBox createChartBox(javafx.scene.Node chart, String title) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 15;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");

        box.getChildren().addAll(titleLabel, chart);
        return box;
    }

    /**
     * Create a box with text statistics
     */
    private static VBox createStatsBox(List<Comment> comments) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setStyle(
            "-fx-background-color: white; -fx-background-radius: 10; " +
            "-fx-padding: 15; -fx-border-color: #e0e0e0; -fx-border-radius: 10; -fx-border-width: 1;"
        );

        Label titleLabel = new Label("📈 Summary");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");

        HBox statsRow = new HBox(40);
        statsRow.setAlignment(Pos.CENTER);

        // Total
        VBox totalBox = createStatItem("Total", String.valueOf(comments.size()), "#5b7fc4");

        // By type
        long corrections = comments.stream().filter(c -> "correction".equalsIgnoreCase(c.getCommentType())).count();
        long suggestions = comments.stream().filter(c -> "suggestion".equalsIgnoreCase(c.getCommentType())).count();
        long validations = comments.stream().filter(c -> "validation".equalsIgnoreCase(c.getCommentType())).count();
        long questions = comments.stream().filter(c -> "question".equalsIgnoreCase(c.getCommentType())).count();

        VBox correctionsBox = createStatItem("Corrections", String.valueOf(corrections), "#ef4444");
        VBox suggestionsBox = createStatItem("Suggestions", String.valueOf(suggestions), "#10b981");
        VBox validationsBox = createStatItem("Validations", String.valueOf(validations), "#f59e0b");
        VBox questionsBox = createStatItem("Questions", String.valueOf(questions), "#8b5cf6");

        statsRow.getChildren().addAll(totalBox, correctionsBox, suggestionsBox, validationsBox, questionsBox);

        box.getChildren().addAll(titleLabel, statsRow);
        return box;
    }

    /**
     * Create a statistic item
     */
    private static VBox createStatItem(String label, String value, String color) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);

        Label valueLabel = new Label(value);
        valueLabel.setStyle(
            "-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + color + ";"
        );

        Label labelLabel = new Label(label);
        labelLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        box.getChildren().addAll(valueLabel, labelLabel);
        return box;
    }

    /**
     * Capitalize first letter
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
