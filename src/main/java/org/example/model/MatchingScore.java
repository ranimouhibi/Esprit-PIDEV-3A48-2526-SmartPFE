package org.example.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MatchingScore {
    private int id;
    private int candidatureId;
    private double score;
    private double skillsScore;
    private double descriptionScore;
    private double keywordsScore;
    private List<String> matchedSkills = new ArrayList<>();
    private List<String> missingSkills = new ArrayList<>();
    private String recommendations;
    private String matchLevel; // excellent, good, fair, low
    private LocalDateTime calculatedAt;

    public MatchingScore() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCandidatureId() { return candidatureId; }
    public void setCandidatureId(int candidatureId) { this.candidatureId = candidatureId; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public double getSkillsScore() { return skillsScore; }
    public void setSkillsScore(double skillsScore) { this.skillsScore = skillsScore; }

    public double getDescriptionScore() { return descriptionScore; }
    public void setDescriptionScore(double descriptionScore) { this.descriptionScore = descriptionScore; }

    public double getKeywordsScore() { return keywordsScore; }
    public void setKeywordsScore(double keywordsScore) { this.keywordsScore = keywordsScore; }

    public List<String> getMatchedSkills() { return matchedSkills; }
    public void setMatchedSkills(List<String> matchedSkills) { this.matchedSkills = matchedSkills; }

    public List<String> getMissingSkills() { return missingSkills; }
    public void setMissingSkills(List<String> missingSkills) { this.missingSkills = missingSkills; }

    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }

    public String getMatchLevel() { return matchLevel; }
    public void setMatchLevel(String matchLevel) { this.matchLevel = matchLevel; }

    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }

    public String getMatchLevelColor() {
        if (matchLevel == null) return "#6c757d";
        return switch (matchLevel) {
            case "excellent" -> "#28a745";
            case "good"      -> "#ffc107";
            case "fair"      -> "#fd7e14";
            default          -> "#dc3545";
        };
    }
}
