package org.example.controller;

import org.example.dao.TaskDAO;
import org.example.dao.UserDAO;
import org.example.model.Task;
import org.example.model.User;
import org.example.util.EmailService;
import org.example.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Window;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class TaskController implements Initializable {

    @FXML private FlowPane cardPane;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField searchField;
    @FXML private Label messageLabel;

    private final TaskDAO taskDAO = new TaskDAO();
    private List<Task> allTasks = List.of();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadTasks();
        if (searchField != null)
            searchField.textProperty().addListener((o, old, v) -> applyFilters());
    }

    private void loadTasks() {
        try {
            User current = SessionManager.getCurrentUser();
            if (current != null && "student".equals(current.getRole())) {
                allTasks = taskDAO.findByAssignedUser(current.getId());
            } else {
                allTasks = taskDAO.findAll();
            }
            renderCards(allTasks);
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Error: " + e.getMessage(), true);
        }
    }

    private void renderCards(List<Task> tasks) {
        if (cardPane == null) return;
        cardPane.getChildren().clear();
        if (tasks.isEmpty()) {
            Label empty = new Label("No tasks found.");
            empty.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14px;");
            cardPane.getChildren().add(empty);
            return;
        }
        for (Task t : tasks) cardPane.getChildren().add(buildCard(t));
    }

    private VBox buildCard(Task task) {
        VBox card = new VBox(8);
        card.setPrefWidth(270);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);");
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(task.getTitle());
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        title.setTextFill(Color.web("#1a1a2e"));
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);
        header.getChildren().addAll(title, statusBadge(task.getStatus()));
        Label priority = priorityBadge(task.getPriority());
        Label sprint  = info("Sprint: " + nvl(task.getSprintName()));
        Label project = info("Project: " + nvl(task.getProjectTitle()));
        Label assigned = info("Assigned: " + (task.getAssignedToName() != null ? task.getAssignedToName() : "Unassigned"));
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button editBtn = btn("Edit", "#667eea");
        Button delBtn  = btn("Delete", "#ef4444");
        editBtn.setOnAction(e -> {
            Window owner = getWindow();
            if (owner != null && new TaskDialog(owner, task).showAndWait()) { loadTasks(); }
        });
        delBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete task?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(b -> {
                if (b == ButtonType.YES) {
                    try { taskDAO.delete(task.getId()); loadTasks(); }
                    catch (Exception ex) { showMessage("Error: " + ex.getMessage(), true); }
                }
            });
        });
        actions.getChildren().addAll(editBtn, delBtn);
        card.getChildren().addAll(header, priority, sprint, project, assigned, new Separator(), actions);
        return card;
    }

    private Label statusBadge(String s) {
        String c = "done".equals(s) ? "#10b981" : "in_progress".equals(s) ? "#3b82f6" : "#f59e0b";
        Label b = new Label(s != null ? s.replace("_"," ").toUpperCase() : "");
        b.setStyle("-fx-background-color:"+c+";-fx-text-fill:white;-fx-background-radius:6;-fx-padding:2 7;-fx-font-size:9px;-fx-font-weight:bold;");
        return b;
    }

    private Label priorityBadge(String p) {
        String c = "critical".equals(p) ? "#dc2626" : "high".equals(p) ? "#f97316" : "medium".equals(p) ? "#eab308" : "#6b7280";
        Label b = new Label(p != null ? p.toUpperCase() : "");
        b.setStyle("-fx-background-color:"+c+"22;-fx-text-fill:"+c+";-fx-background-radius:4;-fx-padding:1 6;-fx-font-size:10px;-fx-font-weight:bold;");
        return b;
    }

    private Label info(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
        return l;
    }

    private Button btn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:"+color+";-fx-text-fill:white;-fx-background-radius:6;-fx-padding:5 12;-fx-font-size:11px;-fx-cursor:hand;");
        return b;
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private Window getWindow() {
        if (scrollPane != null && scrollPane.getScene() != null) return scrollPane.getScene().getWindow();
        if (cardPane != null && cardPane.getScene() != null) return cardPane.getScene().getWindow();
        return null;
    }

    private void applyFilters() {
        String q = searchField != null ? searchField.getText().toLowerCase() : "";
        renderCards(allTasks.stream().filter(t ->
            q.isBlank() ||
            (t.getTitle() != null && t.getTitle().toLowerCase().contains(q)) ||
            (t.getStatus() != null && t.getStatus().toLowerCase().contains(q)) ||
            (t.getAssignedToName() != null && t.getAssignedToName().toLowerCase().contains(q))
        ).toList());
    }

    @FXML public void handleSearch() { applyFilters(); }

    @FXML
    public void handleAddDialog() {
        Window owner = getWindow();
        if (owner == null) return;
        if (new TaskDialog(owner, null).showAndWait()) loadTasks();
    }

    private void showMessage(String msg, boolean isError) {
        if (messageLabel != null) {
            messageLabel.setText(msg);
            messageLabel.setStyle(isError ? "-fx-text-fill:#dc2626;" : "-fx-text-fill:#16a34a;");
        }
    }
}
