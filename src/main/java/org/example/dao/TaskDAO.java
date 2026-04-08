package org.example.dao;

import org.example.config.DatabaseConfig;
import org.example.model.Task;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TaskDAO {

    private static final String SELECT_BASE =
        "SELECT t.*, p.title as project_title, s.name as sprint_name, u.name as assigned_to_name " +
        "FROM tasks t " +
        "LEFT JOIN projects p ON t.project_id = p.id " +
        "LEFT JOIN sprints s ON t.sprint_id = s.id " +
        "LEFT JOIN users u ON t.assigned_to_id = u.id ";

    public List<Task> findAll() throws SQLException {
        List<Task> list = new ArrayList<>();
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(SELECT_BASE + "ORDER BY t.created_at DESC")) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Task> findByProject(int projectId) throws SQLException {
        List<Task> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE t.project_id = ? ORDER BY t.priority DESC, t.created_at DESC";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Task> findBySprint(int sprintId) throws SQLException {
        List<Task> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE t.sprint_id = ? ORDER BY t.priority DESC";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, sprintId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Task> findByAssignedUser(int userId) throws SQLException {
        List<Task> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE t.assigned_to_id = ? ORDER BY t.deadline ASC";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public void save(Task t) throws SQLException {
        String sql = "INSERT INTO tasks (project_id, sprint_id, title, description, status, priority, story_points, assigned_to_id, is_blocked, deadline, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, t.getProjectId());
            if (t.getSprintId() != null) ps.setInt(2, t.getSprintId()); else ps.setNull(2, Types.INTEGER);
            ps.setString(3, t.getTitle());
            ps.setString(4, t.getDescription());
            ps.setString(5, t.getStatus() != null ? t.getStatus() : "todo");
            ps.setString(6, t.getPriority() != null ? t.getPriority() : "medium");
            ps.setInt(7, t.getStoryPoints());
            if (t.getAssignedToId() != null) ps.setInt(8, t.getAssignedToId()); else ps.setNull(8, Types.INTEGER);
            ps.setBoolean(9, t.isBlocked());
            ps.setDate(10, t.getDeadline() != null ? Date.valueOf(t.getDeadline()) : null);
            ps.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) t.setId(keys.getInt(1));
        }
    }

    public void update(Task t) throws SQLException {
        String sql = "UPDATE tasks SET title=?, description=?, status=?, priority=?, story_points=?, assigned_to_id=?, is_blocked=?, blocker_description=?, deadline=?, updated_at=? WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, t.getTitle());
            ps.setString(2, t.getDescription());
            ps.setString(3, t.getStatus());
            ps.setString(4, t.getPriority());
            ps.setInt(5, t.getStoryPoints());
            if (t.getAssignedToId() != null) ps.setInt(6, t.getAssignedToId()); else ps.setNull(6, Types.INTEGER);
            ps.setBoolean(7, t.isBlocked());
            ps.setString(8, t.getBlockerDescription());
            ps.setDate(9, t.getDeadline() != null ? Date.valueOf(t.getDeadline()) : null);
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(11, t.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Task mapRow(ResultSet rs) throws SQLException {
        Task t = new Task();
        t.setId(rs.getInt("id"));
        t.setProjectId(rs.getInt("project_id"));
        t.setProjectTitle(rs.getString("project_title"));
        int sprintId = rs.getInt("sprint_id");
        if (!rs.wasNull()) t.setSprintId(sprintId);
        t.setSprintName(rs.getString("sprint_name"));
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));
        t.setStatus(rs.getString("status"));
        t.setPriority(rs.getString("priority"));
        t.setStoryPoints(rs.getInt("story_points"));
        int assignedId = rs.getInt("assigned_to_id");
        if (!rs.wasNull()) t.setAssignedToId(assignedId);
        t.setAssignedToName(rs.getString("assigned_to_name"));
        t.setBlocked(rs.getBoolean("is_blocked"));
        t.setBlockerDescription(rs.getString("blocker_description"));
        Date d = rs.getDate("deadline");
        if (d != null) t.setDeadline(d.toLocalDate());
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) t.setCreatedAt(ts.toLocalDateTime());
        return t;
    }
}
