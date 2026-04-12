package org.example.dao;

import org.example.config.DatabaseConfig;
import org.example.model.Document;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DocumentDAO {

    private static final String SELECT_BASE =
        "SELECT d.*, p.title as project_title, u.name as uploader_name " +
        "FROM documents d " +
        "LEFT JOIN projects p ON d.project_id = p.id " +
        "LEFT JOIN users u ON d.uploaded_by_id = u.id ";

    public List<Document> findAll() throws SQLException {
        List<Document> list = new ArrayList<>();
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(SELECT_BASE + "ORDER BY d.uploaded_at DESC")) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Document> findByProject(int projectId) throws SQLException {
        List<Document> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE d.project_id = ? ORDER BY d.uploaded_at DESC";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // For supervisor: find docs of projects they supervise
    public List<Document> findBySupervisor(int supervisorId) throws SQLException {
        List<Document> list = new ArrayList<>();
        String sql = SELECT_BASE +
            "WHERE p.supervisor_id = ? ORDER BY d.uploaded_at DESC";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, supervisorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public void save(Document d) throws SQLException {
        String sql = "INSERT INTO documents (project_id, uploaded_by_id, filename, file_path, file_type, category, description, version, uploaded_at) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, d.getProjectId());
            ps.setInt(2, d.getUploadedById());
            ps.setString(3, d.getFilename());
            ps.setString(4, d.getFilePath());
            ps.setString(5, d.getFileType());
            ps.setString(6, d.getCategory());
            ps.setString(7, d.getDescription());
            ps.setInt(8, d.getVersion());
            ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) d.setId(keys.getInt(1));
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM documents WHERE id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Document mapRow(ResultSet rs) throws SQLException {
        Document d = new Document();
        d.setId(rs.getInt("id"));
        d.setProjectId(rs.getInt("project_id"));
        d.setUploadedById(rs.getInt("uploaded_by_id"));
        d.setFilename(rs.getString("filename"));
        d.setFilePath(rs.getString("file_path"));
        d.setFileType(rs.getString("file_type"));
        d.setCategory(rs.getString("category"));
        d.setDescription(rs.getString("description"));
        d.setVersion(rs.getInt("version"));
        d.setProjectTitle(rs.getString("project_title"));
        d.setUploaderName(rs.getString("uploader_name"));
        Timestamp ts = rs.getTimestamp("uploaded_at");
        if (ts != null) d.setUploadedAt(ts.toLocalDateTime());
        return d;
    }
}
