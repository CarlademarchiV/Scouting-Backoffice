# Agentes de Scouting - Backoffice

Backoffice de UCU para que cada persona configure sus exploraciones de scouting: un prompt, una frecuencia y una lista de participantes. Un proceso en Power Automate ejecuta el agente **Scouting** de Copilot Studio en cada vencimiento, envía el informe por correo y reintenta si algo falla.

- **App**: Java 21, Spring Boot 3.5, Thymeleaf, Bootstrap 5.
- **Base**: SQL Server en la red de UCU (esquema `scouting`, migraciones Flyway).
- **Login**: Entra ID (OIDC). Los permisos se dan con grupos propios de la app.
- **Proceso**: Power Automate + Copilot Studio. Un flujo de origen de Scouting, que toma la cola llamando a la API `/api/flujo` del backoffice (HTTPS con clave en encabezado, sin gateway), y tres componentes reutilizables: Planificador, InstructionBuilder y Comunicaciones. Ver [power-automate/README.md](power-automate/README.md).
- **Diseño técnico**: [docs/diseno-tecnico.md](docs/diseno-tecnico.md).

```
Usuarios ───────HTTPS──> Backoffice Java ──JDBC──> SQL Server
Power Automate ─HTTPS──> /api/flujo (X-Scouting-Clave)
      ├──> Copilot Studio (agente Scouting)
      └──> Outlook (cuenta de servicio)
```

## Estructura

```
src/main/java/uy/edu/ucu/scouting
  api/          API REST que consume Power Automate (/api/flujo)
  config/       Seguridad (OIDC, API) y propiedades
  security/     Alta del usuario al ingresar, cálculo de roles y clave de la API
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

## Configuración (secretos y parámetros)

Todo lo que cambia por ambiente o es secreto va en un archivo **fuera del repositorio**:

1. Copiar `config/scouting.example.yml` como `config/scouting.yml`.
2. Completar la base de datos, los datos de Entra ID y los administradores iniciales.
3. `config/scouting.yml` está en `.gitignore`: no se sube nunca. En servidores se deja junto al JAR en `config/scouting.yml` o se indica la ruta con la variable `SCOUTING_CONFIG`.

Las variables de entorno (`SCOUTING_DB_URL`, `ENTRA_CLIENT_SECRET`, etc.) siguen funcionando como alternativa. Los topes, dominios permitidos, reintentos y correo de alertas se administran desde la pantalla **Configuración** de la app.

## Desarrollo local (Windows)

Requisitos: **Java 21** (por ejemplo Eclipse Temurin), **Maven 3.9**, **Docker Desktop** y **Git**.

1. Clonar el repositorio:

   ```powershell
   git clone https://github.com/CarlademarchiV/Scouting-Backoffice.git
   cd Scouting-Backoffice
   ```

2. Levantar SQL Server en Docker y crear la base:

   ```powershell
   docker run -d --name scouting-sql -e ACCEPT_EULA=Y -e "MSSQL_SA_PASSWORD=Scouting.Local.2026" -p 1433:1433 mcr.microsoft.com/mssql/server:2022-latest
   # esperar ~20 segundos a que arranque
   docker exec scouting-sql /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "Scouting.Local.2026" -Q "CREATE DATABASE ScoutingTest"
   ```

3. Registro de aplicación en Entra ID (portal de Entra → Registros de aplicaciones → Nuevo registro):
   - Nombre: `Scouting Backoffice (local)`; cuentas solo de este directorio.
   - URI de redirección, plataforma **Web**: `http://localhost:8080/login/oauth2/code/entra`.
   - En **Certificados y secretos** crear un secreto de cliente y copiar su **Valor**.
   - Anotar el **Id. de aplicación (cliente)** y el **Id. de directorio (inquilino)**.

4. Crear el archivo de configuración y completarlo:

   ```powershell
   Copy-Item config\scouting.example.yml config\scouting.yml
   notepad config\scouting.yml
   ```

5. Arrancar con el perfil `local` (Flyway crea el esquema la primera vez):

   ```powershell
   mvn spring-boot:run "-Dspring-boot.run.profiles=local"
   ```

6. Entrar a http://localhost:8080 con la cuenta UCU. Si tu correo está en `admins-iniciales`, entrás como administrador; para habilitar a otras personas, sumalas a "Usuarios Scouting" en **Grupos** (rige desde su próximo ingreso).

**Sin Entra ID (solo para probar en la PC):** si todavía no hay consentimiento de administrador para el registro de Entra, se puede usar el login de desarrollo, que pide solo el correo:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=local,dev"
```

Se entra en http://localhost:8080 con el correo institucional (si está en `admins-iniciales`, como administrador). Este modo nunca debe activarse en servidores.

**Probar la API desde Power Automate con la app en la PC:** Power Automate corre en la nube y no ve `localhost`. Para una prueba se puede abrir un túnel temporal, por ejemplo con Microsoft Dev Tunnels:

```powershell
winget install Microsoft.devtunnel
devtunnel user login
devtunnel host -p 8080 --allow-anonymous
```

Usar la URL `https://…devtunnels.ms` que muestra como base de la API en el flujo, con la clave de `api.clave` en `X-Scouting-Clave`. Con el login de desarrollo, las pantallas solo responden desde la propia PC, así que por el túnel solo queda accesible la API. Cerrar el túnel (Ctrl+C) al terminar.

Para apagar la base: `docker stop scouting-sql` (y `docker start scouting-sql` para volver a usarla).

## Pruebas

```bash
mvn verify
```

- `CorreosTest`: parseo y validación de listas de correos.
- `ProximaEjecucionIT`: cálculo de la próxima ejecución en hora de Montevideo para cada frecuencia.
- `ColaEjecucionesIT`: contrato de los procedimientos de la cola (toma del lote, tope de participantes, éxito, reintentos y falla).
- `FlujoApiIT`: API `/api/flujo` (clave en encabezado, ciclo lote → informe → resultado, reintento y 404).
- `ScoutingApplicationIT`: arranque completo y reglas de acceso.
- `DevLoginIT`: login de desarrollo (solo desde la propia PC).

Las pruebas de integración levantan SQL Server con Testcontainers (requiere Docker). GitHub Actions las corre en cada push y PR.

## Despliegue en servidores de UCU

1. **Base**: el DBA crea las bases `ScoutingTest` y `Scouting`, los logins `scouting_app` y `scouting_deploy` y aplica `db/permisos.sql` (la parte de `scouting_deploy` antes de la primera migración; el rol de la app después). El login `scouting_flujo` ya no hace falta: Power Automate entra por la API.
2. **Migraciones**: en cada versión, correr una vez con Flyway activo y el login de despliegue:

   ```bash
   SCOUTING_FLYWAY_ENABLED=true SPRING_FLYWAY_USER=scouting_deploy SPRING_FLYWAY_PASSWORD=*** \
     java -jar scouting-backoffice.jar --spring.main.web-application-type=none
   ```

   (Arranca, aplica las migraciones pendientes y termina. Alternativa: Flyway CLI sobre `src/main/resources/db/migration`.)

3. **App**: ejecutar el JAR como servicio con su `config/scouting.yml` al lado (o `SCOUTING_CONFIG=<ruta>`), con `db.usuario: scouting_app`, `db.migrar: false`, los datos del registro de Entra de producción, `api.clave` (distinta por ambiente) y `log.archivo` en la carpeta de logs del servidor.

4. **Proxy**: publicar por HTTPS (por ejemplo `https://scouting.ucu.edu.uy`) reenviando los encabezados `X-Forwarded-*`. La redirect URI de Entra es `https://<host>/login/oauth2/code/entra`. Desde internet alcanza con exponer `/api/flujo/**` (lo llama Power Automate desde la nube de Microsoft); las pantallas pueden quedar solo en la red de UCU. Si el firewall lo permite, limitar `/api/flujo/**` a los rangos IP de Power Platform de la región del entorno.
5. **Monitoreo**: `GET /actuator/health`.

## Notas

- Los roles se calculan al iniciar sesión: un cambio de grupo rige desde el próximo ingreso de la persona.
- Fechas en UTC en la base; la app y los procedimientos las convierten a hora de Montevideo.
- Power Automate se integra solo por la API `/api/flujo` (tres operaciones que envuelven los procedimientos de la cola). No necesita gateway ni login de base de datos.
