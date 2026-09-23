AI Agent convierte un objetivo en lenguaje natural en acciones sobre un dispositivo Android que ejecuta AutoJs6. O bien elige un script que el usuario ha registrado para el agente, completa sus parámetros y lo ejecuta; o bien observa la pantalla a través del árbol de nodos de accesibilidad y actúa paso a paso (observar, decidir, actuar, verificar) hasta alcanzar el objetivo, necesitar una confirmación o agotar un presupuesto. Responde a la [discusión #577 de AutoJs6](https://github.com/SuperMonster003/AutoJs6/discussions/577).

Vista previa: los scripts registrados admiten preguntas de parámetros, confirmación, resultados y cancelación. Los flujos de pantalla siguen en P4 y las API de tareas y el panel en P5/P6. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

### Uso

1. Instale el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) en un dispositivo con AutoJs6 build 5288 o posterior.
2. Abra el centro de plugins de AutoJs6, confirme que `AI Agent` se reconoce y habilítelo. Los paquetes oficiales superan automáticamente la verificación de firma.
3. Solicitud de conexión desde el lanzador con espera de 15 segundos y guía para activar y autorizar AI Agent en AutoJs6.
4. Configure carpetas adicionales en "Directorios de scripts" del lanzador, una ruta absoluta por línea. El anfitrión valida y aplica las rutas guardadas; las tareas solo pueden reducir las carpetas aprobadas.

Consulte el [README del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) y [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) para la guía de conexión y el progreso actual.
