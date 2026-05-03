package org.example.util;

import org.example.config.AppConfig;
import org.example.model.Project;
import org.example.model.User;

import javax.mail.*;
import javax.mail.internet.*;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Email service for SmartPFE Desktop - mirrors the Symfony EmailService.php
 *
 * Sends:
 *  1. sendProjectCreatedEmail(project, owner)
 *     → supervisor : "New Project Created: <title>"
 *     → owner      : "Your Project Has Been Created: <title>"
 *  2. sendProjectStatusChangedEmail(project, owner, oldStatus)
 *     → owner      : "Project Status Updated: <title>"
 */
public class MailUtil {

    // ── Credentials loaded from config.properties ───────────────────────────────
    private static final String FROM_EMAIL = AppConfig.getMailFrom();
    private static final String APP_PASSWORD = AppConfig.getMailPassword();
    private static final String SMTP_HOST = AppConfig.getMailSmtpHost();
    private static final String SMTP_PORT = AppConfig.getMailSmtpPort();

    // ─────────────────────────────────────────────────────────────────────
    // Public API  (same 3 methods as EmailService.php)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Called when a project is created.
     * Sends one email to the supervisor and one to the student (owner).
     */
    public static void sendProjectCreatedEmail(Project project, User owner) {
        // Email to supervisor
        if (project.getSupervisorName() != null && project.getSupervisorEmail() != null
                && !project.getSupervisorEmail().isEmpty()) {
            sendEmail(
                project.getSupervisorEmail(),
                "New Project Created: " + project.getTitle(),
                buildSupervisorCreatedTemplate(project, owner)
            );
        }

        // Email to student (owner)
        if (owner != null && owner.getEmail() != null && !owner.getEmail().isEmpty()) {
            sendEmail(
                owner.getEmail(),
                "Your Project Has Been Created: " + project.getTitle(),
                buildOwnerCreatedTemplate(project, owner)
            );
        }
    }

    /**
     * Called when a project status changes.
     * Sends one email to the student (owner).
     */
    public static void sendProjectStatusChangedEmail(Project project, User owner, String oldStatus) {
        if (owner != null && owner.getEmail() != null && !owner.getEmail().isEmpty()) {
            sendEmail(
                owner.getEmail(),
                "Project Status Updated: " + project.getTitle(),
                buildStatusChangedTemplate(project, owner, oldStatus)
            );
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Core send method
    // ─────────────────────────────────────────────────────────────────────

    private static void sendEmail(String to, String subject, String htmlBody) {
        Properties props = new Properties();
        props.put("mail.smtp.host",               SMTP_HOST);
        props.put("mail.smtp.port",               SMTP_PORT);
        props.put("mail.smtp.auth",               "true");
        props.put("mail.smtp.socketFactory.port", SMTP_PORT);
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.ssl.protocols",      "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(FROM_EMAIL, "SmartPFE"));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            msg.setSubject(subject);
            msg.setContent(htmlBody, "text/html; charset=utf-8");
            Transport.send(msg);
            System.out.println("✅ Email sent to: " + to);
        } catch (Exception e) {
            // Log but don't crash the app (same as PHP: error_log + no rethrow)
            System.err.println("❌ Email sending failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML Templates  (identical structure to EmailService.php)
    // ─────────────────────────────────────────────────────────────────────

    /** Mirror of getProjectCreatedTemplate() */
    private static String buildSupervisorCreatedTemplate(Project project, User owner) {
        String createdAt = project.getCreatedAt() != null
            ? project.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))
            : "N/A";

        return "<html><body style=\"font-family:Arial,sans-serif;padding:20px;background-color:#f5f5f5;\">"
            + "<div style=\"max-width:600px;margin:0 auto;background:white;padding:30px;border-radius:10px;\">"
            + "<h2 style=\"color:#a94442;\">🎉 New Project Created</h2>"
            + "<p>Hello <strong>" + esc(project.getSupervisorName()) + "</strong>,</p>"
            + "<p>A new project has been assigned to you for supervision:</p>"
            + "<div style=\"background:#f8f9fa;padding:20px;border-left:4px solid #a94442;margin:20px 0;\">"
            + "<h3 style=\"margin-top:0;\">" + esc(project.getTitle()) + "</h3>"
            + "<p><strong>Type:</strong> "   + esc(project.getProjectType()) + "</p>"
            + "<p><strong>Status:</strong> " + esc(project.getStatus())      + "</p>"
            + "<p><strong>Student:</strong> " + esc(owner != null ? owner.getName() : "N/A") + "</p>"
            + "<p><strong>Created:</strong> " + createdAt + "</p>"
            + "</div>"
            + "<hr style=\"margin:30px 0;border:none;border-top:1px solid #ddd;\">"
            + "<p style=\"color:#666;font-size:12px;\">Smart-PFE - Academic Project Management System</p>"
            + "</div></body></html>";
    }

    /** Mirror of getProjectCreatedOwnerTemplate() */
    private static String buildOwnerCreatedTemplate(Project project, User owner) {
        return "<html><body style=\"font-family:Arial,sans-serif;padding:20px;background-color:#f5f5f5;\">"
            + "<div style=\"max-width:600px;margin:0 auto;background:white;padding:30px;border-radius:10px;\">"
            + "<h2 style=\"color:#28a745;\">✅ Project Created Successfully</h2>"
            + "<p>Hello <strong>" + esc(owner != null ? owner.getName() : "Student") + "</strong>,</p>"
            + "<p>Your project has been created successfully!</p>"
            + "<div style=\"background:#f8f9fa;padding:20px;border-left:4px solid #28a745;margin:20px 0;\">"
            + "<h3 style=\"margin-top:0;\">" + esc(project.getTitle()) + "</h3>"
            + "<p><strong>Type:</strong> "       + esc(project.getProjectType()) + "</p>"
            + "<p><strong>Status:</strong> "     + esc(project.getStatus())      + "</p>"
            + "<p><strong>Supervisor:</strong> " + esc(project.getSupervisorName() != null
                                                        ? project.getSupervisorName() : "Not assigned") + "</p>"
            + "</div>"
            + "<hr style=\"margin:30px 0;border:none;border-top:1px solid #ddd;\">"
            + "<p style=\"color:#666;font-size:12px;\">Smart-PFE - Academic Project Management System</p>"
            + "</div></body></html>";
    }

    /** Mirror of getProjectStatusChangedTemplate() */
    private static String buildStatusChangedTemplate(Project project, User owner, String oldStatus) {
        return "<html><body style=\"font-family:Arial,sans-serif;padding:20px;background-color:#f5f5f5;\">"
            + "<div style=\"max-width:600px;margin:0 auto;background:white;padding:30px;border-radius:10px;\">"
            + "<h2 style=\"color:#17a2b8;\">🔄 Project Status Updated</h2>"
            + "<p>Hello <strong>" + esc(owner != null ? owner.getName() : "Student") + "</strong>,</p>"
            + "<p>The status of your project has been updated:</p>"
            + "<div style=\"background:#f8f9fa;padding:20px;border-left:4px solid #17a2b8;margin:20px 0;\">"
            + "<h3 style=\"margin-top:0;\">" + esc(project.getTitle()) + "</h3>"
            + "<p><strong>Old Status:</strong> <span style=\"color:#dc3545;\">" + esc(oldStatus)             + "</span></p>"
            + "<p><strong>New Status:</strong> <span style=\"color:#28a745;\">" + esc(project.getStatus())   + "</span></p>"
            + "</div>"
            + "<hr style=\"margin:30px 0;border:none;border-top:1px solid #ddd;\">"
            + "<p style=\"color:#666;font-size:12px;\">Smart-PFE - Academic Project Management System</p>"
            + "</div></body></html>";
    }

    /** Escape HTML special characters to prevent injection */
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
