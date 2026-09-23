******

### Historial de versiones

******

# v1.0.0

###### 2026/09/24

* `Aviso` Vista previa: los scripts registrados admiten preguntas de parámetros, confirmación, resultados y cancelación. Los flujos de pantalla siguen en P4 y las API de tareas y el panel en P5/P6.
* `Función` La finalización exige pruebas, los resultados parciales enumeran el trabajo pendiente y las tareas de pedido o pago requieren un estado de pedido observado
* `Función` La verificación conserva el recuento de pantallas sin cambios al recortar el contexto y bloquea la tercera solicitud de una acción equivalente antes de ejecutarla
* `Función` Espera limitada de estabilidad tras cada acción y resumen de cambios desde la última acción en las observaciones siguientes
* `Función` Acciones vinculadas a nodos inspeccionados por el anfitrión, con adición de texto, desplazamiento limitado y resultados con cambios de ventana
* `Función` OCR de pantalla disponible solo cuando el anfitrión confirma un plugin OCR autorizado, con texto agrupado en líneas limitadas y coordenadas
* `Función` Observaciones con referencias a capturas del anfitrión, salida limitada de nodos y consola, y resúmenes de cambios de texto y estado
* `Función` Las tareas de un solo script conservan ID, ruta, ID de ejecución y resultado al concluir el modelo, con null explícito y truncamiento indicado de resultados grandes
* `Función` Ejecución de scripts registrados con comprobación del manifiesto confirmado, observaciones estructuradas, cola de consola censurada y detención del script por tiempo agotado o cancelación
* `Función` Preferencias en memoria por ámbito para los parámetros de scripts, con límite de 4 KiB, truncamiento explícito y desactivación por tarea
* `Función` Validación de parámetros de scripts registrados con valores predeterminados, preguntas por datos faltantes, revisión del riesgo actual y tablas completas para confirmar
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
* `Función` Los scripts registrados se actualizan al iniciar la tarea, con caché de enlace de 60 segundos, clasificación determinista de hasta 24 candidatos, resúmenes acotados de parámetros y consultas script_catalog
* `Corrección` Los botones de confirmar transacción requieren una confirmación de pago separada y no reutilizan permisos de toda la tarea
* `Corrección` Las coincidencias fuera de pantalla con límites vacíos o invertidos conservan el texto e indican coordenadas no utilizables en vez de errores de argumentos
* `Corrección` La relocalización de nodos distingue límites y capacidades de acción para no confundir contenedores anidados con el objetivo
* `Corrección` Indicaciones precisas para corregir destinos de nodos: conservar el prefijo # y omitir snapshotId con selector
* `Corrección` La admisión precarga las reglas de pedido y evita una compilación costosa de reglas
* `Corrección` La verificación distingue nodos de ventanas distintas, mantiene la observación de pantalla tras leer el portapapeles y no confunde transferencias de archivos con pagos
* `Corrección` La lectura de pantalla sin respuesta tras una acción ya no supera el plazo de estabilización
* `Corrección` Ocultación de parámetros multilínea antes de dividir la consola, sin omitir credenciales cuando un parámetro coincide con su etiqueta
* `Corrección` Un servicio en primer plano que se está cerrando ya no rechaza el inicio de la siguiente tarea
* `Mejora` Los límites de las descripciones de confirmación incluyen el escape JSON para mantener tablas grandes dentro del límite de eventos Binder
* `Mejora` El anfitrión mínimo es AutoJs6 6.8.0 / compilación 5289 para inspeccionar nodos de acción y vincular la confirmación a la ejecución
* `Dependencia` Añadidos common-plugin-api, host-capability-api y ai-agent-api de una misma compilación release de AutoJs6 6.8.0 / 5289 (MPL 2.0), fijados con SHA-256
* `Dependencia` Se añadió Gson 2.13.2 para el análisis JSON estricto con límites y árboles de esquemas
