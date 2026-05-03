-- ============================================================
-- SmartPFE — Scénario Démo Complet
-- Base: symfony_db  |  mysql -u root symfony_db < scenario_demo.sql
-- Mot de passe pour tous les comptes: Test1234
-- ============================================================

USE symfony_db;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- NETTOYAGE
-- ============================================================
DELETE FROM meeting_participants WHERE meeting_id IN
  (SELECT id FROM meetings WHERE project_id IN
    (SELECT id FROM projects WHERE title LIKE '%SmartPFE Demo%'));
DELETE FROM meetings WHERE project_id IN
  (SELECT id FROM projects WHERE title LIKE '%SmartPFE Demo%');
DELETE FROM tasks WHERE project_id IN
  (SELECT id FROM projects WHERE title LIKE '%SmartPFE Demo%');
DELETE FROM sprints WHERE project_id IN
  (SELECT id FROM projects WHERE title LIKE '%SmartPFE Demo%');
DELETE FROM projects WHERE title LIKE '%SmartPFE Demo%';
DELETE FROM candidatures WHERE offer_id IN
  (SELECT id FROM project_offers WHERE title LIKE '%Demo%');
DELETE FROM project_offers WHERE title LIKE '%Demo%';
DELETE FROM users WHERE email IN ('techcorp@demo.tn','ahmed@demo.tn','prof.karim@demo.tn');
DELETE FROM establishments WHERE email = 'techcorp@demo.tn';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 1. ÉTABLISSEMENT
-- ============================================================
INSERT INTO establishments (name, email, address, phone, is_approved, created_at, updated_at)
VALUES ('TechCorp Esprit', 'techcorp@demo.tn', 'Rue de la Technologie, Ariana, Tunisie', '+21620000001', 1, NOW(), NOW());

SET @estab_id = LAST_INSERT_ID();

-- ============================================================
-- 2. UTILISATEURS  (BCrypt hash de "Test1234")
-- ============================================================
INSERT INTO users (email, password, role, name, phone, is_active, is_verified, establishment_id, created_at, updated_at)
VALUES
('techcorp@demo.tn',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LPVyc5AlL2S',
 'establishment', 'TechCorp Esprit', '+21620000001', 1, 1, @estab_id, NOW(), NOW()),
('ahmed@demo.tn',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LPVyc5AlL2S',
 'student', 'Ahmed Ben Ali', '+21620000002', 1, 1, @estab_id, NOW(), NOW()),
('prof.karim@demo.tn',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LPVyc5AlL2S',
 'supervisor', 'Prof. Karim Mansour', '+21620000003', 1, 1, NULL, NOW(), NOW());

SET @est_id  = (SELECT id FROM users WHERE email = 'techcorp@demo.tn');
SET @stu_id  = (SELECT id FROM users WHERE email = 'ahmed@demo.tn');
SET @sup_id  = (SELECT id FROM users WHERE email = 'prof.karim@demo.tn');

-- ============================================================
-- 3. OFFRES
-- ============================================================
INSERT INTO project_offers (establishment_id, title, description, objectives, required_skills, max_candidates, deadline, status, created_at)
VALUES
(@estab_id,
 'Full-Stack Developer Internship Demo',
 'Develop a web application using Spring Boot and React for managing academic projects.',
 'Learn microservices architecture, REST APIs, CI/CD pipelines.',
 'Java, Spring Boot, React, MySQL, Git',
 5, DATE_ADD(CURDATE(), INTERVAL 30 DAY), 'open', NOW()),
(@estab_id,
 'Data Science Internship Demo',
 'Work on machine learning models for student performance prediction.',
 'Apply ML algorithms, data visualization, Python data stack.',
 'Python, Pandas, Scikit-learn, TensorFlow, SQL',
 3, DATE_ADD(CURDATE(), INTERVAL 45 DAY), 'open', NOW()),
(@estab_id,
 'Mobile Developer Internship Demo',
 'Build a cross-platform mobile app for campus services.',
 'Develop Flutter app, integrate REST APIs, publish to stores.',
 'Flutter, Dart, Firebase, REST APIs',
 4, DATE_ADD(CURDATE(), INTERVAL 20 DAY), 'open', NOW());

SET @offer1_id = (SELECT id FROM project_offers WHERE title = 'Full-Stack Developer Internship Demo');
SET @offer2_id = (SELECT id FROM project_offers WHERE title = 'Data Science Internship Demo');

-- ============================================================
-- 4. CANDIDATURES
-- ============================================================
INSERT INTO candidatures (offer_id, student_id, motivation_letter, cv_path, portfolio_url, status, feedback, created_at, updated_at)
VALUES
(@offer1_id, @stu_id,
 'Je suis passionné par le développement Full-Stack. J''ai réalisé plusieurs projets avec Spring Boot et React durant ma formation à Esprit. Je maîtrise les architectures microservices et les bonnes pratiques DevOps. Je suis très motivé à rejoindre TechCorp pour mettre en pratique mes compétences sur un projet réel.',
 'ahmed_cv.pdf', 'https://ahmed-portfolio.github.io',
 'accepted', 'Excellent profile! Strong technical background. Welcome to the team!',
 DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
(@offer2_id, @stu_id,
 'Je suis très intéressé par la Data Science et le Machine Learning. J''ai suivi plusieurs cours sur Python, Pandas et Scikit-learn. Mon projet de fin d''études porte sur la prédiction des résultats académiques.',
 'ahmed_cv_ds.pdf', 'https://ahmed-portfolio.github.io/datascience',
 'pending', NULL,
 DATE_SUB(NOW(), INTERVAL 2 DAY), NOW());

-- ============================================================
-- 5. PROJET
-- ============================================================
INSERT INTO projects (owner_id, supervisor_id, establishment_id, title, description, project_type, join_code, status, created_at, updated_at)
VALUES
(@stu_id, @sup_id, @estab_id,
 'SmartPFE Demo — Full-Stack Web App',
 'Development of a web application for managing academic PFE projects. Built with Spring Boot backend and React frontend, featuring authentication, project management, and real-time notifications.',
 'PFE', 'DEMO2026', 'active',
 DATE_SUB(NOW(), INTERVAL 4 DAY), NOW());

SET @proj_id = LAST_INSERT_ID();

-- ============================================================
-- 6. SPRINTS
-- ============================================================
INSERT INTO sprints (project_id, name, goal, sprint_number, status, start_date, end_date, created_at)
VALUES
(@proj_id, 'Sprint 1 — Setup & Architecture',
 'Initialize project structure, configure CI/CD, design database schema.',
 1, 'closed',
 DATE_SUB(CURDATE(), INTERVAL 14 DAY), DATE_SUB(CURDATE(), INTERVAL 1 DAY),
 DATE_SUB(NOW(), INTERVAL 14 DAY)),
(@proj_id, 'Sprint 2 — Core Features',
 'Implement authentication, user management, and project CRUD operations.',
 2, 'active',
 CURDATE(), DATE_ADD(CURDATE(), INTERVAL 13 DAY), NOW()),
(@proj_id, 'Sprint 3 — Advanced Features',
 'Add notifications, email service, AI matching, and reporting.',
 3, 'planned',
 DATE_ADD(CURDATE(), INTERVAL 14 DAY), DATE_ADD(CURDATE(), INTERVAL 27 DAY), NOW());

SET @sprint1_id = (SELECT id FROM sprints WHERE project_id = @proj_id AND sprint_number = 1);
SET @sprint2_id = (SELECT id FROM sprints WHERE project_id = @proj_id AND sprint_number = 2);
SET @sprint3_id = (SELECT id FROM sprints WHERE project_id = @proj_id AND sprint_number = 3);

-- ============================================================
-- 7. TASKS
-- ============================================================
INSERT INTO tasks (project_id, sprint_id, title, description, status, priority, story_points, assigned_to_id, deadline, created_at)
VALUES
-- Sprint 1 (closed)
(@proj_id, @sprint1_id, 'Setup Spring Boot project',
 'Initialize Spring Boot project with Maven, configure application.properties, add core dependencies.',
 'done', 'high', 3, @stu_id, DATE_SUB(CURDATE(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY)),
(@proj_id, @sprint1_id, 'Design database schema',
 'Create ERD diagram, write SQL migration scripts for all entities.',
 'done', 'high', 5, @stu_id, DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY)),
(@proj_id, @sprint1_id, 'Configure CI/CD pipeline',
 'Setup GitHub Actions for automated build and test on every push.',
 'done', 'medium', 3, @stu_id, DATE_SUB(CURDATE(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY)),
(@proj_id, @sprint1_id, 'Setup React project with Vite',
 'Initialize React frontend, configure routing, add Tailwind CSS.',
 'done', 'medium', 2, @stu_id, DATE_SUB(CURDATE(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY)),
-- Sprint 2 (active)
(@proj_id, @sprint2_id, 'Implement JWT authentication',
 'Create login/register endpoints, JWT token generation and validation middleware.',
 'in_progress', 'high', 8, @stu_id, DATE_ADD(CURDATE(), INTERVAL 3 DAY), NOW()),
(@proj_id, @sprint2_id, 'Create REST API for projects',
 'CRUD endpoints for projects: GET, POST, PUT, DELETE.',
 'in_progress', 'high', 5, @stu_id, DATE_ADD(CURDATE(), INTERVAL 5 DAY), NOW()),
(@proj_id, @sprint2_id, 'Build user profile page',
 'React component for viewing and editing user profile, avatar upload.',
 'todo', 'medium', 3, @stu_id, DATE_ADD(CURDATE(), INTERVAL 8 DAY), NOW()),
(@proj_id, @sprint2_id, 'Write unit tests for auth service',
 'JUnit 5 tests for authentication service, mock database calls.',
 'todo', 'low', 3, @stu_id, DATE_ADD(CURDATE(), INTERVAL 10 DAY), NOW()),
-- Sprint 3 (planned)
(@proj_id, @sprint3_id, 'Implement email notification service',
 'JavaMail integration for sending notifications on key events.',
 'todo', 'high', 5, @stu_id, DATE_ADD(CURDATE(), INTERVAL 16 DAY), NOW()),
(@proj_id, @sprint3_id, 'Add AI matching algorithm',
 'HuggingFace API integration for matching students to offers based on skills.',
 'todo', 'medium', 8, @stu_id, DATE_ADD(CURDATE(), INTERVAL 20 DAY), NOW()),
(@proj_id, @sprint3_id, 'Generate PDF reports',
 'iText PDF generation for project reports and candidature summaries.',
 'todo', 'low', 3, @stu_id, DATE_ADD(CURDATE(), INTERVAL 25 DAY), NOW());

-- ============================================================
-- 8. MEETINGS
-- ============================================================
INSERT INTO meetings (project_id, meeting_type, scheduled_date, duration, location, agenda, status, reschedule_count, meeting_link, created_at)
VALUES
(@proj_id, 'ONLINE', DATE_SUB(NOW(), INTERVAL 7 DAY), 60, 'Jitsi Meet',
 '1. Project kickoff\n2. Sprint 1 planning\n3. Technical stack review\n4. Q&A',
 'COMPLETED', 0, 'https://meet.jit.si/SmartPFE-Demo-Kickoff', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(@proj_id, 'ONLINE', DATE_ADD(NOW(), INTERVAL 2 DAY), 90, 'Jitsi Meet',
 '1. Sprint 1 review\n2. Sprint 2 planning\n3. Demo of completed features\n4. Feedback session',
 'SCHEDULED', 0, 'https://meet.jit.si/SmartPFE-Demo-Sprint2', NOW()),
(@proj_id, 'IN_PERSON', DATE_ADD(NOW(), INTERVAL 7 DAY), 120, 'Salle B204 — Esprit',
 '1. Mid-project review\n2. Technical challenges\n3. Supervisor feedback\n4. Next steps',
 'SCHEDULED', 0, NULL, NOW());

SET @meeting1_id = (SELECT id FROM meetings WHERE project_id = @proj_id AND status = 'COMPLETED' LIMIT 1);
SET @meeting2_id = (SELECT id FROM meetings WHERE project_id = @proj_id AND status = 'SCHEDULED' ORDER BY scheduled_date ASC LIMIT 1);

-- ============================================================
-- 9. PARTICIPANTS MEETINGS
-- ============================================================
INSERT INTO meeting_participants (meeting_id, user_id) VALUES
(@meeting1_id, @stu_id), (@meeting1_id, @sup_id),
(@meeting2_id, @stu_id), (@meeting2_id, @sup_id);

-- ============================================================
-- RÉSUMÉ
-- ============================================================
SELECT '=== SCENARIO DEMO INSERTED ===' AS '';
SELECT 'techcorp@demo.tn  | Test1234 | establishment' AS credentials;
SELECT 'ahmed@demo.tn     | Test1234 | student'       AS credentials;
SELECT 'prof.karim@demo.tn| Test1234 | supervisor'    AS credentials;
SELECT CONCAT('Offers: ',   COUNT(*)) FROM project_offers WHERE title LIKE '%Demo%';
SELECT CONCAT('Candidatures: ', COUNT(*)) FROM candidatures WHERE offer_id IN (SELECT id FROM project_offers WHERE title LIKE '%Demo%');
SELECT CONCAT('Project: ', title) FROM projects WHERE id = @proj_id;
SELECT CONCAT('Sprints: ', COUNT(*)) FROM sprints WHERE project_id = @proj_id;
SELECT CONCAT('Tasks: ',   COUNT(*)) FROM tasks   WHERE project_id = @proj_id;
SELECT CONCAT('Meetings: ',COUNT(*)) FROM meetings WHERE project_id = @proj_id;
