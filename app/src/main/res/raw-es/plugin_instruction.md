AI Agent convierte un objetivo en lenguaje natural en acciones sobre un dispositivo Android que ejecuta AutoJs6. O bien elige un script que el usuario ha registrado para el agente, completa sus parámetros y lo ejecuta; o bien observa la pantalla a través del árbol de nodos de accesibilidad y actúa paso a paso (observar, decidir, actuar, verificar) hasta alcanzar el objetivo, necesitar una confirmación o agotar un presupuesto. Responde a la [discusión #577 de AutoJs6](https://github.com/SuperMonster003/AutoJs6/discussions/577).

El entorno del plugin en la versión 1.0.0 sigue siendo una vista previa P0: INFO, Wake Activity, el servicio provisional `org.autojs.plugin.AI_AGENT` y una pantalla con el estado del anfitrión. El anfitrión implementa los contratos P1, intermediarios, observación de pantalla, ejecución de scripts registrados y accesos del panel lateral y centro de plugins. El bucle del agente, la selección de scripts, la API `ai.agent` y el espacio de tareas siguen pendientes. Se requiere AutoJs6 build 5285; consulte [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) para ver el progreso y las evidencias.

### Uso

1. Instale el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) en un dispositivo con AutoJs6 build 5285 o posterior.
2. Abra el centro de plugins de AutoJs6, confirme que `AI Agent` se reconoce y habilítelo. Los paquetes oficiales superan automáticamente la verificación de firma.
3. Abra AI Agent desde el lanzador o la opción de gestión del panel lateral de AutoJs6. Esta vista previa solo muestra el estado del anfitrión; el espacio de tareas y la API `ai.agent` llegarán en fases posteriores.

Consulte el [README del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) y [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) para la guía de conexión y el progreso actual.
