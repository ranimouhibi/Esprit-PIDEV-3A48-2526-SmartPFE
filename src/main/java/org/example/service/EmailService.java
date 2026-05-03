package org.example.service;

import org.example.model.Meeting;
import org.example.model.User;

import javax.mail.*;
import javax.mail.internet.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Service d'envoi d'emails pour les notifications de meetings.
 * Utilise Gmail SMTP avec mot de passe d'application.
 *
 * Configuration via variables d'environnement :
 *   MAIL_USERNAME=votre.email@gmail.com
 *   MAIL_APP_PASSWORD=xxxx xxxx xxxx xxxx
 */
public class EmailService {

    private static final Logger LOG = Logger.getLogger(EmailService.class.getName());
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String username;
    private final String password;
    private final boolean enabled;

    public EmailService() {
        this.username = System.getenv("MAIL_USERNAME");
        this.password = System.getenv("MAIL_APP_PASSWORD");
        this.enabled = username != null && !username.isBlank() && password != null && !password.isBlank();
        if (!enabled) {
            LOG.warning("EmailService désactivé : MAIL_USERNAME ou MAIL_APP_PASSWORD non configuré.");
        }
    }

    public boolean isEnabled() { return enabled; }

    // ─── 3 méthodes publiques ────────────────────────────────────────────────

    public void sendMeetingInvitation(Meeting meeting, List<User> participants) {
        if (!enabled) return;
        String subject = "New Meeting Invitation: " + safe(meeting.getProjectTitle());
        for (User user : participants) {
            try {
                String html = buildInvitationHtml(meeting, user);
                sendEmail(user.getEmail(), subject, html);
            } catch (Exception e) {
                LOG.severe("Erreur envoi invitation à " + user.getEmail() + " : " + e.getMessage());
            }
        }
    }

    public void sendMeetingUpdate(Meeting meeting, List<User> participants) {
        if (!enabled) return;
        String subject = "Meeting Updated: " + safe(meeting.getProjectTitle());
        for (User user : participants) {
            try {
                String html = buildUpdateHtml(meeting, user);
                sendEmail(user.getEmail(), subject, html);
            } catch (Exception e) {
                LOG.severe("Erreur envoi update à " + user.getEmail() + " : " + e.getMessage());
            }
        }
    }

    public void sendFollowUpEmail(Meeting meeting, List<User> participants, String body) {
        if (!enabled) return;
        String subject = "Suivi Meeting: " + safe(meeting.getProjectTitle());
        for (User user : participants) {
            try {
                String html = buildFollowUpHtml(user, body);
                sendEmail(user.getEmail(), subject, html);
            } catch (Exception e) {
                LOG.severe("Erreur envoi follow-up à " + user.getEmail() + " : " + e.getMessage());
            }
        }
    }

    private String buildFollowUpHtml(User user, String body) {
        String bodyHtml = body.replace("\n", "<br>");
        return "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;background:#f5f5f5;margin:0;padding:20px;'>"
            + "<div style='max-width:600px;margin:0 auto;background:white;border-radius:12px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,0.1);'>"
            + "<div style='background:linear-gradient(135deg,#a12c2f,#c0392b);padding:32px 24px;text-align:center;'>"
            + "<h1 style='color:white;margin:0;font-size:22px;'>📋 Email de Suivi Meeting</h1>"
            + "</div>"
            + "<div style='padding:24px;'>"
            + "<p style='font-size:16px;color:#333;'>Bonjour <strong>" + safe(user.getName()) + "</strong>,</p>"
            + "<div style='background:#f8f9fa;border-radius:8px;padding:16px;margin:16px 0;color:#333;font-size:14px;line-height:1.6;'>"
            + bodyHtml
            + "</div>"
            + "</div>"
            + "<div style='background:#f8f9fa;padding:16px 24px;text-align:center;border-top:1px solid #e9ecef;'>"
            + "<p style='color:#999;font-size:12px;margin:0;'>Smart PFE — Automated Follow-up</p>"
            + "</div>"
            + "</div></body></html>";
    }

    public void sendMeetingCancellation(Meeting meeting, List<User> participants) {
        if (!enabled) return;
        String subject = "Meeting Cancelled: " + safe(meeting.getProjectTitle());
        for (User user : participants) {
            try {
                String html = buildCancellationHtml(meeting, user);
                sendEmail(user.getEmail(), subject, html);
            } catch (Exception e) {
                LOG.severe("Erreur envoi annulation à " + user.getEmail() + " : " + e.getMessage());
            }
        }
    }

    // ─── Envoi SMTP ──────────────────────────────────────────────────────────

    private void sendEmail(String to, String subject, String htmlBody) throws MessagingException, java.io.UnsupportedEncodingException {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username, "Smart PFE", "UTF-8"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setContent(htmlBody, "text/html; charset=UTF-8");
        Transport.send(message);
    }

    // ─── Templates HTML ──────────────────────────────────────────────────────

    private String buildInvitationHtml(Meeting meeting, User user) {
        String headerGradient = "linear-gradient(135deg, #a12c2f, #FF6B6B)";
        String title = "📅 Meeting Invitation";
        String intro = "You have been invited to a new meeting.";
        return buildEmailHtml(meeting, user, headerGradient, title, intro, true);
    }

    private String buildUpdateHtml(Meeting meeting, User user) {
        String headerGradient = "linear-gradient(135deg, #f5a425, #FFD700)";
        String title = "🔄 Meeting Updated";
        String intro = "A meeting you are participating in has been updated.";
        return buildEmailHtml(meeting, user, headerGradient, title, intro, true);
    }

    private String buildCancellationHtml(Meeting meeting, User user) {
        String headerGradient = "linear-gradient(135deg, #dc3545, #c82333)";
        String title = "❌ Meeting Cancelled";
        String intro = "A meeting you were participating in has been cancelled.";
        return buildEmailHtml(meeting, user, headerGradient, title, intro, false);
    }

    private String buildEmailHtml(Meeting meeting, User user, String headerGradient,
                                   String title, String intro, boolean showJitsiLink) {
        String dateStr = meeting.getScheduledDate() != null ? meeting.getScheduledDate().format(FMT) : "—";
        String timeStr = meeting.getScheduledDate() != null
                ? meeting.getScheduledDate().format(DateTimeFormatter.ofPattern("HH:mm")) : "—";

        StringBuilder jitsiBlock = new StringBuilder();
        if (showJitsiLink && "ONLINE".equals(meeting.getMeetingType())
                && meeting.getMeetingLink() != null && !meeting.getMeetingLink().isBlank()) {
            jitsiBlock.append("<div style='background: linear-gradient(135deg,#a12c2f,#FF6B6B); border-radius:8px; padding:16px; margin:16px 0; text-align:center;'>")
                    .append("<p style='color:white; margin:0 0 10px 0; font-weight:bold;'>🎥 Online Meeting Link</p>")
                    .append("<a href='").append(meeting.getMeetingLink()).append("' ")
                    .append("style='background:white; color:#a12c2f; padding:10px 24px; border-radius:6px; text-decoration:none; font-weight:bold; display:inline-block;'>")
                    .append("🎥 Join Online Meeting</a>")
                    .append("</div>");
        }

        return "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif; background:#f5f5f5; margin:0; padding:20px;'>"
                + "<div style='max-width:600px; margin:0 auto; background:white; border-radius:12px; overflow:hidden; box-shadow:0 4px 12px rgba(0,0,0,0.1);'>"
                // Header
                + "<div style='background:" + headerGradient + "; padding:32px 24px; text-align:center;'>"
                + "<h1 style='color:white; margin:0; font-size:24px;'>" + title + "</h1>"
                + "</div>"
                // Body
                + "<div style='padding:24px;'>"
                + "<p style='font-size:16px; color:#333;'>Hello <strong>" + safe(user.getName()) + "</strong>,</p>"
                + "<p style='color:#555;'>" + intro + "</p>"
                // Details card
                + "<div style='background:#f8f9fa; border-radius:8px; padding:16px; margin:16px 0;'>"
                + "<table style='width:100%; border-collapse:collapse;'>"
                + row("📁 Project", safe(meeting.getProjectTitle()))
                + row("📋 Type", safe(meeting.getMeetingType()))
                + row("📅 Date", dateStr)
                + row("🕐 Time", timeStr)
                + row("⏱️ Duration", meeting.getDuration() + " minutes")
                + row("📍 Location", safe(meeting.getLocation()))
                + "</table>"
                + "</div>"
                + jitsiBlock
                + "<div style='text-align:center; margin:20px 0;'>"
                + "<a href='http://localhost:8080/meetings' style='background:#a12c2f; color:white; padding:12px 28px; border-radius:8px; text-decoration:none; font-weight:bold; display:inline-block;'>View Meeting Details</a>"
                + "</div>"
                + "</div>"
                // Footer
                + "<div style='background:#f8f9fa; padding:16px 24px; text-align:center; border-top:1px solid #e9ecef;'>"
                + "<p style='color:#999; font-size:12px; margin:0;'>This is an automated message from Smart PFE.</p>"
                + "</div>"
                + "</div></body></html>";
    }

    private String row(String label, String value) {
        return "<tr><td style='padding:6px 0; color:#666; font-size:13px; width:40%;'>" + label + "</td>"
                + "<td style='padding:6px 0; color:#333; font-size:13px; font-weight:bold;'>" + value + "</td></tr>";
    }

    private String safe(String s) { return s != null ? s : "—"; }
}
