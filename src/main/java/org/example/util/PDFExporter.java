package org.example.util;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.example.model.Comment;
import org.example.model.Project;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PDFExporter {

    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(161, 44, 47); // #a12c2f
    private static final DeviceRgb HEADER_BG = new DeviceRgb(45, 45, 45); // #2d2d2d
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static void exportProjects(List<Project> projects, File file) throws Exception {
        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Logo/Brand
        Paragraph brand = new Paragraph("SMART-PFE")
                .setFontSize(14)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginBottom(5);
        document.add(brand);

        // Title
        Paragraph title = new Paragraph("Projects Report")
                .setFontSize(24)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);

        // Date
        Paragraph date = new Paragraph("Generated: " + LocalDateTime.now().format(DATE_FORMATTER))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(date);

        // Stats
        long individual = projects.stream().filter(p -> "individual".equals(p.getProjectType())).count();
        long team = projects.stream().filter(p -> "team".equals(p.getProjectType())).count();
        long inProgress = projects.stream().filter(p -> "in_progress".equals(p.getStatus())).count();
        long finished = projects.stream().filter(p -> "finished".equals(p.getStatus())).count();

        Paragraph stats = new Paragraph()
                .add("Total Projects: " + projects.size() + " | ")
                .add("Individual: " + individual + " | ")
                .add("Team: " + team + " | ")
                .add("In Progress: " + inProgress + " | ")
                .add("Finished: " + finished)
                .setFontSize(11)
                .setMarginBottom(15);
        document.add(stats);

        // Table
        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 2, 2}));
        table.setWidth(UnitValue.createPercentValue(100));

        // Header
        addTableHeader(table, new String[]{"Title", "Type", "Status", "Owner", "Supervisor"});

        // Data
        for (Project p : projects) {
            table.addCell(createCell(p.getTitle()));
            table.addCell(createCell(p.getProjectType() != null ? p.getProjectType() : "N/A"));
            table.addCell(createCell(p.getStatus() != null ? p.getStatus() : "N/A"));
            table.addCell(createCell(p.getOwnerName() != null ? p.getOwnerName() : "N/A"));
            table.addCell(createCell(p.getSupervisorName() != null ? p.getSupervisorName() : "No supervisor"));
        }

        document.add(table);
        document.close();
    }

    public static void exportComments(List<Comment> comments, File file) throws Exception {
        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Logo/Brand
        Paragraph brand = new Paragraph("SMART-PFE")
                .setFontSize(14)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginBottom(5);
        document.add(brand);

        // Title
        Paragraph title = new Paragraph("Comments Report")
                .setFontSize(24)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);

        // Date
        Paragraph date = new Paragraph("Generated: " + LocalDateTime.now().format(DATE_FORMATTER))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(date);

        // Stats
        long urgent = comments.stream().filter(c -> "urgent".equals(c.getImportance())).count();
        long medium = comments.stream().filter(c -> "medium".equals(c.getImportance())).count();
        long low = comments.stream().filter(c -> "low".equals(c.getImportance())).count();
        long corrections = comments.stream().filter(c -> "correction".equals(c.getCommentType())).count();

        Paragraph stats = new Paragraph()
                .add("Total Comments: " + comments.size() + " | ")
                .add("Urgent: " + urgent + " | ")
                .add("Medium: " + medium + " | ")
                .add("Low: " + low + " | ")
                .add("Corrections: " + corrections)
                .setFontSize(11)
                .setMarginBottom(15);
        document.add(stats);

        // Table
        Table table = new Table(UnitValue.createPercentArray(new float[]{2, 3, 1, 1, 1, 2}));
        table.setWidth(UnitValue.createPercentValue(100));

        // Header
        addTableHeader(table, new String[]{"Subject", "Content", "Type", "Target", "Importance", "Author"});

        // Data
        for (Comment c : comments) {
            table.addCell(createCell(c.getSubject() != null ? c.getSubject() : ""));
            table.addCell(createCell(c.getContent() != null ? truncate(c.getContent(), 100) : ""));
            table.addCell(createCell(c.getCommentType() != null ? c.getCommentType() : ""));
            table.addCell(createCell(c.getTarget() != null ? c.getTarget() : ""));
            table.addCell(createCell(c.getImportance() != null ? c.getImportance() : ""));
            table.addCell(createCell(c.getAuthorName() != null ? c.getAuthorName() : "Unknown"));
        }

        document.add(table);
        document.close();
    }

    public static void exportDocuments(List<org.example.model.Document> documents, File file) throws Exception {
        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Logo/Brand
        Paragraph brand = new Paragraph("SMART-PFE")
                .setFontSize(14)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginBottom(5);
        document.add(brand);

        // Title
        Paragraph title = new Paragraph("Documents Report")
                .setFontSize(24)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);

        // Date
        Paragraph date = new Paragraph("Generated: " + LocalDateTime.now().format(DATE_FORMATTER))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(date);

        // Stats
        long proposals = documents.stream().filter(d -> "proposal".equals(d.getCategory())).count();
        long reports = documents.stream().filter(d -> "report".equals(d.getCategory())).count();
        long presentations = documents.stream().filter(d -> "presentation".equals(d.getCategory())).count();
        long code = documents.stream().filter(d -> "code".equals(d.getCategory())).count();

        Paragraph stats = new Paragraph()
                .add("Total Documents: " + documents.size() + " | ")
                .add("Proposals: " + proposals + " | ")
                .add("Reports: " + reports + " | ")
                .add("Presentations: " + presentations + " | ")
                .add("Code: " + code)
                .setFontSize(11)
                .setMarginBottom(15);
        document.add(stats);

        // Table
        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1, 2, 2, 2}));
        table.setWidth(UnitValue.createPercentValue(100));

        // Header
        addTableHeader(table, new String[]{"Filename", "Category", "Project", "Uploader", "Date"});

        // Data
        for (org.example.model.Document d : documents) {
            table.addCell(createCell(d.getFilename()));
            table.addCell(createCell(d.getCategory() != null ? d.getCategory() : "N/A"));
            table.addCell(createCell(d.getProjectTitle() != null ? d.getProjectTitle() : "N/A"));
            table.addCell(createCell(d.getUploaderName() != null ? d.getUploaderName() : "Unknown"));
            table.addCell(createCell(d.getUploadedAt() != null ? d.getUploadedAt().format(DATE_FORMATTER) : ""));
        }

        document.add(table);
        document.close();
    }

    private static void addTableHeader(Table table, String[] headers) {
        for (String header : headers) {
            Cell cell = new Cell()
                    .add(new Paragraph(header).setBold().setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(HEADER_BG)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(8);
            table.addHeaderCell(cell);
        }
    }

    private static Cell createCell(String content) {
        return new Cell()
                .add(new Paragraph(content != null ? content : ""))
                .setPadding(6)
                .setFontSize(9);
    }

    private static String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }
}
