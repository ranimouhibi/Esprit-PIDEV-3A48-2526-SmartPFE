package org.example.dao;

import org.example.config.DatabaseConfig;
import org.example.model.Meeting;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class MeetingDAO {

    public List<Meeting> findAll(String search, String status, String type, String sortBy, String sortDir, int page, int pageSize) throws SQLException {
        List<Meeting> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT m.*, p.title AS project_title FROM meetings m " +
            "LEFT JOIN projects p ON m.project_id = p.id WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, search, status, type);
        String col = "scheduledDate".equals(sortBy) ? "m.scheduled_date" : "m.created_at";
        String dir = "ASC".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";
        sql.append(" ORDER BY ").append(col).append(" ").append(dir);
        sql.append(" LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add((page - 1) * pageSize);
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public int count(String search, String status, String type) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM meetings m LEFT JOIN projects p ON m.project_id = p.id WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, search, status, type);
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private void appendFilters(StringBuilder sql, List<Object> params, String search, String status, String type) {
        if (search != null && !search.isBlank()) {
            sql.append(" AND (p.title LIKE ? OR m.location LIKE ? OR m.agenda LIKE ?)");
            String like = "%" + search.trim() + "%";
            params.add(like); params.add(like); params.add(like);
        }
        if (status != null && !status.isBlank() && !"ALL".equals(status)) {
            sql.append(" AND m.status = ?");
            params.add(status);
        }
        if (type != null && !type.isBlank() && !"ALL".equals(type)) {
            sql.append(" AND m.meeting_type = ?");
            params.add(type);
        }
    }

    public Meeting findById(int id) throws SQLException {
        String sql = "SELECT m.*, p.title AS project_title FROM meetings m LEFT JOIN projects p ON m.project_id = p.id WHERE m.id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public void save(Meeting m) throws SQLException {
        String sql = "INSERT INTO meetings (meeting_type, scheduled_date, duration, location, agenda, status, reschedule_count, created_at, project_id, meeting_link, raw_content, ai_summary) VALUES (?,?,?,?,?,?,0,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getMeetingType());
            ps.setTimestamp(2, Timestamp.valueOf(m.getScheduledDate()));
            ps.setInt(3, m.getDuration());
            ps.setString(4, m.getLocation());
            ps.setString(5, m.getAgenda());
            ps.setString(6, m.getStatus());
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(8, m.getProjectId());
            ps.setString(9, m.getMeetingLink());
            ps.setString(10, m.getRawContent());
            ps.setString(11, m.getAiSummary());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) m.setId(keys.getInt(1));
        }
    }

    public void update(Meeting m, boolean dateChanged) throws SQLException {
        String sql = "UPDATE meetings SET meeting_type=?, scheduled_date=?, duration=?, location=?, agenda=?, status=?, project_id=?, meeting_link=?, raw_content=?, ai_summary=?" +
                     (dateChanged ? ", reschedule_count = reschedule_count + 1" : "") + " WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, m.getMeetingType());
            ps.setTimestamp(2, Timestamp.valueOf(m.getScheduledDate()));
            ps.setInt(3, m.getDuration());
            ps.setString(4, m.getLocation());
            ps.setString(5, m.getAgenda());
            ps.setString(6, m.getStatus());
            ps.setInt(7, m.getProjectId());
            ps.setString(8, m.getMeetingLink());
            ps.setString(9, m.getRawContent());
            ps.setString(10, m.getAiSummary());
            ps.setInt(11, m.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement("DELETE FROM meetings WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public int countTotal() throws SQLException {
        return countQuery("SELECT COUNT(*) FROM meetings");
    }

    public Map<String, Integer> countByStatus() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT status, COUNT(*) FROM meetings GROUP BY status";
        try (Statement st = DatabaseConfig.getConnection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) map.put(rs.getString(1), rs.getInt(2));
        }
        return map;
    }

    public Map<String, Integer> countByType() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT meeting_type, COUNT(*) FROM meetings GROUP BY meeting_type";
        try (Statement st = DatabaseConfig.getConnection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) map.put(rs.getString(1), rs.getInt(2));
        }
        return map;
    }

    public List<String[]> countByMonth() throws SQLException {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT DATE_FORMAT(scheduled_date,'%Y-%m') AS month, COUNT(*) FROM meetings WHERE scheduled_date >= DATE_SUB(NOW(), INTERVAL 6 MONTH) GROUP BY month ORDER BY month";
        try (Statement st = DatabaseConfig.getConnection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(new String[]{rs.getString(1), rs.getString(2)});
        }
        return list;
    }

    public double avgDuration() throws SQLException {
        String sql = "SELECT AVG(duration) FROM meetings WHERE duration > 0";
        try (Statement st = DatabaseConfig.getConnection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        }
        return 0;
    }

    public int countUpcoming() throws SQLException {
        return countQuery("SELECT COUNT(*) FROM meetings WHERE scheduled_date > NOW() AND status != 'CANCELLED'");
    }

    /** Meetings visibles par un utilisateur (participant OU superviseur/owner du projet). */
    public List<Meeting> findByUser(int userId) throws SQLException {
        List<Meeting> list = new ArrayList<>();
        String sql = "SELECT DISTINCT m.*, p.title AS project_title FROM meetings m "
                + "LEFT JOIN projects p ON m.project_id = p.id "
                + "LEFT JOIN meeting_participants mp ON m.id = mp.meeting_id "
                + "WHERE mp.user_id = ? OR p.supervisor_id = ? OR p.owner_id = ? "
                + "ORDER BY m.scheduled_date ASC";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /** Met à jour uniquement le champ meeting_link. */
    public void updateMeetingLink(int meetingId, String link) throws SQLException {
        String sql = "UPDATE meetings SET meeting_link = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, link);
            ps.setInt(2, meetingId);
            ps.executeUpdate();
        }
    }

    /** Met à jour uniquement le champ ai_summary. */
    public void updateAiSummary(int meetingId, String summary) throws SQLException {
        String sql = "UPDATE meetings SET ai_summary = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, summary);
            ps.setInt(2, meetingId);
            ps.executeUpdate();
        }
    }

    private int countQuery(String sql) throws SQLException {
        try (Statement st = DatabaseConfig.getConnection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private Meeting mapRow(ResultSet rs) throws SQLException {
        Meeting m = new Meeting();
        m.setId(rs.getInt("id"));
        m.setProjectId(rs.getInt("project_id"));
        m.setMeetingType(rs.getString("meeting_type"));
        m.setLocation(rs.getString("location"));
        m.setMeetingLink(rs.getString("meeting_link"));
        m.setStatus(rs.getString("status"));
        m.setAgenda(rs.getString("agenda"));
        m.setDuration(rs.getInt("duration"));
        m.setRescheduleCount(rs.getInt("reschedule_count"));
        m.setRawContent(rs.getString("raw_content"));
        m.setAiSummary(rs.getString("ai_summary"));
        Timestamp sd = rs.getTimestamp("scheduled_date");
        if (sd != null) m.setScheduledDate(sd.toLocalDateTime());
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) m.setCreatedAt(ca.toLocalDateTime());
        try { m.setProjectTitle(rs.getString("project_title")); } catch (SQLException ignored) {}
        return m;
    }
}
