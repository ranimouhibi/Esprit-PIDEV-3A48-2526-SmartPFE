package org.example.dao;

import org.example.config.DatabaseConfig;
import org.example.model.Project;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProjectDAO {

    private static final String SELECT_BASE =
        "SELECT p.*, u.name as owner_name, u.email as owner_email, " +
        "s.name as supervisor_name, s.email as supervisor_email " +
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

    /** Returns all users who are members of a project (owner + approved members). */
    public List<org.example.model.User> findProjectMembers(int projectId) throws SQLException {
        List<org.example.model.User> members = new ArrayList<>();
        String sql = "SELECT u.* FROM users u " +
            "WHERE u.id = (SELECT owner_id FROM projects WHERE id = ?) " +
            "UNION " +
            "SELECT u.* FROM users u " +
            "INNER JOIN project_members pm ON pm.user_id = u.id " +
            "WHERE pm.project_id = ? AND pm.status IN ('pending', 'approved')";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setInt(2, projectId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                org.example.model.User u = new org.example.model.User();
                u.setId(rs.getInt("id"));
                u.setName(rs.getString("name"));
                u.setEmail(rs.getString("email"));
                u.setRole(rs.getString("role"));
                try { u.setSkills(rs.getString("skills")); } catch (Exception ignored) {}
                members.add(u);
            }
        }
        return members;
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

    /** Returns projects visible to the given user based on their role. */
    public List<Project> findForUser(int userId, String role) throws SQLException {
        if ("admin".equalsIgnoreCase(role)) return findAll();
        if ("supervisor".equalsIgnoreCase(role)) return findBySupervisor(userId);
        return findByUserProjects(userId); // student — owner or member
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

    /**
     * Check if a project with the same title, type and owner already exists on the same day
     * Used for uniqueness validation before saving
     */
    public boolean existsDuplicate(String title, String projectType, int ownerId, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM projects " +
                     "WHERE LOWER(TRIM(title)) = LOWER(TRIM(?)) " +
                     "AND project_type = ? " +
                     "AND owner_id = ? " +
                     "AND DATE(created_at) = CURDATE() " +
                     "AND id != ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, projectType);
            ps.setInt(3, ownerId);
            ps.setInt(4, excludeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
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
        Connection conn = DatabaseConfig.getConnection();
        // Delete child records in dependency order before removing the project
        for (String table : new String[]{"tasks", "sprints", "meetings", "comments", "documents", "project_members"}) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM " + table + " WHERE project_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM projects WHERE id = ?")) {
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
        p.setOwnerEmail(rs.getString("owner_email"));
        p.setSupervisorId(rs.getInt("supervisor_id"));
        p.setSupervisorName(rs.getString("supervisor_name"));
        p.setSupervisorEmail(rs.getString("supervisor_email"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) p.setCreatedAt(ts.toLocalDateTime());
        return p;
    }
}
