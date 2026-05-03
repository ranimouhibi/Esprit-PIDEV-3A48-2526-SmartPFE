package org.example.controller;

import org.example.dao.OfferDAO;
import org.example.model.Offer;
import org.example.model.User;
import org.example.util.ModernAlert;
import org.example.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class OfferFormDialogController implements Initializable {

    @FXML private Label dialogTitleLabel;
    @FXML private Label dialogSubtitle;
    @FXML private Label headerIcon;
    @FXML private TextField titleField;
    @FXML private Label titleError;
    @FXML private TextArea descriptionField;
    @FXML private Label descError;
    @FXML private TextArea objectivesField;
    @FXML private TextArea skillsField;
    @FXML private TextField maxCandidatesField;
    @FXML private DatePicker deadlinePicker;
    @FXML private Label deadlineError;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Button saveBtn;

    private Stage dialogStage;
    private Offer editingOffer;
    private Runnable onSuccess;

    private final OfferDAO offerDAO = new OfferDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        statusCombo.setItems(FXCollections.observableArrayList("open", "closed", "draft"));
        statusCombo.setValue("open");

        // Input restrictions
        titleField.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() > 150) titleField.setText(old);
        });
        maxCandidatesField.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.matches("\\d*")) maxCandidatesField.setText(old);
            if (val != null && val.length() > 3) maxCandidatesField.setText(old);
        });
        descriptionField.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() > 1000) descriptionField.setText(old);
        });
        objectivesField.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() > 1000) objectivesField.setText(old);
        });
        skillsField.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() > 500) skillsField.setText(old);
        });

        // Deadline: no past dates
        deadlinePicker.setDayCellFactory(picker -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });

        // Live validation on title
        titleField.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) validateTitle();
        });
        titleField.textProperty().addListener((obs, old, val) -> {
            if (titleError.getText() != null && !titleError.getText().isEmpty()) {
                titleError.setText("");
                titleField.setStyle(titleField.getStyle().replace("#fca5a5", "#e0e0e0"));
            }
        });
    }

    /** Call this to pre-fill form for editing an existing offer. */
    public void setOffer(Offer offer) {
        this.editingOffer = offer;
        dialogTitleLabel.setText("Edit Offer");
        dialogSubtitle.setText("Update the offer details below");
        headerIcon.setText("✏️");
        saveBtn.setText("Update Offer");

        titleField.setText(offer.getTitle() != null ? offer.getTitle() : "");
        descriptionField.setText(offer.getDescription() != null ? offer.getDescription() : "");
        objectivesField.setText(offer.getObjectives() != null ? offer.getObjectives() : "");
        skillsField.setText(offer.getRequiredSkills() != null ? offer.getRequiredSkills() : "");
        maxCandidatesField.setText(String.valueOf(offer.getMaxCandidates() > 0 ? offer.getMaxCandidates() : 10));
        deadlinePicker.setValue(offer.getDeadline());
        statusCombo.setValue(offer.getStatus() != null ? offer.getStatus() : "open");
    }

    public void setDialogStage(Stage stage) { this.dialogStage = stage; }
    public void setOnSuccess(Runnable callback) { this.onSuccess = callback; }

    @FXML
    private void handleSave() {
        if (!validate()) return;

        try {
            String title = titleField.getText().trim();
            int excludeId = editingOffer != null ? editingOffer.getId() : 0;

            // Unicité du titre
            if (offerDAO.existsByTitle(title, excludeId)) {
                titleError.setText("An offer with this title already exists");
                titleField.setStyle(titleField.getStyle().replace("#e0e0e0", "#fca5a5"));
                return;
            }

            int maxC = 10;
            try { maxC = Integer.parseInt(maxCandidatesField.getText().trim()); } catch (Exception ignored) {}

            if (editingOffer == null) {
                // Create new
                User user = SessionManager.getCurrentUser();
                int estId = user.getEstablishmentId();
                if (estId == 0) estId = resolveEstablishmentId(user.getId());

                Offer o = new Offer();
                o.setEstablishmentId(estId);
                o.setTitle(titleField.getText().trim());
                o.setDescription(descriptionField.getText().trim());
                o.setObjectives(objectivesField.getText().trim());
                o.setRequiredSkills(skillsField.getText().trim());
                o.setMaxCandidates(maxC);
                o.setDeadline(deadlinePicker.getValue());
                o.setStatus(statusCombo.getValue() != null ? statusCombo.getValue() : "open");
                offerDAO.save(o);

                // Notify students of this establishment asynchronously
                if ("open".equals(o.getStatus())) {
                    final int newOfferId = o.getId();
                    new org.example.service.EmailNotificationService().sendNewOfferToStudents(newOfferId);
                }
            } else {
                // Update existing
                editingOffer.setTitle(titleField.getText().trim());
                editingOffer.setDescription(descriptionField.getText().trim());
                editingOffer.setObjectives(objectivesField.getText().trim());
                editingOffer.setRequiredSkills(skillsField.getText().trim());
                editingOffer.setMaxCandidates(maxC);
                editingOffer.setDeadline(deadlinePicker.getValue());
                editingOffer.setStatus(statusCombo.getValue() != null ? statusCombo.getValue() : "open");
                offerDAO.update(editingOffer);
            }

            if (dialogStage != null) dialogStage.close();
            if (onSuccess != null) onSuccess.run();

        } catch (Exception e) {
            e.printStackTrace();
            ModernAlert.show(ModernAlert.Type.ERROR, "Error", "Failed to save offer: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        if (dialogStage != null) dialogStage.close();
    }

    private boolean validate() {
        boolean valid = true;

        if (!validateTitle()) valid = false;

        String desc = descriptionField.getText() != null ? descriptionField.getText().trim() : "";
        if (desc.length() < 5) {
            descError.setText("Description required (min 5 characters)");
            valid = false;
        } else {
            descError.setText("");
        }

        LocalDate deadline = deadlinePicker.getValue();
        if (deadline != null && deadline.isBefore(LocalDate.now())) {
            deadlineError.setText("Deadline must be in the future");
            valid = false;
        } else {
            deadlineError.setText("");
        }

        return valid;
    }

    private boolean validateTitle() {
        String t = titleField.getText() != null ? titleField.getText().trim() : "";
        if (t.length() < 3) {
            titleError.setText("Title required (min 3 characters)");
            return false;
        }
        titleError.setText("");
        return true;
    }

    private int resolveEstablishmentId(int userId) {
        try {
            java.sql.ResultSet rs = org.example.config.DatabaseConfig.getConnection()
                .createStatement()
                .executeQuery("SELECT establishment_id FROM users WHERE id = " + userId);
            if (rs.next()) return rs.getInt(1);
        } catch (Exception ignored) {}
        return userId;
    }
}
