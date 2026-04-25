package org.example.controller;

import org.example.dao.CandidatureDAO;
import org.example.dao.OfferDAO;
import org.example.dao.UserDAO;
import org.example.model.Offer;
import org.example.model.User;
import org.example.util.NavigationUtil;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class ProjectOfferController implements Initializable {

    @FXML private TableView<Offer> offerTable;
    @FXML private TableColumn<Offer, Integer> colId;
    @FXML private TableColumn<Offer, String> colTitle;
    @FXML private TableColumn<Offer, String> colStatus;
    @FXML private TableColumn<Offer, String> colDeadline;
    @FXML private TableColumn<Offer, Integer> colMax;
    @FXML private TableColumn<Offer, Integer> colCandidatures;

    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private TextArea objectivesField;
    @FXML private TextArea requiredSkillsField;
    @FXML private Spinner<Integer> maxCandidatesSpinner;
    @FXML private DatePicker deadlinePicker;
    @FXML private ComboBox<String> statusCombo;
    @FXML private ComboBox<User> supervisorCombo;
    @FXML private Label messageLabel;

    // Stats
    @FXML private Label statTotal;
    @FXML private Label statOpen;
    @FXML private Label statCandidatures;

    private final OfferDAO offerDAO = new OfferDAO();
    private final CandidatureDAO candidatureDAO = new CandidatureDAO();
    private final UserDAO userDAO = new UserDAO();
    private Offer selectedOffer;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        statusCombo.setItems(FXCollections.observableArrayList("draft", "open", "published", "closed"));
        maxCandidatesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 10));

        try {
            List<User> supervisors = userDAO.findByRole("supervisor");
            supervisorCombo.setItems(FXCollections.observableArrayList(supervisors));
        } catch (Exception e) { e.printStackTrace(); }

        loadOffers();
        loadStats();

        offerTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) populateForm(sel);
        });

        offerTable.setRowFactory(tv -> {
            TableRow<Offer> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) openCandidatures(row.getItem());
            });
            return row;
        });
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDeadline.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getDeadline() != null ? d.getValue().getDeadline().toString() : "-"));
        colMax.setCellValueFactory(new PropertyValueFactory<>("maxCandidates"));
        colCandidatures.setCellValueFactory(d -> {
            try { return new SimpleIntegerProperty(offerDAO.getCandidatureCount(d.getValue().getId())).asObject(); }
            catch (Exception e) { return new SimpleIntegerProperty(0).asObject(); }
        });
    }

    private void loadOffers() {
        try { offerTable.setItems(FXCollections.observableArrayList(offerDAO.findAll())); }
        catch (Exception e) { showMessage("Error loading offers: " + e.getMessage(), true); }
    }

    private void loadStats() {
        try {
            Map<String, Integer> stats = offerDAO.getStats();
            statTotal.setText(String.valueOf(stats.getOrDefault("total", 0)));
            statOpen.setText(String.valueOf(stats.getOrDefault("open", 0)));
            statCandidatures.setText(String.valueOf(stats.getOrDefault("totalCandidatures", 0)));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void populateForm(Offer o) {
        selectedOffer = o;
        titleField.setText(o.getTitle());
        descriptionField.setText(o.getDescription() != null ? o.getDescription() : "");
        objectivesField.setText(o.getObjectives() != null ? o.getObjectives() : "");
        requiredSkillsField.setText(o.getRequiredSkills() != null ? o.getRequiredSkills() : "");
        maxCandidatesSpinner.getValueFactory().setValue(o.getMaxCandidates() > 0 ? o.getMaxCandidates() : 10);
        deadlinePicker.setValue(o.getDeadline());
        statusCombo.setValue(o.getStatus());
    }

    @FXML
    public void handleSave() {
        if (titleField.getText().trim().isEmpty()) { showMessage("Title is required.", true); return; }
        try {
            Offer o = selectedOffer != null ? selectedOffer : new Offer();
            o.setTitle(titleField.getText().trim());
            o.setDescription(descriptionField.getText());
            o.setObjectives(objectivesField.getText());
            o.setRequiredSkills(requiredSkillsField.getText());
            o.setMaxCandidates(maxCandidatesSpinner.getValue());
            o.setDeadline(deadlinePicker.getValue());
            o.setStatus(statusCombo.getValue() != null ? statusCombo.getValue() : "open");
            if (supervisorCombo.getValue() != null) o.setAssignedSupervisorId(supervisorCombo.getValue().getId());

            if (selectedOffer == null) offerDAO.save(o);
            else offerDAO.update(o);

            showMessage("Offer saved successfully.", false);
            handleClear();
            loadOffers();
            loadStats();
        } catch (Exception e) { showMessage("Error: " + e.getMessage(), true); }
    }

    @FXML
    public void handleDelete() {
        if (selectedOffer == null) { showMessage("Select an offer first.", true); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this offer?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    offerDAO.delete(selectedOffer.getId());
                    showMessage("Offer deleted.", false);
                    handleClear();
                    loadOffers();
                    loadStats();
                } catch (Exception e) { showMessage("Error: " + e.getMessage(), true); }
            }
        });
    }

    @FXML
    public void handleViewCandidatures() {
        if (selectedOffer == null) { showMessage("Select an offer first.", true); return; }
        openCandidatures(selectedOffer);
    }

    @FXML
    public void handleViewStats() {
        if (selectedOffer == null) { showMessage("Select an offer first.", true); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/OfferStatistics.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Statistics - " + selectedOffer.getTitle());
            stage.setScene(new Scene(loader.load(), 800, 600));
            OfferStatisticsController ctrl = loader.getController();
            ctrl.loadForOffer(selectedOffer);
            stage.show();
        } catch (Exception e) { showMessage("Error: " + e.getMessage(), true); }
    }

    private void openCandidatures(Offer offer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CandidaturesByOffer.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Candidatures - " + offer.getTitle());
            stage.setScene(new Scene(loader.load(), 1100, 700));
            CandidaturesByOfferController ctrl = loader.getController();
            ctrl.loadForOffer(offer);
            stage.show();
        } catch (Exception e) { showMessage("Error opening candidatures: " + e.getMessage(), true); }
    }

    @FXML
    public void handleClear() {
        selectedOffer = null;
        titleField.clear(); descriptionField.clear(); objectivesField.clear(); requiredSkillsField.clear();
        statusCombo.setValue(null); deadlinePicker.setValue(null); supervisorCombo.setValue(null);
        maxCandidatesSpinner.getValueFactory().setValue(10);
        offerTable.getSelectionModel().clearSelection();
    }

    private void showMessage(String msg, boolean isError) {
        messageLabel.setText(msg);
        messageLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
}
