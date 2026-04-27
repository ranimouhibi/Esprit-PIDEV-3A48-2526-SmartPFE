package org.example.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import org.example.model.Sprint;
import org.example.model.Task;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PDFExporter {

    private static final String PDFSHIFT_API_KEY = "sk_7dff66689dc5e3328bf1754a070214c575553082";
    private static final String PDFSHIFT_URL     = "https://api.pdfshift.io/v3/convert/pdf";

    private static final BaseColor PRIMARY   = new BaseColor(161, 44, 47);
    private static final BaseColor LIGHT_BG  = new BaseColor(240, 242, 245);
    private static final BaseColor HEADER_BG = new BaseColor(26, 26, 46);
    private static final BaseColor DONE_CLR  = new BaseColor(16, 185, 129);
    private static final BaseColor PROG_CLR  = new BaseColor(59, 130, 246);
    private static final BaseColor TODO_CLR  = new BaseColor(245, 158, 11);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Sprint report (used by SprintStatsController) ─────────────────────────

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
        int pct = tasks.isEmpty() ? 0 : (int)(done * 100 / tasks.size());
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

    // ── exportProjects ────────────────────────────────────────────────────────

    public static void exportProjects(List<org.example.model.Project> projects, File file) throws Exception {
        Document doc = new Document(PageSize.A4, 36, 36, 54, 36);
        PdfWriter.getInstance(doc, new FileOutputStream(file));
        doc.open();
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, PRIMARY);
        Font hdrFont     = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD, BaseColor.WHITE);
        Font cellFont    = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, BaseColor.DARK_GRAY);
        doc.add(new Paragraph("SmartPFE — Projects Report",
            new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.DARK_GRAY)));
        doc.add(new Paragraph("Generated on " + LocalDate.now().format(FMT),
            new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, BaseColor.GRAY)));
        doc.add(Chunk.NEWLINE);
        doc.add(new Paragraph("Total: " + projects.size() + " project(s)", sectionFont));
        doc.add(new LineSeparator(1, 100, PRIMARY, Element.ALIGN_LEFT, -2));
        doc.add(Chunk.NEWLINE);
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100); table.setWidths(new float[]{3, 1.5f, 2, 2});
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
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, PRIMARY);
        Font hdrFont     = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD, BaseColor.WHITE);
        Font cellFont    = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, BaseColor.DARK_GRAY);
        doc.add(new Paragraph("SmartPFE — Comments Report",
            new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.DARK_GRAY)));
        doc.add(new Paragraph("Generated on " + LocalDate.now().format(FMT),
            new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, BaseColor.GRAY)));
        doc.add(Chunk.NEWLINE);
        doc.add(new Paragraph("Total: " + comments.size() + " comment(s)", sectionFont));
        doc.add(new LineSeparator(1, 100, PRIMARY, Element.ALIGN_LEFT, -2));
        doc.add(Chunk.NEWLINE);
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100); table.setWidths(new float[]{2, 3, 1.5f, 1.5f});
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
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, PRIMARY);
        Font hdrFont     = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD, BaseColor.WHITE);
        Font cellFont    = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, BaseColor.DARK_GRAY);
        doc.add(new Paragraph("SmartPFE — Documents Report",
            new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.DARK_GRAY)));
        doc.add(new Paragraph("Generated on " + LocalDate.now().format(FMT),
            new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, BaseColor.GRAY)));
        doc.add(Chunk.NEWLINE);
        doc.add(new Paragraph("Total: " + documents.size() + " document(s)", sectionFont));
        doc.add(new LineSeparator(1, 100, PRIMARY, Element.ALIGN_LEFT, -2));
        doc.add(Chunk.NEWLINE);
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100); table.setWidths(new float[]{3, 1.5f, 1.5f, 2});
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

    // ── exportFullReport — PDFShift API only ──────────────────────────────────

    public static void exportFullReport(
            String reportTitle,
            String userName,
            java.util.List<org.example.model.Project> projects,
            java.util.function.Function<Integer, java.util.List<Sprint>> sprintLoader,
            java.util.function.Function<Integer, java.util.List<Task>> taskLoader,
            File file) throws Exception {

        String html = buildHtmlReport(reportTitle, userName, projects, sprintLoader, taskLoader);
        byte[] pdfBytes = callPdfShiftApi(html);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(pdfBytes);
        }
    }

    // ── PDFShift HTTP call ────────────────────────────────────────────────────

    private static byte[] callPdfShiftApi(String html) throws Exception {
        String escapedHtml = html
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r\n", "\\n")
            .replace("\n", "\\n")
            .replace("\r", "\\n");

        String jsonBody = "{\"source\":\"" + escapedHtml + "\","
            + "\"landscape\":false,\"use_print\":false,\"margin\":\"20px\"}";

        URL url = new URL(PDFSHIFT_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("X-API-Key", PDFSHIFT_API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(60_000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        if (code != 200) {
            InputStream err = conn.getErrorStream();
            String msg = err != null ? new String(err.readAllBytes(), StandardCharsets.UTF_8) : "HTTP " + code;
            throw new RuntimeException("PDFShift API error " + code + ": " + msg);
        }

        try (InputStream is = conn.getInputStream()) {
            return is.readAllBytes();
        }
    }

    // ── HTML builder ─────────────────────────────────────────────────────────

    private static String buildHtmlReport(
            String reportTitle,
            String userName,
            java.util.List<org.example.model.Project> projects,
            java.util.function.Function<Integer, java.util.List<Sprint>> sprintLoader,
            java.util.function.Function<Integer, java.util.List<Task>> taskLoader) {

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><style>")
          .append("body{font-family:Segoe UI,Arial,sans-serif;margin:0;padding:24px;background:#f4f6fb;color:#1a1a2e;}")
          .append("h1{background:#1a1a2e;color:white;padding:18px 24px;border-radius:10px;font-size:22px;margin-bottom:4px;}")
          .append(".meta{color:#6b7280;font-size:12px;margin-bottom:20px;}")
          .append(".summary{display:flex;gap:12px;margin-bottom:24px;}")
          .append(".kpi{background:white;border-radius:10px;padding:14px 20px;flex:1;box-shadow:0 2px 8px rgba(0,0,0,.07);}")
          .append(".kpi .val{font-size:28px;font-weight:bold;}")
          .append(".kpi .lbl{font-size:11px;color:#6b7280;}")
          .append(".project{background:#1a1a2e;color:white;padding:10px 16px;border-radius:8px;margin:20px 0 6px;font-size:15px;font-weight:bold;}")
          .append(".proj-meta{font-size:11px;color:#6b7280;margin-bottom:10px;}")
          .append(".sprint{background:#f0f2f5;border-left:4px solid #a12c2f;padding:8px 14px;border-radius:0 8px 8px 0;margin:10px 0 4px;}")
          .append(".sprint-name{font-weight:bold;font-size:13px;}")
          .append(".badge{display:inline-block;padding:2px 10px;border-radius:5px;font-size:10px;font-weight:bold;color:white;margin-left:8px;}")
          .append(".active{background:#3b82f6;}.closed{background:#6b7280;}.planned{background:#f59e0b;}")
          .append(".sprint-meta{font-size:11px;color:#6b7280;margin:2px 0 8px;}")
          .append("table{width:100%;border-collapse:collapse;margin-bottom:12px;font-size:12px;}")
          .append("th{background:#1a1a2e;color:white;padding:7px 10px;text-align:left;}")
          .append("tr:nth-child(even){background:#f8f9fb;}td{padding:6px 10px;border-bottom:1px solid #e5e7eb;}")
          .append(".done{color:#10b981;font-weight:bold;}.in_progress{color:#3b82f6;font-weight:bold;}.todo{color:#f59e0b;font-weight:bold;}")
          .append(".critical{color:#dc2626;}.high{color:#f97316;}.medium{color:#eab308;}.low{color:#6b7280;}")
          .append(".no-data{color:#9ca3af;font-style:italic;font-size:12px;padding:6px 0;}")
          .append("</style></head><body>");

        sb.append("<h1>").append(esc(reportTitle)).append("</h1>");
        sb.append("<div class='meta'>Generated on ").append(LocalDate.now().format(FMT))
          .append(" &nbsp;|&nbsp; ").append(esc(userName)).append("</div>");

        int totalSprints = 0, totalTasks = 0;
        for (org.example.model.Project p : projects) {
            java.util.List<Sprint> sp = sprintLoader.apply(p.getId());
            totalSprints += sp.size();
            for (Sprint s : sp) totalTasks += taskLoader.apply(s.getId()).size();
        }
        sb.append("<div class='summary'>")
          .append(kpiHtml(String.valueOf(projects.size()), "Projects",    "#a12c2f"))
          .append(kpiHtml(String.valueOf(totalSprints),    "Sprints",     "#3b82f6"))
          .append(kpiHtml(String.valueOf(totalTasks),      "Total Tasks", "#10b981"))
          .append("</div>");

        for (org.example.model.Project project : projects) {
            sb.append("<div class='project'>&#128193; ").append(esc(project.getTitle())).append("</div>");
            sb.append("<div class='proj-meta'>")
              .append("Type: ").append(nvl(project.getProjectType()))
              .append(" &nbsp;|&nbsp; Status: ").append(nvl(project.getStatus()))
              .append(" &nbsp;|&nbsp; Owner: ").append(nvl(project.getOwnerName()))
              .append(" &nbsp;|&nbsp; Supervisor: ").append(nvl(project.getSupervisorName()))
              .append("</div>");

            java.util.List<Sprint> sprints = sprintLoader.apply(project.getId());
            if (sprints.isEmpty()) {
                sb.append("<div class='no-data'>No sprints for this project.</div>");
                continue;
            }
            for (Sprint sprint : sprints) {
                java.util.List<Task> tasks = taskLoader.apply(sprint.getId());
                long done = tasks.stream().filter(t -> "done".equals(t.getStatus())).count();
                int pct = tasks.isEmpty() ? 0 : (int)(done * 100 / tasks.size());
                String statusCss = sprint.getStatus() != null ? sprint.getStatus() : "planned";

                sb.append("<div class='sprint'>")
                  .append("<span class='sprint-name'>").append(esc(sprint.getName())).append("</span>")
                  .append("<span class='badge ").append(statusCss).append("'>").append(statusCss.toUpperCase()).append("</span>")
                  .append("</div>");
                sb.append("<div class='sprint-meta'>")
                  .append(formatDate(sprint.getStartDate())).append(" &rarr; ").append(formatDate(sprint.getEndDate()))
                  .append(" &nbsp;|&nbsp; Progress: ").append(pct).append("%")
                  .append(" &nbsp;|&nbsp; Tasks: ").append(tasks.size())
                  .append("</div>");

                if (tasks.isEmpty()) {
                    sb.append("<div class='no-data'>No tasks in this sprint.</div>");
                    continue;
                }
                sb.append("<table><thead><tr>")
                  .append("<th>Title</th><th>Status</th><th>Priority</th><th>Assigned To</th>")
                  .append("</tr></thead><tbody>");
                for (Task t : tasks) {
                    String sc = t.getStatus()   != null ? t.getStatus()   : "todo";
                    String pc = t.getPriority() != null ? t.getPriority() : "low";
                    sb.append("<tr>")
                      .append("<td>").append(esc(t.getTitle())).append("</td>")
                      .append("<td class='").append(sc).append("'>").append(sc.replace("_", " ").toUpperCase()).append("</td>")
                      .append("<td class='").append(pc).append("'>").append(pc.toUpperCase()).append("</td>")
                      .append("<td>").append(nvl(t.getAssignedToName())).append("</td>")
                      .append("</tr>");
                }
                sb.append("</tbody></table>");
            }
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String kpiHtml(String value, String label, String color) {
        return "<div class='kpi'><div class='val' style='color:" + color + "'>" + value + "</div>"
             + "<div class='lbl'>" + label + "</div></div>";
    }

    private static String esc(String s) {
        if (s == null) return "—";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String formatDate(LocalDate d) {
        return d != null ? d.format(FMT) : "—";
    }

    private static String nvl(String s) { return s != null ? s : "—"; }
}
