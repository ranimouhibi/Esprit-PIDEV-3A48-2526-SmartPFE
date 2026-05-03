package org.example.dao;

import org.example.config.DatabaseConfig;
import org.example.model.MeetingReport;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class MeetingReportDAO {

    public List<MeetingReport> findAll(String search, String status, String sortDir, int page, int pageSize, int userId, String role) throws SQLException {
        List<MeetingReport> list = new ArrayList<>();
        StringBuilder sql = buildBaseQuery();
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, search, status, userId, role);
        String dir = "ASC".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";
        sql.append(" ORDER BY r.created_at ").append(dir);
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

    public int count(String search, String status, int userId, String role) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM meeting_reports r LEFT JOIN users u ON r.created_by_id = u.id LEFT JOIN meetings m ON r.meeting_id = m.id LEFT JOIN projects p ON m.project_id = p.id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, search, status, userId, role);
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private StringBuilder buildBaseQuery() {
        return new StringBuilder(
            "SELECT r.*, u.name AS created_by_name, m.meeting_type, m.scheduled_date AS meeting_date, p.title AS project_title " +
            "FROM meeting_reports r " +
            "LEFT JOIN users u ON r.created_by_id = u.id " +
            "LEFT JOIN meetings m ON r.meeting_id = m.id " +
            "LEFT JOIN projects p ON m.project_id = p.id WHERE 1=1"
        );
    }

    private void appendFilters(StringBuilder sql, List<Object> params, String search, String status, int userId, String role) {
        if (search != null && !search.isBlank()) {
            sql.append(" AND (p.title LIKE ? OR m.meeting_type LIKE ? OR u.name LIKE ?)");
            String like = "%" + search.trim() + "%";
            params.add(like); params.add(like); params.add(like);
        }
        if (status != null && !status.isBlank() && !"ALL".equals(status)) {
            sql.append(" AND r.status = ?");
            params.add(status);
        }
        if (!"ADMIN".equalsIgnoreCase(role) && userId > 0) {
            sql.append(" AND (p.owner_id = ? OR p.supervisor_id = ? OR r.created_by_id = ?)");
            params.add(userId); params.add(userId); params.add(userId);
        }
    }

    public List<MeetingReport> findByMeeting(int meetingId) throws SQLException {
        List<MeetingReport> list = new ArrayList<>();
        StringBuilder sql = buildBaseQuery();
        sql.append(" AND r.meeting_id = ? ORDER BY r.created_at DESC");
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql.toString())) {
            ps.setInt(1, meetingId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public MeetingReport findById(int id) throws SQLException {
        StringBuilder sql = buildBaseQuery();
        sql.append(" AND r.id = ?");
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql.toString())) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public void save(MeetingReport r) throws SQLException {
        String sql = "INSERT INTO meeting_reports (discussion_points, decisions, action_items, next_steps, status, created_at, updated_at, meeting_id, created_by_id, raw_meeting_text) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getDiscussionPoints());
            ps.setString(2, r.getDecisions());
            ps.setString(3, r.getActionItems());
            ps.setString(4, r.getNextSteps());
            ps.setString(5, r.getStatus() != null ? r.getStatus() : "DRAFT");
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(8, r.getMeetingId());
            ps.setInt(9, r.getCreatedById());
            ps.setString(10, r.getRawMeetingText());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) r.setId(keys.getInt(1));
        }
    }

    public void update(MeetingReport r) throws SQLException {
        String sql = "UPDATE meeting_reports SET discussion_points=?, decisions=?, action_items=?, next_steps=?, status=?, updated_at=?, raw_meeting_text=? WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, r.getDiscussionPoints());
            ps.setString(2, r.getDecisions());
            ps.setString(3, r.getActionItems());
            ps.setString(4, r.getNextSteps());
            ps.setString(5, r.getStatus());
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(7, r.getRawMeetingText());
            ps.setInt(8, r.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement("DELETE FROM meeting_reports WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void deleteByMeeting(int meetingId) throws SQLException {
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement("DELETE FROM meeting_reports WHERE meeting_id=?")) {
            ps.setInt(1, meetingId);
            ps.executeUpdate();
        }
    }

    /** Returns int[4]: [total, draft, submitted, approved] */
    public int[] countByStatus() throws SQLException {
        int[] counts = new int[4];
        String sql = "SELECT status, COUNT(*) FROM meeting_reports GROUP BY status";
        try (Statement st = DatabaseConfig.getConnection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            int total = 0;
            while (rs.next()) {
                String s = rs.getString(1);
                int c = rs.getInt(2);
                total += c;
                if ("DRAFT".equalsIgnoreCase(s)) counts[1] = c;
                else if ("SUBMITTED".equalsIgnoreCase(s)) counts[2] = c;
                else if ("APPROVED".equalsIgnoreCase(s)) counts[3] = c;
            }
            counts[0] = total;
        }
        return counts;
    }

    private MeetingReport mapRow(ResultSet rs) throws SQLException {
        MeetingReport r = new MeetingReport();
        r.setId(rs.getInt("id"));
        r.setMeetingId(rs.getInt("meeting_id"));
        r.setCreatedById(rs.getInt("created_by_id"));
        r.setDiscussionPoints(rs.getString("discussion_points"));
        r.setDecisions(rs.getString("decisions"));
        r.setActionItems(rs.getString("action_items"));
        r.setNextSteps(rs.getString("next_steps"));
        r.setStatus(rs.getString("status"));
        r.setRawMeetingText(rs.getString("raw_meeting_text"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) r.setCreatedAt(ca.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) r.setUpdatedAt(ua.toLocalDateTime());
        try { r.setCreatedByName(rs.getString("created_by_name")); } catch (SQLException ignored) {}
        try { r.setMeetingType(rs.getString("meeting_type")); } catch (SQLException ignored) {}
        try { r.setProjectTitle(rs.getString("project_title")); } catch (SQLException ignored) {}
        try {
            Timestamp md = rs.getTimestamp("meeting_date");
            if (md != null) r.setMeetingDate(md.toLocalDateTime());
        } catch (SQLException ignored) {}
        return r;
    }
}
