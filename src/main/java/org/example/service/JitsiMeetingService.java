package org.example.service;

import org.example.model.Meeting;

import java.security.SecureRandom;

/**
 * Génère automatiquement des liens Jitsi Meet pour les meetings ONLINE.
 * Jitsi Meet est 100% gratuit, aucune API key requise.
 */
public class JitsiMeetingService {

    private static final String JITSI_BASE_URL = "https://meet.jit.si/";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String HEX_CHARS = "0123456789abcdef";

    public boolean isConfigured() {
        return true; // Toujours disponible
    }

    /**
     * Génère un lien Jitsi Meet pour un meeting.
     * Format : https://meet.jit.si/{projectTitle}-{meetingId}-{randomHex8}
     */
    public String generateMeetLink(Meeting meeting) {
        String projectTitle = meeting.getProjectTitle() != null ? meeting.getProjectTitle() : "meeting";
        String slug = slugify(projectTitle, 30);
        String randomHex = generateRandomHex(8);
        String roomName = slug + "-" + meeting.getId() + "-" + randomHex;
        return JITSI_BASE_URL + roomName;
    }

    /**
     * Génère un lien Jitsi sans ID (avant sauvegarde en base).
     */
    public String generatePreviewLink(String projectTitle) {
        String slug = slugify(projectTitle != null ? projectTitle : "meeting", 30);
        String randomHex = generateRandomHex(8);
        return JITSI_BASE_URL + slug + "-" + randomHex;
    }

    private String slugify(String input, int maxLength) {
        String lower = input.toLowerCase();
        String slug = lower.replaceAll("[^a-z0-9]+", "-");
        slug = slug.replaceAll("^-+|-+$", "");
        if (slug.isEmpty()) slug = "meeting";
        if (slug.length() > maxLength) slug = slug.substring(0, maxLength).replaceAll("-+$", "");
        return slug;
    }

    private String generateRandomHex(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(HEX_CHARS.charAt(RANDOM.nextInt(HEX_CHARS.length())));
        }
        return sb.toString();
    }
}
