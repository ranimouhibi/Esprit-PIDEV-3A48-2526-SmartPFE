package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HuggingFace Inference API client.
 * Uses sentence-transformers/all-MiniLM-L6-v2 for semantic similarity.
 * Falls back to offline Jaccard if API unavailable.
 */
public class HuggingFaceClient {

    private static final String API_URL =
        "https://api-inference.huggingface.co/models/sentence-transformers/all-MiniLM-L6-v2";
    private static final String SKILL_EXTRACT_URL =
        "https://api-inference.huggingface.co/models/Jean-Baptiste/roberta-large-ner-english";

    // Set your HuggingFace token here or via env variable HF_TOKEN
    private static final String HF_TOKEN = System.getenv("HF_TOKEN") != null
        ? System.getenv("HF_TOKEN") : "";

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Compute semantic similarity between two texts (0.0 - 1.0).
     * Falls back to Jaccard if API unavailable or token missing.
     */
    public double semanticSimilarity(String text1, String text2) {
        if (HF_TOKEN.isBlank() || text1.isBlank() || text2.isBlank()) {
            return jaccardFallback(text1, text2);
        }
        try {
            String payload = mapper.writeValueAsString(Map.of(
                "inputs", Map.of(
                    "source_sentence", truncate(text1, 512),
                    "sentences", List.of(truncate(text2, 512))
                )
            ));
            String response = post(API_URL, payload);
            JsonNode node = mapper.readTree(response);
            if (node.isArray() && node.size() > 0) {
                double score = node.get(0).asDouble();
                // Normalize from [-1,1] to [0,1]
                return Math.max(0, Math.min(1, (score + 1) / 2.0));
            }
        } catch (Exception e) {
            // API unavailable — use fallback silently
        }
        return jaccardFallback(text1, text2);
    }

    /**
     * Extract skills/entities from text using NER model.
     * Falls back to keyword extraction if unavailable.
     */
    public List<String> extractSkills(String text) {
        if (HF_TOKEN.isBlank() || text.isBlank()) {
            return keywordFallback(text);
        }
        try {
            String payload = mapper.writeValueAsString(Map.of("inputs", truncate(text, 512)));
            String response = post(SKILL_EXTRACT_URL, payload);
            JsonNode node = mapper.readTree(response);
            List<String> skills = new ArrayList<>();
            if (node.isArray()) {
                for (JsonNode entity : node) {
                    String word = entity.path("word").asText("");
                    String label = entity.path("entity_group").asText(entity.path("entity").asText(""));
                    if (!word.isBlank() && !word.startsWith("##")) {
                        skills.add(word.toLowerCase().trim());
                    }
                }
            }
            return skills.stream().distinct().collect(Collectors.toList());
        } catch (Exception e) {
            return keywordFallback(text);
        }
    }

    private String post(String urlStr, String payload) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + HF_TOKEN);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(15000);
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (is == null) throw new IOException("No response from HuggingFace API (code " + code + ")");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return br.lines().collect(Collectors.joining());
        }
    }

    // ── Fallbacks ─────────────────────────────────────────────────────────────

    private double jaccardFallback(String a, String b) {
        Set<String> setA = tokenize(a);
        Set<String> setB = tokenize(b);
        if (setA.isEmpty() && setB.isEmpty()) return 0.5;
        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);
        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    private List<String> keywordFallback(String text) {
        List<String> keywords = Arrays.asList(
            "java", "python", "php", "javascript", "typescript", "react", "angular", "vue",
            "spring", "symfony", "laravel", "django", "nodejs", "mysql", "postgresql",
            "mongodb", "docker", "kubernetes", "git", "linux", "html", "css", "rest",
            "api", "microservices", "agile", "scrum", "machine learning", "ai", "sql"
        );
        String lower = text.toLowerCase();
        return keywords.stream().filter(lower::contains).collect(Collectors.toList());
    }

    private Set<String> tokenize(String text) {
        Set<String> words = new HashSet<>();
        for (String w : text.toLowerCase().split("[\\s,;.!?()\\[\\]{}\"'\\-]+")) {
            if (w.length() > 2) words.add(w);
        }
        return words;
    }

    private String truncate(String text, int maxChars) {
        return text.length() > maxChars ? text.substring(0, maxChars) : text;
    }
}
