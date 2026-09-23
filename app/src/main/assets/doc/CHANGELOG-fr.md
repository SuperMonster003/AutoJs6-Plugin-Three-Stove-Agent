******

### Historique des versions

******

# v1.0.0

###### 2026/09/23

* `Note` Aperçu de développement P0 : identité du plugin, contrat de découverte AutoJs6 et écran de lancement indiquant l'état de l'hôte. La boucle de l'agent, le catalogue de scripts, l'API ai.agent et l'espace de tâches ne sont pas encore implémentés. Voir ROADMAP.md.
* `Note` L'hôte implémente les contrats AI Agent, les intermédiaires de capacités et de modèles, l'observation de l'écran, l'exécution des scripts enregistrés et les accès du volet et du centre de plugins; l'exécution des tâches du plugin reste en développement
* `Fonctionnalité` Identité de plugin `ai-agent` avec le service INFO, la Wake Activity, le service provisoire `org.autojs.plugin.AI_AGENT` dans le processus `:agent` et un écran de lancement indiquant si un hôte AutoJs6 compatible est installé
* `Fonctionnalité` README, instructions du centre de plugins et journal des modifications en 10 langues
* `Fonctionnalité` Catalogue du noyau Agent avec 30 outils, contrôle des groupes, schémas de paramètres, préparation des appels bridge, observations bornées et élévation des risques sensibles; intégration ultérieure du moteur
* `Fonctionnalité` Noyau de décision Agent avec schémas adaptés aux protocoles, analyse JSON stricte ou par extraction, validation des outils/branches, deux corrections au maximum et modèles de prompts en anglais/chinois; exécution des tâches non raccordée
* `Fonctionnalité` Budgets Agent pour les étapes, les appels au modèle, la durée et les tokens, avec délais des outils/interactions, estimation d'usage et limitation des tokens de sortie
* `Amélioration` Version minimale de l'hôte fixée à AutoJs6 6.8.0 / build 5285, correspondant à la livraison des interfaces et accès P1
* `Dépendance` Ajout de `common-plugin-api.aar` (module AutoJs6 `plugin-api/common-plugin-api`, build hôte 6.8.0 / 5282, MPL 2.0) comme contrat de plugin partagé, verrouillé par hachage dans `locks/host-api-aars.lock`
* `Dépendance` Ajout de Gson 2.13.2 pour analyser strictement le JSON borné et les arbres de schémas
