package org.example.service;

import org.example.config.DatabaseConfig;

import javax.mail.*;
import javax.mail.internet.*;
import java.sql.*;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Email notification service using JavaMail.
 * Configure SMTP via environment variables or update constants below.
 *
 * Environment variables:
 *   SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS, SMTP_FROM
 */
public class EmailNotificationService {

    private static final String SMTP_HOST = getEnv("SMTP_HOST", "smtp.gmail.com");
    private static final String SMTP_PORT = getEnv("SMTP_PORT", "587");
    private static final String SMTP_USER = getEnv("SMTP_USER", "");
    private static final String SMTP_PASS = getEnv("SMTP_PASS", "");
    private static final String SMTP_FROM = getEnv("SMTP_FROM", SMTP_USER);
    private static final String APP_NAME  = "SmartPFE";

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "email-sender");
        t.setDaemon(true);
        return t;
    });

    // ── Public API ────────────────────────────────────────────────────────────

    /** Send confirmation email to student after applying */
    public void sendCandidatureConfirmation(int candidatureId) {
        executor.submit(() -> {
            try {
                String[] data = loadCandidatureData(candidatureId);
                if (data == null) return;
                String studentEmail = data[0], studentName = data[1], offerTitle = data[2];
                String subject = "[" + APP_NAME + "] Application Confirmation - " + offerTitle;
                String body = buildHtml(
                    "Application Received",
                    "Dear " + studentName + ",",
                    "Your application for <strong>" + offerTitle + "</strong> has been successfully submitted.",
                    "We will review your application and get back to you soon.",
                    "Good luck!"
                );
                send(studentEmail, subject, body);
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    /** Notify student of status change (accepted/rejected/interview) */
    public void sendStatusChange(int candidatureId, String newStatus, String feedback) {
        executor.submit(() -> {
            try {
                String[] data = loadCandidatureData(candidatureId);
                if (data == null) return;
                String studentEmail = data[0], studentName = data[1], offerTitle = data[2];

                String statusLabel = switch (newStatus) {
                    case "accepted"  -> "✅ Accepted";
                    case "rejected"  -> "❌ Rejected";
                    case "interview" -> "📅 Interview Scheduled";
                    default          -> newStatus;
                };
                String subject = "[" + APP_NAME + "] Application Update: " + statusLabel + " - " + offerTitle;
                String feedbackHtml = (feedback != null && !feedback.isBlank())
                    ? "<p><strong>Feedback:</strong> " + feedback + "</p>" : "";
                String body = buildHtml(
                    "Application Status Update",
                    "Dear " + studentName + ",",
                    "Your application for <strong>" + offerTitle + "</strong> has been updated.",
                    "<p>New status: <strong style='color:" + statusColor(newStatus) + "'>" + statusLabel + "</strong></p>" + feedbackHtml,
                    "Log in to SmartPFE for more details."
                );
                send(studentEmail, subject, body);
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    /** Notify establishment of new candidature */
    public void sendNewCandidatureAlert(int candidatureId) {
        executor.submit(() -> {
            try {
                String sql = "SELECT u_est.email as est_email, u_est.name as est_name, " +
                             "u_stu.name as student_name, o.title as offer_title " +
                             "FROM candidatures c " +
                             "JOIN project_offers o ON c.offer_id = o.id " +
                             "JOIN establishments e ON o.establishment_id = e.id " +
                             "JOIN users u_est ON u_est.establishment_id = e.id AND u_est.role = 'establishment' " +
                             "JOIN users u_stu ON c.student_id = u_stu.id " +
                             "WHERE c.id = ? LIMIT 1";
                try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
                    ps.setInt(1, candidatureId);
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) return;
                    String estEmail   = rs.getString("est_email");
                    String estName    = rs.getString("est_name");
                    String studentName = rs.getString("student_name");
                    String offerTitle  = rs.getString("offer_title");
                    String subject = "[" + APP_NAME + "] New Application Received - " + offerTitle;
                    String body = buildHtml(
                        "New Application",
                        "Dear " + estName + ",",
                        "<strong>" + studentName + "</strong> has applied for your offer: <strong>" + offerTitle + "</strong>.",
                        "Log in to SmartPFE to review the application.",
                        ""
                    );
                    send(estEmail, subject, body);
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    /** Send reminder for pending candidatures (call periodically) */
    public void sendPendingReminders() {
        executor.submit(() -> {
            try {
                String sql = "SELECT c.id, u.email, u.name, o.title " +
                             "FROM candidatures c " +
                             "JOIN users u ON c.student_id = u.id " +
                             "JOIN project_offers o ON c.offer_id = o.id " +
                             "WHERE c.status = 'pending' " +
                             "AND c.created_at < DATE_SUB(NOW(), INTERVAL 7 DAY)";
                try (Statement st = DatabaseConfig.getConnection().createStatement();
                     ResultSet rs = st.executeQuery(sql)) {
                    while (rs.next()) {
                        String email = rs.getString("email");
                        String name  = rs.getString("name");
                        String offer = rs.getString("title");
                        String subject = "[" + APP_NAME + "] Reminder: Your application is still pending";
                        String body = buildHtml(
                            "Application Reminder",
                            "Dear " + name + ",",
                            "Your application for <strong>" + offer + "</strong> is still under review.",
                            "We appreciate your patience. You will be notified once a decision is made.",
                            ""
                        );
                        send(email, subject, body);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void send(String to, String subject, String htmlBody) throws MessagingException {
        if (SMTP_USER.isBlank()) {
            System.out.println("[EmailService] SMTP not configured. Would send to: " + to + " | " + subject);
            return;
        }
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_HOST);

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
            }
        });

        Message msg = new MimeMessage(session);
        try {
            msg.setFrom(new InternetAddress(SMTP_FROM, APP_NAME, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            msg.setFrom(new InternetAddress(SMTP_FROM));
        }
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        msg.setSubject(subject);
        msg.setContent(htmlBody, "text/html; charset=UTF-8");
        Transport.send(msg);
        System.out.println("[EmailService] Sent to: " + to);
    }

    private String[] loadCandidatureData(int candidatureId) throws SQLException {
        String sql = "SELECT u.email, u.name, o.title FROM candidatures c " +
                     "JOIN users u ON c.student_id = u.id " +
                     "JOIN project_offers o ON c.offer_id = o.id WHERE c.id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, candidatureId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new String[]{rs.getString(1), rs.getString(2), rs.getString(3)};
        }
        return null;
    }

    private String buildHtml(String title, String greeting, String main, String detail, String closing) {
        return "<html><body style='font-family:Arial,sans-serif;max-width:600px;margin:auto;'>" +
            "<div style='background:#a12c2f;padding:20px;text-align:center;'>" +
            "<h1 style='color:white;margin:0;'>SmartPFE</h1></div>" +
            "<div style='padding:30px;background:#f9f9f9;'>" +
            "<h2 style='color:#1a1a2e;'>" + title + "</h2>" +
            "<p>" + greeting + "</p>" +
            "<p>" + main + "</p>" +
            "<p>" + detail + "</p>" +
            "<p style='color:#888;font-size:12px;'>" + closing + "</p>" +
            "</div><div style='background:#eee;padding:10px;text-align:center;font-size:11px;color:#999;'>" +
            "SmartPFE — Automated notification, please do not reply.</div></body></html>";
    }

    private String statusColor(String status) {
        return switch (status) {
            case "accepted"  -> "#28a745";
            case "rejected"  -> "#dc3545";
            case "interview" -> "#667eea";
            default          -> "#ffc107";
        };
    }

    private static String getEnv(String key, String def) {
        String v = System.getenv(key);
        return (v != null && !v.isBlank()) ? v : def;
    }
}
