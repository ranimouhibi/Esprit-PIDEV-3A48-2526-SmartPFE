package org.example.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Reconnaissance vocale LOCALE avec Vosk (100% hors ligne, aucune inscription)
 * Télécharge automatiquement le modèle français la première fois
 */
public class VoskSpeechUtil {

    private static final String MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip";
    private static final String MODEL_DIR = System.getProperty("user.home") + "/.smartpfe/vosk-model-fr";
    private static Model model = null;

    /**
     * Enregistrer et transcrire la voix (100% local, aucune connexion internet après téléchargement)
     */
    public static String recordAndTranscribe() throws Exception {
        // Initialiser le modèle (télécharge si nécessaire)
        if (!initializeModel()) {
            throw new Exception("Impossible d'initialiser le modèle Vosk");
        }

        System.out.println("🎤 Préparation de l'enregistrement...");
        byte[] audioData = recordAudio(5);

        if (audioData == null || audioData.length == 0) {
            throw new Exception("Aucun audio enregistré. Vérifiez votre microphone.");
        }

        System.out.println("📝 Transcription en cours (local, hors ligne)...");
        String transcript = transcribeAudio(audioData);

        if (transcript == null || transcript.trim().isEmpty()) {
            throw new Exception("Impossible de transcrire l'audio. Veuillez parler clairement et réessayer.");
        }

        return transcript;
    }

    /**
     * Initialiser le modèle Vosk (télécharge si nécessaire)
     */
    private static boolean initializeModel() {
        try {
            // Vérifier si le modèle existe déjà
            File modelDir = new File(MODEL_DIR);
            if (!modelDir.exists()) {
                System.out.println("📥 Téléchargement du modèle français (une seule fois, ~40 MB)...");
                System.out.println("   Cela peut prendre 1-2 minutes selon votre connexion.");
                downloadAndExtractModel();
                System.out.println("✅ Modèle téléchargé et installé !");
            }

            // Charger le modèle
            if (model == null) {
                LibVosk.setLogLevel(LogLevel.WARNINGS);
                model = new Model(MODEL_DIR);
                System.out.println("✅ Modèle Vosk chargé (reconnaissance locale activée)");
            }

            return true;
        } catch (Exception e) {
            System.err.println("❌ Erreur d'initialisation du modèle : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Télécharger et extraire le modèle français
     */
    private static void downloadAndExtractModel() throws Exception {
        // Créer le dossier parent
        File parentDir = new File(System.getProperty("user.home") + "/.smartpfe");
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Télécharger le modèle
        System.out.println("   Téléchargement depuis : " + MODEL_URL);
        URL url = new URL(MODEL_URL);
        
        try (InputStream in = url.openStream();
             ZipInputStream zipIn = new ZipInputStream(in)) {
            
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                String entryName = entry.getName();
                
                // Extraire dans le bon dossier
                // Le zip contient "vosk-model-small-fr-0.22/..." donc on enlève le préfixe
                String fileName = entryName;
                if (entryName.contains("/")) {
                    fileName = entryName.substring(entryName.indexOf("/") + 1);
                }
                
                if (fileName.isEmpty()) continue;
                
                File file = new File(MODEL_DIR, fileName);
                
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zipIn.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                    }
                }
                
                zipIn.closeEntry();
            }
        }
        
        System.out.println("   Extraction terminée !");
    }

    /**
     * Enregistrer l'audio depuis le microphone
     */
    private static byte[] recordAudio(int durationSeconds) {
        try {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("❌ Format audio non supporté");
                return null;
            }

            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            System.out.println("🔴 Enregistrement en cours... (" + durationSeconds + " secondes)");
            System.out.println("   🎙️  Parlez maintenant dans votre microphone !");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            long endTime = System.currentTimeMillis() + (durationSeconds * 1000L);

            while (System.currentTimeMillis() < endTime) {
                int count = line.read(buffer, 0, buffer.length);
                if (count > 0) out.write(buffer, 0, count);
            }

            line.stop();
            line.close();

            byte[] audioData = out.toByteArray();
            System.out.println("✅ Enregistrement terminé (" + audioData.length + " bytes)");
            return audioData;

        } catch (Exception e) {
            System.err.println("❌ Erreur d'enregistrement : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Transcrire l'audio avec Vosk (local, hors ligne)
     */
    private static String transcribeAudio(byte[] audioData) {
        try {
            Recognizer recognizer = new Recognizer(model, 16000);

            // Traiter l'audio en une seule fois
            recognizer.acceptWaveForm(audioData, audioData.length);

            // Obtenir le résultat final
            String result = recognizer.getFinalResult();
            recognizer.close();

            // Parser le JSON
            JsonObject json = JsonParser.parseString(result).getAsJsonObject();
            String text = json.has("text") ? json.get("text").getAsString() : "";

            if (text != null && !text.trim().isEmpty()) {
                System.out.println("✅ Transcription réussie : " + text);
                return text.trim();
            }

            System.err.println("⚠️ Aucun texte transcrit (parlez plus fort ou plus clairement)");
            return "";

        } catch (Exception e) {
            System.err.println("❌ Erreur de transcription : " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Vérifier si le microphone est disponible
     */
    public static boolean isMicrophoneAvailable() {
        try {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            return AudioSystem.isLineSupported(info);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Vérifier si Vosk est configuré (toujours vrai, aucune config nécessaire)
     */
    public static boolean isConfigured() {
        return true; // Vosk fonctionne toujours, télécharge le modèle automatiquement
    }

    /**
     * Vérifier si le modèle est déjà téléchargé
     */
    public static boolean isModelDownloaded() {
        File modelDir = new File(MODEL_DIR);
        return modelDir.exists() && modelDir.isDirectory();
    }
}
