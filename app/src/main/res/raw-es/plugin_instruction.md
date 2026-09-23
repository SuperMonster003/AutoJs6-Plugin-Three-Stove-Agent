AI Agent convierte un objetivo en lenguaje natural en acciones sobre un dispositivo Android que ejecuta AutoJs6. O bien elige un script que el usuario ha registrado para el agente, completa sus parámetros y lo ejecuta; o bien observa la pantalla a través del árbol de nodos de accesibilidad y actúa paso a paso (observar, decidir, actuar, verificar) hasta alcanzar el objetivo, necesitar una confirmación o agotar un presupuesto. Responde a la [discusión #577 de AutoJs6](https://github.com/SuperMonster003/AutoJs6/discussions/577).

La vista previa instalada muestra el estado del anfitrión. Las interfaces P1 y los núcleos de herramientas, decisiones y ejecución P2.1-P2.3 están implementados y probados. La ejecución real aún requiere la integración de modelo/anfitrión P2.4/P2.5 y los adaptadores P3/P4. La API de scripts y el panel de tareas llegarán en P5/P6. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

### Uso

1. Instale el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) en un dispositivo con AutoJs6 build 5285 o posterior.
2. Abra el centro de plugins de AutoJs6, confirme que `AI Agent` se reconoce y habilítelo. Los paquetes oficiales superan automáticamente la verificación de firma.
3. Abra AI Agent desde el lanzador o la opción de gestión del panel lateral de AutoJs6. Esta vista previa solo muestra el estado del anfitrión; el espacio de tareas y la API `ai.agent` llegarán en fases posteriores.

Consulte el [README del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) y [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) para la guía de conexión y el progreso actual.
