AI Agent transforme un objectif en langage naturel en actions sur un appareil Android exécutant AutoJs6. Soit il choisit un script que l'utilisateur a enregistré pour l'agent, complète ses paramètres et l'exécute ; soit il observe l'écran à travers l'arbre de noeuds d'accessibilité et agit étape par étape (observer, décider, agir, vérifier) jusqu'à ce que l'objectif soit atteint, qu'une confirmation soit nécessaire ou qu'un budget soit épuisé. Il répond à la [discussion AutoJs6 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

La version installée affiche l'état de l'hôte. Les interfaces P1 et les noyaux outils, décisions, exécution, contexte et client modèle P2.1-P2.4 sont implémentés et testés. L'exécution réelle nécessite encore l'intégration Binder/service au premier plan P2.5 et les adaptateurs P3/P4. L'API de scripts et l'interface des tâches suivront en P5/P6. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

### Utilisation

1. Installez l'APK du plugin depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) sur un appareil disposant d'AutoJs6 build 5285 ou ultérieure.
2. Ouvrez le centre de plugins d'AutoJs6, vérifiez que `AI Agent` est reconnu et activez-le. Les paquets officiels passent automatiquement la vérification de signature.
3. Ouvrez AI Agent depuis le lanceur ou l'action de gestion du volet AutoJs6. Cet aperçu affiche uniquement l'état de l'hôte; l'espace de tâches et l'API `ai.agent` arriveront dans les phases suivantes.

Consultez le [README du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) et [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) pour le guide de connexion et l'avancement actuel.
