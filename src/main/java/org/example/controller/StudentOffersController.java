package org.example.controller;

import org.example.dao.CandidatureDAO;
import org.example.dao.OfferDAO;
import org.example.model.Candidature;
import org.example.model.Offer;
import org.example.model.User;
import org.example.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class StudentOffersController implements Initializable {

    @FXML private FlowPane offersContainer;
    @FXML private TextField searchField;

    // Apply form
    @FXML private VBox applyFormContainer;
    @FXML private Label applyOfferTitle;
    @FXML private TextArea motivationField;
    @FXML private TextField cvPathField;
    @FXML private TextField portfolioField;
    @FXML private TextField githubField;
    @FXML private Label motivationError;
    @FXML private Label cvError;

    private final OfferDAO offerDAO = new OfferDAO();
    private final CandidatureDAO candidatureDAO = new CandidatureDAO();
    private List<Offer> allOffers = new ArrayList<>();
    private Offer applyingOffer = null;
    private File selectedCvFile = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        searchField.textProperty().addListener((obs, o, v) -> applyFilter());

        // Input restrictions
        motivationField.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() > 2000) motivationField.setText(old);
            if (val != null && !val.isEmpty() && !val.matches("[a-zA-Z0-9\\s\\p{Punct}\u00C0-\u017F\\n\\r]*"))
                motivationField.setText(old);
        });
        portfolioField.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() > 255) portfolioField.setText(old);
            if (val != null && !val.isEmpty() && !val.matches("[a-zA-Z0-9\\s:/\\-._~!$&'()*+,;=@%#?]*"))
                portfolioField.setText(old);
        });
        githubField.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() > 255) githubField.setText(old);
            if (val != null && !val.isEmpty() && !val.matches("[a-zA-Z0-9\\s:/\\-._~!$&'()*+,;=@%#?]*"))
                githubField.setText(old);
        });

        loadOffers();
    }

    private void loadOffers() {
        try {
            allOffers = offerDAO.findAllOpen();
            applyFilter();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyFilter() {
        String query = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        List<Offer> filtered = new ArrayList<>();
        for (Offer o : allOffers) {
            boolean matchQ = query.isEmpty() || (o.getTitle() != null && o.getTitle().toLowerCase().contains(query));
            if (matchQ) filtered.add(o);
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
            Label lbl = new Label("No offers available from your establishment.");
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
        Label estLabel = new Label(offer.getEstablishmentName() != null ? offer.getEstablishmentName() : "");
        estLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 10px;");
        header.getChildren().addAll(statusLabel, spacer, estLabel);

        Label title = new Label(offer.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #1e1e1e;");
        title.setWrapText(true);

        Label desc = new Label(offer.getDescription() != null ? offer.getDescription() : "");
        desc.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
        desc.setWrapText(true);
        desc.setMaxHeight(50);

        card.getChildren().addAll(header, title, desc);

        if (offer.getRequiredSkills() != null && !offer.getRequiredSkills().isEmpty()) {
            Label skills = new Label("Skills: " + offer.getRequiredSkills());
            skills.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
            skills.setWrapText(true);
            card.getChildren().add(skills);
        }

        if (offer.getDeadline() != null) {
            Label deadline = new Label("Deadline: " + offer.getDeadline().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            deadline.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-font-weight: bold;");
            card.getChildren().add(deadline);
        }

        Separator sep = new Separator();

        // Check if already applied
        boolean alreadyApplied = false;
        try {
            User user = SessionManager.getCurrentUser();
            alreadyApplied = candidatureDAO.existsByStudentAndOffer(user.getId(), offer.getId());
        } catch (Exception ignored) {}

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);

        if (alreadyApplied) {
            Label appliedLabel = new Label("Already Applied");
            appliedLabel.setStyle("-fx-background-color: #22c55e22; -fx-text-fill: #22c55e; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 14;");
            actions.getChildren().add(appliedLabel);
        } else {
            Button applyBtn = new Button("Apply Now");
            applyBtn.setStyle("-fx-background-color: #a12c2f; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 6; -fx-padding: 6 16; -fx-cursor: hand; -fx-font-weight: bold;");
            applyBtn.setOnAction(e -> openApplyForm(offer));
            actions.getChildren().add(applyBtn);
        }

        card.getChildren().addAll(sep, actions);
        return card;
    }

    // ── Apply Form ────────────────────────────────────────────────────────────

    private void openApplyForm(Offer offer) {
        applyingOffer = offer;
        applyOfferTitle.setText("Apply for: " + offer.getTitle());
        motivationField.clear();
        cvPathField.clear();
        portfolioField.clear();
        githubField.clear();
        motivationError.setText("");
        cvError.setText("");
        selectedCvFile = null;
        applyFormContainer.setVisible(true);
        applyFormContainer.setManaged(true);
    }

    @FXML public void handleBrowseCV() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select CV");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
            new FileChooser.ExtensionFilter("Word Files", "*.doc", "*.docx"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = fc.showOpenDialog(offersContainer.getScene().getWindow());
        if (file != null) {
            selectedCvFile = file;
            cvPathField.setText(file.getName()); // store only filename
            cvError.setText("");
        }
    }

    @FXML public void handleCancelApply() {
        applyFormContainer.setVisible(false);
        applyFormContainer.setManaged(false);
        applyingOffer = null;
        selectedCvFile = null;
    }

    @FXML public void handleSubmitApply() {
        boolean valid = true;

        if (motivationField.getText() == null || motivationField.getText().trim().isEmpty()) {
            motivationError.setText("Motivation letter is required"); valid = false;
        } else if (motivationField.getText().trim().length() < 50) {
            motivationError.setText("Minimum 50 characters"); valid = false;
        } else { motivationError.setText(""); }

        if (selectedCvFile == null) {
            cvError.setText("CV is required"); valid = false;
        } else { cvError.setText(""); }

        if (!valid) return;

        try {
            User user = SessionManager.getCurrentUser();
            Candidature c = new Candidature();
            c.setOfferId(applyingOffer.getId());
            c.setStudentId(user.getId());
            c.setMotivationLetter(motivationField.getText().trim());
            c.setCvPath(selectedCvFile.getName()); // filename only
            c.setPortfolioUrl(portfolioField.getText().trim());
            c.setGithubUrl(githubField.getText().trim());
            candidatureDAO.save(c);

            handleCancelApply();
            loadOffers(); // refresh to show "Already Applied"
            showAlert("Success", "Your application has been submitted!", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to submit: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML public void handleReset() {
        searchField.clear();
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
