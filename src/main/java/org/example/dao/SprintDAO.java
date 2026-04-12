package org.example.dao;

import org.example.config.DatabaseConfig;
import org.example.model.Sprint;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SprintDAO {

    private static final String SELECT_BASE =
        "SELECT s.*, p.title as project_title FROM sprints s " +
        "LEFT JOIN projects p ON s.project_id = p.id ";

    public List<Sprint> findAll() throws SQLException {
        List<Sprint> list = new ArrayList<>();
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(SELECT_BASE + "ORDER BY s.created_at DESC")) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Sprint> findByProject(int projectId) throws SQLException {
        List<Sprint> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE s.project_id = ? ORDER BY s.sprint_number";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public Sprint findById(int id) throws SQLException {
        String sql = SELECT_BASE + "WHERE s.id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public void save(Sprint s) throws SQLException {
        String sql = "INSERT INTO sprints (project_id, name, goal, sprint_number, status, start_date, end_date, created_at) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getProjectId());
            ps.setString(2, s.getName());
            ps.setString(3, s.getGoal());
            ps.setInt(4, s.getSprintNumber());
            ps.setString(5, s.getStatus() != null ? s.getStatus() : "planned");
            ps.setDate(6, s.getStartDate() != null ? Date.valueOf(s.getStartDate()) : null);
            ps.setDate(7, s.getEndDate() != null ? Date.valueOf(s.getEndDate()) : null);
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) s.setId(keys.getInt(1));
        }
    }

    public void update(Sprint s) throws SQLException {
        String sql = "UPDATE sprints SET name=?, goal=?, status=?, start_date=?, end_date=?, retrospective=? WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getGoal());
            ps.setString(3, s.getStatus());
            ps.setDate(4, s.getStartDate() != null ? Date.valueOf(s.getStartDate()) : null);
            ps.setDate(5, s.getEndDate() != null ? Date.valueOf(s.getEndDate()) : null);
            ps.setString(6, s.getRetrospective());
            ps.setInt(7, s.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        Connection conn = DatabaseConfig.getConnection();
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM tasks WHERE sprint_id = ?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sprints WHERE id = ?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    private Sprint mapRow(ResultSet rs) throws SQLException {
        Sprint s = new Sprint();
        s.setId(rs.getInt("id"));
        s.setProjectId(rs.getInt("project_id"));
        s.setProjectTitle(rs.getString("project_title"));
        s.setName(rs.getString("name"));
        s.setGoal(rs.getString("goal"));
        s.setSprintNumber(rs.getInt("sprint_number"));
        s.setStatus(rs.getString("status"));
        Date sd = rs.getDate("start_date");
        if (sd != null) s.setStartDate(sd.toLocalDate());
        Date ed = rs.getDate("end_date");
        if (ed != null) s.setEndDate(ed.toLocalDate());
        s.setRetrospective(rs.getString("retrospective"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) s.setCreatedAt(ts.toLocalDateTime());
        return s;
    }
}
