AI Agent convierte un objetivo en lenguaje natural en acciones sobre un dispositivo Android que ejecuta AutoJs6. O bien elige un script que el usuario ha registrado para el agente, completa sus parámetros y lo ejecuta; o bien observa la pantalla a través del árbol de nodos de accesibilidad y actúa paso a paso (observar, decidir, actuar, verificar) hasta alcanzar el objetivo, necesitar una confirmación o agotar un presupuesto. Responde a la [discusión #577 de AutoJs6](https://github.com/SuperMonster003/AutoJs6/discussions/577).

Vista previa de desarrollo: el panel P6.1 y el historial P6.2 están disponibles. La API ai.agent requiere AutoJs6 build 5293 o posterior. Los preajustes personalizados y otras interfaces continúan en P6.3-P6.7; la fiabilidad y la publicación siguen en P7/P8. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

### Uso

1. Instale el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) en un dispositivo con AutoJs6 build 5289 o posterior.
2. Abra el centro de plugins de AutoJs6, confirme que `AI Agent` se reconoce y habilítelo. Los paquetes oficiales superan automáticamente la verificación de firma.
3. Abre AI Agent, conecta AutoJs6, introduce un objetivo e inicia con el preajuste predeterminado. Responde o confirma en la tarjeta y consulta los detalles de las tareas recientes.
4. Configure carpetas adicionales en "Directorios de scripts" del lanzador, una ruta absoluta por línea. El anfitrión valida y aplica las rutas guardadas; las tareas solo pueden reducir las carpetas aprobadas.
5. Hasta 200 tareas / 32 MiB. Se eliminan primero las tareas terminadas consultadas hace más tiempo. Repetir rellena el objetivo y preajuste originales en el panel. Revísalos y pulsa Iniciar tarea para ejecutarla. Vaciar el historial conserva las tareas en curso. Se conservan contadores, nombres de herramientas y confirmaciones. Se eliminan objetivos, parámetros, observaciones y resultados de scripts. Elige dónde guardar el archivo.

Consulte el [README del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) y [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) para la guía de conexión y el progreso actual.
