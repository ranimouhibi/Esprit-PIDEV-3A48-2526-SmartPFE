package org.example.service;

import org.example.dao.CommentDAO;
import org.example.model.Comment;
import org.example.util.ProfanityFilter;
import org.example.util.SpeechUtil;
import org.example.util.TranslationUtil;

import java.util.List;

/**
 * Service pour la gestion des commentaires avec fonctionnalités avancées:
 * - Traduction (multi-langues)
 * - Insertion vocale (speech-to-text)
 * - Filtre de mots inappropriés
 */
public class CommentService {

    private final CommentDAO commentDAO;

    public CommentService() {
        this.commentDAO = new CommentDAO();
    }

    /**
     * Créer un commentaire avec filtre de profanité
     */
    public void createComment(Comment comment) throws Exception {
        // Validation
        if (comment.getContent() == null || comment.getContent().trim().isEmpty()) {
            throw new Exception("Le contenu du commentaire est obligatoire");
        }

        // Vérifier les mots inappropriés
        if (ProfanityFilter.containsProfanity(comment.getContent())) {
            throw new Exception("Le commentaire contient des mots inappropriés");
        }

        // Sauvegarder le commentaire
        commentDAO.save(comment);
    }

    /**
     * Créer un commentaire avec filtrage automatique des mots inappropriés
     */
    public void createCommentWithAutoFilter(Comment comment) throws Exception {
        // Validation
        if (comment.getContent() == null || comment.getContent().trim().isEmpty()) {
            throw new Exception("Le contenu du commentaire est obligatoire");
        }

        // Filtrer automatiquement les mots inappropriés
        String filteredContent = ProfanityFilter.filterProfanity(comment.getContent());
        comment.setContent(filteredContent);

        // Sauvegarder le commentaire
        commentDAO.save(comment);
    }

    /**
     * Traduire un commentaire
     */
    public String translateComment(String content, String targetLanguage) {
        try {
            return TranslationUtil.translate(content, targetLanguage);
        } catch (Exception e) {
            e.printStackTrace();
            return content; // Retourner le texte original en cas d'erreur
        }
    }

    /**
     * Créer un commentaire depuis l'audio (speech-to-text)
     */
    public Comment createCommentFromVoice(int projectId, int userId) throws Exception {
        // Enregistrer et transcrire l'audio
        String transcribedText = SpeechUtil.recordAndTranscribe();

        if (transcribedText == null || transcribedText.trim().isEmpty()) {
            throw new Exception("Impossible de transcrire l'audio");
        }

        // Vérifier les mots inappropriés
        if (ProfanityFilter.containsProfanity(transcribedText)) {
            throw new Exception("Le commentaire vocal contient des mots inappropriés");
        }

        // Créer le commentaire
        Comment comment = new Comment();
        comment.setCommentableType("Project");
        comment.setCommentableId(projectId);
        comment.setAuthorId(userId);
        comment.setContent(transcribedText);

        commentDAO.save(comment);
        return comment;
    }

    /**
     * Obtenir tous les commentaires d'un projet
     */
    public List<Comment> getCommentsByProject(int projectId) throws Exception {
        return commentDAO.findByProject(projectId);
    }

    /**
     * Obtenir tous les commentaires
     */
    public List<Comment> getAllComments() throws Exception {
        return commentDAO.findAll();
    }

    /**
     * Mettre à jour un commentaire
     */
    public void updateComment(Comment comment) throws Exception {
        // Validation
        if (comment.getId() <= 0) {
            throw new Exception("ID du commentaire invalide");
        }

        // Vérifier les mots inappropriés
        if (ProfanityFilter.containsProfanity(comment.getContent())) {
            throw new Exception("Le commentaire contient des mots inappropriés");
        }

        commentDAO.update(comment);
    }

    /**
     * Supprimer un commentaire
     */
    public void deleteComment(int commentId) throws Exception {
        commentDAO.delete(commentId);
    }

    /**
     * Vérifier si un texte contient des mots inappropriés
     */
    public boolean containsProfanity(String text) {
        return ProfanityFilter.containsProfanity(text);
    }

    /**
     * Filtrer les mots inappropriés d'un texte
     */
    public String filterProfanity(String text) {
        return ProfanityFilter.filterProfanity(text);
    }

    /**
     * Obtenir les langues disponibles pour la traduction
     */
    public String[] getAvailableLanguages() {
        return new String[]{"en", "fr", "es", "de", "it", "ar", "zh"};
    }

    /**
     * Obtenir les noms des langues
     */
    public String getLanguageName(String code) {
        switch (code) {
            case "en": return "English";
            case "fr": return "Français";
            case "es": return "Español";
            case "de": return "Deutsch";
            case "it": return "Italiano";
            case "ar": return "العربية";
            case "zh": return "中文";
            default: return code;
        }
    }
}
