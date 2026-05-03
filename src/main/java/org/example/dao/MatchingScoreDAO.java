package org.example.dao;

import org.example.config.DatabaseConfig;
import org.example.model.MatchingScore;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class MatchingScoreDAO {

    public MatchingScore findByCandidature(int candidatureId) throws SQLException {
        String sql = "SELECT * FROM matching_scores WHERE candidature_id = ? ORDER BY calculated_at DESC LIMIT 1";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, candidatureId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public void save(MatchingScore ms) throws SQLException {
        // Delete existing first
        try (PreparedStatement del = DatabaseConfig.getConnection().prepareStatement(
                "DELETE FROM matching_scores WHERE candidature_id = ?")) {
            del.setInt(1, ms.getCandidatureId());
            del.executeUpdate();
        }
        String sql = "INSERT INTO matching_scores (candidature_id, score, skills_score, description_score, keywords_score, matched_skills, missing_skills, recommendations, match_level, calculated_at) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, ms.getCandidatureId());
            ps.setDouble(2, ms.getScore());
            ps.setDouble(3, ms.getSkillsScore());
            ps.setDouble(4, ms.getDescriptionScore());
            ps.setDouble(5, ms.getKeywordsScore());
            ps.setString(6, listToJson(ms.getMatchedSkills()));
            ps.setString(7, listToJson(ms.getMissingSkills()));
            ps.setString(8, ms.getRecommendations());
            ps.setString(9, ms.getMatchLevel());
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) ms.setId(keys.getInt(1));
        }
    }

    public Map<String, Object> getStatistics(int offerId) throws SQLException {
        Map<String, Object> stats = new LinkedHashMap<>();
        String sql = "SELECT AVG(ms.score) as avg_score, " +
                     "SUM(CASE WHEN ms.match_level='excellent' THEN 1 ELSE 0 END) as excellent, " +
                     "SUM(CASE WHEN ms.match_level='good' THEN 1 ELSE 0 END) as good, " +
                     "SUM(CASE WHEN ms.match_level='fair' THEN 1 ELSE 0 END) as fair, " +
                     "SUM(CASE WHEN ms.match_level='low' THEN 1 ELSE 0 END) as low " +
                     "FROM matching_scores ms " +
                     "JOIN candidatures c ON ms.candidature_id = c.id " +
                     "WHERE c.offer_id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, offerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                stats.put("avgScore", rs.getDouble("avg_score"));
                stats.put("excellent", rs.getInt("excellent"));
                stats.put("good", rs.getInt("good"));
                stats.put("fair", rs.getInt("fair"));
                stats.put("low", rs.getInt("low"));
            }
        }
        return stats;
    }

    private MatchingScore mapRow(ResultSet rs) throws SQLException {
        MatchingScore ms = new MatchingScore();
        ms.setId(rs.getInt("id"));
        ms.setCandidatureId(rs.getInt("candidature_id"));
        ms.setScore(rs.getDouble("score"));
        ms.setSkillsScore(rs.getDouble("skills_score"));
        ms.setDescriptionScore(rs.getDouble("description_score"));
        ms.setKeywordsScore(rs.getDouble("keywords_score"));
        ms.setMatchedSkills(jsonToList(rs.getString("matched_skills")));
        ms.setMissingSkills(jsonToList(rs.getString("missing_skills")));
        ms.setRecommendations(rs.getString("recommendations"));
        ms.setMatchLevel(rs.getString("match_level"));
        Timestamp ts = rs.getTimestamp("calculated_at");
        if (ts != null) ms.setCalculatedAt(ts.toLocalDateTime());
        return ms;
    }

    private String listToJson(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(list.get(i).replace("\"", "\\\"")).append("\"");
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private List<String> jsonToList(String json) {
        List<String> result = new ArrayList<>();
        if (json == null || json.isBlank() || json.equals("[]")) return result;
        String inner = json.trim().replaceAll("^\\[|\\]$", "");
        for (String part : inner.split(",")) {
            String clean = part.trim().replaceAll("^\"|\"$", "").trim();
            if (!clean.isEmpty()) result.add(clean);
        }
        return result;
    }
}
