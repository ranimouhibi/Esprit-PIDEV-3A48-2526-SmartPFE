package org.example.dao;

import org.example.config.DatabaseConfig;
import org.example.model.Candidature;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CandidatureDAO {

    private static final String SELECT_BASE =
        "SELECT c.*, u.name as student_name, o.title as offer_title " +
        "FROM candidatures c " +
        "LEFT JOIN users u ON c.student_id = u.id " +
        "LEFT JOIN project_offers o ON c.offer_id = o.id ";

    public List<Candidature> findAll() throws SQLException {
        List<Candidature> list = new ArrayList<>();
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(SELECT_BASE + "ORDER BY c.created_at DESC")) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Candidature> findByOffer(int offerId) throws SQLException {
        List<Candidature> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE c.offer_id = ? ORDER BY c.created_at DESC";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, offerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Candidature> findByStudent(int studentId) throws SQLException {
        List<Candidature> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE c.student_id = ? ORDER BY c.created_at DESC";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public boolean existsByStudentAndOffer(int studentId, int offerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM candidatures WHERE student_id = ? AND offer_id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, offerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }

    public void save(Candidature c) throws SQLException {
        String sql = "INSERT INTO candidatures (offer_id, student_id, motivation_letter, cv_path, portfolio_url, status, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getOfferId());
            ps.setInt(2, c.getStudentId());
            ps.setString(3, c.getMotivationLetter());
            ps.setString(4, c.getCvPath());
            ps.setString(5, c.getPortfolioUrl());
            ps.setString(6, "pending");
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                c.setId(keys.getInt(1));
                // Auto-calculate AI matching score asynchronously
                final int candidatureId = c.getId();
                new Thread(() -> {
                    try {
                        new org.example.service.AIMatchingService().calculateMatchingScore(candidatureId);
                    } catch (Exception ignored) {}
                }, "ai-score-" + candidatureId).start();
            }
        }
    }

    public void update(Candidature c) throws SQLException {
        String sql = "UPDATE candidatures SET motivation_letter=?, cv_path=?, portfolio_url=?, updated_at=? WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, c.getMotivationLetter());
            ps.setString(2, c.getCvPath());
            ps.setString(3, c.getPortfolioUrl());
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(5, c.getId());
            ps.executeUpdate();
        }
    }

    public void updateStatus(int id, String status, String feedback) throws SQLException {
        String sql = "UPDATE candidatures SET status=?, feedback=?, updated_at=? WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, feedback);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(4, id);
            ps.executeUpdate();
        }
        // Send email notification asynchronously
        final String finalFeedback = feedback;
        new Thread(() -> {
            try {
                new org.example.service.EmailNotificationService().sendStatusChange(id, status, finalFeedback);
            } catch (Exception ignored) {}
        }, "email-status-" + id).start();
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM candidatures WHERE id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Candidature mapRow(ResultSet rs) throws SQLException {
        Candidature c = new Candidature();
        c.setId(rs.getInt("id"));
        c.setOfferId(rs.getInt("offer_id"));
        c.setOfferTitle(rs.getString("offer_title"));
        c.setStudentId(rs.getInt("student_id"));
        c.setStudentName(rs.getString("student_name"));
        c.setMotivationLetter(rs.getString("motivation_letter"));
        c.setCvPath(rs.getString("cv_path"));
        c.setPortfolioUrl(rs.getString("portfolio_url"));
        c.setStatus(rs.getString("status"));
        c.setFeedback(rs.getString("feedback"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) c.setCreatedAt(ts.toLocalDateTime());
        Timestamp ts2 = rs.getTimestamp("updated_at");
        if (ts2 != null) c.setUpdatedAt(ts2.toLocalDateTime());
        return c;
    }
}
