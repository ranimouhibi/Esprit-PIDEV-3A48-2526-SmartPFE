package org.example.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utilitaire pour filtrer les mots inappropriés
 */
public class ProfanityFilter {
    
    // Liste des mots inappropriés (à compléter selon vos besoins)
    private static final Set<String> PROFANITY_LIST = new HashSet<>(Arrays.asList(
        // Mots en français
        "merde", "putain", "connard", "salaud", "enculé", "con",
        // Mots en anglais
        "fuck", "shit", "damn", "bitch", "asshole",
        // Ajoutez d'autres mots selon vos besoins
        "idiot", "imbécile", "crétin"
    ));
    
    // Patterns pour détecter les variations (avec chiffres, caractères spéciaux, etc.)
    private static final Pattern[] PROFANITY_PATTERNS = {
        Pattern.compile("m[e3]rd[e3]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("put[a@]in", Pattern.CASE_INSENSITIVE),
        Pattern.compile("c[o0]nn[a@]rd", Pattern.CASE_INSENSITIVE),
        Pattern.compile("f[u*]ck", Pattern.CASE_INSENSITIVE),
        Pattern.compile("sh[i1]t", Pattern.CASE_INSENSITIVE),
    };
    
    /**
     * Vérifier si un texte contient des mots inappropriés
     * 
     * @param text Texte à vérifier
     * @return true si le texte contient des mots inappropriés
     */
    public static boolean containsProfanity(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        String lowerText = text.toLowerCase();
        
        // Vérifier les mots exacts
        for (String word : PROFANITY_LIST) {
            if (lowerText.contains(word)) {
                return true;
            }
        }
        
        // Vérifier les patterns (variations)
        for (Pattern pattern : PROFANITY_PATTERNS) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Filtrer les mots inappropriés en les remplaçant par des astérisques
     * 
     * @param text Texte à filtrer
     * @return Texte filtré
     */
    public static String filterProfanity(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        String filtered = text;
        
        // Remplacer les mots exacts
        for (String word : PROFANITY_LIST) {
            filtered = filtered.replaceAll("(?i)" + Pattern.quote(word), generateAsterisks(word.length()));
        }
        
        // Remplacer les patterns
        for (Pattern pattern : PROFANITY_PATTERNS) {
            java.util.regex.Matcher matcher = pattern.matcher(filtered);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(sb, generateAsterisks(matcher.group().length()));
            }
            matcher.appendTail(sb);
            filtered = sb.toString();
        }
        
        return filtered;
    }
    
    /**
     * Générer une chaîne d'astérisques
     */
    private static String generateAsterisks(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append("*");
        }
        return sb.toString();
    }
    
    /**
     * Obtenir le niveau de sévérité d'un texte (0-10)
     * 0 = aucun mot inapproprié
     * 10 = très inapproprié
     */
    public static int getSeverityLevel(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        
        int count = 0;
        String lowerText = text.toLowerCase();
        
        // Compter les occurrences
        for (String word : PROFANITY_LIST) {
            int index = 0;
            while ((index = lowerText.indexOf(word, index)) != -1) {
                count++;
                index += word.length();
            }
        }
        
        // Calculer le niveau (max 10)
        return Math.min(count * 2, 10);
    }
    
    /**
     * Ajouter un mot à la liste des mots inappropriés
     */
    public static void addProfanityWord(String word) {
        if (word != null && !word.trim().isEmpty()) {
            PROFANITY_LIST.add(word.toLowerCase());
        }
    }
    
    /**
     * Supprimer un mot de la liste des mots inappropriés
     */
    public static void removeProfanityWord(String word) {
        if (word != null) {
            PROFANITY_LIST.remove(word.toLowerCase());
        }
    }
    
    /**
     * Obtenir la liste des mots inappropriés détectés dans un texte
     */
    public static Set<String> getDetectedProfanity(String text) {
        Set<String> detected = new HashSet<>();
        
        if (text == null || text.trim().isEmpty()) {
            return detected;
        }
        
        String lowerText = text.toLowerCase();
        
        for (String word : PROFANITY_LIST) {
            if (lowerText.contains(word)) {
                detected.add(word);
            }
        }
        
        return detected;
    }
    
    /**
     * Nettoyer un texte en supprimant les mots inappropriés
     */
    public static String cleanText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        String cleaned = text;
        
        for (String word : PROFANITY_LIST) {
            cleaned = cleaned.replaceAll("(?i)\\b" + Pattern.quote(word) + "\\b", "");
        }
        
        // Nettoyer les espaces multiples
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        return cleaned;
    }
}
