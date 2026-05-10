# SmartPFE — Desktop Application (JavaFX)

> Plateforme de gestion de projets de fin d'études (PFE) développée avec **JavaFX 21** et **MySQL**.  
> Elle permet aux étudiants, superviseurs et établissements de collaborer efficacement sur des projets académiques.

---

## 📋 Table des matières

- [Description du projet](#-description-du-projet)
- [Technologies utilisées](#-technologies-utilisées)
- [Prérequis](#-prérequis)
- [Installation et lancement](#-installation-et-lancement)
- [Fonctionnalités](#-fonctionnalités)
  - [Gestion des Utilisateurs](#-gestion-des-utilisateurs)
  - [Gestion des Projets](#-gestion-des-projets)
  - [Gestion des Sprints](#-gestion-des-sprints)
  - [Gestion des Tâches](#-gestion-des-tâches)
  - [Gestion des Meetings](#-gestion-des-meetings)
  - [Gestion des Offres / Candidatures](#-gestion-des-offres--candidatures)
  - [Export PDF](#-export-pdf)
  - [Statistiques](#-statistiques)
- [Architecture du projet](#-architecture-du-projet)
- [Rôles utilisateurs](#-rôles-utilisateurs)
- [Mots clés](#-mots-clés)

---

## 📌 Description du projet

**SmartPFE** est une application desktop développée en Java avec JavaFX. Elle centralise la gestion complète des projets de fin d'études :

- Création et suivi de projets (individuels ou en équipe)
- Planification agile avec sprints et tâches
- Planification de réunions entre étudiants et superviseurs
- Système de candidatures pour rejoindre des projets
- Export de rapports PDF via l'API **PDFShift**
- Notifications par email automatiques
- Assignation intelligente des tâches via **IA (Groq / LLaMA)**

---

## 🛠 Technologies utilisées

| Technologie | Version | Usage |
|---|---|---|
| Java | 17 | Langage principal |
| JavaFX | 21.0.1 | Interface graphique |
| MySQL | 8+ | Base de données |
| iText PDF | 5.5.13.3 | Génération PDF locale |
| PDFShift API | v3 | Génération PDF via API externe |
| Groq API (LLaMA 3.3) | — | Assignation IA des tâches |
| JavaMail | 1.6.2 | Notifications email |
| BCrypt | 0.4 | Hashage des mots de passe |
| Maven | 3.x | Gestion des dépendances |

---

## ⚙️ Prérequis

- **JDK 17** ou supérieur
- **Maven 3.6+**
- **MySQL 8+** avec une base de données configurée
- Connexion internet (pour PDFShift API et Groq API)

---

## 🚀 Installation et lancement

```bash
# 1. Cloner le dépôt
git clone https://github.com/ranimouhibi/Esprit-PIDEV-3A48-2526-SmartPFE.git
cd Esprit-PIDEV-3A48-2526-SmartPFE

# 2. Configurer la base de données
# Modifier src/main/java/org/example/config/DatabaseConfig.java
# avec vos identifiants MySQL

# 3. Configurer les variables d'environnement (optionnel)
set GROQ_API_KEY=votre_cle_groq

# 4. Lancer l'application
mvn javafx:run
```

---

## ✨ Fonctionnalités

### 👤 Gestion des Utilisateurs

- **Inscription / Connexion** sécurisée avec hashage BCrypt
- **Rôles** : Étudiant, Superviseur, Établissement, Admin
- **Profil utilisateur** : modification des informations personnelles, compétences (`skills`)
- **Gestion admin** : liste complète des utilisateurs, activation/désactivation
- **Authentification par session** via `SessionManager`

---

### 📁 Gestion des Projets

- **Création** de projets individuels ou en équipe
- **Code de jointure** unique généré automatiquement pour rejoindre un projet
- **Filtres avancés** : par type (individual/team), statut, recherche textuelle
- **Tri** : par date, titre, statut (croissant/décroissant)
- **Statuts** : `created` → `waiting_supervisor` → `supervised` → `in_progress` → `finished` → `archived`
- **Statistiques** : total, individuels, équipe, en cours, terminés
- **Assignation d'un superviseur** au projet
- **Export PDF** de la liste des projets (iText)
- **Suppression** avec cascade (sprints, tâches, membres, documents, commentaires)

---

### 🔄 Gestion des Sprints

- **Création / Édition / Suppression** de sprints liés à un projet
- **Statuts** : `planned` → `active` → `closed`
- **Validation des dates** : détection automatique des chevauchements de sprints
- **Fermeture automatique** : quand un sprint passe à `closed`, toutes ses tâches passent automatiquement à `done`
- **Vue calendrier** : visualisation mensuelle des sprints avec navigation mois par mois
- **Détail sprint** : popup avec projet, dates, objectif et liste des tâches
- **Statistiques** : bouton dédié avec vue détaillée par sprint
- **Export PDF hiérarchique** via PDFShift API (Projet → Sprint → Tâches)
- **Notifications email** automatiques aux membres lors de création/modification
- **Restrictions** : les sprints `closed` ne peuvent pas être édités ni recevoir de nouvelles tâches

---

### ✅ Gestion des Tâches

- **Création / Édition / Suppression** de tâches dans un sprint
- **Statuts progressifs** : `todo` → `in_progress` → `done` (transitions contrôlées)
- **Priorités** : `low`, `medium`, `high`, `critical`
- **Assignation** à un étudiant membre du projet
  - Une fois assignée, la tâche **ne peut plus être réassignée** à un autre étudiant
  - Les tâches `done` **ne peuvent plus être éditées** (bouton Edit masqué)
- **Assignation IA** : suggestion automatique du meilleur étudiant via Groq/LLaMA en analysant les compétences
- **Filtres** : par statut, priorité, recherche textuelle
- **Notifications email** : l'étudiant assigné est notifié, le superviseur est notifié des mises à jour
- **Story points** et description optionnelle
- **Indicateur de blocage** avec description du bloqueur

---

### 📅 Gestion des Meetings

- **Création / Modification / Suppression** de réunions
- **Types** : `weekly`, `sprint_review`, `retrospective`, `planning`, `other`
- **Statuts** : `scheduled`, `completed`, `cancelled`
- **Mode** : présentiel (avec lieu) ou en ligne (avec lien)
- **Durée** configurable (15 min à 8h)
- **Agenda et notes** de réunion
- **Association** à un projet spécifique
- **Vue tableau** avec tri et sélection

---

### 🎯 Gestion des Offres / Candidatures

- **Parcourir les offres** de projets disponibles (étudiants)
- **Candidater** à un projet via code de jointure
- **Statuts de candidature** : `pending`, `approved`, `rejected`
- **Gestion des membres** : le propriétaire du projet gère les demandes d'adhésion
- **Vue dédiée** pour les étudiants (`StudentProjects.fxml`)

---

### 📄 Export PDF

- **Export hiérarchique** (Projet → Sprint → Tâches) via **PDFShift API** (HTML → PDF)
  - Rapport Superviseur : tous les projets supervisés avec leurs sprints et tâches
  - Rapport Étudiant : projets auxquels l'étudiant participe
- **Export Projets** : liste tabulaire des projets (iText)
- **Export Commentaires** : liste des commentaires par projet (iText)
- **Export Documents** : liste des documents attachés (iText)
- **Export Sprint Stats** : rapport de statistiques (iText)
- Accessible depuis la vue **Sprints** via le bouton "Export PDF"

---

### 📊 Statistiques

- **KPI cards** : Total Sprints, Sprints Actifs, Tâches Done, En Cours, À Faire
- **Breakdown des tâches** : barres de progression par statut (Done / In Progress / To Do)
- **Breakdown des sprints** : barres de progression par statut (Planned / Active / Closed)
- **Progression par sprint** : barre de complétion colorée + mini-compteurs
- **Liste des tâches** collapsible par sprint avec statut et priorité

---

## 🏗 Architecture du projet

```
src/
├── main/
│   ├── java/org/example/
│   │   ├── config/          # Configuration base de données
│   │   ├── controller/      # Contrôleurs JavaFX (UI logic)
│   │   ├── dao/             # Data Access Objects (SQL)
│   │   ├── model/           # Modèles de données (POJO)
│   │   └── util/            # Services utilitaires
│   │       ├── PDFExporter.java      # Export PDF (iText + PDFShift)
│   │       ├── EmailService.java     # Notifications email
│   │       ├── TaskAssignmentAI.java # Assignation IA (Groq)
│   │       ├── AIAssignmentDialog.java
│   │       ├── SessionManager.java   # Gestion de session
│   │       └── NavigationUtil.java   # Navigation entre vues
│   └── resources/
│       ├── fxml/            # Fichiers de vues JavaFX
│       └── css/             # Styles de l'application
```

---

## 👥 Rôles utilisateurs

| Rôle | Accès |
|---|---|
| **Étudiant** | Ses projets, ses tâches assignées, sprints, meetings, candidatures |
| **Superviseur** | Projets supervisés, toutes les tâches, sprints, commentaires, meetings |
| **Établissement** | Dashboard établissement, gestion des offres |
| **Admin** | Accès complet à toutes les données |

---

## 🔑 Mots clés

`JavaFX` · `Java 17` · `MySQL` · `Agile` · `Scrum` · `Sprint` · `Gestion de projet` · `PFE` · `PDF Export` · `PDFShift API` · `iText` · `Intelligence Artificielle` · `Groq` · `LLaMA` · `Email Notification` · `JavaMail` · `BCrypt` · `MVC` · `DAO Pattern` · `Desktop Application` · `FXML` · `CSS JavaFX`
