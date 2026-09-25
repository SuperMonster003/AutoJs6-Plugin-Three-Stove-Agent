<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-ai-agent-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Ejecuta tareas en lenguaje natural en AutoJs6 eligiendo scripts registrados y manejando la pantalla paso a paso</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-AI-Agent?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-AI-Agent?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-AI-Agent?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Idiomas

******

El README.md actual admite los siguientes idiomas:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-fr.md)
- Español [es] # actual
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ar.md)

******

### Introducción

******

AI Agent convierte un objetivo en lenguaje natural en acciones sobre un dispositivo Android que ejecuta AutoJs6. O bien elige un script que el usuario ha registrado para el agente, completa sus parámetros y lo ejecuta; o bien observa la pantalla a través del árbol de nodos de accesibilidad y actúa paso a paso (observar, decidir, actuar, verificar) hasta alcanzar el objetivo, necesitar una confirmación o agotar un presupuesto. Responde a la [discusión #577 de AutoJs6](https://github.com/SuperMonster003/AutoJs6/discussions/577).

El plugin es a la vez un plugin de AutoJs6 y una aplicación independiente. Los scripts lo usan mediante la API `ai.agent` de AutoJs6; los usuarios lo usan desde su propio espacio de tareas, el cajón de AutoJs6, una burbuja flotante, el menú de compartir del sistema, los accesos directos de la aplicación y la entrada por voz. Las llamadas al modelo y las acciones en el dispositivo siempre pasan por AutoJs6 mediante Binder: el anfitrión presta al plugin un intermediario de modelo (los plugins AI Provider que el anfitrión ya conoce, como 3-Stone AI) y un intermediario de capacidades con una concesión acotada. El plugin nunca guarda credenciales, nunca se vincula por sí mismo a un proveedor de modelo y nunca solicita el permiso de accesibilidad.

******

### Estado

******

Vista previa de desarrollo de 1.0.0. Las API de tareas y las entradas de la interfaz están implementadas; las pruebas de auditoría P7 están documentadas. Las comprobaciones y la publicación de P8 siguen pendientes. Consulte [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) para los casos aprobados y las limitaciones.

******

### Funciones

******

La implementación actual ofrece estas funciones:

- Selección de scripts: los scripts registrados mediante `project.json` o un comentario de cabecera `@agent` se presentan al modelo con sus descripciones y esquemas de parámetros; el agente elige uno, completa los parámetros, pide confirmación cuando hace falta, lo ejecuta dentro de AutoJs6 y lee su resultado estructurado.
- Manejo de la pantalla paso a paso: el agente observa el árbol de nodos de accesibilidad en forma de texto compacto (y el texto de la pantalla mediante un plugin OCR cuando está instalado), y luego pulsa, escribe, desplaza y presiona teclas a través del intermediario de capacidades de AutoJs6 hasta poder verificar el objetivo.
- Seguridad por diseño: las herramientas de solo lectura se ejecutan automáticamente, las acciones sensibles (pago, envío, borrado, escritura de archivos, shell, gestos por coordenadas, scripts registrados como sensibles) requieren confirmación, y cada ejecución tiene presupuestos de pasos, llamadas al modelo, duración y tokens.
- API de script e interfaz de usuario: `ai.agent.run(goal, options)` devuelve un manejador `AgentRun` con eventos, respuestas y cancelación; la aplicación independiente ofrece un espacio de tareas con historial, preajustes, memoria de preferencias, ajustes e historial de versiones.

### Capturas de pantalla

Interfaz inglesa real en Android API 37.1 con tareas de ejemplo y un modelo de respuestas programadas. Las imágenes muestran la interfaz y no demuestran éxito con un modelo real. No contienen datos privados de cuentas. [Procedimiento de captura](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/README.md).

| Panel de tareas | Detalles de la tarea |
| --- | --- |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/workbench.png?raw=true" alt="Panel de tareas" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/detail.png?raw=true" alt="Detalles de la tarea" width="288" /> |
| Confirmación de acciones | Entrada flotante de tareas |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/confirmation.png?raw=true" alt="Confirmación de acciones" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/floating.png?raw=true" alt="Entrada flotante de tareas" width="288" /> |

******

### Instalación

******

1. Instale el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) en un dispositivo con AutoJs6 build 5293 o posterior.
2. Abra el centro de plugins de AutoJs6, confirme que `AI Agent` se reconoce y habilítelo. Los paquetes oficiales superan automáticamente la verificación de firma.

Instale y habilite [3-Stone AI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stone-AI); configure allí un modelo en línea o importe uno local compatible. El intermediario actual del anfitrión selecciona 3-Stone AI; otro Provider necesita integración en el anfitrión. Elija el modelo en AI Agent > Preajustes. Connected to AutoJs6 indica la conexión al anfitrión; el selector de modelo está en los preajustes.

### Compatibilidad

Android 7.0+ (API 24). La conexión requiere AutoJs6 6.8.0 / build 5289+; la API completa y esta guía requieren build 5293+. Use una compilación que incluya los cambios de Agent. Active la accesibilidad del anfitrión para operar la pantalla. OCR es opcional y requiere un complemento instalado, autorizado y disponible según el anfitrión. AI Agent no guarda credenciales de modelos ni tiene servicio de accesibilidad propio.

### Inicio desde la interfaz

Abre AI Agent, conecta AutoJs6, introduce un objetivo e inicia con el preajuste predeterminado. Responde o confirma en la tarjeta y consulta los detalles de las tareas recientes.

### Inicio desde un script

Ejecute este JavaScript en AutoJs6 tras conectar AI Agent y configurar un modelo. La interfaz del complemento recibe preguntas y confirmaciones. Para usar una configuración guardada, añada `preset: "your-preset-name"` a las opciones.

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
if (!context) throw Error('Start this registered script through AI Agent');
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

Se admiten string, number, integer y boolean; no objetos anidados ni matrices. Los scripts sensitive siempre requieren confirmación previa. Registre solo scripts revisados: declarar el riesgo no aísla JavaScript. [Formato completo](https://github.com/SuperMonster003/AutoJs6/blob/master/docs/dev/agent-script-manifest-v1.md).

### Catálogo de herramientas

La tabla se genera desde el ToolCatalog incluido. El objetivo real de pantalla puede elevar el riesgo; el modo cauteloso también confirma acciones que no sean de solo lectura. Ajustes, preajustes, opciones y permisos del anfitrión limitan los grupos disponibles.

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

### Preajustes y memoria

Abra Preajustes en el panel para guardar una configuración. Los nombres identifican scripts y ámbitos de memoria; copie el preajuste para usar otro nombre. El default integrado se puede editar pero no eliminar. Elija un modelo del catálogo del host o la selección automática. Un modelo elegido no disponible falla sin sustituirse. Las opciones de tarea solo pueden reducir los límites del preajuste. El contexto fijo y el de la tarea comparten un límite de 8 KiB. La memoria puede incluir entradas globales y del preajuste, solo uno de los dos ámbitos, o ninguno. Editar o eliminar no cambia las tareas en cola. Almacenamiento privado: hasta 32 preajustes / 1 MiB.

Abra Memoria para consultar, editar, eliminar o respaldar preferencias. Hasta 500 entradas / 256 KiB, con ámbito, tarea de origen y fechas. Confirme cada memory_propose y cada entrada importada. Cree primero los preajustes que falten. La inyección automática conserva entradas completas recientes del ámbito permitido, hasta 4 KiB; el preajuste actual prevalece sobre claves globales iguales. memory: false solo desactiva la inyección. Desactive el grupo memory o el ámbito para impedir también consultas y propuestas. La exportación incluye valores reales y procedencia. No almacene credenciales; se rechazan claves y formatos de token reconocibles.

### Uso

- Configure carpetas adicionales en "Directorios de scripts" del lanzador, una ruta absoluta por línea. El anfitrión valida y aplica las rutas guardadas; las tareas solo pueden reducir las carpetas aprobadas.
- Hasta 200 tareas / 32 MiB. Se eliminan primero las tareas terminadas consultadas hace más tiempo. Repetir rellena el objetivo y preajuste originales en el panel. Revísalos y pulsa Iniciar tarea para ejecutarla. Vaciar el historial conserva las tareas en curso. Se conservan contadores, nombres de herramientas y confirmaciones. Se eliminan objetivos, parámetros, observaciones y resultados de scripts. Elige dónde guardar el archivo.
- Responda en las tareas en primer plano o abra la notificación prioritaria en segundo plano. La confirmación muestra herramienta, parámetros, riesgo y tiempo restante. Permitir acciones similares se limita a esta herramienta y riesgo en esta tarea; los pagos y la memoria siempre requieren aprobación individual. Recordar una respuesta crea una propuesta memory_propose separada en el ámbito permitido. Las confirmaciones esperan normalmente 120 segundos y las preguntas hasta 10 minutos, dentro del presupuesto de la tarea. Al expirar se devuelve USER_TIMEOUT; el modelo decide si pregunta de nuevo o informa un resultado parcial. Las solicitudes antiguas no responden a las nuevas. Los permisos y canales afectan a las notificaciones.
- Abra Ajustes desde tareas para elegir grupos, presupuestos, modo prudente, voz y perfil predeterminado. Los cambios afectan a tareas nuevas. gesture/files/shell empiezan desactivados; OCR requiere un complemento autorizado y disponible en el anfitrión. Los presupuestos vacíos heredan los valores iniciales y respetan los límites del protocolo. Perfiles y opciones solo pueden reducirlos. La gestión muestra cantidades y bytes; borrar una categoría exige confirmación y ninguna tarea activa. Borrar perfiles restaura default. También hay carpetas de scripts, licencias y fuente.
- Historial y avisos legales se incluyen sin conexión. La consulta a GitHub Releases es manual, con caché de éxitos de 24 horas, cancelación y versiones ignoradas. El diálogo abre el historial interno o la página de publicación en el navegador. Sin consultas automáticas ni descargas APK.
- Activa la burbuja en Ajustes, permite la superposición y guarda. Está desactivada por defecto y solo aparece con AutoJs6 conectado; se oculta al bloquear o desconectar, sin servicio en primer plano en reposo. Arrastra para moverla y pulsa para introducir un objetivo, elegir un preajuste, responder o detener. Contraer la tarjeta restaura las notificaciones de confirmación. Comparte texto sin formato, usa Nueva tarea o fija un preajuste con objetivo opcional. Cada entrada abre un borrador editable y requiere iniciar explícitamente. Los preajustes eliminados no se sustituyen en silencio. La voz usa el idioma de la interfaz, se oculta si no está disponible y rellena sin enviar.

### Preguntas frecuentes

**Por qué se necesita AutoJs6?**

El complemento gestiona el ciclo y la interfaz. AutoJs6 gestiona modelos, accesibilidad y ejecución de scripts registrados. Sin un anfitrión compatible conectado puede leer el historial pero no iniciar tareas de dispositivo. Su desconexión bloquea las tareas activas; reconectarlo no las repite automáticamente.

**Por qué hay que confirmar cada pago?**

Pagar es una acción sensible independiente. Aprobar un pedido, script o acciones similares no autoriza un pago. Cada acción de pago detectada exige confirmación propia; el tiempo agotado implica rechazo. Revise comercio, productos, dirección e importe antes de aprobar.

**Qué limitaciones tienen los modelos locales?**

Las tareas dependen del seguimiento de instrucciones, decisiones JSON válidas y contexto disponible. Un modelo pequeño puede cargar y aun así fallar; el caso Wi-Fi registrado de Gemma 4 E2B IT no superó la validación de decisiones. Empiece con tareas pequeñas y revise partial/failed. 1.0.0 usa nodos de texto/OCR y decisiones JSON; visión, llamadas nativas a herramientas y scripts generados siguen en el plan 1.1.0.

******

### Permisos y seguridad

******

El plugin sigue límites explícitos:

- Las entradas Binder requieren el permiso de firma org.autojs.permission.PLUGIN. El lanzador (incluidos accesos directos) y el destino text/plain ACTION_SEND son públicos y solo reciben borradores limitados. Los Intent externos no pueden ejecutar tareas, confirmar ni cambiar permisos. Ajustes, resultados de voz y controles son privados.
- El plugin no guarda claves de API, nunca se vincula a un proveedor de modelo ni solicita el permiso de accesibilidad: las llamadas al modelo y las acciones en el dispositivo pasan por intermediarios que AutoJs6 presta para un enlace adjunto y revoca al desvincularse, cada uno acotado por una concesión (métodos permitidos, tasas, tamaños, cuota de modelo).
- INTERNET solo sirve para comprobaciones manuales en GitHub. FOREGROUND_SERVICE y FOREGROUND_SERVICE_SPECIAL_USE mantienen tareas activas; POST_NOTIFICATIONS muestra progreso y confirmaciones. SYSTEM_ALERT_WINDOW se solicita solo al activar la burbuja en Ajustes. No se solicitan permisos de accesibilidad, almacenamiento ni micrófono.
- El historial de tareas, los preajustes y la memoria de preferencias permanecen en el almacenamiento privado del plugin; las copias de seguridad y las transferencias entre dispositivos están desactivadas.

Obtenga el plugin únicamente desde la página oficial de [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) o el centro de plugins de AutoJs6. Los paquetes de origen desconocido pueden fallar la verificación del anfitrión o conllevar riesgos aunque el número de versión parezca idéntico.

******

### Interfaz del plugin

******

La siguiente información está dirigida a desarrolladores del anfitrión AutoJs6 y de plugins; el anfitrión usa estos identificadores para descubrir el plugin y negociar la compatibilidad:

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

`AiAgentPluginService` / `IAiAgentPlugin` / `IAiAgentLink`: Conexión con identidad del anfitrión verificada, cola de tareas, respuestas, cancelación, consultas e historial privado; las tareas se bloquean al perder el anfitrión y no se reanudan al reiniciar el proceso.

******

### Hoja de ruta

******

Los planes y el progreso del plugin se mantienen como una lista verificable en ROADMAP.md, organizada por fases con criterios de aceptación y niveles de evidencia. Los elementos sin marcar expresan intención y no capacidades actuales; la discusión mediante Issues es bienvenida.

- [Ver ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md)

******

### Historial de versiones

******

#### v1.0.0

_2026/09/25_

- `Aviso` Vista previa de desarrollo: están disponibles las pantallas P6, ajustes, burbuja flotante, compartir, accesos directos y borradores por voz. ai.agent requiere AutoJs6 build 5293 o posterior. Fiabilidad y publicación siguen pendientes en P7/P8.
- `Función` Entrada flotante opcional, progreso, parada y confirmación, compartir texto, accesos estáticos y de preajustes, y reconocimiento de voz del sistema sin envío automático
- `Función` Ajustes globales, gestión de categorías de datos, historial y avisos legales sin conexión, consulta manual cancelable con caché diario y versiones ignoradas
- `Función` Confirmaciones en tareas y notificaciones con riesgo, cuenta atrás, permiso por tarea y memoria de respuestas confirmada por separado
- `Función` Memoria de preferencias con confirmación de cada propuesta, consultas por ámbito, protección de conflictos, almacenamiento por entrada, edición, eliminación y copia JSON con aprobación individual al importar
- `Función` Preajustes con nombre: creación, edición, copia, eliminación y selección predeterminada; catálogo de modelos con ubicación y compatibilidad con JSON estructurado, contexto fijo, límites de herramientas y presupuestos, confirmación, carpetas autorizadas y ámbito de memoria
- `Función` Cronología completa y resultados, filtros por estado/preajuste/fecha, borradores para repetir tareas, eliminación, exportación JSON sin datos sensibles e historial privado versionado con migración y limpieza LRU (200 tareas / 32 MiB)
- `Función` Panel con inicio común, apariencia del anfitrión, interacciones integradas, presupuestos y hasta 20 tareas recientes consultables sin conexión
- `Función` La finalización exige pruebas, los resultados parciales enumeran el trabajo pendiente y las tareas de pedido o pago requieren un estado de pedido observado
- `Función` La verificación conserva el recuento de pantallas sin cambios al recortar el contexto y bloquea la tercera solicitud de una acción equivalente antes de ejecutarla
- `Función` Espera limitada de estabilidad tras cada acción y resumen de cambios desde la última acción en las observaciones siguientes
- `Función` Acciones vinculadas a nodos inspeccionados por el anfitrión, con adición de texto, desplazamiento limitado y resultados con cambios de ventana
- `Función` OCR de pantalla disponible solo cuando el anfitrión confirma un plugin OCR autorizado, con texto agrupado en líneas limitadas y coordenadas
- `Función` Observaciones con referencias a capturas del anfitrión, salida limitada de nodos y consola, y resúmenes de cambios de texto y estado
- `Función` Las tareas de un solo script conservan ID, ruta, ID de ejecución y resultado al concluir el modelo, con null explícito y truncamiento indicado de resultados grandes
- `Función` Ejecución de scripts registrados con comprobación del manifiesto confirmado, observaciones estructuradas, cola de consola censurada y detención del script por tiempo agotado o cancelación
- `Función` Preferencias en memoria por ámbito para los parámetros de scripts, con límite de 4 KiB, truncamiento explícito y desactivación por tarea
- `Función` Validación de parámetros de scripts registrados con valores predeterminados, preguntas por datos faltantes, revisión del riesgo actual y tablas completas para confirmar
- `Función` `ai-agent`: `AiAgentPluginInfoService`, `WakeActivity`, `AiAgentPluginService`, `ui.LauncherActivity`
- `Función` README, instrucciones del centro de plugins y registro de cambios en 10 idiomas
- `Función` Catálogo del núcleo Agent con 30 herramientas, control de grupos, esquemas de parámetros, preparación de llamadas bridge, observaciones limitadas y elevación de riesgos sensibles
- `Función` Núcleo de decisiones Agent con esquemas por protocolo, análisis JSON estricto o por extracción, validación de herramientas/ramas, hasta dos reintentos de corrección y plantillas en inglés/chino
- `Función` Presupuestos Agent de pasos, llamadas al modelo, duración y tokens, con plazos de herramientas/interacciones, estimación de uso y límites de tokens de salida
- `Función` Confirmación Agent con políticas predeterminada/cautelosa, permisos por tarea para la misma herramienta y riesgo, confirmación de cada pago y palabras clave en 10 idiomas
- `Función` Registro privado Agent limitado a 200 pasos y 1 MiB, con ocultación de contraseñas y resultados finales acotados que conservan estado y contadores
- `Función` Conexión con identidad del anfitrión verificada, cola de tareas, respuestas, cancelación, consultas e historial privado; las tareas se bloquean al perder el anfitrión y no se reanudan al reiniciar el proceso
- `Función` Contexto Agent determinista con límites de bytes, pares recientes completos, prompts en inglés/chino y prioridad de nodos; presupuesto local de 3000 tokens y firmas compactas de herramientas
- `Función` Cliente de modelo del anfitrión con validación de eventos, uso, cancelación, plazos y cambio de formato acotado; cada cambio cuenta como llamada y conserva el límite de reparación
- `Función` Solicitud de conexión desde el lanzador con espera de 15 segundos y guía para activar y autorizar AI Agent en AutoJs6
- `Función` Notificaciones en primer plano solo mientras haya tareas, con progreso, Detener y Ver; respuestas y confirmaciones por acción desde el lanzador
- `Función` Los scripts registrados se actualizan al iniciar la tarea, con caché de enlace de 60 segundos, clasificación determinista de hasta 24 candidatos, resúmenes acotados de parámetros y consultas script_catalog
- `Corrección` Áreas táctiles de formularios y filtros, ajuste de textos y columnas de parámetros, y controles flotantes con fuentes grandes y en Android 7
- `Corrección` Omisiones en la validación de credenciales de la memoria de preferencias con caracteres de ancho completo, caracteres de ancho cero y otros nombres de credenciales
- `Corrección` La burbuja de tareas podía permanecer oculta al activar un dispositivo sin bloqueo seguro mientras se estabilizaba el estado de la pantalla
- `Corrección` Las tareas interrumpidas al terminar el proceso del plugin se registran como fallidas al reiniciar; la pantalla bloqueada detiene las acciones posteriores
- `Corrección` Las herramientas de archivos rechazan rutas con recorrido, absolutas o no válidas antes de la confirmación o el envío al anfitrión; el historial guarda categorías limitadas de rechazo sin el texto rechazado del modelo
- `Corrección` La confirmación vuelve a la app de destino antes de reanudar acciones, procesa la respuesta aunque se detenga la pantalla y contrae la tarjeta flotante antes de ejecutar
- `Corrección` El inicio en Android 13 ya no falla al consultar el controlador de las barras del sistema antes de crear la vista de la ventana
- `Corrección` El historial se ordena y conserva por el inicio de las tareas para que reescribir archivos al reiniciar no elimine las más recientes
- `Corrección` Las respuestas y confirmaciones verifican el propietario interaction para impedir que un script responda por la interfaz del complemento
- `Corrección` Los botones de confirmar transacción requieren una confirmación de pago separada y no reutilizan permisos de toda la tarea
- `Corrección` Las coincidencias fuera de pantalla con límites vacíos o invertidos conservan el texto e indican coordenadas no utilizables en vez de errores de argumentos
- `Corrección` La relocalización de nodos distingue límites y capacidades de acción para no confundir contenedores anidados con el objetivo
- `Corrección` Indicaciones precisas para corregir destinos de nodos: conservar el prefijo # y omitir snapshotId con selector
- `Corrección` La admisión precarga las reglas de pedido y evita una compilación costosa de reglas
- `Corrección` La verificación distingue nodos de ventanas distintas, mantiene la observación de pantalla tras leer el portapapeles y no confunde transferencias de archivos con pagos
- `Corrección` La lectura de pantalla sin respuesta tras una acción ya no supera el plazo de estabilización
- `Corrección` Ocultación de parámetros multilínea antes de dividir la consola, sin omitir credenciales cuando un parámetro coincide con su etiqueta
- `Corrección` Un servicio en primer plano que se está cerrando ya no rechaza el inicio de la siguiente tarea
- `Mejora` El ajuste de historiales largos reutiliza fragmentos de instrucciones y observaciones sin cambios para reducir el tiempo de procesamiento por paso
- `Mejora` Los límites de las descripciones de confirmación incluyen el escape JSON para mantener tablas grandes dentro del límite de eventos Binder
- `Mejora` El anfitrión mínimo es AutoJs6 6.8.0 / compilación 5289 para inspeccionar nodos de acción y vincular la confirmación a la ejecución
- `Dependencia` Añadidos common-plugin-api, host-capability-api y ai-agent-api de una misma compilación release de AutoJs6 6.8.0 / 5289 (MPL 2.0), fijados con SHA-256
- `Dependencia` Se añadió Gson 2.13.2 para el análisis JSON estricto con límites y árboles de esquemas

##### Para más historial de versiones

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/assets/doc/CHANGELOG-es.md)

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

El código del proyecto se distribuye bajo la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/LICENSE). Los componentes de terceros y sus licencias se listan en los [Avisos de terceros](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/THIRD_PARTY_NOTICES.md).

******

### Enlaces

******

- Proyecto AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Documentación de AutoJs6: https://docs.autojs6.com
- Discusión #577 de AutoJs6: https://github.com/SuperMonster003/AutoJs6/discussions/577
- Avisos de terceros: https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/THIRD_PARTY_NOTICES.md
