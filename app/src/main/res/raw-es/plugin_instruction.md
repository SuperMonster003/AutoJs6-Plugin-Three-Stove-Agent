3-Stove Agent es un agente que realiza operaciones en el teléfono por usted. Tras describir un objetivo en una frase, observa la pantalla a través de AutoJs6 y toca, escribe y desplaza paso a paso, o elige un script registrado, completa sus parámetros y lo ejecuta, hasta alcanzar el objetivo. Las operaciones sensibles, como pagar, enviar o eliminar, requieren confirmación previa, y cada tarea tiene límites de pasos y de tiempo.

### Primeros pasos

1. Instale y active [3-Stone AI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stone-AI) y añada en él un modelo en línea o importe un modelo local. 3-Stove Agent llama a los modelos a través de él.
2. Abra 3-Stove Agent. Si la pantalla de inicio indica que no está conectado, toque "Conectar con AutoJs6".
3. Toque la cápsula del modelo en la barra superior para elegir un modelo, o mantenga "Automático" (se prefiere un modelo local).
4. Escriba un objetivo en la barra de entrada y toque "Iniciar tarea". La tarjeta de la tarea muestra el progreso de cada paso; las preguntas y las confirmaciones también se atienden allí.

### Funciones principales

- **Tareas e historial**: mientras una tarea se ejecuta, puede ver sus pasos, responder preguntas o detenerla en cualquier momento. "Historial de tareas" conserva las tareas terminadas, que pueden compartirse como resumen o ejecutarse de nuevo (volver a ejecutar solo coloca el objetivo original en la barra de entrada para revisarlo antes de empezar).
- **Confirmaciones y permisos de las operaciones**: las operaciones de solo lectura se ejecutan directamente; las operaciones sensibles, como pagar, enviar, eliminar o escribir archivos, requieren confirmación de forma predeterminada, que puede darse una vez o para el resto de la tarea. "Permisos de las operaciones" en los ajustes ofrece "Estandar", "Prudente" o "Acceso completo" (sin confirmaciones, marcado claramente mientras está activo).
- **Preajustes**: guarde como preajuste los grupos de herramientas, presupuestos y contexto fijo que use a menudo, y elíjalo en la etiqueta de preajuste de la barra de entrada antes de empezar. Con "Modo plan" activado, una tarea propone primero un plan de 3 a 8 pasos y lo ejecuta tras su revisión.
- **Memoria**: guarde las preferencias que el agente debe recordar; se adjuntan automáticamente a las tareas. Cada memoria que propone el agente se guarda solo tras confirmarla; no guarde contraseñas ni otras credenciales en la memoria.
- **Scripts registrados**: tras añadir un comentario `@agent` al principio de un script (vea el ejemplo más abajo), el agente puede elegir y ejecutar ese script. Las ubicaciones de scripts fuera del directorio de trabajo se añaden en "Directorios de scripts".
- **Burbuja flotante**: una vez activada en los ajustes, permite introducir objetivos, atender confirmaciones o detener tareas sobre otras aplicaciones.
- **Inicio rápido**: comparta texto con 3-Stove Agent, use el acceso directo "Nueva tarea" de la pantalla de inicio o fije un preajuste en la pantalla de inicio. Estas entradas solo crean un borrador, que se ejecuta al tocar "Iniciar tarea".
- **Voz**: tras activar "Activar entrada de voz" en los ajustes, aparece un micrófono junto al cuadro de entrada; con 3-Macaw Speech instalado, el reconocimiento se hace sin conexión. "Escucha permanente" (desactivada de forma predeterminada) acepta una tarea tras reconocer la palabra de activación "小炉小炉" o "Hey Stove". Los resultados de voz también se convierten solo en borradores.
- **Más herramientas**: los ajustes permiten activar el reconocimiento de texto (requiere un plugin de OCR), las herramientas de otros plugins (como la lectura en voz alta y la transcripción de 3-Macaw Speech), las herramientas de servidores MCP y los scripts generados por el modelo, que solo se ejecutan tras revisarlos.

### Uso en scripts

En un script de AutoJs6, inicie una tarea con `ai.agent.run`; las preguntas y las confirmaciones se siguen atendiendo en la interfaz de 3-Stove Agent:

```js
let run = ai.agent.run('Lee la versión de Android e informa del valor observado.', {
    tools: ['observe', 'user'],
    interaction: 'plugin',
    budget: { maxSteps: 8 },
});
run.on('progress', (event) => console.log(event.message));
run.result.then(
    (result) => console.log(result.status, result.summary),
    (error) => console.error(error.code, error.message),
);
```

- Compruebe el `status` del resultado: aunque la Promise se cumpla, el resultado puede ser `completed`, `partial`, `failed`, `blocked` o `cancelled`. Llame a `run.cancel()` para detener la tarea.
- Añada `preset: 'your-preset-name'` a las opciones para usar un preajuste guardado, o `plan: true` para que la tarea proponga primero un plan.
- Todas las opciones y eventos se describen en [Documentación de AutoJs6 > ai.agent](https://docs.autojs6.com/#/ai).

Para que el agente ejecute un script propio, guarde el ejemplo siguiente como `text-counter.js` en el directorio de trabajo de AutoJs6. Después, pida el número de caracteres de un texto; si falta un parámetro, el agente lo pregunta primero:

```js
/**
 * @agent
 * @description Count Unicode characters in the supplied text
 * @param {string} text Text to count
 * @risk readonly
 * @confirm never
 * @timeout 10000
 */
let context = ai.agent.context();
if (!context) throw Error('Start this registered script through 3-Stove Agent');
let text = new java.lang.String(context.parameters.text);
ai.agent.result({ characters: text.codePointCount(0, text.length()) });
```

### Requisitos

- Android 7.0 o posterior y AutoJs6 6.8.0 o posterior.
- Los modelos los proporciona [3-Stone AI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stone-AI); 3-Stove Agent no guarda credenciales de modelos.
- Las operaciones en pantalla usan el servicio de accesibilidad de AutoJs6. Se activa automáticamente cuando AutoJs6 tiene configurado Root, los ajustes seguros o Shizuku; de lo contrario, actívelo según se indique.
- El permiso de notificaciones se usa para las confirmaciones en segundo plano y los avisos de tareas; la burbuja flotante requiere el permiso "mostrar sobre otras aplicaciones".

### Preguntas frecuentes

**Motivos por los que se necesita AutoJs6**

3-Stove Agent se encarga de las tareas y la interfaz, mientras que las llamadas a modelos, las operaciones en pantalla y los scripts pasan por AutoJs6. Sin conexión con AutoJs6 se puede consultar el historial, pero no iniciar tareas nuevas; una conexión perdida bloquea la tarea en curso y la reconexión no la repite.

**Casos en que los pagos requieren confirmación**

De forma predeterminada, cada pago requiere su propia confirmación; aprobar un pedido u otra operación no aprueba el pago, y agotar el tiempo equivale a rechazarlo. "Acceso completo" omite las confirmaciones de pago, así que actívelo solo si confía en el objetivo y el modelo.

**Motivos por los que un modelo local es lento o no completa una tarea**

Que un modelo se cargue no garantiza que complete tareas; empiece con tareas sencillas y revise los resultados. En teléfonos antiguos, la primera llamada puede superar el límite de 5 minutos; durante la espera, la pantalla de tareas muestra los segundos transcurridos, y un resultado por tiempo agotado indica las causas probables y las soluciones.

**Motivos por los que se detiene la escucha permanente**

La escucha permanente no se inicia con el arranque y se reanuda al abrir la pantalla de tareas o los ajustes. En ColorOS o MIUI, permita el inicio automático y añada la aplicación a la lista blanca de batería; de lo contrario, el sistema puede detener el servicio permanente. Tampoco se reconoce la palabra de activación mientras otra aplicación graba.

La lista de herramientas, el formato de registro de scripts y los detalles de permisos están en la [página del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent).
