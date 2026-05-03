package org.example.controller;

import org.example.dao.MeetingDAO;
import org.example.dao.MeetingParticipantDAO;
import org.example.dao.ProjectDAO;
import org.example.model.Meeting;
import org.example.model.Project;
import org.example.model.User;
import org.example.service.EmailService;
import org.example.service.JitsiMeetingService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class MeetingFormDialogController implements Initializable {

    private static final Logger LOG = Logger.getLogger(MeetingFormDialogController.class.getName());

    @FXML private Label dialogTitle;
    @FXML private ComboBox<Project> projectCombo;
    @FXML private ComboBox<String> typeCombo, statusCombo;
    @FXML private DatePicker datePicker;
    @FXML private TextField durationField, locationField, linkField;
    @FXML private TextArea agendaField;
    @FXML private ListView<User> participantsListView;
    @FXML private Button saveBtn;
    @FXML private Label errorLabel;

    private final MeetingDAO meetingDAO = new MeetingDAO();
    private final ProjectDAO projectDAO = new ProjectDAO();
    private final JitsiMeetingService jitsiService = new JitsiMeetingService();
    private final EmailService emailService = new EmailService();
    private final MeetingParticipantDAO participantDAO = new MeetingParticipantDAO();
    private final org.example.dao.UserDAO userDAO = new org.example.dao.UserDAO();

    private Meeting editMeeting;
    private Consumer<Meeting> callback;
    private LocalDateTime originalDate;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        typeCombo.setItems(FXCollections.observableArrayList("ONLINE", "IN_PERSON", "HYBRID"));
        statusCombo.setItems(FXCollections.observableArrayList("PENDING", "CONFIRMED", "CANCELLED"));
        statusCombo.setValue("CONFIRMED");

        // Auto-generate Jitsi link when type changes to ONLINE
        typeCombo.setOnAction(e -> {
            String type = typeCombo.getValue();
            if ("ONLINE".equals(type)) {
                // Désactiver le champ lieu pour ONLINE
                locationField.setDisable(true);
                locationField.setText("Online Meeting");
                // Générer le lien Jitsi
                if (linkField.getText() == null || linkField.getText().isBlank()) {
                    Project p = projectCombo.getValue();
                    String title = p != null ? p.getTitle() : "meeting";
                    linkField.setText(jitsiService.generatePreviewLink(title));
                }
            } else if ("IN_PERSON".equals(type)) {
                // Réactiver le champ lieu pour IN_PERSON
                locationField.setDisable(false);
                locationField.clear();
                linkField.clear();
            } else if ("HYBRID".equals(type)) {
                // Réactiver le champ lieu pour HYBRID
                locationField.setDisable(false);
                if (locationField.getText().equals("Online Meeting")) {
                    locationField.clear();
                }
                // Générer le lien Jitsi pour HYBRID aussi
                if (linkField.getText() == null || linkField.getText().isBlank()) {
                    Project p = projectCombo.getValue();
                    String title = p != null ? p.getTitle() : "meeting";
                    linkField.setText(jitsiService.generatePreviewLink(title));
                }
            }
        });

        try {
            List<Project> projects = projectDAO.findAll();
            projectCombo.setItems(FXCollections.observableArrayList(projects));
            
            // Charger tous les users pour la sélection des participants
            List<User> allUsers = userDAO.findAll();
            participantsListView.setItems(FXCollections.observableArrayList(allUsers));
            participantsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            participantsListView.setCellFactory(lv -> new javafx.scene.control.ListCell<User>() {
                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    if (empty || user == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(8);
                        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                        // Avatar initiales
                        String displayName = (user.getName() != null && !user.getName().isBlank())
                            ? user.getName() : user.getEmail();
                        javafx.scene.control.Label avatar = new javafx.scene.control.Label(
                            String.valueOf(displayName.charAt(0)).toUpperCase()
                        );
                        avatar.setStyle("-fx-background-color:" + roleColor(user.getRole()) + ";"
                            + "-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:12px;"
                            + "-fx-min-width:28px;-fx-min-height:28px;-fx-max-width:28px;-fx-max-height:28px;"
                            + "-fx-background-radius:14px;-fx-alignment:center;");

                        // Nom
                        javafx.scene.control.Label name = new javafx.scene.control.Label(displayName);
                        name.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1f2937;");

                        // Badge rôle
                        javafx.scene.control.Label badge = new javafx.scene.control.Label(
                            user.getRole() != null ? user.getRole().toLowerCase() : ""
                        );
                        badge.setStyle("-fx-background-color:" + roleColor(user.getRole()) + "22;"
                            + "-fx-text-fill:" + roleColor(user.getRole()) + ";"
                            + "-fx-font-size:10px;-fx-padding:2 6;-fx-background-radius:8px;");

                        row.getChildren().addAll(avatar, name, badge);
                        setGraphic(row);
                        setText(null);
                    }
                }

                private String roleColor(String role) {
                    if (role == null) return "#6b7280";
                    switch (role.toLowerCase()) {
                        case "admin":       return "#7c3aed";
                        case "supervisor":  return "#0369a1";
                        case "student":     return "#059669";
                        default:            return "#6b7280";
                    }
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void initCreate(Consumer<Meeting> cb) {
        this.callback = cb;
        dialogTitle.setText("Nouveau Meeting");
    }

    public void initCreateWithDate(LocalDate date, Consumer<Meeting> cb) {
        initCreate(cb);
        datePicker.setValue(date);
    }

    public void initEdit(Meeting m, Consumer<Meeting> cb) {
        this.editMeeting = m;
        this.callback = cb;
        this.originalDate = m.getScheduledDate();
        dialogTitle.setText("Modifier Meeting");
        typeCombo.setValue(m.getMeetingType());
        statusCombo.setValue(m.getStatus());
        if (m.getScheduledDate() != null) datePicker.setValue(m.getScheduledDate().toLocalDate());
        durationField.setText(String.valueOf(m.getDuration()));
        locationField.setText(m.getLocation() != null ? m.getLocation() : "");
        linkField.setText(m.getMeetingLink() != null ? m.getMeetingLink() : "");
        agendaField.setText(m.getAgenda() != null ? m.getAgenda() : "");

        // Désactiver le champ lieu si ONLINE
        if ("ONLINE".equals(m.getMeetingType())) {
            locationField.setDisable(true);
        }

        // Sélectionner le projet dans la liste déjà chargée par initialize()
        projectCombo.getItems().stream()
            .filter(p -> p.getId() == m.getProjectId())
            .findFirst()
            .ifPresent(projectCombo::setValue);

        // Pré-sélectionner les participants existants
        try {
            List<User> existingParticipants = participantDAO.findParticipants(m.getId());
            for (User u : existingParticipants) {
                for (int i = 0; i < participantsListView.getItems().size(); i++) {
                    if (participantsListView.getItems().get(i).getId() == u.getId()) {
                        participantsListView.getSelectionModel().select(i);
                        break;
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void handleSave() {
        errorLabel.setText("");
        Project project = projectCombo.getValue();
        String type = typeCombo.getValue();
        LocalDate date = datePicker.getValue();
        String durationStr = durationField.getText().trim();
        String location = locationField.getText().trim();
        String link = linkField.getText().trim();
        String agenda = agendaField.getText().trim();
        String status = statusCombo.getValue();

        // Validation
        if (project == null) { errorLabel.setText("Le projet est obligatoire."); return; }
        if (type == null) { errorLabel.setText("Le type est obligatoire."); return; }
        if (date == null) { errorLabel.setText("La date est obligatoire."); return; }
        if (date.isBefore(LocalDate.now().plusDays(1))) { errorLabel.setText("La date doit être dans le futur (au moins demain)."); return; }

        // Unicité : vérifier qu'aucun meeting n'existe déjà à cette date
        try {
            int excludeId = editMeeting != null ? editMeeting.getId() : -1;
            if (meetingDAO.existsByDate(date, excludeId)) {
                errorLabel.setText("Un meeting existe déjà à cette date. Veuillez choisir une autre date.");
                return;
            }
        } catch (Exception e) {
            errorLabel.setText("Erreur lors de la vérification de la date.");
            return;
        }
        int duration;
        try { duration = Integer.parseInt(durationStr); } catch (NumberFormatException e) { errorLabel.setText("Durée invalide."); return; }
        if (duration < 15 || duration > 480) { errorLabel.setText("Durée entre 15 et 480 minutes."); return; }
        
        // Validation du lieu : obligatoire seulement pour IN_PERSON et HYBRID
        if ("ONLINE".equals(type)) {
            location = "Online Meeting"; // Forcer la valeur pour ONLINE
        } else {
            if (location.length() < 3 || location.length() > 255) { 
                errorLabel.setText("Lieu : 3 à 255 caractères."); 
                return; 
            }
        }
        
        if (agenda.length() > 2000) { errorLabel.setText("Agenda max 2000 caractères."); return; }
        if ("IN_PERSON".equals(type) && !link.isEmpty()) { errorLabel.setText("IN_PERSON ne peut pas avoir de lien."); return; }

        Meeting m = editMeeting != null ? editMeeting : new Meeting();
        m.setProjectId(project.getId());
        m.setProjectTitle(project.getTitle());
        m.setMeetingType(type);
        m.setScheduledDate(LocalDateTime.of(date, LocalTime.of(9, 0)));
        m.setDuration(duration);
        m.setLocation(location);
        m.setMeetingLink(link);
        m.setAgenda(agenda);
        m.setStatus(status);

        try {
            boolean isNew = (editMeeting == null);
            if (isNew) {
                // 1. Sauvegarder pour obtenir l'ID
                meetingDAO.save(m);

                // 2. Sauvegarder les participants sélectionnés
                List<User> selectedParticipants = participantsListView.getSelectionModel().getSelectedItems();
                for (User user : selectedParticipants) {
                    participantDAO.addParticipant(m.getId(), user.getId());
                }

                // 3. Générer le lien Jitsi APRÈS avoir l'ID
                if ("ONLINE".equals(type) && jitsiService.isConfigured()) {
                    String jitsiLink = jitsiService.generateMeetLink(m);
                    m.setMeetingLink(jitsiLink);
                    meetingDAO.updateMeetingLink(m.getId(), jitsiLink);
                    linkField.setText(jitsiLink);
                }

                // 4. Envoyer les emails d'invitation
                sendNotifications(m, "CREATED");
            } else {
                boolean dateChanged = originalDate != null && !originalDate.toLocalDate().equals(date);
                meetingDAO.update(m, dateChanged);

                // Mettre à jour les participants
                participantDAO.removeAllParticipants(m.getId());
                List<User> selectedParticipants = participantsListView.getSelectionModel().getSelectedItems();
                for (User user : selectedParticipants) {
                    participantDAO.addParticipant(m.getId(), user.getId());
                }

                // Envoyer les emails de mise à jour
                sendNotifications(m, "UPDATED");
            }

            if (callback != null) callback.accept(m);
            ((Stage) saveBtn.getScene().getWindow()).close();
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Erreur lors de la sauvegarde.");
        }
    }

    private void sendNotifications(Meeting meeting, String eventType) {
        if (!emailService.isEnabled()) return;
        new Thread(() -> {
            try {
                List<User> recipients = participantDAO.findParticipantsAndSupervisor(meeting.getId());
                if (recipients.isEmpty()) return;
                if ("CREATED".equals(eventType)) {
                    emailService.sendMeetingInvitation(meeting, recipients);
                } else {
                    emailService.sendMeetingUpdate(meeting, recipients);
                }
            } catch (Exception e) {
                LOG.warning("Erreur envoi notifications : " + e.getMessage());
            }
        }).start();
    }

    @FXML public void handleCancel() {
        ((Stage) saveBtn.getScene().getWindow()).close();
    }
}
