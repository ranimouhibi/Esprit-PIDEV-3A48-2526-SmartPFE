-- Migration SQL pour le module Meeting
-- À exécuter sur la base de données symfony_db

-- Table meeting_participants (ManyToMany Meeting <-> User)
CREATE TABLE IF NOT EXISTS meeting_participants (
    meeting_id INT NOT NULL,
    user_id INT NOT NULL,
    PRIMARY KEY (meeting_id, user_id),
    FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_meeting (meeting_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Vérifier que la table meetings a bien les colonnes nécessaires
-- Si elles n'existent pas, les ajouter :

-- Colonne meeting_link (pour Jitsi)
ALTER TABLE meetings 
ADD COLUMN IF NOT EXISTS meeting_link VARCHAR(500) NULL AFTER location;

-- Colonne ai_summary (pour OpenAI)
ALTER TABLE meetings 
ADD COLUMN IF NOT EXISTS ai_summary TEXT NULL AFTER raw_content;

-- Colonne raw_content (si elle n'existe pas déjà)
ALTER TABLE meetings 
ADD COLUMN IF NOT EXISTS raw_content TEXT NULL AFTER agenda;

-- Vérifier la structure de meeting_reports
-- Colonne raw_meeting_text
ALTER TABLE meeting_reports 
ADD COLUMN IF NOT EXISTS raw_meeting_text TEXT NULL AFTER next_steps;

-- Index pour améliorer les performances
CREATE INDEX IF NOT EXISTS idx_meetings_scheduled_date ON meetings(scheduled_date);
CREATE INDEX IF NOT EXISTS idx_meetings_status ON meetings(status);
CREATE INDEX IF NOT EXISTS idx_meetings_project ON meetings(project_id);
CREATE INDEX IF NOT EXISTS idx_meeting_reports_meeting ON meeting_reports(meeting_id);
CREATE INDEX IF NOT EXISTS idx_meeting_reports_status ON meeting_reports(status);

-- Afficher la structure finale
SHOW CREATE TABLE meetings;
SHOW CREATE TABLE meeting_participants;
SHOW CREATE TABLE meeting_reports;
