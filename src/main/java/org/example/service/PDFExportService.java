package org.example.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.example.model.Candidature;
import org.example.model.CandidatureNote;
import org.example.model.MatchingScore;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PDF export service using iText 7.
 */
public class PDFExportService {

    private static final DeviceRgb PRIMARY   = new DeviceRgb(161, 44, 47);   // #a12c2f
    private static final DeviceRgb DARK      = new DeviceRgb(26, 26, 46);    // #1a1a2e
    private static final DeviceRgb LIGHT_BG  = new DeviceRgb(244, 246, 251); // #f4f6fb
    private static final DeviceRgb GREEN     = new DeviceRgb(40, 167, 69);
    private static final DeviceRgb ORANGE    = new DeviceRgb(255, 193, 7);
    private static final DeviceRgb RED       = new DeviceRgb(220, 53, 69);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Export a full candidature report to PDF.
     * @return the generated File
     */
    public File exportCandidature(Candidature c, MatchingScore ms, List<CandidatureNote> notes,
                                   String destPath) throws Exception {
        File file = new File(destPath);
        try (PdfWriter writer = new PdfWriter(file);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            doc.setMargins(40, 50, 40, 50);

            // ── Header ────────────────────────────────────────────────────────
            Table header = new Table(UnitValue.createPercentArray(new float[]{1})).useAllAvailableWidth();
            Cell headerCell = new Cell()
                .setBackgroundColor(PRIMARY)
                .setPadding(20)
                .add(new Paragraph("SmartPFE")
                    .setFontColor(ColorConstants.WHITE).setBold().setFontSize(22))
                .add(new Paragraph("Candidature Report")
                    .setFontColor(ColorConstants.WHITE).setFontSize(13));
            header.addCell(headerCell.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            doc.add(header);
            doc.add(new Paragraph("\n"));

            // ── Candidature Info ──────────────────────────────────────────────
            doc.add(sectionTitle("Candidature Information"));
            Table info = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth();
            addRow(info, "Student", c.getStudentName());
            addRow(info, "Offer", c.getOfferTitle());
            addRow(info, "Status", c.getStatus() != null ? c.getStatus().toUpperCase() : "-");
            addRow(info, "Applied On", c.getCreatedAt() != null ? c.getCreatedAt().format(FMT) : "-");
            addRow(info, "CV", c.getCvPath() != null ? c.getCvPath() : "-");
            addRow(info, "Portfolio", c.getPortfolioUrl() != null ? c.getPortfolioUrl() : "-");
            doc.add(info);
            doc.add(new Paragraph("\n"));

            // ── Motivation Letter ─────────────────────────────────────────────
            if (c.getMotivationLetter() != null && !c.getMotivationLetter().isBlank()) {
                doc.add(sectionTitle("Motivation Letter"));
                doc.add(new Paragraph(c.getMotivationLetter())
                    .setFontSize(11).setFontColor(new DeviceRgb(60, 60, 60))
                    .setBackgroundColor(LIGHT_BG).setPadding(12));
                doc.add(new Paragraph("\n"));
            }

            // ── AI Matching Score ─────────────────────────────────────────────
            if (ms != null) {
                doc.add(sectionTitle("AI Matching Score"));
                Table scoreTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
                    .useAllAvailableWidth();

                addScoreCell(scoreTable, "Overall Score", ms.getScore(), ms.getMatchLevelColor());
                addScoreCell(scoreTable, "Skills Score", ms.getSkillsScore(), "#667eea");
                addScoreCell(scoreTable, "Description", ms.getDescriptionScore(), "#06D6A0");
                addScoreCell(scoreTable, "Keywords", ms.getKeywordsScore(), "#ffc107");
                doc.add(scoreTable);
                doc.add(new Paragraph("\n"));

                // Match level badge
                DeviceRgb levelColor = levelColor(ms.getMatchLevel());
                doc.add(new Paragraph("Match Level: " + (ms.getMatchLevel() != null ? ms.getMatchLevel().toUpperCase() : "-"))
                    .setFontColor(levelColor).setBold().setFontSize(13));

                // Matched skills
                if (!ms.getMatchedSkills().isEmpty()) {
                    doc.add(new Paragraph("Matched Skills: " + String.join(", ", ms.getMatchedSkills()))
                        .setFontColor(GREEN).setFontSize(11));
                }
                // Missing skills
                if (!ms.getMissingSkills().isEmpty()) {
                    doc.add(new Paragraph("Missing Skills: " + String.join(", ", ms.getMissingSkills()))
                        .setFontColor(RED).setFontSize(11));
                }
                // Recommendations
                if (ms.getRecommendations() != null && !ms.getRecommendations().isBlank()) {
                    doc.add(new Paragraph("\n"));
                    doc.add(sectionTitle("AI Recommendations"));
                    doc.add(new Paragraph(ms.getRecommendations())
                        .setFontSize(11).setBackgroundColor(LIGHT_BG).setPadding(10));
                }
                doc.add(new Paragraph("\n"));
            }

            // ── Feedback ──────────────────────────────────────────────────────
            if (c.getFeedback() != null && !c.getFeedback().isBlank()) {
                doc.add(sectionTitle("Feedback"));
                doc.add(new Paragraph(c.getFeedback())
                    .setFontSize(11).setBackgroundColor(LIGHT_BG).setPadding(10));
                doc.add(new Paragraph("\n"));
            }

            // ── Notes ─────────────────────────────────────────────────────────
            if (notes != null && !notes.isEmpty()) {
                doc.add(sectionTitle("Internal Notes"));
                for (CandidatureNote note : notes) {
                    if (note.isPrivate()) continue; // skip private notes in export
                    Table noteTable = new Table(UnitValue.createPercentArray(new float[]{1}))
                        .useAllAvailableWidth().setMarginBottom(8);
                    String stars = "★".repeat(note.getRating()) + "☆".repeat(5 - note.getRating());
                    Cell noteCell = new Cell()
                        .setBackgroundColor(LIGHT_BG).setPadding(10)
                        .add(new Paragraph(note.getAuthorName() + "  " + stars)
                            .setBold().setFontSize(11))
                        .add(new Paragraph(note.getContent()).setFontSize(11))
                        .add(new Paragraph(note.getCreatedAt() != null ? note.getCreatedAt().format(FMT) : "")
                            .setFontSize(9).setFontColor(ColorConstants.GRAY));
                    noteTable.addCell(noteCell.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
                    doc.add(noteTable);
                }
                doc.add(new Paragraph("\n"));
            }

            // ── Footer ────────────────────────────────────────────────────────
            doc.add(new Paragraph("Generated by SmartPFE Desktop — " +
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .setFontSize(9).setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER));
        }
        return file;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Paragraph sectionTitle(String text) {
        return new Paragraph(text)
            .setBold().setFontSize(14).setFontColor(PRIMARY)
            .setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(PRIMARY, 1))
            .setMarginBottom(6);
    }

    private void addRow(Table t, String label, String value) {
        t.addCell(new Cell().add(new Paragraph(label).setBold().setFontSize(11))
            .setBackgroundColor(LIGHT_BG).setPadding(6)
            .setBorder(new com.itextpdf.layout.borders.SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)));
        t.addCell(new Cell().add(new Paragraph(value != null ? value : "-").setFontSize(11))
            .setPadding(6)
            .setBorder(new com.itextpdf.layout.borders.SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)));
    }

    private void addScoreCell(Table t, String label, double score, String hexColor) {
        DeviceRgb color = hexToRgb(hexColor);
        t.addCell(new Cell()
            .setTextAlignment(TextAlignment.CENTER).setPadding(12)
            .setBorder(new com.itextpdf.layout.borders.SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
            .add(new Paragraph(String.format("%.1f%%", score)).setBold().setFontSize(18).setFontColor(color))
            .add(new Paragraph(label).setFontSize(10).setFontColor(new DeviceRgb(100, 100, 100))));
    }

    private DeviceRgb levelColor(String level) {
        if (level == null) return new DeviceRgb(108, 117, 125);
        return switch (level) {
            case "excellent" -> GREEN;
            case "good"      -> ORANGE;
            case "fair"      -> new DeviceRgb(253, 126, 20);
            default          -> RED;
        };
    }

    private DeviceRgb hexToRgb(String hex) {
        try {
            hex = hex.replace("#", "");
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return new DeviceRgb(r, g, b);
        } catch (Exception e) { return DARK; }
    }
}
