# Agentes de Scouting - Backoffice

Backoffice de UCU para que cada persona configure sus exploraciones de scouting: un prompt, una frecuencia y una lista de participantes. Un proceso en Power Automate ejecuta el agente **Scouting** de Copilot Studio en cada vencimiento, envía el informe por correo y reintenta si algo falla.

- **App**: Java 21, Spring Boot 3.5, Thymeleaf, Bootstrap 5.
- **Base**: SQL Server en la red de UCU (esquema `scouting`, migraciones Flyway).
- **Login**: Entra ID (OIDC). Los permisos se dan con grupos propios de la app.
- **Proceso**: Power Automate + Copilot Studio, conectados a SQL Server por el gateway de datos local. Ver [power-automate/README.md](power-automate/README.md).
- **Diseño técnico**: [docs/diseno-tecnico.md](docs/diseno-tecnico.md).

```
Usuarios ──HTTPS──> Backoffice Java ──JDBC──> SQL Server <──gateway── Power Automate ──> Copilot Studio
                                                                          └──> Outlook (cuenta de servicio)
```

## Estructura

```
src/main/java/uy/edu/ucu/scouting
  config/       Seguridad (OIDC) y propiedades
  security/     Alta del usuario al ingresar y cálculo de roles
  domain/       Entidades JPA y enumeraciones
  repository/   Spring Data
  service/      Reglas de negocio (topes, validaciones, cola de ejecuciones)
  web/          Controladores MVC, formularios y administración
src/main/resources
  db/migration/ Esquema, funciones, procedimientos y datos iniciales
  templates/    Vistas Thymeleaf
db/permisos.sql Logins y permisos (DBA)
power-automate/ Definiciones de referencia de los flujos
```

## Requisitos

- Java 21 y Maven 3.9 o superior.
- SQL Server 2019 o superior (usa `AT TIME ZONE` y `STRING_AGG`).
- Docker, solo para las pruebas de integración y el desarrollo local.

## Desarrollo local

1. Levantar SQL Server y crear la base:

   ```bash
   docker run -d --name scouting-sql -e ACCEPT_EULA=Y -e MSSQL_SA_PASSWORD='Scouting.Local.2026' \
     -p 1433:1433 mcr.microsoft.com/mssql/server:2022-latest
   docker exec scouting-sql /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa \
     -P 'Scouting.Local.2026' -Q "CREATE DATABASE ScoutingTest"
   ```

2. Registrar una app de prueba en Entra con redirect URI `http://localhost:8080/login/oauth2/code/entra` y exportar:

   ```bash
   export ENTRA_TENANT_ID=<tenant UCU>
   export ENTRA_CLIENT_ID=<client id>
   export ENTRA_CLIENT_SECRET=<secret>
   export SCOUTING_ADMINS_INICIALES=tu.usuario@ucu.edu.uy
   ```

3. Ejecutar con el perfil `local` (Flyway crea el esquema):

   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

4. Entrar a http://localhost:8080. Como administrador inicial, sumate a un grupo en **Grupos** y volvé a iniciar sesión.

## Pruebas

```bash
mvn verify
```

- `CorreosTest`: parseo y validación de listas de correos.
- `ProximaEjecucionIT`: cálculo de la próxima ejecución en hora de Montevideo para cada frecuencia.
- `ColaEjecucionesIT`: contrato de los procedimientos que usan los flujos (toma del lote, tope de participantes, éxito, reintentos y falla).
- `ScoutingApplicationIT`: arranque completo y reglas de acceso.

Las pruebas de integración levantan SQL Server con Testcontainers (requiere Docker). GitHub Actions las corre en cada push y PR.

## Despliegue en servidores de UCU

1. **Base**: el DBA crea las bases `ScoutingTest` y `Scouting`, los logins `scouting_app`, `scouting_flujo` y `scouting_deploy` y aplica `db/permisos.sql` (la parte de `scouting_deploy` antes de la primera migración; los roles de la app y del flujo después).
2. **Migraciones**: en cada versión, correr una vez con Flyway activo y el login de despliegue:

   ```bash
   SCOUTING_FLYWAY_ENABLED=true SPRING_FLYWAY_USER=scouting_deploy SPRING_FLYWAY_PASSWORD=*** \
     java -jar scouting-backoffice.jar --spring.main.web-application-type=none
   ```

   (Arranca, aplica las migraciones pendientes y termina. Alternativa: Flyway CLI sobre `src/main/resources/db/migration`.)

3. **App**: ejecutar el JAR como servicio con estas variables:

   | Variable | Ejemplo |
   | --- | --- |
   | `SCOUTING_DB_URL` | `jdbc:sqlserver://sqlucu:1433;databaseName=Scouting;encrypt=true` |
   | `SCOUTING_DB_USER` / `SCOUTING_DB_PASSWORD` | `scouting_app` / secreto |
   | `ENTRA_TENANT_ID`, `ENTRA_CLIENT_ID`, `ENTRA_CLIENT_SECRET` | App registration "Scouting Backoffice" |
   | `SCOUTING_ADMINS_INICIALES` | UPNs separados por coma |
   | `SCOUTING_LOG_FILE` | `/var/log/scouting/scouting.log` |

4. **Proxy**: publicar por HTTPS (por ejemplo `https://scouting.ucu.edu.uy`) reenviando los encabezados `X-Forwarded-*`. La redirect URI de Entra es `https://<host>/login/oauth2/code/entra`.
5. **Monitoreo**: `GET /actuator/health`.

## Notas

- Los roles se calculan al iniciar sesión: un cambio de grupo rige desde el próximo ingreso de la persona.
- Fechas en UTC en la base; la app y los procedimientos las convierten a hora de Montevideo.
- Los flujos no llaman a la app: toda la integración pasa por la tabla `ejecucion` y tres procedimientos.
