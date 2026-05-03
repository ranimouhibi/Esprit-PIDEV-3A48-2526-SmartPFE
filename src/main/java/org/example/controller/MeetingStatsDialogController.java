package org.example.controller;

import org.example.dao.MeetingDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class MeetingStatsDialogController implements Initializable {

    @FXML private Label statTotal, statUpcoming, statAvgDuration;
    @FXML private Label statPending, statConfirmed, statCancelled;
    @FXML private Label statOnline, statInPerson, statHybrid;
    @FXML private ListView<String> monthList;
    @FXML private Button closeBtn;

    private final MeetingDAO meetingDAO = new MeetingDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadStats();
    }

    private void loadStats() {
        try {
            statTotal.setText(String.valueOf(meetingDAO.countTotal()));
            statUpcoming.setText(String.valueOf(meetingDAO.countUpcoming()));
            statAvgDuration.setText(String.format("%.0f min", meetingDAO.avgDuration()));

            Map<String, Integer> byStatus = meetingDAO.countByStatus();
            statPending.setText(String.valueOf(byStatus.getOrDefault("PENDING", 0)));
            statConfirmed.setText(String.valueOf(byStatus.getOrDefault("CONFIRMED", 0)));
            statCancelled.setText(String.valueOf(byStatus.getOrDefault("CANCELLED", 0)));

            Map<String, Integer> byType = meetingDAO.countByType();
            statOnline.setText(String.valueOf(byType.getOrDefault("ONLINE", 0)));
            statInPerson.setText(String.valueOf(byType.getOrDefault("IN_PERSON", 0)));
            statHybrid.setText(String.valueOf(byType.getOrDefault("HYBRID", 0)));

            List<String[]> months = meetingDAO.countByMonth();
            List<String> items = new ArrayList<>();
            for (String[] row : months) items.add(row[0] + "  →  " + row[1] + " meeting(s)");
            monthList.setItems(FXCollections.observableArrayList(items));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void handleClose() {
        ((Stage) closeBtn.getScene().getWindow()).close();
    }
}
