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
 * Service de génération de résumés IA via OpenAI GPT-3.5-turbo.
 * Nécessite la variable d'environnement OPENAI_API_KEY.
 */
public class OpenAIService {

    private static final Logger LOG = Logger.getLogger(OpenAIService.class.getName());
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-3.5-turbo";

    private final String apiKey;
    private final boolean enabled;

    public OpenAIService() {
        this.apiKey = System.getenv("OPENAI_API_KEY");
        this.enabled = apiKey != null && !apiKey.isBlank();
        if (!enabled) {
            LOG.warning("OpenAIService désactivé : OPENAI_API_KEY non configuré.");
        }
    }

    public boolean isEnabled() { return enabled; }

    /**
     * Génère un résumé IA pour un meeting à partir de son dernier report.
     * @return le résumé généré, ou null en cas d'erreur
     */
    public String generateMeetingSummary(Meeting meeting, MeetingReport report) {
        if (!enabled) return null;
        try {
            String userPrompt = buildPrompt(meeting, report);
            return callOpenAI(userPrompt);
        } catch (Exception e) {
            LOG.severe("Erreur OpenAI : " + e.getMessage());
            return null;
        }
    }

    private String buildPrompt(Meeting meeting, MeetingReport report) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String dateStr = meeting.getScheduledDate() != null ? meeting.getScheduledDate().format(fmt) : "N/A";
        return "Meeting: " + safe(meeting.getProjectTitle()) + " - " + safe(meeting.getMeetingType()) + "\n"
                + "Date: " + dateStr + "\n"
                + "Duration: " + meeting.getDuration() + " minutes\n"
                + "Location: " + safe(meeting.getLocation()) + "\n\n"
                + "Discussion Points:\n" + safe(report.getDiscussionPoints()) + "\n\n"
                + "Decisions Made:\n" + safe(report.getDecisions()) + "\n\n"
                + "Action Items:\n" + safe(report.getActionItems()) + "\n\n"
                + "Next Steps:\n" + safe(report.getNextSteps()) + "\n\n"
                + "Please provide a concise summary of this meeting.";
    }

    private String callOpenAI(String userPrompt) throws IOException {
        String systemPrompt = "You are an expert meeting summarizer. Given the meeting report details, "
                + "generate a concise professional summary highlighting key decisions and action items.";

        // Build JSON body manually (no external JSON library needed)
        String body = "{"
                + "\"model\":\"" + MODEL + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":" + jsonString(systemPrompt) + "},"
                + "{\"role\":\"user\",\"content\":" + jsonString(userPrompt) + "}"
                + "],"
                + "\"max_tokens\":500,"
                + "\"temperature\":0.7"
                + "}";

        HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        if (status != 200) {
            throw new IOException("OpenAI API error " + status + ": " + response);
        }

        return extractContent(response);
    }

    /**
     * Extrait le contenu du champ choices[0].message.content de la réponse JSON.
     * Parsing minimal sans bibliothèque externe.
     */
    private String extractContent(String json) {
        String marker = "\"content\":";
        int idx = json.indexOf(marker);
        if (idx < 0) throw new RuntimeException("Réponse OpenAI inattendue : " + json);
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
                .replace("\\\\", "\\");
    }

    /** Encode une chaîne en JSON string (avec guillemets). */
    private String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    private String safe(String s) { return s != null ? s : ""; }
}
