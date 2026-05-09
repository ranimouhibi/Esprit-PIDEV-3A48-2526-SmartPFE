package org.example.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Persists a "remember me" session to ~/.smartpfe/session.dat
 *
 * File format (AES-encrypted, then Base64):
 *   userId:token
 *
 * The token is also stored in the DB (users.remember_token).
 * On launch we read the file, decrypt it, look up the token in DB — if it
 * matches we auto-login without asking for credentials.
 */
public class LocalSessionStore {

    private static final Path SESSION_DIR  = Path.of(System.getProperty("user.home"), ".smartpfe");
    private static final Path SESSION_FILE = SESSION_DIR.resolve("session.dat");
    private static final Path PIN_FILE     = SESSION_DIR.resolve("pins.dat");
    private static final Path LAST_USER_FILE = SESSION_DIR.resolve("last_user.dat");

    // 16-byte AES key — fixed per application (obfuscation, not true security)
    private static final byte[] AES_KEY = "SmartPFE2026Key!".getBytes();

    // ── Token ─────────────────────────────────────────────────────────────────

    /** Generate a cryptographically random token string. */
    public static String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Persist userId + token to disk (encrypted). */
    public static void saveSession(int userId, String token) {
        try {
            Files.createDirectories(SESSION_DIR);
            String plain = userId + ":" + token;
            String encrypted = encrypt(plain);
            Files.writeString(SESSION_FILE, encrypted);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Last user (for PIN — persists independently of remember-me) ───────────

    /** Always save the last logged-in userId so PIN can work without a session token. */
    public static void saveLastUser(int userId) {
        try {
            Files.createDirectories(SESSION_DIR);
            Files.writeString(LAST_USER_FILE, String.valueOf(userId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Returns the last logged-in userId, or -1 if not found. */
    public static int loadLastUserId() {
        try {
            if (!Files.exists(LAST_USER_FILE)) return -1;
            String content = Files.readString(LAST_USER_FILE).trim();
            return Integer.parseInt(content);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Load saved session from disk.
     * @return int[]{userId} and sets loadedToken field, or null if none / corrupt.
     */
    public static SavedSession loadSession() {
        try {
            if (!Files.exists(SESSION_FILE)) return null;
            String encrypted = Files.readString(SESSION_FILE).trim();
            if (encrypted.isEmpty()) return null;
            String plain = decrypt(encrypted);
            String[] parts = plain.split(":", 2);
            if (parts.length != 2) return null;
            return new SavedSession(Integer.parseInt(parts[0]), parts[1]);
        } catch (Exception e) {
            clearSession(); // corrupt file — delete it
            return null;
        }
    }

    /** Delete the saved session (called on logout). */
    public static void clearSession() {
        try { Files.deleteIfExists(SESSION_FILE); } catch (Exception ignored) {}
    }

    // ── PIN ───────────────────────────────────────────────────────────────────

    /** Save a bcrypt-hashed PIN for a user. */
    public static void savePin(int userId, String hashedPin) {
        try {
            Files.createDirectories(SESSION_DIR);
            // Format: userId=hashedPin  (one entry per line)
            java.util.Properties props = loadPinProps();
            props.setProperty(String.valueOf(userId), hashedPin);
            try (OutputStream os = Files.newOutputStream(PIN_FILE)) {
                props.store(os, "SmartPFE PIN store");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Return the stored bcrypt hash for a user's PIN, or null if not set. */
    public static String loadPinHash(int userId) {
        try {
            java.util.Properties props = loadPinProps();
            return props.getProperty(String.valueOf(userId));
        } catch (Exception e) {
            return null;
        }
    }

    /** Remove PIN for a user. */
    public static void clearPin(int userId) {
        try {
            java.util.Properties props = loadPinProps();
            props.remove(String.valueOf(userId));
            try (OutputStream os = Files.newOutputStream(PIN_FILE)) {
                props.store(os, "SmartPFE PIN store");
            }
        } catch (Exception ignored) {}
    }

    private static java.util.Properties loadPinProps() throws IOException {
        java.util.Properties props = new java.util.Properties();
        if (Files.exists(PIN_FILE)) {
            try (InputStream is = Files.newInputStream(PIN_FILE)) {
                props.load(is);
            }
        }
        return props;
    }

    // ── AES helpers ───────────────────────────────────────────────────────────

    private static String encrypt(String plain) throws Exception {
        SecretKey key = new SecretKeySpec(AES_KEY, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(plain.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private static String decrypt(String encoded) throws Exception {
        SecretKey key = new SecretKeySpec(AES_KEY, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encoded));
        return new String(decrypted);
    }

    // ── Data class ────────────────────────────────────────────────────────────

    public record SavedSession(int userId, String token) {}
}
