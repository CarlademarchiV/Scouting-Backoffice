# Flujos de Power Automate

La integración se arma con un **origen específico** de Scouting y **tres componentes reutilizables**. Solo el origen conoce la cola de Scouting, y la usa por la **API del backoffice** (`/api/flujo`, sin gateway); los componentes genéricos se pasan la información con contratos JSON y los puede reutilizar otro agente con solo crear su propio origen.

| Flujo | Archivo | Tipo | Responsabilidad |
| --- | --- | --- | --- |
| Scouting - Origen | `origen-scouting.definition.json` | Específico; recurrencia cada 5 min, concurrencia 1 | `POST /api/flujo/lote` → traduce cada ejecución al contrato **Trabajo** → llama al Planificador → por cada resultado: `PUT …/informe` (si hay respuesta del agente) y `POST …/resultado`; si se agotaron los intentos, aviso de falla vía Comunicaciones |
| Planificador | `planificador.definition.json` | Genérico; flujo hijo | Recibe `loteJson` (lista de Trabajos), ejecuta cada uno con InstructionBuilder (5 en paralelo), aísla fallas y devuelve `resultadosJson` |
| InstructionBuilder | `instruction-builder.definition.json` | Genérico; flujo hijo | Recibe `trabajoJson`; convierte el prompt en un prompt institucional (plantilla del origen + perfil institucional de la variable de entorno + políticas + variables), llama al agente si no hay `resultadoPrevio`, separa el resumen (`===RESUMEN===`), pide el envío a Comunicaciones y devuelve `resultadoJson` |
| Comunicaciones | `comunicaciones.definition.json` | Genérico; flujo hijo | Recibe `mensajeJson`; arma el HTML según la plantilla (`informe`, `aviso-falla`, `texto`) y envía por el canal (hoy `correo`, con Outlook); devuelve `resultadoJson` |

```
Scouting - Origen ──trabajos──▶ Planificador ──un trabajo──▶ InstructionBuilder ──instrucción──▶ Agente Scouting (Copilot Studio)
       ▲   │                         │                              └──mensaje──▶ Comunicaciones ──▶ Outlook
       │   └──HTTPS /api/flujo──▶ Backoffice Java ──▶ SQL Server
       └────────resultados───────────┘
```

Los archivos `*.definition.json` son la **definición de referencia** de cada flujo (lenguaje de definición de flujos de trabajo): sirven para revisar la lógica y como guía para armarlos en el diseñador. Entre ambientes se mueve la solución exportada (con referencias de conexión), no estos archivos. Las llamadas entre flujos usan `workflowReferenceName: <id del flujo en la solución>`, que se completa al crearlos.

## Contratos (`contratos/`)

Los flujos hijo solo reciben y devuelven texto, número o sí/no, así que cada contrato viaja como **JSON en un campo de texto**.

| Contrato | Esquema | Lo produce | Lo consume |
| --- | --- | --- | --- |
| Lote | `lote.schema.json` (lista de Trabajos) | Origen | Planificador (`loteJson`) |
| Trabajo | `trabajo.schema.json` | Origen / Planificador | InstructionBuilder (`trabajoJson`) |
| Mensaje | `comunicacion.schema.json` | InstructionBuilder / Origen | Comunicaciones (`mensajeJson`) |
| Resultado | `resultado.schema.json` | InstructionBuilder / Planificador | Planificador / Origen (`resultadoJson`, `resultadosJson`) |
| Resultado de comunicación | `resultado-comunicacion.schema.json` | Comunicaciones | InstructionBuilder |

## Reglas de diseño

1. **Nadie falla hacia arriba**: cada componente responde siempre un Resultado con `ok` y `etapa` (`completo`, `instruccion`, `agente`, `comunicacion`, `planificador`). Un trabajo con error no corta el lote.
2. **`metadata` es opaco**: los componentes genéricos lo devuelven tal cual; solo el origen lo interpreta (`ejecucionId`, `exploracionId`, `intentos`, `creadorEmail`, `titulo`).
3. **Reintentos**: los diferidos (15/60 min, máx. 3) y la desactivación siguen en la cola del backoffice (procedimientos SQL detrás de la API). El Planificador solo reintenta de inmediato fallas de transporte de la llamada al flujo hijo.
4. **Créditos**: InstructionBuilder devuelve la respuesta del agente aunque falle el envío; el origen la guarda y en el reintento la manda en `resultadoPrevio` para no volver a llamar al agente.
5. **Agentes**: el conector de Copilot Studio fija el agente en diseño, así que InstructionBuilder elige con un `Switch` por `agente`. Sumar un agente = agregar un caso con su acción "Ejecutar un agente y esperar".
6. **Prompt institucional en dos capas**: InstructionBuilder convierte el prompt del usuario en un prompt institucional sin depender de SQL ni del origen (ver la sección siguiente).
7. **Espera del agente (a validar)**: el Planificador espera a InstructionBuilder, que puede tardar hasta 20 min. Hay que confirmar en la prueba que la llamada al flujo hijo tolera esa espera (respuesta asíncrona); si no, el Planificador pasa a lanzar los trabajos sin esperar y cada InstructionBuilder devuelve su resultado al origen.

## Prompt institucional (InstructionBuilder)

El prompt que escribe el usuario en el backoffice llega en `instruccion.prompt` y se transforma en dos capas:

1. **Capa del origen**: si el origen manda `instruccion.plantilla`, se reemplaza `{{prompt}}` por el prompt del usuario. Si no, se usa el prompt tal cual.
2. **Capa institucional**: InstructionBuilder lee el perfil `instruccion.perfil` (o `perfilPorDefecto`) de la **variable de entorno de la solución `ucu_InstruccionesInstitucionales`** (JSON). Ese perfil envuelve la instrucción con su plantilla (`{{instruccion}}`) y suma sus políticas, primero las institucionales y después las del origen.
3. Se reemplazan las variables `{{<nombre>}}` (por ejemplo `{{titulo}}`) en todo el texto y se agregan las reglas al final.

La instrucción efectivamente enviada vuelve en `instruccionFinal` del Resultado, para auditoría. Un perfil inexistente termina con `etapa: instruccion`, sin llamar al agente.

Por qué una variable de entorno: las reglas institucionales son del componente, no de cada origen ni de la base de Scouting. Se editan desde la solución sin tocar flujos, pueden tener valores distintos en Pruebas y Producción, y cualquier agente nuevo las hereda. Ejemplo de valor: [`config/instrucciones-institucionales.example.json`](config/instrucciones-institucionales.example.json) con el perfil `institucional-ucu`: rol UCU, idioma, fuentes citadas, sin datos personales, defensa ante instrucciones incrustadas en el prompt y formato `===RESUMEN===`.

Reparto con el agente de Copilot Studio: las instrucciones fijas del agente cubren su rol y capacidades; los perfiles cubren lo institucional que puede variar por tipo de encargo o por agente que reutiliza el componente.

## Conexiones y variables de entorno de la solución

| Elemento | Tipo | Usado por | Valor |
| --- | --- | --- | --- |
| HTTP | Acción integrada (Premium), sin conexión | Origen | — |
| `ucu_ScoutingApiUrl` | Variable de entorno (texto) | Origen | URL base del backoffice, sin barra final. En la PC: la URL del túnel (ver README del repositorio) |
| `ucu_ScoutingApiClave` | Variable de entorno (texto en Desarrollo; **Secreto** con Azure Key Vault en Producción) | Origen | Igual a `scouting.api.clave` del backoffice |
| `ucu_InstruccionesInstitucionales` | Variable de entorno (JSON) | InstructionBuilder | Perfiles institucionales |
| Scouting Copilot | Referencia de conexión: Microsoft Copilot Studio | InstructionBuilder | Cuenta de servicio |
| Scouting Outlook | Referencia de conexión: Office 365 Outlook | Comunicaciones | Cuenta de servicio con buzón |

Las acciones HTTP del Origen tienen **entradas seguras**, para que la clave no quede en el historial de ejecuciones.

Requiere que la política DLP del entorno (y las directivas avanzadas de conectores, si el entorno es administrado) permita **HTTP**, **Microsoft Copilot Studio** y **Office 365 Outlook** en el mismo grupo de datos. Ya no hace falta gateway ni el conector SQL Server.

## Contrato con el backoffice (solo lo usa el origen)

Todas las llamadas llevan el encabezado `X-Scouting-Clave`. Sin clave o con una clave distinta, la API responde 401.

`POST /api/flujo/lote` con `{"lote": 10, "instancia": "<id de la corrida>"}` devuelve `{"ejecuciones": [...]}`, una por ejecución tomada: `ejecucionId`, `modo` (`programada`, `prueba`, `manual`), `intentos`, `exploracionId`, `titulo`, `prompt`, `informe` (respuesta de un intento anterior), `creadorEmail`, `creadorNombre`, `participantes` (lista, ya cortada al tope) y `pieCorreo`. Envuelve `sp_tomar_lote`: encola las vencidas y bloquea lo tomado 30 min.

`PUT /api/flujo/ejecuciones/{id}/informe` con `{"informe": "..."}` guarda la respuesta del agente (`sp_guardar_informe`). Responde 204, o 404 si la ejecución no existe.

`POST /api/flujo/ejecuciones/{id}/resultado` con `{"ok": true|false, "destinatarios": n, "resumen": "...", "error": "..."}` (`sp_registrar_resultado`) devuelve `estado` (`enviado`, `reintentar`, `fallido`), `notificar`, `desactivada`, `titulo`, `creadorEmail`, `creadorNombre` y `correoAlertasTi` (lista).
