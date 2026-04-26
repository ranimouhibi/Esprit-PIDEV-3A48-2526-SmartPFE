package org.example.util;

import java.awt.*;
import java.awt.TrayIcon.MessageType;

/**
 * Utilitaire pour les notifications desktop
 * Affiche des notifications système natives
 */
public class NotificationUtil {
    
    private static TrayIcon trayIcon = null;
    private static boolean initialized = false;

    /**
     * Initialiser le système de notifications
     */
    private static void initialize() {
        if (initialized) return;
        
        try {
            if (SystemTray.isSupported()) {
                SystemTray tray = SystemTray.getSystemTray();
                
                // Créer une icône simple (carré bleu)
                Image image = createDefaultIcon();
                
                trayIcon = new TrayIcon(image, "SmartPFE");
                trayIcon.setImageAutoSize(true);
                trayIcon.setToolTip("SmartPFE - Gestion de Projets");
                
                try {
                    tray.add(trayIcon);
                    initialized = true;
                    System.out.println("✅ Système de notifications initialisé");
                } catch (AWTException e) {
                    System.err.println("⚠️ Impossible d'ajouter l'icône au system tray");
                }
            } else {
                System.err.println("⚠️ System Tray non supporté sur ce système");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur initialisation notifications : " + e.getMessage());
        }
    }

    /**
     * Créer une icône par défaut (carré bleu)
     */
    private static Image createDefaultIcon() {
        int size = 16;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
            size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(59, 130, 246)); // Bleu
        g.fillRoundRect(0, 0, size, size, 4, 4);
        g.dispose();
        return image;
    }

    /**
     * Afficher une notification d'information
     */
    public static void showInfo(String title, String message) {
        initialize();
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, MessageType.INFO);
        }
    }

    /**
     * Afficher une notification d'avertissement
     */
    public static void showWarning(String title, String message) {
        initialize();
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, MessageType.WARNING);
        }
    }

    /**
     * Afficher une notification d'erreur
     */
    public static void showError(String title, String message) {
        initialize();
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, MessageType.ERROR);
        }
    }

    /**
     * Afficher une notification de succès
     */
    public static void showSuccess(String title, String message) {
        initialize();
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, MessageType.INFO);
        }
    }

    /**
     * Vérifier si les notifications sont supportées
     */
    public static boolean isSupported() {
        return SystemTray.isSupported();
    }

    /**
     * Notifications prédéfinies pour l'application
     */
    public static class Notifications {
        
        public static void newComment(String projectName) {
            showInfo("Nouveau commentaire", 
                "Un nouveau commentaire a été ajouté sur le projet : " + projectName);
        }

        public static void projectCreated(String projectName) {
            showSuccess("Projet créé", 
                "Le projet \"" + projectName + "\" a été créé avec succès");
        }

        public static void projectUpdated(String projectName) {
            showInfo("Projet mis à jour", 
                "Le projet \"" + projectName + "\" a été modifié");
        }

        public static void documentUploaded(String documentName) {
            showSuccess("Document ajouté", 
                "Le document \"" + documentName + "\" a été téléchargé");
        }

        public static void deadlineApproaching(String projectName, int daysLeft) {
            showWarning("Deadline proche", 
                "Le projet \"" + projectName + "\" doit être rendu dans " + daysLeft + " jour(s)");
        }

        public static void taskAssigned(String taskName) {
            showInfo("Nouvelle tâche", 
                "Une nouvelle tâche vous a été assignée : " + taskName);
        }

        public static void exportSuccess(String fileName) {
            showSuccess("Export réussi", 
                "Le fichier \"" + fileName + "\" a été exporté avec succès");
        }

        public static void qrCodeGenerated(String projectName) {
            showSuccess("QR Code généré", 
                "Le QR Code pour \"" + projectName + "\" a été créé");
        }
    }
}
