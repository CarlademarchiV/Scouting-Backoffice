-- Ejecutar por el DBA en cada base (ScoutingTest y Scouting) DESPUÉS de crear los logins a nivel servidor:
--   CREATE LOGIN scouting_app    WITH PASSWORD = '<secreto>';
--   CREATE LOGIN scouting_flujo  WITH PASSWORD = '<secreto>';   -- opcional (ver abajo)
--   CREATE LOGIN scouting_deploy WITH PASSWORD = '<secreto>';   -- usado solo por Flyway en despliegues

CREATE USER scouting_app    FOR LOGIN scouting_app    WITH DEFAULT_SCHEMA = scouting;
CREATE USER scouting_flujo  FOR LOGIN scouting_flujo  WITH DEFAULT_SCHEMA = scouting;
CREATE USER scouting_deploy FOR LOGIN scouting_deploy WITH DEFAULT_SCHEMA = scouting;

-- Flyway: crea y modifica objetos.
ALTER ROLE db_ddladmin   ADD MEMBER scouting_deploy;
ALTER ROLE db_datareader ADD MEMBER scouting_deploy;
ALTER ROLE db_datawriter ADD MEMBER scouting_deploy;
GRANT CREATE SCHEMA TO scouting_deploy;

-- Backoffice Java: datos del esquema scouting y las funciones.
CREATE ROLE rol_scouting_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON SCHEMA::scouting TO rol_scouting_app;
GRANT EXECUTE ON SCHEMA::scouting TO rol_scouting_app;
ALTER ROLE rol_scouting_app ADD MEMBER scouting_app;

-- OPCIONAL, hoy no se usa: Power Automate entra por la API /api/flujo del backoffice (login scouting_app).
-- Solo si se volviera al conector SQL Server vía gateway: el flujo ejecuta los tres procedimientos, sin acceso a tablas.
CREATE ROLE rol_scouting_flujo;
GRANT EXECUTE ON OBJECT::scouting.sp_tomar_lote          TO rol_scouting_flujo;
GRANT EXECUTE ON OBJECT::scouting.sp_guardar_informe     TO rol_scouting_flujo;
GRANT EXECUTE ON OBJECT::scouting.sp_registrar_resultado TO rol_scouting_flujo;
ALTER ROLE rol_scouting_flujo ADD MEMBER scouting_flujo;
