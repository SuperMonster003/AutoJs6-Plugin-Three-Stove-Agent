<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-ai-agent-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Exécute des tâches en langage naturel dans AutoJs6 en choisissant des scripts enregistrés et en manipulant l'écran étape par étape</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-AI-Agent?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-AI-Agent?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-AI-Agent?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Langues

******

Le README.md actuel prend en charge les langues suivantes:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-en.md)
- Français [fr] # actuel
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ar.md)

******

### Introduction

******

AI Agent transforme un objectif en langage naturel en actions sur un appareil Android exécutant AutoJs6. Soit il choisit un script que l'utilisateur a enregistré pour l'agent, complète ses paramètres et l'exécute ; soit il observe l'écran à travers l'arbre de noeuds d'accessibilité et agit étape par étape (observer, décider, agir, vérifier) jusqu'à ce que l'objectif soit atteint, qu'une confirmation soit nécessaire ou qu'un budget soit épuisé. Il répond à la [discussion AutoJs6 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

Le plugin est à la fois un plugin AutoJs6 et une application autonome. Les scripts l'utilisent via l'API `ai.agent` d'AutoJs6 ; les utilisateurs y accèdent par son propre espace de tâches, le tiroir d'AutoJs6, une bulle flottante, le partage système, les raccourcis d'application et la saisie vocale. Les appels de modèle et les actions sur l'appareil passent toujours par AutoJs6 via Binder : l'hôte prête au plugin un courtier de modèle (les plugins AI Provider que l'hôte connaît déjà, comme 3-Stone AI) et un courtier de capacités avec une autorisation bornée. Le plugin ne détient jamais d'identifiants, ne se lie jamais lui-même à un fournisseur de modèle et ne demande jamais la permission d'accessibilité.

******

### État

******

Aperçu de développement: les scripts enregistrés prennent en charge questions, confirmation, résultats et annulation. Les parcours écran suivent en P4, les API de tâches et l'espace de travail en P5/P6. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

******

### Fonctionnalités

******

La version 1.0.0 doit fournir les capacités suivantes:

- Sélection de scripts : les scripts enregistrés via `project.json` ou un commentaire d'en-tête `@agent` sont présentés au modèle avec leurs descriptions et schémas de paramètres ; l'agent en choisit un, complète les paramètres, demande confirmation si nécessaire, l'exécute dans AutoJs6 et lit son résultat structuré.
- Manipulation de l'écran étape par étape : l'agent observe l'arbre de noeuds d'accessibilité sous forme de texte compact (et le texte de l'écran via un plugin OCR lorsqu'il est installé), puis clique, saisit, fait défiler et appuie sur des touches via le courtier de capacités d'AutoJs6 jusqu'à pouvoir vérifier l'objectif.
- Sécurité par conception : les outils en lecture seule s'exécutent automatiquement, les actions sensibles (paiement, envoi, suppression, écriture de fichiers, shell, gestes par coordonnées, scripts enregistrés comme sensibles) nécessitent une confirmation, et chaque exécution a des budgets d'étapes, d'appels de modèle, de durée et de jetons.
- API de script et interface utilisateur : `ai.agent.run(goal, options)` renvoie un handle `AgentRun` avec événements, réponses et annulation ; l'application autonome offre un espace de tâches avec historique, préréglages, mémoire de préférences, paramètres et historique des versions.

### Catalogue des outils

Aperçu de développement: les scripts enregistrés prennent en charge questions, confirmation, résultats et annulation. Les parcours écran suivent en P4, les API de tâches et l'espace de travail en P5/P6.

| Outil | Groupe | Risque | Défaut | Description |
| --- | --- | --- | --- | --- |
| `app_launch` | `act` | `NORMAL` | `on` | Open an application by package name or display name. |
| `clipboard_get` | `act` | `READ_ONLY` | `on` | Read clipboard text. |
| `clipboard_set` | `act` | `NORMAL` | `on` | Replace clipboard text. |
| `ui_click` | `act` | `NORMAL` | `on` | Click one observed target. |
| `ui_long_click` | `act` | `NORMAL` | `on` | Long-click one observed target. |
| `ui_press_key` | `act` | `NORMAL` | `on` | Use an Android navigation or notification-panel action. |
| `ui_scroll` | `act` | `NORMAL` | `on` | Scroll one observed target a bounded number of times. |
| `ui_set_text` | `act` | `NORMAL` | `on` | Set or append text on one observed editable target. |
| `files_list` | `files` | `NORMAL` | `off` | List workspace files. |
| `files_read` | `files` | `NORMAL` | `off` | Read bounded workspace file text. |
| `files_stat` | `files` | `NORMAL` | `off` | Read workspace file metadata. |
| `files_write` | `files` | `SENSITIVE` | `off` | Write a workspace file after confirmation. |
| `ui_click_xy` | `gesture` | `SENSITIVE` | `off` | Tap coordinates only with the gesture group enabled and confirmation. |
| `ui_gesture` | `gesture` | `SENSITIVE` | `off` | Follow a bounded coordinate path after confirmation. |
| `ui_swipe` | `gesture` | `SENSITIVE` | `off` | Swipe between coordinates after confirmation. |
| `memory_get` | `memory` | `READ_ONLY` | `on` | Read available preference memory in the current scope. |
| `memory_propose` | `memory` | `SENSITIVE` | `on` | Propose a preference for user-approved storage; never store credentials. |
| `app_current` | `observe` | `READ_ONLY` | `on` | Read the current window and application. |
| `console_tail` | `observe` | `READ_ONLY` | `on` | Read bounded recent console lines; they may include unrelated scripts. |
| `device_info` | `observe` | `READ_ONLY` | `on` | Read device information. |
| `screen_state` | `observe` | `READ_ONLY` | `on` | Read whether the screen is on. |
| `ui_dump` | `observe` | `READ_ONLY` | `on` | Observe the current accessibility tree before choosing an action. |
| `ui_find` | `observe` | `READ_ONLY` | `on` | Find nodes matching all selector conditions. |
| `ui_wait_for` | `observe` | `READ_ONLY` | `on` | Wait for a selector to appear or disappear within a deadline. |
| `ocr_screen` | `ocr` | `READ_ONLY` | `auto (OCR)` | Read screen text through the host OCR plugin. |
| `script_catalog` | `script` | `READ_ONLY` | `on` | Find scripts explicitly registered for Agent use. |
| `script_run` | `script` | `NORMAL` | `on` | Run a registered script by id with validated parameters and its registered risk. |
| `script_stop` | `script` | `NORMAL` | `on` | Stop an owned script execution. |
| `shell_exec` | `shell` | `SENSITIVE` | `off` | Execute a bounded non-root shell command after confirmation. |
| `report_progress` | `user` | `READ_ONLY` | `on` | Report bounded progress without declaring task completion. |

******

### Utilisation

******

1. Installez l'APK du plugin depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) sur un appareil disposant d'AutoJs6 build 5288 ou ultérieure.
2. Ouvrez le centre de plugins d'AutoJs6, vérifiez que `AI Agent` est reconnu et activez-le. Les paquets officiels passent automatiquement la vérification de signature.
3. Connexion demandée depuis le lanceur avec délai de 15 secondes et aide pour activer et autoriser AI Agent dans AutoJs6.
4. Configurez les dossiers supplémentaires dans "Dossiers de scripts" du lanceur, un chemin absolu par ligne. L'hôte valide et applique les chemins enregistrés; les tâches peuvent seulement restreindre ces dossiers.

> Aperçu de développement: les scripts enregistrés prennent en charge questions, confirmation, résultats et annulation. Les parcours écran suivent en P4, les API de tâches et l'espace de travail en P5/P6.

******

### Permissions et sécurité

******

Le plugin respecte des limites explicites :

- Les points d'entrée Binder sont protégés par la permission de signature `org.autojs.permission.PLUGIN`, de sorte que seul AutoJs6 peut les atteindre ; l'écran de lancement est le seul autre composant exporté.
- Le plugin ne détient aucune clé d'API, ne se lie jamais à un fournisseur de modèle et ne demande pas la permission d'accessibilité : les appels de modèle et les actions sur l'appareil passent par des courtiers qu'AutoJs6 prête pour un lien attaché et révoque au détachement, chacun borné par une autorisation (méthodes permises, débits, tailles, quota de modèle).
- Aucune permission réseau. FOREGROUND_SERVICE et FOREGROUND_SERVICE_SPECIAL_USE servent aux tâches actives; POST_NOTIFICATIONS affiche la progression et les commandes de fin. Aucune permission pour accessibilité ou superposition.
- L'historique des tâches, les préréglages et la mémoire de préférences restent dans le stockage privé du plugin ; les sauvegardes et les transferts d'appareil sont désactivés.

N'obtenez le plugin que depuis la page officielle [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) ou le centre de plugins d'AutoJs6. Les paquets de sources inconnues peuvent échouer à la vérification de l'hôte ou présenter des risques même lorsque le numéro de version semble identique.

******

### Interface du plugin

******

Les informations suivantes s'adressent aux développeurs de l'hôte AutoJs6 et de plugins ; l'hôte utilise ces identifiants pour découvrir le plugin et négocier la compatibilité:

```text
application id: io.github.supermonster003.autojs6.plugin.ai.agent
plugin id: ai-agent
engine: ai-agent
variant: default
service action: org.autojs.plugin.AI_AGENT
service category: ai-agent
service process: :agent
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.ai.agent.api.IAiAgentPlugin
minimum host build: 5288 (6.8.0)
```

`AiAgentPluginService` / `IAiAgentPlugin` / `IAiAgentLink`: Connexion avec identité du programme hôte vérifiée, file de tâches, réponses, annulation, requêtes et historique privé; tâches bloquées après déconnexion et aucun redémarrage automatique après arrêt du processus.

******

### Feuille de route

******

Les plans et l'avancement du plugin sont tenus sous forme de liste cochable dans ROADMAP.md, organisée par phase avec des critères d'acceptation et des niveaux de preuve. Les éléments non cochés expriment une intention et non une capacité actuelle ; les discussions via Issues sont les bienvenues.

- [Voir ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md)

******

### Historique des versions

******

#### v1.0.0

_2026/09/23_

- `Note` Aperçu de développement: les scripts enregistrés prennent en charge questions, confirmation, résultats et annulation. Les parcours écran suivent en P4, les API de tâches et l'espace de travail en P5/P6.
- `Fonctionnalité` OCR d'écran proposé uniquement si l'hôte signale un plugin OCR autorisé disponible, avec fusion en lignes bornées et coordonnées
- `Fonctionnalité` Observations avec références aux instantanés de l'hôte, sorties bornées des noeuds et de la console, et résumés des changements de texte et d'état
- `Fonctionnalité` Les tâches à script unique conservent ID, chemin, ID d'exécution et résultat après la conclusion du modèle, avec null explicite et troncature signalée des grands résultats
- `Fonctionnalité` Exécution des scripts enregistrés avec vérification du manifeste confirmé, observations structurées, fin de console expurgée et arrêt du script en cas de délai dépassé ou de tâche annulée
- `Fonctionnalité` Injection des préférences mémorisées selon leur portée pour les paramètres des scripts, avec limite de 4 KiB, troncature explicite et désactivation par tâche
- `Fonctionnalité` Validation des paramètres des scripts enregistrés avec valeurs par défaut, questions sur les valeurs manquantes, contrôle du risque actuel et tableaux complets pour confirmation
- `Fonctionnalité` `ai-agent`: `AiAgentPluginInfoService`, `WakeActivity`, `AiAgentPluginService`, `ui.LauncherActivity`
- `Fonctionnalité` README, instructions du centre de plugins et journal des modifications en 10 langues
- `Fonctionnalité` Catalogue du noyau Agent avec 30 outils, contrôle des groupes, schémas de paramètres, préparation des appels bridge, observations bornées et élévation des risques sensibles
- `Fonctionnalité` Noyau de décision Agent avec schémas adaptés aux protocoles, analyse JSON stricte ou par extraction, validation des outils/branches, deux corrections au maximum et modèles de prompts en anglais/chinois
- `Fonctionnalité` Budgets Agent pour les étapes, les appels au modèle, la durée et les tokens, avec délais des outils/interactions, estimation d'usage et limitation des tokens de sortie
- `Fonctionnalité` Confirmation Agent avec politiques par défaut/prudente, autorisations limitées à la tâche, au même outil et au même risque, confirmation de chaque paiement et mots-clés dans 10 langues
- `Fonctionnalité` Journal privé Agent limité à 200 étapes et 1 MiB, avec masquage des mots de passe et résultats terminaux bornés conservant le statut et les compteurs
- `Fonctionnalité` Connexion avec identité du programme hôte vérifiée, file de tâches, réponses, annulation, requêtes et historique privé; tâches bloquées après déconnexion et aucun redémarrage automatique après arrêt du processus
- `Fonctionnalité` Assemblage déterministe du contexte Agent avec limites en octets, paires récentes complètes, prompts anglais/chinois et sélection prioritaire des noeuds; budget local de 3000 tokens et signatures compactes des outils
- `Fonctionnalité` Client de modèle hôte avec validation de l'ordre des événements, comptage usage, annulation, délais et repli de format borné; chaque repli compte comme appel et conserve le quota de correction
- `Fonctionnalité` Connexion demandée depuis le lanceur avec délai de 15 secondes et aide pour activer et autoriser AI Agent dans AutoJs6
- `Fonctionnalité` Notifications au premier plan pendant les tâches seulement, avec progression, Arrêter et Voir; saisie et confirmation de chaque action depuis le lanceur
- `Fonctionnalité` Le catalogue des scripts est actualisé au début de chaque tâche, avec un cache de liaison de 60 secondes, un classement déterministe de 24 candidats au plus, des résumés bornés et les recherches script_catalog
- `Correctif` Expurgation des paramètres multilignes avant le découpage de la console, sans laisser passer de secret quand un paramètre correspond à son libellé
- `Correctif` Un service de premier plan en cours de fermeture ne rejette plus le démarrage de la tâche suivante
- `Amélioration` La taille des descriptions de confirmation tient compte des échappements JSON pour respecter la limite des événements Binder avec de grands tableaux
- `Amélioration` L'hôte minimum est AutoJs6 6.8.0 / build 5288 pour détecter la disponibilité OCR et vérifier les autorisations
- `Dépendance` Ajout de common-plugin-api, host-capability-api et ai-agent-api provenant du même build release AutoJs6 6.8.0 / 5288 (MPL 2.0), verrouillés par SHA-256
- `Dépendance` Ajout de Gson 2.13.2 pour analyser strictement le JSON borné et les arbres de schémas

##### Pour plus d'historique des versions

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/assets/doc/CHANGELOG-fr.md)

******

### Compilation et vérification

******

Cette section s'adresse aux développeurs souhaitant compiler le plugin depuis les sources ; les utilisateurs ordinaires peuvent simplement installer l'APK préconstruit depuis la page Releases.

Compiler un APK de débogage:

```powershell
.\gradlew.bat :app:assembleDebug
```

Exécuter les tests unitaires JVM et compiler l'APK de tests d'instrumentation:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Compiler l'APK de release:

```powershell
.\gradlew.bat :app:assembleRelease
```

Collecter l'artefact de release et ajouter la version et le condensé CRC32 à son nom de fichier:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Vérifier que les sources de documentation multilingues et les artefacts générés sont synchronisés (également appliqué par la CI):

```powershell
py .python\generate_markdown.py --check
```

La compilation nécessite JDK 21 ou ultérieur et Android SDK 37 ; les versions de Gradle et des plugins sont gérées de manière centralisée par `version.properties` et `io.github.supermonster003.autojs6-platform-versions`.

******

### Localisation et génération de la documentation

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

Les fichiers JSON de langue sous `.readme/` et `.changelog/` sont la source unique du README, des instructions du centre de plugins et du journal des modifications. Modifiez toujours ces sources JSON et relancez `py .python/generate_markdown.py` ; les artefacts README, `plugin_instruction.md` et journal des modifications générés ne sont jamais édités à la main. Exécutez `py .python/generate_markdown.py --check` pour vérifier tous les artefacts générés.

******

### Licence

******

Le code du projet est publié sous la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/LICENSE). Les composants tiers et leurs licences sont listés dans les [Avis de tiers](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/THIRD_PARTY_NOTICES.md).

******

### Liens

******

- Projet AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Documentation AutoJs6: https://docs.autojs6.com
- Discussion AutoJs6 #577: https://github.com/SuperMonster003/AutoJs6/discussions/577
- Avis de tiers: https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/THIRD_PARTY_NOTICES.md
