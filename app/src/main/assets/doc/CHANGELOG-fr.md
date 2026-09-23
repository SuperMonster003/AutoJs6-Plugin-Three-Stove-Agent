******

### Historique des versions

******

# v1.0.0

###### 2026/09/23

* `Note` Aperçu de développement: les scripts enregistrés prennent en charge questions, confirmation, résultats et annulation. Les parcours écran suivent en P4, les API de tâches et l'espace de travail en P5/P6.
* `Fonctionnalité` Actions liées aux noeuds inspectés par l'hôte, avec ajout de texte, défilement borné et retour du résultat et des changements de fenêtre
* `Fonctionnalité` OCR d'écran proposé uniquement si l'hôte signale un plugin OCR autorisé disponible, avec fusion en lignes bornées et coordonnées
* `Fonctionnalité` Observations avec références aux instantanés de l'hôte, sorties bornées des noeuds et de la console, et résumés des changements de texte et d'état
* `Fonctionnalité` Les tâches à script unique conservent ID, chemin, ID d'exécution et résultat après la conclusion du modèle, avec null explicite et troncature signalée des grands résultats
* `Fonctionnalité` Exécution des scripts enregistrés avec vérification du manifeste confirmé, observations structurées, fin de console expurgée et arrêt du script en cas de délai dépassé ou de tâche annulée
* `Fonctionnalité` Injection des préférences mémorisées selon leur portée pour les paramètres des scripts, avec limite de 4 KiB, troncature explicite et désactivation par tâche
* `Fonctionnalité` Validation des paramètres des scripts enregistrés avec valeurs par défaut, questions sur les valeurs manquantes, contrôle du risque actuel et tableaux complets pour confirmation
* `Fonctionnalité` `ai-agent`: `AiAgentPluginInfoService`, `WakeActivity`, `AiAgentPluginService`, `ui.LauncherActivity`
* `Fonctionnalité` README, instructions du centre de plugins et journal des modifications en 10 langues
* `Fonctionnalité` Catalogue du noyau Agent avec 30 outils, contrôle des groupes, schémas de paramètres, préparation des appels bridge, observations bornées et élévation des risques sensibles
* `Fonctionnalité` Noyau de décision Agent avec schémas adaptés aux protocoles, analyse JSON stricte ou par extraction, validation des outils/branches, deux corrections au maximum et modèles de prompts en anglais/chinois
* `Fonctionnalité` Budgets Agent pour les étapes, les appels au modèle, la durée et les tokens, avec délais des outils/interactions, estimation d'usage et limitation des tokens de sortie
* `Fonctionnalité` Confirmation Agent avec politiques par défaut/prudente, autorisations limitées à la tâche, au même outil et au même risque, confirmation de chaque paiement et mots-clés dans 10 langues
* `Fonctionnalité` Journal privé Agent limité à 200 étapes et 1 MiB, avec masquage des mots de passe et résultats terminaux bornés conservant le statut et les compteurs
* `Fonctionnalité` Connexion avec identité du programme hôte vérifiée, file de tâches, réponses, annulation, requêtes et historique privé; tâches bloquées après déconnexion et aucun redémarrage automatique après arrêt du processus
* `Fonctionnalité` Assemblage déterministe du contexte Agent avec limites en octets, paires récentes complètes, prompts anglais/chinois et sélection prioritaire des noeuds; budget local de 3000 tokens et signatures compactes des outils
* `Fonctionnalité` Client de modèle hôte avec validation de l'ordre des événements, comptage usage, annulation, délais et repli de format borné; chaque repli compte comme appel et conserve le quota de correction
* `Fonctionnalité` Connexion demandée depuis le lanceur avec délai de 15 secondes et aide pour activer et autoriser AI Agent dans AutoJs6
* `Fonctionnalité` Notifications au premier plan pendant les tâches seulement, avec progression, Arrêter et Voir; saisie et confirmation de chaque action depuis le lanceur
* `Fonctionnalité` Le catalogue des scripts est actualisé au début de chaque tâche, avec un cache de liaison de 60 secondes, un classement déterministe de 24 candidats au plus, des résumés bornés et les recherches script_catalog
* `Correctif` Expurgation des paramètres multilignes avant le découpage de la console, sans laisser passer de secret quand un paramètre correspond à son libellé
* `Correctif` Un service de premier plan en cours de fermeture ne rejette plus le démarrage de la tâche suivante
* `Amélioration` La taille des descriptions de confirmation tient compte des échappements JSON pour respecter la limite des événements Binder avec de grands tableaux
* `Amélioration` L'hôte minimum est AutoJs6 6.8.0 / build 5289 pour inspecter les noeuds et lier la confirmation à l'exécution
* `Dépendance` Ajout de common-plugin-api, host-capability-api et ai-agent-api provenant du même build release AutoJs6 6.8.0 / 5289 (MPL 2.0), verrouillés par SHA-256
* `Dépendance` Ajout de Gson 2.13.2 pour analyser strictement le JSON borné et les arbres de schémas
