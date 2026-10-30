-- Reglas compartidas por la app Java y los flujos de Power Automate.

-- Próxima ejecución en UTC: primera fecha/hora local posterior a @desde_utc que cumple la frecuencia.
CREATE OR ALTER FUNCTION scouting.fn_proxima_ejecucion (
    @frecuencia   tinyint,
    @dia_semana   tinyint,
    @dia_mes      tinyint,
    @hora         tinyint,
    @desde_utc    datetime2(0),
    @anterior_utc datetime2(0),
    @zona         nvarchar(64)
)
RETURNS datetime2(0)
AS
BEGIN
    DECLARE @ahora_local datetime2(0) = CONVERT(datetime2(0), (@desde_utc AT TIME ZONE 'UTC') AT TIME ZONE @zona);
    DECLARE @hoy date = CAST(@ahora_local AS date);
    -- Día ISO (1 = lunes) sin depender de SET DATEFIRST: 1900-01-01 fue lunes.
    DECLARE @dow_hoy int = (DATEDIFF(day, '19000101', @hoy) % 7) + 1;
    DECLARE @cand datetime2(0);

    IF @frecuencia = 0
    BEGIN
        SET @cand = DATEADD(hour, @hora, CAST(@hoy AS datetime2(0)));
        IF @cand <= @ahora_local SET @cand = DATEADD(day, 1, @cand);
    END
    ELSE IF @frecuencia = 2 AND @anterior_utc IS NOT NULL
    BEGIN
        DECLARE @anterior_local datetime2(0) = CONVERT(datetime2(0), (@anterior_utc AT TIME ZONE 'UTC') AT TIME ZONE @zona);
        SET @cand = DATEADD(hour, @hora, CAST(CAST(@anterior_local AS date) AS datetime2(0)));
        WHILE @cand <= @ahora_local SET @cand = DATEADD(day, 14, @cand);
    END
    ELSE IF @frecuencia IN (1, 2)
    BEGIN
        SET @cand = DATEADD(hour, @hora, CAST(DATEADD(day, (@dia_semana - @dow_hoy + 7) % 7, @hoy) AS datetime2(0)));
        IF @cand <= @ahora_local SET @cand = DATEADD(day, 7, @cand);
    END
    ELSE IF @frecuencia = 3
    BEGIN
        SET @cand = DATEADD(hour, @hora, CAST(DATEFROMPARTS(YEAR(@hoy), MONTH(@hoy), @dia_mes) AS datetime2(0)));
        IF @cand <= @ahora_local SET @cand = DATEADD(month, 1, @cand);
    END

    RETURN CONVERT(datetime2(0), (@cand AT TIME ZONE @zona) AT TIME ZONE 'UTC');
END;
GO

-- Tope de participantes del usuario: el más alto de sus grupos activos con tope propio, si no el global.
CREATE OR ALTER FUNCTION scouting.fn_max_participantes (@usuario_id bigint)
RETURNS int
AS
BEGIN
    RETURN COALESCE(
        (SELECT MAX(g.max_participantes)
           FROM scouting.grupo g
           JOIN scouting.grupo_miembro m ON m.grupo_id = g.id
          WHERE m.usuario_id = @usuario_id AND g.activo = 1),
        (SELECT max_participantes FROM scouting.configuracion WHERE id = 1));
END;
GO

CREATE OR ALTER FUNCTION scouting.fn_max_exploraciones_activas (@usuario_id bigint)
RETURNS int
AS
BEGIN
    RETURN COALESCE(
        (SELECT MAX(g.max_exploraciones_activas)
           FROM scouting.grupo g
           JOIN scouting.grupo_miembro m ON m.grupo_id = g.id
          WHERE m.usuario_id = @usuario_id AND g.activo = 1),
        (SELECT max_exploraciones_activas FROM scouting.configuracion WHERE id = 1));
END;
GO

-- Usado por el flujo "Scouting - Planificador": encola las programadas vencidas y toma un lote de la cola.
CREATE OR ALTER PROCEDURE scouting.sp_tomar_lote
    @lote      int = 10,
    @instancia nvarchar(200) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @ahora datetime2(0) = SYSUTCDATETIME();
    DECLARE @zona nvarchar(64), @pie nvarchar(2000), @max_intentos int;
    SELECT @zona = zona_horaria, @pie = pie_correo, @max_intentos = max_intentos
      FROM scouting.configuracion WHERE id = 1;

    DECLARE @vencidas TABLE (id bigint PRIMARY KEY, programada datetime2(0));
    DECLARE @tomadas TABLE (id bigint PRIMARY KEY);

    BEGIN TRAN;

    -- 1. Exploraciones activas vencidas: se encolan y se avanza su próxima ejecución.
    INSERT @vencidas (id, programada)
    SELECT e.id, e.proxima_ejecucion
      FROM scouting.exploracion e WITH (UPDLOCK, READPAST)
     WHERE e.activa = 1 AND e.proxima_ejecucion <= @ahora;

    INSERT scouting.ejecucion (exploracion_id, clave, modo, estado, programada_para, creada_en)
    SELECT v.id, CONCAT(v.id, '-', FORMAT(v.programada, 'yyyyMMddHH')), 0, 0, v.programada, @ahora
      FROM @vencidas v
     WHERE NOT EXISTS (SELECT 1 FROM scouting.ejecucion x
                        WHERE x.clave = CONCAT(v.id, '-', FORMAT(v.programada, 'yyyyMMddHH')));

    UPDATE e
       SET proxima_ejecucion = scouting.fn_proxima_ejecucion(e.frecuencia, e.dia_semana, e.dia_mes, e.hora, @ahora, v.programada, @zona)
      FROM scouting.exploracion e
      JOIN @vencidas v ON v.id = e.id;

    -- Activas sin programar (por ejemplo, cargadas por script).
    UPDATE scouting.exploracion
       SET proxima_ejecucion = scouting.fn_proxima_ejecucion(frecuencia, dia_semana, dia_mes, hora, @ahora, NULL, @zona)
     WHERE activa = 1 AND proxima_ejecucion IS NULL;

    -- 2. En curso con bloqueo vencido y sin intentos disponibles: se dan por fallidas.
    UPDATE scouting.ejecucion
       SET estado = 4, finalizada_en = @ahora, bloqueo_hasta = NULL,
           ultimo_error = CONCAT(COALESCE(ultimo_error + N' | ', N''), N'El flujo no informó resultado antes de vencer el bloqueo.')
     WHERE estado = 1 AND bloqueo_hasta < @ahora AND intentos >= @max_intentos;

    -- 3. Toma del lote: pendientes, reintentos vencidos y en curso con bloqueo vencido.
    UPDATE TOP (@lote) x
       SET estado = 1,
           intentos = x.intentos + 1,
           iniciada_en = @ahora,
           bloqueo_hasta = DATEADD(minute, 30, @ahora),
           run_id_flujo = @instancia
    OUTPUT inserted.id INTO @tomadas (id)
      FROM scouting.ejecucion x WITH (UPDLOCK, READPAST, ROWLOCK)
     WHERE x.estado = 0
        OR (x.estado = 2 AND x.proximo_intento <= @ahora)
        OR (x.estado = 1 AND x.bloqueo_hasta < @ahora);

    COMMIT;

    SELECT x.id              AS ejecucion_id,
           x.modo            AS modo,
           x.intentos        AS intentos,
           e.id              AS exploracion_id,
           e.titulo          AS titulo,
           e.prompt          AS prompt,
           x.informe         AS informe,
           u.email           AS creador_email,
           u.nombre          AS creador_nombre,
           COALESCE(p.participantes, N'') AS participantes,
           COALESCE(@pie, N'') AS pie_correo
      FROM @tomadas t
      JOIN scouting.ejecucion x   ON x.id = t.id
      JOIN scouting.exploracion e ON e.id = x.exploracion_id
      JOIN scouting.usuario u     ON u.id = e.propietario_id
     OUTER APPLY (
        SELECT STRING_AGG(q.correo, N';') AS participantes
          FROM (SELECT TOP (scouting.fn_max_participantes(e.propietario_id)) pa.correo
                  FROM scouting.participante pa
                 WHERE pa.exploracion_id = e.id AND pa.activo = 1
                 ORDER BY pa.id) q
     ) p;
END;
GO

-- Guarda el informe generado por el agente para no volver a consumir créditos en un reintento.
CREATE OR ALTER PROCEDURE scouting.sp_guardar_informe
    @ejecucion_id bigint,
    @informe      nvarchar(max)
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE scouting.ejecucion SET informe = @informe WHERE id = @ejecucion_id;
    SELECT @@ROWCOUNT AS filas;
END;
GO

-- Registra el resultado de una ejecución. Siempre devuelve una fila para que el flujo decida si avisa.
CREATE OR ALTER PROCEDURE scouting.sp_registrar_resultado
    @ejecucion_id  bigint,
    @ok            bit,
    @destinatarios int = NULL,
    @resumen       nvarchar(2000) = NULL,
    @error         nvarchar(4000) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @ahora datetime2(0) = SYSUTCDATETIME();
    DECLARE @max_intentos int, @fallas_para_desactivar int, @alertas nvarchar(320);
    SELECT @max_intentos = max_intentos, @fallas_para_desactivar = fallas_para_desactivar, @alertas = correo_alertas_ti
      FROM scouting.configuracion WHERE id = 1;

    DECLARE @modo tinyint, @intentos int, @exploracion_id bigint;
    DECLARE @estado_final tinyint, @notificar bit = 0, @desactivada bit = 0;

    BEGIN TRAN;

    SELECT @modo = modo, @intentos = intentos, @exploracion_id = exploracion_id
      FROM scouting.ejecucion WITH (UPDLOCK, ROWLOCK)
     WHERE id = @ejecucion_id;

    IF @exploracion_id IS NULL
    BEGIN
        ROLLBACK;
        THROW 50001, N'La ejecución no existe.', 1;
    END

    IF @ok = 1
    BEGIN
        SET @estado_final = 3;
        UPDATE scouting.ejecucion
           SET estado = 3, finalizada_en = @ahora, bloqueo_hasta = NULL, proximo_intento = NULL,
               destinatarios = @destinatarios, resumen = @resumen, ultimo_error = NULL
         WHERE id = @ejecucion_id;

        IF @modo <> 1
            UPDATE scouting.exploracion
               SET ultima_ejecucion_ok = @ahora, ultimo_resumen = @resumen, fallas_consecutivas = 0
             WHERE id = @exploracion_id;
    END
    ELSE IF @intentos < @max_intentos
    BEGIN
        SET @estado_final = 2;
        UPDATE scouting.ejecucion
           SET estado = 2, bloqueo_hasta = NULL, ultimo_error = @error,
               proximo_intento = DATEADD(minute, CASE WHEN @intentos <= 1 THEN 15 ELSE 60 END, @ahora)
         WHERE id = @ejecucion_id;
    END
    ELSE
    BEGIN
        SET @estado_final = 4;
        SET @notificar = 1;
        UPDATE scouting.ejecucion
           SET estado = 4, finalizada_en = @ahora, bloqueo_hasta = NULL, proximo_intento = NULL, ultimo_error = @error
         WHERE id = @ejecucion_id;

        IF @modo <> 1
        BEGIN
            UPDATE scouting.exploracion
               SET fallas_consecutivas = fallas_consecutivas + 1,
                   activa = CASE WHEN fallas_consecutivas + 1 >= @fallas_para_desactivar THEN 0 ELSE activa END,
                   proxima_ejecucion = CASE WHEN fallas_consecutivas + 1 >= @fallas_para_desactivar THEN NULL ELSE proxima_ejecucion END
             WHERE id = @exploracion_id;

            SELECT @desactivada = CASE WHEN activa = 0 THEN 1 ELSE 0 END
              FROM scouting.exploracion WHERE id = @exploracion_id;
        END
    END

    COMMIT;

    SELECT @estado_final        AS estado,
           @notificar           AS notificar,
           @desactivada         AS desactivada,
           e.titulo             AS titulo,
           u.email              AS creador_email,
           u.nombre             AS creador_nombre,
           COALESCE(@alertas, N'') AS correo_alertas_ti
      FROM scouting.exploracion e
      JOIN scouting.usuario u ON u.id = e.propietario_id
     WHERE e.id = @exploracion_id;
END;
GO
