package org.example.controller;

import org.example.config.DatabaseConfig;
import org.example.dao.ProjectDAO;
import org.example.model.Meeting;
import org.example.model.Project;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class MeetingController implements Initializable {

    @FXML private TableView<Meeting> meetingTable;
    @FXML private TableColumn<Meeting, Integer> colId;
    @FXML private TableColumn<Meeting, String> colTitle;
    @FXML private TableColumn<Meeting, String> colType;
    @FXML private TableColumn<Meeting, String> colStatus;
    @FXML private TableColumn<Meeting, String> colProject;
    @FXML private TableColumn<Meeting, LocalDateTime> colDate;

    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<Project> projectCombo;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextField locationField;
    @FXML private TextField linkField;
    @FXML private DatePicker datePicker;
    @FXML private Label messageLabel;

    private final ProjectDAO projectDAO = new ProjectDAO();
    private Meeting selectedMeeting;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colType.setCellValueFactory(new PropertyValueFactory<>("meetingType"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colProject.setCellValueFactory(new PropertyValueFactory<>("projectTitle"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("scheduledDate"));

        typeCombo.setItems(FXCollections.observableArrayList("weekly", "sprint_review", "retrospective", "planning", "other"));
        statusCombo.setItems(FXCollections.observableArrayList("scheduled", "completed", "cancelled"));

        try { projectCombo.setItems(FXCollections.observableArrayList(projectDAO.findAll())); }
        catch (Exception e) { e.printStackTrace(); }

        loadMeetings();

        meetingTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                selectedMeeting = sel;
                titleField.setText(sel.getTitle());
                descriptionField.setText(sel.getDescription());
                typeCombo.setValue(sel.getMeetingType());
                statusCombo.setValue(sel.getStatus());
                locationField.setText(sel.getLocation());
                linkField.setText(sel.getMeetingLink());
                if (sel.getScheduledDate() != null) datePicker.setValue(sel.getScheduledDate().toLocalDate());
            }
        });
    }

    private void loadMeetings() {
        try {
            List<Meeting> list = new ArrayList<>();
            String sql = "SELECT m.*, p.title as project_title FROM meetings m LEFT JOIN projects p ON m.project_id = p.id ORDER BY m.created_at DESC";
            ResultSet rs = DatabaseConfig.getConnection().createStatement().executeQuery(sql);
            while (rs.next()) {
                Meeting m = new Meeting();
                m.setId(rs.getInt("id"));
                m.setTitle(rs.getString("title"));
                m.setDescription(rs.getString("description"));
                m.setMeetingType(rs.getString("meeting_type"));
                m.setStatus(rs.getString("status"));
                m.setLocation(rs.getString("location"));
                m.setMeetingLink(rs.getString("meeting_link"));
                m.setProjectTitle(rs.getString("project_title"));
                m.setProjectId(rs.getInt("project_id"));
                Timestamp ts = rs.getTimestamp("scheduled_date");
                if (ts != null) m.setScheduledDate(ts.toLocalDateTime());
                list.add(m);
            }
            meetingTable.setItems(FXCollections.observableArrayList(list));
        } catch (Exception e) { showMessage("Erreur: " + e.getMessage(), true); }
    }

    @FXML
    public void handleSave() {
        if (titleField.getText().trim().isEmpty() || projectCombo.getValue() == null) {
            showMessage("Titre et projet sont obligatoires.", true);
            return;
        }
        try {
            if (selectedMeeting == null) {
                String sql = "INSERT INTO meetings (project_id, title, description, meeting_type, status, location, meeting_link, scheduled_date, created_at) VALUES (?,?,?,?,?,?,?,?,?)";
                PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql);
                ps.setInt(1, projectCombo.getValue().getId());
                ps.setString(2, titleField.getText().trim());
                ps.setString(3, descriptionField.getText());
                ps.setString(4, typeCombo.getValue());
                ps.setString(5, statusCombo.getValue() != null ? statusCombo.getValue() : "scheduled");
                ps.setString(6, locationField.getText());
                ps.setString(7, linkField.getText());
                ps.setDate(8, datePicker.getValue() != null ? Date.valueOf(datePicker.getValue()) : null);
                ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
            } else {
                String sql = "UPDATE meetings SET title=?, description=?, meeting_type=?, status=?, location=?, meeting_link=?, scheduled_date=? WHERE id=?";
                PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql);
                ps.setString(1, titleField.getText().trim());
                ps.setString(2, descriptionField.getText());
                ps.setString(3, typeCombo.getValue());
                ps.setString(4, statusCombo.getValue());
                ps.setString(5, locationField.getText());
                ps.setString(6, linkField.getText());
                ps.setDate(7, datePicker.getValue() != null ? Date.valueOf(datePicker.getValue()) : null);
                ps.setInt(8, selectedMeeting.getId());
                ps.executeUpdate();
            }
            showMessage("Meeting sauvegardé.", false);
            handleClear();
            loadMeetings();
        } catch (Exception e) { showMessage("Erreur: " + e.getMessage(), true); }
    }

    @FXML
    public void handleDelete() {
        if (selectedMeeting == null) { showMessage("Sélectionnez un meeting.", true); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce meeting?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement("DELETE FROM meetings WHERE id=?");
                    ps.setInt(1, selectedMeeting.getId());
                    ps.executeUpdate();
                    showMessage("Meeting supprimé.", false);
                    handleClear();
                    loadMeetings();
                } catch (Exception e) { showMessage("Erreur: " + e.getMessage(), true); }
            }
        });
    }

    @FXML
    public void handleClear() {
        selectedMeeting = null;
        titleField.clear(); descriptionField.clear(); locationField.clear(); linkField.clear();
        typeCombo.setValue(null); statusCombo.setValue(null); projectCombo.setValue(null); datePicker.setValue(null);
        meetingTable.getSelectionModel().clearSelection();
    }

    private void showMessage(String msg, boolean isError) {
        messageLabel.setText(msg);
        messageLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
}
