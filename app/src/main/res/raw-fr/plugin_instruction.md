AI Agent transforme un objectif en langage naturel en actions sur un appareil Android exécutant AutoJs6. Soit il choisit un script que l'utilisateur a enregistré pour l'agent, complète ses paramètres et l'exécute ; soit il observe l'écran à travers l'arbre de noeuds d'accessibilité et agit étape par étape (observer, décider, agir, vérifier) jusqu'à ce que l'objectif soit atteint, qu'une confirmation soit nécessaire ou qu'un budget soit épuisé. Il répond à la [discussion AutoJs6 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

Aperçu de développement: scripts enregistrés, actions écran et API de tâches ai.agent sont reliés. Les API de tâches nécessitent AutoJs6 build 5293 ou ultérieur; l'interface complète suit en P6 et la validation de fiabilité continue en P7. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

### Utilisation

1. Installez l'APK du plugin depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) sur un appareil disposant d'AutoJs6 build 5289 ou ultérieure.
2. Ouvrez le centre de plugins d'AutoJs6, vérifiez que `AI Agent` est reconnu et activez-le. Les paquets officiels passent automatiquement la vérification de signature.
3. Connexion demandée depuis le lanceur avec délai de 15 secondes et aide pour activer et autoriser AI Agent dans AutoJs6.
4. Configurez les dossiers supplémentaires dans "Dossiers de scripts" du lanceur, un chemin absolu par ligne. L'hôte valide et applique les chemins enregistrés; les tâches peuvent seulement restreindre ces dossiers.

Consultez le [README du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) et [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) pour le guide de connexion et l'avancement actuel.
