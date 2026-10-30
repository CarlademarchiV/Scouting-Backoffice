# Diseño técnico

El diseño técnico completo (arquitectura, modelo de datos, seguridad, pantallas, endpoints, integración con Power Automate, reglas, operación y migración) está en el documento **Agentes de Scouting - Diseño técnico opción Java**:

https://claude.ai/artifact/GGxMd6WqLXRkbWdLorJSYa

Resumen de decisiones:

- Backoffice Java 21 + Spring Boot 3.5 + Thymeleaf, desplegado como JAR en servidores de UCU.
- SQL Server en la red de UCU, esquema `scouting`, versionado con Flyway (`src/main/resources/db/migration`).
- Login con Entra ID (OIDC); grupos de seguridad propios de la app, no de AD.
- La tabla `ejecucion` es la cola entre la app y los flujos; los flujos solo ejecutan `sp_tomar_lote`, `sp_guardar_informe` y `sp_registrar_resultado` vía gateway.
- `fn_proxima_ejecucion`, `fn_max_participantes` y `fn_max_exploraciones_activas` concentran las reglas para que la app y los flujos usen la misma lógica.
