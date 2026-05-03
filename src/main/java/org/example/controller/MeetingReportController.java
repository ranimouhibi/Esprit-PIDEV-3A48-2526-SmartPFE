package org.example.controller;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import org.example.dao.MeetingReportDAO;
import org.example.model.Meeting;
import org.example.model.MeetingReport;
import org.example.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class MeetingReportController implements Initializable {

    @FXML private TableView<MeetingReport> reportTable;
    @FXML private TableColumn<MeetingReport, String> colProject, colType, colStatus, colCreatedBy, colDate;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatus, sortDirCombo;
    @FXML private Label statTotal, statDraft, statSubmitted, statApproved, meetingInfoLabel;

    private final MeetingReportDAO reportDAO = new MeetingReportDAO();
    private Meeting currentMeeting;
    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;
    private int totalCount = 0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        filterStatus.setItems(FXCollections.observableArrayList("ALL", "DRAFT", "SUBMITTED", "APPROVED"));
        filterStatus.setValue("ALL");
        sortDirCombo.setItems(FXCollections.observableArrayList("DESC", "ASC"));
        sortDirCombo.setValue("DESC");
        filterStatus.setOnAction(e -> loadData());
        sortDirCombo.setOnAction(e -> loadData());
    }

    public void initForMeeting(Meeting m) {
        this.currentMeeting = m;
        if (meetingInfoLabel != null) {
            String dateStr = m.getScheduledDate() != null ? m.getScheduledDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
            meetingInfoLabel.setText(m.getMeetingType() + " — " + (m.getProjectTitle() != null ? m.getProjectTitle() : "") + " — " + dateStr);
        }
        loadStats();
        loadData();
    }

    private void setupTable() {
        colProject.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProjectTitle() != null ? c.getValue().getProjectTitle() : ""));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMeetingType() != null ? c.getValue().getMeetingType() : ""));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus() != null ? c.getValue().getStatus() : ""));
        colCreatedBy.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCreatedByName() != null ? c.getValue().getCreatedByName() : ""));
        colDate.setCellValueFactory(c -> {
            if (c.getValue().getCreatedAt() == null) return new SimpleStringProperty("");
            return new SimpleStringProperty(c.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        });
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String color = switch (item) {
                    case "APPROVED" -> "#27ae60";
                    case "SUBMITTED" -> "#2980b9";
                    default -> "#f39c12";
                };
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
            }
        });
        reportTable.setPlaceholder(new Label("📭  Aucun report trouvé"));
    }

    private void loadStats() {
        try {
            int[] counts = reportDAO.countByStatus();
            statTotal.setText(String.valueOf(counts[0]));
            statDraft.setText(String.valueOf(counts[1]));
            statSubmitted.setText(String.valueOf(counts[2]));
            statApproved.setText(String.valueOf(counts[3]));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadData() {
        try {
            String search = searchField.getText();
            String status = filterStatus.getValue();
            String sortDir = sortDirCombo.getValue();
            var user = SessionManager.getCurrentUser();
            int userId = user != null ? user.getId() : 0;
            String role = user != null ? user.getRole() : "";

            List<MeetingReport> reports;
            if (currentMeeting != null) {
                reports = reportDAO.findByMeeting(currentMeeting.getId());
            } else {
                totalCount = reportDAO.count(search, status, userId, role);
                reports = reportDAO.findAll(search, status, sortDir, currentPage, PAGE_SIZE, userId, role);
            }
            reportTable.setItems(FXCollections.observableArrayList(reports));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void handleSearch() { currentPage = 1; loadData(); }

    @FXML public void handleNewReport() {
        if (currentMeeting == null) { showAlert("Sélectionnez un meeting d'abord."); return; }
        openDialog("MeetingReportFormDialog.fxml", 600, 580, ctrl -> {
            if (ctrl instanceof MeetingReportFormDialogController c) {
                c.initCreate(currentMeeting, r -> { loadStats(); loadData(); });
            }
        });
    }

    @FXML public void handleEdit() {
        MeetingReport selected = reportTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Sélectionnez un report."); return; }
        openDialog("MeetingReportFormDialog.fxml", 600, 580, ctrl -> {
            if (ctrl instanceof MeetingReportFormDialogController c) {
                c.initEdit(selected, r -> { loadStats(); loadData(); });
            }
        });
    }

    @FXML public void handleDelete() {
        MeetingReport selected = reportTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Sélectionnez un report."); return; }
        if ("APPROVED".equalsIgnoreCase(selected.getStatus())) { showAlert("Impossible de supprimer un report APPROVED."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce report ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try { reportDAO.delete(selected.getId()); loadStats(); loadData(); }
                catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    @FXML public void handleExportPdf() {
        MeetingReport selected = reportTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Sélectionnez un report à exporter."); return; }
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName("meeting_report_" + selected.getId() + ".pdf");
        File file = fc.showSaveDialog(reportTable.getScene().getWindow());
        if (file == null) return;
        try {
            exportToPdf(selected, file);
            showInfo("PDF exporté avec succès : " + file.getName());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur lors de l'export PDF.");
        }
    }

    private void exportToPdf(MeetingReport r, File file) throws Exception {
        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);

        DeviceRgb red = new DeviceRgb(161, 44, 47);
        DeviceRgb dark = new DeviceRgb(30, 30, 46);
        DeviceRgb gray = new DeviceRgb(139, 143, 168);

        // Title
        Paragraph title = new Paragraph("Meeting Report")
            .setFontColor(red).setFontSize(22).setBold()
            .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4);
        doc.add(title);

        // Meta
        String dateStr = r.getCreatedAt() != null ? r.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
        Paragraph meta = new Paragraph("Projet: " + safe(r.getProjectTitle()) + "  |  Type: " + safe(r.getMeetingType()) + "  |  Créé le: " + dateStr)
            .setFontColor(gray).setFontSize(10)
            .setTextAlignment(TextAlignment.CENTER).setMarginBottom(16);
        doc.add(meta);

        doc.add(new LineSeparator(new SolidLine()));

        addSection(doc, "Discussion Points", r.getDiscussionPoints(), red, dark);
        addSection(doc, "Decisions", r.getDecisions(), red, dark);
        addSection(doc, "Action Items", r.getActionItems(), red, dark);
        addSection(doc, "Next Steps", r.getNextSteps(), red, dark);

        doc.add(new LineSeparator(new SolidLine()));
        Paragraph footer = new Paragraph("SmartPFE — Generated automatically")
            .setFontColor(gray).setFontSize(9)
            .setTextAlignment(TextAlignment.CENTER).setMarginTop(8);
        doc.add(footer);

        doc.close();
    }

    private void addSection(Document doc, String title, String content, DeviceRgb titleColor, DeviceRgb bodyColor) {
        doc.add(new Paragraph(title).setFontColor(titleColor).setFontSize(13).setBold().setMarginTop(14).setMarginBottom(4));
        doc.add(new Paragraph(content != null ? content : "—").setFontColor(bodyColor).setFontSize(11).setMarginBottom(6));
    }

    private String safe(String s) { return s != null ? s : ""; }

    @FXML public void handleViewDetail() {
        MeetingReport selected = reportTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Sélectionnez un report."); return; }
        openDialog("MeetingReportDetailDialog.fxml", 560, 620, ctrl -> {
            if (ctrl instanceof MeetingReportDetailDialogController c) {
                c.init(selected);
            }
        });
    }

    private void openDialog(String fxml, double w, double h, java.util.function.Consumer<Object> setup) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxml));
            Pane root = loader.load();
            setup.accept(loader.getController());
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, w, h));
            stage.getScene().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.showAndWait();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
}
