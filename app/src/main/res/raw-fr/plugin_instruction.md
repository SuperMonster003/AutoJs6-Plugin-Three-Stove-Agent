3-Stove Agent est un agent qui effectue des opérations sur le téléphone à votre place. Après la description d'un objectif en une phrase, il observe l'écran par l'intermédiaire d'AutoJs6 et touche, saisit et fait défiler étape par étape, ou choisit un script enregistré, complète ses paramètres et l'exécute, jusqu'à atteindre l'objectif. Les opérations sensibles comme payer, envoyer ou supprimer demandent d'abord une confirmation, et chaque tâche est limitée en nombre d'étapes et en durée.

### Prise en main

1. Installez et activez [3-Stone AI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stone-AI), puis ajoutez-y un modèle en ligne ou importez un modèle local. 3-Stove Agent appelle les modèles par son intermédiaire.
2. Ouvrez 3-Stove Agent. Si l'écran d'accueil indique qu'il n'est pas connecté, touchez "Connecter AutoJs6".
3. Touchez la pastille du modèle dans la barre supérieure pour choisir un modèle, ou conservez "Automatique" (un modèle local est privilégié).
4. Saisissez un objectif dans la barre de saisie et touchez "Démarrer la tâche". La carte de la tâche affiche la progression de chaque étape; les questions et les confirmations s'y traitent également.

### Fonctionnalités principales

- **Tâches et historique**: pendant une tâche, vous pouvez consulter ses étapes, répondre aux questions ou l'arrêter à tout moment. "Historique des tâches" conserve les tâches terminées, qui peuvent être partagées sous forme de résumé ou relancées (la relance replace seulement l'objectif d'origine dans la barre de saisie, pour vérification avant le démarrage).
- **Confirmations et autorisations des opérations**: les opérations en lecture seule s'exécutent directement; les opérations sensibles comme payer, envoyer, supprimer ou écrire des fichiers demandent par défaut une confirmation, accordée une seule fois ou pour le reste de la tâche. "Autorisations des opérations" dans les paramètres propose "Standard", "Prudent" ou "Accès complet" (sans confirmation, signalé clairement tant qu'il est actif).
- **Préréglages**: enregistrez comme préréglage les groupes d'outils, budgets et contextes fixes utilisés souvent, puis choisissez-le dans l'étiquette de préréglage de la barre de saisie avant de démarrer. Avec "Mode plan" activé, une tâche propose d'abord un plan de 3 à 8 étapes et l'exécute après examen.
- **Mémoire**: conservez les préférences que l'agent doit retenir; elles sont jointes automatiquement aux tâches. Chaque mémoire proposée par l'agent n'est enregistrée qu'après confirmation; n'y enregistrez ni mots de passe ni autres identifiants.
- **Scripts enregistrés**: après l'ajout d'un commentaire `@agent` en tête d'un script (voir l'exemple ci-dessous), l'agent peut choisir et exécuter ce script. Les emplacements de scripts hors du répertoire de travail s'ajoutent dans "Dossiers de scripts".
- **Bulle flottante**: une fois activée dans les paramètres, elle permet de saisir des objectifs, de traiter les confirmations ou d'arrêter des tâches par-dessus les autres applications.
- **Démarrage rapide**: partagez du texte vers 3-Stove Agent, utilisez le raccourci "Nouvelle tâche" de l'écran d'accueil ou épinglez un préréglage sur l'écran d'accueil. Ces entrées créent seulement un brouillon, exécuté après avoir touché "Démarrer la tâche".
- **Voix**: après l'activation de "Activer la saisie vocale" dans les paramètres, un microphone apparaît à côté du champ de saisie; avec 3-Macaw Speech installé, la reconnaissance se fait hors ligne. "Écoute permanente" (désactivée par défaut) accepte une tâche après avoir reconnu le mot d'activation "小炉小炉" ou "Hey Stove". Les résultats vocaux deviennent eux aussi de simples brouillons.
- **Autres outils**: les paramètres permettent d'activer la reconnaissance de texte (nécessite un plugin OCR), les outils fournis par d'autres plugins (comme la lecture à voix haute et la transcription de 3-Macaw Speech), les outils fournis par des serveurs MCP et les scripts générés par le modèle, exécutés uniquement après examen.

### Utilisation dans les scripts

Dans un script AutoJs6, démarrez une tâche avec `ai.agent.run`; les questions et les confirmations restent traitées dans l'interface de 3-Stove Agent:

```js
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

- Vérifiez le `status` du résultat: même lorsque la Promise est tenue, le résultat peut être `completed`, `partial`, `failed`, `blocked` ou `cancelled`. Appelez `run.cancel()` pour arrêter la tâche.
- Ajoutez `preset: 'your-preset-name'` aux options pour utiliser un préréglage enregistré, ou `plan: true` pour que la tâche propose d'abord un plan.
- Toutes les options et tous les événements sont décrits dans [Documentation AutoJs6 > ai.agent](https://docs.autojs6.com/#/ai).

Pour que l'agent exécute un script personnalisé, enregistrez l'exemple ci-dessous sous le nom `text-counter.js` dans le répertoire de travail d'AutoJs6. Demandez ensuite le nombre de caractères d'un texte; si un paramètre manque, l'agent le demande d'abord:

```js
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

### Prérequis

- Android 7.0 ou version ultérieure et AutoJs6 6.8.0 ou version ultérieure.
- Les modèles sont fournis par [3-Stone AI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stone-AI); 3-Stove Agent ne conserve lui-même aucun identifiant de modèle.
- Les opérations à l'écran utilisent le service d'accessibilité d'AutoJs6. Il s'active automatiquement lorsque Root, les paramètres sécurisés ou Shizuku sont configurés dans AutoJs6; sinon, activez-le en suivant l'invite.
- L'autorisation de notification sert aux confirmations en arrière-plan et aux alertes de tâche; la bulle flottante nécessite l'autorisation "afficher par-dessus les autres applications".

### Questions fréquentes

**Raisons pour lesquelles AutoJs6 est nécessaire**

3-Stove Agent gère les tâches et l'interface, tandis que les appels de modèle, les opérations à l'écran et les scripts passent tous par AutoJs6. Sans connexion à AutoJs6, l'historique reste consultable mais aucune nouvelle tâche ne peut démarrer; une connexion perdue bloque la tâche en cours, et la reconnexion ne la rejoue pas.

**Cas où les paiements demandent une confirmation**

Par défaut, chaque paiement demande sa propre confirmation; approuver une commande ou une autre opération n'approuve pas le paiement, et un délai dépassé vaut refus. "Accès complet" ignore les confirmations de paiement; ne l'activez que si l'objectif et le modèle sont fiables.

**Raisons de la lenteur ou de l'échec d'un modèle local**

Un modèle qui se charge n'est pas pour autant capable d'accomplir les tâches; commencez par des tâches simples et vérifiez les résultats. Sur les téléphones anciens, le premier appel peut dépasser la limite de 5 minutes; pendant l'attente, l'écran des tâches affiche les secondes écoulées, et un résultat de dépassement indique les causes probables et les remèdes.

**Raisons de l'arrêt de l'écoute permanente**

L'écoute permanente ne démarre pas avec l'appareil et reprend à l'ouverture de l'écran des tâches ou des paramètres. Sur ColorOS ou MIUI, autorisez le démarrage automatique et ajoutez l'application à la liste blanche de la batterie, sinon le système peut arrêter le service permanent; le mot d'activation ne peut pas non plus être reconnu pendant qu'une autre application enregistre.

La liste des outils, le format d'enregistrement des scripts et le détail des autorisations figurent sur la [page du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent).
