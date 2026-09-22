******

### Historial de versiones

******

# v1.0.0

###### 2026/09/23

* `Aviso` Vista previa de desarrollo P0: identidad del plugin, contrato de descubrimiento de AutoJs6 y pantalla de inicio que informa del estado del anfitrión. El bucle del agente, el catálogo de scripts, la API ai.agent y el espacio de tareas aún no están implementados. Véase ROADMAP.md.
* `Aviso` El contrato AI Agent, los intermediarios de capacidades y modelos, la observación de pantalla y la ejecución de scripts registrados están implementados en el anfitrión. La ejecución de tareas del complemento sigue en desarrollo
* `Función` Identidad de plugin `ai-agent` con el servicio INFO, la Wake Activity, el servicio provisional `org.autojs.plugin.AI_AGENT` en el proceso `:agent` y una pantalla de inicio que indica si hay instalado un anfitrión AutoJs6 compatible
* `Función` README, instrucciones del centro de plugins y registro de cambios en 10 idiomas
* `Dependencia` Añadido `common-plugin-api.aar` (módulo de AutoJs6 `plugin-api/common-plugin-api`, build del anfitrión 6.8.0 / 5282, MPL 2.0) como contrato de plugin compartido, bloqueado por hash en `locks/host-api-aars.lock`
