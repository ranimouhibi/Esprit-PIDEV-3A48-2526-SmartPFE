package org.example.controller;

import org.example.dao.CandidatureDAO;
import org.example.dao.OfferDAO;
import org.example.model.Offer;
import org.example.util.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.net.URL;
import java.sql.ResultSet;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class OfferCalendarController implements Initializable {

    @FXML private WebView calendarWebView;

    private final OfferDAO offerDAO = new OfferDAO();
    private final CandidatureDAO candidatureDAO = new CandidatureDAO();
    private WebEngine engine;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        engine = calendarWebView.getEngine();

        // Load the HTML file
        URL htmlUrl = getClass().getResource("/html/offer_calendar.html");
        if (htmlUrl != null) {
            engine.load(htmlUrl.toExternalForm());
        }

        // Once page is loaded, inject events
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                // Register Java bridge for click callbacks
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaConnector", new JavaConnector());
                // Inject events
                injectEvents();
            }
        });
    }

    private void injectEvents() {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        try {
            // Offer deadlines — red
            for (Offer o : offerDAO.findAll()) {
                if (o.getDeadline() != null) {
                    if (!first) json.append(",");
                    json.append("{")
                        .append("\"id\":\"offer-").append(o.getId()).append("\",")
                        .append("\"title\":\"📋 ").append(escape(o.getTitle())).append("\",")
                        .append("\"start\":\"").append(o.getDeadline().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\",")
                        .append("\"allDay\":true,")
                        .append("\"backgroundColor\":\"#a12c2f\",")
                        .append("\"borderColor\":\"#a12c2f\",")
                        .append("\"extendedProps\":{")
                        .append("\"type\":\"offer\",")
                        .append("\"id\":\"").append(o.getId()).append("\",")
                        .append("\"description\":\"Deadline: ").append(o.getDeadline()).append("\\nSkills: ").append(escape(o.getRequiredSkills() != null ? o.getRequiredSkills() : "")).append("\"")
                        .append("}}");
                    first = false;
                }
            }

            // Candidature submission dates — blue
            var cands = candidatureDAO.findAll();
            for (var c : cands) {
                if (c.getCreatedAt() != null) {
                    if (!first) json.append(",");
                    String color = "accepted".equals(c.getStatus()) ? "#22c55e"
                                 : "rejected".equals(c.getStatus()) ? "#ef4444"
                                 : "#667eea";
                    json.append("{")
                        .append("\"id\":\"cand-").append(c.getId()).append("\",")
                        .append("\"title\":\"✉ ").append(escape(c.getStudentName() != null ? c.getStudentName() : "Student")).append("\",")
                        .append("\"start\":\"").append(c.getCreatedAt().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\",")
                        .append("\"allDay\":true,")
                        .append("\"backgroundColor\":\"").append(color).append("\",")
                        .append("\"borderColor\":\"").append(color).append("\",")
                        .append("\"extendedProps\":{")
                        .append("\"type\":\"candidature\",")
                        .append("\"id\":\"").append(c.getId()).append("\",")
                        .append("\"description\":\"").append(escape(c.getOfferTitle() != null ? c.getOfferTitle() : "")).append("\\nStatus: ").append(c.getStatus()).append("\"")
                        .append("}}");
                    first = false;
                }
            }

            // Interview dates — purple
            try {
                String sql = "SELECT c.id, c.interview_date, u.name, o.title " +
                             "FROM candidatures c " +
                             "JOIN users u ON c.student_id = u.id " +
                             "JOIN project_offers o ON c.offer_id = o.id " +
                             "WHERE c.interview_date IS NOT NULL";
                try (java.sql.Statement st = org.example.config.DatabaseConfig.getConnection().createStatement();
                     ResultSet rs = st.executeQuery(sql)) {
                    while (rs.next()) {
                        java.sql.Date d = rs.getDate("interview_date");
                        if (d != null) {
                            if (!first) json.append(",");
                            json.append("{")
                                .append("\"id\":\"interview-").append(rs.getInt("id")).append("\",")
                                .append("\"title\":\"🎤 ").append(escape(rs.getString("name"))).append("\",")
                                .append("\"start\":\"").append(d.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\",")
                                .append("\"allDay\":true,")
                                .append("\"backgroundColor\":\"#8b5cf6\",")
                                .append("\"borderColor\":\"#8b5cf6\",")
                                .append("\"extendedProps\":{")
                                .append("\"type\":\"interview\",")
                                .append("\"id\":\"").append(rs.getInt("id")).append("\",")
                                .append("\"description\":\"Interview for: ").append(escape(rs.getString("title"))).append("\"")
                                .append("}}");
                            first = false;
                        }
                    }
                }
            } catch (Exception ignored) {}

        } catch (Exception e) {
            e.printStackTrace();
        }

        json.append("]");
        final String eventsJson = json.toString();

        Platform.runLater(() -> {
            try {
                engine.executeScript("loadEvents(" + jsString(eventsJson) + ")");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    public void handleRefresh() {
        injectEvents();
    }

    /** Safely escape a string for JSON */
    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", " ");
    }

    /** Wrap a Java string as a JS string literal */
    private String jsString(String s) {
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    /** Bridge object accessible from JavaScript */
    public class JavaConnector {
        public void onEventClick(String id, String type) {
            System.out.println("[Calendar] Clicked: type=" + type + " id=" + id);
            // Could open detail dialog here
        }
    }
}
