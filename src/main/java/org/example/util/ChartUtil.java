package org.example.util;

import org.example.model.Comment;
import org.example.model.Project;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utilitaire pour générer des graphiques avec Google Charts
 * Génère des fichiers HTML avec des graphiques interactifs
 */
public class ChartUtil {

    /**
     * Générer un graphique des commentaires par type
     */
    public static String generateCommentsTypeChart(List<Comment> comments, String title) {
        try {
            // Compter les commentaires par type
            Map<String, Long> typeCount = comments.stream()
                .collect(Collectors.groupingBy(
                    c -> c.getCommentType() != null ? c.getCommentType() : "unknown",
                    Collectors.counting()
                ));

            // Créer le HTML avec Google Charts
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html>\n<head>\n");
            html.append("<script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>\n");
            html.append("<script type=\"text/javascript\">\n");
            html.append("google.charts.load('current', {'packages':['corechart']});\n");
            html.append("google.charts.setOnLoadCallback(drawChart);\n");
            html.append("function drawChart() {\n");
            html.append("  var data = google.visualization.arrayToDataTable([\n");
            html.append("    ['Type', 'Nombre'],\n");
            
            for (Map.Entry<String, Long> entry : typeCount.entrySet()) {
                html.append("    ['").append(capitalize(entry.getKey())).append("', ")
                    .append(entry.getValue()).append("],\n");
            }
            
            html.append("  ]);\n");
            html.append("  var options = {\n");
            html.append("    title: '").append(title).append("',\n");
            html.append("    pieHole: 0.4,\n");
            html.append("    colors: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444'],\n");
            html.append("    chartArea: {width: '90%', height: '80%'},\n");
            html.append("    legend: {position: 'bottom'},\n");
            html.append("    fontSize: 14\n");
            html.append("  };\n");
            html.append("  var chart = new google.visualization.PieChart(document.getElementById('chart_div'));\n");
            html.append("  chart.draw(data, options);\n");
            html.append("}\n");
            html.append("</script>\n");
            html.append("</head>\n<body>\n");
            html.append("<div id=\"chart_div\" style=\"width: 900px; height: 500px;\"></div>\n");
            html.append("</body>\n</html>");

            // Sauvegarder le fichier HTML
            File chartsDir = new File("charts");
            if (!chartsDir.exists()) {
                chartsDir.mkdirs();
            }

            String fileName = "charts/comments_by_type.html";
            try (FileWriter writer = new FileWriter(fileName)) {
                writer.write(html.toString());
            }

            System.out.println("✅ Graphique généré : " + fileName);
            return fileName;

        } catch (Exception e) {
            System.err.println("❌ Erreur génération graphique : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Générer un graphique des commentaires par importance
     */
    public static String generateCommentsImportanceChart(List<Comment> comments, String title) {
        try {
            Map<String, Long> importanceCount = comments.stream()
                .collect(Collectors.groupingBy(
                    c -> c.getImportance() != null ? c.getImportance() : "unknown",
                    Collectors.counting()
                ));

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html>\n<head>\n");
            html.append("<script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>\n");
            html.append("<script type=\"text/javascript\">\n");
            html.append("google.charts.load('current', {'packages':['corechart']});\n");
            html.append("google.charts.setOnLoadCallback(drawChart);\n");
            html.append("function drawChart() {\n");
            html.append("  var data = google.visualization.arrayToDataTable([\n");
            html.append("    ['Importance', 'Nombre'],\n");
            
            for (Map.Entry<String, Long> entry : importanceCount.entrySet()) {
                html.append("    ['").append(capitalize(entry.getKey())).append("', ")
                    .append(entry.getValue()).append("],\n");
            }
            
            html.append("  ]);\n");
            html.append("  var options = {\n");
            html.append("    title: '").append(title).append("',\n");
            html.append("    colors: ['#ef4444', '#f59e0b', '#10b981'],\n");
            html.append("    chartArea: {width: '70%', height: '70%'},\n");
            html.append("    legend: {position: 'right'},\n");
            html.append("    fontSize: 14,\n");
            html.append("    hAxis: {title: 'Nombre de commentaires'},\n");
            html.append("    vAxis: {title: 'Importance'}\n");
            html.append("  };\n");
            html.append("  var chart = new google.visualization.BarChart(document.getElementById('chart_div'));\n");
            html.append("  chart.draw(data, options);\n");
            html.append("}\n");
            html.append("</script>\n");
            html.append("</head>\n<body>\n");
            html.append("<div id=\"chart_div\" style=\"width: 900px; height: 500px;\"></div>\n");
            html.append("</body>\n</html>");

            File chartsDir = new File("charts");
            if (!chartsDir.exists()) {
                chartsDir.mkdirs();
            }

            String fileName = "charts/comments_by_importance.html";
            try (FileWriter writer = new FileWriter(fileName)) {
                writer.write(html.toString());
            }

            System.out.println("✅ Graphique généré : " + fileName);
            return fileName;

        } catch (Exception e) {
            System.err.println("❌ Erreur génération graphique : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Générer un graphique des projets par statut
     */
    public static String generateProjectsStatusChart(List<Project> projects, String title) {
        try {
            Map<String, Long> statusCount = projects.stream()
                .collect(Collectors.groupingBy(
                    p -> p.getStatus() != null ? p.getStatus() : "unknown",
                    Collectors.counting()
                ));

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html>\n<head>\n");
            html.append("<script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>\n");
            html.append("<script type=\"text/javascript\">\n");
            html.append("google.charts.load('current', {'packages':['corechart']});\n");
            html.append("google.charts.setOnLoadCallback(drawChart);\n");
            html.append("function drawChart() {\n");
            html.append("  var data = google.visualization.arrayToDataTable([\n");
            html.append("    ['Statut', 'Nombre'],\n");
            
            for (Map.Entry<String, Long> entry : statusCount.entrySet()) {
                html.append("    ['").append(capitalize(entry.getKey())).append("', ")
                    .append(entry.getValue()).append("],\n");
            }
            
            html.append("  ]);\n");
            html.append("  var options = {\n");
            html.append("    title: '").append(title).append("',\n");
            html.append("    is3D: true,\n");
            html.append("    colors: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'],\n");
            html.append("    chartArea: {width: '90%', height: '80%'},\n");
            html.append("    legend: {position: 'bottom'},\n");
            html.append("    fontSize: 14\n");
            html.append("  };\n");
            html.append("  var chart = new google.visualization.PieChart(document.getElementById('chart_div'));\n");
            html.append("  chart.draw(data, options);\n");
            html.append("}\n");
            html.append("</script>\n");
            html.append("</head>\n<body>\n");
            html.append("<div id=\"chart_div\" style=\"width: 900px; height: 500px;\"></div>\n");
            html.append("</body>\n</html>");

            File chartsDir = new File("charts");
            if (!chartsDir.exists()) {
                chartsDir.mkdirs();
            }

            String fileName = "charts/projects_by_status.html";
            try (FileWriter writer = new FileWriter(fileName)) {
                writer.write(html.toString());
            }

            System.out.println("✅ Graphique généré : " + fileName);
            return fileName;

        } catch (Exception e) {
            System.err.println("❌ Erreur génération graphique : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Générer un dashboard complet avec plusieurs graphiques
     */
    public static String generateDashboard(List<Project> projects, List<Comment> comments) {
        try {
            // Statistiques
            Map<String, Long> projectsByStatus = projects.stream()
                .collect(Collectors.groupingBy(
                    p -> p.getStatus() != null ? p.getStatus() : "unknown",
                    Collectors.counting()
                ));

            Map<String, Long> commentsByType = comments.stream()
                .collect(Collectors.groupingBy(
                    c -> c.getCommentType() != null ? c.getCommentType() : "unknown",
                    Collectors.counting()
                ));

            Map<String, Long> commentsByImportance = comments.stream()
                .collect(Collectors.groupingBy(
                    c -> c.getImportance() != null ? c.getImportance() : "unknown",
                    Collectors.counting()
                ));

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html>\n<head>\n");
            html.append("<meta charset=\"UTF-8\">\n");
            html.append("<title>Dashboard SmartPFE</title>\n");
            html.append("<script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>\n");
            html.append("<style>\n");
            html.append("body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }\n");
            html.append("h1 { color: #333; text-align: center; }\n");
            html.append(".chart-container { background: white; padding: 20px; margin: 20px 0; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
            html.append(".stats { display: flex; justify-content: space-around; margin: 20px 0; }\n");
            html.append(".stat-card { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); text-align: center; min-width: 150px; }\n");
            html.append(".stat-number { font-size: 36px; font-weight: bold; color: #3b82f6; }\n");
            html.append(".stat-label { color: #666; margin-top: 8px; }\n");
            html.append("</style>\n");
            html.append("<script type=\"text/javascript\">\n");
            html.append("google.charts.load('current', {'packages':['corechart']});\n");
            html.append("google.charts.setOnLoadCallback(drawCharts);\n");
            html.append("function drawCharts() {\n");
            html.append("  drawProjectsChart();\n");
            html.append("  drawCommentsTypeChart();\n");
            html.append("  drawCommentsImportanceChart();\n");
            html.append("}\n");

            // Graphique projets
            html.append("function drawProjectsChart() {\n");
            html.append("  var data = google.visualization.arrayToDataTable([\n");
            html.append("    ['Statut', 'Nombre'],\n");
            for (Map.Entry<String, Long> entry : projectsByStatus.entrySet()) {
                html.append("    ['").append(capitalize(entry.getKey())).append("', ")
                    .append(entry.getValue()).append("],\n");
            }
            html.append("  ]);\n");
            html.append("  var options = { title: 'Projets par Statut', is3D: true, colors: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444'] };\n");
            html.append("  var chart = new google.visualization.PieChart(document.getElementById('projects_chart'));\n");
            html.append("  chart.draw(data, options);\n");
            html.append("}\n");

            // Graphique commentaires par type
            html.append("function drawCommentsTypeChart() {\n");
            html.append("  var data = google.visualization.arrayToDataTable([\n");
            html.append("    ['Type', 'Nombre'],\n");
            for (Map.Entry<String, Long> entry : commentsByType.entrySet()) {
                html.append("    ['").append(capitalize(entry.getKey())).append("', ")
                    .append(entry.getValue()).append("],\n");
            }
            html.append("  ]);\n");
            html.append("  var options = { title: 'Commentaires par Type', pieHole: 0.4, colors: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444'] };\n");
            html.append("  var chart = new google.visualization.PieChart(document.getElementById('comments_type_chart'));\n");
            html.append("  chart.draw(data, options);\n");
            html.append("}\n");

            // Graphique commentaires par importance
            html.append("function drawCommentsImportanceChart() {\n");
            html.append("  var data = google.visualization.arrayToDataTable([\n");
            html.append("    ['Importance', 'Nombre'],\n");
            for (Map.Entry<String, Long> entry : commentsByImportance.entrySet()) {
                html.append("    ['").append(capitalize(entry.getKey())).append("', ")
                    .append(entry.getValue()).append("],\n");
            }
            html.append("  ]);\n");
            html.append("  var options = { title: 'Commentaires par Importance', colors: ['#ef4444', '#f59e0b', '#10b981'], hAxis: {title: 'Nombre'}, vAxis: {title: 'Importance'} };\n");
            html.append("  var chart = new google.visualization.BarChart(document.getElementById('comments_importance_chart'));\n");
            html.append("  chart.draw(data, options);\n");
            html.append("}\n");

            html.append("</script>\n");
            html.append("</head>\n<body>\n");
            html.append("<h1>📊 Dashboard SmartPFE</h1>\n");

            // Cartes de statistiques
            html.append("<div class=\"stats\">\n");
            html.append("<div class=\"stat-card\"><div class=\"stat-number\">").append(projects.size())
                .append("</div><div class=\"stat-label\">Projets</div></div>\n");
            html.append("<div class=\"stat-card\"><div class=\"stat-number\">").append(comments.size())
                .append("</div><div class=\"stat-label\">Commentaires</div></div>\n");
            html.append("</div>\n");

            // Graphiques
            html.append("<div class=\"chart-container\"><div id=\"projects_chart\" style=\"width: 100%; height: 400px;\"></div></div>\n");
            html.append("<div class=\"chart-container\"><div id=\"comments_type_chart\" style=\"width: 100%; height: 400px;\"></div></div>\n");
            html.append("<div class=\"chart-container\"><div id=\"comments_importance_chart\" style=\"width: 100%; height: 400px;\"></div></div>\n");

            html.append("</body>\n</html>");

            File chartsDir = new File("charts");
            if (!chartsDir.exists()) {
                chartsDir.mkdirs();
            }

            String fileName = "charts/dashboard.html";
            try (FileWriter writer = new FileWriter(fileName)) {
                writer.write(html.toString());
            }

            System.out.println("✅ Dashboard généré : " + fileName);
            return fileName;

        } catch (Exception e) {
            System.err.println("❌ Erreur génération dashboard : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Capitaliser la première lettre
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
