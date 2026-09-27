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

AI Agent ofrece una interfaz independiente y un plugin AutoJs6 accesible mediante ai.agent. Las acciones integradas y las llamadas al modelo pasan por AutoJs6. Las herramientas MCP opcionales solo conectan servidores configurados. No se enlaza directamente al proveedor ni se solicita accesibilidad.

******

### Estado

******

La versión 1.2.0 en desarrollo ofrece herramientas MCP opcionales, llamadas nativas, capturas y scripts generados. AiGoCode gpt-5.6-sol pasó las pruebas P9.2 de imagen inicial e imagen en resultados de herramientas. P9.1 completó la activación de Wi-Fi y la lectura posterior de su estado con las rutas JSON y nativa, desactivando temporalmente la conexión automática al punto de acceso actual y accediendo al modelo mediante datos móviles y VPN. Los fallos tras el cambio de red de la VPN con la conexión automática predeterminada siguen sin resolverse; consulte [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

******

### Funciones

******

La implementación actual ofrece estas funciones:

- Selección de scripts: los scripts registrados mediante `project.json` o un comentario de cabecera `@agent` se presentan al modelo con sus descripciones y esquemas de parámetros; el agente elige uno, completa los parámetros, pide confirmación cuando hace falta, lo ejecuta dentro de AutoJs6 y lee su resultado estructurado.
- Manejo de la pantalla paso a paso: el agente observa el árbol de nodos de accesibilidad en forma de texto compacto (y el texto de la pantalla mediante un plugin OCR cuando está instalado), y luego pulsa, escribe, desplaza y presiona teclas a través del intermediario de capacidades de AutoJs6 hasta poder verificar el objetivo.
- Seguridad por diseño: las herramientas de solo lectura se ejecutan automáticamente, las acciones sensibles (pago, envío, borrado, escritura de archivos, shell, gestos por coordenadas, scripts registrados como sensibles) requieren confirmación de forma predeterminada, y cada ejecución tiene presupuestos de pasos, llamadas al modelo, duración y tokens. Una confirmación puede valer una vez o hasta que termine la tarea; Ajustes también ofrece el modo prudente y el acceso completo, que omite las confirmaciones y queda claramente señalado.
- API de script e interfaz de usuario: `ai.agent.run(goal, options)` devuelve un manejador `AgentRun` con eventos, respuestas y cancelación; la aplicación independiente ofrece un espacio de tareas con historial, preajustes, memoria de preferencias, ajustes e historial de versiones.
- Llamadas nativas mediante el host: esquemas del catálogo, validación del lote completo, ejecución secuencial, confirmaciones individuales, devolución de resultados y registro compartido
- Observación de capturas mediante AutoJs6 en Android 11+: screen_capture limita el lado mayor a 1280 y usa JPEG de calidad 70, con instrucciones visuales, presupuesto de tokens de imagen e imágenes en resultados de herramientas nativas
- JavaScript generado mediante script_run_source: el grupo script_dynamic está desactivado inicialmente. Cada llamada muestra un resumen ampliable al código completo para aprobarlo una vez o durante la tarea actual; el acceso completo omite esta revisión. La ejecución ofrece plazo, cancelación, resultados estructurados y código en el historial privado. Tanto el UTF-8 como su cadena JSON tienen un límite de 8 KiB.
- Herramientas MCP de servidores locales o externos seleccionados, con riesgo por servidor y el grupo mcp desactivado inicialmente
- Aplicación independiente rediseñada con Material 3: el inicio es un flujo de tareas con el área de escritura fija sobre el teclado, una barra superior con la cápsula de modelo, el historial y un menú (Nueva tarea, Preajustes, Memoria, Carpetas de scripts, Servidores MCP, Ajustes), un aviso solo mientras AutoJs6 no está conectado, una cronología de pasos que se actualiza por paso, y Ejecutar de nuevo o Reintentar con otro modelo que rellenan la escritura sin iniciar. Ajustes organizados en secciones claras y apariencia clara/oscura coherente

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

Instale y habilite [3-Stone AI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stone-AI); configure allí un modelo en línea o importe uno local compatible. El intermediario actual del anfitrión selecciona 3-Stone AI; otro Provider necesita integración en el anfitrión. Elija el modelo con la cápsula de modelo del inicio de AI Agent. Solo aparece un aviso allí mientras AutoJs6 no está conectado.

### Compatibilidad

Android 7.0+ (API 24). La conexión requiere AutoJs6 6.8.0 / build 5289+; la API completa y esta guía requieren build 5293+. Use una compilación que incluya los cambios de Agent. Operar la pantalla requiere la accesibilidad del anfitrión; Agent la inicia primero con el método automático configurado en AutoJs6 (Root, ajustes seguros o Shizuku) y solo le pide activarla si eso falla. OCR es opcional y requiere un complemento instalado, autorizado y disponible según el anfitrión. AI Agent no guarda credenciales de modelos ni tiene servicio de accesibilidad propio.

### Inicio desde la interfaz

Abre AI Agent, conecta AutoJs6, introduce un objetivo y comienza. La cápsula de modelo del inicio elige un modelo en línea o local, o Automático (primero un modelo en el dispositivo; si no, el primero disponible). Busca modelos, fija tus favoritos y reutiliza los recientes; las insignias muestran la compatibilidad declarada con herramientas e imágenes. El panel y la burbuja flotante comparten esta selección para tareas nuevas sin editar preajustes ni cambiar la tarea en curso; los preajustes ya no incluyen modelo. El chip de preajuste del área de escritura elige un preajuste opcional. Responde preguntas y sigue el progreso en la tarjeta de tarea.

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

### Preajustes y memoria

Abra Preajustes en el panel para guardar una configuración. Los nombres identifican scripts y ámbitos de memoria; copie el preajuste para usar otro nombre. El default integrado se puede editar pero no eliminar. Los preajustes no incluyen modelo; un modelo guardado por una versión anterior se conserva solo para scripts. Las opciones de tarea solo pueden reducir los límites del preajuste. El contexto fijo y el de la tarea comparten un límite de 8 KiB. La memoria puede incluir entradas globales y del preajuste, solo uno de los dos ámbitos, o ninguno. Editar o eliminar no cambia las tareas en cola. Almacenamiento privado: hasta 32 preajustes / 1 MiB.

Abra Memoria para consultar, editar, eliminar o respaldar preferencias. Hasta 500 entradas / 256 KiB, con ámbito, tarea de origen y fechas. Confirme cada memory_propose y cada entrada importada. Cree primero los preajustes que falten. La inyección automática conserva entradas completas recientes del ámbito permitido, hasta 4 KiB; el preajuste actual prevalece sobre claves globales iguales. memory: false solo desactiva la inyección. Desactive el grupo memory o el ámbito para impedir también consultas y propuestas. La exportación incluye valores reales y procedencia. No almacene credenciales; se rechazan claves y formatos de token reconocibles.

### Uso

- Configure carpetas adicionales en "Directorios de scripts" del lanzador, una ruta absoluta por línea. El anfitrión valida y aplica las rutas guardadas; las tareas solo pueden reducir las carpetas aprobadas.
- Hasta 200 tareas / 32 MiB. Se eliminan primero las tareas terminadas consultadas hace más tiempo. Repetir rellena el objetivo y preajuste originales en el panel. Revísalos y pulsa Iniciar tarea para ejecutarla. Vaciar el historial conserva las tareas en curso. Se conservan contadores, nombres de herramientas y confirmaciones. Se eliminan objetivos, parámetros, observaciones y resultados de scripts. Elige dónde guardar el archivo.
- Responda en las tareas en primer plano o abra la notificación prioritaria en segundo plano. La confirmación muestra herramienta, parámetros, riesgo y tiempo restante. Permitir siempre en esta sesión aprueba la misma herramienta con el mismo riesgo hasta que termine la tarea, incluidas propuestas de memoria o códigos generados posteriores; los pagos requieren su propia aprobación. Recordar una respuesta crea una propuesta memory_propose separada en el ámbito permitido. Las confirmaciones esperan normalmente 120 segundos y las preguntas hasta 10 minutos, dentro del presupuesto de la tarea. Al expirar se devuelve USER_TIMEOUT; el modelo decide si pregunta de nuevo o informa un resultado parcial. Las solicitudes antiguas no responden a las nuevas. Los permisos y canales afectan a las notificaciones.
- Abra Ajustes desde tareas para elegir grupos, presupuestos, permisos de operación (estándar, prudente o acceso completo), voz y perfil predeterminado. Cada cambio se guarda al instante y afecta a tareas nuevas. El acceso completo ejecuta las herramientas activadas, incluidos pagos, sin aprobación; mientras está activo, las tareas, la burbuja flotante y el historial muestran un aviso. gesture/files/shell/script_dynamic empiezan desactivados; OCR requiere un complemento autorizado y disponible en el anfitrión. Los límites en automático usan los valores iniciales; la duración se indica en minutos y todo respeta los límites del protocolo. Perfiles y opciones solo pueden reducirlos. La gestión muestra cantidades y bytes; borrar una categoría exige confirmación y ninguna tarea activa. Borrar perfiles restaura default. Perfiles, memoria, carpetas de scripts y servidores MCP también se abren desde Ajustes.
- Abre Ajustes desde el menú superior derecho. El idioma, modo oscuro y color pueden seguir AutoJs6 o configurarse por separado. El idioma y modo oscuro también pueden seguir Android. El historial y los avisos legales se incluyen sin conexión. Las comprobaciones manuales de GitHub guardan resultados correctos durante 24 horas. Las automáticas están desactivadas por defecto. Al activarlas se intentan durante el uso de la app, como máximo cada 12 horas, sin avisar de fallos ni versiones ignoradas y sin descargar APK. Gestionar actualizaciones ignoradas permite restaurar versiones individualmente. Acerca de muestra la versión, el desarrollador, el código fuente, la licencia y los avisos de terceros.
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

- Las entradas Binder requieren el permiso de firma org.autojs.permission.PLUGIN. El lanzador (incluidos accesos directos) y el destino text/plain ACTION_SEND son públicos y solo reciben borradores limitados. Los Intent externos no pueden ejecutar tareas, confirmar ni cambiar permisos. Ajustes, resultados de voz y controles son privados.
- AI Agent ofrece una interfaz independiente y un plugin AutoJs6 accesible mediante ai.agent. Las acciones integradas y las llamadas al modelo pasan por AutoJs6. Las herramientas MCP opcionales solo conectan servidores configurados. No se enlaza directamente al proveedor ni se solicita accesibilidad.
- Las credenciales del modelo permanecen en su proveedor; AutoJs6 transmite sus llamadas. Los tokens MCP Bearer se cifran con Android Keystore en almacenamiento privado y no se incluyen en prompts ni exportaciones del historial. INTERNET también conecta los servidores MCP configurados; Android 17+ solicita acceso a la red local desde Ajustes de MCP. El riesgo por servidor empieza en SENSITIVE. Cancelar no revierte acciones remotas; las llamadas fallidas no se repiten automáticamente.
- El historial de tareas, los preajustes y la memoria de preferencias permanecen en el almacenamiento privado del plugin; las copias de seguridad y las transferencias entre dispositivos están desactivadas.
- Las capturas se envían mediante AutoJs6 al modelo elegido, que puede estar en línea. La pantalla debe estar activa y desbloqueada. El historial guarda dimensiones y bytes, sin el contenido de las imágenes. Las decisiones JSON conservan la imagen actual hasta otra observación o respuesta. Las conversaciones nativas conservan imágenes previas dentro de los límites del lote y de la sesión, reservando sus tokens en cada ronda.
- Los scripts generados usan permisos de AutoJs6 sin aislamiento JavaScript y pueden actuar fuera de los grupos habilitados. El código completo permanece en pasos privados, sujeto a eliminación de contraseñas y retención del historial. Un código modificado por esa eliminación posterior no puede guardarse como original. Revise los .js antes de compartirlos.
- El acceso completo solo se activa en los ajustes privados del complemento; ni la salida del modelo, ni el contenido de pantalla, ni solicitudes de scripts o Intents externos pueden activarlo o ampliarlo. Omite las confirmaciones de las herramientas activadas, incluidos pagos, pero no activa grupos adicionales ni relaja presupuestos o permisos del anfitrión. El anfitrión inicia la accesibilidad con el método configurado en AutoJs6; el complemento sigue sin solicitar permiso de accesibilidad.

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

#### v1.2.0

_2026/09/27_

- `Aviso` La versión 1.2.0 en desarrollo ofrece herramientas MCP opcionales, llamadas nativas, capturas y scripts generados. AiGoCode gpt-5.6-sol pasó las pruebas P9.2 de imagen inicial e imagen en resultados de herramientas. P9.1 completó la activación de Wi-Fi y la lectura posterior de su estado con las rutas JSON y nativa, desactivando temporalmente la conexión automática al punto de acceso actual y accediendo al modelo mediante datos móviles y VPN. Los fallos tras el cambio de red de la VPN con la conexión automática predeterminada siguen sin resolverse; consulte ROADMAP.md.
- `Función` Herramientas MCP de servidores locales o externos seleccionados, con riesgo por servidor y el grupo mcp desactivado inicialmente
- `Función` Abre AI Agent, conecta AutoJs6, introduce un objetivo y comienza. La cápsula de modelo del inicio elige un modelo en línea o local, o Automático (primero un modelo en el dispositivo; si no, el primero disponible). Busca modelos, fija tus favoritos y reutiliza los recientes; las insignias muestran la compatibilidad declarada con herramientas e imágenes. El panel y la burbuja flotante comparten esta selección para tareas nuevas sin editar preajustes ni cambiar la tarea en curso; los preajustes ya no incluyen modelo. El chip de preajuste del área de escritura elige un preajuste opcional. Responde preguntas y sigue el progreso en la tarjeta de tarea.
- `Función` Abre Ajustes desde el menú superior derecho. Cada cambio se aplica al instante, sin botón Guardar: apariencia, permisos de operación, grupos de herramientas, límites (duración en minutos), entrada de voz, burbuja flotante y limpieza de datos. El idioma, modo oscuro y color pueden seguir AutoJs6 o configurarse por separado. El idioma y modo oscuro también pueden seguir Android. El historial y los avisos legales se incluyen sin conexión. Las comprobaciones manuales de GitHub guardan resultados correctos durante 24 horas. Las automáticas están desactivadas por defecto. Al activarlas se intentan durante el uso de la app, como máximo cada 12 horas, sin avisar de fallos ni versiones ignoradas y sin descargar APK. Gestionar actualizaciones ignoradas permite restaurar versiones individualmente. Acerca de muestra la versión, el desarrollador, el código fuente, la licencia y los avisos de terceros.
- `Función` Las tareas de pantalla inician primero la accesibilidad con el método automático configurado en AutoJs6 (Root, ajustes seguros o Shizuku). Solo si falla o no hay ninguno configurado, la tarjeta de la tarea pide activarla y ofrece un acceso a los ajustes de accesibilidad.
- `Función` Los permisos de operación de Ajustes incluyen Acceso completo: las herramientas activadas, incluidos pagos, borrados, scripts y escritura de memoria, se ejecutan sin aprobación. No activa grupos de herramientas adicionales ni relaja presupuestos o permisos del anfitrión. El panel, la burbuja flotante, la tarea actual y el detalle del historial muestran una etiqueta visible en lugar de un diálogo. Las tareas que piden explícitamente confirmación prudente la mantienen.
- `Función` Las tarjetas de confirmación añaden Permitir siempre en esta sesión: hasta que termine la tarea, la misma herramienta con el mismo nivel de riesgo se ejecuta sin volver a preguntar, aunque cambien los argumentos. Los pagos requieren su propia aprobación; los scripts generados y las propuestas de memoria también pueden permitirse para la sesión.
- `Corrección` Un servicio de accesibilidad de AutoJs6 detenido se comunica al modelo como A11Y_SERVICE_NOT_RUNNING en lugar de un error de argumentos
- `Mejora` Las credenciales del modelo permanecen en su proveedor; AutoJs6 transmite sus llamadas. Los tokens MCP Bearer se cifran con Android Keystore en almacenamiento privado y no se incluyen en prompts ni exportaciones del historial. INTERNET también conecta los servidores MCP configurados; Android 17+ solicita acceso a la red local desde Ajustes de MCP. El riesgo por servidor empieza en SENSITIVE. Cancelar no revierte acciones remotas; las llamadas fallidas no se repiten automáticamente.
- `Mejora` Aplicación independiente rediseñada con Material 3: el inicio es un flujo de tareas con el área de escritura fija sobre el teclado, una barra superior con la cápsula de modelo, el historial y un menú (Nueva tarea, Preajustes, Memoria, Carpetas de scripts, Servidores MCP, Ajustes), un aviso solo mientras AutoJs6 no está conectado, una cronología de pasos que se actualiza por paso, y Ejecutar de nuevo o Reintentar con otro modelo que rellenan la escritura sin iniciar. Ajustes organizados en secciones claras y apariencia clara/oscura coherente
- `Mejora` Las confirmaciones muestran el nivel de riesgo, el grupo de herramientas y cada parámetro en una tabla legible en lugar de JSON sin procesar, con acciones claras: permitir una vez, permitir siempre en esta sesión y denegar. La burbuja flotante usa el mismo diseño Material, elige el preajuste dentro de la tarjeta y su fila de modelo abre el selector de modelo compartido
- `Dependencia` Adición de AndroidX AppCompat 1.7.1 y Material Components for Android 1.13.0 con sus dependencias AndroidX de ejecución para la interfaz Material 3

#### v1.1.0

_2026/09/26_

- `Aviso` Las llamadas nativas requieren AutoJs6 build 5297+ y un destino tools, como un destino en línea de la versión de desarrollo 3-Stone AI 1.2.0. Los hosts antiguos y destinos no compatibles mantienen JSON. Cada conversación conserva su plazo inicial, límites de contexto/salida y hasta 16 rondas de herramientas; un error tras una acción no reinicia por JSON
- `Aviso` La entrada de imágenes requiere un host compatible, el grupo observe y un modelo visual con esta entrada activada explícitamente. Implementación y pruebas deterministas completas; la validación visual real en línea sigue pendiente. Sistemas anteriores y modelos de texto mantienen observaciones textuales. Consulte ROADMAP.md
- `Aviso` Los scripts generados usan permisos de AutoJs6 sin aislamiento JavaScript y pueden actuar fuera de los grupos habilitados. El código completo permanece en pasos privados, sujeto a eliminación de contraseñas y retención del historial. Un código modificado por esa eliminación posterior no puede guardarse como original. Revise los .js antes de compartirlos.
- `Función` Llamadas nativas mediante el host: esquemas del catálogo, validación del lote completo, ejecución secuencial, confirmaciones individuales, devolución de resultados y registro compartido
- `Función` Observación de capturas mediante AutoJs6 en Android 11+: screen_capture limita el lado mayor a 1280 y usa JPEG de calidad 70, con instrucciones visuales, presupuesto de tokens de imagen e imágenes en resultados de herramientas nativas
- `Función` JavaScript generado mediante script_run_source: el grupo script_dynamic está desactivado inicialmente. Cada llamada exige revisar un resumen ampliable al código completo y dar aprobación individual. La ejecución ofrece plazo, cancelación, resultados estructurados y código en el historial privado. Tanto el UTF-8 como su cadena JSON tienen un límite de 8 KiB.
- `Dependencia` Actualización de los tres artefactos API del host release a AutoJs6 52ce694f92 / build 5297 para imágenes negociadas, manteniendo el contrato de conexión build 5289+

#### v1.0.0

_2026/09/25_

- `Aviso` La versión 1.0.0 ofrece tareas en lenguaje natural, ejecución de scripts registrados y acciones del dispositivo con confirmación según el riesgo. Consulte [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) para los casos verificados, las limitaciones de los modelos y las comprobaciones de dispositivos pendientes. Las llamadas nativas a herramientas, la entrada visual y la generación dinámica de scripts están previstas para 1.1.0.
- `Aviso` Requiere Android 7+, AutoJs6 6.8.0 / build 5293+ para las API de tareas y 3-Stone AI habilitado con un modelo configurado. OCR es opcional. El protocolo de conexión por sí solo requiere build 5289+.
- `Aviso` Compatibilidad: la extensión nativa de herramientas de AutoJs6 build 5297 es compatible con esta versión. La versión candidata de desarrollo 3-Stone AI 1.2.0 implementa la continuación de herramientas en línea para tres protocolos. Este Agent sigue usando decisiones JSON estructuradas; la integración del bucle nativo y las comparaciones siguen en [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).
- `Función` Panel de tareas en lenguaje natural con preguntas, progreso, parada y resultados; entrada flotante opcional, texto compartido, accesos a preajustes y borradores de voz
- `Función` API ai.agent para crear tareas, eventos, consultas, respuestas y cancelación, incluidas tareas detached y resultados/contexto de scripts registrados
- `Función` Scripts project.json / @agent con búsqueda, validación y valores predeterminados de parámetros, preguntas por valores ausentes, confirmación, ejecución limitada y resultados estructurados
- `Función` Observación mediante nodos de texto y OCR autorizado opcional, clics por referencia, entrada, desplazamiento y teclas, con verificación de cambios y evidencia de finalización
- `Función` Modelos en línea y locales mediante AutoJs6 sin guardar credenciales; un objetivo seleccionado ausente falla sin cambiar de modelo silenciosamente
- `Función` Presupuestos de pasos, llamadas, duración y tokens, plazos de herramientas, hasta dos reintentos de reparación por paso y protección ante acciones repetidas sin efecto
- `Función` Preajustes con nombre y ajustes globales de modelo, contexto, herramientas, presupuestos, cautela, carpetas y memoria; gesture/files/shell desactivados inicialmente
- `Función` Memoria de preferencias por ámbito con aprobación individual de propuestas/importaciones, edición, borrado y copia JSON, hasta 500 entradas / 256 KiB; inyección automática hasta 4 KiB
- `Función` Detalles y cronologías, filtros, borradores de repetición y exportación JSON depurada, con historial privado de hasta 200 tareas / 32 MiB
- `Función` Confirmación según riesgo en panel, notificaciones y tarjeta flotante; pagos y memoria siempre con aprobación individual; perder el anfitrión bloquea tareas y reiniciar no las reanuda
- `Función` Ajustes, historial sin conexión y avisos legales en diez idiomas; consulta manual de GitHub con cancelación, caché diaria y versiones ignoradas, sin descarga automática de APK
- `Corrección` Finalización prematura de tareas al interpretar el presupuesto restante como consumido
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
