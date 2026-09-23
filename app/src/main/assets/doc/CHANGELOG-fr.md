******

### Historique des versions

******

# v1.0.0

###### 2026/09/23

* `Note` La version installée affiche l'état de l'hôte. Les interfaces P1 et les noyaux outils, décisions, exécution, contexte et client modèle P2.1-P2.4 sont implémentés et testés. L'exécution réelle nécessite encore l'intégration Binder/service au premier plan P2.5 et les adaptateurs P3/P4. L'API de scripts et l'interface des tâches suivront en P5/P6.
* `Note` L'hôte implémente les contrats AI Agent, les intermédiaires de capacités et de modèles, l'observation de l'écran, l'exécution des scripts enregistrés et les accès du volet et du centre de plugins; l'exécution des tâches du plugin reste en développement
* `Fonctionnalité` Identité de plugin `ai-agent` avec le service INFO, la Wake Activity, le service provisoire `org.autojs.plugin.AI_AGENT` dans le processus `:agent` et un écran de lancement indiquant si un hôte AutoJs6 compatible est installé
* `Fonctionnalité` README, instructions du centre de plugins et journal des modifications en 10 langues
* `Fonctionnalité` Catalogue du noyau Agent avec 30 outils, contrôle des groupes, schémas de paramètres, préparation des appels bridge, observations bornées et élévation des risques sensibles; intégration ultérieure du moteur
* `Fonctionnalité` Noyau de décision Agent avec schémas adaptés aux protocoles, analyse JSON stricte ou par extraction, validation des outils/branches, deux corrections au maximum et modèles de prompts en anglais/chinois; exécution des tâches non raccordée
* `Fonctionnalité` Budgets Agent pour les étapes, les appels au modèle, la durée et les tokens, avec délais des outils/interactions, estimation d'usage et limitation des tokens de sortie
* `Fonctionnalité` Confirmation Agent avec politiques par défaut/prudente, autorisations limitées à la tâche, au même outil et au même risque, confirmation de chaque paiement et mots-clés dans 10 langues
* `Fonctionnalité` Journal privé Agent limité à 200 étapes et 1 MiB, avec masquage des mots de passe et résultats terminaux bornés conservant le statut et les compteurs
* `Fonctionnalité` Exécuteur Agent séquentiel et file de tâches (1 active + 8 en attente), appels modèle/outil annulables, délais d'interaction, événement terminal unique et blocage après perte de l'hôte; intégration réelle prévue aux étapes suivantes
* `Fonctionnalité` Assemblage déterministe du contexte Agent avec limites en octets, paires récentes complètes, prompts anglais/chinois et sélection prioritaire des noeuds; budget local de 3000 tokens et signatures compactes des outils
* `Fonctionnalité` Client de modèle hôte avec validation de l'ordre des événements, comptage usage, annulation, délais et repli de format borné; chaque repli compte comme appel et conserve le quota de correction
* `Amélioration` Version minimale de l'hôte fixée à AutoJs6 6.8.0 / build 5285, correspondant à la livraison des interfaces et accès P1
* `Dépendance` Ajout de `common-plugin-api.aar` (module AutoJs6 `plugin-api/common-plugin-api`, build hôte 6.8.0 / 5282, MPL 2.0) comme contrat de plugin partagé, verrouillé par hachage dans `locks/host-api-aars.lock`
* `Dépendance` Ajout de Gson 2.13.2 pour analyser strictement le JSON borné et les arbres de schémas
