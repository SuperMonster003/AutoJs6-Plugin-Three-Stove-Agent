******

### Historial de versiones

******

# v1.0.0

###### 2026/09/23

* `Aviso` Vista previa de desarrollo P0: identidad del plugin, contrato de descubrimiento de AutoJs6 y pantalla de inicio que informa del estado del anfitrión. El bucle del agente, el catálogo de scripts, la API ai.agent y el espacio de tareas aún no están implementados. Véase ROADMAP.md.
* `Aviso` El anfitrión implementa los contratos de AI Agent, intermediarios de capacidades y modelos, observación de pantalla, ejecución de scripts registrados y accesos del panel lateral y centro de plugins; la ejecución de tareas del plugin sigue en desarrollo
* `Función` Identidad de plugin `ai-agent` con el servicio INFO, la Wake Activity, el servicio provisional `org.autojs.plugin.AI_AGENT` en el proceso `:agent` y una pantalla de inicio que indica si hay instalado un anfitrión AutoJs6 compatible
* `Función` README, instrucciones del centro de plugins y registro de cambios en 10 idiomas
* `Función` Catálogo del núcleo Agent con 30 herramientas, control de grupos, esquemas de parámetros, preparación de llamadas bridge, observaciones limitadas y elevación de riesgos sensibles; integración del motor en fases posteriores
* `Función` Núcleo de decisiones Agent con esquemas por protocolo, análisis JSON estricto o por extracción, validación de herramientas/ramas, hasta dos reintentos de corrección y plantillas en inglés/chino; ejecución de tareas aún sin conectar
* `Función` Presupuestos Agent de pasos, llamadas al modelo, duración y tokens, con plazos de herramientas/interacciones, estimación de uso y límites de tokens de salida
* `Mejora` Requisito mínimo fijado en AutoJs6 6.8.0 / build 5285, correspondiente a la entrega de interfaces y accesos del anfitrión en P1
* `Dependencia` Añadido `common-plugin-api.aar` (módulo de AutoJs6 `plugin-api/common-plugin-api`, build del anfitrión 6.8.0 / 5282, MPL 2.0) como contrato de plugin compartido, bloqueado por hash en `locks/host-api-aars.lock`
* `Dependencia` Se añadió Gson 2.13.2 para el análisis JSON estricto con límites y árboles de esquemas
