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
    @FXML private Label statusLabel;
    @FXML private TextArea motivationField;
    @FXML private TextArea feedbackField;

    private Candidature selectedCandidature;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colStudent.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colOffer.setCellValueFactory(new PropertyValueFactory<>("offerTitle"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        loadCandidatures();

        candidatureTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                selectedCandidature = sel;
                studentLabel.setText(sel.getStudentName());
                offerLabel.setText(sel.getOfferTitle());
                statusLabel.setText(sel.getStatus() != null ? sel.getStatus().toUpperCase() : "");
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
        } catch (Exception e) { 
            e.printStackTrace();
        }
    }
}
