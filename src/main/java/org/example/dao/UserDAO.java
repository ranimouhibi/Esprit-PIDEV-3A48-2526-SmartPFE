package org.example.dao;

import org.example.config.DatabaseConfig;
import org.example.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public User authenticate(String email, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ? AND password = ? AND is_active = 1";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
            if (rs.next()) {
                String stored = rs.getString("password");
                boolean match;
                if (stored != null && stored.startsWith("$2")) {
                    // BCrypt hash — normalize $2y$ and $2b$ to $2a$
                    String hashed = stored.replaceAll("^\\$2[yb]\\$", "\\$2a\\$");
                    try {
                        match = BCrypt.checkpw(password, hashed);
                    } catch (Exception e) {
                        match = false;
                    }
                } else {
                    // Plain text password (test accounts)
                    match = password.equals(stored);
                }
                if (match) return mapRow(rs);
            }
        }
        return null;
    }

    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) users.add(mapRow(rs));
        }
        return users;
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public List<User> findByRole(String role) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY name";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, role);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) users.add(mapRow(rs));
        }
        return users;
    }

    public void save(User user) throws SQLException {
        String sql = "INSERT INTO users (email, password, role, name, phone, is_active, is_verified, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole());
            ps.setString(4, user.getName());
            ps.setString(5, user.getPhone());
            ps.setBoolean(6, user.isActive());
            ps.setBoolean(7, false);
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) user.setId(keys.getInt(1));
        }
    }

    public void update(User user) throws SQLException {
        String sql = "UPDATE users SET email=?, name=?, phone=?, role=?, is_active=?, updated_at=? WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getName());
            ps.setString(3, user.getPhone());
            ps.setString(4, user.getRole());
            ps.setBoolean(5, user.isActive());
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(7, user.getId());
            ps.executeUpdate();
        }
    }

    public void updateProfile(User user) throws SQLException {
        // Base update — always works regardless of whether profile_picture column exists
        String sql = "UPDATE users SET name=?, phone=?, email=?, updated_at=? WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getPhone());
            ps.setString(3, user.getEmail());
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(5, user.getId());
            ps.executeUpdate();
        }
        // Store Cloudinary URL — also handles null (clears picture), skip if column missing
        try {
            String picSql = "UPDATE users SET profile_picture=? WHERE id=?";
            try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(picSql)) {
                ps.setString(1, user.getProfilePicture());
                ps.setInt(2, user.getId());
                ps.executeUpdate();
            }
        } catch (SQLException ignored) {
            // Column doesn't exist yet — run: ALTER TABLE users ADD COLUMN profile_picture VARCHAR(500) NULL
        }
    }

    public void updatePassword(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password=?, updated_at=? WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    public boolean emailExistsForOther(String email, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ? AND id != ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setInt(2, excludeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }

    public void saveResetToken(String email, String token, Timestamp expiry) throws SQLException {
        String sql = "UPDATE users SET password_reset_token=?, password_reset_expires=? WHERE email=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setTimestamp(2, expiry);
            ps.setString(3, email);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new SQLException("No user found with email: " + email);
        }
    }

    public User findByResetToken(String token) throws SQLException {
        String sql = "SELECT * FROM users WHERE password_reset_token=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Timestamp expiry = rs.getTimestamp("password_reset_expires");
                if (expiry == null || expiry.toLocalDateTime().isBefore(LocalDateTime.now())) {
                    System.out.println("[DEBUG] Token expired. expiry=" + expiry + " now=" + LocalDateTime.now());
                    return null;
                }
                return mapRow(rs);
            }
        }
        return null;
    }

    public void clearResetToken(int userId) throws SQLException {
        String sql = "UPDATE users SET password_reset_token=NULL, password_reset_expires=NULL WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setRole(rs.getString("role"));
        u.setName(rs.getString("name"));
        u.setPhone(rs.getString("phone"));
        u.setActive(rs.getBoolean("is_active"));
        try { u.setSkills(rs.getString("skills")); } catch (Exception ignored) {}
        try { u.setEstablishmentId(rs.getInt("establishment_id")); } catch (Exception ignored) {}
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) u.setCreatedAt(ts.toLocalDateTime());
        try { u.setProfilePicture(rs.getString("profile_picture")); } catch (SQLException ignored) {}
        return u;
    }
}
