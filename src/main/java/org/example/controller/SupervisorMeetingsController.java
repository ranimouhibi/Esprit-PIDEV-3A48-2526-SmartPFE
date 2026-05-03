package org.example.controller;

import org.example.config.DatabaseConfig;
import org.example.dao.ProjectDAO;
import org.example.model.Project;
import org.example.model.User;
import org.example.service.MeetingService;
import org.example.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.ResourceBundle;

public class SupervisorMeetingsController implements Initializable {

    @FXML private VBox meetingsContainer;
    @FXML private ComboBox<Project> projectCombo;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private DatePicker datePicker;
    @FXML private TextField locationField;
    @FXML private TextArea agendaField;
    @FXML private Label jitsiLinkLabel;
    @FXML private Label messageLabel;

    // AI Report
    @FXML private TextArea rawNotesArea;
    @FXML private TextArea reportArea;
    @FXML private Label reportStatusLabel;

    // Calendar
    @FXML private Label calMonthLabel;
    @FXML private GridPane calGrid;

    private final ProjectDAO projectDAO = new ProjectDAO();
    private final MeetingService meetingService = new MeetingService();
    private int currentMeetingId = 0;
    private java.time.YearMonth calMonth = java.time.YearMonth.now();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        typeCombo.setItems(FXCollections.observableArrayList("weekly", "sprint_review", "retrospective", "planning", "other"));
        statusCombo.setItems(FXCollections.observableArrayList("scheduled", "completed", "cancelled"));
        typeCombo.setValue("weekly");
        statusCombo.setValue("scheduled");

        projectCombo.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Project p) { return p == null ? "" : p.getTitle(); }
            public Project fromString(String s) { return null; }
        });

        try {
            User user = SessionManager.getCurrentUser();
            projectCombo.setItems(FXCollections.observableArrayList(projectDAO.findBySupervisor(user.getId())));
        } catch (Exception e) { e.printStackTrace(); }

        loadMeetings();
        renderCalendar();
    }

    private void loadMeetings() {
        meetingsContainer.getChildren().clear();
        try {
            String sql = "SELECT m.*, p.title as project_title FROM meetings m " +
                         "LEFT JOIN projects p ON m.project_id = p.id " +
                         "ORDER BY m.scheduled_date DESC LIMIT 20";
            try (Statement st = DatabaseConfig.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    meetingsContainer.getChildren().add(buildMeetingCard(rs));
                }
                if (!found) {
                    Label empty = new Label("No meetings yet. Schedule one below.");
                    empty.setStyle("-fx-text-fill: #999; -fx-font-size: 13px;");
                    meetingsContainer.getChildren().add(empty);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private VBox buildMeetingCard(ResultSet rs) throws SQLException {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 14; " +
            "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        VBox.setMargin(card, new Insets(0, 0, 8, 0));

        int id = rs.getInt("id");
        String type = rs.getString("meeting_type");
        String status = rs.getString("status");
        String project = rs.getString("project_title");
        Timestamp ts = rs.getTimestamp("scheduled_date");
        String dateStr = ts != null ? ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-";
        String location = rs.getString("location");
        String link = rs.getString("meeting_link");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label typeLbl = new Label(type != null ? type.toUpperCase() : "MEETING");
        typeLbl.setStyle("-fx-background-color: #667eea22; -fx-text-fill: #667eea; -fx-background-radius: 6; -fx-padding: 3 8; -fx-font-size: 10px; -fx-font-weight: bold;");
        String statusColor = "completed".equals(status) ? "#28a745" : "cancelled".equals(status) ? "#dc3545" : "#ffc107";
        Label statusLbl = new Label(status != null ? status.toUpperCase() : "");
        statusLbl.setStyle("-fx-background-color: " + statusColor + "22; -fx-text-fill: " + statusColor + "; -fx-background-radius: 6; -fx-padding: 3 8; -fx-font-size: 10px;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label dateLbl = new Label(dateStr);
        dateLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        header.getChildren().addAll(typeLbl, statusLbl, sp, dateLbl);

        Label projLbl = new Label("Project: " + (project != null ? project : "-"));
        projLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        card.getChildren().addAll(header, projLbl);

        if (location != null && !location.isBlank()) {
            Label locLbl = new Label("📍 " + location);
            locLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
            card.getChildren().add(locLbl);
        }

        // Jitsi link
        if (link != null && !link.isBlank()) {
            Hyperlink jitsiLink = new Hyperlink("🎥 Join: " + link);
            jitsiLink.setStyle("-fx-font-size: 11px; -fx-text-fill: #667eea;");
            jitsiLink.setOnAction(e -> openBrowser(link));
            card.getChildren().add(jitsiLink);
        }

        return card;
    }

    @FXML public void handleGenerateJitsi() {
        Project p = projectCombo.getValue();
        String projectTitle = p != null ? p.getTitle() : "Meeting";
        currentMeetingId = (int)(System.currentTimeMillis() % 100000);
        String link = meetingService.generateJitsiLink(projectTitle, currentMeetingId);
        if (jitsiLinkLabel != null) {
            jitsiLinkLabel.setText(link);
            jitsiLinkLabel.setStyle("-fx-text-fill: #667eea; -fx-font-size: 12px;");
        }
    }

    @FXML public void handleOpenJitsi() {
        if (jitsiLinkLabel != null && !jitsiLinkLabel.getText().isBlank()) {
            openBrowser(jitsiLinkLabel.getText());
        }
    }

    @FXML public void handleSaveMeeting() {
        Project p = projectCombo.getValue();
        if (p == null) { showMsg("Select a project.", true); return; }
        try {
            String jitsiLink = jitsiLinkLabel != null ? jitsiLinkLabel.getText() : "";
            String sql = "INSERT INTO meetings (project_id, meeting_type, status, location, meeting_link, agenda, scheduled_date, created_at) VALUES (?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
                ps.setInt(1, p.getId());
                ps.setString(2, typeCombo.getValue());
                ps.setString(3, statusCombo.getValue());
                ps.setString(4, locationField.getText());
                ps.setString(5, jitsiLink);
                ps.setString(6, agendaField.getText());
                ps.setDate(7, datePicker.getValue() != null ? java.sql.Date.valueOf(datePicker.getValue()) : null);
                ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
            }
            showMsg("Meeting saved.", false);
            loadMeetings();
            renderCalendar();
        } catch (Exception e) { showMsg("Error: " + e.getMessage(), true); }
    }

    @FXML public void handleGenerateReport() {
        String raw = rawNotesArea != null ? rawNotesArea.getText() : "";
        Project p = projectCombo.getValue();
        String report = meetingService.generateMeetingReport(raw,
            typeCombo.getValue() != null ? typeCombo.getValue() : "Meeting",
            p != null ? p.getTitle() : "Project");
        if (reportArea != null) reportArea.setText(report);
        if (reportStatusLabel != null) reportStatusLabel.setText("Report generated.");
    }

    // ── Calendar ──────────────────────────────────────────────────────────────

    @FXML public void calPrev() { calMonth = calMonth.minusMonths(1); renderCalendar(); }
    @FXML public void calNext() { calMonth = calMonth.plusMonths(1); renderCalendar(); }

    private void renderCalendar() {
        if (calGrid == null || calMonthLabel == null) return;
        calMonthLabel.setText(calMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        calGrid.getChildren().clear();

        String[] days = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
        for (int i = 0; i < 7; i++) {
            Label h = new Label(days[i]);
            h.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #888;");
            h.setMaxWidth(Double.MAX_VALUE); h.setAlignment(Pos.CENTER);
            calGrid.add(h, i, 0);
        }

        Map<java.time.LocalDate, List<String>> events = new HashMap<>();
        try {
            String sql = "SELECT scheduled_date, meeting_type FROM meetings WHERE scheduled_date IS NOT NULL";
            try (Statement st = DatabaseConfig.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("scheduled_date");
                    if (ts != null) {
                        java.time.LocalDate d = ts.toLocalDateTime().toLocalDate();
                        events.computeIfAbsent(d, k -> new ArrayList<>()).add(rs.getString("meeting_type"));
                    }
                }
            }
        } catch (Exception ignored) {}

        java.time.LocalDate first = calMonth.atDay(1);
        int startCol = first.getDayOfWeek().getValue() - 1;
        int daysInMonth = calMonth.lengthOfMonth();
        java.time.LocalDate today = java.time.LocalDate.now();
        int col = startCol, row = 1;

        for (int day = 1; day <= daysInMonth; day++) {
            java.time.LocalDate date = calMonth.atDay(day);
            List<String> dayEvents = events.getOrDefault(date, List.of());
            VBox cell = new VBox(2);
            cell.setPadding(new Insets(3));
            cell.setMinHeight(55);
            cell.setStyle("-fx-background-color: " + (date.equals(today) ? "#fff3f3" : "white") +
                "; -fx-border-color: #eee; -fx-border-width: 0.5;");
            Label dayLbl = new Label(String.valueOf(day));
            dayLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: " + (date.equals(today) ? "bold" : "normal") +
                "; -fx-text-fill: " + (date.equals(today) ? "#a12c2f" : "#333") + ";");
            cell.getChildren().add(dayLbl);
            for (String ev : dayEvents) {
                Label evLbl = new Label("📅 " + ev);
                evLbl.setStyle("-fx-background-color: #667eea22; -fx-text-fill: #667eea; -fx-font-size: 9px; -fx-background-radius: 3; -fx-padding: 1 3;");
                evLbl.setMaxWidth(Double.MAX_VALUE);
                cell.getChildren().add(evLbl);
            }
            calGrid.add(cell, col, row);
            col++; if (col == 7) { col = 0; row++; }
        }
    }

    private void openBrowser(String url) {
        try { Desktop.getDesktop().browse(new URI(url)); } catch (Exception e) { e.printStackTrace(); }
    }

    private void showMsg(String msg, boolean isError) {
        if (messageLabel != null) {
            messageLabel.setText(msg);
            messageLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
        }
    }
}
