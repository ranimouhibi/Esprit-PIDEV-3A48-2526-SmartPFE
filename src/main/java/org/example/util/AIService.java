package org.example.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Calls the Google Gemini API (gemini-2.0-flash) to generate content.
 *
 * Free tier: 15 requests/min, 1 million tokens/day — no credit card needed.
 * Get your API key at: https://aistudio.google.com/app/apikey
 *
 * Set it via environment variable GEMINI_API_KEY,
 * or replace the fallback string below.
 */
public class AIService {

    // ── Configure your API key here ───────────────────────────────────────────
    private static final String API_KEY = System.getenv("GEMINI_API_KEY") != null
            ? System.getenv("GEMINI_API_KEY")
            : "YOUR_GEMINI_API_KEY"; // set env var GEMINI_API_KEY or replace this
    // ─────────────────────────────────────────────────────────────────────────

    private static final String MODEL   = "gemini-2.5-flash";
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/"
            + MODEL + ":generateContent?key=" + API_KEY;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /**
     * Generate a professional bio for a user.
     *
     * @param name         Full name
     * @param role         "student", "supervisor", or "establishment"
     * @param institution  Establishment/university name (may be null)
     * @param skills       Comma-separated skills (may be null)
     * @param experience   Experience description (may be null)
     * @return Generated bio text
     */
    public static String generateBio(String name, String role, String institution,
                                     String skills, String experience, String formations)
            throws Exception {

        String roleLabel = switch (role) {
            case "student"       -> "university student";
            case "supervisor"    -> "academic project supervisor";
            case "establishment" -> "educational institution representative";
            default              -> role;
        };

        StringBuilder prompt = new StringBuilder();
        prompt.append("Write a concise, professional bio (3-4 sentences) for ")
              .append(name).append(", a ").append(roleLabel);

        if (institution != null && !institution.isBlank()) {
            prompt.append(" at ").append(institution);
        }
        if (skills != null && !skills.isBlank()) {
            prompt.append(". Skills: ").append(skills);
        }
        if (experience != null && !experience.isBlank()) {
            prompt.append(". Experience: ").append(experience);
        }
        if (formations != null && !formations.isBlank()) {
            prompt.append(". Education/Formations: ").append(formations);
        }
        prompt.append(". Write in third person. Be professional but approachable. "
                + "Always write complete sentences. Do not cut off mid-sentence. No bullet points.");

        // Build Gemini request body
        // POST /v1beta/models/gemini-2.0-flash:generateContent
        // { "contents": [{ "parts": [{ "text": "..." }] }] }
        JSONObject part     = new JSONObject().put("text", prompt.toString());
        JSONObject content  = new JSONObject().put("parts", new JSONArray().put(part));
        JSONObject body     = new JSONObject().put("contents", new JSONArray().put(content));

        // Generation config — enough tokens for a full 3-4 sentence bio
        JSONObject genConfig = new JSONObject()
                .put("maxOutputTokens", 512)
                .put("temperature", 0.7)
                .put("stopSequences", new JSONArray()); // no early stop
        body.put("generationConfig", genConfig);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("[AIService] HTTP " + response.statusCode() + " — " + response.body());
            JSONObject err = new JSONObject(response.body());
            String raw = err.optJSONObject("error") != null
                    ? err.getJSONObject("error").optString("message", "")
                    : "";
            // Map known API errors to friendly messages
            String friendly;
            if (raw.contains("quota") || raw.contains("RESOURCE_EXHAUSTED")) {
                friendly = "Daily AI quota reached. Please try again tomorrow.";
            } else if (raw.contains("API_KEY") || raw.contains("invalid") || response.statusCode() == 400) {
                friendly = "Invalid API key. Please check your Gemini configuration.";
            } else if (response.statusCode() == 429) {
                friendly = "Too many requests. Please wait a moment and try again.";
            } else if (response.statusCode() >= 500) {
                friendly = "Gemini service is temporarily unavailable. Try again later.";
            } else {
                friendly = "AI generation failed. Please try again.";
            }
            throw new Exception(friendly);
        }

        // Parse: candidates[0].content.parts[0].text
        JSONObject json = new JSONObject(response.body());
        return json.getJSONArray("candidates")
                   .getJSONObject(0)
                   .getJSONObject("content")
                   .getJSONArray("parts")
                   .getJSONObject(0)
                   .getString("text")
                   .trim();
    }
}
