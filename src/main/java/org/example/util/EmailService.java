package org.example.util;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailService {

    // Configure these with your SMTP credentials
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
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
        props.put("mail.smtp.starttls.enable",     "true");
        props.put("mail.smtp.starttls.required",   "true");
        props.put("mail.smtp.host",                SMTP_HOST);
        props.put("mail.smtp.port",                SMTP_PORT);
        props.put("mail.smtp.ssl.trust",           SMTP_HOST);
        props.put("mail.smtp.connectiontimeout",   "10000");
        props.put("mail.smtp.timeout",             "10000");
        props.put("mail.smtp.writetimeout",        "10000");

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
    }
}
