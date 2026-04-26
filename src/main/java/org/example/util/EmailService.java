package org.example.util;

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
