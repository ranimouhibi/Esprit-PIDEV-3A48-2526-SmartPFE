package org.example.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Gestionnaire de configuration centralisé pour SmartPFE
 * Charge les paramètres depuis config.properties
 */
public class AppConfig {
    
    private static final Properties properties = new Properties();
    private static boolean loaded = false;
    
    static {
        loadConfig();
    }
    
    /**
     * Charge le fichier de configuration
     */
    private static void loadConfig() {
        if (loaded) return;
        
        try (InputStream input = AppConfig.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            
            if (input == null) {
                System.err.println("⚠️ config.properties not found. Using default values.");
                setDefaultValues();
                return;
            }
            
            properties.load(input);
            loaded = true;
            System.out.println("✅ Configuration loaded successfully");
            
        } catch (IOException e) {
            System.err.println("❌ Error loading config.properties: " + e.getMessage());
            setDefaultValues();
        }
    }
    
    /**
     * Définit les valeurs par défaut si le fichier n'existe pas
     */
    private static void setDefaultValues() {
        properties.setProperty("mail.from", "knanimalek18@gmail.com");
        properties.setProperty("mail.password", "YOUR_GMAIL_APP_PASSWORD");
        properties.setProperty("mail.smtp.host", "smtp.gmail.com");
        properties.setProperty("mail.smtp.port", "587");
        properties.setProperty("gemini.api.key", "VOTRE_CLE_GEMINI_ICI");
        properties.setProperty("gemini.test.mode", "true");
        loaded = true;
    }
    
    /**
     * Récupère une propriété
     */
    public static String get(String key) {
        return properties.getProperty(key);
    }
    
    /**
     * Récupère une propriété avec valeur par défaut
     */
    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Récupère une propriété booléenne
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Méthodes d'accès rapide pour les configurations courantes
    // ═══════════════════════════════════════════════════════════════════
    
    // Email
    public static String getMailFrom() {
        return get("mail.from");
    }
    
    public static String getMailPassword() {
        return get("mail.password");
    }
    
    public static String getMailSmtpHost() {
        return get("mail.smtp.host");
    }
    
    public static String getMailSmtpPort() {
        return get("mail.smtp.port");
    }
    
    // Gemini AI
    public static String getGeminiApiKey() {
        return get("gemini.api.key");
    }
    
    public static boolean isGeminiTestMode() {
        return getBoolean("gemini.test.mode", true);
    }
    
    public static boolean isGeminiConfigured() {
        String key = getGeminiApiKey();
        return key != null && !key.equals("VOTRE_CLE_GEMINI_ICI");
    }
}
