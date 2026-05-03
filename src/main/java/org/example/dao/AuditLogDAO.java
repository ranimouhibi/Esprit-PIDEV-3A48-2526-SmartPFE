package org.example.dao;

import org.example.config.DatabaseConfig;
import org.example.model.AuditLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {

    public List<AuditLog> findAll() throws SQLException {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT al.id, al.action, al.details, al.ip_address, al.created_at, al.user_id, u.name AS user_name " +
                     "FROM audit_logs al " +
                     "LEFT JOIN users u ON al.user_id = u.id " +
                     "ORDER BY al.created_at DESC";
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) logs.add(mapRow(rs));
        }
        return logs;
    }

    public List<AuditLog> search(String keyword, String actionFilter) throws SQLException {
        List<AuditLog> all = findAll();
        return all.stream().filter(log -> {
            boolean matchKeyword = keyword == null || keyword.isBlank()
                || (log.getUserName() != null && log.getUserName().toLowerCase().contains(keyword.toLowerCase()))
                || (log.getIpAddress() != null && log.getIpAddress().toLowerCase().contains(keyword.toLowerCase()))
                || (log.getDetails() != null && log.getDetails().toLowerCase().contains(keyword.toLowerCase()));
            boolean matchAction = actionFilter == null || actionFilter.isBlank() || actionFilter.equals("All")
                || actionFilter.equalsIgnoreCase(log.getAction());
            return matchKeyword && matchAction;
        }).toList();
    }

    public List<String> findDistinctActions() throws SQLException {
        List<String> actions = new ArrayList<>();
        actions.add("All");
        String sql = "SELECT DISTINCT action FROM audit_logs WHERE action IS NOT NULL ORDER BY action";
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String a = rs.getString("action");
                if (a != null && !a.isBlank()) actions.add(a);
            }
        }
        return actions;
    }

    private AuditLog mapRow(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setId(rs.getInt("id"));

        int uid = rs.getInt("user_id");
        if (!rs.wasNull()) log.setUserId(uid);

        log.setUserName(rs.getString("user_name"));
        log.setAction(rs.getString("action"));
        log.setDetails(rs.getString("details"));
        log.setIpAddress(rs.getString("ip_address"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) log.setCreatedAt(ts.toLocalDateTime());

        return log;
    }
}
