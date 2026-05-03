# Module Meeting - Instructions d'installation

## 1. Migration de la base de données

Exécutez le script SQL `database_migration.sql` sur votre base de données `symfony_db` :

```bash
mysql -u root -p symfony_db < database_migration.sql
```

Ou via phpMyAdmin / MySQL Workbench, copiez-collez le contenu du fichier.

## 2. Variables d'environnement

Configurez les variables d'environnement suivantes (optionnelles) :

### Email (Gmail SMTP)
```
MAIL_USERNAME=votre.email@gmail.com
MAIL_APP_PASSWORD=xxxx xxxx xxxx xxxx
```

Pour obtenir un mot de passe d'application Gmail :
1. Allez sur https://myaccount.google.com/security
2. Activez la validation en 2 étapes
3. Générez un mot de passe d'application

### OpenAI (résumés IA)
```
OPENAI_API_KEY=sk-...
```

Obtenez votre clé sur https://platform.openai.com/api-keys

## 3. Fonctionnalités implémentées

### ✅ Génération automatique de liens Jitsi Meet
- Lien généré automatiquement pour les meetings ONLINE
- Format : `https://meet.jit.si/{projet}-{id}-{random}`
- 100% gratuit, aucune configuration requise

### ✅ Envoi d'emails HTML
- Invitation lors de la création
- Notification lors de la modification
- Annulation lors de la suppression
- Emails envoyés aux participants + superviseur du projet

### ✅ Calcul de distance
- Géolocalisation du navigateur
- Géocodage via OpenStreetMap Nominatim
- Calcul d'itinéraire via OSRM (voiture + à pied)
- Distance à vol d'oiseau (formule Haversine)
- Bouton "📏 Calculate Distance" dans le détail du meeting

### ✅ Calendrier interactif
- FullCalendar.js v5.11.3
- Vue mois / semaine / jour / liste
- Couleurs par statut (Pending/Confirmed/Cancelled)
- Filtrage par utilisateur connecté
- Bouton "📅 Calendrier" dans la toolbar

### ✅ Résumés IA (OpenAI GPT-3.5-turbo)
- Génération de résumé à partir du dernier meeting report
- Bouton "🤖 Generate AI Summary" dans le détail
- Sauvegarde dans `meeting.ai_summary`
- Désactivé si `OPENAI_API_KEY` non configuré

## 4. Règles métier

### Lieu du meeting
- **ONLINE** : Le champ lieu est désactivé et rempli automatiquement avec "Online Meeting"
- **IN_PERSON** : Le champ lieu est obligatoire (3-255 caractères)
- **HYBRID** : Le champ lieu est obligatoire + lien Jitsi généré

### Lien de meeting
- **ONLINE** : Lien Jitsi généré automatiquement
- **IN_PERSON** : Aucun lien autorisé
- **HYBRID** : Lien Jitsi généré automatiquement

### Validation
- Date : au moins demain (pas aujourd'hui)
- Durée : 15-480 minutes
- Agenda : max 2000 caractères

## 5. Structure de la base de données

### Table `meeting_participants`
```sql
CREATE TABLE meeting_participants (
    meeting_id INT NOT NULL,
    user_id INT NOT NULL,
    PRIMARY KEY (meeting_id, user_id),
    FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### Nouvelles colonnes dans `meetings`
- `meeting_link` VARCHAR(500) NULL
- `ai_summary` TEXT NULL
- `raw_content` TEXT NULL

### Nouvelles colonnes dans `meeting_reports`
- `raw_meeting_text` TEXT NULL

## 6. Dépendances Maven ajoutées

```xml
<!-- JavaMail -->
<dependency>
    <groupId>com.sun.mail</groupId>
    <artifactId>javax.mail</artifactId>
    <version>1.6.2</version>
</dependency>

<!-- JavaFX Web (WebView) -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-web</artifactId>
    <version>21.0.1</version>
</dependency>
```

## 7. Fichiers créés

### Services
- `JitsiMeetingService.java` - Génération de liens Jitsi
- `EmailService.java` - Envoi d'emails via Gmail SMTP
- `OpenAIService.java` - Résumés IA via OpenAI API

### DAOs
- `MeetingParticipantDAO.java` - Gestion des participants

### Controllers
- `MeetingCalendarController.java` - Calendrier interactif
- `MeetingDistanceController.java` - Calcul de distance
- Modifications dans `MeetingController.java`, `MeetingFormDialogController.java`, `MeetingDetailDialogController.java`

### FXML
- `MeetingCalendar.fxml` - Vue calendrier
- `MeetingDistance.fxml` - Vue calcul de distance
- Modifications dans `MeetingDetailDialog.fxml`, `MeetingFormDialog.fxml`, `Meetings.fxml`

## 8. Troubleshooting

### La page Meetings est vide
→ Exécutez le script `database_migration.sql` pour créer la table `meeting_participants`

### Les emails ne partent pas
→ Vérifiez que `MAIL_USERNAME` et `MAIL_APP_PASSWORD` sont configurés
→ Vérifiez que le mot de passe d'application Gmail est correct

### Le bouton IA est désactivé
→ Configurez la variable `OPENAI_API_KEY`

### Le calcul de distance ne fonctionne pas
→ Autorisez la géolocalisation dans votre navigateur
→ Vérifiez que le lieu du meeting est une adresse valide

## 9. Compilation et exécution

```bash
mvn clean compile
mvn javafx:run
```

Ou via IntelliJ IDEA : Run → Run 'Main'
