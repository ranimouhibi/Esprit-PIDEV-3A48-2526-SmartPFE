package org.example.service;

import org.example.dao.ProjectDAO;
import org.example.model.Project;
import org.example.model.User;
import org.example.util.AIUtil;
import org.example.util.MailUtil;

import java.util.List;

/**
 * Service for project management with advanced features:
 * - Mailing (email notifications)
 * - AI Suggestions
 *
 * Email logic mirrors EmailService.php from the web part.
 * History is handled directly via created_at / updated_at fields in the projects table.
 */
public class ProjectService {

    private final ProjectDAO projectDAO;

    public ProjectService() {
        this.projectDAO = new ProjectDAO();
    }

    // ─────────────────────────────────────────────────────────────────────
    // CRUD
    // ─────────────────────────────────────────────────────────────────────

    public void createProject(Project project, User owner) throws Exception {
        if (project.getTitle() == null || project.getTitle().trim().isEmpty()) {
            throw new Exception("Project title is required");
        }
        projectDAO.save(project);

        // Re-fetch to get supervisor email populated by DAO
        Project saved = projectDAO.findById(project.getId());
        if (saved != null) {
            MailUtil.sendProjectCreatedEmail(saved, owner);
        }
    }

    public void updateProject(Project project, User owner, String oldStatus) throws Exception {
        if (project.getId() <= 0) {
            throw new Exception("Invalid project ID");
        }
        projectDAO.update(project);

        // Send status-change email if status changed
        if (oldStatus != null && !oldStatus.equals(project.getStatus())) {
            MailUtil.sendProjectStatusChangedEmail(project, owner, oldStatus);
        }
    }

    public void deleteProject(int projectId) throws Exception {
        projectDAO.delete(projectId);
    }

    public List<Project> getAllProjects() throws Exception {
        return projectDAO.findAll();
    }

    public Project getProjectById(int id) throws Exception {
        return projectDAO.findById(id);
    }

    // ─────────────────────────────────────────────────────────────────────
    // AI Suggestions
    // ─────────────────────────────────────────────────────────────────────

    public String getAISuggestions(String projectTitle, String description) {
        try {
            return AIUtil.getProjectSuggestions(projectTitle, description);
        } catch (Exception e) {
            e.printStackTrace();
            return "Unable to generate suggestions at the moment.";
        }
    }
}
