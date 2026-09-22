AI Agent transforme un objectif en langage naturel en actions sur un appareil Android exécutant AutoJs6. Soit il choisit un script que l'utilisateur a enregistré pour l'agent, complète ses paramètres et l'exécute ; soit il observe l'écran à travers l'arbre de noeuds d'accessibilité et agit étape par étape (observer, décider, agir, vérifier) jusqu'à ce que l'objectif soit atteint, qu'une confirmation soit nécessaire ou qu'un budget soit épuisé. Il répond à la [discussion AutoJs6 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

La version 1.0.0 est l'aperçu de développement P0 de la feuille de route : l'identité du plugin, le contrat de découverte AutoJs6 (service INFO, Wake Activity et service `org.autojs.plugin.AI_AGENT` provisoire) et un écran de lancement qui indique l'état de l'hôte. La boucle de l'agent, le catalogue de scripts, l'API `ai.agent` et l'espace de tâches ne sont pas encore implémentés ; l'avancement et les preuves sont suivis dans [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md). Le plugin nécessitera AutoJs6 build 5283 ou ultérieure.

### Utilisation

1. Installez l'APK du plugin depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) sur un appareil disposant d'AutoJs6 build 5283 ou ultérieure.
2. Ouvrez le centre de plugins d'AutoJs6, vérifiez que `AI Agent` est reconnu et activez-le. Les paquets officiels passent automatiquement la vérification de signature.
3. Ouvrez AI Agent depuis le lanceur : dans cet aperçu, l'écran indique seulement si un hôte AutoJs6 compatible est installé. L'espace de tâches, l'entrée du tiroir et l'API `ai.agent` arrivent avec les phases suivantes de la feuille de route.

Consultez le [README du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) et [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) pour le guide de connexion et l'avancement actuel.
