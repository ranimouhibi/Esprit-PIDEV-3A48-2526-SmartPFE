package org.example.controller;

import org.example.dao.CandidatureDAO;
import org.example.dao.MatchingScoreDAO;
import org.example.model.Candidature;
import org.example.model.Offer;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OfferStatisticsController {

    @FXML private Label lblOfferTitle;
    @FXML private Label lblTotal;
    @FXML private Label lblPending;
    @FXML private Label lblAccepted;
    @FXML private Label lblRejected;
    @FXML private Label lblAvgScore;

    // Distribution bars
    @FXML private Region barExcellent;
    @FXML private Region barGood;
    @FXML private Region barFair;
    @FXML private Region barLow;
    @FXML private Label lblExcellent;
    @FXML private Label lblGood;
    @FXML private Label lblFair;
    @FXML private Label lblLow;

    // Top candidates table
    @FXML private TableView<Candidature> topTable;
    @FXML private TableColumn<Candidature, String> colStudent;
    @FXML private TableColumn<Candidature, String> colStatus;
    @FXML private TableColumn<Candidature, Double> colScore;
    @FXML private TableColumn<Candidature, String> colLevel;

    private final CandidatureDAO candidatureDAO = new CandidatureDAO();
    private final MatchingScoreDAO matchingScoreDAO = new MatchingScoreDAO();

    public void loadForOffer(Offer offer) {
        lblOfferTitle.setText("Statistics: " + offer.getTitle());

        try {
            List<Candidature> all = candidatureDAO.findByOffer(offer.getId());
            lblTotal.setText(String.valueOf(all.size()));
            lblPending.setText(String.valueOf(all.stream().filter(c -> "pending".equals(c.getStatus())).count()));
            lblAccepted.setText(String.valueOf(all.stream().filter(c -> "accepted".equals(c.getStatus())).count()));
            lblRejected.setText(String.valueOf(all.stream().filter(c -> "rejected".equals(c.getStatus())).count()));

            // Score stats
            Map<String, Object> scoreStats = matchingScoreDAO.getStatistics(offer.getId());
            double avg = (double) scoreStats.getOrDefault("avgScore", 0.0);
            lblAvgScore.setText(String.format("%.1f%%", avg));

            int excellent = (int) scoreStats.getOrDefault("excellent", 0);
            int good = (int) scoreStats.getOrDefault("good", 0);
            int fair = (int) scoreStats.getOrDefault("fair", 0);
            int low = (int) scoreStats.getOrDefault("low", 0);
            int total = excellent + good + fair + low;

            lblExcellent.setText(excellent + " Excellent");
            lblGood.setText(good + " Good");
            lblFair.setText(fair + " Fair");
            lblLow.setText(low + " Low");

            if (total > 0) {
                barExcellent.setPrefWidth(200.0 * excellent / total);
                barGood.setPrefWidth(200.0 * good / total);
                barFair.setPrefWidth(200.0 * fair / total);
                barLow.setPrefWidth(200.0 * low / total);
            }

            // Top 5 by score
            colStudent.setCellValueFactory(new PropertyValueFactory<>("studentName"));
            colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            colScore.setCellValueFactory(d -> {
                try {
                    var ms = matchingScoreDAO.findByCandidature(d.getValue().getId());
                    return new SimpleDoubleProperty(ms != null ? ms.getScore() : 0).asObject();
                } catch (Exception e) { return new SimpleDoubleProperty(0).asObject(); }
            });
            colLevel.setCellValueFactory(d -> {
                try {
                    var ms = matchingScoreDAO.findByCandidature(d.getValue().getId());
                    return new SimpleStringProperty(ms != null && ms.getMatchLevel() != null ? ms.getMatchLevel() : "-");
                } catch (Exception e) { return new SimpleStringProperty("-"); }
            });

            // Sort by score desc, take top 5
            List<Candidature> top5 = all.stream()
                .sorted(Comparator.comparingDouble((Candidature c) -> {
                    try {
                        var ms = matchingScoreDAO.findByCandidature(c.getId());
                        return ms != null ? ms.getScore() : 0;
                    } catch (Exception e) { return 0; }
                }).reversed())
                .limit(5)
                .collect(Collectors.toList());

            topTable.setItems(FXCollections.observableArrayList(top5));

            // Color rows by match level
            topTable.setRowFactory(tv -> new TableRow<>() {
                @Override
                protected void updateItem(Candidature item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) { setStyle(""); return; }
                    try {
                        var ms = matchingScoreDAO.findByCandidature(item.getId());
                        if (ms != null && "excellent".equals(ms.getMatchLevel()))
                            setStyle("-fx-background-color: #f0fff4;");
                        else setStyle("");
                    } catch (Exception e) { setStyle(""); }
                }
            });

        } catch (Exception e) { e.printStackTrace(); }
    }
}
