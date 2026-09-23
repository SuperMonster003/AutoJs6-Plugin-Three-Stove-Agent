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

Vista previa: los scripts registrados admiten preguntas de parámetros, confirmación, resultados y cancelación. Los flujos de pantalla siguen en P4 y las API de tareas y el panel en P5/P6. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

******

### Funciones

******

La versión 1.0.0 está prevista para ofrecer las siguientes capacidades:

- Selección de scripts: los scripts registrados mediante `project.json` o un comentario de cabecera `@agent` se presentan al modelo con sus descripciones y esquemas de parámetros; el agente elige uno, completa los parámetros, pide confirmación cuando hace falta, lo ejecuta dentro de AutoJs6 y lee su resultado estructurado.
- Manejo de la pantalla paso a paso: el agente observa el árbol de nodos de accesibilidad en forma de texto compacto (y el texto de la pantalla mediante un plugin OCR cuando está instalado), y luego pulsa, escribe, desplaza y presiona teclas a través del intermediario de capacidades de AutoJs6 hasta poder verificar el objetivo.
- Seguridad por diseño: las herramientas de solo lectura se ejecutan automáticamente, las acciones sensibles (pago, envío, borrado, escritura de archivos, shell, gestos por coordenadas, scripts registrados como sensibles) requieren confirmación, y cada ejecución tiene presupuestos de pasos, llamadas al modelo, duración y tokens.
- API de script e interfaz de usuario: `ai.agent.run(goal, options)` devuelve un manejador `AgentRun` con eventos, respuestas y cancelación; la aplicación independiente ofrece un espacio de tareas con historial, preajustes, memoria de preferencias, ajustes e historial de versiones.

### Catálogo de herramientas

Vista previa: los scripts registrados admiten preguntas de parámetros, confirmación, resultados y cancelación. Los flujos de pantalla siguen en P4 y las API de tareas y el panel en P5/P6.

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

******

### Uso

******

1. Instale el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) en un dispositivo con AutoJs6 build 5288 o posterior.
2. Abra el centro de plugins de AutoJs6, confirme que `AI Agent` se reconoce y habilítelo. Los paquetes oficiales superan automáticamente la verificación de firma.
3. Solicitud de conexión desde el lanzador con espera de 15 segundos y guía para activar y autorizar AI Agent en AutoJs6.
4. Configure carpetas adicionales en "Directorios de scripts" del lanzador, una ruta absoluta por línea. El anfitrión valida y aplica las rutas guardadas; las tareas solo pueden reducir las carpetas aprobadas.

> Vista previa: los scripts registrados admiten preguntas de parámetros, confirmación, resultados y cancelación. Los flujos de pantalla siguen en P4 y las API de tareas y el panel en P5/P6.

******

### Permisos y seguridad

******

El plugin sigue límites explícitos:

- Los puntos de entrada Binder están protegidos por el permiso de firma `org.autojs.permission.PLUGIN`, por lo que solo AutoJs6 puede alcanzarlos; la pantalla de inicio es el único otro componente exportado.
- El plugin no guarda claves de API, nunca se vincula a un proveedor de modelo ni solicita el permiso de accesibilidad: las llamadas al modelo y las acciones en el dispositivo pasan por intermediarios que AutoJs6 presta para un enlace adjunto y revoca al desvincularse, cada uno acotado por una concesión (métodos permitidos, tasas, tamaños, cuota de modelo).
- Sin permiso de red. FOREGROUND_SERVICE y FOREGROUND_SERVICE_SPECIAL_USE mantienen las tareas activas; POST_NOTIFICATIONS muestra progreso y controles de parada. No se solicitan permisos de accesibilidad ni superposición.
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
minimum host build: 5288 (6.8.0)
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

_2026/09/23_

- `Aviso` Vista previa: los scripts registrados admiten preguntas de parámetros, confirmación, resultados y cancelación. Los flujos de pantalla siguen en P4 y las API de tareas y el panel en P5/P6.
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
- `Corrección` Ocultación de parámetros multilínea antes de dividir la consola, sin omitir credenciales cuando un parámetro coincide con su etiqueta
- `Corrección` Un servicio en primer plano que se está cerrando ya no rechaza el inicio de la siguiente tarea
- `Mejora` Los límites de las descripciones de confirmación incluyen el escape JSON para mantener tablas grandes dentro del límite de eventos Binder
- `Mejora` El anfitrión mínimo es AutoJs6 6.8.0 / compilación 5288 para detectar la disponibilidad de OCR y verificar sus permisos
- `Dependencia` Añadidos common-plugin-api, host-capability-api y ai-agent-api de una misma compilación release de AutoJs6 6.8.0 / 5288 (MPL 2.0), fijados con SHA-256
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
