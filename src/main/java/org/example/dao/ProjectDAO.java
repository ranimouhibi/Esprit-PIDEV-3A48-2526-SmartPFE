package org.example.dao;

import org.example.config.DatabaseConfig;
import org.example.model.Project;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProjectDAO {

    private static final String SELECT_BASE =
        "SELECT p.*, u.name as owner_name, s.name as supervisor_name " +
        "FROM projects p " +
        "LEFT JOIN users u ON p.owner_id = u.id " +
        "LEFT JOIN users s ON p.supervisor_id = s.id ";

    public List<Project> findAll() throws SQLException {
        List<Project> list = new ArrayList<>();
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(SELECT_BASE + "ORDER BY p.created_at DESC")) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Project> findByOwner(int ownerId) throws SQLException {
        List<Project> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE p.owner_id = ? ORDER BY p.created_at DESC";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // Find all projects where user is owner OR member (approved or pending)
    public List<Project> findByUserProjects(int userId) throws SQLException {
        List<Project> list = new ArrayList<>();
        String sql = SELECT_BASE + 
            "WHERE p.owner_id = ? OR p.id IN (SELECT project_id FROM project_members WHERE user_id = ? AND status IN ('pending', 'approved')) " +
            "ORDER BY p.created_at DESC";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // Add member to project
    public void addMember(int projectId, int userId) throws SQLException {
        String sql = "INSERT INTO project_members (project_id, user_id, status, joined_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setInt(2, userId);
            ps.setString(3, "pending"); // Status: pending, approved, rejected
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    // Check if user is already a member
    public boolean isMember(int projectId, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM project_members WHERE project_id = ? AND user_id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public Project findByJoinCode(String joinCode) throws SQLException {
        String sql = SELECT_BASE + "WHERE p.join_code = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, joinCode);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public List<Project> findBySupervisor(int supervisorId) throws SQLException {
        List<Project> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE p.supervisor_id = ? ORDER BY p.created_at DESC";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, supervisorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public Project findById(int id) throws SQLException {
        String sql = SELECT_BASE + "WHERE p.id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public void save(Project p) throws SQLException {
        String sql = "INSERT INTO projects (title, description, project_type, status, join_code, owner_id, supervisor_id, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getTitle());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getProjectType());
            ps.setString(4, p.getStatus() != null ? p.getStatus() : "created");
            ps.setString(5, p.getJoinCode());
            ps.setInt(6, p.getOwnerId());
            if (p.getSupervisorId() > 0) ps.setInt(7, p.getSupervisorId());
            else ps.setNull(7, Types.INTEGER);
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) p.setId(keys.getInt(1));
        }
    }

    public void update(Project p) throws SQLException {
        String sql = "UPDATE projects SET title=?, description=?, project_type=?, status=?, supervisor_id=?, updated_at=? WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, p.getTitle());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getProjectType());
            ps.setString(4, p.getStatus());
            if (p.getSupervisorId() > 0) ps.setInt(5, p.getSupervisorId());
            else ps.setNull(5, Types.INTEGER);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(7, p.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM projects WHERE id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Project mapRow(ResultSet rs) throws SQLException {
        Project p = new Project();
        p.setId(rs.getInt("id"));
        p.setTitle(rs.getString("title"));
        p.setDescription(rs.getString("description"));
        p.setProjectType(rs.getString("project_type"));
        p.setStatus(rs.getString("status"));
        p.setJoinCode(rs.getString("join_code"));
        p.setOwnerId(rs.getInt("owner_id"));
        p.setOwnerName(rs.getString("owner_name"));
        p.setSupervisorId(rs.getInt("supervisor_id"));
        p.setSupervisorName(rs.getString("supervisor_name"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) p.setCreatedAt(ts.toLocalDateTime());
        return p;
    }
}
