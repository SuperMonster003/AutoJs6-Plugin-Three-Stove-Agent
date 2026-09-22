******

### Historique des versions

******

# v1.0.0

###### 2026/09/22

* `Note` Aperçu de développement P0 : identité du plugin, contrat de découverte AutoJs6 et écran de lancement indiquant l'état de l'hôte. La boucle de l'agent, le catalogue de scripts, l'API ai.agent et l'espace de tâches ne sont pas encore implémentés. Voir ROADMAP.md.
* `Fonctionnalité` Identité de plugin `ai-agent` avec le service INFO, la Wake Activity, le service provisoire `org.autojs.plugin.AI_AGENT` dans le processus `:agent` et un écran de lancement indiquant si un hôte AutoJs6 compatible est installé
* `Fonctionnalité` README, instructions du centre de plugins et journal des modifications en 10 langues
* `Dépendance` Ajout de `common-plugin-api.aar` (module AutoJs6 `plugin-api/common-plugin-api`, build hôte 6.8.0 / 5282, MPL 2.0) comme contrat de plugin partagé, verrouillé par hachage dans `locks/host-api-aars.lock`
