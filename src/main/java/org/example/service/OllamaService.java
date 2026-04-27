package org.example.service;

import org.example.model.Meeting;
import org.example.model.MeetingReport;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * Service de génération de résumés IA via Ollama (100% local, 100% gratuit).
 * Ollama doit être installé et démarré sur localhost:11434.
 * Modèle recommandé : llama3.2 (léger et rapide)
 *
 * Installation :
 *   1. Télécharger Ollama : https://ollama.com/download
 *   2. Dans un terminal : ollama pull llama3.2
 *   3. Ollama démarre automatiquement au lancement de Windows
 */
public class OllamaService {

    private static final Logger LOG = Logger.getLogger(OllamaService.class.getName());
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL = "llama3.2";

    /**
     * Vérifie si Ollama est disponible sur localhost:11434.
     */
    public boolean isAvailable() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:11434").openConnection();
            conn.setConnectTimeout(2000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Génère un résumé du meeting à partir du report.
     * @return le résumé généré, ou null en cas d'erreur
     */
    public String generateMeetingSummary(Meeting meeting, MeetingReport report) {
        try {
            String prompt = buildPrompt(meeting, report);
            String raw = callOllama(prompt);
            return cleanResponse(raw);
        } catch (Exception e) {
            LOG.severe("Ollama error: " + e.getMessage());
            return null;
        }
    }

    private String buildPrompt(Meeting meeting, MeetingReport report) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String dateStr = meeting.getScheduledDate() != null
            ? meeting.getScheduledDate().format(fmt) : "N/A";

        return "Write a single short paragraph (3 sentences maximum) summarizing this meeting. "
            + "No bullet points. No lists. No headers. No asterisks. No numbers. "
            + "Just one flowing paragraph of plain text.\n\n"
            + "Project: " + safe(meeting.getProjectTitle()) + "\n"
            + "Date: " + dateStr + "\n"
            + "Decisions: " + safe(report.getDecisions()) + "\n"
            + "Action Items: " + safe(report.getActionItems()) + "\n\n"
            + "One paragraph summary:";
    }

    private String callOllama(String prompt) throws IOException {
        return callOllama(prompt, 120);
    }

    private String callOllama(String prompt, int maxTokens) throws IOException {
        String body = "{\"model\":\"" + MODEL + "\","
            + "\"prompt\":" + jsonString(prompt) + ","
            + "\"stream\":false,"
            + "\"options\":{\"temperature\":0.5,\"num_predict\":" + maxTokens + "}}";

        HttpURLConnection conn = (HttpURLConnection) new URL(OLLAMA_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(120000); // 2 min max pour la génération

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        if (status != 200) {
            throw new IOException("Ollama error " + status + ": " + response);
        }

        return extractResponse(response);
    }

    /**
     * Extrait le champ "response" du JSON Ollama.
     * Format : {"model":"llama3.2","response":"...","done":true,...}
     */
    private String extractResponse(String json) {
        String marker = "\"response\":";
        int idx = json.indexOf(marker);
        if (idx < 0) throw new RuntimeException("Unexpected Ollama response: " + json);
        int start = json.indexOf('"', idx + marker.length()) + 1;
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '\\') { end += 2; continue; }
            if (c == '"') break;
            end++;
        }
        return json.substring(start, end)
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .trim();
    }

    /** Nettoie la réponse : supprime les puces, numéros, headers. */
    private String cleanResponse(String raw) {
        if (raw == null) return null;
        // Supprimer les lignes qui commencent par *, -, 1., 2., #, etc.
        String[] lines = raw.split("\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            // Sauter les lignes de type liste ou header
            if (trimmed.matches("^[\\*\\-#>].*") || trimmed.matches("^\\d+[\\.\\)].*")) {
                // Extraire juste le texte après le marqueur
                String text = trimmed.replaceFirst("^[\\*\\-#>\\d\\.\\)]+\\s*", "").trim();
                if (!text.isEmpty()) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(text);
                }
            } else {
                if (sb.length() > 0) sb.append(" ");
                sb.append(trimmed);
            }
        }
        // Limiter à ~300 caractères
        String result = sb.toString().trim();
        if (result.length() > 350) {
            int lastDot = result.lastIndexOf('.', 350);
            result = lastDot > 100 ? result.substring(0, lastDot + 1) : result.substring(0, 350) + "...";
        }
        return result;
    }

    private String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t") + "\"";
    }

    private String safe(String s) { return s != null ? s : ""; }

    // ─── Live Meeting Analysis ────────────────────────────────────────────────

    public LiveInsight analyzeSegment(String transcript) {
        try {
            String prompt = "Analyse ce segment de meeting en francais.\n"
                + "Reponds UNIQUEMENT en JSON avec ce format exact (pas d'autre texte) :\n"
                + "{\"points\":[\"...\"],\"decisions\":[\"...\"],\"actions\":[\"...\"],\"alerts\":[\"...\"]}\n"
                + "Si une categorie est vide, mets un tableau vide [].\n"
                + "Segment : " + transcript;
            String json = callOllama(prompt, 300);
            return parseInsight(json);
        } catch (Exception e) {
            LOG.warning("analyzeSegment error: " + e.getMessage());
            return new LiveInsight();
        }
    }

    private LiveInsight parseInsight(String json) {
        LiveInsight insight = new LiveInsight();
        if (json == null || json.isBlank()) return insight;
        insight.points   = extractJsonArray(json, "points");
        insight.decisions = extractJsonArray(json, "decisions");
        insight.actions  = extractJsonArray(json, "actions");
        insight.alerts   = extractJsonArray(json, "alerts");
        return insight;
    }

    private java.util.List<String> extractJsonArray(String json, String key) {
        java.util.List<String> result = new java.util.ArrayList<>();
        String marker = "\"" + key + "\":[";
        int start = json.indexOf(marker);
        if (start < 0) return result;
        start += marker.length();
        int end = json.indexOf("]", start);
        if (end < 0) return result;
        String content = json.substring(start, end).trim();
        if (content.isEmpty()) return result;
        for (String item : content.split(",")) {
            String clean = item.trim().replaceAll("^\"|\"$", "").trim();
            if (!clean.isEmpty() && !clean.equals("...")) result.add(clean);
        }
        return result;
    }

    // ─── Chatbot Q&A ─────────────────────────────────────────────────────────

    public String chat(String question, String meetingsContext) {
        try {
            String prompt = "Tu es un assistant intelligent pour la gestion de meetings academiques (PFE).\n"
                + "Reponds en francais, de facon concise et precise.\n"
                + "Voici les donnees actuelles des meetings :\n"
                + meetingsContext + "\n\n"
                + "Question : " + question + "\n"
                + "Reponse :";
            return callOllama(prompt, 250).trim();
        } catch (Exception e) {
            LOG.severe("Ollama chat error: " + e.getMessage());
            return "Erreur : " + e.getMessage();
        }
    }

    // ─── Follow-up Email ─────────────────────────────────────────────────────

    public String generateFollowUpEmail(Meeting meeting, MeetingReport report) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String dateStr = meeting.getScheduledDate() != null ? meeting.getScheduledDate().format(fmt) : "N/A";
            String decisions = report != null ? safe(report.getDecisions()) : "Non renseigne";
            String actions   = report != null ? safe(report.getActionItems()) : "Non renseigne";
            String notes     = report != null && report.getDiscussionPoints() != null ? report.getDiscussionPoints() : "";

            String prompt = "Genere un email de suivi professionnel en francais pour ce meeting.\n"
                + "L'email doit avoir : une salutation generale, un resume du meeting, "
                + "les decisions prises, les actions a faire, et une conclusion cordiale.\n"
                + "Format : texte brut uniquement, pas de markdown, pas d'asterisques, pas de tirets.\n\n"
                + "Projet : " + safe(meeting.getProjectTitle()) + "\n"
                + "Date : " + dateStr + "\n"
                + "Type : " + safe(meeting.getMeetingType()) + "\n"
                + "Decisions : " + decisions + "\n"
                + "Actions : " + actions + "\n"
                + (notes.isBlank() ? "" : "Notes : " + notes + "\n")
                + "\nEmail de suivi :";
            return callOllama(prompt, 450).trim();
        } catch (Exception e) {
            LOG.severe("Ollama follow-up error: " + e.getMessage());
            return null;
        }
    }
}
