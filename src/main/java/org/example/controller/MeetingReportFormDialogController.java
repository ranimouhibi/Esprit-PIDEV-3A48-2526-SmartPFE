package org.example.controller;

import org.example.dao.MeetingReportDAO;
import org.example.model.Meeting;
import org.example.model.MeetingReport;
import org.example.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class MeetingReportFormDialogController implements Initializable {

    @FXML private Label dialogTitle;
    @FXML private TextArea discussionField, decisionsField, actionItemsField, nextStepsField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Button saveBtn;
    @FXML private Label errorLabel;

    private final MeetingReportDAO reportDAO = new MeetingReportDAO();
    private MeetingReport editReport;
    private Meeting meeting;
    private Consumer<MeetingReport> callback;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        statusCombo.setItems(FXCollections.observableArrayList("DRAFT", "SUBMITTED", "APPROVED"));
        statusCombo.setValue("DRAFT");
    }

    public void initCreate(Meeting m, Consumer<MeetingReport> cb) {
        this.meeting = m;
        this.callback = cb;
        dialogTitle.setText("Nouveau Report");
        statusCombo.setValue("DRAFT");
        statusCombo.setDisable(false);
    }

    public void initEdit(MeetingReport r, Consumer<MeetingReport> cb) {
        this.editReport = r;
        this.callback = cb;
        dialogTitle.setText("Modifier Report");
        discussionField.setText(r.getDiscussionPoints() != null ? r.getDiscussionPoints() : "");
        decisionsField.setText(r.getDecisions() != null ? r.getDecisions() : "");
        actionItemsField.setText(r.getActionItems() != null ? r.getActionItems() : "");
        nextStepsField.setText(r.getNextSteps() != null ? r.getNextSteps() : "");
        statusCombo.setValue(r.getStatus());

        if ("APPROVED".equalsIgnoreCase(r.getStatus())) {
            discussionField.setEditable(false);
            decisionsField.setEditable(false);
            actionItemsField.setEditable(false);
            nextStepsField.setEditable(false);
            statusCombo.setDisable(true);
            saveBtn.setDisable(true);
        }
    }

    @FXML public void handleSave() {
        errorLabel.setText("");
        String discussion = discussionField.getText().trim();
        String decisions = decisionsField.getText().trim();
        String actionItems = actionItemsField.getText().trim();
        String nextSteps = nextStepsField.getText().trim();
        String status = statusCombo.getValue();

        if (discussion.length() < 10 || discussion.length() > 5000) { errorLabel.setText("Discussion: 10 à 5000 caractères."); return; }
        if (decisions.length() < 10 || decisions.length() > 5000) { errorLabel.setText("Decisions: 10 à 5000 caractères."); return; }
        if (actionItems.length() < 10 || actionItems.length() > 5000) { errorLabel.setText("Action Items: 10 à 5000 caractères."); return; }
        if (nextSteps.length() > 5000) { errorLabel.setText("Next Steps: max 5000 caractères."); return; }

        MeetingReport r = editReport != null ? editReport : new MeetingReport();
        r.setDiscussionPoints(discussion);
        r.setDecisions(decisions);
        r.setActionItems(actionItems);
        r.setNextSteps(nextSteps);
        r.setStatus(editReport == null ? "DRAFT" : status);

        try {
            if (editReport == null) {
                r.setMeetingId(meeting.getId());
                var user = SessionManager.getCurrentUser();
                r.setCreatedById(user != null ? user.getId() : 0);
                reportDAO.save(r);
            } else {
                reportDAO.update(r);
            }
            if (callback != null) callback.accept(r);
            ((Stage) saveBtn.getScene().getWindow()).close();
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Erreur lors de la sauvegarde.");
        }
    }

    @FXML public void handleCancel() {
        ((Stage) saveBtn.getScene().getWindow()).close();
    }
}
