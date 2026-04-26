package org.example.dao;

import org.example.config.DatabaseConfig;
import org.example.model.Comment;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommentDAO {

    private static final String SELECT_BASE =
        "SELECT c.*, u.name as author_name, p.title as project_title " +
        "FROM comments c " +
        "LEFT JOIN users u ON c.author_id = u.id " +
        "LEFT JOIN projects p ON c.commentable_id = p.id AND c.commentable_type = 'project' ";

    public List<Comment> findAll() throws SQLException {
        List<Comment> list = new ArrayList<>();
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(SELECT_BASE + "ORDER BY c.created_at DESC")) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Comment> findByProject(int projectId) throws SQLException {
        List<Comment> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE c.commentable_type = 'project' AND c.commentable_id = ? ORDER BY c.created_at DESC";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Comment> findByAuthor(int authorId) throws SQLException {
        List<Comment> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE c.author_id = ? ORDER BY c.created_at DESC";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, authorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public void save(Comment c) throws SQLException {
        String sql = "INSERT INTO comments (author_id, commentable_type, commentable_id, content, subject, comment_type, target, importance, created_at) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getAuthorId());
            ps.setString(2, c.getCommentableType());
            ps.setInt(3, c.getCommentableId());
            ps.setString(4, c.getContent());
            ps.setString(5, c.getSubject());
            ps.setString(6, c.getCommentType());
            ps.setString(7, c.getTarget());
            ps.setString(8, c.getImportance());
            ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) c.setId(keys.getInt(1));
        }
    }

    /**
     * Check if a comment with the same subject, content and author already exists on the same day
     * Used for uniqueness validation before saving
     */
    public boolean existsDuplicate(String subject, String content, int authorId, int commentableId, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM comments " +
                     "WHERE LOWER(TRIM(subject)) = LOWER(TRIM(?)) " +
                     "AND LOWER(TRIM(content)) = LOWER(TRIM(?)) " +
                     "AND author_id = ? " +
                     "AND commentable_id = ? " +
                     "AND DATE(created_at) = CURDATE() " +
                     "AND id != ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, subject != null ? subject : "");
            ps.setString(2, content);
            ps.setInt(3, authorId);
            ps.setInt(4, commentableId);
            ps.setInt(5, excludeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }

    public void update(Comment c) throws SQLException {
        String sql = "UPDATE comments SET content=?, subject=?, comment_type=?, target=?, importance=? WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, c.getContent());
            ps.setString(2, c.getSubject());
            ps.setString(3, c.getCommentType());
            ps.setString(4, c.getTarget());
            ps.setString(5, c.getImportance());
            ps.setInt(6, c.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM comments WHERE id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Comment mapRow(ResultSet rs) throws SQLException {
        Comment c = new Comment();
        c.setId(rs.getInt("id"));
        c.setAuthorId(rs.getInt("author_id"));
        c.setAuthorName(rs.getString("author_name"));
        c.setCommentableType(rs.getString("commentable_type"));
        c.setCommentableId(rs.getInt("commentable_id"));
        c.setProjectTitle(rs.getString("project_title"));
        c.setContent(rs.getString("content"));
        c.setSubject(rs.getString("subject"));
        c.setCommentType(rs.getString("comment_type"));
        c.setTarget(rs.getString("target"));
        c.setImportance(rs.getString("importance"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) c.setCreatedAt(ts.toLocalDateTime());
        return c;
    }
}
