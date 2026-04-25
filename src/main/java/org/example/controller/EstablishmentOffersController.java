package org.example.controller;

import org.example.dao.CandidatureDAO;
import org.example.dao.OfferDAO;
import org.example.model.Offer;
import org.example.model.User;
import org.example.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class EstablishmentOffersController implements Initializable {

    @FXML private FlowPane offersContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatus;
    @FXML private Label statTotal;
    @FXML private Label statOpen;
    @FXML private Label statClosed;

    // Form
    @FXML private VBox formContainer;
    @FXML private Label formTitle;
    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private TextArea objectivesField;
    @FXML private TextArea skillsField;
    @FXML private TextField maxCandidatesField;
    @FXML private TextField deadlineField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Label titleError;
    @FXML private Label descError;
    @FXML private Button addBtn;

    private final OfferDAO offerDAO = new OfferDAO();
    private final CandidatureDAO candidatureDAO = new CandidatureDAO();
    private List<Offer> allOffers = new ArrayList<>();
    private Offer editingOffer = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filterStatus.setItems(FXCollections.observableArrayList("All", "open", "closed", "draft"));
        filterStatus.setValue("All");
        filterStatus.valueProperty().addListener((obs, o, v) -> applyFilter());
        searchField.textProperty().addListener((obs, o, v) -> applyFilter());

        statusCombo.setItems(FXCollections.observableArrayList("open", "closed", "draft"));

        // Input restrictions
        titleField.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() > 150) titleField.setText(old);
            if (val != null && !val.isEmpty() && !val.matches("[a-zA-Z0-9\\s\\p{Punct}\u00C0-\u017F]*"))
                titleField.setText(old);
        });
        maxCandidatesField.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.matches("\\d*")) maxCandidatesField.setText(old);
            if (val != null && val.length() > 3) maxCandidatesField.setText(old);
        });
        deadlineField.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() > 10) deadlineField.setText(old);
            if (val != null && !val.isEmpty() && !val.matches("[0-9/]*")) deadlineField.setText(old);
        });
        descriptionField.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() > 1000) descriptionField.setText(old);
            if (val != null && !val.isEmpty() && !val.matches("[a-zA-Z0-9\\s\\p{Punct}\u00C0-\u017F\\n\\r]*"))
                descriptionField.setText(old);
        });
        objectivesField.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() > 1000) objectivesField.setText(old);
            if (val != null && !val.isEmpty() && !val.matches("[a-zA-Z0-9\\s\\p{Punct}\u00C0-\u017F\\n\\r]*"))
                objectivesField.setText(old);
        });
        skillsField.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() > 500) skillsField.setText(old);
            if (val != null && !val.isEmpty() && !val.matches("[a-zA-Z0-9\\s\\p{Punct}\u00C0-\u017F\\n\\r]*"))
                skillsField.setText(old);
        });

        loadOffers();
    }

    private void loadOffers() {
        try {
            User user = SessionManager.getCurrentUser();
            int estId = user.getEstablishmentId();
            if (estId == 0) {
                // fallback: try to find establishment by user id
                estId = resolveEstablishmentId(user.getId());
            }
            allOffers = offerDAO.findByEstablishment(estId);
            updateStats();
            applyFilter();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int resolveEstablishmentId(int userId) {
        try {
            java.sql.ResultSet rs = org.example.config.DatabaseConfig.getConnection()
                .createStatement()
                .executeQuery("SELECT id FROM establishments WHERE id = (SELECT establishment_id FROM users WHERE id = " + userId + ")");
            if (rs.next()) return rs.getInt(1);
        } catch (Exception ignored) {}
        return userId; // last resort
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

        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 6; -fx-padding: 6 12; -fx-cursor: hand;");
        editBtn.setOnAction(e -> handleEdit(offer));

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 6; -fx-padding: 6 12; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> handleDelete(offer));

        actions.getChildren().addAll(candidaturesBtn, editBtn, deleteBtn);
        card.getChildren().addAll(sep, actions);
        return card;
    }

    // ── Form ─────────────────────────────────────────────────────────────────

    @FXML public void handleToggleForm() {
        boolean visible = formContainer.isVisible();
        if (!visible) {
            editingOffer = null;
            clearForm();
            formTitle.setText("New Offer");
            formContainer.setVisible(true);
            formContainer.setManaged(true);
            addBtn.setText("Cancel");
        } else {
            hideForm();
        }
    }

    private void handleEdit(Offer offer) {
        editingOffer = offer;
        formTitle.setText("Edit Offer");
        titleField.setText(offer.getTitle() != null ? offer.getTitle() : "");
        descriptionField.setText(offer.getDescription() != null ? offer.getDescription() : "");
        objectivesField.setText(offer.getObjectives() != null ? offer.getObjectives() : "");
        skillsField.setText(offer.getRequiredSkills() != null ? offer.getRequiredSkills() : "");
        maxCandidatesField.setText(String.valueOf(offer.getMaxCandidates()));
        deadlineField.setText(offer.getDeadline() != null ? offer.getDeadline().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
        statusCombo.setValue(offer.getStatus() != null ? offer.getStatus() : "open");
        formContainer.setVisible(true);
        formContainer.setManaged(true);
        addBtn.setText("Cancel");
    }

    @FXML public void handleCancelForm() { hideForm(); }

    private void hideForm() {
        formContainer.setVisible(false);
        formContainer.setManaged(false);
        addBtn.setText("+ Add Offer");
        editingOffer = null;
        clearForm();
    }

    private void clearForm() {
        titleField.clear();
        descriptionField.clear();
        objectivesField.clear();
        skillsField.clear();
        maxCandidatesField.setText("10");
        deadlineField.clear();
        statusCombo.setValue("open");
        titleError.setText("");
        descError.setText("");
    }

    @FXML public void handleSave() {
        boolean valid = true;

        if (titleField.getText() == null || titleField.getText().trim().length() < 3) {
            titleError.setText("Title required (min 3 characters)"); valid = false;
        } else { titleError.setText(""); }

        if (descriptionField.getText() == null || descriptionField.getText().trim().length() < 5) {
            descError.setText("Description required (min 5 characters)"); valid = false;
        } else { descError.setText(""); }

        if (!valid) return;

        try {
            User user = SessionManager.getCurrentUser();
            int estId = user.getEstablishmentId();
            if (estId == 0) estId = resolveEstablishmentId(user.getId());
            int maxC = 10;
            try { maxC = Integer.parseInt(maxCandidatesField.getText().trim()); } catch (Exception ignored) {}

            LocalDate deadline = null;
            if (!deadlineField.getText().trim().isEmpty()) {
                try {
                    deadline = LocalDate.parse(deadlineField.getText().trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                } catch (Exception e) {
                    titleError.setText("Deadline format: dd/MM/yyyy");
                    return;
                }
            }

            if (editingOffer == null) {
                Offer o = new Offer();
                o.setEstablishmentId(estId);
                o.setTitle(titleField.getText().trim());
                o.setDescription(descriptionField.getText().trim());
                o.setObjectives(objectivesField.getText().trim());
                o.setRequiredSkills(skillsField.getText().trim());
                o.setMaxCandidates(maxC);
                o.setDeadline(deadline);
                o.setStatus(statusCombo.getValue() != null ? statusCombo.getValue() : "open");
                offerDAO.save(o);
            } else {
                editingOffer.setTitle(titleField.getText().trim());
                editingOffer.setDescription(descriptionField.getText().trim());
                editingOffer.setObjectives(objectivesField.getText().trim());
                editingOffer.setRequiredSkills(skillsField.getText().trim());
                editingOffer.setMaxCandidates(maxC);
                editingOffer.setDeadline(deadline);
                editingOffer.setStatus(statusCombo.getValue() != null ? statusCombo.getValue() : "open");
                offerDAO.update(editingOffer);
            }
            hideForm();
            loadOffers();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to save: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleDelete(Offer offer) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Offer");
        confirm.setHeaderText("Delete \"" + offer.getTitle() + "\"?");
        confirm.setContentText("All related candidatures will also be deleted.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                offerDAO.delete(offer.getId());
                loadOffers();
            } catch (Exception e) {
                showAlert("Error", "Failed to delete: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
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
