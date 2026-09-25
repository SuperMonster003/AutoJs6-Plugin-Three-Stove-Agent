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

Aperçu de développement de 1.0.0. Les API de tâches et les entrées de l'interface sont implémentées; les preuves d'audit P7 sont consignées. Les contrôles et la publication P8 restent à effectuer. Voir [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) pour les cas validés et les limites connues.

******

### Fonctionnalités

******

L'implémentation actuelle propose les fonctions suivantes:

- Sélection de scripts : les scripts enregistrés via `project.json` ou un commentaire d'en-tête `@agent` sont présentés au modèle avec leurs descriptions et schémas de paramètres ; l'agent en choisit un, complète les paramètres, demande confirmation si nécessaire, l'exécute dans AutoJs6 et lit son résultat structuré.
- Manipulation de l'écran étape par étape : l'agent observe l'arbre de noeuds d'accessibilité sous forme de texte compact (et le texte de l'écran via un plugin OCR lorsqu'il est installé), puis clique, saisit, fait défiler et appuie sur des touches via le courtier de capacités d'AutoJs6 jusqu'à pouvoir vérifier l'objectif.
- Sécurité par conception : les outils en lecture seule s'exécutent automatiquement, les actions sensibles (paiement, envoi, suppression, écriture de fichiers, shell, gestes par coordonnées, scripts enregistrés comme sensibles) nécessitent une confirmation, et chaque exécution a des budgets d'étapes, d'appels de modèle, de durée et de jetons.
- API de script et interface utilisateur : `ai.agent.run(goal, options)` renvoie un handle `AgentRun` avec événements, réponses et annulation ; l'application autonome offre un espace de tâches avec historique, préréglages, mémoire de préférences, paramètres et historique des versions.

### Captures

Interface anglaise réelle sur Android API 37.1 avec des tâches fictives et un modèle aux réponses programmées. Ces images illustrent l'interface, sans attester la réussite avec un modèle réel. Aucune donnée privée de compte n'est incluse. [Procédure de capture](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/README.md).

| Tableau des tâches | Détails de la tâche |
| --- | --- |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/workbench.png?raw=true" alt="Tableau des tâches" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/detail.png?raw=true" alt="Détails de la tâche" width="288" /> |
| Confirmation d'action | Saisie flottante |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/confirmation.png?raw=true" alt="Confirmation d'action" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/floating.png?raw=true" alt="Saisie flottante" width="288" /> |

******

### Installation

******

1. Installez l'APK du plugin depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) sur un appareil disposant d'AutoJs6 build 5293 ou ultérieure.
2. Ouvrez le centre de plugins d'AutoJs6, vérifiez que `AI Agent` est reconnu et activez-le. Les paquets officiels passent automatiquement la vérification de signature.

Installez et activez [3-Stone AI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stone-AI), puis configurez-y un modèle en ligne ou importez un modèle local compatible. Le courtier actuel de l'hôte sélectionne 3-Stone AI; un autre Provider nécessite une intégration côté hôte. Choisissez le modèle dans AI Agent > Préréglages. Connected to AutoJs6 indique la connexion à l'hôte; le choix du modèle se fait dans les préréglages.

### Compatibilité

Android 7.0+ (API 24). La connexion nécessite AutoJs6 6.8.0 / build 5289+; l'API complète et ce guide nécessitent build 5293+. Utilisez une compilation contenant les changements Agent. Activez le service d'accessibilité de l'hôte pour agir sur l'écran. OCR est facultatif et nécessite un plugin installé, autorisé et déclaré disponible par l'hôte. AI Agent ne conserve aucun identifiant de modèle et ne possède pas de service d'accessibilité propre.

### Démarrer depuis l'interface

Ouvrez AI Agent, connectez AutoJs6, saisissez un objectif et démarrez avec le préréglage par défaut. Répondez dans la carte et consultez les détails des tâches récentes.

### Démarrer depuis un script

Exécutez ce JavaScript dans AutoJs6 après connexion de AI Agent et configuration du modèle. L'interface du plugin reçoit les questions et confirmations. Pour réutiliser une configuration, ajoutez `preset: "your-preset-name"` aux options.

```javascript
let run = ai.agent.run('Lire la version Android et rapporter la valeur observée.', {
    tools: ['observe', 'user'],
    interaction: 'plugin',
    budget: { maxSteps: 8 },
});
run.on('progress', (event) => console.log(event.message));
run.result.then(
    (result) => console.log(result.status, result.summary),
    (error) => console.error(error.code, error.message),
);
```

Vérifiez `result.status`: une promesse résolue peut contenir completed, partial, failed, blocked ou cancelled. `run.cancel()` arrête la tâche. Voir [ai.agent API](https://docs.autojs6.com/#ai) pour les modèles, événements, budgets et réponses par script.

### Enregistrer un script

Enregistrez cet exemple dans `text-counter.js`, dans le répertoire de travail AutoJs6 ou un dossier approuvé par l'hôte. Le premier JSDoc contenant `@agent` inscrit le fichier au catalogue. Demandez le nombre de caractères d'un texte; les paramètres obligatoires manquants sont demandés avant exécution.

```javascript
/**
 * @agent
 * @description Count Unicode characters in the supplied text
 * @param {string} text Text to count
 * @risk readonly
 * @confirm never
 * @timeout 10000
 */
let context = ai.agent.context();
if (!context) throw Error('Start this registered script through AI Agent');
let text = new java.lang.String(context.parameters.text);
ai.agent.result({ characters: text.codePointCount(0, text.length()) });
```

Vous pouvez aussi placer ce `project.json` à côté de `main.js`, dont le corps lit `ai.agent.context().parameters` et appelle `ai.agent.result(...)` comme ci-dessus. La déclaration du projet se place dans l'objet `agent`.

```json
{
  "name": "Text counter",
  "main": "main.js",
  "agent": {
    "id": "text-counter",
    "description": "Count Unicode characters in the supplied text",
    "parameters": {
      "type": "object",
      "properties": { "text": { "type": "string" } },
      "required": ["text"],
      "additionalProperties": false
    },
    "risk": "readonly",
    "confirm": "never",
    "timeoutMs": 10000
  }
}
```

Les types acceptés sont string, number, integer et boolean; les objets imbriqués et tableaux ne sont pas pris en charge. Les scripts sensitive exigent toujours une confirmation préalable. N'enregistrez que des scripts relus: la déclaration du risque ne crée pas de bac à sable JavaScript. [Format complet](https://github.com/SuperMonster003/AutoJs6/blob/master/docs/dev/agent-script-manifest-v1.md).

### Catalogue des outils

Ce tableau provient du ToolCatalog embarqué. La cible réelle à l'écran peut accroître le risque; le mode prudent confirme aussi les actions autres que la lecture seule. Paramètres, préréglages, options et autorisations de l'hôte limitent les groupes disponibles.

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

### Préréglages et mémoire

Ouvrez les préréglages depuis les tâches pour enregistrer une configuration. Les noms identifient les scripts et les portées mémoire; dupliquez pour utiliser un autre nom. Le préréglage intégré default peut être modifié mais pas supprimé. Choisissez un modèle du catalogue de l'hôte ou la sélection automatique. Un modèle choisi indisponible provoque un échec sans substitution. Les options de tâche peuvent seulement réduire les limites du préréglage. Les contextes fixe et de tâche partagent 8 KiB. La mémoire peut inclure les entrées globales et celles du préréglage, un seul ensemble, ou aucun. Modifier ou supprimer un préréglage ne change pas les tâches en attente. Stockage privé: 32 préréglages / 1 MiB maximum.

Ouvrez Mémoire pour consulter, modifier, supprimer ou sauvegarder les préférences. Limites: 500 entrées / 256 KiB, avec portée, tâche source et dates. Confirmez chaque memory_propose et chaque entrée importée. Créez d'abord les préréglages manquants. L'injection automatique conserve les entrées complètes les plus récentes de la portée autorisée, jusqu'à 4 KiB; le préréglage courant prime sur une clé globale identique. memory: false désactive uniquement l'injection. Désactivez aussi le groupe memory ou la portée pour bloquer recherches et propositions. L'export contient les valeurs réelles et leur origine. Ne stockez pas de secrets; les clés et formats de jetons reconnaissables sont refusés.

### Utilisation

- Configurez les dossiers supplémentaires dans "Dossiers de scripts" du lanceur, un chemin absolu par ligne. L'hôte valide et applique les chemins enregistrés; les tâches peuvent seulement restreindre ces dossiers.
- 200 tâches / 32 MiB au maximum. Les tâches terminées consultées le moins récemment sont supprimées en premier. Relancer remplit l'objectif et le préréglage d'origine dans le tableau de tâches. Vérifiez-les puis appuyez sur Démarrer. Vider l'historique conserve les tâches en cours. L'export conserve les compteurs, les noms des outils et les confirmations. Les objectifs, paramètres, observations et résultats des scripts sont retirés. Choisissez un emplacement.
- Répondez dans les tâches au premier plan, ou ouvrez la notification prioritaire en arrière-plan. La confirmation affiche outil, paramètres, risque et temps restant. Une autorisation répétée reste limitée à cet outil et ce risque dans cette tâche; paiements et mémoire demandent toujours une confirmation individuelle. Mémoriser une réponse crée une proposition memory_propose séparée dans la portée autorisée. Une confirmation attend normalement 120 secondes, une question jusqu'à 10 minutes, dans le budget de la tâche. Un délai expiré renvoie USER_TIMEOUT; le modèle choisit de redemander ou de signaler un résultat partiel. Les anciens liens ne répondent pas aux nouvelles demandes. Les permissions et canaux contrôlent les notifications.
- Ouvrez les paramètres depuis les tâches pour choisir groupes, budgets, mode prudent, saisie vocale et profil par défaut. Les changements concernent les nouvelles tâches. gesture/files/shell sont désactivés initialement; OCR exige un plugin autorisé et disponible sur le service hôte. Un budget vide reprend les valeurs initiales, dans les limites du protocole. Profils et options ne peuvent que les réduire. La gestion affiche nombres et octets; effacer une catégorie exige confirmation et aucune tâche active. Effacer les profils restaure default. Dossiers de scripts, licences et source sont accessibles.
- Historique et mentions légales sont disponibles hors ligne. La vérification GitHub Releases est manuelle, avec cache de succès de 24 heures, annulation et version ignorée. La boîte ouvre les notes internes ou la page de publication dans le navigateur. Aucune vérification automatique ni téléchargement APK.
- Activez la bulle dans les paramètres, autorisez la superposition et enregistrez. Désactivée par défaut, elle apparaît seulement avec AutoJs6 connecté et se masque au verrouillage ou à la déconnexion, sans service de premier plan au repos. Déplacez-la par glissement et touchez-la pour saisir un objectif, choisir un préréglage, répondre ou arrêter. Réduire la carte rétablit les notifications de confirmation. Partagez du texte brut, utilisez Nouvelle tâche ou épinglez un préréglage avec un objectif facultatif. Chaque entrée ouvre un brouillon modifiable et exige de démarrer explicitement. Aucun remplacement silencieux des préréglages supprimés. La reconnaissance vocale suit la langue de l'interface, se masque si indisponible et remplit le texte sans envoyer.

### Questions fréquentes

**Pourquoi AutoJs6 est-il nécessaire?**

Le plugin gère la boucle et l'interface. AutoJs6 gère les modèles, les actions d'accessibilité et les scripts enregistrés. Sans hôte compatible connecté, l'historique reste lisible mais aucune nouvelle tâche sur l'appareil ne peut démarrer. La perte de l'hôte bloque les tâches; la reconnexion ne les rejoue jamais automatiquement.

**Pourquoi confirmer chaque paiement?**

Le paiement est une action sensible distincte. Approuver une commande, un script ou des actions similaires n'autorise pas un paiement. Chaque action de paiement détectée exige sa propre confirmation; un délai expiré vaut refus. Vérifiez le marchand, les produits, l'adresse et le montant avant approbation.

**Quelles sont les limites des modèles locaux?**

Les tâches dépendent du respect des consignes, de décisions JSON valides et du contexte disponible. Un petit modèle peut échouer après un chargement réussi; le cas Wi-Fi consigné pour Gemma 4 E2B IT a échoué à la validation des décisions. Commencez par des tâches simples et consultez les résultats partial/failed. 1.0.0 utilise le texte des noeuds/OCR et une boucle JSON; vision, appels natifs aux outils et scripts générés restent prévus pour 1.1.0.

******

### Permissions et sécurité

******

Le plugin respecte des limites explicites :

- Les entrées Binder exigent la permission de signature org.autojs.permission.PLUGIN. Le lanceur (raccourcis inclus) et la cible de partage text/plain ACTION_SEND sont publics et acceptent seulement des brouillons bornés. Un Intent externe ne peut exécuter une tâche, confirmer une action ou modifier les autorisations. Les paramètres, résultats vocaux et commandes restent privés.
- Le plugin ne détient aucune clé d'API, ne se lie jamais à un fournisseur de modèle et ne demande pas la permission d'accessibilité : les appels de modèle et les actions sur l'appareil passent par des courtiers qu'AutoJs6 prête pour un lien attaché et révoque au détachement, chacun borné par une autorisation (méthodes permises, débits, tailles, quota de modèle).
- INTERNET sert uniquement aux vérifications manuelles sur GitHub. FOREGROUND_SERVICE et FOREGROUND_SERVICE_SPECIAL_USE servent aux tâches actives; POST_NOTIFICATIONS à leur progression et aux confirmations. SYSTEM_ALERT_WINDOW est demandé seulement à l'activation de la bulle dans les paramètres. Aucune permission d'accessibilité, de stockage ou de microphone.
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
minimum host build: 5289 (6.8.0)
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

_2026/09/25_

- `Note` Aperçu de développement: les écrans P6, les paramètres, la bulle flottante, le partage, les raccourcis et les brouillons vocaux sont disponibles. ai.agent nécessite AutoJs6 build 5293 ou ultérieur. La fiabilité et la publication restent à valider dans P7/P8.
- `Fonctionnalité` Bulle facultative avec saisie, progression, arrêt et confirmation, partage de texte, raccourcis statiques et de préréglages, reconnaissance vocale système sans envoi automatique
- `Fonctionnalité` Paramètres globaux, gestion des catégories de données, historique et mentions légales hors ligne, vérification manuelle annulable avec cache quotidien et versions ignorées
- `Fonctionnalité` Confirmation dans les tâches et notifications avec risque, compte à rebours, autorisation par tâche et mémoire des réponses confirmée séparément
- `Fonctionnalité` Mémoire des préférences avec confirmation de chaque proposition, recherche par portée, protection des conflits, stockage par entrée, modification, suppression et sauvegarde JSON avec approbation individuelle des imports
- `Fonctionnalité` Préréglages nommés avec création, modification, duplication, suppression et choix par défaut; catalogue des modèles avec localisation et prise en charge du JSON structuré, contexte fixe, restrictions des outils et budgets, confirmation, dossiers approuvés et portée mémoire
- `Fonctionnalité` Chronologie complète et résultats, filtres par état/préréglage/date, brouillons de relance, suppression, export JSON expurgé et historique privé versionné avec migration et nettoyage LRU (200 tâches / 32 MiB)
- `Fonctionnalité` Tableau des tâches avec lancement commun, apparence de l'hôte, interactions intégrées, budgets et 20 tâches récentes consultables hors connexion
- `Fonctionnalité` La fin de tâche exige des preuves, les résultats partiels listent le travail restant et les commandes ou paiements exigent un état de commande observé
- `Fonctionnalité` La vérification conserve le compteur d'écrans inchangés après réduction du contexte et bloque la troisième demande d'action équivalente avant exécution
- `Fonctionnalité` Attente bornée de stabilité après une action et résumé des changements depuis la dernière action dans les observations suivantes
- `Fonctionnalité` Actions liées aux noeuds inspectés par l'hôte, avec ajout de texte, défilement borné et retour du résultat et des changements de fenêtre
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
- `Correctif` Zones tactiles des formulaires et filtres, retour à la ligne des choix et colonnes de paramètres, et commandes flottantes avec les grandes polices et sur Android 7
- `Correctif` Contournements de la validation des identifiants dans la mémoire des préférences avec des caractères pleine chasse, sans chasse et certains noms supplémentaires
- `Correctif` La bulle de tâche pouvait rester masquée au réveil sans verrouillage sécurisé, avant la stabilisation de l'état de l'écran
- `Correctif` Les tâches interrompues par la fin du processus du plugin sont marquées en échec au redémarrage; un écran verrouillé bloque les actions suivantes
- `Correctif` Les outils de fichiers rejettent les traversées de répertoires et les chemins absolus ou invalides avant confirmation ou envoi à l'hôte; l'historique conserve des catégories de rejet bornées sans le texte rejeté du modèle
- `Correctif` La confirmation revient dans l'application cible avant de reprendre les actions, traite les accusés après l'arrêt de l'écran et replie la carte flottante avant l'exécution
- `Correctif` Le lancement sous Android 13 ne plante plus lors de la lecture du contrôleur des barres système avant la création de la vue de fenêtre
- `Correctif` Tri et conservation de l'historique selon le début des tâches pour éviter que la réécriture des fichiers au redémarrage supprime les plus récentes
- `Correctif` Les réponses et confirmations vérifient le propriétaire interaction afin qu'un script ne réponde pas à la place de l'interface du plugin
- `Correctif` Les boutons de confirmation de transaction exigent une confirmation de paiement distincte sans réutiliser les autorisations de toute la tâche
- `Correctif` Les résultats hors écran aux limites vides ou inversées conservent leur texte et signalent des coordonnées inutilisables au lieu d'une erreur de paramètres
- `Correctif` La relocalisation des noeuds distingue les limites et capacités des conteneurs pour ne pas confondre les conteneurs imbriqués avec la cible
- `Correctif` Indications précises pour corriger les cibles de noeuds: conserver le préfixe # et omettre snapshotId avec selector
- `Correctif` L'admission précharge les règles de commande et évite leur compilation coûteuse
- `Correctif` La vérification distingue les noeuds de fenêtres différentes, conserve l'obligation d'observer après lecture du presse-papiers et ne confond plus transfert de fichiers et paiement
- `Correctif` Une lecture de l'écran sans réponse après une action ne dépasse plus le délai de stabilisation
- `Correctif` Expurgation des paramètres multilignes avant le découpage de la console, sans laisser passer de secret quand un paramètre correspond à son libellé
- `Correctif` Un service de premier plan en cours de fermeture ne rejette plus le démarrage de la tâche suivante
- `Amélioration` La réduction des longs historiques réutilise les fragments inchangés des instructions et observations pour réduire le temps de traitement par étape
- `Amélioration` La taille des descriptions de confirmation tient compte des échappements JSON pour respecter la limite des événements Binder avec de grands tableaux
- `Amélioration` L'hôte minimum est AutoJs6 6.8.0 / build 5289 pour inspecter les noeuds et lier la confirmation à l'exécution
- `Dépendance` Ajout de common-plugin-api, host-capability-api et ai-agent-api provenant du même build release AutoJs6 6.8.0 / 5289 (MPL 2.0), verrouillés par SHA-256
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
