package org.example.controller;

import org.example.dao.CandidatureDAO;
import org.example.model.Candidature;
import org.example.model.Offer;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import org.example.util.ModernAlert;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class CandidaturesByOfferController {

    @FXML private Label offerTitleLabel;
    @FXML private TableView<Candidature> candidatureTable;
    @FXML private TableColumn<Candidature, Integer> colId;
    @FXML private TableColumn<Candidature, String> colStudent;
    @FXML private TableColumn<Candidature, String> colStatus;
    @FXML private TableColumn<Candidature, String> colDate;
    @FXML private Button btnCompare;

    private final CandidatureDAO candidatureDAO = new CandidatureDAO();
    private Offer currentOffer;

    public void loadForOffer(Offer offer) {
        this.currentOffer = offer;
        offerTitleLabel.setText("Candidatures for: " + offer.getTitle());
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colStudent.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDate.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().toLocalDate().toString() : "-"));

        try {
            List<Candidature> list = candidatureDAO.findByOffer(offer.getId());
            candidatureTable.setItems(FXCollections.observableArrayList(list));
        } catch (Exception e) { e.printStackTrace(); }

        candidatureTable.setRowFactory(tv -> {
            TableRow<Candidature> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) openDetail(row.getItem());
            });
            return row;
        });
    }

    @FXML
    public void handleOpenDetail() {
        Candidature sel = candidatureTable.getSelectionModel().getSelectedItem();
        if (sel != null) openDetail(sel);
    }

    @FXML
    public void handleCompare() {
        List<Candidature> selected = candidatureTable.getSelectionModel().getSelectedItems();
        if (selected.size() < 2) {
            ModernAlert.show(ModernAlert.Type.INFO, "Information", "Select 2 or 3 candidatures to compare (hold Ctrl).");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CandidatureCompare.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Compare Candidatures");
            stage.setScene(new Scene(loader.load(), 1000, 650));
            CandidatureCompareController ctrl = loader.getController();
            ctrl.loadCandidatures(selected.subList(0, Math.min(3, selected.size())));
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void openDetail(Candidature c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CandidatureDetail.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Candidature Detail - " + c.getStudentName());
            stage.setScene(new Scene(loader.load(), 900, 750));
            CandidatureDetailController ctrl = loader.getController();
            ctrl.loadCandidature(c);
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
