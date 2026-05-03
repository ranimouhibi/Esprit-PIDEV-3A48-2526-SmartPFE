package org.example.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Utilitaire pour générer des QR codes
 * Permet de partager facilement des projets, documents, etc.
 */
public class QRCodeUtil {

    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 300;

    /**
     * Générer un QR code et le sauvegarder dans un fichier
     */
    public static void generateQRCode(String text, String filePath, int width, int height) 
            throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        
        Path path = FileSystems.getDefault().getPath(filePath);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
        
        System.out.println("✅ QR Code généré : " + filePath);
    }

    /**
     * Display a QR code in a JavaFX Dialog
     */
    public static void showQRCodeDialog(String qrCodePath, String title) {
        try {
            File qrFile = new File(qrCodePath);
            if (!qrFile.exists()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("QR Code not found");
                alert.setContentText("The QR code file does not exist: " + qrCodePath);
                alert.showAndWait();
                return;
            }

            // Create the Dialog
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("QR Code - " + title);
            dialog.setHeaderText("Scan this QR code with your smartphone");

            // Load the image
            Image qrImage = new Image(qrFile.toURI().toString());
            ImageView imageView = new ImageView(qrImage);
            imageView.setFitWidth(350);
            imageView.setFitHeight(350);
            imageView.setPreserveRatio(true);

            // Create content
            VBox content = new VBox(15);
            content.setAlignment(Pos.CENTER);
            content.setStyle("-fx-padding: 20;");
            
            Label infoLabel = new Label("📱 Use your phone's camera to scan");
            infoLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #666;");
            
            content.getChildren().addAll(imageView, infoLabel);
            dialog.getDialogPane().setContent(content);

            // Buttons
            ButtonType saveButton = new ButtonType("💾 Save", ButtonBar.ButtonData.LEFT);
            ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButton, closeButton);

            // Button styles
            dialog.getDialogPane().lookupButton(saveButton).setStyle(
                "-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 13px;"
            );
            dialog.getDialogPane().lookupButton(closeButton).setStyle(
                "-fx-background-color: #4a5568; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 13px;"
            );

            // Save button action
            ((Button) dialog.getDialogPane().lookupButton(saveButton)).setOnAction(e -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save QR Code");
                fileChooser.setInitialFileName("qrcode_" + title.replaceAll("[^a-zA-Z0-9]", "_") + ".png");
                fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PNG Image", "*.png")
                );
                
                File saveFile = fileChooser.showSaveDialog(dialog.getOwner());
                if (saveFile != null) {
                    try {
                        Files.copy(qrFile.toPath(), saveFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        NotificationUtil.showSuccess("QR Code saved", 
                            "QR code has been saved: " + saveFile.getName());
                    } catch (IOException ex) {
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle("Error");
                        errorAlert.setHeaderText("Save error");
                        errorAlert.setContentText("Unable to save QR code: " + ex.getMessage());
                        errorAlert.showAndWait();
                    }
                }
            });

            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Display error");
            alert.setContentText("Unable to display QR code: " + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Générer un QR code pour un projet
     */
    public static String generateProjectQRCode(int projectId, String projectTitle) {
        try {
            // Créer le dossier qrcodes s'il n'existe pas
            File qrcodesDir = new File("qrcodes");
            if (!qrcodesDir.exists()) {
                qrcodesDir.mkdirs();
            }

            // Générer l'URL du projet (vous pouvez adapter selon votre besoin)
            String projectUrl = "smartpfe://project/" + projectId;
            
            // Nom du fichier
            String fileName = "qrcodes/project_" + projectId + ".png";
            
            // Générer le QR code
            generateQRCode(projectUrl, fileName, DEFAULT_WIDTH, DEFAULT_HEIGHT);
            
            // Notification
            NotificationUtil.Notifications.qrCodeGenerated(projectTitle);
            
            return fileName;
            
        } catch (Exception e) {
            System.err.println("❌ Erreur génération QR Code : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Générer ET afficher un QR code pour un projet
     */
    public static void generateAndShowProjectQRCode(int projectId, String projectTitle) {
        String qrPath = generateProjectQRCode(projectId, projectTitle);
        if (qrPath != null) {
            showQRCodeDialog(qrPath, projectTitle);
        }
    }

    /**
     * Générer un QR code pour un document
     */
    public static String generateDocumentQRCode(int documentId, String documentName) {
        try {
            File qrcodesDir = new File("qrcodes");
            if (!qrcodesDir.exists()) {
                qrcodesDir.mkdirs();
            }

            String documentUrl = "smartpfe://document/" + documentId;
            String fileName = "qrcodes/document_" + documentId + ".png";
            
            generateQRCode(documentUrl, fileName, DEFAULT_WIDTH, DEFAULT_HEIGHT);
            
            System.out.println("✅ QR Code document généré : " + fileName);
            return fileName;
            
        } catch (Exception e) {
            System.err.println("❌ Erreur génération QR Code document : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Générer un QR code pour une URL personnalisée
     */
    public static String generateCustomQRCode(String url, String fileName) {
        try {
            File qrcodesDir = new File("qrcodes");
            if (!qrcodesDir.exists()) {
                qrcodesDir.mkdirs();
            }

            String fullPath = "qrcodes/" + fileName;
            generateQRCode(url, fullPath, DEFAULT_WIDTH, DEFAULT_HEIGHT);
            
            return fullPath;
            
        } catch (Exception e) {
            System.err.println("❌ Erreur génération QR Code : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Générer un QR code avec texte (pour partage rapide)
     */
    public static String generateTextQRCode(String text, String fileName) {
        try {
            File qrcodesDir = new File("qrcodes");
            if (!qrcodesDir.exists()) {
                qrcodesDir.mkdirs();
            }

            String fullPath = "qrcodes/" + fileName;
            generateQRCode(text, fullPath, DEFAULT_WIDTH, DEFAULT_HEIGHT);
            
            return fullPath;
            
        } catch (Exception e) {
            System.err.println("❌ Erreur génération QR Code : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Générer un QR code pour partager les informations d'un projet
     * (format JSON ou texte structuré)
     */
    public static String generateProjectShareQRCode(int projectId, String projectTitle, 
                                                     String description, String supervisor) {
        try {
            File qrcodesDir = new File("qrcodes");
            if (!qrcodesDir.exists()) {
                qrcodesDir.mkdirs();
            }

            // Créer un texte structuré avec les infos du projet
            String projectInfo = String.format(
                "Projet: %s\nID: %d\nDescription: %s\nEncadrant: %s\nURL: smartpfe://project/%d",
                projectTitle, projectId, 
                description != null && description.length() > 50 
                    ? description.substring(0, 50) + "..." 
                    : description,
                supervisor != null ? supervisor : "Non assigné",
                projectId
            );
            
            String fileName = "qrcodes/share_project_" + projectId + ".png";
            generateQRCode(projectInfo, fileName, 400, 400); // Plus grand pour plus d'infos
            
            return fileName;
            
        } catch (Exception e) {
            System.err.println("❌ Erreur génération QR Code partage : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
