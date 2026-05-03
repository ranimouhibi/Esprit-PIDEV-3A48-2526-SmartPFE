package org.example.util;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioInputStream;
import java.io.*;

/**
 * Utilitaire pour la synthèse vocale (Text-to-Speech)
 * Lit les commentaires et textes à voix haute
 * 
 * Note: Utilise une approche simple avec la commande système
 * Pour Windows: PowerShell Add-Type -AssemblyName System.Speech
 * Pour Linux: espeak ou festival
 * Pour Mac: say
 */
public class TextToSpeechUtil {

    private static String OS = System.getProperty("os.name").toLowerCase();
    private static boolean isWindows = OS.contains("win");
    private static boolean isMac = OS.contains("mac");
    private static boolean isLinux = OS.contains("nux");

    /**
     * Lire un texte à voix haute
     */
    public static void speak(String text) {
        if (text == null || text.trim().isEmpty()) {
            System.err.println("⚠️ Texte vide, impossible de lire");
            return;
        }

        try {
            if (isWindows) {
                speakWindows(text);
            } else if (isMac) {
                speakMac(text);
            } else if (isLinux) {
                speakLinux(text);
            } else {
                System.err.println("⚠️ Système d'exploitation non supporté pour TTS");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur Text-to-Speech : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Lire un texte sur Windows (PowerShell)
     */
    private static void speakWindows(String text) throws Exception {
        // Échapper les guillemets
        String escapedText = text.replace("\"", "`\"").replace("'", "''");
        
        // Commande PowerShell pour TTS
        String command = "powershell -Command \"" +
            "Add-Type -AssemblyName System.Speech; " +
            "$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
            "$speak.Speak('" + escapedText + "')\"";
        
        System.out.println("🔊 Lecture en cours (Windows)...");
        
        Process process = Runtime.getRuntime().exec(command);
        
        // Lire les erreurs éventuelles
        BufferedReader errorReader = new BufferedReader(
            new InputStreamReader(process.getErrorStream())
        );
        String line;
        while ((line = errorReader.readLine()) != null) {
            System.err.println(line);
        }
        
        process.waitFor();
        System.out.println("✅ Lecture terminée");
    }

    /**
     * Lire un texte sur Mac (commande say)
     */
    private static void speakMac(String text) throws Exception {
        System.out.println("🔊 Lecture en cours (Mac)...");
        
        ProcessBuilder processBuilder = new ProcessBuilder("say", text);
        Process process = processBuilder.start();
        process.waitFor();
        
        System.out.println("✅ Lecture terminée");
    }

    /**
     * Lire un texte sur Linux (espeak)
     */
    private static void speakLinux(String text) throws Exception {
        System.out.println("🔊 Lecture en cours (Linux)...");
        
        // Essayer espeak d'abord
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("espeak", text);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                System.out.println("✅ Lecture terminée");
                return;
            }
        } catch (IOException e) {
            System.err.println("⚠️ espeak non trouvé, essai avec festival...");
        }
        
        // Essayer festival si espeak n'est pas disponible
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("festival", "--tts");
            Process process = processBuilder.start();
            
            // Envoyer le texte à festival
            try (PrintWriter writer = new PrintWriter(process.getOutputStream())) {
                writer.println(text);
            }
            
            process.waitFor();
            System.out.println("✅ Lecture terminée");
        } catch (IOException e) {
            System.err.println("❌ Ni espeak ni festival ne sont installés");
            System.err.println("   Installez espeak: sudo apt-get install espeak");
        }
    }

    /**
     * Lire un commentaire à voix haute
     */
    public static void speakComment(String subject, String content) {
        String text = "Commentaire : " + subject + ". " + content;
        speak(text);
    }

    /**
     * Lire un commentaire de manière asynchrone (non bloquant)
     */
    public static void speakAsync(String text) {
        new Thread(() -> speak(text)).start();
    }

    /**
     * Lire un commentaire de manière asynchrone
     */
    public static void speakCommentAsync(String subject, String content) {
        new Thread(() -> speakComment(subject, content)).start();
    }

    /**
     * Vérifier si TTS est disponible sur le système
     */
    public static boolean isAvailable() {
        if (isWindows) {
            // Windows a toujours PowerShell avec System.Speech
            return true;
        } else if (isMac) {
            // Mac a toujours la commande 'say'
            return true;
        } else if (isLinux) {
            // Vérifier si espeak ou festival est installé
            try {
                Process process = Runtime.getRuntime().exec("which espeak");
                int exitCode = process.waitFor();
                if (exitCode == 0) return true;
                
                process = Runtime.getRuntime().exec("which festival");
                exitCode = process.waitFor();
                return exitCode == 0;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Obtenir le nom du moteur TTS utilisé
     */
    public static String getTTSEngine() {
        if (isWindows) return "Windows Speech Synthesis";
        if (isMac) return "macOS Say";
        if (isLinux) return "espeak/festival";
        return "Non disponible";
    }

    /**
     * Messages prédéfinis pour l'application
     */
    public static class Messages {
        
        public static void welcome(String userName) {
            speakAsync("Bienvenue " + userName);
        }

        public static void newComment() {
            speakAsync("Vous avez reçu un nouveau commentaire");
        }

        public static void projectCreated(String projectName) {
            speakAsync("Le projet " + projectName + " a été créé avec succès");
        }

        public static void deadlineWarning(int daysLeft) {
            speakAsync("Attention, il reste " + daysLeft + " jours avant la deadline");
        }

        public static void exportSuccess() {
            speakAsync("Export réussi");
        }

        public static void error(String message) {
            speakAsync("Erreur : " + message);
        }
    }

    /**
     * Test de la fonctionnalité TTS
     */
    public static void test() {
        System.out.println("=== Test Text-to-Speech ===");
        System.out.println("Système : " + OS);
        System.out.println("Moteur TTS : " + getTTSEngine());
        System.out.println("Disponible : " + (isAvailable() ? "Oui" : "Non"));
        System.out.println("");
        
        if (isAvailable()) {
            System.out.println("Test de lecture...");
            speak("Bonjour, ceci est un test de synthèse vocale pour SmartPFE");
        } else {
            System.err.println("❌ TTS non disponible sur ce système");
        }
    }
}
