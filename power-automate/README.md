# Flujos de Power Automate

Dos flujos, en una solución del entorno de Power Platform de Scouting. Ambos usan el conector **SQL Server** a través del **gateway de datos local** con el login `scouting_flujo`, que solo puede ejecutar tres procedimientos.

| Flujo | Disparo | Qué hace |
| --- | --- | --- |
| Scouting - Planificador | Recurrencia cada 5 minutos, concurrencia 1 | `sp_tomar_lote` encola las exploraciones vencidas y devuelve hasta 10 ejecuciones; llama al flujo hijo por cada una (concurrencia 5) |
| Scouting - Ejecutar exploración | Flujo hijo (disparo manual) | Agente Scouting (si no hay informe guardado) → `sp_guardar_informe` → correo → `sp_registrar_resultado`; en error registra y avisa si se agotaron los intentos |

Los archivos `*.definition.json` son la **definición de referencia** de cada flujo (lenguaje de definición de flujos de trabajo). Sirven para revisar la lógica y como guía para armarlos en el diseñador. Para pasar entre ambientes se exporta la solución (con las referencias de conexión), no estos archivos.

## Conexiones (referencias de conexión de la solución)

| Referencia | Conector | Cuenta |
| --- | --- | --- |
| Scouting SQL | SQL Server (gateway) | Login SQL `scouting_flujo` |
| Scouting Copilot | Microsoft Copilot Studio | Cuenta de servicio |
| Scouting Outlook | Office 365 Outlook | Cuenta de servicio con buzón |

## Contrato con SQL Server

`sp_tomar_lote(@lote int, @instancia nvarchar)` devuelve una fila por ejecución tomada:

| Columna | Tipo | Uso |
| --- | --- | --- |
| ejecucion_id | bigint | Clave para los otros procedimientos |
| modo | tinyint | 0 Programada, 1 Prueba (solo al creador), 2 Manual |
| intentos | int | Intento actual (1..max) |
| exploracion_id, titulo, prompt | | Datos de la exploración |
| informe | nvarchar(max) | Informe de un intento anterior; si viene, no se llama al agente |
| creador_email, creador_nombre | | Para y Responder a del correo |
| participantes | nvarchar | Correos activos separados por `;`, ya cortados al tope |
| pie_correo | nvarchar | Texto del pie |

`sp_guardar_informe(@ejecucion_id, @informe)` guarda el informe apenas lo genera el agente.

`sp_registrar_resultado(@ejecucion_id, @ok, @destinatarios, @resumen, @error)` devuelve `estado`, `notificar`, `desactivada`, `titulo`, `creador_email`, `creador_nombre`, `correo_alertas_ti`. Si `notificar = 1`, el flujo avisa al creador con copia a TI.

## Reintentos

- Cada acción contra SQL reintenta 4 veces con espera exponencial; el envío de correo, 3.
- Si el flujo hijo falla, la ejecución pasa a **Reintentar** (15 minutos el primer reintento, 60 el segundo) y la vuelve a tomar el planificador.
- Si el flujo se corta sin registrar resultado, el bloqueo de 30 minutos vence y la ejecución se toma de nuevo.
- Tras el último intento queda **Fallido**; tres fallidas seguidas desactivan la exploración.
