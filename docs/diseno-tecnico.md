# Diseño técnico

El diseño técnico completo (arquitectura, modelo de datos, seguridad, pantallas, endpoints, integración con Power Automate, reglas, operación y migración) está en el documento **Agentes de Scouting - Diseño técnico opción Java**:

https://claude.ai/artifact/GGxMd6WqLXRkbWdLorJSYa

Resumen de decisiones:

- Backoffice Java 21 + Spring Boot 3.5 + Thymeleaf, desplegado como JAR en servidores de UCU.
- SQL Server en la red de UCU, esquema `scouting`, versionado con Flyway (`src/main/resources/db/migration`).
- Login con Entra ID (OIDC); grupos de seguridad propios de la app, no de AD.
- La tabla `ejecucion` es la cola entre la app y los flujos. Solo el flujo "Scouting - Origen" conoce la cola, y lo hace por la API del backoffice (`POST /api/flujo/lote`, `PUT /api/flujo/ejecuciones/{id}/informe`, `POST /api/flujo/ejecuciones/{id}/resultado`), que envuelve los procedimientos `sp_*`. Sin gateway: el backoffice se publica por HTTPS y Power Automate se autentica con la clave del encabezado `X-Scouting-Clave`; Planificador, InstructionBuilder y Comunicaciones son componentes reutilizables con contratos JSON (`power-automate/contratos`).
- `fn_proxima_ejecucion`, `fn_max_participantes` y `fn_max_exploraciones_activas` concentran las reglas para que la app y los flujos usen la misma lógica.

Documento técnico de la funcionalidad (arquitectura de componentes): https://claude.ai/artifact/CxHQQoo5RRHkibv2ev1sQt
