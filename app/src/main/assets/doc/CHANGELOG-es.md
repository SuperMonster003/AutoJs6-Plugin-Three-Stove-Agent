******

### Historial de versiones

******

# v1.0.0

###### 2026/09/23

* `Aviso` Vista previa: conexión al anfitrión y control de tareas integrados. La ejecución de scripts y recuperación de pantalla continúan en P3/P4; la API de scripts y el panel llegarán en P5/P6.
* `Aviso` El anfitrión envía las tareas. El panel independiente y la API ai.agent siguen previstos para P5/P6.
* `Función` `ai-agent`: `AiAgentPluginInfoService`, `WakeActivity`, `AiAgentPluginService`, `ui.LauncherActivity`
* `Función` README, instrucciones del centro de plugins y registro de cambios en 10 idiomas
* `Función` Catálogo del núcleo Agent con 30 herramientas, control de grupos, esquemas de parámetros, preparación de llamadas bridge, observaciones limitadas y elevación de riesgos sensibles
* `Función` Núcleo de decisiones Agent con esquemas por protocolo, análisis JSON estricto o por extracción, validación de herramientas/ramas, hasta dos reintentos de corrección y plantillas en inglés/chino
* `Función` Presupuestos Agent de pasos, llamadas al modelo, duración y tokens, con plazos de herramientas/interacciones, estimación de uso y límites de tokens de salida
* `Función` Confirmación Agent con políticas predeterminada/cautelosa, permisos por tarea para la misma herramienta y riesgo, confirmación de cada pago y palabras clave en 10 idiomas
* `Función` Registro privado Agent limitado a 200 pasos y 1 MiB, con ocultación de contraseñas y resultados finales acotados que conservan estado y contadores
* `Función` Conexión con identidad del anfitrión verificada, cola de tareas, respuestas, cancelación, consultas e historial privado; las tareas se bloquean al perder el anfitrión y no se reanudan al reiniciar el proceso
* `Función` Contexto Agent determinista con límites de bytes, pares recientes completos, prompts en inglés/chino y prioridad de nodos; presupuesto local de 3000 tokens y firmas compactas de herramientas
* `Función` Cliente de modelo del anfitrión con validación de eventos, uso, cancelación, plazos y cambio de formato acotado; cada cambio cuenta como llamada y conserva el límite de reparación
* `Función` Solicitud de conexión desde el lanzador con espera de 15 segundos y guía para activar y autorizar AI Agent en AutoJs6
* `Función` Notificaciones en primer plano solo mientras haya tareas, con progreso, Detener y Ver; respuestas y confirmaciones por acción desde el lanzador
* `Mejora` Requisito mínimo fijado en AutoJs6 6.8.0 / build 5285, correspondiente a la entrega de interfaces y accesos del anfitrión en P1
* `Dependencia` Añadidos common-plugin-api, host-capability-api y ai-agent-api de una misma compilación release de AutoJs6 6.8.0 / 5285 (MPL 2.0), fijados con SHA-256
* `Dependencia` Se añadió Gson 2.13.2 para el análisis JSON estricto con límites y árboles de esquemas
