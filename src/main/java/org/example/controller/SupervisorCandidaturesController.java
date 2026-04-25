package org.example.controller;

import org.example.config.DatabaseConfig;
import org.example.dao.CandidatureDAO;
import org.example.dao.MatchingScoreDAO;
import org.example.dao.OfferDAO;
import org.example.model.Candidature;
import org.example.model.MatchingScore;
import org.example.model.Offer;
import org.example.model.User;
import org.example.service.AIMatchingService;
import org.example.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class SupervisorCandidaturesController implements Initializable {

    @FXML private TableView<Candidature> candidatureTable;
    @FXML private TableColumn<Candidature, Integer> colId;
    @FXML private TableColumn<Candidature, String> colStudent;
    @FXML private TableColumn<Candidature, String> colOffer;
    @FXML private TableColumn<Candidature, String> colStatus;
    @FXML private TableColumn<Candidature, String> colScore;
    @FXML private TableColumn<Candidature, String> colLevel;
    @FXML private TableColumn<Candidature, String> colDate;
    @FXML private ComboBox<Offer> offerFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TextField searchField;
    @FXML private Label statusLabel;

    private final OfferDAO offerDAO = new OfferDAO();
    private final CandidatureDAO candidatureDAO = new CandidatureDAO();
    private final MatchingScoreDAO matchingScoreDAO = new MatchingScoreDAO();
    private final AIMatchingService aiService = new AIMatchingService();

    private List<Candidature> allCandidatures = new ArrayList<>();
    private List<Offer> assignedOffers = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        statusFilter.setItems(FXCollections.observableArrayList("All", "pending", "accepted", "rejected", "interview"));
        statusFilter.setValue("All");
        statusFilter.valueProperty().addListener((obs, o, v) -> applyFilter());
        searchField.textProperty().addListener((obs, o, v) -> applyFilter());
        offerFilter.valueProperty().addListener((obs, o, v) -> applyFilter());
        candidatureTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        candidatureTable.setRowFactory(tv -> {
            TableRow<Candidature> row = new TableRow<>();
            row.setOnMouseClicked(e -> { if (e.getClickCount() == 2 && !row.isEmpty()) openDetail(row.getItem()); });
            return row;
        });
        loadData();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colStudent.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colOffer.setCellValueFactory(new PropertyValueFactory<>("offerTitle"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDate.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().toLocalDate().toString() : "-"));
        colScore.setCellValueFactory(d -> {
            try {
                MatchingScore ms = matchingScoreDAO.findByCandidature(d.getValue().getId());
                return new SimpleStringProperty(ms != null ? String.format("%.1f%%", ms.getScore()) : "-");
            } catch (Exception e) { return new SimpleStringProperty("-"); }
        });
        colLevel.setCellValueFactory(d -> {
            try {
                MatchingScore ms = matchingScoreDAO.findByCandidature(d.getValue().getId());
                return new SimpleStringProperty(ms != null && ms.getMatchLevel() != null ? ms.getMatchLevel().toUpperCase() : "-");
            } catch (Exception e) { return new SimpleStringProperty("-"); }
        });
    }

    private void loadData() {
        try {
            User user = SessionManager.getCurrentUser();
            // Load offers assigned to this supervisor
            assignedOffers = loadAssignedOffers(user.getId());

            List<Offer> filterItems = new ArrayList<>();
            filterItems.add(null);
            filterItems.addAll(assignedOffers);
            offerFilter.setItems(FXCollections.observableArrayList(filterItems));
            offerFilter.setConverter(new javafx.util.StringConverter<>() {
                public String toString(Offer o) { return o == null ? "All Offers" : o.getTitle(); }
                public Offer fromString(String s) { return null; }
            });

            allCandidatures.clear();
            for (Offer o : assignedOffers) {
                allCandidatures.addAll(candidatureDAO.findByOffer(o.getId()));
            }
            applyFilter();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private List<Offer> loadAssignedOffers(int supervisorId) {
        List<Offer> list = new ArrayList<>();
        try {
            // Offers where assigned_supervisor_id = this supervisor
            String sql = "SELECT o.*, u.name as establishment_name FROM project_offers o " +
                         "LEFT JOIN users u ON o.establishment_id = u.establishment_id " +
                         "WHERE o.assigned_supervisor_id = ? ORDER BY o.created_at DESC";
            try (java.sql.PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
                ps.setInt(1, supervisorId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Offer o = new Offer();
                    o.setId(rs.getInt("id"));
                    o.setTitle(rs.getString("title"));
                    o.setDescription(rs.getString("description"));
                    o.setRequiredSkills(rs.getString("required_skills"));
                    o.setStatus(rs.getString("status"));
                    o.setAssignedSupervisorId(supervisorId);
                    list.add(o);
                }
            }
            // Also include offers from projects supervised by this person
            if (list.isEmpty()) {
                list = offerDAO.findAll(); // fallback: show all if none assigned
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    private void applyFilter() {
        String status = statusFilter.getValue();
        String query = searchField.getText() != null ? searchField.getText().toLowerCase() : "";
        Offer selectedOffer = offerFilter.getValue();

        List<Candidature> filtered = allCandidatures.stream()
            .filter(c -> selectedOffer == null || c.getOfferId() == selectedOffer.getId())
            .filter(c -> "All".equals(status) || status.equals(c.getStatus()))
            .filter(c -> query.isEmpty() || (c.getStudentName() != null && c.getStudentName().toLowerCase().contains(query)))
            .toList();

        candidatureTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML public void handleRefresh() { loadData(); }

    @FXML public void handleViewDetail() {
        Candidature sel = candidatureTable.getSelectionModel().getSelectedItem();
        if (sel != null) openDetail(sel);
        else statusLabel.setText("Select a candidature first.");
    }

    @FXML public void handleCalculateScore() {
        Candidature sel = candidatureTable.getSelectionModel().getSelectedItem();
        if (sel == null) { statusLabel.setText("Select a candidature first."); return; }
        statusLabel.setText("Calculating...");
        new Thread(() -> {
            try {
                aiService.calculateMatchingScore(sel.getId());
                Platform.runLater(() -> { statusLabel.setText("Score calculated."); candidatureTable.refresh(); });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    @FXML public void handleCompare() {
        ObservableList<Candidature> selected = candidatureTable.getSelectionModel().getSelectedItems();
        if (selected.size() < 2) { statusLabel.setText("Select 2-3 candidatures (Ctrl+Click)."); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CandidatureCompare.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Compare Candidatures");
            stage.setScene(new Scene(loader.load(), 1000, 650));
            CandidatureCompareController ctrl = loader.getController();
            ctrl.loadCandidatures(selected.subList(0, Math.min(3, selected.size())));
            stage.show();
        } catch (Exception e) { statusLabel.setText("Error: " + e.getMessage()); }
    }

    private void openDetail(Candidature c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CandidatureDetail.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Candidature - " + c.getStudentName());
            stage.setScene(new Scene(loader.load(), 900, 750));
            CandidatureDetailController ctrl = loader.getController();
            ctrl.loadCandidature(c);
            stage.showAndWait();
            candidatureTable.refresh();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
