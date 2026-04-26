package org.example.controller;

import org.example.dao.AuditLogDAO;
import org.example.model.AuditLog;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AuditLogController implements Initializable {

    @FXML private TableView<AuditLog>           auditTable;
    @FXML private TableColumn<AuditLog, String> colDate;
    @FXML private TableColumn<AuditLog, String> colUser;
    @FXML private TableColumn<AuditLog, String> colAction;
    @FXML private TableColumn<AuditLog, String> colIp;
    @FXML private TableColumn<AuditLog, String> colDetails;

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> actionFilter;
    @FXML private Label            countLabel;

    private final AuditLogDAO dao = new AuditLogDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        loadActions();
        loadAll();
    }

    // ── Setup ────────────────────────────────────────────────────────────────

    private void setupColumns() {
        colDate   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFormattedDate()));
        colUser   .setCellValueFactory(c -> new SimpleStringProperty(
                       nvl(c.getValue().getUserName())));
        colAction .setCellValueFactory(c -> new SimpleStringProperty(
                       nvl(c.getValue().getAction())));
        colIp     .setCellValueFactory(c -> new SimpleStringProperty(
                       nvl(c.getValue().getIpAddress())));
        colDetails.setCellValueFactory(c -> new SimpleStringProperty(
                       nvl(c.getValue().getDetails())));

        // Color-code Action column
        colAction.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String color = switch (item.toUpperCase()) {
                    case "CREATE", "INSERT", "REGISTER" -> "#16a34a";
                    case "UPDATE", "EDIT"               -> "#2563eb";
                    case "DELETE", "REMOVE"             -> "#dc2626";
                    case "LOGIN"                        -> "#7c3aed";
                    case "LOGOUT"                       -> "#d97706";
                    default                             -> "#555";
                };
                setStyle("-fx-font-weight: bold; -fx-text-fill: " + color + ";");
            }
        });

        // Truncate details, show full text in tooltip
        colDetails.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setTooltip(null); return; }
                String display = item.length() > 90 ? item.substring(0, 87) + "…" : item;
                setText(display);
                setTooltip(new Tooltip(item));
            }
        });

        // Alternating row colors
        auditTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(AuditLog item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(empty || item == null ? "" :
                        getIndex() % 2 == 0 ? "-fx-background-color: white;" : "-fx-background-color: #fafafa;");
            }
        });
    }

    private void loadActions() {
        try {
            actionFilter.setItems(FXCollections.observableArrayList(dao.findDistinctActions()));
        } catch (Exception e) {
            actionFilter.setItems(FXCollections.observableArrayList("All"));
        }
        actionFilter.setValue("All");
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    private void loadAll() {
        try {
            List<AuditLog> logs = dao.findAll();
            auditTable.setItems(FXCollections.observableArrayList(logs));
            updateCount(logs.size());
        } catch (Exception e) {
            countLabel.setText("Error: " + e.getMessage());
            countLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSearch() {
        try {
            List<AuditLog> logs = dao.search(
                searchField.getText().trim(),
                actionFilter.getValue()
            );
            auditTable.setItems(FXCollections.observableArrayList(logs));
            updateCount(logs.size());
        } catch (Exception e) {
            countLabel.setText("Search error: " + e.getMessage());
            countLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
        }
    }

    @FXML
    public void handleReset() {
        searchField.clear();
        actionFilter.setValue("All");
        loadAll();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void updateCount(int count) {
        countLabel.setText(count + " entr" + (count == 1 ? "y" : "ies"));
        countLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
    }

    private String nvl(String s) {
        return s != null ? s : "—";
    }
}
