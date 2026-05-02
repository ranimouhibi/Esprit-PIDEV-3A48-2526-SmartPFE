package org.example.controller;

import org.example.dao.CandidatureDAO;
import org.example.dao.OfferDAO;
import org.example.model.Candidature;
import org.example.model.Offer;
import org.example.model.User;
import org.example.service.EmailNotificationService;
import org.example.util.DialogUtil;
import org.example.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import org.example.util.ModernAlert;
import javafx.scene.layout.*;

import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.ResourceBundle;

public class StudentOffersController implements Initializable {

    @FXML private FlowPane offersContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    // View panels
    @FXML private VBox listView;
    @FXML private VBox calendarView;
    @FXML private VBox statsView;
    // Calendar
    @FXML private Label calMonthLabel;
    @FXML private GridPane calGrid;
    // Stats
    @FXML private VBox statsContainer;

    private final OfferDAO offerDAO = new OfferDAO();
    private final CandidatureDAO candidatureDAO = new CandidatureDAO();
    private final EmailNotificationService emailService = new EmailNotificationService();
    private List<Offer> allOffers = new ArrayList<>();
    private YearMonth calendarMonth = YearMonth.now();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (sortCombo != null) {
            sortCombo.setItems(FXCollections.observableArrayList("Newest", "Deadline", "Title"));
            sortCombo.setValue("Newest");
            sortCombo.valueProperty().addListener((obs, o, v) -> applyFilter());
        }
        searchField.textProperty().addListener((obs, o, v) -> applyFilter());
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
        String sort = sortCombo != null ? sortCombo.getValue() : "Newest";
        List<Offer> filtered = new ArrayList<>();
        for (Offer o : allOffers) {
            boolean matchQ = query.isEmpty() || (o.getTitle() != null && o.getTitle().toLowerCase().contains(query));
            if (matchQ) filtered.add(o);
        }
        // Sort
        if ("Deadline".equals(sort)) {
            filtered.sort(Comparator.comparing(o -> o.getDeadline() != null ? o.getDeadline() : LocalDate.MAX));
        } else if ("Title".equals(sort)) {
            filtered.sort(Comparator.comparing(o -> o.getTitle() != null ? o.getTitle() : ""));
        } else {
            filtered.sort(Comparator.comparing(o -> o.getCreatedAt() != null ? o.getCreatedAt() : java.time.LocalDateTime.MIN, Comparator.reverseOrder()));
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
        String statusColor = "open".equals(offer.getStatus()) || offer.getStatus() == null ? "#22c55e" : "#888888";
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

    // ── Apply Form (now opens as popup dialog) ────────────────────────────────

    private void openApplyForm(Offer offer) {
        DialogUtil.openApplyDialog(offer,
            offersContainer.getScene() != null ? offersContainer.getScene().getWindow() : null,
            this::loadOffers);
    }

    @FXML public void handleBrowseCV() { /* no-op, kept for FXML compatibility */ }
    @FXML public void handleCancelApply() { /* no-op, kept for FXML compatibility */ }
    @FXML public void handleSubmitApply() { /* no-op, kept for FXML compatibility */ }

    @FXML public void handleReset() {
        searchField.clear();
        if (sortCombo != null) sortCombo.setValue("Newest");
    }

    // ── View switching ────────────────────────────────────────────────────────

    @FXML public void showListView() {
        if (listView != null) { listView.setVisible(true); listView.setManaged(true); }
        if (calendarView != null) { calendarView.setVisible(false); calendarView.setManaged(false); }
        if (statsView != null) { statsView.setVisible(false); statsView.setManaged(false); }
    }

    @FXML public void showCalendarView() {
        if (listView != null) { listView.setVisible(false); listView.setManaged(false); }
        if (calendarView != null) { calendarView.setVisible(true); calendarView.setManaged(true); }
        if (statsView != null) { statsView.setVisible(false); statsView.setManaged(false); }
        renderCalendar();
    }

    @FXML public void showStatsView() {
        if (listView != null) { listView.setVisible(false); listView.setManaged(false); }
        if (calendarView != null) { calendarView.setVisible(false); calendarView.setManaged(false); }
        if (statsView != null) { statsView.setVisible(true); statsView.setManaged(true); }
        renderStats();
    }

    @FXML public void calPrev() { calendarMonth = calendarMonth.minusMonths(1); renderCalendar(); }
    @FXML public void calNext() { calendarMonth = calendarMonth.plusMonths(1); renderCalendar(); }

    private void renderCalendar() {
        if (calGrid == null || calMonthLabel == null) return;
        calMonthLabel.setText(calendarMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        calGrid.getChildren().clear();

        String[] days = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
        for (int i = 0; i < 7; i++) {
            Label h = new Label(days[i]);
            h.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #888;");
            h.setMaxWidth(Double.MAX_VALUE);
            h.setAlignment(Pos.CENTER);
            calGrid.add(h, i, 0);
        }

        User user = SessionManager.getCurrentUser();
        Map<LocalDate, List<String[]>> events = new HashMap<>();

        // Offer deadlines (violet)
        for (Offer o : allOffers) {
            if (o.getDeadline() != null) {
                events.computeIfAbsent(o.getDeadline(), k -> new ArrayList<>())
                      .add(new String[]{"📋 " + truncate(o.getTitle(), 14), "#8b5cf6"});
            }
        }
        // My candidatures (pink/green/red by status)
        try {
            for (Candidature c : candidatureDAO.findByStudent(user.getId())) {
                if (c.getCreatedAt() != null) {
                    String color = "accepted".equals(c.getStatus()) ? "#28a745"
                                 : "rejected".equals(c.getStatus()) ? "#dc3545" : "#ec4899";
                    events.computeIfAbsent(c.getCreatedAt().toLocalDate(), k -> new ArrayList<>())
                          .add(new String[]{"✉ " + truncate(c.getOfferTitle(), 12), color});
                }
            }
        } catch (Exception ignored) {}

        LocalDate first = calendarMonth.atDay(1);
        int startCol = first.getDayOfWeek().getValue() - 1;
        int daysInMonth = calendarMonth.lengthOfMonth();
        LocalDate today = LocalDate.now();
        int col = startCol, row = 1;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = calendarMonth.atDay(day);
            List<String[]> dayEvents = events.getOrDefault(date, List.of());
            VBox cell = new VBox(2);
            cell.setPadding(new Insets(3));
            cell.setMinHeight(60);
            cell.setStyle("-fx-background-color: " + (date.equals(today) ? "#fff3f3" : "white") +
                "; -fx-border-color: #eee; -fx-border-width: 0.5;");
            Label dayLbl = new Label(String.valueOf(day));
            dayLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: " + (date.equals(today) ? "bold" : "normal") +
                "; -fx-text-fill: " + (date.equals(today) ? "#a12c2f" : "#333") + ";");
            cell.getChildren().add(dayLbl);
            for (String[] ev : dayEvents) {
                Label evLbl = new Label(ev[0]);
                evLbl.setStyle("-fx-background-color: " + ev[1] + "22; -fx-text-fill: " + ev[1] +
                    "; -fx-font-size: 9px; -fx-background-radius: 3; -fx-padding: 1 3;");
                evLbl.setMaxWidth(Double.MAX_VALUE);
                cell.getChildren().add(evLbl);
            }
            calGrid.add(cell, col, row);
            col++;
            if (col == 7) { col = 0; row++; }
        }
    }

    private void renderStats() {
        if (statsContainer == null) return;
        statsContainer.getChildren().clear();
        try {
            User user = SessionManager.getCurrentUser();
            List<Candidature> myCandidatures = candidatureDAO.findByStudent(user.getId());

            // Summary cards
            HBox cards = new HBox(12);
            cards.getChildren().addAll(
                statCard("Total Offers", String.valueOf(allOffers.size()), "#667eea"),
                statCard("My Applications", String.valueOf(myCandidatures.size()), "#a12c2f"),
                statCard("Accepted", String.valueOf(myCandidatures.stream().filter(c -> "accepted".equals(c.getStatus())).count()), "#28a745"),
                statCard("Pending", String.valueOf(myCandidatures.stream().filter(c -> "pending".equals(c.getStatus())).count()), "#ffc107")
            );
            statsContainer.getChildren().add(cards);

            // Monthly distribution bar chart (simple)
            Label chartTitle = new Label("Monthly Applications");
            chartTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1a1a2e;");
            statsContainer.getChildren().add(chartTitle);

            Map<String, Long> monthly = new LinkedHashMap<>();
            for (int i = 5; i >= 0; i--) {
                YearMonth ym = YearMonth.now().minusMonths(i);
                String key = ym.format(DateTimeFormatter.ofPattern("MMM yy"));
                long count = myCandidatures.stream()
                    .filter(c -> c.getCreatedAt() != null && YearMonth.from(c.getCreatedAt()).equals(ym))
                    .count();
                monthly.put(key, count);
            }
            long maxVal = monthly.values().stream().mapToLong(v -> v).max().orElse(1);
            if (maxVal == 0) maxVal = 1;

            VBox chart = new VBox(6);
            chart.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 16;");
            for (Map.Entry<String, Long> entry : monthly.entrySet()) {
                HBox barRow = new HBox(8);
                barRow.setAlignment(Pos.CENTER_LEFT);
                Label monthLbl = new Label(entry.getKey());
                monthLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
                monthLbl.setPrefWidth(50);
                double pct = (double) entry.getValue() / maxVal;
                Region bar = new Region();
                bar.setPrefHeight(18);
                bar.setPrefWidth(Math.max(4, pct * 200));
                bar.setStyle("-fx-background-color: #a12c2f; -fx-background-radius: 4;");
                Label cnt = new Label(String.valueOf(entry.getValue()));
                cnt.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #a12c2f;");
                barRow.getChildren().addAll(monthLbl, bar, cnt);
                chart.getChildren().add(barRow);
            }
            statsContainer.getChildren().add(chart);

            // Status distribution
            Label distTitle = new Label("Application Status Distribution");
            distTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1a1a2e;");
            statsContainer.getChildren().add(distTitle);

            Map<String, String> statusColors = Map.of("pending","#ffc107","accepted","#28a745","rejected","#dc3545","interview","#667eea");
            HBox distRow = new HBox(12);
            for (Map.Entry<String, String> sc : statusColors.entrySet()) {
                long cnt = myCandidatures.stream().filter(c -> sc.getKey().equals(c.getStatus())).count();
                VBox box = new VBox(4);
                box.setAlignment(Pos.CENTER);
                box.setStyle("-fx-background-color: " + sc.getValue() + "22; -fx-background-radius: 8; -fx-padding: 12 20;");
                Label num = new Label(String.valueOf(cnt));
                num.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + sc.getValue() + ";");
                Label lbl = new Label(sc.getKey().toUpperCase());
                lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: " + sc.getValue() + ";");
                box.getChildren().addAll(num, lbl);
                distRow.getChildren().add(box);
            }
            statsContainer.getChildren().add(distRow);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private VBox statCard(String label, String value, String color) {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 10; -fx-padding: 14 20;");
        box.setMinWidth(120);
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.85);");
        box.getChildren().addAll(val, lbl);
        return box;
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : (s != null ? s : "");
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        ModernAlert.Type mType = (type == Alert.AlertType.ERROR) ? ModernAlert.Type.ERROR :
                                 (type == Alert.AlertType.WARNING) ? ModernAlert.Type.WARNING :
                                 ModernAlert.Type.SUCCESS;
        ModernAlert.show(mType, title, msg);
    }
}
