******

### Historique des versions

******

# v1.0.0

###### 2026/09/25

* `Note` La version 1.0.0 propose des tâches en langage naturel, des appels de scripts enregistrés et des actions sur le périphérique avec confirmation selon le risque. Consultez [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) pour les cas vérifiés, les limites des modèles et les vérifications de périphériques restantes. Les appels natifs aux outils, les entrées visuelles et la génération dynamique de scripts sont prévus pour 1.1.0.
* `Note` Nécessite Android 7+, AutoJs6 6.8.0 / build 5293+ pour les API de tâches, et 3-Stone AI activé avec un modèle configuré. OCR est facultatif. Le seul protocole de connexion nécessite build 5289+.
* `Fonctionnalité` Tableau de tâches en langage naturel avec questions, progression, arrêt et résultats; saisie flottante facultative, partage de texte, raccourcis de préréglages et brouillons vocaux
* `Fonctionnalité` API ai.agent pour créer des tâches, suivre événements et requêtes, répondre et annuler, avec tâches detached et accès aux résultats/contexte des scripts enregistrés
* `Fonctionnalité` Scripts project.json / @agent avec recherche, validation des paramètres et valeurs par défaut, questions sur les valeurs manquantes, confirmation, exécution bornée et résultats structurés
* `Fonctionnalité` Observation par texte des noeuds et OCR autorisé facultatif, clics par référence, saisie, défilement et touches, avec contrôle des changements et preuves d'achèvement
* `Fonctionnalité` Modèles en ligne et locaux via AutoJs6 sans conserver leurs identifiants; une cible choisie absente échoue sans changement silencieux de modèle
* `Fonctionnalité` Budgets de pas, appels, durée et tokens, délais des outils, deux tentatives de réparation au plus par étape et protection contre les actions répétées sans effet
* `Fonctionnalité` Préréglages nommés et paramètres globaux de modèle, contexte, outils, budgets, prudence, dossiers et mémoire; gesture/files/shell désactivés par défaut
* `Fonctionnalité` Mémoire de préférences par portée avec approbation individuelle des propositions/importations, édition, suppression et sauvegarde JSON, jusqu'à 500 entrées / 256 KiB; injection automatique limitée à 4 KiB
* `Fonctionnalité` Détails et chronologies, filtres, brouillons de relance et export JSON expurgé, avec historique privé limité à 200 tâches / 32 MiB
* `Fonctionnalité` Confirmation selon le risque dans le tableau, les notifications et la carte flottante; paiements et mémoire toujours approuvés individuellement; perte de l'hôte bloquante et aucune reprise automatique après redémarrage
* `Fonctionnalité` Paramètres, historique hors ligne et mentions légales en dix langues; recherche manuelle GitHub avec annulation, cache quotidien et versions ignorées, sans téléchargement automatique d'APK
* `Correctif` Fin prématurée des tâches lorsque le budget restant est interprété comme consommé
* `Correctif` Zones tactiles des formulaires et filtres, retour à la ligne des choix et colonnes de paramètres, et commandes flottantes avec les grandes polices et sur Android 7
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
