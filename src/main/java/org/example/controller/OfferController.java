package org.example.controller;

import org.example.dao.CandidatureDAO;
import org.example.dao.OfferDAO;
import org.example.model.Candidature;
import org.example.model.Offer;
import org.example.model.User;
import org.example.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import org.example.util.ModernAlert;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class OfferController implements Initializable {

    @FXML private FlowPane offersContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatus;
    @FXML private Label statTotal;
    @FXML private Label statOpen;
    @FXML private Label statClosed;

    private final OfferDAO offerDAO = new OfferDAO();
    private final CandidatureDAO candidatureDAO = new CandidatureDAO();
    private List<Offer> allOffers = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filterStatus.setItems(FXCollections.observableArrayList("All", "open", "closed", "draft"));
        filterStatus.setValue("All");
        filterStatus.valueProperty().addListener((obs, o, v) -> applyFilter());
        searchField.textProperty().addListener((obs, o, v) -> applyFilter());

        loadOffers();
    }

    private void loadOffers() {
        try {
            allOffers = offerDAO.findAll();
            updateStats();
            applyFilter();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStats() {
        statTotal.setText(String.valueOf(allOffers.size()));
        long open = allOffers.stream().filter(o -> "open".equals(o.getStatus()) || o.getStatus() == null).count();
        long closed = allOffers.stream().filter(o -> "closed".equals(o.getStatus())).count();
        statOpen.setText(String.valueOf(open));
        statClosed.setText(String.valueOf(closed));
    }

    private void applyFilter() {
        String query = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String status = filterStatus.getValue();
        List<Offer> filtered = new ArrayList<>();
        for (Offer o : allOffers) {
            boolean matchQ = query.isEmpty() || (o.getTitle() != null && o.getTitle().toLowerCase().contains(query));
            boolean matchS = "All".equals(status) || status.equals(o.getStatus())
                || ("open".equals(status) && o.getStatus() == null);
            if (matchQ && matchS) filtered.add(o);
        }
        displayOffers(filtered);
    }

    private void displayOffers(List<Offer> offers) {
        offersContainer.getChildren().clear();
        if (offers.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(Pos.CENTER);
            empty.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e0e0e0; -fx-border-radius: 12; -fx-border-width: 2; -fx-border-style: dashed;");
            empty.setPrefSize(400, 180);
            empty.setPadding(new Insets(30));
            Label lbl = new Label("No offers yet. Click '+ Add Offer' to create one.");
            lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #999;");
            empty.getChildren().add(lbl);
            offersContainer.getChildren().add(empty);
            return;
        }
        for (Offer o : offers) offersContainer.getChildren().add(createOfferCard(o));
    }

    private VBox createOfferCard(Offer offer) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 3); -fx-border-color: #f0f0f0; -fx-border-radius: 14; -fx-border-width: 1;");
        card.setPrefWidth(340);
        card.setPadding(new Insets(20));

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        String statusColor = "open".equals(offer.getStatus()) || offer.getStatus() == null ? "#22c55e" : "#888";
        Label statusLabel = new Label(offer.getStatus() != null ? offer.getStatus().toUpperCase() : "OPEN");
        statusLabel.setStyle("-fx-background-color: " + statusColor + "22; -fx-text-fill: " + statusColor + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 4 10;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label dateLabel = new Label(offer.getCreatedAt() != null ? offer.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
        dateLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10px;");
        header.getChildren().addAll(statusLabel, spacer, dateLabel);

        Label title = new Label(offer.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #1e1e1e;");
        title.setWrapText(true);

        Label desc = new Label(offer.getDescription() != null ? offer.getDescription() : "");
        desc.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
        desc.setWrapText(true);
        desc.setMaxHeight(50);

        card.getChildren().addAll(header, title, desc);

        if (offer.getDeadline() != null) {
            Label deadline = new Label("Deadline: " + offer.getDeadline().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            deadline.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-font-weight: bold;");
            card.getChildren().add(deadline);
        }

        if (offer.getRequiredSkills() != null && !offer.getRequiredSkills().isEmpty()) {
            Label skills = new Label("Skills: " + offer.getRequiredSkills());
            skills.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
            skills.setWrapText(true);
            card.getChildren().add(skills);
        }

        Separator sep = new Separator();

        // Actions
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);

        Button candidaturesBtn = new Button("Candidatures");
        candidaturesBtn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 6; -fx-padding: 6 12; -fx-cursor: hand;");
        try {
            int count = candidatureDAO.findByOffer(offer.getId()).size();
            candidaturesBtn.setText("Candidatures (" + count + ")");
        } catch (Exception ignored) {}
        candidaturesBtn.setOnAction(e -> openCandidaturesDialog(offer));

        actions.getChildren().add(candidaturesBtn);
        card.getChildren().addAll(sep, actions);
        return card;
    }

    private void openCandidaturesDialog(Offer offer) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/CandidaturesDialog.fxml"));
            VBox content = loader.load();
            CandidaturesDialogController ctrl = loader.getController();
            ctrl.setOffer(offer);

            Stage dialog = new Stage();
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.initStyle(javafx.stage.StageStyle.UNDECORATED);
            javafx.scene.Scene scene = new javafx.scene.Scene(content);
            scene.setFill(javafx.scene.paint.Color.WHITE);
            dialog.setScene(scene);
            ctrl.setDialogStage(dialog);
            dialog.showAndWait();
            loadOffers();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML public void handleReset() {
        searchField.clear();
        filterStatus.setValue("All");
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
