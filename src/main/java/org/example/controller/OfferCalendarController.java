package org.example.controller;

import org.example.dao.CandidatureDAO;
import org.example.dao.OfferDAO;
import org.example.model.Candidature;
import org.example.model.Offer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.ResourceBundle;

public class OfferCalendarController implements Initializable {

    @FXML private Label monthLabel;
    @FXML private GridPane calendarGrid;
    @FXML private VBox legendBox;
    @FXML private VBox eventList;

    private final OfferDAO offerDAO = new OfferDAO();
    private final CandidatureDAO candidatureDAO = new CandidatureDAO();

    private YearMonth currentMonth;
    private Map<LocalDate, List<String[]>> events = new HashMap<>(); // date -> list of [label, color]

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentMonth = YearMonth.now();
        loadEvents();
        renderCalendar();
    }

    @FXML public void prevMonth() { currentMonth = currentMonth.minusMonths(1); renderCalendar(); }
    @FXML public void nextMonth() { currentMonth = currentMonth.plusMonths(1); renderCalendar(); }

    private void loadEvents() {
        events.clear();
        try {
            // Offer deadlines
            for (Offer o : offerDAO.findAll()) {
                if (o.getDeadline() != null) {
                    addEvent(o.getDeadline(), "📋 " + truncate(o.getTitle(), 20), "#a12c2f");
                }
            }
            // Candidature interview dates
            String sql = "SELECT c.interview_date, u.name, o.title FROM candidatures c " +
                         "JOIN users u ON c.student_id = u.id " +
                         "JOIN project_offers o ON c.offer_id = o.id " +
                         "WHERE c.interview_date IS NOT NULL";
            try (java.sql.Statement st = org.example.config.DatabaseConfig.getConnection().createStatement();
                 java.sql.ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    java.sql.Date d = rs.getDate("interview_date");
                    if (d != null) {
                        addEvent(d.toLocalDate(),
                            "🎤 " + truncate(rs.getString("name"), 15),
                            "#667eea");
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void addEvent(LocalDate date, String label, String color) {
        events.computeIfAbsent(date, k -> new ArrayList<>()).add(new String[]{label, color});
    }

    private void renderCalendar() {
        monthLabel.setText(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        calendarGrid.getChildren().clear();
        eventList.getChildren().clear();

        // Day headers
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int i = 0; i < 7; i++) {
            Label lbl = new Label(days[i]);
            lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #888;");
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER);
            calendarGrid.add(lbl, i, 0);
        }

        LocalDate first = currentMonth.atDay(1);
        int startCol = first.getDayOfWeek().getValue() - 1; // Mon=0
        int daysInMonth = currentMonth.lengthOfMonth();
        LocalDate today = LocalDate.now();

        int col = startCol, row = 1;
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            List<String[]> dayEvents = events.getOrDefault(date, List.of());

            VBox cell = new VBox(2);
            cell.setPadding(new Insets(4));
            cell.setMinHeight(70);
            cell.setStyle("-fx-background-color: " + (date.equals(today) ? "#fff3f3" : "white") +
                "; -fx-border-color: #e8e8e8; -fx-border-width: 0.5;");

            Label dayLbl = new Label(String.valueOf(day));
            dayLbl.setStyle("-fx-font-weight: " + (date.equals(today) ? "bold" : "normal") +
                "; -fx-text-fill: " + (date.equals(today) ? "#a12c2f" : "#333") + "; -fx-font-size: 12px;");
            cell.getChildren().add(dayLbl);

            for (String[] ev : dayEvents) {
                Label evLbl = new Label(ev[0]);
                evLbl.setStyle("-fx-background-color: " + ev[1] + "22; -fx-text-fill: " + ev[1] +
                    "; -fx-font-size: 9px; -fx-background-radius: 3; -fx-padding: 1 4;");
                evLbl.setMaxWidth(Double.MAX_VALUE);
                evLbl.setWrapText(false);
                cell.getChildren().add(evLbl);
            }

            // Click to show events in sidebar
            if (!dayEvents.isEmpty()) {
                cell.setOnMouseClicked(e -> showDayEvents(date, dayEvents));
                cell.setStyle(cell.getStyle() + " -fx-cursor: hand;");
            }

            calendarGrid.add(cell, col, row);
            col++;
            if (col == 7) { col = 0; row++; }
        }

        // Show upcoming events in sidebar
        showUpcomingEvents();
    }

    private void showDayEvents(LocalDate date, List<String[]> dayEvents) {
        eventList.getChildren().clear();
        Label title = new Label(date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1a1a2e;");
        eventList.getChildren().add(title);
        for (String[] ev : dayEvents) {
            Label lbl = new Label(ev[0]);
            lbl.setStyle("-fx-background-color: " + ev[1] + "22; -fx-text-fill: " + ev[1] +
                "; -fx-background-radius: 6; -fx-padding: 6 10; -fx-font-size: 12px;");
            lbl.setMaxWidth(Double.MAX_VALUE);
            eventList.getChildren().add(lbl);
        }
    }

    private void showUpcomingEvents() {
        eventList.getChildren().clear();
        Label title = new Label("Upcoming Events");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1a1a2e;");
        eventList.getChildren().add(title);

        LocalDate today = LocalDate.now();
        events.entrySet().stream()
            .filter(e -> !e.getKey().isBefore(today))
            .sorted(Map.Entry.comparingByKey())
            .limit(10)
            .forEach(entry -> {
                for (String[] ev : entry.getValue()) {
                    VBox item = new VBox(2);
                    item.setStyle("-fx-background-color: white; -fx-background-radius: 6; -fx-padding: 8; " +
                        "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.05),4,0,0,1);");
                    Label dateLbl = new Label(entry.getKey().format(DateTimeFormatter.ofPattern("dd MMM")));
                    dateLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");
                    Label evLbl = new Label(ev[0]);
                    evLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + ev[1] + ";");
                    item.getChildren().addAll(dateLbl, evLbl);
                    VBox.setMargin(item, new Insets(0, 0, 6, 0));
                    eventList.getChildren().add(item);
                }
            });
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : (s != null ? s : "");
    }
}
