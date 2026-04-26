package org.example.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.model.Comment;
import org.example.model.Project;
import org.example.model.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utilitaire pour exporter des données en Excel
 * Utilise Apache POI pour générer des fichiers .xlsx
 */
public class ExcelExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Exporter une liste de projets en Excel
     */
    public static void exportProjects(List<Project> projects, File file) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Projets");

        // Styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);

        // En-têtes
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Titre", "Description", "Type", "Statut", "Encadrant", 
                           "Date création"};
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Données
        int rowNum = 1;
        for (Project project : projects) {
            Row row = sheet.createRow(rowNum++);
            
            row.createCell(0).setCellValue(project.getId());
            row.createCell(1).setCellValue(project.getTitle() != null ? project.getTitle() : "");
            row.createCell(2).setCellValue(project.getDescription() != null ? project.getDescription() : "");
            row.createCell(3).setCellValue(project.getProjectType() != null ? project.getProjectType() : "");
            row.createCell(4).setCellValue(project.getStatus() != null ? project.getStatus() : "");
            row.createCell(5).setCellValue(project.getSupervisorName() != null ? project.getSupervisorName() : "");
            
            Cell dateCell = row.createCell(6);
            if (project.getCreatedAt() != null) {
                dateCell.setCellValue(project.getCreatedAt().format(DATE_FORMATTER));
            }
            dateCell.setCellStyle(dateStyle);
        }

        // Auto-size colonnes
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Sauvegarder
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            workbook.write(outputStream);
        }
        workbook.close();

        System.out.println("✅ Export Excel projets réussi : " + file.getName());
        NotificationUtil.Notifications.exportSuccess(file.getName());
    }

    /**
     * Exporter une liste de commentaires en Excel
     */
    public static void exportComments(List<Comment> comments, File file) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Commentaires");

        // Styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);

        // En-têtes
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Sujet", "Contenu", "Type", "Cible", "Importance", 
                           "Auteur", "Date création"};
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Données
        int rowNum = 1;
        for (Comment comment : comments) {
            Row row = sheet.createRow(rowNum++);
            
            row.createCell(0).setCellValue(comment.getId());
            row.createCell(1).setCellValue(comment.getSubject() != null ? comment.getSubject() : "");
            row.createCell(2).setCellValue(comment.getContent() != null ? comment.getContent() : "");
            row.createCell(3).setCellValue(comment.getCommentType() != null ? comment.getCommentType() : "");
            row.createCell(4).setCellValue(comment.getTarget() != null ? comment.getTarget() : "");
            row.createCell(5).setCellValue(comment.getImportance() != null ? comment.getImportance() : "");
            row.createCell(6).setCellValue(comment.getAuthorName() != null ? comment.getAuthorName() : "");
            
            Cell dateCell = row.createCell(7);
            if (comment.getCreatedAt() != null) {
                dateCell.setCellValue(comment.getCreatedAt().format(DATE_FORMATTER));
            }
            dateCell.setCellStyle(dateStyle);
        }

        // Auto-size colonnes
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Sauvegarder
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            workbook.write(outputStream);
        }
        workbook.close();

        System.out.println("✅ Export Excel commentaires réussi : " + file.getName());
        NotificationUtil.Notifications.exportSuccess(file.getName());
    }

    /**
     * Exporter des statistiques de projet en Excel
     */
    public static void exportProjectStatistics(Project project, List<Comment> comments, 
                                               List<org.example.model.Document> documents, File file) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        
        // Feuille 1 : Informations du projet
        Sheet projectSheet = workbook.createSheet("Projet");
        CellStyle headerStyle = createHeaderStyle(workbook);
        
        int rowNum = 0;
        createStatRow(projectSheet, rowNum++, "Titre", project.getTitle(), headerStyle);
        createStatRow(projectSheet, rowNum++, "Description", project.getDescription(), headerStyle);
        createStatRow(projectSheet, rowNum++, "Type", project.getProjectType(), headerStyle);
        createStatRow(projectSheet, rowNum++, "Statut", project.getStatus(), headerStyle);
        createStatRow(projectSheet, rowNum++, "Encadrant", project.getSupervisorName(), headerStyle);
        
        projectSheet.autoSizeColumn(0);
        projectSheet.autoSizeColumn(1);

        // Feuille 2 : Statistiques
        Sheet statsSheet = workbook.createSheet("Statistiques");
        rowNum = 0;
        
        createStatRow(statsSheet, rowNum++, "Nombre de commentaires", String.valueOf(comments.size()), headerStyle);
        createStatRow(statsSheet, rowNum++, "Nombre de documents", String.valueOf(documents.size()), headerStyle);
        
        // Statistiques par type de commentaire
        long corrections = comments.stream().filter(c -> "correction".equals(c.getCommentType())).count();
        long suggestions = comments.stream().filter(c -> "suggestion".equals(c.getCommentType())).count();
        long validations = comments.stream().filter(c -> "validation".equals(c.getCommentType())).count();
        long questions = comments.stream().filter(c -> "question".equals(c.getCommentType())).count();
        
        rowNum++;
        createStatRow(statsSheet, rowNum++, "Corrections", String.valueOf(corrections), headerStyle);
        createStatRow(statsSheet, rowNum++, "Suggestions", String.valueOf(suggestions), headerStyle);
        createStatRow(statsSheet, rowNum++, "Validations", String.valueOf(validations), headerStyle);
        createStatRow(statsSheet, rowNum++, "Questions", String.valueOf(questions), headerStyle);
        
        // Statistiques par importance
        long urgent = comments.stream().filter(c -> "urgent".equals(c.getImportance())).count();
        long medium = comments.stream().filter(c -> "medium".equals(c.getImportance())).count();
        long low = comments.stream().filter(c -> "low".equals(c.getImportance())).count();
        
        rowNum++;
        createStatRow(statsSheet, rowNum++, "Urgents", String.valueOf(urgent), headerStyle);
        createStatRow(statsSheet, rowNum++, "Moyens", String.valueOf(medium), headerStyle);
        createStatRow(statsSheet, rowNum++, "Faibles", String.valueOf(low), headerStyle);
        
        statsSheet.autoSizeColumn(0);
        statsSheet.autoSizeColumn(1);

        // Feuille 3 : Commentaires
        if (!comments.isEmpty()) {
            Sheet commentsSheet = workbook.createSheet("Commentaires");
            Row headerRow = commentsSheet.createRow(0);
            String[] headers = {"Sujet", "Contenu", "Type", "Importance", "Auteur", "Date"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            int commentRow = 1;
            for (Comment comment : comments) {
                Row row = commentsSheet.createRow(commentRow++);
                row.createCell(0).setCellValue(comment.getSubject() != null ? comment.getSubject() : "");
                row.createCell(1).setCellValue(comment.getContent() != null ? comment.getContent() : "");
                row.createCell(2).setCellValue(comment.getCommentType() != null ? comment.getCommentType() : "");
                row.createCell(3).setCellValue(comment.getImportance() != null ? comment.getImportance() : "");
                row.createCell(4).setCellValue(comment.getAuthorName() != null ? comment.getAuthorName() : "");
                row.createCell(5).setCellValue(comment.getCreatedAt() != null ? comment.getCreatedAt().format(DATE_FORMATTER) : "");
            }
            
            for (int i = 0; i < headers.length; i++) {
                commentsSheet.autoSizeColumn(i);
            }
        }

        // Sauvegarder
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            workbook.write(outputStream);
        }
        workbook.close();

        System.out.println("✅ Export Excel statistiques réussi : " + file.getName());
        NotificationUtil.Notifications.exportSuccess(file.getName());
    }

    /**
     * Créer une ligne de statistique
     */
    private static void createStatRow(Sheet sheet, int rowNum, String label, String value, CellStyle headerStyle) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(headerStyle);
        
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value != null ? value : "");
    }

    /**
     * Créer un style pour les en-têtes
     */
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Créer un style pour les dates
     */
    private static CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
