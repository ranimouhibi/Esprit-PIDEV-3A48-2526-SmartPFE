package org.example.controller;

import org.example.config.DatabaseConfig;
import org.example.dao.ProjectDAO;
import org.example.model.Meeting;
import org.example.model.Project;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class MeetingController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> filterStatus;
    @FXML private FlowPane meetingsContainer;

    private final ProjectDAO projectDAO = new ProjectDAO();
    private List<Meeting> allMeetings;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filterType.getItems().addAll("All Types", "weekly", "sprint_review", "retrospective", "planning", "other");
        filterType.setValue("All Types");

        filterStatus.getItems().addAll("All Status", "scheduled", "completed", "cancelled");
        filterStatus.setValue("All Status");

        loadMeetings();
    }

    private void loadMeetings() {
        try {
            allMeetings = new ArrayList<>();
            String sql = "SELECT m.*, p.title as project_title FROM meetings m LEFT JOIN projects p ON m.project_id = p.id ORDER BY m.created_at DESC";
            ResultSet rs = DatabaseConfig.getConnection().createStatement().executeQuery(sql);
            while (rs.next()) {
                Meeting m = new Meeting();
                m.setId(rs.getInt("id"));
                m.setProjectId(rs.getInt("project_id"));
                m.setProjectTitle(rs.getString("project_title"));
                m.setTitle(rs.getString("title"));
                m.setDescription(rs.getString("description"));
                m.setMeetingType(rs.getString("meeting_type"));
                m.setStatus(rs.getString("status"));
                m.setLocation(rs.getString("location"));
                m.setMeetingLink(rs.getString("meeting_link"));
                Timestamp ts = rs.getTimestamp("scheduled_date");
                if (ts != null) m.setScheduledDate(ts.toLocalDateTime());
                Timestamp created = rs.getTimestamp("created_at");
                if (created != null) m.setCreatedAt(created.toLocalDateTime());
                allMeetings.add(m);
            }
            displayMeetings(allMeetings);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load meetings: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void displayMeetings(List<Meeting> meetings) {
        meetingsContainer.getChildren().clear();

        if (meetings.isEmpty()) {
            VBox emptyState = createEmptyState();
            meetingsContainer.getChildren().add(emptyState);
            return;
        }

        for (Meeting meeting : meetings) {
            VBox card = createMeetingCard(meeting);
            meetingsContainer.getChildren().add(card);
        }
    }

    private VBox createEmptyState() {
        VBox empty = new VBox(12);
        empty.setAlignment(javafx.geometry.Pos.CENTER);
        empty.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e0e0e0; -fx-border-radius: 12; -fx-border-width: 2; -fx-border-style: dashed;");
        empty.setPrefSize(400, 200);
        empty.setPadding(new Insets(30));

        Label icon = new Label("[EMPTY]");
        icon.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #999;");

        Label title = new Label("No meetings found");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #666;");

        Label subtitle = new Label("Click 'New Meeting' to schedule your first meeting");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #999;");

        empty.getChildren().addAll(icon, title, subtitle);
        return empty;
    }

    private VBox createMeetingCard(Meeting meeting) {
        VBox card = new VBox(14);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 3); -fx-border-color: #f0f0f0; -fx-border-radius: 14; -fx-border-width: 1;");
        card.setPrefWidth(340);
        card.setPadding(new Insets(20));

        // Header
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label typeLabel = new Label(meeting.getMeetingType() != null ? meeting.getMeetingType().toUpperCase() : "N/A");
        typeLabel.setStyle("-fx-background-color: #667eea22; -fx-text-fill: #667eea; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 4 10;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusLabel = new Label("• " + (meeting.getStatus() != null ? meeting.getStatus() : "scheduled"));
        String statusColor = getStatusColor(meeting.getStatus());
        statusLabel.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        header.getChildren().addAll(typeLabel, spacer, statusLabel);

        // Title
        Label titleLabel = new Label(meeting.getTitle() != null ? meeting.getTitle() : "No title");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #1e1e1e;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(300);

        // Description
        Label descLabel = new Label(meeting.getDescription() != null && !meeting.getDescription().isEmpty()
            ? meeting.getDescription() : "No description");
        descLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(300);
        descLabel.setMaxHeight(60);

        // Project
        HBox projectBox = new HBox(6);
        projectBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label projectIcon = new Label("[P]");
        projectIcon.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #a12c2f;");
        Label projectLabel = new Label("Project: " + (meeting.getProjectTitle() != null ? meeting.getProjectTitle() : "N/A"));
        projectLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        projectBox.getChildren().addAll(projectIcon, projectLabel);

        // Date
        HBox dateBox = new HBox(6);
        dateBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label dateIcon = new Label("[D]");
        dateIcon.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #667eea;");
        String dateText = meeting.getScheduledDate() != null
            ? meeting.getScheduledDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            : "Not scheduled";
        Label dateLabel = new Label(dateText);
        dateLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        dateBox.getChildren().addAll(dateIcon, dateLabel);

        // Location
        if (meeting.getLocation() != null && !meeting.getLocation().isEmpty()) {
            HBox locationBox = new HBox(6);
            locationBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label locationIcon = new Label("[L]");
            locationIcon.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #a12c2f;");
            Label locationLabel = new Label(meeting.getLocation());
            locationLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
            locationBox.getChildren().addAll(locationIcon, locationLabel);
            card.getChildren().add(locationBox);
        }

        // Link
        if (meeting.getMeetingLink() != null && !meeting.getMeetingLink().isEmpty()) {
            HBox linkBox = new HBox(6);
            linkBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label linkIcon = new Label("[LINK]");
            linkIcon.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #3b82f6;");
            Label linkLabel = new Label("Online meeting");
            linkLabel.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 11px; -fx-text-decoration: underline; -fx-cursor: hand;");
            linkBox.getChildren().addAll(linkIcon, linkLabel);
            card.getChildren().add(linkBox);
        }

        Separator sep = new Separator();

        // Actions
        HBox actions = new HBox(8);
        actions.setAlignment(javafx.geometry.Pos.CENTER);

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 6; -fx-padding: 6 14; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> handleDeleteMeeting(meeting));

        actions.getChildren().add(deleteBtn);

        card.getChildren().addAll(header, titleLabel, descLabel, projectBox, dateBox, sep, actions);
        return card;
    }

    private String getStatusColor(String status) {
        if (status == null) {
            return "#888";
        }
        String statusLower = status.toLowerCase();
        if (statusLower.equals("completed")) {
            return "#22c55e";
        } else if (statusLower.equals("cancelled")) {
            return "#ef4444";
        } else {
            return "#667eea";
        }
    }

    @FXML
    public void handleAddMeeting() {
        showAlert("Info", "Meeting form coming soon!", Alert.AlertType.INFORMATION);
    }

    @FXML
    public void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        String typeFilter = filterType.getValue();
        String statusFilter = filterStatus.getValue();

        List<Meeting> filtered = new ArrayList<>();
        for (Meeting m : allMeetings) {
            boolean matchQuery = query.isEmpty() ||
                (m.getTitle() != null && m.getTitle().toLowerCase().contains(query)) ||
                (m.getProjectTitle() != null && m.getProjectTitle().toLowerCase().contains(query)) ||
                (m.getDescription() != null && m.getDescription().toLowerCase().contains(query));
            boolean matchType = typeFilter.equals("All Types") ||
                (m.getMeetingType() != null && m.getMeetingType().equals(typeFilter));
            boolean matchStatus = statusFilter.equals("All Status") ||
                (m.getStatus() != null && m.getStatus().equals(statusFilter));

            if (matchQuery && matchType && matchStatus) {
                filtered.add(m);
            }
        }

        displayMeetings(filtered);
    }

    @FXML
    public void handleReset() {
        searchField.clear();
        filterType.setValue("All Types");
        filterStatus.setValue("All Status");
        displayMeetings(allMeetings);
    }

    private void handleDeleteMeeting(Meeting meeting) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Meeting");
        confirm.setHeaderText("Delete this meeting?");
        confirm.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement("DELETE FROM meetings WHERE id=?");
                ps.setInt(1, meeting.getId());
                ps.executeUpdate();
                showAlert("Success", "Meeting deleted successfully", Alert.AlertType.INFORMATION);
                loadMeetings();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to delete meeting: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
