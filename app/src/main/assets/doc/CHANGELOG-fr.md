******

### Historique des versions

******

# v1.2.0

###### 2026/09/27

* `Note` La version 1.2.0 en développement propose les outils MCP facultatifs, appels natifs, captures et scripts générés. AiGoCode gpt-5.6-sol a réussi les tests P9.2 avec image initiale et image dans un résultat d'outil. P9.1 a validé l'activation du Wi-Fi et la relecture de son état avec les parcours JSON et natif, en désactivant temporairement la connexion automatique au point d'accès actuel et en accédant au modèle via les données mobiles et le VPN. Les échecs après un changement de réseau du VPN avec la connexion automatique par défaut restent non résolus; voir ROADMAP.md.
* `Fonctionnalité` Outils MCP de serveurs locaux ou externes choisis, avec un niveau de risque par serveur et le groupe mcp désactivé par défaut
* `Fonctionnalité` Ouvrez AI Agent, connectez AutoJs6, saisissez un objectif et démarrez. La capsule de modèle de l'accueil choisit un modèle en ligne ou local, ou Automatique (un modèle sur l'appareil d'abord, sinon le premier disponible). Recherchez, épinglez vos modèles favoris et réutilisez les récents; des badges indiquent la prise en charge déclarée des outils et des images. Le plan de travail et la bulle flottante partagent ce choix pour les nouvelles tâches, sans modifier les préréglages ni la tâche en cours; les préréglages ne contiennent plus de modèle. La puce de préréglage de la zone de saisie choisit un préréglage facultatif. Répondez aux questions et suivez la progression dans la carte de tâche.
* `Fonctionnalité` Ouvrez les paramètres depuis le menu en haut à droite. Chaque modification s'applique immédiatement, sans bouton Enregistrer: apparence, autorisations, groupes d'outils, limites (durée en minutes), saisie vocale, bulle flottante et nettoyage des données. La langue, le mode sombre et la couleur peuvent suivre AutoJs6 ou être définis séparément. La langue et le mode sombre peuvent aussi suivre Android. Historique et mentions légales sont disponibles hors ligne. Les vérifications manuelles GitHub gardent les résultats réussis 24 heures. Les vérifications automatiques sont désactivées par défaut. Une fois activées, elles ont lieu pendant l'utilisation, au plus toutes les 12 heures, sans signaler les échecs ou versions ignorées et sans télécharger d'APK. La gestion des mises à jour ignorées permet de rétablir chaque version. La page À propos indique la version, le développeur, le code source, la licence et les mentions tierces.
* `Fonctionnalité` Les tâches d'écran démarrent d'abord l'accessibilité avec la méthode automatique configurée dans AutoJs6 (Root, paramètres sécurisés ou Shizuku). Ce n'est qu'en cas d'échec ou d'absence de configuration que la carte de tâche vous demande de l'activer, avec un raccourci vers les paramètres d'accessibilité.
* `Fonctionnalité` Les autorisations des opérations proposent l'Accès complet: les outils activés, y compris paiements, suppressions, scripts et écriture en mémoire, s'exécutent sans approbation. Il n'active aucun groupe d'outils supplémentaire et ne relâche ni les budgets ni les autorisations de l'hôte. L'espace de travail, la bulle flottante, la tâche en cours et le détail de l'historique affichent un libellé visible au lieu d'une boîte de dialogue. Les tâches qui demandent explicitement une confirmation prudente la conservent.
* `Fonctionnalité` Les cartes de confirmation ajoutent Toujours autoriser pour cette session: jusqu'à la fin de la tâche, le même outil au même niveau de risque s'exécute sans nouvelle demande, même avec d'autres arguments. Les paiements demandent leur propre accord; les scripts générés et les propositions de mémoire peuvent aussi être autorisés pour la session.
* `Correctif` Un service d'accessibilité AutoJs6 arrêté est signalé au modèle par A11Y_SERVICE_NOT_RUNNING au lieu d'une erreur d'arguments
* `Amélioration` Les identifiants du modèle restent dans son fournisseur; AutoJs6 transmet les appels. Les jetons MCP Bearer sont chiffrés avec Android Keystore dans le stockage privé et exclus des prompts et exports historiques. INTERNET sert aussi aux serveurs MCP configurés; sur Android 17+, la permission réseau local se demande depuis les paramètres MCP. Le risque choisi par serveur est initialement SENSITIVE. Annuler ne rétablit pas les actions distantes; aucun appel échoué ne se rejoue automatiquement.
* `Amélioration` Application autonome repensée en Material 3: l'accueil est un fil de tâches avec la zone de saisie ancrée au-dessus du clavier, une barre supérieure avec la capsule de modèle, l'historique et un menu (Nouvelle tâche, Préréglages, Mémoire, Dossiers de scripts, Serveurs MCP, Paramètres), un bandeau seulement tant qu'AutoJs6 n'est pas connecté, une chronologie des étapes mise à jour par étape, et Relancer ou Réessayer avec un autre modèle qui remplissent la saisie sans démarrer. Paramètres organisés en sections claires et apparence claire/sombre cohérente
* `Amélioration` Les confirmations affichent le niveau de risque, le groupe d'outils et chaque paramètre dans un tableau lisible au lieu du JSON brut, avec des actions distinctes: autoriser une fois, toujours autoriser pour cette session et refuser. La bulle flottante adopte le même design Material, choisit le préréglage directement dans la carte et sa ligne Modèle ouvre le sélecteur de modèle partagé
* `Dépendance` Ajout d'AndroidX AppCompat 1.7.1 et de Material Components for Android 1.13.0 avec leurs dépendances AndroidX d'exécution pour l'interface Material 3

# v1.1.0

###### 2026/09/26

* `Note` Les appels natifs exigent AutoJs6 build 5297+ et une cible tools, comme une cible en ligne de la version de développement 3-Stone AI 1.2.0. Les anciens hôtes et les cibles incompatibles conservent JSON. Chaque conversation garde son délai initial, ses limites de contexte/sortie et 16 tours d'outils au maximum; aucune reprise JSON après une action
* `Note` Les images nécessitent un hôte compatible, le groupe observe et un modèle visuel dont cette entrée est explicitement activée. Implémentation et tests déterministes terminés; validation visuelle réelle en ligne encore en attente. Les anciens systèmes et modèles texte gardent les observations textuelles. Voir ROADMAP.md
* `Note` Les scripts générés utilisent les autorisations AutoJs6 sans bac à sable JavaScript et peuvent agir hors des groupes activés. Le code complet est conservé dans les étapes privées, sous réserve du masquage des mots de passe et de la rétention. Un code modifié par un masquage ultérieur ne peut être enregistré comme original. Vérifiez les .js avant de les partager.
* `Fonctionnalité` Appels natifs via l'hôte: schémas du catalogue, validation du lot entier, exécution séquentielle, confirmations individuelles, retour des résultats et journal commun
* `Fonctionnalité` Observation par capture via AutoJs6 sur Android 11+: screen_capture limite le grand côté à 1280 et utilise JPEG qualité 70, avec instructions visuelles, budget de tokens image et images dans les résultats des outils natifs
* `Fonctionnalité` JavaScript généré via script_run_source : le groupe script_dynamic est désactivé par défaut. Chaque appel exige un résumé du code extensible au texte complet et une approbation individuelle. Exécution avec délai, annulation, résultats structurés et code dans les étapes privées. Le code UTF-8 et sa chaîne JSON sont chacun limités à 8 KiB.
* `Dépendance` Mise à niveau des trois artefacts API hôte release vers AutoJs6 52ce694f92 / build 5297 pour les images négociées, en conservant le contrat de connexion build 5289+

# v1.0.0

###### 2026/09/25

* `Note` La version 1.0.0 propose des tâches en langage naturel, des appels de scripts enregistrés et des actions sur le périphérique avec confirmation selon le risque. Consultez [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) pour les cas vérifiés, les limites des modèles et les vérifications de périphériques restantes. Les appels natifs aux outils, les entrées visuelles et la génération dynamique de scripts sont prévus pour 1.1.0.
* `Note` Nécessite Android 7+, AutoJs6 6.8.0 / build 5293+ pour les API de tâches, et 3-Stone AI activé avec un modèle configuré. OCR est facultatif. Le seul protocole de connexion nécessite build 5289+.
* `Note` Compatibilité: l'extension native des outils dans AutoJs6 build 5297 est compatible avec cette version. Le candidat de développement 3-Stone AI 1.2.0 implémente la continuation des outils en ligne pour trois protocoles. Cette version Agent utilise encore des décisions JSON structurées; intégration de la boucle native et comparaisons restent dans [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).
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
