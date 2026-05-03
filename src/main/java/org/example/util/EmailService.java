package org.example.util;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailService {

    // Configure these with your SMTP credentials
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "465"; // Changed from 587 to 465 (SSL)
    private static final String FROM_EMAIL = "manefbelkadhi800@gmail.com";
    private static final String FROM_PASSWORD = "upulidjovtmuigvf";

    public static void sendAsync(String toEmail, String subject, String htmlBody) {
        if (toEmail == null || toEmail.isBlank()) {
            System.err.println("[EMAIL] Skipped: empty recipient");
            return;
        }
        System.out.println("[EMAIL] Queuing email to: " + toEmail + " | Subject: " + subject);
        new Thread(() -> {
            try {
                send(toEmail, subject, htmlBody);
                System.out.println("[EMAIL] ✓ Sent successfully to: " + toEmail);
            } catch (Exception e) {
                System.err.println("[EMAIL] ✗ FAILED to " + toEmail);
                System.err.println("[EMAIL] Cause: " + e.getClass().getSimpleName() + " — " + e.getMessage());
                Throwable cause = e.getCause();
                while (cause != null) {
                    System.err.println("[EMAIL] Caused by: " + cause.getClass().getSimpleName() + " — " + cause.getMessage());
                    cause = cause.getCause();
                }
            }
        }, "email-sender").start();
    }

    private static void send(String to, String subject, String htmlBody) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth",                "true");
        props.put("mail.smtp.host",                SMTP_HOST);
        props.put("mail.smtp.port",                SMTP_PORT);
        
        // Use SSL instead of STARTTLS for port 465
        props.put("mail.smtp.ssl.enable",          "true");
        props.put("mail.smtp.ssl.trust",           SMTP_HOST);
        props.put("mail.smtp.ssl.protocols",       "TLSv1.2 TLSv1.3");
        props.put("mail.smtp.ssl.checkserveridentity", "true");
        
        props.put("mail.smtp.connectiontimeout",   "30000");
        props.put("mail.smtp.timeout",             "30000");
        props.put("mail.smtp.writetimeout",        "30000");
        props.put("mail.transport.protocol",       "smtp");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
            }
        });
        session.setDebug(true); // prints full SMTP conversation to stdout

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(FROM_EMAIL));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        msg.setSubject(subject);
        msg.setContent(htmlBody, "text/html; charset=utf-8");
        Transport.send(msg);
    }

    public static String sprintSupervisorTemplate(String action, String sprintName, String projectName,
                                                    String startDate, String endDate,
                                                    String studentName, String supervisorName) {
        return "<div style='font-family:Segoe UI,Arial,sans-serif;max-width:600px;margin:auto'>"
            + "<div style='background:#1a1a2e;padding:20px;border-radius:8px 8px 0 0'>"
            + "<h2 style='color:white;margin:0'>SmartPFE</h2></div>"
            + "<div style='background:#f9fafb;padding:24px;border-radius:0 0 8px 8px'>"
            + "<p>Hello <b>" + supervisorName + "</b>,</p>"
            + "<p>Your student <b>" + studentName + "</b> has <b>" + action + "</b> a sprint in project <b>" + projectName + "</b>.</p>"
            + "<table style='border-collapse:collapse;width:100%'>"
            + "<tr><td style='padding:6px;color:#6b7280'>Sprint</td><td style='padding:6px'>" + sprintName + "</td></tr>"
            + "<tr><td style='padding:6px;color:#6b7280'>Start</td><td style='padding:6px'>" + startDate + "</td></tr>"
            + "<tr><td style='padding:6px;color:#6b7280'>End</td><td style='padding:6px'>" + endDate + "</td></tr>"
            + "</table>"
            + "<p style='color:#6b7280;font-size:12px;margin-top:24px'>SmartPFE — Academic Project Management</p>"
            + "</div></div>";
    }

    public static String sprintTemplate(String action, String sprintName, String projectName,
                                         String startDate, String endDate, String studentName) {
        return "<div style='font-family:Segoe UI,Arial,sans-serif;max-width:600px;margin:auto'>"
            + "<div style='background:#1a1a2e;padding:20px;border-radius:8px 8px 0 0'>"
            + "<h2 style='color:white;margin:0'>SmartPFE</h2></div>"
            + "<div style='background:#f9fafb;padding:24px;border-radius:0 0 8px 8px'>"
            + "<p>Hello <b>" + studentName + "</b>,</p>"
            + "<p>Sprint <b>" + sprintName + "</b> in project <b>" + projectName + "</b> has been <b>" + action + "</b>.</p>"
            + "<table style='border-collapse:collapse;width:100%'>"
            + "<tr><td style='padding:6px;color:#6b7280'>Start</td><td style='padding:6px'>" + startDate + "</td></tr>"
            + "<tr><td style='padding:6px;color:#6b7280'>End</td><td style='padding:6px'>" + endDate + "</td></tr>"
            + "</table>"
            + "<p style='color:#6b7280;font-size:12px;margin-top:24px'>SmartPFE — Academic Project Management</p>"
            + "</div></div>";
    }

    public static String taskSupervisorTemplate(String action, String taskTitle, String sprintName,
                                                  String priority, String studentName, String supervisorName) {
        return "<div style='font-family:Segoe UI,Arial,sans-serif;max-width:600px;margin:auto'>"
            + "<div style='background:#1a1a2e;padding:20px;border-radius:8px 8px 0 0'>"
            + "<h2 style='color:white;margin:0'>SmartPFE</h2></div>"
            + "<div style='background:#f9fafb;padding:24px;border-radius:0 0 8px 8px'>"
            + "<p>Hello <b>" + supervisorName + "</b>,</p>"
            + "<p>Your student <b>" + studentName + "</b> has <b>" + action + "</b> a task.</p>"
            + "<table style='border-collapse:collapse;width:100%'>"
            + "<tr><td style='padding:6px;color:#6b7280'>Task</td><td style='padding:6px'>" + taskTitle + "</td></tr>"
            + "<tr><td style='padding:6px;color:#6b7280'>Sprint</td><td style='padding:6px'>" + sprintName + "</td></tr>"
            + "<tr><td style='padding:6px;color:#6b7280'>Priority</td><td style='padding:6px'>" + priority + "</td></tr>"
            + "</table>"
            + "<p style='color:#6b7280;font-size:12px;margin-top:24px'>SmartPFE — Academic Project Management</p>"
            + "</div></div>";
    }

    public static String taskTemplate(String action, String taskTitle, String sprintName,
                                       String priority, String studentName) {
        return "<div style='font-family:Segoe UI,Arial,sans-serif;max-width:600px;margin:auto'>"
            + "<div style='background:#1a1a2e;padding:20px;border-radius:8px 8px 0 0'>"
            + "<h2 style='color:white;margin:0'>SmartPFE</h2></div>"
            + "<div style='background:#f9fafb;padding:24px;border-radius:0 0 8px 8px'>"
            + "<p>Hello <b>" + studentName + "</b>,</p>"
            + "<p>Task <b>" + taskTitle + "</b> has been <b>" + action + "</b>.</p>"
            + "<table style='border-collapse:collapse;width:100%'>"
            + "<tr><td style='padding:6px;color:#6b7280'>Sprint</td><td style='padding:6px'>" + sprintName + "</td></tr>"
            + "<tr><td style='padding:6px;color:#6b7280'>Priority</td><td style='padding:6px'>" + priority + "</td></tr>"
            + "</table>"
            + "<p style='color:#6b7280;font-size:12px;margin-top:24px'>SmartPFE — Academic Project Management</p>"
            + "</div></div>";
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/**
 * Sends emails via Gmail SMTP (TLS on port 587).
 *
 * Setup:
 *  1. Use a Gmail account.
 *  2. Enable 2-Step Verification on that account.
 *  3. Generate an App Password: Google Account → Security → App Passwords.
 *  4. Fill in SENDER_EMAIL and SENDER_APP_PASSWORD below.
 */
public class EmailService {

    // ── CONFIGURE THESE ──────────────────────────────────────────────────────
    private static final String SENDER_EMAIL        = "mabroukikacem10001@gmail.com";
    private static final String SENDER_APP_PASSWORD = "fcfg eodq irly qzxn";   // 16-char app password
    // ─────────────────────────────────────────────────────────────────────────

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int    SMTP_PORT = 587;

    /**
     * Sends a plain HTML email.
     *
     * @param toEmail   recipient address
     * @param subject   email subject
     * @param htmlBody  HTML content
     */
    public static void sendHtml(String toEmail, String subject, String htmlBody)
            throws MessagingException {

        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            String.valueOf(SMTP_PORT));
        props.put("mail.smtp.ssl.trust",       SMTP_HOST);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_APP_PASSWORD);
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SENDER_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject, "UTF-8");
        message.setContent(htmlBody, "text/html; charset=UTF-8");

        Transport.send(message);
    }

    /**
     * Sends the password reset email with a 6-digit OTP code.
     */
    public static void sendPasswordReset(String toEmail, String userName, String otp)
            throws MessagingException {

        String subject = "Smart-PFE — Password Reset Code";
        String body = """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 480px; margin: 0 auto; background: #f4f6fb; padding: 32px; border-radius: 16px;">
                  <div style="text-align: center; margin-bottom: 24px;">
                    <h1 style="color: #a12c2f; font-size: 28px; margin: 0;">Smart-PFE</h1>
                    <p style="color: #888; font-size: 13px; margin: 4px 0 0;">Password Reset Request</p>
                  </div>
                  <div style="background: white; border-radius: 12px; padding: 28px;">
                    <p style="color: #333; font-size: 14px;">Hello <strong>%s</strong>,</p>
                    <p style="color: #555; font-size: 13px; line-height: 1.6;">
                      We received a request to reset your password. Use the code below to proceed.
                      This code expires in <strong>15 minutes</strong>.
                    </p>
                    <div style="text-align: center; margin: 28px 0;">
                      <span style="display: inline-block; background: #a12c2f; color: white;
                                   font-size: 32px; font-weight: bold; letter-spacing: 10px;
                                   padding: 16px 32px; border-radius: 12px;">%s</span>
                    </div>
                    <p style="color: #888; font-size: 12px;">
                      If you did not request a password reset, you can safely ignore this email.
                    </p>
                  </div>
                  <p style="text-align: center; color: #bbb; font-size: 11px; margin-top: 20px;">
                    © 2026 Smart-PFE Desktop
                  </p>
                </div>
                """.formatted(userName, otp);

        sendHtml(toEmail, subject, body);
    }
}
