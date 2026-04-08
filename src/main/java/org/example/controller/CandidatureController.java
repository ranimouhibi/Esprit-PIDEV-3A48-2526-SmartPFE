package org.example.controller;

import org.example.config.DatabaseConfig;
import org.example.model.Candidature;
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

public class CandidatureController implements Initializable {

    @FXML private TableView<Candidature> candidatureTable;
    @FXML private TableColumn<Candidature, Integer> colId;
    @FXML private TableColumn<Candidature, String> colStudent;
    @FXML private TableColumn<Candidature, String> colOffer;
    @FXML private TableColumn<Candidature, String> colStatus;
    @FXML private TableColumn<Candidature, LocalDateTime> colDate;

    @FXML private Label studentLabel;
    @FXML private Label offerLabel;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextArea motivationField;
    @FXML private TextArea feedbackField;
    @FXML private Label messageLabel;

    private Candidature selectedCandidature;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colStudent.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colOffer.setCellValueFactory(new PropertyValueFactory<>("offerTitle"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        statusCombo.setItems(FXCollections.observableArrayList("pending", "accepted", "rejected", "interview"));

        loadCandidatures();

        candidatureTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                selectedCandidature = sel;
                studentLabel.setText(sel.getStudentName());
                offerLabel.setText(sel.getOfferTitle());
                statusCombo.setValue(sel.getStatus());
                motivationField.setText(sel.getMotivationLetter());
                feedbackField.setText(sel.getFeedback());
            }
        });
    }

    private void loadCandidatures() {
        try {
            List<Candidature> list = new ArrayList<>();
            String sql = "SELECT c.*, u.name as student_name, o.title as offer_title " +
                         "FROM candidatures c " +
                         "LEFT JOIN users u ON c.student_id = u.id " +
                         "LEFT JOIN project_offers o ON c.offer_id = o.id " +
                         "ORDER BY c.created_at DESC";
            ResultSet rs = DatabaseConfig.getConnection().createStatement().executeQuery(sql);
            while (rs.next()) {
                Candidature c = new Candidature();
                c.setId(rs.getInt("id"));
                c.setStudentId(rs.getInt("student_id"));
                c.setStudentName(rs.getString("student_name"));
                c.setOfferId(rs.getInt("offer_id"));
                c.setOfferTitle(rs.getString("offer_title"));
                c.setStatus(rs.getString("status"));
                c.setMotivationLetter(rs.getString("motivation_letter"));
                c.setFeedback(rs.getString("feedback"));
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) c.setCreatedAt(ts.toLocalDateTime());
                list.add(c);
            }
            candidatureTable.setItems(FXCollections.observableArrayList(list));
        } catch (Exception e) { showMessage("Erreur: " + e.getMessage(), true); }
    }

    @FXML
    public void handleUpdate() {
        if (selectedCandidature == null) { showMessage("Sélectionnez une candidature.", true); return; }
        try {
            String sql = "UPDATE candidatures SET status=?, feedback=?, updated_at=? WHERE id=?";
            PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql);
            ps.setString(1, statusCombo.getValue());
            ps.setString(2, feedbackField.getText());
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(4, selectedCandidature.getId());
            ps.executeUpdate();
            showMessage("Candidature mise à jour.", false);
            loadCandidatures();
        } catch (Exception e) { showMessage("Erreur: " + e.getMessage(), true); }
    }

    @FXML
    public void handleDelete() {
        if (selectedCandidature == null) { showMessage("Sélectionnez une candidature.", true); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cette candidature?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement("DELETE FROM candidatures WHERE id=?");
                    ps.setInt(1, selectedCandidature.getId());
                    ps.executeUpdate();
                    showMessage("Candidature supprimée.", false);
                    selectedCandidature = null;
                    loadCandidatures();
                } catch (Exception e) { showMessage("Erreur: " + e.getMessage(), true); }
            }
        });
    }

    private void showMessage(String msg, boolean isError) {
        messageLabel.setText(msg);
        messageLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
}
