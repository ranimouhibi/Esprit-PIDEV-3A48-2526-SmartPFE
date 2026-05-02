package org.example.service;

import org.example.config.DatabaseConfig;
import org.example.dao.MatchingScoreDAO;
import org.example.model.MatchingScore;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Matching Service — weighted score: Skills 60% + Description 25% + Keywords 15%.
 * Uses HuggingFace semantic similarity when API token is available,
 * falls back to offline Jaccard/keyword matching otherwise.
 */
public class AIMatchingService {

    private static final List<String> TECH_KEYWORDS = Arrays.asList(
        "php", "java", "python", "javascript", "typescript", "react", "angular", "vue",
        "spring", "symfony", "laravel", "django", "nodejs", "express", "mysql", "postgresql",
        "mongodb", "redis", "docker", "kubernetes", "git", "linux", "html", "css", "rest",
        "api", "microservices", "agile", "scrum", "maven", "gradle", "junit", "hibernate",
        "jpa", "jdbc", "javafx", "android", "ios", "swift", "kotlin", "c++", "c#", ".net",
        "aws", "azure", "gcp", "jenkins", "ci/cd", "devops", "machine learning", "ai",
        "tensorflow", "pytorch", "pandas", "numpy", "sql", "nosql", "graphql", "kafka",
        "flutter", "dart", "rust", "go", "scala", "spark", "hadoop", "elasticsearch",
        "rabbitmq", "nginx", "apache", "linux", "bash", "powershell", "uml", "merise"
    );

    private final MatchingScoreDAO matchingScoreDAO = new MatchingScoreDAO();
    private final HuggingFaceClient hfClient = new HuggingFaceClient();

    public MatchingScore calculateMatchingScore(int candidatureId) throws SQLException {
        String sql = "SELECT c.student_id, c.offer_id, c.motivation_letter, " +
                     "u.bio, u.skills, u.competences, u.experiences, " +
                     "o.description as offer_desc, o.required_skills, o.title as offer_title, o.objectives " +
                     "FROM candidatures c " +
                     "JOIN users u ON c.student_id = u.id " +
                     "JOIN project_offers o ON c.offer_id = o.id " +
                     "WHERE c.id = ?";

        String studentBio, studentSkillsRaw, offerDesc, requiredSkills, motivationLetter, objectives, experiences;

        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, candidatureId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new SQLException("Candidature not found: " + candidatureId);
            studentBio       = nullSafe(rs.getString("bio")) + " " + nullSafe(rs.getString("competences"));
            studentSkillsRaw = nullSafe(rs.getString("skills"));
            experiences      = nullSafe(rs.getString("experiences"));
            offerDesc        = nullSafe(rs.getString("offer_desc"));
            requiredSkills   = nullSafe(rs.getString("required_skills"));
            motivationLetter = nullSafe(rs.getString("motivation_letter"));
            objectives       = nullSafe(rs.getString("objectives"));
        }

        String studentFullText = studentBio + " " + studentSkillsRaw + " " + motivationLetter + " " + experiences;
        String offerFullText   = offerDesc + " " + requiredSkills + " " + objectives;

        // Parse skills lists
        List<String> studentSkills = parseSkills(studentSkillsRaw);
        studentSkills.addAll(hfClient.extractSkills(studentBio + " " + motivationLetter));
        studentSkills = studentSkills.stream().map(String::toLowerCase).distinct().collect(Collectors.toList());

        List<String> offerSkills = parseSkills(requiredSkills);
        offerSkills.addAll(extractKeywords(requiredSkills));
        offerSkills = offerSkills.stream().map(String::toLowerCase).distinct().collect(Collectors.toList());

        // 1. Skills Score — Jaccard on parsed skills (60%)
        double skillsScore = calculateJaccard(studentSkills, offerSkills) * 100;

        // 2. Description Score — HuggingFace semantic similarity (25%)
        double rawSim = hfClient.semanticSimilarity(
            truncate(studentFullText, 512),
            truncate(offerFullText, 512)
        );
        double descriptionScore = rawSim * 100;

        // 3. Keywords Score — tech keyword overlap (15%)
        double keywordsScore = calculateKeywordsScore(studentFullText, offerFullText);

        // Weighted final score
        double finalScore = (skillsScore * 0.60) + (descriptionScore * 0.25) + (keywordsScore * 0.15);
        finalScore = Math.min(100, Math.max(0, finalScore));

        // Matched / missing skills
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String skill : offerSkills) {
            boolean found = studentSkills.stream().anyMatch(s -> s.contains(skill) || skill.contains(s));
            if (found) matched.add(capitalize(skill));
            else missing.add(capitalize(skill));
        }

        // Match level
        String matchLevel = finalScore >= 80 ? "excellent"
                          : finalScore >= 60 ? "good"
                          : finalScore >= 40 ? "fair" : "low";

        String recommendations = buildRecommendations(matchLevel, missing, finalScore);

        MatchingScore ms = new MatchingScore();
        ms.setCandidatureId(candidatureId);
        ms.setScore(round(finalScore));
        ms.setSkillsScore(round(skillsScore));
        ms.setDescriptionScore(round(descriptionScore));
        ms.setKeywordsScore(round(keywordsScore));
        ms.setMatchedSkills(matched);
        ms.setMissingSkills(missing);
        ms.setRecommendations(recommendations);
        ms.setMatchLevel(matchLevel);

        matchingScoreDAO.save(ms);
        return ms;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private double calculateJaccard(List<String> a, List<String> b) {
        if (b.isEmpty()) return a.isEmpty() ? 0.5 : 0.2;
        Set<String> setA = new HashSet<>(a);
        Set<String> setB = new HashSet<>(b);
        Set<String> inter = new HashSet<>(setA); inter.retainAll(setB);
        Set<String> union = new HashSet<>(setA); union.addAll(setB);
        return union.isEmpty() ? 0 : (double) inter.size() / union.size();
    }

    private double calculateKeywordsScore(String studentText, String offerText) {
        String st = studentText.toLowerCase();
        String ot = offerText.toLowerCase();
        List<String> offerKw = TECH_KEYWORDS.stream().filter(ot::contains).collect(Collectors.toList());
        if (offerKw.isEmpty()) return 50.0;
        long matched = offerKw.stream().filter(st::contains).count();
        return (double) matched / offerKw.size() * 100;
    }

    private List<String> parseSkills(String raw) {
        List<String> skills = new ArrayList<>();
        if (raw == null || raw.isBlank()) return skills;
        if (raw.trim().startsWith("[")) {
            String inner = raw.trim().replaceAll("^\\[|\\]$", "");
            for (String part : inner.split(",")) {
                String clean = part.trim().replaceAll("^\"|\"$", "").trim();
                if (!clean.isEmpty()) skills.add(clean.toLowerCase());
            }
        } else {
            for (String part : raw.split("[,;\\n]")) {
                String clean = part.trim().toLowerCase();
                if (!clean.isEmpty() && clean.length() < 50) skills.add(clean);
            }
        }
        return skills;
    }

    private List<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) return new ArrayList<>();
        String lower = text.toLowerCase();
        return TECH_KEYWORDS.stream().filter(lower::contains).collect(Collectors.toList());
    }

    private String buildRecommendations(String level, List<String> missing, double score) {
        StringBuilder sb = new StringBuilder();
        switch (level) {
            case "excellent" -> sb.append("Excellent match! This candidate is highly qualified for the position.");
            case "good"      -> sb.append("Good match. The candidate meets most requirements.");
            case "fair"      -> sb.append("Fair match. The candidate meets some requirements but has gaps.");
            default          -> sb.append("Low match. The candidate may not meet the core requirements.");
        }
        if (!missing.isEmpty()) {
            sb.append("\n\nMissing skills: ")
              .append(String.join(", ", missing.subList(0, Math.min(5, missing.size()))));
            sb.append("\nConsider whether these skills can be acquired through training.");
        }
        sb.append(score >= 60
            ? "\n\nRecommendation: Proceed to interview stage."
            : "\n\nRecommendation: Review application carefully before proceeding.");
        return sb.toString();
    }

    private double round(double v) { return Math.round(v * 10.0) / 10.0; }
    private String nullSafe(String s) { return s != null ? s : ""; }
    private String truncate(String s, int max) { return s.length() > max ? s.substring(0, max) : s; }
    private String capitalize(String s) {
        return (s == null || s.isEmpty()) ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
