package org.example.service;

import org.example.dao.DocumentDAO;
import org.example.model.Document;
import org.example.util.AIUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Service pour la gestion des documents avec fonctionnalités avancées:
 * - AI Résumé (analyse et résumé automatique du contenu)
 */
public class DocumentService {

    private final DocumentDAO documentDAO;

    public DocumentService() {
        this.documentDAO = new DocumentDAO();
    }

    /**
     * Créer un document
     */
    public void createDocument(Document document) throws Exception {
        // Validation
        if (document.getFilename() == null || document.getFilename().trim().isEmpty()) {
            throw new Exception("Le nom du fichier est obligatoire");
        }

        if (document.getFilePath() == null || document.getFilePath().trim().isEmpty()) {
            throw new Exception("Le chemin du fichier est obligatoire");
        }

        // Sauvegarder le document
        documentDAO.save(document);
    }

    /**
     * Créer un document avec génération automatique de résumé AI
     */
    public void createDocumentWithAISummary(Document document) throws Exception {
        // Validation
        if (document.getFilename() == null || document.getFilename().trim().isEmpty()) {
            throw new Exception("Le nom du fichier est obligatoire");
        }

        if (document.getFilePath() == null || document.getFilePath().trim().isEmpty()) {
            throw new Exception("Le chemin du fichier est obligatoire");
        }

        // Lire le contenu du document
        String content = readDocumentContent(document.getFilePath());

        // Générer le résumé avec AI
        String summary = AIUtil.summarizeDocument(content);
        document.setDescription(summary);

        // Sauvegarder le document
        documentDAO.save(document);
    }

    /**
     * Générer un résumé AI pour un document existant
     */
    public String generateAISummary(int documentId) throws Exception {
        // Récupérer le document
        Document document = documentDAO.findById(documentId);
        if (document == null) {
            throw new Exception("Document introuvable");
        }

        // Vérifier si le fichier existe
        File file = new File(document.getFilePath());
        if (!file.exists()) {
            // Si le fichier n'existe pas, générer un résumé basé sur les métadonnées
            String summary = AIUtil.summarizeDocument(
                "Document: " + document.getFilename() + "\n" +
                "Catégorie: " + (document.getCategory() != null ? document.getCategory() : "Non spécifiée") + "\n" +
                "Description: " + (document.getDescription() != null ? document.getDescription() : "Aucune description") + "\n" +
                "Type: " + (document.getFileType() != null ? document.getFileType() : "Inconnu")
            );
            
            // Mettre à jour la description du document
            document.setDescription(summary);
            documentDAO.update(document);
            
            return summary;
        }

        // Lire le contenu du document
        String content = readDocumentContent(document.getFilePath());

        // Générer le résumé avec AI
        String summary = AIUtil.summarizeDocument(content);

        // Mettre à jour la description du document
        document.setDescription(summary);
        documentDAO.update(document);

        return summary;
    }

    /**
     * Analyser un document avec AI (résumé + mots-clés)
     */
    public DocumentAnalysis analyzeDocument(int documentId) throws Exception {
        // Récupérer le document
        Document document = documentDAO.findById(documentId);
        if (document == null) {
            throw new Exception("Document introuvable");
        }

        // Lire le contenu du document
        String content = readDocumentContent(document.getFilePath());

        // Générer le résumé
        String summary = AIUtil.summarizeDocument(content);

        // Extraire les mots-clés
        String keywords = AIUtil.extractKeywords(content);

        // Retourner l'analyse
        return new DocumentAnalysis(summary, keywords, content.length());
    }

    /**
     * Lire le contenu d'un document
     */
    private String readDocumentContent(String filePath) throws IOException {
        File file = new File(filePath);
        
        if (!file.exists()) {
            throw new IOException("Le fichier n'existe pas: " + filePath);
        }

        // Lire le contenu du fichier
        byte[] bytes = Files.readAllBytes(file.toPath());
        String content = new String(bytes);

        // Limiter à 4000 caractères pour l'API AI
        if (content.length() > 4000) {
            content = content.substring(0, 4000);
        }

        return content;
    }

    /**
     * Obtenir tous les documents d'un projet
     */
    public List<Document> getDocumentsByProject(int projectId) throws Exception {
        return documentDAO.findByProject(projectId);
    }

    /**
     * Obtenir tous les documents
     */
    public List<Document> getAllDocuments() throws Exception {
        return documentDAO.findAll();
    }

    /**
     * Obtenir un document par ID
     */
    public Document getDocumentById(int id) throws Exception {
        return documentDAO.findById(id);
    }

    /**
     * Mettre à jour un document
     */
    public void updateDocument(Document document) throws Exception {
        // Validation
        if (document.getId() <= 0) {
            throw new Exception("ID du document invalide");
        }

        documentDAO.update(document);
    }

    /**
     * Supprimer un document
     */
    public void deleteDocument(int documentId) throws Exception {
        // Récupérer le document
        Document document = documentDAO.findById(documentId);
        if (document != null) {
            // Supprimer le fichier physique
            File file = new File(document.getFilePath());
            if (file.exists()) {
                file.delete();
            }
        }

        // Supprimer de la base de données
        documentDAO.delete(documentId);
    }

    /**
     * Classe interne pour l'analyse de document
     */
    public static class DocumentAnalysis {
        private String summary;
        private String keywords;
        private int contentLength;

        public DocumentAnalysis(String summary, String keywords, int contentLength) {
            this.summary = summary;
            this.keywords = keywords;
            this.contentLength = contentLength;
        }

        public String getSummary() { return summary; }
        public String getKeywords() { return keywords; }
        public int getContentLength() { return contentLength; }
    }
}
