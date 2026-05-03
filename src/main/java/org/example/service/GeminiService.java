package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Google Gemini API service for AI suggestions, CV analysis, and recommendations.
 * Set GEMINI_API_KEY environment variable or replace the constant below.
 * Falls back to rule-based analysis if API key is missing.
 */
public class GeminiService {

    private static final String GEMINI_API_KEY = System.getenv("GEMINI_API_KEY") != null
        ? System.getenv("GEMINI_API_KEY") : "AIzaSyDmPFtJy-HmlqRM9GOFQyQwvQvwPQ3Uk-E";
    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=";

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Analyze a CV text and extract key skills, experience, and suggestions.
     */
    public String analyzeCV(String cvText) {
        if (GEMINI_API_KEY.isBlank() || cvText.isBlank()) {
            return fallbackCVAnalysis(cvText);
        }
        String prompt = "Analyze this CV and extract: 1) Key technical skills, 2) Years of experience, " +
            "3) Education level, 4) Top 3 strengths, 5) Areas for improvement. " +
            "Be concise. CV:\n\n" + truncate(cvText, 3000);
        return callGemini(prompt, "CV analysis unavailable (API key not configured).");
    }

    /**
     * Generate AI suggestions for improving a candidature.
     */
    public String suggestImprovements(String motivationLetter, String requiredSkills, String studentSkills) {
        if (GEMINI_API_KEY.isBlank()) {
            return fallbackSuggestions(studentSkills, requiredSkills);
        }
        String prompt = String.format(
            "A student is applying for a position requiring: %s\n" +
            "Student skills: %s\n" +
            "Motivation letter: %s\n\n" +
            "Provide 3 specific suggestions to improve this application. Be concise and actionable.",
            requiredSkills, studentSkills, truncate(motivationLetter, 500)
        );
        return callGemini(prompt, fallbackSuggestions(studentSkills, requiredSkills));
    }

    /**
     * Generate a match explanation for a candidature.
     */
    public String explainMatch(String studentProfile, String offerDescription, double score) {
        if (GEMINI_API_KEY.isBlank()) {
            return fallbackMatchExplanation(score);
        }
        String prompt = String.format(
            "Explain in 2-3 sentences why this candidate (score: %.1f%%) matches or doesn't match this offer.\n" +
            "Candidate profile: %s\nOffer: %s",
            score, truncate(studentProfile, 400), truncate(offerDescription, 400)
        );
        return callGemini(prompt, fallbackMatchExplanation(score));
    }

    private String callGemini(String prompt, String fallback) {
        try {
            String payload = mapper.writeValueAsString(
                java.util.Map.of("contents", java.util.List.of(
                    java.util.Map.of("parts", java.util.List.of(
                        java.util.Map.of("text", prompt)
                    ))
                ))
            );
            URL url = new URL(GEMINI_URL + GEMINI_API_KEY);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(20000);
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (is == null) return fallback;
            String response;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                response = br.lines().collect(Collectors.joining());
            }
            JsonNode root = mapper.readTree(response);
            return root.path("candidates").get(0)
                       .path("content").path("parts").get(0)
                       .path("text").asText(fallback);
        } catch (Exception e) {
            return fallback;
        }
    }

    // ── Fallbacks ─────────────────────────────────────────────────────────────

    private String fallbackCVAnalysis(String cvText) {
        if (cvText.isBlank()) return "No CV text provided for analysis.";
        long wordCount = cvText.split("\\s+").length;
        return String.format(
            "CV Analysis (offline mode):\n" +
            "• Document length: ~%d words\n" +
            "• Configure GEMINI_API_KEY for detailed AI analysis\n" +
            "• Skills detected: see matching score panel",
            wordCount
        );
    }

    private String fallbackSuggestions(String studentSkills, String requiredSkills) {
        return "Suggestions (offline mode):\n" +
            "1. Highlight your most relevant skills matching: " + truncate(requiredSkills, 100) + "\n" +
            "2. Quantify your achievements with specific metrics\n" +
            "3. Tailor your motivation letter to the specific offer requirements\n" +
            "Configure GEMINI_API_KEY for personalized AI suggestions.";
    }

    private String fallbackMatchExplanation(double score) {
        if (score >= 80) return "Strong match — candidate profile aligns well with offer requirements.";
        if (score >= 60) return "Good match — candidate meets most requirements with minor gaps.";
        if (score >= 40) return "Partial match — candidate has relevant background but missing key skills.";
        return "Weak match — significant skill gaps identified. Consider training or alternative candidates.";
    }

    private String truncate(String text, int max) {
        return text != null && text.length() > max ? text.substring(0, max) + "..." : (text != null ? text : "");
    }
}
