package org.example.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import org.example.model.Sprint;
import org.example.model.Task;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PDFExporter {

    private static final BaseColor PRIMARY   = new BaseColor(161, 44, 47);
    private static final BaseColor LIGHT_BG  = new BaseColor(240, 242, 245);
    private static final BaseColor HEADER_BG = new BaseColor(26, 26, 46);
    private static final BaseColor DONE_CLR  = new BaseColor(16, 185, 129);
    private static final BaseColor PROG_CLR  = new BaseColor(59, 130, 246);
    private static final BaseColor TODO_CLR  = new BaseColor(245, 158, 11);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static void export(List<Sprint> sprints, List<Task> allTasks, File file) throws Exception {
        Document doc = new Document(PageSize.A4, 36, 36, 54, 36);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(file));
        doc.open();
        addHeader(doc, writer);
        addSummary(doc, sprints, allTasks);
        doc.add(Chunk.NEWLINE);
        for (Sprint sprint : sprints) {
            List<Task> tasks = allTasks.stream()
                .filter(t -> Objects.equals(t.getSprintId(), sprint.getId()))
                .collect(Collectors.toList());
            addSprintSection(doc, sprint, tasks);
        }
        doc.close();
    }

    private static void addHeader(Document doc, PdfWriter writer) throws Exception {
        PdfContentByte cb = writer.getDirectContent();
        cb.setColorFill(HEADER_BG);
        cb.rectangle(36, PageSize.A4.getHeight() - 90, PageSize.A4.getWidth() - 72, 54);
        cb.fill();
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, BaseColor.WHITE);
        Font subFont   = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(170, 170, 200));
        Paragraph title = new Paragraph("SmartPFE — Sprint Report", titleFont);
        title.setSpacingBefore(14); title.setSpacingAfter(2);
        doc.add(title);
        Paragraph sub = new Paragraph("Generated on " + LocalDate.now().format(FMT), subFont);
        sub.setSpacingAfter(16);
        doc.add(sub);
    }

    private static void addSummary(Document doc, List<Sprint> sprints, List<Task> allTasks) throws Exception {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, PRIMARY);
        doc.add(new Paragraph("Global Summary", sectionFont));
        doc.add(new LineSeparator(1, 100, PRIMARY, Element.ALIGN_LEFT, -2));
        doc.add(Chunk.NEWLINE);
        long inProg = allTasks.stream().filter(t -> "in_progress".equals(t.getStatus())).count();
        long done   = allTasks.stream().filter(t -> "done".equals(t.getStatus())).count();
        PdfPTable summary = new PdfPTable(4);
        summary.setWidthPercentage(100); summary.setSpacingAfter(16);
        addSummaryCell(summary, "Sprints",     String.valueOf(sprints.size()),  HEADER_BG);
        addSummaryCell(summary, "Total Tasks", String.valueOf(allTasks.size()), PRIMARY);
        addSummaryCell(summary, "In Progress", String.valueOf(inProg),          PROG_CLR);
        addSummaryCell(summary, "Completed",   String.valueOf(done),            DONE_CLR);
        doc.add(summary);
    }

    private static void addSummaryCell(PdfPTable table, String label, String value, BaseColor bg) {
        Font valFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.WHITE);
        Font lblFont = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, new BaseColor(220, 220, 220));
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg); cell.setPadding(12); cell.setBorder(Rectangle.NO_BORDER);
        Paragraph p = new Paragraph(value, valFont);
        p.add(Chunk.NEWLINE); p.add(new Chunk(label, lblFont));
        cell.addElement(p);
        table.addCell(cell);
    }

    private static void addSprintSection(Document doc, Sprint sprint, List<Task> tasks) throws Exception {
        Font sprintFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, HEADER_BG);
        Font metaFont   = new Font(Font.FontFamily.HELVETICA, 9,  Font.ITALIC, BaseColor.GRAY);
        Font tableHdr   = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD, BaseColor.WHITE);
        Font cellFont   = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, BaseColor.DARK_GRAY);

        long done = tasks.stream().filter(t -> "done".equals(t.getStatus())).count();
        int pct = tasks.isEmpty() ? 0 : (int) (done * 100 / tasks.size());

        PdfPTable titleBar = new PdfPTable(2);
        titleBar.setWidthPercentage(100); titleBar.setWidths(new float[]{3, 1}); titleBar.setSpacingBefore(10);
        PdfPCell nameCell = new PdfPCell(new Phrase(sprint.getName(), sprintFont));
        nameCell.setBackgroundColor(LIGHT_BG); nameCell.setPadding(8);
        nameCell.setBorder(Rectangle.LEFT); nameCell.setBorderColorLeft(PRIMARY); nameCell.setBorderWidthLeft(3);
        BaseColor badgeColor = "active".equals(sprint.getStatus()) ? DONE_CLR :
                               "closed".equals(sprint.getStatus()) ? BaseColor.GRAY : TODO_CLR;
        PdfPCell statusCell = new PdfPCell(new Phrase(sprint.getStatus().toUpperCase(),
            new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE)));
        statusCell.setBackgroundColor(badgeColor); statusCell.setPadding(8);
        statusCell.setHorizontalAlignment(Element.ALIGN_CENTER); statusCell.setBorder(Rectangle.NO_BORDER);
        titleBar.addCell(nameCell); titleBar.addCell(statusCell);
        doc.add(titleBar);

        String dates   = formatDate(sprint.getStartDate()) + " → " + formatDate(sprint.getEndDate());
        String project = sprint.getProjectTitle() != null ? sprint.getProjectTitle() : "";
        doc.add(new Paragraph("Project: " + project + "   |   " + dates + "   |   Progress: " + pct + "%", metaFont));
        doc.add(Chunk.NEWLINE);

        if (tasks.isEmpty()) {
            doc.add(new Paragraph("No tasks in this sprint.", metaFont));
            doc.add(Chunk.NEWLINE);
            return;
        }

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100); table.setWidths(new float[]{3, 1.5f, 1.2f, 2}); table.setSpacingAfter(12);
        for (String h : new String[]{"Title", "Status", "Priority", "Assigned To"}) {
            PdfPCell hCell = new PdfPCell(new Phrase(h, tableHdr));
            hCell.setBackgroundColor(HEADER_BG); hCell.setPadding(6); hCell.setBorder(Rectangle.NO_BORDER);
            table.addCell(hCell);
        }
        boolean alt = false;
        for (Task t : tasks) {
            BaseColor rowBg = alt ? new BaseColor(248, 249, 251) : BaseColor.WHITE;
            addTaskCell(table, t.getTitle(), cellFont, rowBg);
            addStatusCell(table, t.getStatus(), rowBg);
            addTaskCell(table, t.getPriority() != null ? t.getPriority() : "—", cellFont, rowBg);
            addTaskCell(table, t.getAssignedToName() != null ? t.getAssignedToName() : "—", cellFont, rowBg);
            alt = !alt;
        }
        doc.add(table);
    }

    private static void addTaskCell(PdfPTable table, String text, Font font, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setPadding(5); cell.setBackgroundColor(bg); cell.setBorderColor(new BaseColor(229, 231, 235));
        table.addCell(cell);
    }

    private static void addStatusCell(PdfPTable table, String status, BaseColor rowBg) {
        BaseColor color = switch (status != null ? status : "") {
            case "done"        -> DONE_CLR;
            case "in_progress" -> PROG_CLR;
            default            -> TODO_CLR;
        };
        Font f = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, color);
        PdfPCell cell = new PdfPCell(new Phrase(status != null ? status.replace("_", " ") : "—", f));
        cell.setPadding(5); cell.setBackgroundColor(rowBg); cell.setBorderColor(new BaseColor(229, 231, 235));
        table.addCell(cell);
    }

    private static String formatDate(LocalDate d) {
        return d != null ? d.format(FMT) : "—";
    }

    // ── exportProjects ────────────────────────────────────────────────────────

    public static void exportProjects(List<org.example.model.Project> projects, File file) throws Exception {
        Document doc = new Document(PageSize.A4, 36, 36, 54, 36);
        PdfWriter.getInstance(doc, new FileOutputStream(file));
        doc.open();
        Font titleFont   = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.WHITE);
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, PRIMARY);
        Font hdrFont     = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD, BaseColor.WHITE);
        Font cellFont    = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, BaseColor.DARK_GRAY);

        Paragraph title = new Paragraph("SmartPFE — Projects Report", titleFont);
        title.setSpacingBefore(10); title.setSpacingAfter(4);
        doc.add(title);
        doc.add(new Paragraph("Generated on " + LocalDate.now().format(FMT),
            new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, BaseColor.GRAY)));
        doc.add(Chunk.NEWLINE);
        doc.add(new Paragraph("Total: " + projects.size() + " project(s)", sectionFont));
        doc.add(new LineSeparator(1, 100, PRIMARY, Element.ALIGN_LEFT, -2));
        doc.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1.5f, 2, 2});
        for (String h : new String[]{"Title", "Type", "Owner", "Supervisor"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
            c.setBackgroundColor(HEADER_BG); c.setPadding(6); c.setBorder(Rectangle.NO_BORDER);
            table.addCell(c);
        }
        boolean alt = false;
        for (org.example.model.Project p : projects) {
            BaseColor bg = alt ? new BaseColor(248, 249, 251) : BaseColor.WHITE;
            addPlainCell(table, p.getTitle(), cellFont, bg);
            addPlainCell(table, p.getProjectType() != null ? p.getProjectType() : "—", cellFont, bg);
            addPlainCell(table, p.getOwnerName() != null ? p.getOwnerName() : "—", cellFont, bg);
            addPlainCell(table, p.getSupervisorName() != null ? p.getSupervisorName() : "—", cellFont, bg);
            alt = !alt;
        }
        doc.add(table);
        doc.close();
    }

    // ── exportComments ────────────────────────────────────────────────────────

    public static void exportComments(List<org.example.model.Comment> comments, File file) throws Exception {
        Document doc = new Document(PageSize.A4, 36, 36, 54, 36);
        PdfWriter.getInstance(doc, new FileOutputStream(file));
        doc.open();
        Font titleFont   = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.WHITE);
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, PRIMARY);
        Font hdrFont     = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD, BaseColor.WHITE);
        Font cellFont    = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, BaseColor.DARK_GRAY);

        Paragraph title = new Paragraph("SmartPFE — Comments Report", titleFont);
        title.setSpacingBefore(10); title.setSpacingAfter(4);
        doc.add(title);
        doc.add(new Paragraph("Generated on " + LocalDate.now().format(FMT),
            new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, BaseColor.GRAY)));
        doc.add(Chunk.NEWLINE);
        doc.add(new Paragraph("Total: " + comments.size() + " comment(s)", sectionFont));
        doc.add(new LineSeparator(1, 100, PRIMARY, Element.ALIGN_LEFT, -2));
        doc.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2, 3, 1.5f, 1.5f});
        for (String h : new String[]{"Subject", "Content", "Author", "Importance"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
            c.setBackgroundColor(HEADER_BG); c.setPadding(6); c.setBorder(Rectangle.NO_BORDER);
            table.addCell(c);
        }
        boolean alt = false;
        for (org.example.model.Comment cm : comments) {
            BaseColor bg = alt ? new BaseColor(248, 249, 251) : BaseColor.WHITE;
            addPlainCell(table, cm.getSubject() != null ? cm.getSubject() : "—", cellFont, bg);
            addPlainCell(table, cm.getContent() != null ? cm.getContent() : "—", cellFont, bg);
            addPlainCell(table, cm.getAuthorName() != null ? cm.getAuthorName() : "—", cellFont, bg);
            addPlainCell(table, cm.getImportance() != null ? cm.getImportance() : "—", cellFont, bg);
            alt = !alt;
        }
        doc.add(table);
        doc.close();
    }

    // ── exportDocuments ───────────────────────────────────────────────────────

    public static void exportDocuments(List<org.example.model.Document> documents, File file) throws Exception {
        Document doc = new Document(PageSize.A4, 36, 36, 54, 36);
        PdfWriter.getInstance(doc, new FileOutputStream(file));
        doc.open();
        Font titleFont   = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.WHITE);
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, PRIMARY);
        Font hdrFont     = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD, BaseColor.WHITE);
        Font cellFont    = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, BaseColor.DARK_GRAY);

        Paragraph title = new Paragraph("SmartPFE — Documents Report", titleFont);
        title.setSpacingBefore(10); title.setSpacingAfter(4);
        doc.add(title);
        doc.add(new Paragraph("Generated on " + LocalDate.now().format(FMT),
            new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, BaseColor.GRAY)));
        doc.add(Chunk.NEWLINE);
        doc.add(new Paragraph("Total: " + documents.size() + " document(s)", sectionFont));
        doc.add(new LineSeparator(1, 100, PRIMARY, Element.ALIGN_LEFT, -2));
        doc.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1.5f, 1.5f, 2});
        for (String h : new String[]{"Filename", "Type", "Category", "Project"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
            c.setBackgroundColor(HEADER_BG); c.setPadding(6); c.setBorder(Rectangle.NO_BORDER);
            table.addCell(c);
        }
        boolean alt = false;
        for (org.example.model.Document d : documents) {
            BaseColor bg = alt ? new BaseColor(248, 249, 251) : BaseColor.WHITE;
            addPlainCell(table, d.getFilename() != null ? d.getFilename() : "—", cellFont, bg);
            addPlainCell(table, d.getFileType() != null ? d.getFileType() : "—", cellFont, bg);
            addPlainCell(table, d.getCategory() != null ? d.getCategory() : "—", cellFont, bg);
            addPlainCell(table, d.getProjectTitle() != null ? d.getProjectTitle() : "—", cellFont, bg);
            alt = !alt;
        }
        doc.add(table);
        doc.close();
    }

    private static void addPlainCell(PdfPTable table, String text, Font font, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5); cell.setBackgroundColor(bg); cell.setBorderColor(new BaseColor(229, 231, 235));
        table.addCell(cell);
    }
}
