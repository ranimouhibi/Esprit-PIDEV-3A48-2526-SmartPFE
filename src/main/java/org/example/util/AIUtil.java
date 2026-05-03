package org.example.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.example.config.AppConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Utilitaire pour les fonctionnalités d'Intelligence Artificielle
 * Utilise l'API Google Gemini (100% GRATUIT)
 * 
 * Configuration dans src/main/resources/config.properties:
 * - gemini.api.key=VOTRE_CLE_ICI
 * - gemini.test.mode=true/false
 */
public class AIUtil {
    
    // ═══════════════════════════════════════════════════════════════════
    // CONFIGURATION - Chargée depuis config.properties
    // ═══════════════════════════════════════════════════════════════════
    // Pour configurer:
    // 1. Allez sur : https://makersuite.google.com/app/apikey
    // 2. Cliquez sur "Create API Key"
    // 3. Ouvrez src/main/resources/config.properties
    // 4. Ajoutez votre clé: gemini.api.key=VOTRE_CLE_ICI
    // 5. Changez: gemini.test.mode=false
    //
    // ✅ 100% GRATUIT - Aucune carte bancaire requise
    // ✅ Quota généreux : 60 requêtes/minute
    private static final String GEMINI_API_KEY = AppConfig.getGeminiApiKey();
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + GEMINI_API_KEY;
    
    // Mode test: chargé depuis config.properties
    private static final boolean TEST_MODE = AppConfig.isGeminiTestMode();
    
    /**
     * Obtenir des suggestions pour améliorer un projet
     */
    public static String getProjectSuggestions(String projectTitle, String description) {
        if (TEST_MODE) {
            return getTestSuggestions(projectTitle, description);
        }
        
        String prompt = "En tant qu'expert en gestion de projets académiques, donne 3 suggestions concrètes pour améliorer ce projet:\n\n" +
                "Titre: " + projectTitle + "\n" +
                "Description: " + description + "\n\n" +
                "Format: Liste numérotée avec des suggestions courtes et actionnables.";
        
        return callGemini(prompt);
    }
    
    /**
     * Résumer le contenu d'un document
     */
    public static String summarizeDocument(String documentContent) {
        if (TEST_MODE) {
            return getTestSummary(documentContent);
        }
        
        String prompt = "Résume ce document en 3-4 phrases claires et concises:\n\n" + documentContent;
        
        return callGemini(prompt);
    }
    
    /**
     * Extraire les mots-clés d'un document
     */
    public static String extractKeywords(String documentContent) {
        if (TEST_MODE) {
            return "projet, développement, application, gestion, innovation";
        }
        
        String prompt = "Extrais les 5 mots-clés principaux de ce texte (séparés par des virgules):\n\n" + documentContent;
        
        return callGemini(prompt);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // MODE TEST - Réponses simulées sans API
    // ═══════════════════════════════════════════════════════════════════════
    
    private static String getTestSuggestions(String projectTitle, String description) {
        // Analyser le titre et la description pour générer des suggestions personnalisées
        String lowerTitle = projectTitle.toLowerCase();
        String lowerDesc = description != null ? description.toLowerCase() : "";
        String combined = lowerTitle + " " + lowerDesc;
        
        StringBuilder suggestions = new StringBuilder();
        suggestions.append("🤖 AI Suggestions for: ").append(projectTitle).append("\n\n");
        
        // Suggestions based on detected project type
        if (combined.contains("hotel") || combined.contains("booking") || combined.contains("reservation")) {
            suggestions.append("🏨 Suggestions for your Hotel project:\n\n");
            suggestions.append("1. 🔐 Reservation System\n");
            suggestions.append("   • Implement a real-time availability calendar\n");
            suggestions.append("   • Add a secure payment system (Stripe, PayPal)\n");
            suggestions.append("   • Handle automatic cancellations and refunds\n\n");
            suggestions.append("2. 👥 Customer Management\n");
            suggestions.append("   • Create customer profiles with stay history\n");
            suggestions.append("   • Loyalty program and reward points\n");
            suggestions.append("   • Email/SMS notifications for confirmations\n\n");
            suggestions.append("3. 🏢 Hotel Administration\n");
            suggestions.append("   • Dashboard to manage rooms and rates\n");
            suggestions.append("   • Occupancy statistics and revenue reports\n");
            suggestions.append("   • Staff management and scheduling\n\n");
        } else if (combined.contains("e-commerce") || combined.contains("shop") || combined.contains("store")) {
            suggestions.append("🛒 Suggestions for your E-commerce project:\n\n");
            suggestions.append("1. 🛍️ Product Catalog\n");
            suggestions.append("   • Advanced category and filter system\n");
            suggestions.append("   • Smart search with suggestions\n");
            suggestions.append("   • Real-time inventory management\n\n");
            suggestions.append("2. 💳 Purchase Process\n");
            suggestions.append("   • Persistent multi-session cart\n");
            suggestions.append("   • One-page checkout\n");
            suggestions.append("   • Multiple payment options\n\n");
            suggestions.append("3. 📦 Order Management\n");
            suggestions.append("   • Real-time delivery tracking\n");
            suggestions.append("   • Returns and exchanges system\n");
            suggestions.append("   • Automatic notifications\n\n");
        } else if (combined.contains("school") || combined.contains("education") || combined.contains("learning") || combined.contains("student")) {
            suggestions.append("🎓 Suggestions for your Education project:\n\n");
            suggestions.append("1. 📚 Course Management\n");
            suggestions.append("   • Online course platform\n");
            suggestions.append("   • Assignments and evaluation system\n");
            suggestions.append("   • Student progress tracking\n\n");
            suggestions.append("2. 👨‍🏫 Teacher Space\n");
            suggestions.append("   • Content creation and sharing\n");
            suggestions.append("   • Automatic MCQ correction\n");
            suggestions.append("   • Communication with students\n\n");
            suggestions.append("3. 📊 Analytics and Reports\n");
            suggestions.append("   • Performance statistics\n");
            suggestions.append("   • Attendance reports\n");
            suggestions.append("   • Customized dashboards\n\n");
        } else if (combined.contains("social") || combined.contains("chat") || combined.contains("messaging")) {
            suggestions.append("💬 Suggestions for your Social project:\n\n");
            suggestions.append("1. 👥 Social Features\n");
            suggestions.append("   • Friends and followers system\n");
            suggestions.append("   • Personalized news feed\n");
            suggestions.append("   • Multimedia content sharing\n\n");
            suggestions.append("2. 💬 Messaging\n");
            suggestions.append("   • Real-time chat (WebSocket)\n");
            suggestions.append("   • Private messages and groups\n");
            suggestions.append("   • Push notifications\n\n");
            suggestions.append("3. 🔒 Privacy\n");
            suggestions.append("   • Granular privacy settings\n");
            suggestions.append("   • Content moderation\n");
            suggestions.append("   • Reporting and blocking\n\n");
        } else {
            suggestions.append("💡 General suggestions for: ").append(projectTitle).append("\n\n");
            suggestions.append("1. 📚 Architecture & Documentation\n");
            suggestions.append("   • Document your application architecture\n");
            suggestions.append("   • Create a detailed README with screenshots\n");
            suggestions.append("   • Add UML diagrams (use case, class diagrams)\n\n");
            suggestions.append("2. 🧪 Testing & Quality\n");
            suggestions.append("   • Unit tests for business logic\n");
            suggestions.append("   • Integration tests for APIs\n");
            suggestions.append("   • UI tests for critical features\n\n");
            suggestions.append("3. 🎨 User Experience\n");
            suggestions.append("   • Intuitive and modern interface\n");
            suggestions.append("   • Responsive design (mobile, tablet, desktop)\n");
            suggestions.append("   • Accessibility (WCAG 2.1)\n\n");
            suggestions.append("4. 🔒 Security\n");
            suggestions.append("   • User input validation\n");
            suggestions.append("   • SQL injection protection\n");
            suggestions.append("   • Secure authentication (JWT, OAuth)\n\n");
            suggestions.append("5. ⚡ Performance\n");
            suggestions.append("   • Database query optimization\n");
            suggestions.append("   • Caching (Redis, Memcached)\n");
            suggestions.append("   • Lazy loading of resources\n\n");
        }
        
        suggestions.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        suggestions.append("💡 Test Mode Active\n");
        suggestions.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        suggestions.append("These suggestions are generated in test mode based on your project title.\n\n");
        suggestions.append("To get personalized AI suggestions:\n");
        suggestions.append("1. Get a FREE API key at https://makersuite.google.com/app/apikey\n");
        suggestions.append("2. Open src/main/java/org/example/util/AIUtil.java\n");
        suggestions.append("3. Line 19: Add your Gemini API key\n");
        suggestions.append("4. Line 23: Change TEST_MODE = false\n");
        
        return suggestions.toString();
    }
    
    private static String getTestSummary(String documentContent) {
        int wordCount = documentContent.split("\\s+").length;
        String preview = documentContent.length() > 150 
            ? documentContent.substring(0, 150) + "..." 
            : documentContent;
        
        // Analyser le contenu pour générer un résumé personnalisé
        String lowerContent = documentContent.toLowerCase();
        
        StringBuilder summary = new StringBuilder();
        summary.append("📝 AI Summary (Test Mode)\n\n");
        
        // Detect document type
        if (lowerContent.contains("hotel") || lowerContent.contains("booking") || lowerContent.contains("reservation")) {
            summary.append("📄 Document Type: Hotel Project\n\n");
            summary.append("This document (").append(wordCount).append(" words) presents a hotel management system. ");
            summary.append("It covers reservation features, room management, and administration. ");
            summary.append("The project aims to modernize management processes and improve the customer experience.\n\n");
        } else if (lowerContent.contains("e-commerce") || lowerContent.contains("shop") || lowerContent.contains("product")) {
            summary.append("📄 Document Type: E-commerce Project\n\n");
            summary.append("This document (").append(wordCount).append(" words) describes an e-commerce platform. ");
            summary.append("It covers product management, the purchase process, and the payment system. ");
            summary.append("The goal is to create a smooth and secure shopping experience.\n\n");
        } else if (lowerContent.contains("school") || lowerContent.contains("education") || lowerContent.contains("student")) {
            summary.append("📄 Document Type: Education Project\n\n");
            summary.append("This document (").append(wordCount).append(" words) presents an educational solution. ");
            summary.append("It covers course management, student tracking, and teaching tools. ");
            summary.append("The project aims to facilitate learning and collaboration.\n\n");
        } else if (lowerContent.contains("pdf") || lowerContent.contains("report") || lowerContent.contains("document")) {
            summary.append("📄 Document Type: Report / Documentation\n\n");
            summary.append("This document (").append(wordCount).append(" words) contains technical and functional information. ");
            summary.append("It presents the architecture, features, and specifications of the project. ");
            summary.append("The content is structured to facilitate understanding and implementation.\n\n");
        } else {
            summary.append("📄 Document Analysis\n\n");
            summary.append("This document contains approximately ").append(wordCount).append(" words. ");
            summary.append("It covers topics related to project development and application management. ");
            summary.append("The content addresses important technical and functional aspects for project delivery.\n\n");
        }
        
        summary.append("📋 Content Preview:\n");
        summary.append(preview).append("\n\n");
        
        summary.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        summary.append("💡 Test Mode Active\n");
        summary.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        summary.append("This summary is generated in test mode based on content analysis.\n\n");
        summary.append("To get a detailed personalized AI summary:\n");
        summary.append("1. Get a FREE API key at https://makersuite.google.com/app/apikey\n");
        summary.append("2. Open src/main/java/org/example/util/AIUtil.java\n");
        summary.append("3. Line 19: Add your Gemini API key\n");
        summary.append("4. Line 23: Change TEST_MODE = false\n");
        
        return summary.toString();
    }
    
    /**
     * Générer une description de projet à partir d'un titre
     */
    public static String generateProjectDescription(String projectTitle) {
        String prompt = "Génère une description professionnelle de 2-3 phrases pour un projet académique intitulé: " + projectTitle;
        
        return callGemini(prompt);
    }
    
    /**
     * Corriger la grammaire et l'orthographe d'un texte
     */
    public static String correctText(String text) {
        String prompt = "Corrige la grammaire et l'orthographe de ce texte sans changer le sens:\n\n" + text;
        
        return callGemini(prompt);
    }
    
    /**
     * Appeler l'API Google Gemini
     */
    private static String callGemini(String prompt) {
        try {
            // Construire le corps de la requête JSON pour Gemini
            JsonObject requestBody = new JsonObject();
            
            // Ajouter le contenu
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", prompt);
            parts.add(part);
            
            content.add("parts", parts);
            contents.add(content);
            requestBody.add("contents", contents);
            
            // Créer la requête HTTP
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();
            
            // Envoyer la requête
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Parser la réponse
            if (response.statusCode() == 200) {
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                return jsonResponse.getAsJsonArray("candidates")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("content")
                        .getAsJsonArray("parts")
                        .get(0).getAsJsonObject()
                        .get("text").getAsString().trim();
            } else {
                System.err.println("❌ Erreur API Gemini: " + response.statusCode());
                System.err.println("Réponse: " + response.body());
                return "Erreur lors de la génération de contenu AI.";
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'appel à l'API Gemini: " + e.getMessage());
            e.printStackTrace();
            return "Impossible de générer du contenu AI pour le moment.";
        }
    }
    
    /**
     * Vérifier si l'API est configurée
     */
    public static boolean isConfigured() {
        return AppConfig.isGeminiConfigured();
    }
}
