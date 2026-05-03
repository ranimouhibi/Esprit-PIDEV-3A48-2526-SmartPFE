package org.example.dao;

import org.example.config.DatabaseConfig;
import org.example.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la table meeting_participants (ManyToMany Meeting <-> User).
 * Gère aussi la récupération du superviseur du projet lié au meeting.
 */
public class MeetingParticipantDAO {

    /**
     * Récupère tous les participants d'un meeting (depuis meeting_participants).
     * Retourne une liste vide si la table n'existe pas encore.
     */
    public List<User> findParticipants(int meetingId) throws SQLException {
        List<User> list = new ArrayList<>();
        try {
            String sql = "SELECT u.* FROM users u "
                    + "INNER JOIN meeting_participants mp ON u.id = mp.user_id "
                    + "WHERE mp.meeting_id = ?";
            try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
                ps.setInt(1, meetingId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("meeting_participants") || e.getMessage().contains("doesn't exist")) {
                return list; // Table pas encore créée, retourner liste vide
            }
            throw e;
        }
        return list;
    }

    /**
     * Récupère participants + superviseur du projet (sans doublons).
     * Gère le cas où la table meeting_participants n'existe pas encore.
     */
    public List<User> findParticipantsAndSupervisor(int meetingId) throws SQLException {
        List<User> list = new ArrayList<>();
        try {
            // Participants
            String sql = "SELECT DISTINCT u.* FROM users u "
                    + "INNER JOIN meeting_participants mp ON u.id = mp.user_id "
                    + "WHERE mp.meeting_id = ? "
                    + "UNION "
                    + "SELECT DISTINCT u.* FROM users u "
                    + "INNER JOIN projects p ON u.id = p.supervisor_id "
                    + "INNER JOIN meetings m ON m.project_id = p.id "
                    + "WHERE m.id = ? AND u.email IS NOT NULL";
            try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
                ps.setInt(1, meetingId);
                ps.setInt(2, meetingId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            // Si la table meeting_participants n'existe pas, récupérer seulement le superviseur
            if (e.getMessage().contains("meeting_participants") || e.getMessage().contains("doesn't exist")) {
                String sql = "SELECT DISTINCT u.* FROM users u "
                        + "INNER JOIN projects p ON u.id = p.supervisor_id "
                        + "INNER JOIN meetings m ON m.project_id = p.id "
                        + "WHERE m.id = ? AND u.email IS NOT NULL";
                try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
                    ps.setInt(1, meetingId);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) list.add(mapUser(rs));
                }
            } else {
                throw e;
            }
        }
        return list;
    }

    /**
     * Ajoute un participant à un meeting.
     */
    public void addParticipant(int meetingId, int userId) throws SQLException {
        String sql = "INSERT IGNORE INTO meeting_participants (meeting_id, user_id) VALUES (?, ?)";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, meetingId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Supprime tous les participants d'un meeting.
     */
    public void removeAllParticipants(int meetingId) throws SQLException {
        String sql = "DELETE FROM meeting_participants WHERE meeting_id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, meetingId);
            ps.executeUpdate();
        }
    }

    /**
     * Récupère les meetings d'un utilisateur (participant OU superviseur du projet).
     * Utilisé pour le calendrier.
     */
    public List<int[]> findMeetingIdsByUser(int userId) throws SQLException {
        List<int[]> list = new ArrayList<>();
        String sql = "SELECT DISTINCT m.id FROM meetings m "
                + "LEFT JOIN meeting_participants mp ON m.id = mp.meeting_id "
                + "LEFT JOIN projects p ON m.project_id = p.id "
                + "WHERE mp.user_id = ? OR p.supervisor_id = ? OR p.owner_id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new int[]{rs.getInt(1)});
        }
        return list;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setEmail(rs.getString("email"));
        u.setName(rs.getString("name"));
        u.setRole(rs.getString("role"));
        u.setPhone(rs.getString("phone"));
        u.setActive(rs.getBoolean("is_active"));
        return u;
    }
}
