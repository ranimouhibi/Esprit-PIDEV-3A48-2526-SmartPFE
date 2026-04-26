package org.example.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

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
