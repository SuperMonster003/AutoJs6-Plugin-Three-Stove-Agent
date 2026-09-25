******

### Historial de versiones

******

# v1.0.0

###### 2026/09/25

* `Aviso` Vista previa de desarrollo de 1.0.0. Las API de tareas y las entradas de la interfaz están implementadas; las pruebas de auditoría P7 están documentadas. Las comprobaciones y la publicación de P8 siguen pendientes. Consulte [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) para los casos aprobados y las limitaciones.
* `Aviso` Requiere Android 7+, AutoJs6 6.8.0 / build 5293+ para las API de tareas y 3-Stone AI habilitado con un modelo configurado. OCR es opcional. El protocolo de conexión por sí solo requiere build 5289+.
* `Función` Panel de tareas en lenguaje natural con preguntas, progreso, parada y resultados; entrada flotante opcional, texto compartido, accesos a preajustes y borradores de voz
* `Función` API ai.agent para crear tareas, eventos, consultas, respuestas y cancelación, incluidas tareas detached y resultados/contexto de scripts registrados
* `Función` Scripts project.json / @agent con búsqueda, validación y valores predeterminados de parámetros, preguntas por valores ausentes, confirmación, ejecución limitada y resultados estructurados
* `Función` Observación mediante nodos de texto y OCR autorizado opcional, clics por referencia, entrada, desplazamiento y teclas, con verificación de cambios y evidencia de finalización
* `Función` Modelos en línea y locales mediante AutoJs6 sin guardar credenciales; un objetivo seleccionado ausente falla sin cambiar de modelo silenciosamente
* `Función` Presupuestos de pasos, llamadas, duración y tokens, plazos de herramientas, hasta dos reintentos de reparación por paso y protección ante acciones repetidas sin efecto
* `Función` Preajustes con nombre y ajustes globales de modelo, contexto, herramientas, presupuestos, cautela, carpetas y memoria; gesture/files/shell desactivados inicialmente
* `Función` Memoria de preferencias por ámbito con aprobación individual de propuestas/importaciones, edición, borrado y copia JSON, hasta 500 entradas / 256 KiB; inyección automática hasta 4 KiB
* `Función` Detalles y cronologías, filtros, borradores de repetición y exportación JSON depurada, con historial privado de hasta 200 tareas / 32 MiB
* `Función` Confirmación según riesgo en panel, notificaciones y tarjeta flotante; pagos y memoria siempre con aprobación individual; perder el anfitrión bloquea tareas y reiniciar no las reanuda
* `Función` Ajustes, historial sin conexión y avisos legales en diez idiomas; consulta manual de GitHub con cancelación, caché diaria y versiones ignoradas, sin descarga automática de APK
* `Corrección` Áreas táctiles de formularios y filtros, ajuste de textos y columnas de parámetros, y controles flotantes con fuentes grandes y en Android 7
* `Corrección` Omisiones en la validación de credenciales de la memoria de preferencias con caracteres de ancho completo, caracteres de ancho cero y otros nombres de credenciales
* `Corrección` La burbuja de tareas podía permanecer oculta al activar un dispositivo sin bloqueo seguro mientras se estabilizaba el estado de la pantalla
* `Corrección` Las tareas interrumpidas al terminar el proceso del plugin se registran como fallidas al reiniciar; la pantalla bloqueada detiene las acciones posteriores
* `Corrección` Las herramientas de archivos rechazan rutas con recorrido, absolutas o no válidas antes de la confirmación o el envío al anfitrión; el historial guarda categorías limitadas de rechazo sin el texto rechazado del modelo
* `Corrección` La confirmación vuelve a la app de destino antes de reanudar acciones, procesa la respuesta aunque se detenga la pantalla y contrae la tarjeta flotante antes de ejecutar
* `Corrección` El inicio en Android 13 ya no falla al consultar el controlador de las barras del sistema antes de crear la vista de la ventana
* `Corrección` El historial se ordena y conserva por el inicio de las tareas para que reescribir archivos al reiniciar no elimine las más recientes
* `Corrección` Las respuestas y confirmaciones verifican el propietario interaction para impedir que un script responda por la interfaz del complemento
* `Corrección` Los botones de confirmar transacción requieren una confirmación de pago separada y no reutilizan permisos de toda la tarea
* `Corrección` Las coincidencias fuera de pantalla con límites vacíos o invertidos conservan el texto e indican coordenadas no utilizables en vez de errores de argumentos
* `Corrección` La relocalización de nodos distingue límites y capacidades de acción para no confundir contenedores anidados con el objetivo
* `Corrección` Indicaciones precisas para corregir destinos de nodos: conservar el prefijo # y omitir snapshotId con selector
* `Corrección` La admisión precarga las reglas de pedido y evita una compilación costosa de reglas
* `Corrección` La verificación distingue nodos de ventanas distintas, mantiene la observación de pantalla tras leer el portapapeles y no confunde transferencias de archivos con pagos
* `Corrección` La lectura de pantalla sin respuesta tras una acción ya no supera el plazo de estabilización
* `Corrección` Ocultación de parámetros multilínea antes de dividir la consola, sin omitir credenciales cuando un parámetro coincide con su etiqueta
* `Corrección` Un servicio en primer plano que se está cerrando ya no rechaza el inicio de la siguiente tarea
* `Mejora` El ajuste de historiales largos reutiliza fragmentos de instrucciones y observaciones sin cambios para reducir el tiempo de procesamiento por paso
* `Mejora` Los límites de las descripciones de confirmación incluyen el escape JSON para mantener tablas grandes dentro del límite de eventos Binder
* `Mejora` El anfitrión mínimo es AutoJs6 6.8.0 / compilación 5289 para inspeccionar nodos de acción y vincular la confirmación a la ejecución
* `Dependencia` Añadidos common-plugin-api, host-capability-api y ai-agent-api de una misma compilación release de AutoJs6 6.8.0 / 5289 (MPL 2.0), fijados con SHA-256
* `Dependencia` Se añadió Gson 2.13.2 para el análisis JSON estricto con límites y árboles de esquemas
