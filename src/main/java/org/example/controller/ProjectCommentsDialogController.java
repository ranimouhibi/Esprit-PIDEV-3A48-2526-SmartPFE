package org.example.controller;

import org.example.dao.CommentDAO;
import org.example.model.Comment;
import org.example.model.Project;
import org.example.model.User;
import org.example.service.CommentService;
import org.example.util.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ProjectCommentsDialogController implements Initializable {

    @FXML private Label dialogTitle;
    @FXML private Label projectNameLabel;
    @FXML private Button addCommentBtn;

    // Stats
    @FXML private Label statTotal;
    @FXML private Label statUrgent;
    @FXML private Label statMedium;
    @FXML private Label statLow;

    // Search & Filter
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> filterImportance;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label resultCountLabel;

    // Form
    @FXML private VBox commentFormContainer;
    @FXML private Label formTitle;
    @FXML private TextField subjectField;
    @FXML private TextArea contentField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> targetCombo;
    @FXML private ComboBox<String> importanceCombo;
    @FXML private Label subjectError;
    @FXML private Label contentError;
    @FXML private Label typeError;
    @FXML private Label targetError;
    @FXML private Label importanceError;

    // List
    @FXML private VBox commentsContainer;

    private Stage dialogStage;
    private Project project;
    private final CommentDAO commentDAO = new CommentDAO();
    private final CommentService commentService = new CommentService();
    private List<Comment> allComments = new ArrayList<>();
    private Comment editingComment = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Filter combos
        filterType.setItems(FXCollections.observableArrayList(
            "All Types", "correction", "suggestion", "validation", "question"
        ));
        filterType.setValue("All Types");

        filterImportance.setItems(FXCollections.observableArrayList(
            "All", "urgent", "medium", "low"
        ));
        filterImportance.setValue("All");

        sortCombo.setItems(FXCollections.observableArrayList(
            "Newest first", "Oldest first", "By importance", "By type"
        ));
        sortCombo.setValue("Newest first");

        // Form combos
        typeCombo.setItems(FXCollections.observableArrayList(
            "correction", "suggestion", "validation", "question"
        ));
        targetCombo.setItems(FXCollections.observableArrayList(
            "code", "design", "database", "other"
        ));
        importanceCombo.setItems(FXCollections.observableArrayList(
            "low", "medium", "urgent"
        ));

        // Input restrictions
        subjectField.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() > 255) subjectField.setText(old);
            if (val != null && !val.isEmpty() && !val.matches("[a-zA-Z0-9\\s\\p{Punct}\u00C0-\u017F]*"))
                subjectField.setText(old);
        });
        contentField.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() > 1000) contentField.setText(old);
            if (val != null && !val.isEmpty() && !val.matches("[a-zA-Z0-9\\s\\p{Punct}\u00C0-\u017F\\n\\r]*"))
                contentField.setText(old);
        });

        // Live search
        searchField.textProperty().addListener((obs, old, val) -> applyFilters());
        filterType.valueProperty().addListener((obs, old, val) -> applyFilters());
        filterImportance.valueProperty().addListener((obs, old, val) -> applyFilters());
        sortCombo.valueProperty().addListener((obs, old, val) -> applyFilters());
    }

    public void setDialogStage(Stage stage) { this.dialogStage = stage; }

    public void setProject(Project project) {
        this.project = project;
        if (project != null) {
            projectNameLabel.setText(project.getTitle());
            loadComments();
        }
    }

    private void loadComments() {
        try {
            allComments = commentDAO.findByProject(project.getId());
            updateStats();
            applyFilters();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStats() {
        int total = allComments.size();
        long urgent = allComments.stream().filter(c -> "urgent".equals(c.getImportance())).count();
        long medium = allComments.stream().filter(c -> "medium".equals(c.getImportance())).count();
        long low    = allComments.stream().filter(c -> "low".equals(c.getImportance())).count();

        statTotal.setText(String.valueOf(total));
        statUrgent.setText(String.valueOf(urgent));
        statMedium.setText(String.valueOf(medium));
        statLow.setText(String.valueOf(low));
    }

    private void applyFilters() {
        String query = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String type = filterType.getValue();
        String importance = filterImportance.getValue();
        String sort = sortCombo.getValue();

        List<Comment> filtered = new ArrayList<>();
        for (Comment c : allComments) {
            boolean matchQuery = query.isEmpty()
                || (c.getSubject() != null && c.getSubject().toLowerCase().contains(query))
                || (c.getContent() != null && c.getContent().toLowerCase().contains(query));
            boolean matchType = type == null || type.equals("All Types")
                || type.equals(c.getCommentType());
            boolean matchImportance = importance == null || importance.equals("All")
                || importance.equals(c.getImportance());

            if (matchQuery && matchType && matchImportance) {
                filtered.add(c);
            }
        }

        // Sort
        if (sort != null) {
            switch (sort) {
                case "Oldest first":
                    filtered.sort((a, b) -> {
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return a.getCreatedAt().compareTo(b.getCreatedAt());
                    });
                    break;
                case "By importance":
                    filtered.sort((a, b) -> importanceOrder(a.getImportance()) - importanceOrder(b.getImportance()));
                    break;
                case "By type":
                    filtered.sort((a, b) -> {
                        String ta = a.getCommentType() != null ? a.getCommentType() : "";
                        String tb = b.getCommentType() != null ? b.getCommentType() : "";
                        return ta.compareTo(tb);
                    });
                    break;
                default: // Newest first
                    filtered.sort((a, b) -> {
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });
                    break;
            }
        }

        resultCountLabel.setText(filtered.size() + " comment(s) found");
        displayComments(filtered);
    }

    private int importanceOrder(String imp) {
        if ("urgent".equals(imp)) return 0;
        if ("medium".equals(imp)) return 1;
        return 2;
    }

    @FXML public void handleSearch() { applyFilters(); }

    @FXML public void handleReset() {
        searchField.clear();
        filterType.setValue("All Types");
        filterImportance.setValue("All");
        sortCombo.setValue("Newest first");
        applyFilters();
    }

    private void displayComments(List<Comment> comments) {
        commentsContainer.getChildren().clear();

        if (comments.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(javafx.geometry.Pos.CENTER);
            empty.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e0e0e0; -fx-border-radius: 12; -fx-border-width: 2; -fx-border-style: dashed;");
            empty.setPrefHeight(120);
            empty.setPadding(new Insets(24));
            Label lbl = new Label("No comments match your search");
            lbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #999;");
            empty.getChildren().add(lbl);
            commentsContainer.getChildren().add(empty);
            return;
        }

        for (Comment c : comments) {
            commentsContainer.getChildren().add(createCommentCard(c));
        }
    }

    private VBox createCommentCard(Comment comment) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2); -fx-border-color: #f0f0f0; -fx-border-radius: 12; -fx-border-width: 1;");
        card.setPadding(new Insets(14));

        // Header
        HBox header = new HBox(8);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label typeLabel = new Label(comment.getCommentType() != null ? comment.getCommentType().toUpperCase() : "COMMENT");
        typeLabel.setStyle("-fx-background-color: #3b82f622; -fx-text-fill: #3b82f6; -fx-font-size: 9px; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 3 8;");
        header.getChildren().add(typeLabel);

        if (comment.getImportance() != null) {
            String color = getImportanceColor(comment.getImportance());
            Label imp = new Label(comment.getImportance().toUpperCase());
            imp.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color + "; -fx-font-size: 9px; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 3 8;");
            header.getChildren().add(imp);
        }

        if (comment.getTarget() != null) {
            Label tgt = new Label(comment.getTarget().toUpperCase());
            tgt.setStyle("-fx-background-color: #8b5cf622; -fx-text-fill: #8b5cf6; -fx-font-size: 9px; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 3 8;");
            header.getChildren().add(tgt);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dateLabel = new Label(comment.getCreatedAt() != null
            ? comment.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
        dateLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 10px;");
        header.getChildren().addAll(spacer, dateLabel);

        // Subject
        if (comment.getSubject() != null && !comment.getSubject().isEmpty()) {
            Label subject = new Label(comment.getSubject());
            subject.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e1e1e;");
            subject.setWrapText(true);
            card.getChildren().add(subject);
        }

        // Content
        Label content = new Label(comment.getContent() != null ? comment.getContent() : "");
        content.setStyle("-fx-text-fill: #555; -fx-font-size: 12px;");
        content.setWrapText(true);

        // Author
        HBox authorBox = new HBox(5);
        authorBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label authorIcon = new Label("[A]");
        authorIcon.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #667eea;");
        Label authorLabel = new Label("By: " + (comment.getAuthorName() != null ? comment.getAuthorName() : "Unknown"));
        authorLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");
        authorBox.getChildren().addAll(authorIcon, authorLabel);

        card.getChildren().addAll(header, content, authorBox);

        // 🆕 Boutons de métiers (Traduction, TTS, QR Code)
        Separator metiersSep = new Separator();
        HBox metiersActions = new HBox(8);
        metiersActions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Button translateBtn = new Button("🌍 Translate");
        translateBtn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 6; -fx-padding: 5 12; -fx-cursor: hand;");
        translateBtn.setOnAction(e -> handleTranslateComment(comment));
        
        Button ttsBtn = new Button("🔊 Listen");
        ttsBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 6; -fx-padding: 5 12; -fx-cursor: hand;");
        ttsBtn.setOnAction(e -> handleListenComment(comment));
        
        metiersActions.getChildren().addAll(translateBtn, ttsBtn);
        card.getChildren().addAll(metiersSep, metiersActions);

        // Actions only for author
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && currentUser.getId() == comment.getAuthorId()) {
            Separator sep = new Separator();
            HBox actions = new HBox(8);
            actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            Button editBtn = new Button("Edit");
            editBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 6; -fx-padding: 5 12; -fx-cursor: hand;");
            editBtn.setOnAction(e -> handleEditComment(comment));

            Button deleteBtn = new Button("Delete");
            deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 6; -fx-padding: 5 12; -fx-cursor: hand;");
            deleteBtn.setOnAction(e -> handleDeleteComment(comment));

            actions.getChildren().addAll(editBtn, deleteBtn);
            card.getChildren().addAll(sep, actions);
        }

        return card;
    }

    private String getImportanceColor(String importance) {
        if (importance == null) return "#888";
        if (importance.equals("urgent")) return "#ef4444";
        if (importance.equals("medium")) return "#f59e0b";
        return "#10b981";
    }

    // ── Form ──────────────────────────────────────────────────────────────────

    @FXML public void handleToggleForm() {
        boolean visible = commentFormContainer.isVisible();
        if (!visible) {
            editingComment = null;
            clearForm();
            formTitle.setText("New Comment");
            commentFormContainer.setVisible(true);
            commentFormContainer.setManaged(true);
            addCommentBtn.setText("Cancel");
        } else {
            hideForm();
        }
    }

    private void handleEditComment(Comment comment) {
        editingComment = comment;
        formTitle.setText("Edit Comment");
        subjectField.setText(comment.getSubject() != null ? comment.getSubject() : "");
        contentField.setText(comment.getContent() != null ? comment.getContent() : "");
        typeCombo.setValue(comment.getCommentType() != null ? comment.getCommentType() : "question");
        targetCombo.setValue(comment.getTarget() != null ? comment.getTarget() : "other");
        importanceCombo.setValue(comment.getImportance() != null ? comment.getImportance() : "medium");
        commentFormContainer.setVisible(true);
        commentFormContainer.setManaged(true);
        addCommentBtn.setText("Cancel");
    }

    @FXML public void handleCancelForm() { hideForm(); }

    private void hideForm() {
        commentFormContainer.setVisible(false);
        commentFormContainer.setManaged(false);
        addCommentBtn.setText("+ Add Comment");
        editingComment = null;
        clearForm();
    }

    private void clearForm() {
        subjectField.clear();
        contentField.clear();
        typeCombo.setValue(null);
        targetCombo.setValue(null);
        importanceCombo.setValue(null);
        subjectError.setText("");
        contentError.setText("");
        typeError.setText("");
        targetError.setText("");
        importanceError.setText("");
    }

    @FXML public void handleSaveComment() {
        boolean valid = true;

        if (subjectField.getText() == null || subjectField.getText().trim().isEmpty()) {
            subjectError.setText("Subject is required"); valid = false;
        } else { subjectError.setText(""); }

        if (contentField.getText() == null || contentField.getText().trim().isEmpty()) {
            contentError.setText("Comment is required"); valid = false;
        } else { contentError.setText(""); }

        if (typeCombo.getValue() == null) {
            typeError.setText("Required"); valid = false;
        } else { typeError.setText(""); }

        if (targetCombo.getValue() == null) {
            targetError.setText("Required"); valid = false;
        } else { targetError.setText(""); }

        if (importanceCombo.getValue() == null) {
            importanceError.setText("Required"); valid = false;
        } else { importanceError.setText(""); }

        if (!valid) return;

        // 🆕 Vérifier les mots inappropriés avec le filtre de profanité
        String content = contentField.getText().trim();
        if (commentService.containsProfanity(content)) {
            Alert profanityAlert = new Alert(Alert.AlertType.WARNING);
            profanityAlert.setTitle("Profanity Detected");
            profanityAlert.setHeaderText("Inappropriate content detected");
            profanityAlert.setContentText("Your comment contains inappropriate words. Do you want to:\n\n" +
                "• Filter them automatically (replace with ***)\n" +
                "• Edit your comment manually");
            
            ButtonType filterButton = new ButtonType("Filter Automatically");
            ButtonType editButton = new ButtonType("Edit Manually");
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            
            profanityAlert.getButtonTypes().setAll(filterButton, editButton, cancelButton);
            
            Optional<ButtonType> result = profanityAlert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == filterButton) {
                    // Filtrer automatiquement
                    content = commentService.filterProfanity(content);
                    contentField.setText(content);
                } else if (result.get() == editButton) {
                    // Laisser l'utilisateur éditer
                    return;
                } else {
                    // Annuler
                    return;
                }
            } else {
                return;
            }
        }

        try {
            User user = SessionManager.getCurrentUser();
            String subject = subjectField.getText().trim();

            if (editingComment == null) {
                // ── Uniqueness check (new comment only) ───────────────────
                if (commentDAO.existsDuplicate(subject, content, user.getId(), project.getId(), 0)) {
                    subjectError.setText("A comment with the same subject and content already exists today");
                    showAlert("Duplicate Comment",
                        "You already added a comment with the same subject and content today.\nPlease modify your comment.",
                        Alert.AlertType.WARNING);
                    return;
                }

                Comment c = new Comment();
                c.setAuthorId(user.getId());
                c.setCommentableType("project");
                c.setCommentableId(project.getId());
                c.setSubject(subject);
                c.setContent(content);
                c.setCommentType(typeCombo.getValue());
                c.setTarget(targetCombo.getValue());
                c.setImportance(importanceCombo.getValue());
                commentDAO.save(c);
                
                // 🆕 Notification
                NotificationUtil.Notifications.newComment(project.getTitle());
            } else {
                editingComment.setSubject(subject);
                editingComment.setContent(content);
                editingComment.setCommentType(typeCombo.getValue());
                editingComment.setTarget(targetCombo.getValue());
                editingComment.setImportance(importanceCombo.getValue());
                commentDAO.update(editingComment);
            }

            hideForm();
            loadComments();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to save: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleDeleteComment(Comment comment) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Comment");
        confirm.setHeaderText("Delete this comment?");
        confirm.setContentText("This action cannot be undone.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                commentDAO.delete(comment.getId());
                loadComments();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to delete: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML public void handleClose() { dialogStage.close(); }

    @FXML
    public void handleExportPDF() {
        if (allComments == null || allComments.isEmpty()) {
            showAlert("Warning", "No comments to export", Alert.AlertType.WARNING);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF Report");
        fileChooser.setInitialFileName(project.getTitle().replaceAll("[^a-zA-Z0-9]", "_") + "_comments.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        
        File file = fileChooser.showSaveDialog(dialogStage);
        if (file != null) {
            try {
                PDFExporter.exportComments(allComments, file);
                showAlert("Success", "PDF exported successfully!", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to export PDF: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    // ── Métiers Methods ──────────────────────────────────────────────────────

    /**
     * Traduire un commentaire
     */
    private void handleTranslateComment(Comment comment) {
        // Créer un dialog pour choisir la langue
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Traduire le commentaire");
        dialog.setHeaderText("Choisir la langue cible");

        // Boutons
        ButtonType translateButtonType = new ButtonType("Traduire", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(translateButtonType, cancelButtonType);

        // Contenu
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        Label label = new Label("Sélectionner la langue :");
        ComboBox<String> languageCombo = new ComboBox<>();
        languageCombo.setItems(FXCollections.observableArrayList(
            "English", "Français", "Español", "Deutsch", "Italiano", "العربية", "中文"
        ));
        languageCombo.setValue("Français");

        content.getChildren().addAll(label, languageCombo);
        dialog.getDialogPane().setContent(content);

        // Convertir le résultat
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == translateButtonType) {
                String selected = languageCombo.getValue();
                // Mapper vers le code de langue
                switch (selected) {
                    case "English": return "en";
                    case "Français": return "fr";
                    case "Español": return "es";
                    case "Deutsch": return "de";
                    case "Italiano": return "it";
                    case "العربية": return "ar";
                    case "中文": return "zh";
                    default: return "fr";
                }
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(targetLang -> {
            try {
                // Traduire le commentaire
                String translatedContent = commentService.translateComment(comment.getContent(), targetLang);
                
                // Afficher le résultat dans un dialog
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Résultat de la traduction");
                alert.setHeaderText("Traduit en " + commentService.getLanguageName(targetLang));
                
                TextArea textArea = new TextArea(translatedContent);
                textArea.setWrapText(true);
                textArea.setEditable(false);
                textArea.setPrefRowCount(10);
                
                alert.getDialogPane().setContent(textArea);
                alert.showAndWait();
                
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Erreur", "La traduction a échoué : " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    /**
     * Créer un commentaire vocal
     */
    @FXML
    public void handleVocalComment() {
        // Vérifier si le modèle Vosk doit être téléchargé
        if (!VoskSpeechUtil.isModelDownloaded()) {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("📥 Téléchargement requis");
            info.setHeaderText("Premier lancement - Téléchargement du modèle");
            info.setContentText(
                "Vosk va télécharger le modèle français (~40 MB) une seule fois.\n\n" +
                "✅ Ensuite, tout fonctionnera HORS LIGNE !\n" +
                "✅ Aucune inscription requise\n" +
                "✅ 100% gratuit\n\n" +
                "Le téléchargement prendra 1-2 minutes selon votre connexion.\n" +
                "Voulez-vous continuer ?"
            );
            
            ButtonType continueBtn = new ButtonType("Télécharger", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelBtn = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            info.getButtonTypes().setAll(continueBtn, cancelBtn);
            
            Optional<ButtonType> choice = info.showAndWait();
            // Si l'utilisateur annule ou ferme, on arrête
            if (choice.isEmpty() || choice.get() == cancelBtn) {
                return;
            }
            // Sinon, on continue avec le téléchargement et l'enregistrement
        }

        try {
            // Transcribe audio avec Vosk (local, hors ligne)
            // Le téléchargement du modèle se fait automatiquement ici si nécessaire
            String transcribedText = org.example.util.SpeechUtil.recordAndTranscribe();

            if (transcribedText == null || transcribedText.trim().isEmpty()) {
                showAlert("Aucun texte", "Aucun texte transcrit. Parlez plus fort et plus clairement.", Alert.AlertType.INFORMATION);
                return;
            }

            // Show transcription for review — user can edit before inserting
            Alert reviewAlert = new Alert(Alert.AlertType.CONFIRMATION);
            reviewAlert.setTitle("Transcription vocale");
            reviewAlert.setHeaderText("Vérifier le texte transcrit");

            TextArea textArea = new TextArea(transcribedText);
            textArea.setWrapText(true);
            textArea.setEditable(true);   // user can correct mistakes
            textArea.setPrefRowCount(4);

            VBox vbox = new VBox(8);
            vbox.setPadding(new javafx.geometry.Insets(10));
            
            Label info = new Label("Vous pouvez modifier le texte avant de l'insérer dans le formulaire de commentaire :");
            info.setStyle("-fx-text-fill: #555; -fx-font-size: 11px;");
            info.setWrapText(true);
            vbox.getChildren().addAll(info, textArea);

            reviewAlert.getDialogPane().setContent(vbox);
            reviewAlert.getDialogPane().setPrefWidth(500);

            // Customize button text
            ButtonType insertButton = new ButtonType("Insérer dans le formulaire", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            reviewAlert.getButtonTypes().setAll(insertButton, cancelButton);

            Optional<ButtonType> reviewResult = reviewAlert.showAndWait();

            if (reviewResult.isPresent() && reviewResult.get() == insertButton) {
                // ✅ Insert transcribed text into the content field of the form
                String finalText = textArea.getText().trim();
                contentField.setText(finalText);

                // Open the form if it's not already visible
                if (!commentFormContainer.isVisible()) {
                    editingComment = null;
                    clearForm();
                    formTitle.setText("Nouveau commentaire (Vocal)");
                    commentFormContainer.setVisible(true);
                    commentFormContainer.setManaged(true);
                    addCommentBtn.setText("Annuler");
                }

                // Focus on subject field so user fills it next
                subjectField.requestFocus();

                showAlert("Texte vocal inséré",
                    "Le texte transcrit a été inséré dans le formulaire de commentaire.\nVeuillez remplir le Sujet, le Type, la Cible et l'Importance, puis cliquez sur Enregistrer.",
                    Alert.AlertType.INFORMATION);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "L'enregistrement vocal a échoué : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Lire un commentaire à voix haute (Text-to-Speech)
     */
    private void handleListenComment(Comment comment) {
        try {
            if (!TextToSpeechUtil.isAvailable()) {
                showAlert("TTS non disponible", 
                    "La synthèse vocale n'est pas disponible sur votre système.\n" +
                    "Moteur TTS : " + TextToSpeechUtil.getTTSEngine(),
                    Alert.AlertType.WARNING);
                return;
            }

            // Lire le commentaire de manière asynchrone (non bloquant)
            String textToRead = comment.getSubject() + ". " + comment.getContent();
            TextToSpeechUtil.speakAsync(textToRead);
            
            // Notification
            NotificationUtil.showInfo("Lecture en cours", 
                "Le commentaire est en cours de lecture...");
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de lire le commentaire : " + e.getMessage(), 
                Alert.AlertType.ERROR);
        }
    }

    /**
     * Exporter les commentaires en Excel
     */
    @FXML
    public void handleExportExcel() {
        if (allComments == null || allComments.isEmpty()) {
            showAlert("Avertissement", "Aucun commentaire à exporter", Alert.AlertType.WARNING);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en Excel");
        fileChooser.setInitialFileName(project.getTitle().replaceAll("[^a-zA-Z0-9]", "_") + "_comments.xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        
        File file = fileChooser.showSaveDialog(dialogStage);
        if (file != null) {
            try {
                ExcelExporter.exportComments(allComments, file);
                showAlert("Succès", "Export Excel réussi !", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Erreur", "Échec de l'export Excel : " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    /**
     * Générer un QR Code pour partager le projet
     */
    @FXML
    public void handleGenerateQRCode() {
        try {
            String qrCodePath = QRCodeUtil.generateProjectQRCode(project.getId(), project.getTitle());
            
            if (qrCodePath != null) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("QR Code généré");
                alert.setHeaderText("QR Code créé avec succès !");
                alert.setContentText("Le QR Code a été sauvegardé dans :\n" + qrCodePath + 
                    "\n\nVous pouvez le scanner pour accéder rapidement au projet.");
                
                ButtonType openButton = new ButtonType("Ouvrir le dossier");
                ButtonType closeButton = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(openButton, closeButton);
                
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == openButton) {
                    // Ouvrir le dossier contenant le QR code
                    File qrFile = new File(qrCodePath);
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(qrFile.getParentFile());
                    }
                }
            } else {
                showAlert("Erreur", "Impossible de générer le QR Code", Alert.AlertType.ERROR);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Échec de la génération du QR Code : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Générer des graphiques statistiques
     */
    @FXML
    public void handleGenerateCharts() {
        try {
            if (allComments == null || allComments.isEmpty()) {
                showAlert("Avertissement", "Aucun commentaire pour générer des statistiques", Alert.AlertType.WARNING);
                return;
            }

            // Générer les graphiques
            String chartPath = ChartUtil.generateCommentsTypeChart(allComments, 
                "Commentaires par Type - " + project.getTitle());
            
            if (chartPath != null) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Graphiques générés");
                alert.setHeaderText("Statistiques créées avec succès !");
                alert.setContentText("Les graphiques ont été sauvegardés dans :\n" + chartPath + 
                    "\n\nOuvrir le fichier HTML dans votre navigateur pour voir les graphiques interactifs.");
                
                ButtonType openButton = new ButtonType("Ouvrir dans le navigateur");
                ButtonType closeButton = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(openButton, closeButton);
                
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == openButton) {
                    // Ouvrir le fichier HTML dans le navigateur
                    File chartFile = new File(chartPath);
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(chartFile.toURI());
                    }
                }
            } else {
                showAlert("Erreur", "Impossible de générer les graphiques", Alert.AlertType.ERROR);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Échec de la génération des graphiques : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
