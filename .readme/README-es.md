<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-stove-agent-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Ejecuta tareas en lenguaje natural en AutoJs6 eligiendo scripts registrados y manejando la pantalla paso a paso</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Idiomas

******

El README.md actual admite los siguientes idiomas:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-fr.md)
- Español [es] # actual
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-ar.md)

******

### Introducción

******

3-Stove Agent convierte un objetivo en lenguaje natural en acciones sobre un dispositivo Android que ejecuta AutoJs6. O bien elige un script que el usuario ha registrado para el agente, completa sus parámetros y lo ejecuta; o bien observa la pantalla a través del árbol de nodos de accesibilidad y actúa paso a paso (observar, decidir, actuar, verificar) hasta alcanzar el objetivo, necesitar una confirmación o agotar un presupuesto. Responde a la [discusión #577 de AutoJs6](https://github.com/SuperMonster003/AutoJs6/discussions/577).

3-Stove Agent ofrece una interfaz independiente y un plugin AutoJs6 accesible mediante ai.agent. Las acciones integradas y las llamadas al modelo pasan por AutoJs6. Las herramientas MCP opcionales solo conectan servidores configurados. No se enlaza directamente al proveedor ni se solicita accesibilidad.

******

### Estado

******

La versión 1.2.0 incluye herramientas MCP opcionales, llamadas nativas, observación por capturas y scripts generados, validada en cinco dispositivos reales y en emuladores API 24 / 35 / 36.1. Limitaciones conocidas: los modelos locales pequeños (Gemma 4 E2B / E4B) deciden mal; los fallos tras el cambio de red de la VPN con la conexión automática predeterminada siguen sin resolverse; no se ha validado una tarea visual completa entre UID, AiGoCode gpt-5.6-sol solo pasó las pruebas de imagen inicial e imagen en resultados de herramientas. Consulte [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/ROADMAP.md).

******

### Funciones

******

La implementación actual ofrece estas funciones:

- Selección de scripts: los scripts registrados mediante `project.json` o un comentario de cabecera `@agent` se presentan al modelo con sus descripciones y esquemas de parámetros; el agente elige uno, completa los parámetros, pide confirmación cuando hace falta, lo ejecuta dentro de AutoJs6 y lee su resultado estructurado.
- Manejo de la pantalla paso a paso: el agente observa el árbol de nodos de accesibilidad en forma de texto compacto (y el texto de la pantalla mediante un plugin OCR cuando está instalado), y luego pulsa, escribe, desplaza y presiona teclas a través del intermediario de capacidades de AutoJs6 hasta poder verificar el objetivo.
- Seguridad por diseño: las herramientas de solo lectura se ejecutan automáticamente, las acciones sensibles (pago, envío, borrado, escritura de archivos, shell, gestos por coordenadas, scripts registrados como sensibles) requieren confirmación de forma predeterminada, y cada ejecución tiene presupuestos de pasos, llamadas al modelo, duración y tokens. Una confirmación puede valer una vez o hasta que termine la tarea; Ajustes también ofrece el modo prudente y el acceso completo, que omite las confirmaciones y queda claramente señalado. La lista de aplicaciones de pago y la tabla de palabras clave sensibles se pueden ampliar en la pantalla de ajustes Reconocimiento de riesgos; las entradas integradas no se pueden quitar.
- API de script e interfaz de usuario: `ai.agent.run(goal, options)` devuelve un manejador `AgentRun` con eventos, respuestas y cancelación; la aplicación independiente ofrece un espacio de tareas con historial, preajustes, memoria de preferencias, ajustes e historial de versiones.
- Llamadas nativas mediante el host: esquemas del catálogo, validación del lote completo, ejecución secuencial, confirmaciones individuales, devolución de resultados y registro compartido
- Observación de capturas mediante AutoJs6 en Android 11+: screen_capture limita el lado mayor a 1280 y usa JPEG de calidad 70, con instrucciones visuales, presupuesto de tokens de imagen e imágenes en resultados de herramientas nativas
- JavaScript generado mediante script_run_source: el grupo script_dynamic está desactivado inicialmente. Cada llamada muestra un resumen ampliable al código completo para aprobarlo una vez o durante la tarea actual; el acceso completo omite esta revisión. La ejecución ofrece plazo, cancelación, resultados estructurados y código en el historial privado. Tanto el UTF-8 como su cadena JSON tienen un límite de 8 KiB.
- Herramientas MCP de servidores locales o externos seleccionados, con riesgo por servidor y el grupo mcp desactivado inicialmente
- Aplicación independiente rediseñada con Material 3: el inicio es un flujo de tareas con el área de escritura fija sobre el teclado, una barra superior con la cápsula de modelo, el historial y un menú (Nueva tarea, Preajustes, Memoria, Carpetas de scripts, Servidores MCP, Ajustes), un aviso solo mientras AutoJs6 no está conectado, una cronología de pasos que se actualiza por paso, y Ejecutar de nuevo, que rellena la escritura sin iniciar. Ajustes organizados en secciones claras y apariencia clara/oscura coherente

### Capturas de pantalla

Interfaz inglesa real en Android API 37.1 con tareas de ejemplo y un modelo de respuestas programadas. Las imágenes muestran la interfaz y no demuestran éxito con un modelo real. No contienen datos privados de cuentas. [Procedimiento de captura](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/README.md).

| Panel de tareas | Detalles de la tarea |
| --- | --- |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/workbench.png?raw=true" alt="Panel de tareas" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/detail.png?raw=true" alt="Detalles de la tarea" width="288" /> |
| Confirmación de acciones | Entrada flotante de tareas |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/confirmation.png?raw=true" alt="Confirmación de acciones" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/floating.png?raw=true" alt="Entrada flotante de tareas" width="288" /> |

******

### Instalación

******

1. Instale el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/releases) en un dispositivo con AutoJs6 build 5293 o posterior.
2. Abra el centro de plugins AutoJs6 y confirme que `3-Stove Agent` se reconoce. Los paquetes oficiales superan la verificación de firma y se activan tras instalarlos sin confirmación; un plugin desactivado explícitamente sigue desactivado. Este centro es el único interruptor. Los scripts y la interfaz se conectan bajo demanda, sin inicio al arrancar ni repetición de tareas anteriores.

Instale y habilite [3-Stone AI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stone-AI); configure allí un modelo en línea o importe uno local compatible. El intermediario actual del anfitrión selecciona 3-Stone AI; otro Provider necesita integración en el anfitrión. Elija el modelo con la cápsula de modelo del inicio de 3-Stove Agent. Solo aparece un aviso allí mientras AutoJs6 no está conectado.

### Compatibilidad

Android 7.0+ (API 24). Requiere un host AutoJs6 6.8.0 / build 5298+, que ya incluye todos los cambios necesarios para la API de tareas (build 5293+) y para las llamadas nativas y la entrada de imágenes (build 5297+). Operar la pantalla requiere la accesibilidad del anfitrión; Agent la inicia primero con el método automático configurado en AutoJs6 (Root, ajustes seguros o Shizuku) y solo le pide activarla si eso falla. OCR es opcional y requiere un complemento instalado, autorizado y disponible según el anfitrión. 3-Stove Agent no guarda credenciales de modelos ni tiene servicio de accesibilidad propio.

### Inicio desde la interfaz

Abre 3-Stove Agent, conecta AutoJs6, introduce un objetivo y comienza. La cápsula de modelo del inicio elige un modelo en línea o local, o Automático (primero un modelo en el dispositivo; si no, el primero disponible). Busca modelos, fija tus favoritos y reutiliza los recientes; las insignias muestran la compatibilidad declarada con herramientas e imágenes. El panel y la burbuja flotante comparten esta selección para tareas nuevas sin editar preajustes ni cambiar la tarea en curso; los preajustes ya no incluyen modelo. El chip de preajuste del área de escritura elige un preajuste opcional. Responde preguntas y sigue el progreso en la tarjeta de tarea.

### Inicio desde un script

Ejecute este JavaScript en AutoJs6 tras conectar 3-Stove Agent y configurar un modelo. La interfaz del complemento recibe preguntas y confirmaciones. Para usar una configuración guardada, añada `preset: "your-preset-name"` a las opciones. Añada `plan: true` para que la tarea proponga primero un plan para su revisión.

```javascript
let run = ai.agent.run('Lee la versión de Android e informa del valor observado.', {
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

Compruebe `result.status`: una promesa resuelta puede devolver completed, partial, failed, blocked o cancelled. `run.cancel()` detiene la tarea. Consulte [ai.agent API](https://docs.autojs6.com/#ai) para modelos, eventos, presupuestos y respuestas desde scripts.

### Registrar un script

Guarde el ejemplo como `text-counter.js` en el directorio de trabajo de AutoJs6 o uno aprobado por el anfitrión. El JSDoc inicial con `@agent` registra el archivo. Pida al agente contar los caracteres de un texto; solicitará los parámetros obligatorios que falten antes de ejecutarlo.

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

También puede colocar este `project.json` junto a `main.js`, cuyo código lee `ai.agent.context().parameters` y llama a `ai.agent.result(...)` como arriba. El registro del proyecto se coloca en el objeto `agent`.

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

Se admiten string, number, integer y boolean; no objetos anidados ni matrices. Los scripts sensitive requieren confirmación previa, salvo con acceso completo. Registre solo scripts revisados: declarar el riesgo no aísla JavaScript. [Formato completo](https://github.com/SuperMonster003/AutoJs6/blob/master/docs/dev/agent-script-manifest-v1.md).

### Catálogo de herramientas

La tabla se genera desde el ToolCatalog incluido. El objetivo real de pantalla puede elevar el riesgo; el modo cauteloso también confirma acciones que no sean de solo lectura y el acceso completo omite la confirmación de las herramientas activadas. Ajustes, preajustes, opciones y permisos del anfitrión limitan los grupos disponibles.

| Herramienta | Grupo | Riesgo | Predeterminado | Descripción |
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

### Preajustes y memoria

Abra Preajustes en el panel para guardar una configuración. Los nombres identifican scripts y ámbitos de memoria; copie el preajuste para usar otro nombre. El default integrado se puede editar pero no eliminar. Los preajustes no incluyen modelo; un modelo guardado por una versión anterior se conserva solo para scripts. Las opciones de tarea solo pueden reducir los límites del preajuste. El contexto fijo y el de la tarea comparten un límite de 8 KiB. La memoria puede incluir entradas globales y del preajuste, solo uno de los dos ámbitos, o ninguno. Editar o eliminar no cambia las tareas en cola. Almacenamiento privado: hasta 32 preajustes / 1 MiB. Los preajustes se pueden exportar a JSON e importar tras revisarlos uno por uno; el archivo no contiene ningún modelo, y los grupos de herramientas o directorios de scripts ausentes en este dispositivo se descartan al importar. El modo plan hace que el modelo proponga de 3 a 8 pasos que usted revisa, y puede editar, antes de la ejecución, y un plan nuevo cuando el anterior deja de encajar; está desactivado de forma predeterminada.

Abra Memoria para consultar, editar, eliminar o respaldar preferencias. Hasta 500 entradas / 256 KiB, con ámbito, tarea de origen y fechas. Confirme cada memory_propose y cada entrada importada. Cree primero los preajustes que falten. La inyección automática conserva entradas completas recientes del ámbito permitido, hasta 4 KiB; el preajuste actual prevalece sobre claves globales iguales. memory: false solo desactiva la inyección. Desactive el grupo memory o el ámbito para impedir también consultas y propuestas. La exportación incluye valores reales y procedencia. No almacene credenciales; se rechazan claves y formatos de token reconocibles.

### Uso

- Configure carpetas adicionales en "Directorios de scripts" del lanzador, una ruta absoluta por línea. El anfitrión valida y aplica las rutas guardadas; las tareas solo pueden reducir las carpetas aprobadas.
- Hasta 200 tareas / 32 MiB. Se eliminan primero las tareas terminadas consultadas hace más tiempo. Repetir rellena el objetivo y preajuste originales en el panel. Revísalos y pulsa Iniciar tarea para ejecutarla. Vaciar el historial conserva las tareas en curso. Se conservan contadores, nombres de herramientas y confirmaciones. Se eliminan objetivos, parámetros, observaciones y resultados de scripts. Elige dónde guardar el archivo. Compartir resumen en la página de detalles entrega a la hoja de compartir del sistema el objetivo, el estado, el resumen, las evidencias y el trabajo pendiente, nunca las observaciones.
- Responda en las tareas en primer plano o abra la notificación prioritaria en segundo plano. La confirmación muestra herramienta, parámetros, riesgo y tiempo restante. Permitir siempre en esta sesión aprueba la misma herramienta con el mismo riesgo hasta que termine la tarea, incluidas propuestas de memoria o códigos generados posteriores; los pagos requieren su propia aprobación. Recordar una respuesta crea una propuesta memory_propose separada en el ámbito permitido. Las confirmaciones esperan normalmente 120 segundos y las preguntas hasta 10 minutos, dentro del presupuesto de la tarea. Al expirar se devuelve USER_TIMEOUT; el modelo decide si pregunta de nuevo o informa un resultado parcial. Las solicitudes antiguas no responden a las nuevas. Los permisos y canales afectan a las notificaciones.
- Abra Ajustes desde tareas para elegir grupos, presupuestos, permisos de operación (estándar, prudente o acceso completo), voz y perfil predeterminado. Cada cambio se guarda al instante y afecta a tareas nuevas. El acceso completo ejecuta las herramientas activadas, incluidos pagos, sin aprobación; mientras está activo, las tareas, la burbuja flotante y el historial muestran un aviso. gesture/files/shell/script_dynamic empiezan desactivados; OCR requiere un complemento autorizado y disponible en el anfitrión. Los límites en automático usan los valores iniciales; la duración se indica en minutos y todo respeta los límites del protocolo. Perfiles y opciones solo pueden reducirlos. La gestión muestra cantidades y bytes; borrar una categoría exige confirmación y ninguna tarea activa. Borrar perfiles restaura default. Perfiles, memoria, carpetas de scripts y servidores MCP también se abren desde Ajustes. Los avisos de tareas agrupan errores y finalizaciones en dos filas de resumen, cada una con opciones independientes de notificación, toast y diálogo. Los valores iniciales activan las tres para errores y notificación más toast para finalizaciones; se conservan las elecciones guardadas. Una finalización normal, incluso parcial sin error, avisa una vez; cancelar o consultar el historial no avisa. En el banco de tareas el chip de acceso cambia los permisos de operacion directamente, el chip de preajuste abre un panel para elegir o gestionar preajustes y el menu superior activa la burbuja flotante.
- Abre Ajustes desde el menú superior derecho. El idioma, modo oscuro y color pueden seguir AutoJs6 o configurarse por separado. El idioma y modo oscuro también pueden seguir Android. El historial y los avisos legales se incluyen sin conexión. Las comprobaciones manuales de GitHub guardan resultados correctos durante 24 horas. Las automáticas están desactivadas por defecto. Al activarlas se intentan durante el uso de la app, como máximo cada 12 horas, sin avisar de fallos ni versiones ignoradas y sin descargar APK. Gestionar actualizaciones ignoradas permite restaurar versiones individualmente. Acerca de muestra la versión, el desarrollador, el código fuente, la licencia y los avisos de terceros. Unificar los ajustes con grupos planos, filas coherentes y diálogos redondeados centrados. El idioma, el modo nocturno, el color y el icono solo cambian al confirmar; Cancelar conserva los valores guardados. El color sigue AutoJs6 por defecto, con una paleta común, entrada HEX/RGB y vista previa local. Los fondos neutros se mantienen estables y los controles siguen el tema. El icono usa el modo adaptativo automático por defecto, conservando las elecciones explícitas al actualizar.
- Activa la burbuja en Ajustes y permite la superposición. Está desactivada por defecto y solo aparece con AutoJs6 conectado; se oculta al bloquear o desconectar, sin servicio en primer plano en reposo. Arrastra para moverla y pulsa para introducir un objetivo, elegir un preajuste, responder o detener. Contraer la tarjeta restaura las notificaciones de confirmación. Comparte texto sin formato, usa Nueva tarea o fija un preajuste con objetivo opcional. Cada entrada abre un borrador editable y requiere iniciar explícitamente. Los preajustes eliminados no se sustituyen en silencio. La voz usa el idioma de la interfaz, se oculta si no está disponible y rellena sin enviar.

### Preguntas frecuentes

**Por qué se necesita AutoJs6?**

El complemento gestiona el ciclo y la interfaz. AutoJs6 gestiona modelos, accesibilidad y ejecución de scripts registrados. Sin un anfitrión compatible conectado puede leer el historial pero no iniciar tareas de dispositivo. Su desconexión bloquea las tareas activas; reconectarlo no las repite automáticamente.

**Cuándo se confirman los pagos?**

Pagar es una acción sensible independiente. Aprobar un pedido, script u otra acción no autoriza un pago. De forma predeterminada, cada acción de pago detectada exige confirmación propia y el tiempo agotado implica rechazo. Elegir Permitir siempre en esta sesión en una solicitud de pago solo cubre los pagos posteriores de esa herramienta en la misma tarea. El acceso completo omite la confirmación de pagos: actívelo solo con objetivos y modelos de confianza. Revise comercio, productos, dirección e importe antes de aprobar.

**Qué limitaciones tienen los modelos locales?**

Cargar un modelo no garantiza completar tareas. La prueba registrada de validación de decisiones Wi-Fi con Gemma 4 E2B IT no pasó; este destino mantiene JSON. Las llamadas nativas requieren host y destino compatibles y conservan validación, confirmaciones y presupuestos. Empiece con tareas pequeñas y revise partial/failed. La entrada de imágenes requiere un destino que las acepte; AiGoCode gpt-5.6-sol pasó las pruebas de imagen inicial e imagen en resultados de herramientas, mientras que otros destinos requieren una verificación aparte. Los scripts generados requieren activación expresa y cada código sigue la política de confirmación.

******

### Permisos y seguridad

******

El plugin sigue límites explícitos:

- Lista de permisos: org.autojs.permission.PLUGIN (entradas del contrato del anfitrion), FOREGROUND_SERVICE y FOREGROUND_SERVICE_SPECIAL_USE (servicio en primer plano mientras se ejecuta una tarea), POST_NOTIFICATIONS (avisos de confirmacion y progreso en segundo plano), INTERNET (comprobacion manual o automatica de versiones en GitHub y conexion a servidores MCP configurados por el usuario), ACCESS_LOCAL_NETWORK (solicitado solo desde los ajustes de MCP en Android 17+), SYSTEM_ALERT_WINDOW (solicitado solo al activar la burbuja flotante en Ajustes). No se solicitan permisos de accesibilidad, almacenamiento ni microfono, y el trafico del modelo nunca pasa por el complemento.
- Las entradas Binder requieren el permiso de firma org.autojs.permission.PLUGIN. El lanzador (incluidos accesos directos) y el destino text/plain ACTION_SEND son públicos y solo reciben borradores limitados. Los Intent externos no pueden ejecutar tareas, confirmar ni cambiar permisos. Ajustes, resultados de voz y controles son privados.
- 3-Stove Agent ofrece una interfaz independiente y un plugin AutoJs6 accesible mediante ai.agent. Las acciones integradas y las llamadas al modelo pasan por AutoJs6. Las herramientas MCP opcionales solo conectan servidores configurados. No se enlaza directamente al proveedor ni se solicita accesibilidad.
- Las credenciales del modelo permanecen en su proveedor; AutoJs6 transmite sus llamadas. Los tokens MCP Bearer se cifran con Android Keystore en almacenamiento privado y no se incluyen en prompts ni exportaciones del historial. INTERNET también conecta los servidores MCP configurados; Android 17+ solicita acceso a la red local desde Ajustes de MCP. El riesgo por servidor empieza en SENSITIVE. Cancelar no revierte acciones remotas; las llamadas fallidas no se repiten automáticamente.
- El historial de tareas, los preajustes y la memoria de preferencias permanecen en el almacenamiento privado del plugin; las copias de seguridad y las transferencias entre dispositivos están desactivadas.
- Las capturas se envían mediante AutoJs6 al modelo elegido, que puede estar en línea. La pantalla debe estar activa y desbloqueada. El historial guarda dimensiones y bytes, sin el contenido de las imágenes. Las decisiones JSON conservan la imagen actual hasta otra observación o respuesta. Las conversaciones nativas conservan imágenes previas dentro de los límites del lote y de la sesión, reservando sus tokens en cada ronda.
- Los scripts generados usan permisos de AutoJs6 sin aislamiento JavaScript y pueden actuar fuera de los grupos habilitados. El código completo permanece en pasos privados, sujeto a eliminación de contraseñas y retención del historial. Un código modificado por esa eliminación posterior no puede guardarse como original. Revise los .js antes de compartirlos.
- El acceso completo solo se activa en los ajustes privados del complemento; ni la salida del modelo, ni el contenido de pantalla, ni solicitudes de scripts o Intents externos pueden activarlo o ampliarlo. Omite las confirmaciones de las herramientas activadas, incluidos pagos, pero no activa grupos adicionales ni relaja presupuestos o permisos del anfitrión. El anfitrión inicia la accesibilidad con el método configurado en AutoJs6; el complemento sigue sin solicitar permiso de accesibilidad.

Obtenga el plugin únicamente desde la página oficial de [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/releases) o el centro de plugins de AutoJs6. Los paquetes de origen desconocido pueden fallar la verificación del anfitrión o conllevar riesgos aunque el número de versión parezca idéntico.

******

### Interfaz del plugin

******

La siguiente información está dirigida a desarrolladores del anfitrión AutoJs6 y de plugins; el anfitrión usa estos identificadores para descubrir el plugin y negociar la compatibilidad:

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

`ThreeStoveAgentPluginService` / `IThreeStoveAgentPlugin` / `IThreeStoveAgentLink`: Conexión con identidad del anfitrión verificada, cola de tareas, respuestas, cancelación, consultas e historial privado; las tareas se bloquean al perder el anfitrión y no se reanudan al reiniciar el proceso.

******

### Hoja de ruta

******

Los planes y el progreso del plugin se mantienen como una lista verificable en ROADMAP.md, organizada por fases con criterios de aceptación y niveles de evidencia. Los elementos sin marcar expresan intención y no capacidades actuales; la discusión mediante Issues es bienvenida.

- [Ver ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/ROADMAP.md)

******

### Historial de versiones

******

#### v1.3.0

_2026/09/30_

- `Función` Reconocimiento de riesgos configurable: una nueva pantalla de ajustes Reconocimiento de riesgos añade sus propios nombres de paquete y palabras clave sobre la lista integrada de aplicaciones de pago (Alipay, AlipayHK, UnionPay, PayPal, Google Wallet, Samsung Pay, Huawei Wallet, Mi Pay) y la tabla de palabras clave sensibles en diez idiomas; las adiciones solo amplían las listas y se aplican de inmediato, y las acciones en pantalla coincidentes pasan a ser sensibles y requieren confirmación
- `Función` Rediseño de la bola flotante: en modo reducido muestra la tarea y su paso actual en dos líneas y tocar el texto abre una tarjeta con la cronología de pasos; la tarjeta de control se condensa en las mismas dos filas que el espacio de trabajo (preajuste, modelo y acceso sobre el campo, voz e inicio), el modelo y el modo de acceso se cambian dentro de la superposición y se comparten con la aplicación; la cronología sigue los pasos nuevos mientras está al final, se detiene al subir y se reanuda al final, igual que la pantalla de detalles de la tarea
- `Función` Importación / exportación de preajustes: la pantalla de preajustes añade Importar JSON y Exportar JSON; el archivo exportado contiene la configuración de cada preajuste pero ningún modelo, cada preajuste importado se revisa uno por uno con un aviso de reemplazo para un nombre existente, los grupos de herramientas o directorios de scripts ausentes en este dispositivo se descartan, y una revisión pendiente se restaura tras salir de la pantalla
- `Función` Herramientas de observación completadas: las herramientas de solo lectura app_list (aplicaciones instaladas, filtrables por un fragmento del nombre de paquete o de la etiqueta, como máximo 200 filas), app_installed (si un paquete está instalado) y script_list (ejecuciones de scripts en curso en AutoJs6 con sus identificadores y estados, para usar con script_stop) corresponden a package_manager.listApps, app.isInstalled y engines.list, que la concesión del anfitrión ya permitía; se suman a los grupos de observación y scripts y están habilitadas de forma predeterminada
- `Función` Modo plan (un interruptor del preajuste, desactivado de forma predeterminada; los scripts pueden anularlo con options.plan): el modelo propone primero de 3 a 8 pasos, la pantalla de tareas, la burbuja flotante y la pantalla de confirmación muestran una tarjeta de revisión del plan editable, y tras la aprobación el entorno pasa el plan al modelo con cada petición y espera que se siga en orden; cuando el plan deja de encajar, el modelo propone uno nuevo para otra revisión; el esquema de decisión gana una rama plan aceptada solo en modo plan, y la cronología y el historial registran los pasos del plan
- `Función` Compartir el resultado de una tarea: el menú de detalles gana Compartir resumen, que entrega a la hoja de compartir del sistema texto sin formato con el objetivo, el estado y el resumen, las evidencias y el trabajo pendiente; las observaciones, los argumentos, los resultados de scripts y los detalles de error nunca salen del historial privado, y la opción permanece desactivada mientras la tarea sigue en ejecución
- `Función` Los avisos de tareas agrupan errores y finalizaciones en dos filas de resumen, cada una con opciones independientes de notificación, toast y diálogo. Los valores iniciales activan las tres para errores y notificación más toast para finalizaciones; se conservan las elecciones guardadas. Una finalización normal, incluso parcial sin error, avisa una vez; cancelar o consultar el historial no avisa
- `Función` Unificar los ajustes con grupos planos, filas coherentes y diálogos redondeados centrados. El idioma, el modo nocturno, el color y el icono solo cambian al confirmar; Cancelar conserva los valores guardados. El color sigue AutoJs6 por defecto, con una paleta común, entrada HEX/RGB y vista previa local. Los fondos neutros se mantienen estables y los controles siguen el tema. El icono usa el modo adaptativo automático por defecto, conservando las elecciones explícitas al actualizar.
- `Corrección` El botón de envío ya no se queda en la primera línea de un objetivo de varias líneas: se alinea abajo como el botón del micrófono
- `Corrección` En el modo de plan, la solicitud del plan ya no ofrece definiciones de herramientas a los modelos en línea con llamadas nativas a herramientas, de modo que el modelo solo puede devolver un plan en lugar de llamar a una herramienta (en un dispositivo real, un modelo tipo Codex respondió tres veces seguidas con llamadas a herramientas y la tarea falló con DECISION_UNPARSABLE); las llamadas nativas se reanudan cuando el plan se aprueba
- `Corrección` Las rondas de continuación de la llamada nativa a herramientas ya no heredan el resto del tiempo límite del modelo de la primera solicitud: cada ronda (el modelo devuelve llamadas a herramientas, las herramientas se ejecutan, sale la siguiente solicitud) vuelve a obtener el tiempo límite completo del modelo, y el turno entero solo queda limitado por el presupuesto de duración de la tarea y el tope de rondas. En un dispositivo real, las tareas nativas de más de 5 minutos (por ejemplo leer una calculadora tras varias capturas de pantalla) terminaban exactamente a los 300 s con MODEL_TIMEOUT. Requiere un host posterior a AutoJs6 5298 y un 3-Stone AI posterior a 1.2.1, que reinician por ronda de la misma manera
- `Corrección` La casilla Burbuja flotante del menú Más sigue ahora el color del tema
- `Corrección` Mantener dinámico el recurso del icono automático tras la instalación para que el lanzador pueda cargar la variante clara u oscura correspondiente.
- `Corrección` Una consulta de ejecución podía devolver brevemente un estado terminal sin su resultado mientras la tarea terminaba (fallo ocasional de la suite de conformidad del host simulado en la CI remota): el estado terminal ahora se archiva junto con el resultado, por lo que hosts y scripts nunca ven una tarea finalizada sin él
- `Mejora` Se retiran los botones Reintentar con otro modelo (el modelo se cambia en la parte superior del espacio de trabajo o dentro de la bola flotante); Historial de tareas y Abrir espacio de trabajo de la bola pasan al menú Más; el icono de historial usa el glifo estándar
- `Mejora` Tras un tools/list_changed enviado por un servidor MCP, las definiciones de herramientas congeladas se vuelven a verificar antes de la siguiente llamada: las que no cambiaron siguen funcionando, y solo una definición cambiada falla con MCP_CATALOG_CHANGED indicando al modelo que pida al usuario actualizar la selección de herramientas en los ajustes de MCP y comenzar una tarea nueva; la nota del punto de conexión ahora indica que el inicio de sesión OAuth y el transporte HTTP+SSE antiguo no son compatibles
- `Mejora` Se elimina la migración única introducida en 1.2.0: una elección de modelo antigua en las preferencias de borrador del banco de trabajo ya no se traslada a model-selection.json, por lo que una elección guardada por una versión anterior debe volver a hacerse en el banco de trabajo
- `Mejora` El interruptor "Activar entrada de voz" de Ajustes ahora se explica: el micrófono solo aparece cuando hay instalada una aplicación de reconocimiento de voz, el reconocimiento se realiza en ella y este plugin no graba nada; en dispositivos sin una (algunas ROM chinas, por ejemplo) el motivo se muestra ahí mismo en lugar de que el banco de trabajo se quede sin micrófono en silencio tras activar el interruptor
- `Mejora` El centro de plugins es el único interruptor. Los paquetes oficiales se activan tras instalarlos sin confirmación y se conservan las desactivaciones explícitas. Los scripts y la interfaz se conectan bajo demanda, sin inicio al arrancar ni repetición de tareas
- `Mejora` Mantener el marco redondeado del icono de Acerca de con el interior transparente sobre el fondo de la página. Mostrar las opciones del lanzador desde arriba con notas más pequeñas.
- `Mejora` Cuando una tarea falla porque el host fundió la fuente del modelo (RATE_LIMITED: FUSED), el resultado explica la causa y qué hacer (esperar y reintentar, reiniciar AutoJs6 si persiste); el host también levanta ahora el fusible automáticamente cuando vuelve la transacción bloqueante agotada, así que las tareas posteriores no requieren reinicio

#### v1.2.0

_2026/09/28_

- `Aviso` La aplicacion pasa a llamarse 3-Stove Agent: el ID de aplicacion es ahora io.github.supermonster003.autojs6.plugin.three.stove.agent, el repositorio es AutoJs6-Plugin-Three-Stove-Agent, el ID y el engine del plugin son three-stove-agent, la accion del servicio es org.autojs.plugin.THREE_STOVE_AGENT y la version del contrato es 2. El nombre antiguo no es compatible: desinstale el antiguo AI Agent antes de instalar; el historial, los preajustes y las memorias no se migran. El anfitrion minimo es ahora AutoJs6 6.8.0 / build 5298; los anfitriones anteriores ya no reconocen este plugin
- `Aviso` La versión 1.2.0 incluye herramientas MCP opcionales, llamadas nativas, observación por capturas y scripts generados, validada en cinco dispositivos reales y en emuladores API 24 / 35 / 36.1. Limitaciones conocidas: los modelos locales pequeños (Gemma 4 E2B / E4B) deciden mal; los fallos tras el cambio de red de la VPN con la conexión automática predeterminada siguen sin resolverse; no se ha validado una tarea visual completa entre UID, AiGoCode gpt-5.6-sol solo pasó las pruebas de imagen inicial e imagen en resultados de herramientas. Consulte ROADMAP.md.
- `Función` El compositor muestra el modo de acceso actual (Estandar / Prudente / Acceso completo; solo Acceso completo en rojo) y lo cambia al tocarlo; el menu superior incorpora una casilla "Burbuja flotante" sincronizada con el ajuste; la tarjeta flotante desplegada se minimiza al tocar fuera, se ajusta a su contenido y tiene un boton Mas con Minimizar y Desactivar; el campo de objetivo de una linea centra el cursor con los botones de voz y envio, que se mantienen alineados abajo cuando el campo crece
- `Función` Panel de preajustes del banco de tareas: al tocar el chip de preajuste se abre un panel para elegir el preajuste y gestionarlo en el sitio (nuevo, editar, copiar, establecer como predeterminado, eliminar con confirmacion) o abrir la pantalla completa de preajustes
- `Función` Avisos de fallo: Ajustes incorpora una seccion "Avisos de fallo" con interruptores independientes para una notificacion (activada por defecto), un mensaje flotante y un dialogo; cuando una tarea se detiene por un error, un limite de presupuesto o la perdida del anfitrion, el proceso del agente emite los avisos elegidos, la notificacion abre los detalles de la tarea y en Android 10+ el dialogo requiere el permiso de superposicion o se sustituye por una notificacion; las tareas completadas o canceladas no avisan
- `Función` Herramientas MCP de servidores locales o externos seleccionados, con riesgo por servidor y el grupo mcp desactivado inicialmente
- `Función` Abre 3-Stove Agent, conecta AutoJs6, introduce un objetivo y comienza. La cápsula de modelo del inicio elige un modelo en línea o local, o Automático (primero un modelo en el dispositivo; si no, el primero disponible). Busca modelos, fija tus favoritos y reutiliza los recientes; las insignias muestran la compatibilidad declarada con herramientas e imágenes. El panel y la burbuja flotante comparten esta selección para tareas nuevas sin editar preajustes ni cambiar la tarea en curso; los preajustes ya no incluyen modelo. El chip de preajuste del área de escritura elige un preajuste opcional. Responde preguntas y sigue el progreso en la tarjeta de tarea.
- `Función` Abre Ajustes desde el menú superior derecho. Cada cambio se aplica al instante, sin botón Guardar: apariencia, permisos de operación, grupos de herramientas, límites (duración en minutos), entrada de voz, burbuja flotante y limpieza de datos. El idioma, modo oscuro y color pueden seguir AutoJs6 o configurarse por separado. El idioma y modo oscuro también pueden seguir Android. El historial y los avisos legales se incluyen sin conexión. Las comprobaciones manuales de GitHub guardan resultados correctos durante 24 horas. Las automáticas están desactivadas por defecto. Al activarlas se intentan durante el uso de la app, como máximo cada 12 horas, sin avisar de fallos ni versiones ignoradas y sin descargar APK. Gestionar actualizaciones ignoradas permite restaurar versiones individualmente. Acerca de muestra la versión, el desarrollador, el código fuente, la licencia y los avisos de terceros.
- `Función` Las tareas de pantalla inician primero la accesibilidad con el método automático configurado en AutoJs6 (Root, ajustes seguros o Shizuku). Solo si falla o no hay ninguno configurado, la tarjeta de la tarea pide activarla y ofrece un acceso a los ajustes de accesibilidad.
- `Función` Los permisos de operación de Ajustes incluyen Acceso completo: las herramientas activadas, incluidos pagos, borrados, scripts y escritura de memoria, se ejecutan sin aprobación. No activa grupos de herramientas adicionales ni relaja presupuestos o permisos del anfitrión. El panel, la burbuja flotante, la tarea actual y el detalle del historial muestran una etiqueta visible en lugar de un diálogo. Las tareas que piden explícitamente confirmación prudente la mantienen.
- `Función` Las tarjetas de confirmación añaden Permitir siempre en esta sesión: hasta que termine la tarea, la misma herramienta con el mismo nivel de riesgo se ejecuta sin volver a preguntar, aunque cambien los argumentos. Los pagos requieren su propia aprobación; los scripts generados y las propuestas de memoria también pueden permitirse para la sesión.
- `Corrección` Las tareas se detenían tras varias observaciones de pantallas grandes o rondas de herramientas nativas; ahora conservan los pasos completados y compactan el contexto anterior, respetando las confirmaciones, el presupuesto y el tiempo límite
- `Corrección` En un telefono de 360 dp con el texto al doble de tamano, el aviso de conexion con el host comprimia el boton "Conectar con AutoJs6" a un caracter por linea (visto en un Redmi Note 12 y un Xperia XZ1 Compact); las dos acciones del aviso ahora se apilan cuando no caben una junto a la otra
- `Corrección` La nota de presupuesto del editor de preajustes seguia citando los valores anteriores a la ampliacion (40 pasos, 60 llamadas, 600000 ms, 300000 tokens); ahora coincide con el presupuesto automatico (60 pasos, 90 llamadas, 15 minutos, 30 minutos en tareas independientes, 500000 tokens) y expresa la duracion en minutos
- `Corrección` El chip de permisos de operacion del panel no tenia nombre accesible hasta que llegaba el primer estado, por lo que un lector de pantalla encontraba un boton sin nombre (detectado por la auditoria de diseno de CI en API 35); ahora se llama "Permisos de operacion" hasta que el estado aporte el modo
- `Corrección` Los motivos de parada son especificos: el resumen final lleva la causa entre corchetes con la dimension del presupuesto y usado/limite (pasos 60/60, tiempo 900 s/900 s), que limite se supero (tamano de la respuesta del modelo, contexto por encima del limite de entrada del modelo, lote de resultados de herramientas) o el codigo de error con el motivo fijo del anfitrion (MODEL_FAILED: ONLINE_NETWORK_UNAVAILABLE); antes se descartaban los motivos distintos de REQUEST_REJECTED
- `Corrección` Un nombre de ambito largo en la lista de memoria ya no expulsa la clave de la entrada fuera de su fila: la insignia de ambito se recorta en una linea y conserva el nombre completo en su descripcion de accesibilidad (detectado por la auditoria de diseno de CI en API 24 / 360 dp)
- `Corrección` Cuando una excepcion interna hace fallar una tarea, el registro del paso conserva la clase de la excepcion (nunca su mensaje) para el diagnostico; un tiempo de espera de herramienta ahora indica la dimension del limite de tiempo de la herramienta en el resultado; el descubrimiento de herramientas MCP se limita a 8 segundos para no consumir la ventana de preparacion de 15 segundos
- `Corrección` Las capacidades del complemento ahora declaran native-tools y vision, los limites de ejecucion se vinculan directamente a las constantes del contrato del anfitrion, la version del cliente MCP proviene del paquete instalado y los literales dispersos de tiempo y tamano referencian el contrato
- `Corrección` Un servicio de accesibilidad de AutoJs6 detenido se comunica al modelo como A11Y_SERVICE_NOT_RUNNING en lugar de un error de argumentos
- `Mejora` Las pantallas de gestion se unifican: el editor de preajustes da al contexto fijo su propia seccion "Contexto", el editor de servidores MCP usa la misma barra de acciones fija (Eliminar / Guardar) que los editores de preajustes y memoria, la nota de retencion del historial comparte el estilo de las demas pantallas y la tarjeta flotante deja un espacio entre su cabecera y su contenido
- `Mejora` Los detalles de la tarea muestran modelo, preajuste, duracion y limites de tarea en dos columnas alineadas de clave / valor, con los valores largos ajustados junto a la columna de etiquetas, con el mismo estilo que la tabla de parametros de confirmacion
- `Mejora` La tarjeta de tarea actual tiene una sola fila de estado: el estado en su color (acento en ejecucion, verde completada, rojo fallida, ambar parcial) con el modelo y el preajuste en la misma linea, y el presupuesto es una linea compacta "Paso n/m · llamadas · min · tokens"; en los detalles "Ejecutar de nuevo" es la accion principal a todo el ancho y "Reintentar con otro modelo" va en su propia linea sin partirse
- `Mejora` El presupuesto automatico de tareas se amplia: pasos 40 -> 60, llamadas al modelo 60 -> 90, duracion 10 -> 15 minutos, tokens 300k -> 500k; los ajustes, los preajustes y cada tarea solo pueden reducirlo
- `Mejora` El icono del lanzador es la ilustracion Three Stove proporcionada por el mantenedor: un glifo oscuro sobre gris claro en modo claro, un glifo claro sobre gris oscuro en modo oscuro, con los iconos redondo y adaptable compuestos a partir de la misma imagen
- `Mejora` La etiqueta de paso de la burbuja flotante ofrece su texto completo a los lectores de pantalla bajo el rol truncable, el aviso de modelos fijados llenos es una barra en pagina y el lanzador declara un icono redondo; el kit de interfaz elimina miembros sin uso y comparte sus constructores de parrafo y nota
- `Mejora` El catalogo de herramientas declara la confirmacion obligatoria de propuestas de memoria y scripts generados con el atributo confirmAlways, y los nombres de herramientas integradas se referencian mediante constantes ToolNames que la prueba de instantanea mantiene alineadas con el catalogo
- `Mejora` Las credenciales del modelo permanecen en su proveedor; AutoJs6 transmite sus llamadas. Los tokens MCP Bearer se cifran con Android Keystore en almacenamiento privado y no se incluyen en prompts ni exportaciones del historial. INTERNET también conecta los servidores MCP configurados; Android 17+ solicita acceso a la red local desde Ajustes de MCP. El riesgo por servidor empieza en SENSITIVE. Cancelar no revierte acciones remotas; las llamadas fallidas no se repiten automáticamente.
- `Mejora` Aplicación independiente rediseñada con Material 3: el inicio es un flujo de tareas con el área de escritura fija sobre el teclado, una barra superior con la cápsula de modelo, el historial y un menú (Nueva tarea, Preajustes, Memoria, Carpetas de scripts, Servidores MCP, Ajustes), un aviso solo mientras AutoJs6 no está conectado, una cronología de pasos que se actualiza por paso, y Ejecutar de nuevo o Reintentar con otro modelo que rellenan la escritura sin iniciar. Ajustes organizados en secciones claras y apariencia clara/oscura coherente
- `Mejora` Las confirmaciones muestran el nivel de riesgo, el grupo de herramientas y cada parámetro en una tabla legible en lugar de JSON sin procesar, con acciones claras: permitir una vez, permitir siempre en esta sesión y denegar. La burbuja flotante usa el mismo diseño Material, elige el preajuste dentro de la tarjeta y su fila de modelo abre el selector de modelo compartido
- `Mejora` El historial añade búsqueda, chips de estado y filtros por preajuste y rango de fechas, y borra las tareas terminadas desde su menú. Los detalles de la tarea muestran el modelo, una cronología de pasos con tablas de parámetros y observaciones desplegables, Ejecutar de nuevo o Reintentar con otro modelo, y un menú para exportar diagnósticos, eliminar el registro o usar el modelo de la tarea en tareas nuevas
- `Mejora` Preajustes, memoria, servidores MCP y carpetas de scripts comparten el mismo diseño: tarjetas de preajustes con menú de fila y editor a pantalla completa (duración en minutos, botón Guardar fijo), búsqueda y chips de ámbito en la memoria, lista de herramientas MCP con interruptor de activación y elección de riesgo, y confirmación antes de descartar cambios sin guardar
- `Dependencia` Actualizacion de los tres artefactos API del host release a AutoJs6 86d9bfa26b / build 5298: ai-agent-api pasa a ser three-stove-agent-api (paquete AIDL org.autojs.plugin.three.stove.agent.api, version de contrato 2), con common-plugin-api y host-capability-api rebloqueados desde la misma compilacion
- `Dependencia` Actualizar los tres artefactos release de la API del anfitrion a AutoJs6 3cdf7de13c / build 5297 (opcion del grupo mcp de P10 y constante TOOL_FAILED); el contrato base sigue en V1
- `Dependencia` Adición de AndroidX AppCompat 1.7.1 y Material Components for Android 1.13.0 con sus dependencias AndroidX de ejecución para la interfaz Material 3

#### v1.1.0

_2026/09/26_

- `Aviso` 1.1.0 no se publico por separado; todos sus cambios se distribuyen con 1.2.0
- `Aviso` Las llamadas nativas requieren AutoJs6 build 5297+ y un destino tools, como un destino en línea de la versión de desarrollo 3-Stone AI 1.2.0. Los hosts antiguos y destinos no compatibles mantienen JSON. Cada conversación conserva su plazo inicial, límites de contexto/salida y hasta 16 rondas de herramientas; un error tras una acción no reinicia por JSON
- `Aviso` La entrada de imágenes requiere un host compatible, el grupo observe y un modelo visual con esta entrada activada explícitamente. Implementación y pruebas deterministas completas; la validación visual real en línea sigue pendiente. Sistemas anteriores y modelos de texto mantienen observaciones textuales. Consulte ROADMAP.md
- `Aviso` Los scripts generados usan permisos de AutoJs6 sin aislamiento JavaScript y pueden actuar fuera de los grupos habilitados. El código completo permanece en pasos privados, sujeto a eliminación de contraseñas y retención del historial. Un código modificado por esa eliminación posterior no puede guardarse como original. Revise los .js antes de compartirlos.
- `Función` Llamadas nativas mediante el host: esquemas del catálogo, validación del lote completo, ejecución secuencial, confirmaciones individuales, devolución de resultados y registro compartido
- `Función` Observación de capturas mediante AutoJs6 en Android 11+: screen_capture limita el lado mayor a 1280 y usa JPEG de calidad 70, con instrucciones visuales, presupuesto de tokens de imagen e imágenes en resultados de herramientas nativas
- `Función` JavaScript generado mediante script_run_source: el grupo script_dynamic está desactivado inicialmente. Cada llamada exige revisar un resumen ampliable al código completo y dar aprobación individual. La ejecución ofrece plazo, cancelación, resultados estructurados y código en el historial privado. Tanto el UTF-8 como su cadena JSON tienen un límite de 8 KiB.
- `Dependencia` Actualización de los tres artefactos API del host release a AutoJs6 52ce694f92 / build 5297 para imágenes negociadas, manteniendo el contrato de conexión build 5289+

##### Para más historial de versiones

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/app/src/main/assets/doc/CHANGELOG-es.md)

******

### Compilación y verificación

******

Esta sección está dirigida a desarrolladores que quieran compilar el plugin desde el código fuente; los usuarios normales pueden instalar simplemente el APK precompilado de la página Releases.

Compilar un APK de depuración:

```powershell
.\gradlew.bat :app:assembleDebug
```

Ejecutar las pruebas unitarias JVM y compilar el APK de pruebas de instrumentación:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Compilar el APK de release:

```powershell
.\gradlew.bat :app:assembleRelease
```

Recopilar el artefacto de release y añadir la versión y el resumen CRC32 a su nombre de archivo:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Verificar que las fuentes de documentación multilingüe y los artefactos generados están sincronizados (también lo exige la CI):

```powershell
py .python\generate_markdown.py --check
```

La compilación requiere JDK 21 o posterior y Android SDK 37; las versiones de Gradle y de los plugins se gestionan de forma centralizada mediante `version.properties` e `io.github.supermonster003.autojs6-platform-versions`.

******

### Localización y generación de documentación

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

Los archivos JSON de idioma en `.readme/` y `.changelog/` son la única fuente del README, las instrucciones del centro de plugins y el registro de cambios. Edite siempre esas fuentes JSON y vuelva a ejecutar `py .python/generate_markdown.py`; los artefactos generados de README, `plugin_instruction.md` y registro de cambios nunca se editan a mano. Ejecute `py .python/generate_markdown.py --check` para verificar todos los artefactos generados.

******

### Licencia

******

El código del proyecto se distribuye bajo la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/LICENSE). Los componentes de terceros y sus licencias se listan en los [Avisos de terceros](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/THIRD_PARTY_NOTICES.md).

******

### Enlaces

******

- Proyecto AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Documentación de AutoJs6: https://docs.autojs6.com
- Discusión #577 de AutoJs6: https://github.com/SuperMonster003/AutoJs6/discussions/577
- Avisos de terceros: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/THIRD_PARTY_NOTICES.md
