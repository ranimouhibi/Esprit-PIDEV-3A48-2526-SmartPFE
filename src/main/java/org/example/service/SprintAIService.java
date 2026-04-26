package org.example.service;

import org.example.config.DatabaseConfig;
import org.example.model.User;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Sprint Assignment Service.
 * Recommends top 3 students for a sprint based on skill matching.
 * Uses HuggingFace semantic similarity when available, falls back to keyword matching.
 */
public class SprintAIService {

    private final HuggingFaceClient hfClient = new HuggingFaceClient();

    public record StudentRecommendation(User student, double score, List<String> matchedSkills, String reason) {}

    /**
     * Recommend top 3 students for a sprint based on sprint goal + project requirements.
     */
    public List<StudentRecommendation> recommendStudentsForSprint(int sprintId, int projectId) throws SQLException {
        // Load sprint info
        String sprintGoal = "", projectDesc = "", requiredSkills = "";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(
            "SELECT s.goal, p.description, p.title FROM sprints s JOIN projects p ON s.project_id = p.id WHERE s.id = ?")) {
            ps.setInt(1, sprintId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                sprintGoal = nullSafe(rs.getString("goal"));
                projectDesc = nullSafe(rs.getString("description"));
            }
        }

        // Load project required skills from related offers
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(
            "SELECT required_skills FROM project_offers WHERE id IN " +
            "(SELECT offer_id FROM candidatures WHERE offer_id IN " +
            "(SELECT id FROM project_offers WHERE establishment_id IN " +
            "(SELECT establishment_id FROM users WHERE id = (SELECT owner_id FROM projects WHERE id = ?))))")) {
            ps.setInt(1, projectId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) requiredSkills = nullSafe(rs.getString("required_skills"));
        } catch (Exception ignored) {}

        // Load all students with their skills
        List<User> students = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(
            "SELECT id, name, email, bio, skills, competences FROM users WHERE role = 'student' AND is_active = 1")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setName(rs.getString("name"));
                u.setEmail(rs.getString("email"));
                // Store skills in a temp field via toString override
                String skills = nullSafe(rs.getString("skills")) + " " +
                                nullSafe(rs.getString("competences")) + " " +
                                nullSafe(rs.getString("bio"));
                u.setPhone(skills); // reuse phone field to carry skills text
                students.add(u);
            }
        }

        if (students.isEmpty()) return List.of();

        String sprintContext = sprintGoal + " " + projectDesc + " " + requiredSkills;
        List<StudentRecommendation> recommendations = new ArrayList<>();

        for (User student : students) {
            String studentProfile = student.getPhone(); // skills text stored here
            double score = computeMatchScore(studentProfile, sprintContext);
            List<String> matched = extractMatchedKeywords(studentProfile, sprintContext);
            String reason = buildReason(score, matched, student.getName());
            recommendations.add(new StudentRecommendation(student, score, matched, reason));
        }

        // Sort by score desc, return top 3
        return recommendations.stream()
            .sorted(Comparator.comparingDouble(StudentRecommendation::score).reversed())
            .limit(3)
            .collect(Collectors.toList());
    }

    private double computeMatchScore(String studentProfile, String sprintContext) {
        if (studentProfile.isBlank() || sprintContext.isBlank()) return Math.random() * 30 + 20;
        // Try HuggingFace semantic similarity
        double semantic = hfClient.semanticSimilarity(
            truncate(studentProfile, 512), truncate(sprintContext, 512));
        // Keyword overlap bonus
        double keyword = keywordOverlap(studentProfile.toLowerCase(), sprintContext.toLowerCase());
        return Math.min(100, (semantic * 70) + (keyword * 30));
    }

    private double keywordOverlap(String a, String b) {
        Set<String> wordsA = tokenize(a);
        Set<String> wordsB = tokenize(b);
        if (wordsB.isEmpty()) return 0;
        Set<String> inter = new HashSet<>(wordsA);
        inter.retainAll(wordsB);
        return (double) inter.size() / wordsB.size();
    }

    private List<String> extractMatchedKeywords(String profile, String context) {
        List<String> techKeywords = Arrays.asList(
            "java", "python", "php", "javascript", "react", "angular", "spring", "mysql",
            "docker", "git", "agile", "scrum", "rest", "api", "html", "css", "nodejs",
            "mongodb", "postgresql", "linux", "devops", "machine learning", "ai"
        );
        String p = profile.toLowerCase();
        String c = context.toLowerCase();
        return techKeywords.stream()
            .filter(k -> p.contains(k) && c.contains(k))
            .map(k -> k.substring(0, 1).toUpperCase() + k.substring(1))
            .collect(Collectors.toList());
    }

    private String buildReason(double score, List<String> matched, String name) {
        if (score >= 70) return name + " is an excellent fit with strong skill alignment.";
        if (score >= 50) return name + " has good relevant skills for this sprint.";
        if (score >= 30) return name + " has some matching skills but may need support.";
        return name + " has limited matching skills for this sprint.";
    }

    private Set<String> tokenize(String text) {
        Set<String> words = new HashSet<>();
        for (String w : text.split("[\\s,;.!?()\\[\\]{}\"'\\-]+"))
            if (w.length() > 2) words.add(w);
        return words;
    }

    private String truncate(String s, int max) { return s.length() > max ? s.substring(0, max) : s; }
    private String nullSafe(String s) { return s != null ? s : ""; }
}
