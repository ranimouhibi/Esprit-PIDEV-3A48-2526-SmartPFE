package org.example.test;

import org.example.util.MailUtil;
import org.example.model.Project;
import org.example.model.User;

import java.time.LocalDateTime;

/**
 * Test simple pour vérifier que le mailing fonctionne
 * Exécutez cette classe pour envoyer un email de test
 */
public class TestMailing {

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║   TEST MAILING - SmartPFE                                  ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Créer un projet de test
        Project testProject = new Project();
        testProject.setId(999);
        testProject.setTitle("Projet Test Mailing");
        testProject.setDescription("Ceci est un test d'envoi d'email depuis SmartPFE Desktop");
        testProject.setProjectType("PFE");
        testProject.setStatus("En cours");
        testProject.setSupervisorName("Professeur Test");
        testProject.setSupervisorEmail("knanimalek18@gmail.com"); // Ton email pour recevoir le test
        testProject.setCreatedAt(LocalDateTime.now());

        // Créer un utilisateur de test (étudiant)
        User testStudent = new User();
        testStudent.setId(1);
        testStudent.setName("Malek Knani");
        testStudent.setEmail("knanimalek18@gmail.com"); // Ton email pour recevoir le test
        testStudent.setRole("student");

        System.out.println("📧 Envoi d'un email de test...");
        System.out.println();
        System.out.println("De      : knanimalek18@gmail.com");
        System.out.println("À       : " + testStudent.getEmail());
        System.out.println("Sujet   : Projet créé - " + testProject.getTitle());
        System.out.println();

        try {
            // Envoyer l'email de création de projet
            MailUtil.sendProjectCreatedEmail(testProject, testStudent);
            
            System.out.println("✅ Email envoyé avec succès !");
            System.out.println();
            System.out.println("📬 Vérifiez votre boîte email : knanimalek18@gmail.com");
            System.out.println("   Vous devriez recevoir 2 emails :");
            System.out.println("   1. Email au superviseur : 'New Project Created'");
            System.out.println("   2. Email à l'étudiant : 'Your Project Has Been Created'");
            System.out.println();
            System.out.println("⚠️  Si vous ne recevez pas l'email :");
            System.out.println("   - Vérifiez votre dossier SPAM");
            System.out.println("   - Vérifiez que le mot de passe dans config.properties est correct");
            System.out.println("   - Vérifiez votre connexion internet");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email :");
            System.err.println("   " + e.getMessage());
            e.printStackTrace();
            System.out.println();
            System.out.println("🔧 Solutions possibles :");
            System.out.println("   1. Vérifiez le mot de passe dans config.properties");
            System.out.println("   2. Vérifiez votre connexion internet");
            System.out.println("   3. Vérifiez que le compte Gmail autorise les apps moins sécurisées");
        }

        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║   Test terminé                                             ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }
}
