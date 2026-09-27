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

Three Stove Agent transforme un objectif en langage naturel en actions sur un appareil Android exécutant AutoJs6. Soit il choisit un script que l'utilisateur a enregistré pour l'agent, complète ses paramètres et l'exécute ; soit il observe l'écran à travers l'arbre de noeuds d'accessibilité et agit étape par étape (observer, décider, agir, vérifier) jusqu'à ce que l'objectif soit atteint, qu'une confirmation soit nécessaire ou qu'un budget soit épuisé. Il répond à la [discussion AutoJs6 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

Three Stove Agent fournit une interface autonome et un plugin AutoJs6 accessible par ai.agent. Les actions intégrées et les appels de modèle passent par AutoJs6. Les outils MCP facultatifs utilisent uniquement les serveurs configurés. Aucun accès direct au fournisseur de modèle ni permission d'accessibilité.

******

### État

******

La version 1.2.0 en développement propose les outils MCP facultatifs, appels natifs, captures et scripts générés. AiGoCode gpt-5.6-sol a réussi les tests P9.2 avec image initiale et image dans un résultat d'outil. P9.1 a validé l'activation du Wi-Fi et la relecture de son état avec les parcours JSON et natif, en désactivant temporairement la connexion automatique au point d'accès actuel et en accédant au modèle via les données mobiles et le VPN. Les échecs après un changement de réseau du VPN avec la connexion automatique par défaut restent non résolus; voir [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/ROADMAP.md).

******

### Fonctionnalités

******

L'implémentation actuelle propose les fonctions suivantes:

- Sélection de scripts : les scripts enregistrés via `project.json` ou un commentaire d'en-tête `@agent` sont présentés au modèle avec leurs descriptions et schémas de paramètres ; l'agent en choisit un, complète les paramètres, demande confirmation si nécessaire, l'exécute dans AutoJs6 et lit son résultat structuré.
- Manipulation de l'écran étape par étape : l'agent observe l'arbre de noeuds d'accessibilité sous forme de texte compact (et le texte de l'écran via un plugin OCR lorsqu'il est installé), puis clique, saisit, fait défiler et appuie sur des touches via le courtier de capacités d'AutoJs6 jusqu'à pouvoir vérifier l'objectif.
- Sécurité par conception : les outils en lecture seule s'exécutent automatiquement, les actions sensibles (paiement, envoi, suppression, écriture de fichiers, shell, gestes par coordonnées, scripts enregistrés comme sensibles) nécessitent par défaut une confirmation, et chaque exécution a des budgets d'étapes, d'appels de modèle, de durée et de jetons. Une confirmation peut valoir une fois ou jusqu'à la fin de la tâche; les paramètres proposent aussi le mode prudent et l'accès complet, qui saute les confirmations et reste clairement signalé.
- API de script et interface utilisateur : `ai.agent.run(goal, options)` renvoie un handle `AgentRun` avec événements, réponses et annulation ; l'application autonome offre un espace de tâches avec historique, préréglages, mémoire de préférences, paramètres et historique des versions.
- Appels natifs via l'hôte: schémas du catalogue, validation du lot entier, exécution séquentielle, confirmations individuelles, retour des résultats et journal commun
- Observation par capture via AutoJs6 sur Android 11+: screen_capture limite le grand côté à 1280 et utilise JPEG qualité 70, avec instructions visuelles, budget de tokens image et images dans les résultats des outils natifs
- JavaScript généré via script_run_source : le groupe script_dynamic est désactivé par défaut. Chaque appel montre un résumé du code extensible au texte complet, à approuver une fois ou pour la tâche en cours; l'accès complet saute cette revue. Exécution avec délai, annulation, résultats structurés et code dans les étapes privées. Le code UTF-8 et sa chaîne JSON sont chacun limités à 8 KiB.
- Outils MCP de serveurs locaux ou externes choisis, avec un niveau de risque par serveur et le groupe mcp désactivé par défaut
- Application autonome repensée en Material 3: l'accueil est un fil de tâches avec la zone de saisie ancrée au-dessus du clavier, une barre supérieure avec la capsule de modèle, l'historique et un menu (Nouvelle tâche, Préréglages, Mémoire, Dossiers de scripts, Serveurs MCP, Paramètres), un bandeau seulement tant qu'AutoJs6 n'est pas connecté, une chronologie des étapes mise à jour par étape, et Relancer ou Réessayer avec un autre modèle qui remplissent la saisie sans démarrer. Paramètres organisés en sections claires et apparence claire/sombre cohérente

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
2. Ouvrez le centre de plugins d'AutoJs6, vérifiez que `Three Stove Agent` est reconnu et activez-le. Les paquets officiels passent automatiquement la vérification de signature.

Installez et activez [3-Stone AI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stone-AI), puis configurez-y un modèle en ligne ou importez un modèle local compatible. Le courtier actuel de l'hôte sélectionne 3-Stone AI; un autre Provider nécessite une intégration côté hôte. Choisissez le modèle avec la capsule de modèle de l'accueil d'Three Stove Agent. Un bandeau n'y apparaît que tant qu'AutoJs6 n'est pas connecté.

### Compatibilité

Android 7.0+ (API 24). La connexion nécessite AutoJs6 6.8.0 / build 5289+; l'API complète et ce guide nécessitent build 5293+. Utilisez une compilation contenant les changements Agent. Les actions à l'écran exigent le service d'accessibilité de l'hôte; Agent le démarre d'abord avec la méthode automatique configurée dans AutoJs6 (Root, paramètres sécurisés ou Shizuku) et ne vous demande de l'activer qu'en cas d'échec. OCR est facultatif et nécessite un plugin installé, autorisé et déclaré disponible par l'hôte. Three Stove Agent ne conserve aucun identifiant de modèle et ne possède pas de service d'accessibilité propre.

### Démarrer depuis l'interface

Ouvrez Three Stove Agent, connectez AutoJs6, saisissez un objectif et démarrez. La capsule de modèle de l'accueil choisit un modèle en ligne ou local, ou Automatique (un modèle sur l'appareil d'abord, sinon le premier disponible). Recherchez, épinglez vos modèles favoris et réutilisez les récents; des badges indiquent la prise en charge déclarée des outils et des images. Le plan de travail et la bulle flottante partagent ce choix pour les nouvelles tâches, sans modifier les préréglages ni la tâche en cours; les préréglages ne contiennent plus de modèle. La puce de préréglage de la zone de saisie choisit un préréglage facultatif. Répondez aux questions et suivez la progression dans la carte de tâche.

### Démarrer depuis un script

Exécutez ce JavaScript dans AutoJs6 après connexion de Three Stove Agent et configuration du modèle. L'interface du plugin reçoit les questions et confirmations. Pour réutiliser une configuration, ajoutez `preset: "your-preset-name"` aux options.

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
if (!context) throw Error('Start this registered script through Three Stove Agent');
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
| `console_tail` | `observe` | `READ_ONLY` | `on` | Read bounded recent console lines; they may include unrelated scripts. |
| `device_info` | `observe` | `READ_ONLY` | `on` | Read device information. |
| `screen_capture` | `observe` | `READ_ONLY` | `auto (vision)` | Capture the unlocked screen for the selected vision model when text nodes are insufficient. Returns a scaled JPEG observation, not device coordinates. |
| `screen_state` | `observe` | `READ_ONLY` | `on` | Read whether the screen is on. |
| `ui_dump` | `observe` | `READ_ONLY` | `on` | Observe the current accessibility tree before choosing an action. |
| `ui_find` | `observe` | `READ_ONLY` | `on` | Find nodes matching all selector conditions. |
| `ui_wait_for` | `observe` | `READ_ONLY` | `on` | Wait for a selector to appear or disappear within a deadline. |
| `ocr_screen` | `ocr` | `READ_ONLY` | `auto (OCR)` | Read screen text through the host OCR plugin. |
| `script_catalog` | `script` | `READ_ONLY` | `on` | Find scripts explicitly registered for Agent use. |
| `script_run` | `script` | `NORMAL` | `on` | Run a registered script by id with validated parameters and its registered risk. |
| `script_stop` | `script` | `NORMAL` | `on` | Stop an owned script execution. |
| `script_run_source` | `script_dynamic` | `SENSITIVE` | `off` | Run generated Rhino JavaScript with host script privileges after individual source approval. No sandbox. Source including JSON escaping <=8192 UTF-8 bytes. Use ai.agent.result(value) for results. |
| `shell_exec` | `shell` | `SENSITIVE` | `off` | Execute a bounded non-root shell command after confirmation. |
| `report_progress` | `user` | `READ_ONLY` | `on` | Report bounded progress without declaring task completion. |

### Préréglages et mémoire

Ouvrez les préréglages depuis les tâches pour enregistrer une configuration. Les noms identifient les scripts et les portées mémoire; dupliquez pour utiliser un autre nom. Le préréglage intégré default peut être modifié mais pas supprimé. Les préréglages n'incluent pas de modèle; un modèle enregistré par une version antérieure est conservé pour les scripts uniquement. Les options de tâche peuvent seulement réduire les limites du préréglage. Les contextes fixe et de tâche partagent 8 KiB. La mémoire peut inclure les entrées globales et celles du préréglage, un seul ensemble, ou aucun. Modifier ou supprimer un préréglage ne change pas les tâches en attente. Stockage privé: 32 préréglages / 1 MiB maximum.

Ouvrez Mémoire pour consulter, modifier, supprimer ou sauvegarder les préférences. Limites: 500 entrées / 256 KiB, avec portée, tâche source et dates. Confirmez chaque memory_propose et chaque entrée importée. Créez d'abord les préréglages manquants. L'injection automatique conserve les entrées complètes les plus récentes de la portée autorisée, jusqu'à 4 KiB; le préréglage courant prime sur une clé globale identique. memory: false désactive uniquement l'injection. Désactivez aussi le groupe memory ou la portée pour bloquer recherches et propositions. L'export contient les valeurs réelles et leur origine. Ne stockez pas de secrets; les clés et formats de jetons reconnaissables sont refusés.

### Utilisation

- Configurez les dossiers supplémentaires dans "Dossiers de scripts" du lanceur, un chemin absolu par ligne. L'hôte valide et applique les chemins enregistrés; les tâches peuvent seulement restreindre ces dossiers.
- 200 tâches / 32 MiB au maximum. Les tâches terminées consultées le moins récemment sont supprimées en premier. Relancer remplit l'objectif et le préréglage d'origine dans le tableau de tâches. Vérifiez-les puis appuyez sur Démarrer. Vider l'historique conserve les tâches en cours. L'export conserve les compteurs, les noms des outils et les confirmations. Les objectifs, paramètres, observations et résultats des scripts sont retirés. Choisissez un emplacement.
- Répondez dans les tâches au premier plan, ou ouvrez la notification prioritaire en arrière-plan. La confirmation affiche outil, paramètres, risque et temps restant. Toujours autoriser pour cette session accepte le même outil au même niveau de risque jusqu'à la fin de la tâche, y compris les propositions de mémoire ou codes générés suivants; les paiements demandent leur propre accord. Mémoriser une réponse crée une proposition memory_propose séparée dans la portée autorisée. Une confirmation attend normalement 120 secondes, une question jusqu'à 10 minutes, dans le budget de la tâche. Un délai expiré renvoie USER_TIMEOUT; le modèle choisit de redemander ou de signaler un résultat partiel. Les anciens liens ne répondent pas aux nouvelles demandes. Les permissions et canaux contrôlent les notifications.
- Ouvrez les paramètres depuis les tâches pour choisir groupes, budgets, autorisations des opérations (standard, prudent ou accès complet), saisie vocale et profil par défaut. Chaque changement est enregistré immédiatement et concerne les nouvelles tâches. L'accès complet exécute les outils activés, paiements compris, sans approbation; les tâches, la bulle flottante et l'historique affichent alors un avertissement. gesture/files/shell/script_dynamic sont désactivés initialement; OCR exige un plugin autorisé et disponible sur le service hôte. Une limite en mode automatique reprend la valeur initiale; la durée se saisit en minutes, dans les limites du protocole. Profils et options ne peuvent que les réduire. La gestion affiche nombres et octets; effacer une catégorie exige confirmation et aucune tâche active. Effacer les profils restaure default. Profils, mémoire, dossiers de scripts et serveurs MCP s'ouvrent aussi depuis les paramètres.
- Ouvrez les paramètres depuis le menu en haut à droite. La langue, le mode sombre et la couleur peuvent suivre AutoJs6 ou être définis séparément. La langue et le mode sombre peuvent aussi suivre Android. Historique et mentions légales sont disponibles hors ligne. Les vérifications manuelles GitHub gardent les résultats réussis 24 heures. Les vérifications automatiques sont désactivées par défaut. Une fois activées, elles ont lieu pendant l'utilisation, au plus toutes les 12 heures, sans signaler les échecs ou versions ignorées et sans télécharger d'APK. La gestion des mises à jour ignorées permet de rétablir chaque version. La page À propos indique la version, le développeur, le code source, la licence et les mentions tierces.
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
- Three Stove Agent fournit une interface autonome et un plugin AutoJs6 accessible par ai.agent. Les actions intégrées et les appels de modèle passent par AutoJs6. Les outils MCP facultatifs utilisent uniquement les serveurs configurés. Aucun accès direct au fournisseur de modèle ni permission d'accessibilité.
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

`ThreeStoveAgentPluginService` / `IAiAgentPlugin` / `IAiAgentLink`: Connexion avec identité du programme hôte vérifiée, file de tâches, réponses, annulation, requêtes et historique privé; tâches bloquées après déconnexion et aucun redémarrage automatique après arrêt du processus.

******

### Feuille de route

******

Les plans et l'avancement du plugin sont tenus sous forme de liste cochable dans ROADMAP.md, organisée par phase avec des critères d'acceptation et des niveaux de preuve. Les éléments non cochés expriment une intention et non une capacité actuelle ; les discussions via Issues sont les bienvenues.

- [Voir ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/ROADMAP.md)

******

### Historique des versions

******

#### v1.2.0

_2026/09/27_

- `Note` L'application est renommée Three Stove Agent: l'ID d'application devient io.github.supermonster003.autojs6.plugin.three.stove.agent et le dépôt devient AutoJs6-Plugin-Three-Stove-Agent. L'ancien nom n'est plus pris en charge: désinstallez l'ancien AI Agent avant l'installation; l'historique, les préréglages et la mémoire ne sont pas migrés. L'ID de plugin ai-agent, l'action du service et le paquet AIDL proviennent des AAR du contrat AutoJs6 et seront remplacés avec le renommage de l'hôte et les AAR reverrouillés.
- `Note` La version 1.2.0 en développement propose les outils MCP facultatifs, appels natifs, captures et scripts générés. AiGoCode gpt-5.6-sol a réussi les tests P9.2 avec image initiale et image dans un résultat d'outil. P9.1 a validé l'activation du Wi-Fi et la relecture de son état avec les parcours JSON et natif, en désactivant temporairement la connexion automatique au point d'accès actuel et en accédant au modèle via les données mobiles et le VPN. Les échecs après un changement de réseau du VPN avec la connexion automatique par défaut restent non résolus; voir ROADMAP.md.
- `Fonctionnalité` Outils MCP de serveurs locaux ou externes choisis, avec un niveau de risque par serveur et le groupe mcp désactivé par défaut
- `Fonctionnalité` Ouvrez Three Stove Agent, connectez AutoJs6, saisissez un objectif et démarrez. La capsule de modèle de l'accueil choisit un modèle en ligne ou local, ou Automatique (un modèle sur l'appareil d'abord, sinon le premier disponible). Recherchez, épinglez vos modèles favoris et réutilisez les récents; des badges indiquent la prise en charge déclarée des outils et des images. Le plan de travail et la bulle flottante partagent ce choix pour les nouvelles tâches, sans modifier les préréglages ni la tâche en cours; les préréglages ne contiennent plus de modèle. La puce de préréglage de la zone de saisie choisit un préréglage facultatif. Répondez aux questions et suivez la progression dans la carte de tâche.
- `Fonctionnalité` Ouvrez les paramètres depuis le menu en haut à droite. Chaque modification s'applique immédiatement, sans bouton Enregistrer: apparence, autorisations, groupes d'outils, limites (durée en minutes), saisie vocale, bulle flottante et nettoyage des données. La langue, le mode sombre et la couleur peuvent suivre AutoJs6 ou être définis séparément. La langue et le mode sombre peuvent aussi suivre Android. Historique et mentions légales sont disponibles hors ligne. Les vérifications manuelles GitHub gardent les résultats réussis 24 heures. Les vérifications automatiques sont désactivées par défaut. Une fois activées, elles ont lieu pendant l'utilisation, au plus toutes les 12 heures, sans signaler les échecs ou versions ignorées et sans télécharger d'APK. La gestion des mises à jour ignorées permet de rétablir chaque version. La page À propos indique la version, le développeur, le code source, la licence et les mentions tierces.
- `Fonctionnalité` Les tâches d'écran démarrent d'abord l'accessibilité avec la méthode automatique configurée dans AutoJs6 (Root, paramètres sécurisés ou Shizuku). Ce n'est qu'en cas d'échec ou d'absence de configuration que la carte de tâche vous demande de l'activer, avec un raccourci vers les paramètres d'accessibilité.
- `Fonctionnalité` Les autorisations des opérations proposent l'Accès complet: les outils activés, y compris paiements, suppressions, scripts et écriture en mémoire, s'exécutent sans approbation. Il n'active aucun groupe d'outils supplémentaire et ne relâche ni les budgets ni les autorisations de l'hôte. L'espace de travail, la bulle flottante, la tâche en cours et le détail de l'historique affichent un libellé visible au lieu d'une boîte de dialogue. Les tâches qui demandent explicitement une confirmation prudente la conservent.
- `Fonctionnalité` Les cartes de confirmation ajoutent Toujours autoriser pour cette session: jusqu'à la fin de la tâche, le même outil au même niveau de risque s'exécute sans nouvelle demande, même avec d'autres arguments. Les paiements demandent leur propre accord; les scripts générés et les propositions de mémoire peuvent aussi être autorisés pour la session.
- `Correctif` Lorsqu'une exception interne fait echouer une tache, l'enregistrement de l'etape conserve la classe de l'exception (jamais son message) pour le diagnostic; un delai d'outil depasse nomme desormais la dimension de limite de temps d'outil dans le resultat; la decouverte des outils MCP est limitee a 8 secondes pour ne pas consommer la fenetre de preparation de 15 secondes
- `Correctif` Les capacites du plugin declarent desormais native-tools et vision, les limites d'execution sont liees directement aux constantes du contrat hote, la version du client MCP provient du paquet installe et les litteraux disperses de delai et de taille referencent le contrat
- `Correctif` Un service d'accessibilité AutoJs6 arrêté est signalé au modèle par A11Y_SERVICE_NOT_RUNNING au lieu d'une erreur d'arguments
- `Amélioration` L'icone du lanceur est l'illustration Three Stove fournie par le mainteneur: un glyphe sombre sur gris clair en mode clair, un glyphe clair sur gris sombre en mode sombre, les icones ronde et adaptative etant composees a partir de la meme image source
- `Amélioration` L'etiquette d'etape de la bulle flottante fournit son texte complet aux lecteurs d'ecran sous le role tronquable, l'avis de modeles epingles pleins est une barre en page et le lanceur declare une icone ronde; le kit d'interface retire les membres inutilises et partage ses constructeurs de paragraphe et de note
- `Amélioration` Le catalogue d'outils declare la confirmation obligatoire des propositions de memoire et des scripts generes via l'attribut confirmAlways, et les noms d'outils integres sont references par des constantes ToolNames que le test d'instantane maintient alignees avec le catalogue
- `Amélioration` Les identifiants du modèle restent dans son fournisseur; AutoJs6 transmet les appels. Les jetons MCP Bearer sont chiffrés avec Android Keystore dans le stockage privé et exclus des prompts et exports historiques. INTERNET sert aussi aux serveurs MCP configurés; sur Android 17+, la permission réseau local se demande depuis les paramètres MCP. Le risque choisi par serveur est initialement SENSITIVE. Annuler ne rétablit pas les actions distantes; aucun appel échoué ne se rejoue automatiquement.
- `Amélioration` Application autonome repensée en Material 3: l'accueil est un fil de tâches avec la zone de saisie ancrée au-dessus du clavier, une barre supérieure avec la capsule de modèle, l'historique et un menu (Nouvelle tâche, Préréglages, Mémoire, Dossiers de scripts, Serveurs MCP, Paramètres), un bandeau seulement tant qu'AutoJs6 n'est pas connecté, une chronologie des étapes mise à jour par étape, et Relancer ou Réessayer avec un autre modèle qui remplissent la saisie sans démarrer. Paramètres organisés en sections claires et apparence claire/sombre cohérente
- `Amélioration` Les confirmations affichent le niveau de risque, le groupe d'outils et chaque paramètre dans un tableau lisible au lieu du JSON brut, avec des actions distinctes: autoriser une fois, toujours autoriser pour cette session et refuser. La bulle flottante adopte le même design Material, choisit le préréglage directement dans la carte et sa ligne Modèle ouvre le sélecteur de modèle partagé
- `Amélioration` L'historique ajoute la recherche, des puces d'état et des filtres par préréglage et par période, et efface les tâches terminées depuis son menu. Les détails d'une tâche montrent le modèle, une chronologie des étapes avec tableaux de paramètres et observations dépliables, Relancer ou Réessayer avec un autre modèle, et un menu pour exporter le diagnostic, supprimer l'enregistrement ou utiliser le modèle de la tâche pour les nouvelles tâches
- `Amélioration` Préréglages, mémoire, serveurs MCP et dossiers de scripts partagent le même design: cartes de préréglages avec menu de ligne et éditeur plein écran (durée en minutes, bouton Enregistrer fixe), recherche et puces de portée pour la mémoire, liste d'outils MCP avec interrupteur d'activation et choix du risque, et confirmation avant d'abandonner des modifications non enregistrées
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

#### v1.0.0

_2026/09/25_

- `Note` La version 1.0.0 propose des tâches en langage naturel, des appels de scripts enregistrés et des actions sur le périphérique avec confirmation selon le risque. Consultez [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/ROADMAP.md) pour les cas vérifiés, les limites des modèles et les vérifications de périphériques restantes. Les appels natifs aux outils, les entrées visuelles et la génération dynamique de scripts sont prévus pour 1.1.0.
- `Note` Nécessite Android 7+, AutoJs6 6.8.0 / build 5293+ pour les API de tâches, et 3-Stone AI activé avec un modèle configuré. OCR est facultatif. Le seul protocole de connexion nécessite build 5289+.
- `Note` Compatibilité: l'extension native des outils dans AutoJs6 build 5297 est compatible avec cette version. Le candidat de développement 3-Stone AI 1.2.0 implémente la continuation des outils en ligne pour trois protocoles. Cette version Agent utilise encore des décisions JSON structurées; intégration de la boucle native et comparaisons restent dans [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/ROADMAP.md).
- `Fonctionnalité` Tableau de tâches en langage naturel avec questions, progression, arrêt et résultats; saisie flottante facultative, partage de texte, raccourcis de préréglages et brouillons vocaux
- `Fonctionnalité` API ai.agent pour créer des tâches, suivre événements et requêtes, répondre et annuler, avec tâches detached et accès aux résultats/contexte des scripts enregistrés
- `Fonctionnalité` Scripts project.json / @agent avec recherche, validation des paramètres et valeurs par défaut, questions sur les valeurs manquantes, confirmation, exécution bornée et résultats structurés
- `Fonctionnalité` Observation par texte des noeuds et OCR autorisé facultatif, clics par référence, saisie, défilement et touches, avec contrôle des changements et preuves d'achèvement
- `Fonctionnalité` Modèles en ligne et locaux via AutoJs6 sans conserver leurs identifiants; une cible choisie absente échoue sans changement silencieux de modèle
- `Fonctionnalité` Budgets de pas, appels, durée et tokens, délais des outils, deux tentatives de réparation au plus par étape et protection contre les actions répétées sans effet
- `Fonctionnalité` Préréglages nommés et paramètres globaux de modèle, contexte, outils, budgets, prudence, dossiers et mémoire; gesture/files/shell désactivés par défaut
- `Fonctionnalité` Mémoire de préférences par portée avec approbation individuelle des propositions/importations, édition, suppression et sauvegarde JSON, jusqu'à 500 entrées / 256 KiB; injection automatique limitée à 4 KiB
- `Fonctionnalité` Détails et chronologies, filtres, brouillons de relance et export JSON expurgé, avec historique privé limité à 200 tâches / 32 MiB
- `Fonctionnalité` Confirmation selon le risque dans le tableau, les notifications et la carte flottante; paiements et mémoire toujours approuvés individuellement; perte de l'hôte bloquante et aucune reprise automatique après redémarrage
- `Fonctionnalité` Paramètres, historique hors ligne et mentions légales en dix langues; recherche manuelle GitHub avec annulation, cache quotidien et versions ignorées, sans téléchargement automatique d'APK
- `Correctif` Fin prématurée des tâches lorsque le budget restant est interprété comme consommé
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
