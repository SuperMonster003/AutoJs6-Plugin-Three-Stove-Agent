******

### Historique des versions

******

# v1.0.0

###### 2026/09/25

* `Note` Aperçu de développement: les écrans P6, les paramètres, la bulle flottante, le partage, les raccourcis et les brouillons vocaux sont disponibles. ai.agent nécessite AutoJs6 build 5293 ou ultérieur. La fiabilité et la publication restent à valider dans P7/P8.
* `Fonctionnalité` Bulle facultative avec saisie, progression, arrêt et confirmation, partage de texte, raccourcis statiques et de préréglages, reconnaissance vocale système sans envoi automatique
* `Fonctionnalité` Paramètres globaux, gestion des catégories de données, historique et mentions légales hors ligne, vérification manuelle annulable avec cache quotidien et versions ignorées
* `Fonctionnalité` Confirmation dans les tâches et notifications avec risque, compte à rebours, autorisation par tâche et mémoire des réponses confirmée séparément
* `Fonctionnalité` Mémoire des préférences avec confirmation de chaque proposition, recherche par portée, protection des conflits, stockage par entrée, modification, suppression et sauvegarde JSON avec approbation individuelle des imports
* `Fonctionnalité` Préréglages nommés avec création, modification, duplication, suppression et choix par défaut; catalogue des modèles avec localisation et prise en charge du JSON structuré, contexte fixe, restrictions des outils et budgets, confirmation, dossiers approuvés et portée mémoire
* `Fonctionnalité` Chronologie complète et résultats, filtres par état/préréglage/date, brouillons de relance, suppression, export JSON expurgé et historique privé versionné avec migration et nettoyage LRU (200 tâches / 32 MiB)
* `Fonctionnalité` Tableau des tâches avec lancement commun, apparence de l'hôte, interactions intégrées, budgets et 20 tâches récentes consultables hors connexion
* `Fonctionnalité` La fin de tâche exige des preuves, les résultats partiels listent le travail restant et les commandes ou paiements exigent un état de commande observé
* `Fonctionnalité` La vérification conserve le compteur d'écrans inchangés après réduction du contexte et bloque la troisième demande d'action équivalente avant exécution
* `Fonctionnalité` Attente bornée de stabilité après une action et résumé des changements depuis la dernière action dans les observations suivantes
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
* `Correctif` Contournements de la validation des identifiants dans la mémoire des préférences avec des caractères pleine chasse, sans chasse et certains noms supplémentaires
* `Correctif` La bulle de tâche pouvait rester masquée au réveil sans verrouillage sécurisé, avant la stabilisation de l'état de l'écran
* `Correctif` Les tâches interrompues par la fin du processus du plugin sont marquées en échec au redémarrage; un écran verrouillé bloque les actions suivantes
* `Correctif` Les outils de fichiers rejettent les traversées de répertoires et les chemins absolus ou invalides avant confirmation ou envoi à l'hôte; l'historique conserve des catégories de rejet bornées sans le texte rejeté du modèle
* `Correctif` La confirmation revient dans l'application cible avant de reprendre les actions, traite les accusés après l'arrêt de l'écran et replie la carte flottante avant l'exécution
* `Correctif` Le lancement sous Android 13 ne plante plus lors de la lecture du contrôleur des barres système avant la création de la vue de fenêtre
* `Correctif` Tri et conservation de l'historique selon le début des tâches pour éviter que la réécriture des fichiers au redémarrage supprime les plus récentes
* `Correctif` Les réponses et confirmations vérifient le propriétaire interaction afin qu'un script ne réponde pas à la place de l'interface du plugin
* `Correctif` Les boutons de confirmation de transaction exigent une confirmation de paiement distincte sans réutiliser les autorisations de toute la tâche
* `Correctif` Les résultats hors écran aux limites vides ou inversées conservent leur texte et signalent des coordonnées inutilisables au lieu d'une erreur de paramètres
* `Correctif` La relocalisation des noeuds distingue les limites et capacités des conteneurs pour ne pas confondre les conteneurs imbriqués avec la cible
* `Correctif` Indications précises pour corriger les cibles de noeuds: conserver le préfixe # et omettre snapshotId avec selector
* `Correctif` L'admission précharge les règles de commande et évite leur compilation coûteuse
* `Correctif` La vérification distingue les noeuds de fenêtres différentes, conserve l'obligation d'observer après lecture du presse-papiers et ne confond plus transfert de fichiers et paiement
* `Correctif` Une lecture de l'écran sans réponse après une action ne dépasse plus le délai de stabilisation
* `Correctif` Expurgation des paramètres multilignes avant le découpage de la console, sans laisser passer de secret quand un paramètre correspond à son libellé
* `Correctif` Un service de premier plan en cours de fermeture ne rejette plus le démarrage de la tâche suivante
* `Amélioration` La réduction des longs historiques réutilise les fragments inchangés des instructions et observations pour réduire le temps de traitement par étape
* `Amélioration` La taille des descriptions de confirmation tient compte des échappements JSON pour respecter la limite des événements Binder avec de grands tableaux
* `Amélioration` L'hôte minimum est AutoJs6 6.8.0 / build 5289 pour inspecter les noeuds et lier la confirmation à l'exécution
* `Dépendance` Ajout de common-plugin-api, host-capability-api et ai-agent-api provenant du même build release AutoJs6 6.8.0 / 5289 (MPL 2.0), verrouillés par SHA-256
* `Dépendance` Ajout de Gson 2.13.2 pour analyser strictement le JSON borné et les arbres de schémas
