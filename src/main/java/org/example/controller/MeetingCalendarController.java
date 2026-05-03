package org.example.controller;

import org.example.dao.MeetingDAO;
import org.example.model.Meeting;
import org.example.util.SessionManager;
import org.example.model.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class MeetingCalendarController implements Initializable {

    @FXML private WebView calendarWebView;
    @FXML private Button closeBtn;

    private final MeetingDAO meetingDAO = new MeetingDAO();

    /** Bridge exposé au JavaScript via window.javaApp — référence forte pour éviter le GC */
    public final JavaBridge bridge = new JavaBridge();

    public class JavaBridge {
        public void onDateClick(String dateStr) {
            Platform.runLater(() -> {
                LocalDate date = LocalDate.parse(dateStr.substring(0, 10));
                openCreateDialog(date);
            });
        }

        public void onEventClick(String meetingIdStr) {
            Platform.runLater(() -> {
                try {
                    openEditDialog(Integer.parseInt(meetingIdStr));
                } catch (NumberFormatException e) { e.printStackTrace(); }
            });
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        calendarWebView.getEngine().getLoadWorker().stateProperty().addListener(
            (obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    JSObject win = (JSObject) calendarWebView.getEngine().executeScript("window");
                    win.setMember("javaApp", bridge);
                }
            }
        );
        loadCalendar();
    }

    @FXML
    public void handleRefresh() {
        loadCalendar();
    }

    @FXML
    public void handleClose() {
        ((Stage) closeBtn.getScene().getWindow()).close();
    }

    private void openCreateDialog(LocalDate date) {
        openDialog(ctrl -> {
            if (ctrl instanceof MeetingFormDialogController)
                ((MeetingFormDialogController) ctrl).initCreateWithDate(date, m -> loadCalendar());
        });
    }

    private void openEditDialog(int meetingId) {
        try {
            Meeting meeting = meetingDAO.findById(meetingId);
            if (meeting == null) return;
            openDialog(ctrl -> {
                if (ctrl instanceof MeetingFormDialogController)
                    ((MeetingFormDialogController) ctrl).initEdit(meeting, m -> loadCalendar());
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void openDialog(java.util.function.Consumer<Object> setup) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MeetingFormDialog.fxml"));
            Pane root = loader.load();
            setup.accept(loader.getController());
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 600, 650));
            stage.getScene().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.showAndWait();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadCalendar() {
        String eventsJson = buildEventsJson();
        calendarWebView.getEngine().loadContent(buildHtml(eventsJson), "text/html");
    }

    private String buildEventsJson() {
        try {
            User user = SessionManager.getCurrentUser();
            String role = user != null ? user.getRole() : "";
            List<Meeting> meetings;

            if ("ADMIN".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role)) {
                meetings = meetingDAO.findAll(null, "ALL", "ALL", "scheduledDate", "ASC", 1, 1000);
            } else if (user != null) {
                meetings = meetingDAO.findByUser(user.getId());
                if (meetings.isEmpty()) {
                    // fallback : afficher tous les meetings si aucun trouvé pour l'utilisateur
                    meetings = meetingDAO.findAll(null, "ALL", "ALL", "scheduledDate", "ASC", 1, 1000);
                }
            } else {
                meetings = meetingDAO.findAll(null, "ALL", "ALL", "scheduledDate", "ASC", 1, 1000);
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Meeting m : meetings) {
                if (m.getScheduledDate() == null) continue;
                String start = m.getScheduledDate().format(fmt);
                String end   = m.getScheduledDate().plusMinutes(m.getDuration()).format(fmt);
                String color = statusColor(m.getStatus());
                String title = escape(safe(m.getProjectTitle()) + " - " + safe(m.getMeetingType()));
                String loc   = escape(safe(m.getLocation()));
                String status = safe(m.getStatus());

                if (!first) sb.append(",");
                first = false;
                sb.append("{")
                  .append("\"id\":").append(m.getId()).append(",")
                  .append("\"title\":\"").append(title).append("\",")
                  .append("\"start\":\"").append(start).append("\",")
                  .append("\"end\":\"").append(end).append("\",")
                  .append("\"backgroundColor\":\"").append(color).append("\",")
                  .append("\"borderColor\":\"").append(color).append("\",")
                  .append("\"extendedProps\":{")
                  .append("\"location\":\"").append(loc).append("\",")
                  .append("\"duration\":").append(m.getDuration()).append(",")
                  .append("\"status\":\"").append(status).append("\"")
                  .append("}")
                  .append("}");
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }

    private String statusColor(String status) {
        if (status == null) return "#2196F3";
        switch (status) {
            case "CONFIRMED": return "#4CAF50";
            case "CANCELLED": return "#F44336";
            case "PENDING":   return "#FFA500";
            default:          return "#2196F3";
        }
    }

    private String safe(String s)   { return s != null ? s : ""; }
    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }

    private String buildHtml(String eventsJson) {
        return "<!DOCTYPE html>"
            + "<html><head>"
            + "<meta charset='UTF-8'>"
            + "<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/fullcalendar@5.11.3/main.min.css'>"
            + "<script src='https://cdn.jsdelivr.net/npm/fullcalendar@5.11.3/main.min.js'></script>"
            + "<style>"
            + "body{margin:0;padding:8px;font-family:Arial,sans-serif;background:#f0f2f5;}"
            + ".legend{display:flex;gap:16px;padding:8px 4px;align-items:center;}"
            + ".dot{width:12px;height:12px;border-radius:50%;display:inline-block;margin-right:4px;}"
            + "#calendar{background:white;border-radius:10px;padding:12px;"
            + "  box-shadow:0 2px 8px rgba(0,0,0,0.1);}"
            + ".fc-event{cursor:pointer;font-size:12px;}"
            + ".fc-daygrid-day{cursor:pointer;}"
            + ".fc-daygrid-day:hover{background:#f0f4ff !important;}"
            + "</style>"
            + "</head><body>"
            + "<div class='legend'>"
            + "<span><span class='dot' style='background:#FFA500'></span>Pending</span>"
            + "<span><span class='dot' style='background:#4CAF50'></span>Confirmed</span>"
            + "<span><span class='dot' style='background:#F44336'></span>Cancelled</span>"
            + "</div>"
            + "<div id='calendar'></div>"
            + "<script>"
            + "var EVENTS = " + eventsJson + ";"
            + "function callJava(action, param) {"
            + "  try {"
            + "    if (window.javaApp) {"
            + "      if (action === 'date') window.javaApp.onDateClick(param);"
            + "      else window.javaApp.onEventClick(param);"
            + "    } else { console.warn('javaApp not ready, retrying...'); setTimeout(function(){ callJava(action,param); }, 200); }"
            + "  } catch(e) { console.error(e); }"
            + "}"
            + "window.addEventListener('load', function() {"
            + "  var cal = new FullCalendar.Calendar(document.getElementById('calendar'), {"
            + "    initialView: 'dayGridMonth',"
            + "    locale: 'en',"
            + "    height: 'auto',"
            + "    headerToolbar: {"
            + "      left: 'prev,next today',"
            + "      center: 'title',"
            + "      right: 'dayGridMonth,timeGridWeek,timeGridDay,listMonth'"
            + "    },"
            + "    events: EVENTS,"
            + "    dateClick: function(info) { callJava('date', info.dateStr); },"
            + "    eventClick: function(info) { callJava('event', String(info.event.id)); },"
            + "    eventDidMount: function(info) {"
            + "      var p = info.event.extendedProps;"
            + "      info.el.title = info.event.title"
            + "        + (p.location ? ' | ' + p.location : '')"
            + "        + ' | ' + p.duration + ' min';"
            + "    }"
            + "  });"
            + "  cal.render();"
            + "});"
            + "</script>"
            + "</body></html>";
    }
}
