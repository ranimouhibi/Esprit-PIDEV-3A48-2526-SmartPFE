package org.example.util;

import org.example.model.User;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.*;

/**
 * AI-powered task assignment using Groq API (free tier).
 * Extracts technical skills from task title+description,
 * then scores students based on their skills profile.
 */
public class TaskAssignmentAI {

    // Groq free API — replace with your key from https://console.groq.com
    private static final String GROQ_API_KEY = System.getenv().getOrDefault("GROQ_API_KEY", "");
    private static final String GROQ_URL     = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL        = "llama-3.3-70b-versatile";

    public record AssignmentResult(User recommended, String reason, int score) {}

    /**
     * Call Groq API to extract technical skills from task title + description.
     * Returns a list of detected skill keywords (e.g. ["java", "react", "mysql"]).
     * Returns empty list if API fails or no skills detected.
     */
    public static List<String> extractSkillsFromAI(String title, String description) {
        String prompt = "Extract only the technical skills and technologies mentioned in the following task. "
            + "Return ONLY a comma-separated list of lowercase skill names (e.g: java, react, mysql). "
            + "If no technical skills are found, return exactly: NONE\n\n"
            + "Task title: " + (title != null ? title : "") + "\n"
            + "Task description: " + (description != null ? description : "");

        try {
            String body = "{"
                + "\"model\":\"" + MODEL + "\","
                + "\"messages\":[{\"role\":\"user\",\"content\":" + jsonString(prompt) + "}],"
                + "\"max_tokens\":100,"
                + "\"temperature\":0"
                + "}";

            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + GROQ_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(15))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("[AI] Groq API error: " + response.statusCode() + " — " + response.body());
                return List.of();
            }

            String content = extractContent(response.body());
            System.out.println("[AI] Groq response: " + content);

            if (content == null || content.isBlank() || content.trim().equalsIgnoreCase("NONE")) {
                return List.of();
            }

            List<String> skills = new ArrayList<>();
            for (String s : content.split(",")) {
                String skill = s.trim().toLowerCase();
                if (!skill.isEmpty() && !skill.equals("none")) skills.add(skill);
            }
            return skills;

        } catch (Exception e) {
            System.err.println("[AI] API call failed: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Score a student based on how many detected skills match their skills profile.
     * +10 points per matching skill.
     */
    public static int scoreStudent(User student, List<String> detectedSkills) {
        if (detectedSkills.isEmpty()) return 0;
        String studentSkills = (student.getSkills() != null ? student.getSkills() : "").toLowerCase();
        int score = 0;
        for (String skill : detectedSkills) {
            if (studentSkills.contains(skill)) score += 10;
        }
        return score;
    }

    /**
     * Recommend the best student from a list of 2.
     * Returns null if no skills detected (manual assignment needed).
     */
    public static AssignmentResult recommend(List<User> students, String taskTitle, String taskDesc) {
        List<String> detectedSkills = extractSkillsFromAI(taskTitle, taskDesc);
        System.out.println("[AI] Detected skills: " + detectedSkills);

        if (detectedSkills.isEmpty()) return null;

        User best = null;
        int bestScore = -1;
        List<String> matchedSkills = new ArrayList<>();

        for (User student : students) {
            int score = scoreStudent(student, detectedSkills);
            System.out.println("[AI] Score for " + student.getName() + ": " + score);
            if (score > bestScore) {
                bestScore = score;
                best = student;
                matchedSkills.clear();
                String studentSkills = (student.getSkills() != null ? student.getSkills() : "").toLowerCase();
                for (String skill : detectedSkills) {
                    if (studentSkills.contains(skill)) matchedSkills.add(skill);
                }
            }
        }

        if (best == null) return null;

        String reason = matchedSkills.isEmpty()
            ? "Best available match (score: " + bestScore + ")"
            : "Recommended: match with " + String.join(", ", matchedSkills);

        return new AssignmentResult(best, reason, bestScore);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Extract the message content from Groq JSON response */
    private static String extractContent(String json) {
        // Parse: "content":"..."
        Pattern p = Pattern.compile("\"content\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1)
                .replace("\\n", " ")
                .replace("\\\"", "\"")
                .trim();
        }
        return null;
    }

    /** Escape a string for JSON */
    private static String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r") + "\"";
    }
}
