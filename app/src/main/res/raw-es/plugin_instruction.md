AI Agent convierte un objetivo en lenguaje natural en acciones sobre un dispositivo Android que ejecuta AutoJs6. O bien elige un script que el usuario ha registrado para el agente, completa sus parámetros y lo ejecuta; o bien observa la pantalla a través del árbol de nodos de accesibilidad y actúa paso a paso (observar, decidir, actuar, verificar) hasta alcanzar el objetivo, necesitar una confirmación o agotar un presupuesto. Responde a la [discusión #577 de AutoJs6](https://github.com/SuperMonster003/AutoJs6/discussions/577).

La versión 1.0.0 es la vista previa de desarrollo P0 de la hoja de ruta: la identidad del plugin, el contrato de descubrimiento de AutoJs6 (servicio INFO, Wake Activity y el servicio provisional `org.autojs.plugin.AI_AGENT`) y una pantalla de inicio que informa del estado del anfitrión. El bucle del agente, el catálogo de scripts, la API `ai.agent` y el espacio de tareas aún no están implementados; el progreso y las evidencias se registran en [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md). El plugin requerirá AutoJs6 build 5283 o posterior.

### Uso

1. Instale el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) en un dispositivo con AutoJs6 build 5283 o posterior.
2. Abra el centro de plugins de AutoJs6, confirme que `AI Agent` se reconoce y habilítelo. Los paquetes oficiales superan automáticamente la verificación de firma.
3. Abra AI Agent desde el lanzador: en esta vista previa la pantalla solo indica si hay instalado un anfitrión AutoJs6 compatible. El espacio de tareas, la entrada del cajón y la API `ai.agent` llegan con las fases posteriores de la hoja de ruta.

Consulte el [README del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) y [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) para la guía de conexión y el progreso actual.
