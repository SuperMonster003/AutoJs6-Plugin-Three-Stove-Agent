<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-stove-agent-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Exécute des tâches en langage naturel dans AutoJs6 en choisissant des scripts enregistrés et en manipulant l'écran étape par étape</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Langues

******

Le README.md actuel prend en charge les langues suivantes:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-en.md)
- Français [fr] # actuel
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-ar.md)

******

### Introduction

******

3-Stove Agent transforme un objectif en langage naturel en actions sur un appareil Android exécutant AutoJs6. Soit il choisit un script que l'utilisateur a enregistré pour l'agent, complète ses paramètres et l'exécute ; soit il observe l'écran à travers l'arbre de noeuds d'accessibilité et agit étape par étape (observer, décider, agir, vérifier) jusqu'à ce que l'objectif soit atteint, qu'une confirmation soit nécessaire ou qu'un budget soit épuisé. Il répond à la [discussion AutoJs6 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

3-Stove Agent fournit une interface autonome et un plugin AutoJs6 accessible par ai.agent. Les actions intégrées et les appels de modèle passent par AutoJs6. Les outils MCP facultatifs utilisent uniquement les serveurs configurés. Aucun accès direct au fournisseur de modèle ni permission d'accessibilité.

******

### État

******

La version 1.2.0 apporte les outils MCP facultatifs, les appels natifs, l'observation par captures et les scripts générés, validés sur cinq appareils réels et sur les émulateurs API 24 / 35 / 36.1. Limites connues: les petits modèles locaux (Gemma 4 E2B / E4B) décident mal; les échecs après un changement de réseau du VPN avec la connexion automatique par défaut restent non résolus; aucune tâche visuelle complète entre UID n'a été validée, AiGoCode gpt-5.6-sol n'a réussi que les tests d'image initiale et d'image dans un résultat d'outil. Voir [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/ROADMAP.md).

******

### Fonctionnalités

******

L'implémentation actuelle propose les fonctions suivantes:

- Sélection de scripts : les scripts enregistrés via `project.json` ou un commentaire d'en-tête `@agent` sont présentés au modèle avec leurs descriptions et schémas de paramètres ; l'agent en choisit un, complète les paramètres, demande confirmation si nécessaire, l'exécute dans AutoJs6 et lit son résultat structuré.
- Manipulation de l'écran étape par étape : l'agent observe l'arbre de noeuds d'accessibilité sous forme de texte compact (et le texte de l'écran via un plugin OCR lorsqu'il est installé), puis clique, saisit, fait défiler et appuie sur des touches via le courtier de capacités d'AutoJs6 jusqu'à pouvoir vérifier l'objectif.
- Sécurité par conception : les outils en lecture seule s'exécutent automatiquement, les actions sensibles (paiement, envoi, suppression, écriture de fichiers, shell, gestes par coordonnées, scripts enregistrés comme sensibles) nécessitent par défaut une confirmation, et chaque exécution a des budgets d'étapes, d'appels de modèle, de durée et de jetons. Une confirmation peut valoir une fois ou jusqu'à la fin de la tâche; les paramètres proposent aussi le mode prudent et l'accès complet, qui saute les confirmations et reste clairement signalé. La liste des applications de paiement et la table des mots-clés sensibles peuvent être étendues depuis l'écran de réglages Reconnaissance des risques; les entrées intégrées ne peuvent pas être retirées.
- API de script et interface utilisateur : `ai.agent.run(goal, options)` renvoie un handle `AgentRun` avec événements, réponses et annulation ; l'application autonome offre un espace de tâches avec historique, préréglages, mémoire de préférences, paramètres et historique des versions.
- Appels natifs via l'hôte: schémas du catalogue, validation du lot entier, exécution séquentielle, confirmations individuelles, retour des résultats et journal commun
- Observation par capture via AutoJs6 sur Android 11+: screen_capture limite le grand côté à 1280 et utilise JPEG qualité 70, avec instructions visuelles, budget de tokens image et images dans les résultats des outils natifs
- JavaScript généré via script_run_source : le groupe script_dynamic est désactivé par défaut. Chaque appel montre un résumé du code extensible au texte complet, à approuver une fois ou pour la tâche en cours; l'accès complet saute cette revue. Exécution avec délai, annulation, résultats structurés et code dans les étapes privées. Le code UTF-8 et sa chaîne JSON sont chacun limités à 8 KiB.
- Outils MCP de serveurs locaux ou externes choisis, avec un niveau de risque par serveur et le groupe mcp désactivé par défaut
- Application autonome repensée en Material 3: l'accueil est un fil de tâches avec la zone de saisie ancrée au-dessus du clavier, une barre supérieure avec la capsule de modèle, l'historique et un menu (Nouvelle tâche, Préréglages, Mémoire, Dossiers de scripts, Serveurs MCP, Paramètres), un bandeau seulement tant qu'AutoJs6 n'est pas connecté, une chronologie des étapes mise à jour par étape, et Relancer, qui remplit la saisie sans démarrer. Paramètres organisés en sections claires et apparence claire/sombre cohérente

### Captures

Interface anglaise réelle sur Android API 37.1 avec des tâches fictives et un modèle aux réponses programmées. Ces images illustrent l'interface, sans attester la réussite avec un modèle réel. Aucune donnée privée de compte n'est incluse. [Procédure de capture](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/README.md).

| Tableau des tâches | Détails de la tâche |
| --- | --- |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/workbench.png?raw=true" alt="Tableau des tâches" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/detail.png?raw=true" alt="Détails de la tâche" width="288" /> |
| Confirmation d'action | Saisie flottante |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/confirmation.png?raw=true" alt="Confirmation d'action" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/floating.png?raw=true" alt="Saisie flottante" width="288" /> |

******

### Installation

******

1. Installez l'APK du plugin depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/releases) sur un appareil disposant d'AutoJs6 build 5293 ou ultérieure.
2. Ouvrez le centre de plugins AutoJs6 et vérifiez que `3-Stove Agent` est reconnu. Les paquets officiels passent la vérification de signature et s'activent après installation sans confirmation; un plugin désactivé explicitement le reste. Ce centre est le seul interrupteur. Les scripts et l'interface se connectent à la demande, sans lancement au démarrage ni reprise d'anciennes tâches.

Installez et activez [3-Stone AI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stone-AI), puis configurez-y un modèle en ligne ou importez un modèle local compatible. Le courtier actuel de l'hôte sélectionne 3-Stone AI; un autre Provider nécessite une intégration côté hôte. Choisissez le modèle avec la capsule de modèle de l'accueil d'3-Stove Agent. Un bandeau n'y apparaît que tant qu'AutoJs6 n'est pas connecté.

### Compatibilité

Android 7.0+ (API 24). Nécessite un hôte AutoJs6 6.8.0 / build 5298+, qui contient déjà tous les changements requis par l'API des tâches (build 5293+) ainsi que par les appels natifs et l'entrée d'images (build 5297+). Les actions à l'écran exigent le service d'accessibilité de l'hôte; Agent le démarre d'abord avec la méthode automatique configurée dans AutoJs6 (Root, paramètres sécurisés ou Shizuku) et ne vous demande de l'activer qu'en cas d'échec. OCR est facultatif et nécessite un plugin installé, autorisé et déclaré disponible par l'hôte. 3-Stove Agent ne conserve aucun identifiant de modèle et ne possède pas de service d'accessibilité propre.

### Démarrer depuis l'interface

Ouvrez 3-Stove Agent, connectez AutoJs6, saisissez un objectif et démarrez. La capsule de modèle de l'accueil choisit un modèle en ligne ou local, ou Automatique (un modèle sur l'appareil d'abord, sinon le premier disponible). Recherchez, épinglez vos modèles favoris et réutilisez les récents; des badges indiquent la prise en charge déclarée des outils et des images. Le plan de travail et la bulle flottante partagent ce choix pour les nouvelles tâches, sans modifier les préréglages ni la tâche en cours; les préréglages ne contiennent plus de modèle. La puce de préréglage de la zone de saisie choisit un préréglage facultatif. Répondez aux questions et suivez la progression dans la carte de tâche.

### Démarrer depuis un script

Exécutez ce JavaScript dans AutoJs6 après connexion de 3-Stove Agent et configuration du modèle. L'interface du plugin reçoit les questions et confirmations. Pour réutiliser une configuration, ajoutez `preset: "your-preset-name"` aux options. Ajoutez `plan: true` pour que la tâche propose d'abord un plan à vérifier.

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
if (!context) throw Error('Start this registered script through 3-Stove Agent');
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

Les types acceptés sont string, number, integer et boolean; les objets imbriqués et tableaux ne sont pas pris en charge. Les scripts sensitive exigent une confirmation préalable, sauf en accès complet. N'enregistrez que des scripts relus: la déclaration du risque ne crée pas de bac à sable JavaScript. [Format complet](https://github.com/SuperMonster003/AutoJs6/blob/master/docs/dev/agent-script-manifest-v1.md).

### Catalogue des outils

Ce tableau provient du ToolCatalog embarqué. La cible réelle à l'écran peut accroître le risque; le mode prudent confirme aussi les actions autres que la lecture seule, tandis que l'accès complet saute la confirmation des outils activés. Paramètres, préréglages, options et autorisations de l'hôte limitent les groupes disponibles.

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
| `app_installed` | `observe` | `READ_ONLY` | `on` | Check whether an application package is installed. |
| `app_list` | `observe` | `READ_ONLY` | `on` | List installed applications, optionally filtered by a package name or label fragment; the result is bounded. |
| `console_tail` | `observe` | `READ_ONLY` | `on` | Read bounded recent console lines; they may include unrelated scripts. |
| `device_info` | `observe` | `READ_ONLY` | `on` | Read device information. |
| `screen_capture` | `observe` | `READ_ONLY` | `auto (vision)` | Capture the unlocked screen for the selected vision model when text nodes are insufficient. Returns a scaled JPEG observation, not device coordinates. |
| `screen_state` | `observe` | `READ_ONLY` | `on` | Read whether the screen is on. |
| `ui_dump` | `observe` | `READ_ONLY` | `on` | Observe the current accessibility tree before choosing an action. |
| `ui_find` | `observe` | `READ_ONLY` | `on` | Find nodes matching all selector conditions. |
| `ui_wait_for` | `observe` | `READ_ONLY` | `on` | Wait for a selector to appear or disappear within a deadline. |
| `ocr_screen` | `ocr` | `READ_ONLY` | `auto (OCR)` | Read screen text through the host OCR plugin. |
| `script_catalog` | `script` | `READ_ONLY` | `on` | Find scripts explicitly registered for Agent use. |
| `script_list` | `script` | `READ_ONLY` | `on` | List script executions currently running in AutoJs6 with their ids and states. |
| `script_run` | `script` | `NORMAL` | `on` | Run a registered script by id with validated parameters and its registered risk. |
| `script_stop` | `script` | `NORMAL` | `on` | Stop an owned script execution. |
| `script_run_source` | `script_dynamic` | `SENSITIVE` | `off` | Run generated Rhino JavaScript with host script privileges after individual source approval. No sandbox. Source including JSON escaping <=8192 UTF-8 bytes. Use ai.agent.result(value) for results. |
| `shell_exec` | `shell` | `SENSITIVE` | `off` | Execute a bounded non-root shell command after confirmation. |
| `report_progress` | `user` | `READ_ONLY` | `on` | Report bounded progress without declaring task completion. |

### Préréglages et mémoire

Ouvrez les préréglages depuis les tâches pour enregistrer une configuration. Les noms identifient les scripts et les portées mémoire; dupliquez pour utiliser un autre nom. Le préréglage intégré default peut être modifié mais pas supprimé. Les préréglages n'incluent pas de modèle; un modèle enregistré par une version antérieure est conservé pour les scripts uniquement. Les options de tâche peuvent seulement réduire les limites du préréglage. Les contextes fixe et de tâche partagent 8 KiB. La mémoire peut inclure les entrées globales et celles du préréglage, un seul ensemble, ou aucun. Modifier ou supprimer un préréglage ne change pas les tâches en attente. Stockage privé: 32 préréglages / 1 MiB maximum. Les préréglages peuvent être exportés en JSON et importés après un examen un par un; le fichier ne contient aucun modèle, et les groupes d'outils ou dossiers de scripts absents de cet appareil sont écartés à l'import. Le mode plan fait proposer au modèle 3 à 8 étapes que vous vérifiez, et pouvez modifier, avant l'exécution, puis un nouveau plan quand l'ancien ne convient plus; il est désactivé par défaut.

Ouvrez Mémoire pour consulter, modifier, supprimer ou sauvegarder les préférences. Limites: 500 entrées / 256 KiB, avec portée, tâche source et dates. Confirmez chaque memory_propose et chaque entrée importée. Créez d'abord les préréglages manquants. L'injection automatique conserve les entrées complètes les plus récentes de la portée autorisée, jusqu'à 4 KiB; le préréglage courant prime sur une clé globale identique. memory: false désactive uniquement l'injection. Désactivez aussi le groupe memory ou la portée pour bloquer recherches et propositions. L'export contient les valeurs réelles et leur origine. Ne stockez pas de secrets; les clés et formats de jetons reconnaissables sont refusés.

### Utilisation

- Configurez les dossiers supplémentaires dans "Dossiers de scripts" du lanceur, un chemin absolu par ligne. L'hôte valide et applique les chemins enregistrés; les tâches peuvent seulement restreindre ces dossiers.
- 200 tâches / 32 MiB au maximum. Les tâches terminées consultées le moins récemment sont supprimées en premier. Relancer remplit l'objectif et le préréglage d'origine dans le tableau de tâches. Vérifiez-les puis appuyez sur Démarrer. Vider l'historique conserve les tâches en cours. L'export conserve les compteurs, les noms des outils et les confirmations. Les objectifs, paramètres, observations et résultats des scripts sont retirés. Choisissez un emplacement. Partager le résumé sur la page de détails remet à la feuille de partage du système l'objectif, l'état, le résumé, les preuves et le travail inachevé, jamais les observations.
- Répondez dans les tâches au premier plan, ou ouvrez la notification prioritaire en arrière-plan. La confirmation affiche outil, paramètres, risque et temps restant. Toujours autoriser pour cette session accepte le même outil au même niveau de risque jusqu'à la fin de la tâche, y compris les propositions de mémoire ou codes générés suivants; les paiements demandent leur propre accord. Mémoriser une réponse crée une proposition memory_propose séparée dans la portée autorisée. Une confirmation attend normalement 120 secondes, une question jusqu'à 10 minutes, dans le budget de la tâche. Un délai expiré renvoie USER_TIMEOUT; le modèle choisit de redemander ou de signaler un résultat partiel. Les anciens liens ne répondent pas aux nouvelles demandes. Les permissions et canaux contrôlent les notifications.
- Ouvrez les paramètres depuis les tâches pour choisir groupes, budgets, autorisations des opérations (standard, prudent ou accès complet), saisie vocale et profil par défaut. Chaque changement est enregistré immédiatement et concerne les nouvelles tâches. L'accès complet exécute les outils activés, paiements compris, sans approbation; les tâches, la bulle flottante et l'historique affichent alors un avertissement. gesture/files/shell/script_dynamic sont désactivés initialement; OCR exige un plugin autorisé et disponible sur le service hôte. Une limite en mode automatique reprend la valeur initiale; la durée se saisit en minutes, dans les limites du protocole. Profils et options ne peuvent que les réduire. La gestion affiche nombres et octets; effacer une catégorie exige confirmation et aucune tâche active. Effacer les profils restaure default. Profils, mémoire, dossiers de scripts et serveurs MCP s'ouvrent aussi depuis les paramètres. Les alertes de tâche regroupent les échecs et les fins normales en deux lignes résumées, chacune ouvrant les choix indépendants notification, toast et dialogue. Par défaut, les échecs utilisent les trois et les fins utilisent notification et toast; les choix enregistrés sont conservés. Une fin normale, même partielle sans erreur, alerte une fois; annulation et consultation de l'historique restent silencieuses. Sur le plan de travail, la puce d'acces change directement les autorisations, la puce de preselection ouvre un panneau qui choisit ou gere les preselections et le menu superieur active la bulle flottante.
- Ouvrez les paramètres depuis le menu en haut à droite. La langue, le mode sombre et la couleur peuvent suivre AutoJs6 ou être définis séparément. La langue et le mode sombre peuvent aussi suivre Android. Historique et mentions légales sont disponibles hors ligne. Les vérifications manuelles GitHub gardent les résultats réussis 24 heures. Les vérifications automatiques sont désactivées par défaut. Une fois activées, elles ont lieu pendant l'utilisation, au plus toutes les 12 heures, sans signaler les échecs ou versions ignorées et sans télécharger d'APK. La gestion des mises à jour ignorées permet de rétablir chaque version. La page À propos indique la version, le développeur, le code source, la licence et les mentions tierces. Unifier les réglages avec des groupes plats, des lignes cohérentes et des dialogues arrondis centrés. La langue, le mode nuit, la couleur et l'icône ne changent qu'après validation; Annuler conserve les valeurs enregistrées. La couleur suit AutoJs6 par défaut et propose une palette commune, une saisie HEX/RGB et un aperçu local. Les fonds neutres restent stables et les contrôles suivent le thème. L'icône utilise le mode adaptatif automatique par défaut, tout en préservant les choix explicites lors des mises à jour.
- Activez la bulle dans les paramètres, puis autorisez la superposition. Désactivée par défaut, elle apparaît seulement avec AutoJs6 connecté et se masque au verrouillage ou à la déconnexion, sans service de premier plan au repos. Déplacez-la par glissement et touchez-la pour saisir un objectif, choisir un préréglage, répondre ou arrêter. Réduire la carte rétablit les notifications de confirmation. Partagez du texte brut, utilisez Nouvelle tâche ou épinglez un préréglage avec un objectif facultatif. Chaque entrée ouvre un brouillon modifiable et exige de démarrer explicitement. Aucun remplacement silencieux des préréglages supprimés. La reconnaissance vocale suit la langue de l'interface, se masque si indisponible et remplit le texte sans envoyer.

### Questions fréquentes

**Pourquoi AutoJs6 est-il nécessaire?**

Le plugin gère la boucle et l'interface. AutoJs6 gère les modèles, les actions d'accessibilité et les scripts enregistrés. Sans hôte compatible connecté, l'historique reste lisible mais aucune nouvelle tâche sur l'appareil ne peut démarrer. La perte de l'hôte bloque les tâches; la reconnexion ne les rejoue jamais automatiquement.

**Quand les paiements sont-ils confirmés?**

Le paiement est une action sensible distincte. Approuver une commande, un script ou une autre action n'autorise pas un paiement. Par défaut, chaque action de paiement détectée exige sa propre confirmation; un délai expiré vaut refus. Choisir Toujours autoriser pour cette session sur une demande de paiement ne couvre que les paiements suivants de cet outil dans la même tâche. L'accès complet saute la confirmation des paiements: activez-le seulement pour des objectifs et modèles de confiance. Vérifiez le marchand, les produits, l'adresse et le montant avant approbation.

**Quelles sont les limites des modèles locaux?**

Un modèle chargé ne garantit pas la réussite. Le test enregistré de validation des décisions Wi-Fi avec Gemma 4 E2B IT a échoué; cette cible conserve JSON. Les appels natifs nécessitent un hôte et une cible compatibles, avec les mêmes validations, confirmations et budgets. Commencez par de petites tâches et examinez les résultats partial/failed. Les images exigent une cible compatible; AiGoCode gpt-5.6-sol a réussi les tests avec image initiale et image dans un résultat d'outil, les autres cibles nécessitant une vérification distincte. Les scripts générés exigent une activation explicite, et chaque code suit la règle de confirmation.

******

### Permissions et sécurité

******

Le plugin respecte des limites explicites :

- Liste des permissions: org.autojs.permission.PLUGIN (points d'entree du contrat hote), FOREGROUND_SERVICE et FOREGROUND_SERVICE_SPECIAL_USE (service au premier plan pendant une tache), POST_NOTIFICATIONS (confirmations et progression en arriere-plan), INTERNET (verification manuelle ou automatique des versions GitHub et connexion aux serveurs MCP configures par l'utilisateur), ACCESS_LOCAL_NETWORK (demande uniquement depuis les reglages MCP sur Android 17+), SYSTEM_ALERT_WINDOW (demande uniquement lorsque la bulle flottante est activee dans les Reglages). Aucune permission d'accessibilite, de stockage ou de micro n'est demandee, et le trafic du modele ne passe jamais par le plugin.
- Les entrées Binder exigent la permission de signature org.autojs.permission.PLUGIN. Le lanceur (raccourcis inclus) et la cible de partage text/plain ACTION_SEND sont publics et acceptent seulement des brouillons bornés. Un Intent externe ne peut exécuter une tâche, confirmer une action ou modifier les autorisations. Les paramètres, résultats vocaux et commandes restent privés.
- 3-Stove Agent fournit une interface autonome et un plugin AutoJs6 accessible par ai.agent. Les actions intégrées et les appels de modèle passent par AutoJs6. Les outils MCP facultatifs utilisent uniquement les serveurs configurés. Aucun accès direct au fournisseur de modèle ni permission d'accessibilité.
- Les identifiants du modèle restent dans son fournisseur; AutoJs6 transmet les appels. Les jetons MCP Bearer sont chiffrés avec Android Keystore dans le stockage privé et exclus des prompts et exports historiques. INTERNET sert aussi aux serveurs MCP configurés; sur Android 17+, la permission réseau local se demande depuis les paramètres MCP. Le risque choisi par serveur est initialement SENSITIVE. Annuler ne rétablit pas les actions distantes; aucun appel échoué ne se rejoue automatiquement.
- L'historique des tâches, les préréglages et la mémoire de préférences restent dans le stockage privé du plugin ; les sauvegardes et les transferts d'appareil sont désactivés.
- Les captures sont envoyées via AutoJs6 au modèle choisi, éventuellement en ligne. L'écran doit être actif et déverrouillé. L'historique conserve les dimensions et le nombre d'octets, sans contenu image. Les décisions JSON gardent l'image courante jusqu'à une autre observation ou réponse. Les conversations natives conservent les images précédentes dans les limites du lot et de la session, avec une nouvelle réservation de tokens à chaque tour.
- Les scripts générés utilisent les autorisations AutoJs6 sans bac à sable JavaScript et peuvent agir hors des groupes activés. Le code complet est conservé dans les étapes privées, sous réserve du masquage des mots de passe et de la rétention. Un code modifié par un masquage ultérieur ne peut être enregistré comme original. Vérifiez les .js avant de les partager.
- L'accès complet ne s'active que dans les paramètres privés du plugin; ni la sortie du modèle, ni l'écran, ni une demande de script, ni un Intent externe ne peuvent l'activer ou l'étendre. Il saute les confirmations des outils activés, paiements compris, sans activer d'autres groupes ni relâcher budgets et autorisations de l'hôte. L'accessibilité est démarrée par l'hôte selon la méthode configurée dans AutoJs6; le plugin ne demande toujours aucune autorisation d'accessibilité.

N'obtenez le plugin que depuis la page officielle [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/releases) ou le centre de plugins d'AutoJs6. Les paquets de sources inconnues peuvent échouer à la vérification de l'hôte ou présenter des risques même lorsque le numéro de version semble identique.

******

### Interface du plugin

******

Les informations suivantes s'adressent aux développeurs de l'hôte AutoJs6 et de plugins ; l'hôte utilise ces identifiants pour découvrir le plugin et négocier la compatibilité:

```text
application id: io.github.supermonster003.autojs6.plugin.three.stove.agent
plugin id: three-stove-agent
engine: three-stove-agent
variant: default
service action: org.autojs.plugin.THREE_STOVE_AGENT
service category: three-stove-agent
service process: :agent
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.three.stove.agent.api.IThreeStoveAgentPlugin
minimum host build: 5298 (6.8.0)
```

`ThreeStoveAgentPluginService` / `IThreeStoveAgentPlugin` / `IThreeStoveAgentLink`: Connexion avec identité du programme hôte vérifiée, file de tâches, réponses, annulation, requêtes et historique privé; tâches bloquées après déconnexion et aucun redémarrage automatique après arrêt du processus.

******

### Feuille de route

******

Les plans et l'avancement du plugin sont tenus sous forme de liste cochable dans ROADMAP.md, organisée par phase avec des critères d'acceptation et des niveaux de preuve. Les éléments non cochés expriment une intention et non une capacité actuelle ; les discussions via Issues sont les bienvenues.

- [Voir ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/ROADMAP.md)

******

### Historique des versions

******

#### v1.3.0

_2026/09/30_

- `Fonctionnalité` Reconnaissance des risques configurable: un nouvel écran de réglages Reconnaissance des risques ajoute vos propres noms de paquets et mots-clés en plus de la liste intégrée d'applications de paiement (Alipay, AlipayHK, UnionPay, PayPal, Google Wallet, Samsung Pay, Huawei Wallet, Mi Pay) et de la table de mots-clés sensibles en dix langues; les ajouts ne font qu'élargir les listes et s'appliquent immédiatement, et les actions à l'écran correspondantes deviennent sensibles et passent par la confirmation
- `Fonctionnalité` Refonte de la bulle flottante: en mode réduit, la tâche et son étape en cours s'affichent sur deux lignes et un appui sur le texte ouvre une carte de chronologie des étapes; la carte de contrôle est condensée sur les deux mêmes rangées que l'espace de travail (préréglage, modèle et accès au-dessus du champ, voix et démarrage), le modèle et le mode d'accès se changent dans la superposition et sont partagés avec l'application; la chronologie suit les nouvelles étapes tant que vous êtes en bas, s'arrête quand vous remontez et reprend en bas, comme l'écran des détails de la tâche
- `Fonctionnalité` Import / export des préréglages: l'écran des préréglages gagne Importer du JSON et Exporter en JSON; le fichier exporté contient la configuration de chaque préréglage mais aucun modèle, chaque préréglage importé est examiné un par un avec un avis de remplacement pour un nom existant, les groupes d'outils ou dossiers de scripts absents de cet appareil sont écartés, et un examen en cours est restauré après avoir quitté l'écran
- `Fonctionnalité` Outils d'observation complétés: les outils en lecture seule app_list (applications installées, filtrables par fragment de nom de paquet ou de libellé, 200 lignes au plus), app_installed (si un paquet est installé) et script_list (exécutions de scripts en cours dans AutoJs6 avec leurs identifiants et états, à utiliser avec script_stop) correspondent à package_manager.listApps, app.isInstalled et engines.list, que l'autorisation de l'hôte permettait déjà; ils rejoignent les groupes observation et scripts et sont activés par défaut
- `Fonctionnalité` Mode plan (un interrupteur du préréglage, désactivé par défaut; les scripts peuvent le remplacer avec options.plan): le modèle propose d'abord 3 à 8 étapes, l'écran des tâches, la bulle flottante et l'écran de confirmation affichent une carte de vérification du plan modifiable, puis après approbation l'exécution transmet le plan au modèle à chaque requête et attend qu'il soit suivi dans l'ordre; quand le plan ne convient plus, le modèle en propose un nouveau à vérifier; le schéma de décision gagne une branche plan acceptée uniquement en mode plan, et la chronologie et l'historique enregistrent les étapes du plan
- `Fonctionnalité` Partage du résultat d'une tâche: le menu des détails gagne Partager le résumé, qui remet à la feuille de partage du système un texte brut avec l'objectif, l'état et le résumé, les preuves et le travail inachevé; les observations, arguments, résultats de scripts et détails d'erreur ne quittent jamais l'historique privé, et l'entrée reste désactivée tant que la tâche s'exécute
- `Fonctionnalité` Les alertes de tâche regroupent les échecs et les fins normales en deux lignes résumées, chacune ouvrant les choix indépendants notification, toast et dialogue. Par défaut, les échecs utilisent les trois et les fins utilisent notification et toast; les choix enregistrés sont conservés. Une fin normale, même partielle sans erreur, alerte une fois; annulation et consultation de l'historique restent silencieuses
- `Fonctionnalité` Unifier les réglages avec des groupes plats, des lignes cohérentes et des dialogues arrondis centrés. La langue, le mode nuit, la couleur et l'icône ne changent qu'après validation; Annuler conserve les valeurs enregistrées. La couleur suit AutoJs6 par défaut et propose une palette commune, une saisie HEX/RGB et un aperçu local. Les fonds neutres restent stables et les contrôles suivent le thème. L'icône utilise le mode adaptatif automatique par défaut, tout en préservant les choix explicites lors des mises à jour.
- `Correctif` Le bouton d'envoi ne reste plus sur la première ligne d'un objectif multiligne: il se cale en bas comme le bouton micro
- `Correctif` En mode plan, la demande de plan ne propose plus de définitions d'outils aux modèles en ligne à appel d'outils natif: le modèle ne peut que renvoyer un plan au lieu d'appeler un outil (sur un appareil réel, un modèle de type Codex a répondu trois fois de suite par des appels d'outils et la tâche a échoué avec DECISION_UNPARSABLE); l'appel d'outils natif reprend une fois le plan approuvé
- `Correctif` Les tours de continuation de l'appel d'outils natif n'héritent plus du reste du délai de modèle de la première requête: chaque tour (le modèle renvoie des appels d'outils, les outils s'exécutent, la requête suivante part) obtient à nouveau le délai de modèle complet, et le tour entier n'est limité que par le budget de durée de la tâche et le plafond de tours. Sur un appareil réel, les tâches natives de plus de 5 minutes (par exemple lire une calculatrice après plusieurs captures d'écran) se terminaient à exactement 300 s avec MODEL_TIMEOUT. Nécessite un hôte plus récent qu'AutoJs6 5298 et un 3-Stone AI plus récent que 1.2.1, qui réinitialisent de la même façon à chaque tour
- `Correctif` La case Bulle flottante du menu Plus suit désormais la couleur du thème
- `Correctif` Conserver la ressource automatique dynamique après installation afin que le lanceur puisse charger la variante claire ou sombre correspondante.
- `Correctif` Une requête sur une exécution pouvait renvoyer brièvement un état terminal sans son résultat pendant la fin de la tâche (échec occasionnel de la suite de conformité du faux hôte sur la CI distante). L'état terminal est désormais archivé avec le résultat, les hôtes et les scripts ne voient donc jamais une tâche terminée sans résultat
- `Amélioration` Les boutons Réessayer avec un autre modèle sont retirés (le modèle se change en haut de l'espace de travail ou dans la bulle flottante); Historique des tâches et Ouvrir l'espace de travail de la bulle passent dans le menu Plus; l'icône d'historique reprend le glyphe standard
- `Amélioration` Après un tools/list_changed envoyé par un serveur MCP, les définitions d'outils gelées sont revérifiées avant l'appel suivant: les définitions inchangées continuent de fonctionner, et seule une définition modifiée échoue avec MCP_CATALOG_CHANGED en indiquant au modèle de faire actualiser la sélection d'outils dans les réglages MCP par l'utilisateur puis de relancer une tâche; la note du point de terminaison précise désormais que la connexion OAuth et l'ancien transport HTTP+SSE ne sont pas pris en charge
- `Amélioration` La migration ponctuelle introduite en 1.2.0 est supprimée: un ancien choix de modèle dans les préférences de brouillon de l'atelier n'est plus déplacé vers model-selection.json, un choix enregistré par une version antérieure doit donc être refait dans l'atelier
- `Amélioration` L'interrupteur "Activer la saisie vocale" des paramètres s'explique désormais: le microphone n'apparaît que si une application de reconnaissance vocale est installée, la reconnaissance s'y effectue et ce plugin n'enregistre rien; sur les appareils qui n'en ont pas (certaines ROM chinoises, par exemple), la raison est affichée sur place au lieu que l'atelier reste silencieusement sans microphone une fois l'interrupteur activé
- `Amélioration` Le centre de plugins devient le seul interrupteur. Les paquets officiels s'activent après installation sans confirmation, en conservant une désactivation explicite. Les scripts et l'interface se connectent à la demande, sans lancement au démarrage ni reprise de tâches
- `Amélioration` Conserver le cadre arrondi de l'icône À propos avec un intérieur transparent laissant voir le fond de la page. Afficher les choix du lanceur depuis le haut avec des notes plus petites.
- `Amélioration` Lorsqu'une tâche échoue parce que l'hôte a mis la source du modèle en fusible (RATE_LIMITED: FUSED), le résultat explique la cause et la marche à suivre (patienter et réessayer, redémarrer AutoJs6 si l'échec persiste) ; l'hôte lève désormais aussi le fusible automatiquement lorsque la transaction bloquante expirée revient, si bien que les tâches suivantes n'exigent aucun redémarrage
- `Amélioration` Taille visuelle harmonisée des icônes du lanceur et du Centre de plugins, avec des fonds transparents et des motifs noirs, blancs ou gris neutres

#### v1.2.0

_2026/09/28_

- `Note` L'application est renommee 3-Stove Agent: l'ID d'application est desormais io.github.supermonster003.autojs6.plugin.three.stove.agent, le depot est AutoJs6-Plugin-Three-Stove-Agent, l'ID et l'engine du plugin sont three-stove-agent, l'action du service est org.autojs.plugin.THREE_STOVE_AGENT et la version du contrat est 2. L'ancien nom n'est pas pris en charge: desinstallez l'ancien AI Agent avant d'installer; l'historique, les preselections et les memoires ne sont pas migres. L'hote minimum est desormais AutoJs6 6.8.0 / build 5298; les hotes anterieurs ne reconnaissent plus ce plugin
- `Note` La version 1.2.0 apporte les outils MCP facultatifs, les appels natifs, l'observation par captures et les scripts générés, validés sur cinq appareils réels et sur les émulateurs API 24 / 35 / 36.1. Limites connues: les petits modèles locaux (Gemma 4 E2B / E4B) décident mal; les échecs après un changement de réseau du VPN avec la connexion automatique par défaut restent non résolus; aucune tâche visuelle complète entre UID n'a été validée, AiGoCode gpt-5.6-sol n'a réussi que les tests d'image initiale et d'image dans un résultat d'outil. Voir ROADMAP.md.
- `Fonctionnalité` Le compositeur affiche le mode d'acces actuel (Standard / Prudent / Acces complet; seul Acces complet est en rouge) et le change au toucher; le menu superieur gagne une case "Bulle flottante" synchronisee avec le reglage; la carte flottante deployee se reduit au toucher exterieur, s'ajuste a son contenu et a un bouton Plus avec Reduire et Desactiver; le champ d'objectif d'une ligne centre son curseur avec les boutons voix et envoi, qui restent alignes en bas quand le champ grandit
- `Fonctionnalité` Panneau des preselections du plan de travail: toucher la puce de preselection ouvre un panneau qui choisit la preselection et la gere sur place (nouvelle, modifier, copier, definir par defaut, supprimer avec confirmation) ou ouvre l'ecran complet des preselections
- `Fonctionnalité` Alertes d'echec: les Reglages gagnent une section "Alertes d'echec" avec des interrupteurs independants pour une notification (activee par defaut), un message flottant et une boite de dialogue; quand une tache s'arrete sur une erreur, une limite de budget ou un hote perdu, le processus agent emet les alertes choisies, la notification ouvre les details de la tache et sous Android 10+ la boite de dialogue exige la permission de superposition ou se replie sur une notification; les taches terminees ou annulees n'alertent jamais
- `Fonctionnalité` Outils MCP de serveurs locaux ou externes choisis, avec un niveau de risque par serveur et le groupe mcp désactivé par défaut
- `Fonctionnalité` Ouvrez 3-Stove Agent, connectez AutoJs6, saisissez un objectif et démarrez. La capsule de modèle de l'accueil choisit un modèle en ligne ou local, ou Automatique (un modèle sur l'appareil d'abord, sinon le premier disponible). Recherchez, épinglez vos modèles favoris et réutilisez les récents; des badges indiquent la prise en charge déclarée des outils et des images. Le plan de travail et la bulle flottante partagent ce choix pour les nouvelles tâches, sans modifier les préréglages ni la tâche en cours; les préréglages ne contiennent plus de modèle. La puce de préréglage de la zone de saisie choisit un préréglage facultatif. Répondez aux questions et suivez la progression dans la carte de tâche.
- `Fonctionnalité` Ouvrez les paramètres depuis le menu en haut à droite. Chaque modification s'applique immédiatement, sans bouton Enregistrer: apparence, autorisations, groupes d'outils, limites (durée en minutes), saisie vocale, bulle flottante et nettoyage des données. La langue, le mode sombre et la couleur peuvent suivre AutoJs6 ou être définis séparément. La langue et le mode sombre peuvent aussi suivre Android. Historique et mentions légales sont disponibles hors ligne. Les vérifications manuelles GitHub gardent les résultats réussis 24 heures. Les vérifications automatiques sont désactivées par défaut. Une fois activées, elles ont lieu pendant l'utilisation, au plus toutes les 12 heures, sans signaler les échecs ou versions ignorées et sans télécharger d'APK. La gestion des mises à jour ignorées permet de rétablir chaque version. La page À propos indique la version, le développeur, le code source, la licence et les mentions tierces.
- `Fonctionnalité` Les tâches d'écran démarrent d'abord l'accessibilité avec la méthode automatique configurée dans AutoJs6 (Root, paramètres sécurisés ou Shizuku). Ce n'est qu'en cas d'échec ou d'absence de configuration que la carte de tâche vous demande de l'activer, avec un raccourci vers les paramètres d'accessibilité.
- `Fonctionnalité` Les autorisations des opérations proposent l'Accès complet: les outils activés, y compris paiements, suppressions, scripts et écriture en mémoire, s'exécutent sans approbation. Il n'active aucun groupe d'outils supplémentaire et ne relâche ni les budgets ni les autorisations de l'hôte. L'espace de travail, la bulle flottante, la tâche en cours et le détail de l'historique affichent un libellé visible au lieu d'une boîte de dialogue. Les tâches qui demandent explicitement une confirmation prudente la conservent.
- `Fonctionnalité` Les cartes de confirmation ajoutent Toujours autoriser pour cette session: jusqu'à la fin de la tâche, le même outil au même niveau de risque s'exécute sans nouvelle demande, même avec d'autres arguments. Les paiements demandent leur propre accord; les scripts générés et les propositions de mémoire peuvent aussi être autorisés pour la session.
- `Correctif` Arrêt des tâches après plusieurs observations de grands écrans ou tours d'appels d'outils natifs; les étapes terminées sont conservées et le contexte antérieur est compacté, avec maintien des confirmations, du budget et des délais
- `Correctif` Sur un telephone de 360 dp avec le texte a deux fois sa taille, la banniere de connexion a l'hote ecrasait le bouton "Se connecter a AutoJs6" en un caractere par ligne (constate sur un Redmi Note 12 et un Xperia XZ1 Compact); les deux actions de la banniere s'empilent desormais quand elles ne tiennent pas cote a cote
- `Correctif` La note de budget de l'editeur de preselections citait encore les valeurs d'avant l'assouplissement (40 etapes, 60 appels, 600000 ms, 300000 jetons); elle correspond maintenant au budget automatique (60 etapes, 90 appels, 15 minutes, 30 minutes pour les taches detachees, 500000 jetons) et exprime la duree en minutes
- `Correctif` La puce du mode d'acces du plan de travail n'avait pas de nom accessible avant l'arrivee du premier etat, si bien qu'un lecteur d'ecran rencontrait un bouton sans nom (releve par l'audit de mise en page CI sur API 35); elle s'appelle desormais "Permissions d'operation" jusqu'a ce que l'etat fournisse le mode
- `Correctif` Les raisons d'arret sont precises: le resume final porte la cause entre crochets avec la dimension du budget et utilise/limite (etapes 60/60, duree 900 s/900 s), la limite depassee (taille de la reponse du modele, contexte au-dela de la limite d'entree du modele, lot de resultats d'outils) ou le code d'erreur avec la raison fixe de l'hote (MODEL_FAILED: ONLINE_NETWORK_UNAVAILABLE); les raisons autres que REQUEST_REJECTED etaient perdues auparavant
- `Correctif` Un nom de portee long dans la liste de memoire ne repousse plus la cle de l'entree hors de sa ligne: le badge de portee est tronque sur une ligne et conserve le nom complet dans sa description d'accessibilite (detecte par l'audit de mise en page CI en API 24 / 360 dp)
- `Correctif` Lorsqu'une exception interne fait echouer une tache, l'enregistrement de l'etape conserve la classe de l'exception (jamais son message) pour le diagnostic; un delai d'outil depasse nomme desormais la dimension de limite de temps d'outil dans le resultat; la decouverte des outils MCP est limitee a 8 secondes pour ne pas consommer la fenetre de preparation de 15 secondes
- `Correctif` Les capacites du plugin declarent desormais native-tools et vision, les limites d'execution sont liees directement aux constantes du contrat hote, la version du client MCP provient du paquet installe et les litteraux disperses de delai et de taille referencent le contrat
- `Correctif` Un service d'accessibilité AutoJs6 arrêté est signalé au modèle par A11Y_SERVICE_NOT_RUNNING au lieu d'une erreur d'arguments
- `Amélioration` Les ecrans de gestion sont harmonises: l'editeur de preselections donne au contexte fixe sa propre section "Contexte", l'editeur de serveurs MCP utilise la meme barre d'actions fixe (Supprimer / Enregistrer) que les editeurs de preselections et de memoire, la note de conservation de l'historique reprend le style des autres ecrans et la carte flottante garde un espace entre son en-tete et son contenu
- `Amélioration` Les details de la tache presentent le modele, la preselection, la duree et les limites de tache en deux colonnes cle / valeur alignees, les valeurs longues passant a la ligne a cote de la colonne des libelles, dans le style du tableau de parametres des confirmations
- `Amélioration` La carte de la tache en cours a une seule ligne d'etat: l'etat dans sa couleur (accent en cours, vert terminee, rouge echouee, ambre partielle) avec le modele et la preselection sur la meme ligne, et le budget devient une ligne compacte "Etape n/m · appels · min · tokens"; dans les details "Relancer" est l'action principale pleine largeur et "Reessayer avec un autre modele" occupe sa propre ligne sans retour a la ligne
- `Amélioration` Le budget automatique des taches est assoupli: etapes 40 -> 60, appels au modele 60 -> 90, duree 10 -> 15 minutes, tokens 300k -> 500k; les reglages, les preselections et chaque tache ne peuvent toujours que le restreindre
- `Amélioration` L'icone du lanceur est l'illustration Three Stove fournie par le mainteneur: un glyphe sombre sur gris clair en mode clair, un glyphe clair sur gris sombre en mode sombre, les icones ronde et adaptative etant composees a partir de la meme image source
- `Amélioration` L'etiquette d'etape de la bulle flottante fournit son texte complet aux lecteurs d'ecran sous le role tronquable, l'avis de modeles epingles pleins est une barre en page et le lanceur declare une icone ronde; le kit d'interface retire les membres inutilises et partage ses constructeurs de paragraphe et de note
- `Amélioration` Le catalogue d'outils declare la confirmation obligatoire des propositions de memoire et des scripts generes via l'attribut confirmAlways, et les noms d'outils integres sont references par des constantes ToolNames que le test d'instantane maintient alignees avec le catalogue
- `Amélioration` Les identifiants du modèle restent dans son fournisseur; AutoJs6 transmet les appels. Les jetons MCP Bearer sont chiffrés avec Android Keystore dans le stockage privé et exclus des prompts et exports historiques. INTERNET sert aussi aux serveurs MCP configurés; sur Android 17+, la permission réseau local se demande depuis les paramètres MCP. Le risque choisi par serveur est initialement SENSITIVE. Annuler ne rétablit pas les actions distantes; aucun appel échoué ne se rejoue automatiquement.
- `Amélioration` Application autonome repensée en Material 3: l'accueil est un fil de tâches avec la zone de saisie ancrée au-dessus du clavier, une barre supérieure avec la capsule de modèle, l'historique et un menu (Nouvelle tâche, Préréglages, Mémoire, Dossiers de scripts, Serveurs MCP, Paramètres), un bandeau seulement tant qu'AutoJs6 n'est pas connecté, une chronologie des étapes mise à jour par étape, et Relancer ou Réessayer avec un autre modèle qui remplissent la saisie sans démarrer. Paramètres organisés en sections claires et apparence claire/sombre cohérente
- `Amélioration` Les confirmations affichent le niveau de risque, le groupe d'outils et chaque paramètre dans un tableau lisible au lieu du JSON brut, avec des actions distinctes: autoriser une fois, toujours autoriser pour cette session et refuser. La bulle flottante adopte le même design Material, choisit le préréglage directement dans la carte et sa ligne Modèle ouvre le sélecteur de modèle partagé
- `Amélioration` L'historique ajoute la recherche, des puces d'état et des filtres par préréglage et par période, et efface les tâches terminées depuis son menu. Les détails d'une tâche montrent le modèle, une chronologie des étapes avec tableaux de paramètres et observations dépliables, Relancer ou Réessayer avec un autre modèle, et un menu pour exporter le diagnostic, supprimer l'enregistrement ou utiliser le modèle de la tâche pour les nouvelles tâches
- `Amélioration` Préréglages, mémoire, serveurs MCP et dossiers de scripts partagent le même design: cartes de préréglages avec menu de ligne et éditeur plein écran (durée en minutes, bouton Enregistrer fixe), recherche et puces de portée pour la mémoire, liste d'outils MCP avec interrupteur d'activation et choix du risque, et confirmation avant d'abandonner des modifications non enregistrées
- `Dépendance` Mise a niveau des trois artefacts API hote release vers AutoJs6 86d9bfa26b / build 5298: ai-agent-api devient three-stove-agent-api (paquet AIDL org.autojs.plugin.three.stove.agent.api, version de contrat 2), common-plugin-api et host-capability-api etant reverrouilles depuis le meme build
- `Dépendance` Mettre a niveau les trois artefacts release de l'API hote vers AutoJs6 3cdf7de13c / build 5297 (option du groupe mcp de P10 et constante TOOL_FAILED); le contrat de base reste en V1
- `Dépendance` Ajout d'AndroidX AppCompat 1.7.1 et de Material Components for Android 1.13.0 avec leurs dépendances AndroidX d'exécution pour l'interface Material 3

#### v1.1.0

_2026/09/26_

- `Note` 1.1.0 n'a pas ete publie separement; tous ses changements sont livres avec 1.2.0
- `Note` Les appels natifs exigent AutoJs6 build 5297+ et une cible tools, comme une cible en ligne de la version de développement 3-Stone AI 1.2.0. Les anciens hôtes et les cibles incompatibles conservent JSON. Chaque conversation garde son délai initial, ses limites de contexte/sortie et 16 tours d'outils au maximum; aucune reprise JSON après une action
- `Note` Les images nécessitent un hôte compatible, le groupe observe et un modèle visuel dont cette entrée est explicitement activée. Implémentation et tests déterministes terminés; validation visuelle réelle en ligne encore en attente. Les anciens systèmes et modèles texte gardent les observations textuelles. Voir ROADMAP.md
- `Note` Les scripts générés utilisent les autorisations AutoJs6 sans bac à sable JavaScript et peuvent agir hors des groupes activés. Le code complet est conservé dans les étapes privées, sous réserve du masquage des mots de passe et de la rétention. Un code modifié par un masquage ultérieur ne peut être enregistré comme original. Vérifiez les .js avant de les partager.
- `Fonctionnalité` Appels natifs via l'hôte: schémas du catalogue, validation du lot entier, exécution séquentielle, confirmations individuelles, retour des résultats et journal commun
- `Fonctionnalité` Observation par capture via AutoJs6 sur Android 11+: screen_capture limite le grand côté à 1280 et utilise JPEG qualité 70, avec instructions visuelles, budget de tokens image et images dans les résultats des outils natifs
- `Fonctionnalité` JavaScript généré via script_run_source : le groupe script_dynamic est désactivé par défaut. Chaque appel exige un résumé du code extensible au texte complet et une approbation individuelle. Exécution avec délai, annulation, résultats structurés et code dans les étapes privées. Le code UTF-8 et sa chaîne JSON sont chacun limités à 8 KiB.
- `Dépendance` Mise à niveau des trois artefacts API hôte release vers AutoJs6 52ce694f92 / build 5297 pour les images négociées, en conservant le contrat de connexion build 5289+

##### Pour plus d'historique des versions

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/app/src/main/assets/doc/CHANGELOG-fr.md)

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

Le code du projet est publié sous la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/LICENSE). Les composants tiers et leurs licences sont listés dans les [Avis de tiers](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/THIRD_PARTY_NOTICES.md).

******

### Liens

******

- Projet AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Documentation AutoJs6: https://docs.autojs6.com
- Discussion AutoJs6 #577: https://github.com/SuperMonster003/AutoJs6/discussions/577
- Avis de tiers: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/THIRD_PARTY_NOTICES.md
