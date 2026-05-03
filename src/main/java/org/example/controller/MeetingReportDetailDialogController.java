package org.example.controller;

import org.example.model.MeetingReport;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

public class MeetingReportDetailDialogController {

    @FXML private Label headerSub, statusBadge;
    @FXML private Label detailProject, detailType, detailCreatedBy, detailMeetingDate;
    @FXML private Label detailCreatedAt, detailDiscussion, detailDecisions, detailActionItems, detailNextSteps;
    @FXML private Button closeBtn;

    public void init(MeetingReport r) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        headerSub.setText((r.getMeetingType() != null ? r.getMeetingType() : "") + " — " + (r.getProjectTitle() != null ? r.getProjectTitle() : ""));
        statusBadge.setText(r.getStatus());
        String color = switch (r.getStatus() != null ? r.getStatus() : "") {
            case "APPROVED" -> "#27ae60";
            case "SUBMITTED" -> "#2980b9";
            default -> "#f39c12";
        };
        statusBadge.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 3 10; -fx-font-weight: bold;");
        detailProject.setText(r.getProjectTitle() != null ? r.getProjectTitle() : "—");
        detailType.setText(r.getMeetingType() != null ? r.getMeetingType() : "—");
        detailCreatedBy.setText(r.getCreatedByName() != null ? r.getCreatedByName() : "—");
        detailMeetingDate.setText(r.getMeetingDate() != null ? r.getMeetingDate().format(fmt) : "—");
        detailCreatedAt.setText(r.getCreatedAt() != null ? r.getCreatedAt().format(fmt) : "—");
        detailDiscussion.setText(r.getDiscussionPoints() != null ? r.getDiscussionPoints() : "—");
        detailDecisions.setText(r.getDecisions() != null ? r.getDecisions() : "—");
        detailActionItems.setText(r.getActionItems() != null ? r.getActionItems() : "—");
        detailNextSteps.setText(r.getNextSteps() != null ? r.getNextSteps() : "—");
    }

    @FXML public void handleClose() {
        ((Stage) closeBtn.getScene().getWindow()).close();
    }
}
