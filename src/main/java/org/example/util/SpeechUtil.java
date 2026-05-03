package org.example.util;

import javax.sound.sampled.*;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;

/**
 * Utility for Speech-to-Text (voice recording → text)
 * Supports multiple providers:
 * 1. Wit.ai (FREE, no credit card required)
 * 2. Google Speech-to-Text API (paid)
 * 3. Test mode (for development)
 */
public class SpeechUtil {

    // ═══════════════════════════════════════════════════════════════════
    // CONFIGURATION - Choose your provider
    // ═══════════════════════════════════════════════════════════════════
    
    // Options: "VOSK", "WIT_AI", "GOOGLE", "TEST"
    // - VOSK: Reconnaissance vocale LOCALE (100% hors ligne, aucune inscription) ⭐ RECOMMANDÉ
    // - WIT_AI: Reconnaissance vocale gratuite via Wit.ai (nécessite compte)
    // - GOOGLE: Reconnaissance vocale payante via Google Cloud
    // - TEST: Mode de démonstration (phrases prédéfinies)
    private static final String PROVIDER = "VOSK";
    
    // ═══════════════════════════════════════════════════════════════════
    // Vosk Configuration (LOCAL - AUCUNE INSCRIPTION REQUISE) ⭐
    // ═══════════════════════════════════════════════════════════════════
    // Vosk télécharge automatiquement le modèle français (~40 MB) la première fois
    // Ensuite, tout fonctionne HORS LIGNE, sans internet !
    // Aucune configuration nécessaire, ça marche directement !
    
    // ═══════════════════════════════════════════════════════════════════
    // Wit.ai Configuration (FREE - Nécessite compte Meta/Facebook)
    // ═══════════════════════════════════════════════════════════════════
    private static final String WIT_AI_TOKEN = "VOTRE_TOKEN_WIT_AI_ICI";
    private static final String WIT_AI_URL = "https://api.wit.ai/speech?v=20220622";
    
    // ═══════════════════════════════════════════════════════════════════
    // Google Speech Configuration (PAID - Nécessite carte bancaire)
    // ═══════════════════════════════════════════════════════════════════
    private static final String GOOGLE_SPEECH_API_KEY = "votre-clé-google-speech";
    private static final String GOOGLE_API_URL =
            "https://speech.googleapis.com/v1/speech:recognize?key=" + GOOGLE_SPEECH_API_KEY;

    // ═══════════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Record audio from microphone and transcribe it to text.
     */
    public static String recordAndTranscribe() throws Exception {
        // Utiliser Vosk par défaut (local, aucune inscription)
        if (PROVIDER.equals("VOSK")) {
            return VoskSpeechUtil.recordAndTranscribe();
        }
        
        if (PROVIDER.equals("TEST")) {
            return recordAndTranscribeTest();
        }

        System.out.println("🎤 Préparation de l'enregistrement...");
        byte[] audioData = recordAudio(5);

        if (audioData == null || audioData.length == 0) {
            throw new Exception("Aucun audio enregistré. Vérifiez votre microphone.");
        }

        System.out.println("📝 Transcription en cours avec " + PROVIDER + "...");
        
        String transcript;
        switch (PROVIDER) {
            case "WIT_AI":
                transcript = transcribeWithWitAi(audioData);
                break;
            case "GOOGLE":
                transcript = transcribeWithGoogle(audioData);
                break;
            default:
                throw new Exception("Provider non supporté : " + PROVIDER);
        }

        if (transcript == null || transcript.trim().isEmpty()) {
            throw new Exception("Impossible de transcrire l'audio. Veuillez parler clairement et réessayer.");
        }

        return transcript;
    }

    public static boolean isMicrophoneAvailable() {
        if (PROVIDER.equals("VOSK")) {
            return VoskSpeechUtil.isMicrophoneAvailable();
        }
        
        try {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            return AudioSystem.isLineSupported(info);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isConfigured() {
        switch (PROVIDER) {
            case "VOSK":
                return true; // Vosk fonctionne toujours, aucune config nécessaire
            case "TEST":
                return false; // Mode TEST = non configuré
            case "WIT_AI":
                return WIT_AI_TOKEN != null && !WIT_AI_TOKEN.equals("VOTRE_TOKEN_WIT_AI_ICI");
            case "GOOGLE":
                return GOOGLE_SPEECH_API_KEY != null && !GOOGLE_SPEECH_API_KEY.startsWith("votre");
            default:
                return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Provider Implementations
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Transcribe audio using Wit.ai (FREE)
     */
    private static String transcribeWithWitAi(byte[] audioData) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(WIT_AI_URL))
                    .header("Authorization", "Bearer " + WIT_AI_TOKEN)
                    .header("Content-Type", "audio/raw;encoding=signed-integer;bits=16;rate=16000;endian=little")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(audioData))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                if (json.has("text")) {
                    String text = json.get("text").getAsString();
                    System.out.println("✅ Transcription réussie : " + text);
                    return text;
                }
                
                // Wit.ai peut aussi retourner "speech" au lieu de "text"
                if (json.has("speech")) {
                    String text = json.get("speech").getAsString();
                    System.out.println("✅ Transcription réussie : " + text);
                    return text;
                }
                
                System.err.println("❌ Aucun texte dans la réponse Wit.ai");
                System.err.println("Réponse : " + response.body());
                return "";
            } else {
                System.err.println("❌ Erreur API Wit.ai : " + response.statusCode());
                System.err.println("Réponse : " + response.body());
                return "";
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur de transcription Wit.ai : " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Transcribe audio using Google Speech API (PAID)
     */
    private static String transcribeWithGoogle(byte[] audioData) {
        return transcribeWithGoogle(audioData, "fr-FR");
    }

    private static String transcribeWithGoogle(byte[] audioData, String languageCode) {
        try {
            String audioBase64 = Base64.getEncoder().encodeToString(audioData);

            JsonObject config = new JsonObject();
            config.addProperty("encoding", "LINEAR16");
            config.addProperty("sampleRateHertz", 16000);
            config.addProperty("languageCode", languageCode);

            JsonObject audio = new JsonObject();
            audio.addProperty("content", audioBase64);

            JsonObject requestBody = new JsonObject();
            requestBody.add("config", config);
            requestBody.add("audio", audio);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GOOGLE_API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                if (json.has("results") && json.getAsJsonArray("results").size() > 0) {
                    String text = json.getAsJsonArray("results")
                            .get(0).getAsJsonObject()
                            .getAsJsonArray("alternatives")
                            .get(0).getAsJsonObject()
                            .get("transcript").getAsString();
                    System.out.println("✅ Transcription réussie : " + text);
                    return text;
                }
                return "";
            } else {
                System.err.println("❌ Erreur API Google Speech : " + response.statusCode());
                System.err.println("Réponse : " + response.body());
                return "";
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur de transcription Google : " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Test Mode
    // ═══════════════════════════════════════════════════════════════════

    private static String recordAndTranscribeTest() throws Exception {
        System.out.println("🎤 [MODE TEST] Simulation d'enregistrement vocal...");
        System.out.println("⚠️  ATTENTION : Vous êtes en mode TEST !");
        System.out.println("   Le système ne transcrit PAS votre voix réelle.");
        System.out.println("   Il retourne des phrases prédéfinies pour tester l'interface.");
        System.out.println("");
        System.out.println("   Pour utiliser la VRAIE reconnaissance vocale :");
        System.out.println("   1. Créez un compte gratuit sur https://wit.ai/");
        System.out.println("   2. Créez une nouvelle app");
        System.out.println("   3. Copiez le 'Server Access Token' depuis Settings");
        System.out.println("   4. Dans SpeechUtil.java, remplacez :");
        System.out.println("      - WIT_AI_TOKEN par votre token");
        System.out.println("      - PROVIDER = \"TEST\" par PROVIDER = \"WIT_AI\"");
        System.out.println("");

        // Simulate a 3-second recording delay so the UI feels realistic
        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}

        System.out.println("✅ [MODE TEST] Simulation d'enregistrement terminée");

        // Return a realistic sample transcription in French
        String[] samples = {
            "Ceci est un commentaire vocal ajouté via la reconnaissance vocale.",
            "Le projet nécessite des améliorations au niveau de la couche base de données.",
            "Je suggère d'ajouter des tests unitaires pour le module d'authentification.",
            "L'interface utilisateur est bien conçue mais les performances peuvent être optimisées.",
            "Veuillez consulter la documentation de l'API avant le prochain sprint."
        };

        // Pick one based on current second (pseudo-random but deterministic)
        int index = (int)(System.currentTimeMillis() / 1000) % samples.length;
        String result = samples[index];

        System.out.println("📝 [MODE TEST] Transcription simulée : " + result);

        return result;
    }

    // ═══════════════════════════════════════════════════════════════════
    // Audio Recording
    // ═══════════════════════════════════════════════════════════════════

    private static byte[] recordAudio(int durationSeconds) {
        try {
            // Configuration audio : 16kHz, 16 bits, mono, signed, little-endian
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
}
