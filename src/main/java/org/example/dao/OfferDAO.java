package org.example.dao;

import org.example.config.DatabaseConfig;
import org.example.model.Offer;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
public class OfferDAO {

    private static final String SELECT_BASE =
        "SELECT o.*, u.name as establishment_name " +
        "FROM project_offers o " +
        "LEFT JOIN users u ON o.establishment_id = u.id ";

    public List<Offer> findAll() throws SQLException {
        List<Offer> list = new ArrayList<>();
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(SELECT_BASE + "ORDER BY o.created_at DESC")) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Offer> findByEstablishment(int establishmentId) throws SQLException {
        List<Offer> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE o.establishment_id = ? ORDER BY o.created_at DESC";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, establishmentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // Student sees all open offers (or filter by establishment if needed)
    public List<Offer> findAllOpen() throws SQLException {
        List<Offer> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE (o.status = 'open' OR o.status IS NULL) ORDER BY o.created_at DESC";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public void save(Offer o) throws SQLException {
        String sql = "INSERT INTO project_offers (establishment_id, title, description, objectives, required_skills, max_candidates, deadline, status, created_at) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, o.getEstablishmentId());
            ps.setString(2, o.getTitle());
            ps.setString(3, o.getDescription());
            ps.setString(4, o.getObjectives());
            ps.setString(5, o.getRequiredSkills());
            ps.setInt(6, o.getMaxCandidates() > 0 ? o.getMaxCandidates() : 10);
            ps.setDate(7, o.getDeadline() != null ? java.sql.Date.valueOf(o.getDeadline()) : null);
            ps.setString(8, "open");
            ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) o.setId(keys.getInt(1));
        }
    }

    public void update(Offer o) throws SQLException {
        String sql = "UPDATE project_offers SET title=?, description=?, objectives=?, required_skills=?, max_candidates=?, deadline=?, status=? WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, o.getTitle());
            ps.setString(2, o.getDescription());
            ps.setString(3, o.getObjectives());
            ps.setString(4, o.getRequiredSkills());
            ps.setInt(5, o.getMaxCandidates());
            ps.setDate(6, o.getDeadline() != null ? java.sql.Date.valueOf(o.getDeadline()) : null);
            ps.setString(7, o.getStatus() != null ? o.getStatus() : "open");
            ps.setInt(8, o.getId());
            ps.executeUpdate();
        }
    }

    public Offer findById(int id) throws SQLException {
        String sql = SELECT_BASE + "WHERE o.id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public List<Offer> findOpen() throws SQLException {
        return findAllOpen();
    }

    public java.util.Map<String, Integer> getStats() throws SQLException {
        java.util.Map<String, Integer> stats = new java.util.LinkedHashMap<>();
        ResultSet rs = DatabaseConfig.getConnection().createStatement().executeQuery(
            "SELECT COUNT(*) as total, " +
            "SUM(CASE WHEN status='open' THEN 1 ELSE 0 END) as open_count, " +
            "SUM(CASE WHEN status='closed' THEN 1 ELSE 0 END) as closed_count, " +
            "SUM(CASE WHEN status='draft' THEN 1 ELSE 0 END) as draft_count, " +
            "(SELECT COUNT(*) FROM candidatures) as total_candidatures " +
            "FROM project_offers");
        if (rs.next()) {
            stats.put("total", rs.getInt("total"));
            stats.put("open", rs.getInt("open_count"));
            stats.put("closed", rs.getInt("closed_count"));
            stats.put("draft", rs.getInt("draft_count"));
            stats.put("totalCandidatures", rs.getInt("total_candidatures"));
        }
        return stats;
    }

    public int getCandidatureCount(int offerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM candidatures WHERE offer_id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, offerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM project_offers WHERE id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Offer mapRow(ResultSet rs) throws SQLException {
        Offer o = new Offer();
        o.setId(rs.getInt("id"));
        o.setEstablishmentId(rs.getInt("establishment_id"));
        o.setEstablishmentName(rs.getString("establishment_name"));
        o.setTitle(rs.getString("title"));
        o.setDescription(rs.getString("description"));
        o.setObjectives(rs.getString("objectives"));
        o.setRequiredSkills(rs.getString("required_skills"));
        o.setMaxCandidates(rs.getInt("max_candidates"));
        o.setStatus(rs.getString("status"));
        java.sql.Date deadline = rs.getDate("deadline");
        if (deadline != null) o.setDeadline(deadline.toLocalDate());
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) o.setCreatedAt(ts.toLocalDateTime());
        return o;
    }
}
