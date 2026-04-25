package org.example.dao;

import org.example.config.DatabaseConfig;
import org.example.model.CandidatureNote;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CandidatureNoteDAO {

    public List<CandidatureNote> findByCandidature(int candidatureId) throws SQLException {
        List<CandidatureNote> list = new ArrayList<>();
        String sql = "SELECT cn.*, u.name as author_name FROM candidature_notes cn " +
                     "LEFT JOIN users u ON cn.author_id = u.id " +
                     "WHERE cn.candidature_id = ? ORDER BY cn.created_at DESC";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, candidatureId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public void save(CandidatureNote note) throws SQLException {
        String sql = "INSERT INTO candidature_notes (candidature_id, author_id, content, rating, is_private, created_at, updated_at) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, note.getCandidatureId());
            ps.setInt(2, note.getAuthorId());
            ps.setString(3, note.getContent());
            ps.setInt(4, note.getRating());
            ps.setBoolean(5, note.isPrivate());
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) note.setId(keys.getInt(1));
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement("DELETE FROM candidature_notes WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private CandidatureNote mapRow(ResultSet rs) throws SQLException {
        CandidatureNote n = new CandidatureNote();
        n.setId(rs.getInt("id"));
        n.setCandidatureId(rs.getInt("candidature_id"));
        n.setAuthorId(rs.getInt("author_id"));
        n.setAuthorName(rs.getString("author_name"));
        n.setContent(rs.getString("content"));
        n.setRating(rs.getInt("rating"));
        n.setPrivate(rs.getBoolean("is_private"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) n.setCreatedAt(ts.toLocalDateTime());
        Timestamp ts2 = rs.getTimestamp("updated_at");
        if (ts2 != null) n.setUpdatedAt(ts2.toLocalDateTime());
        return n;
    }
}
