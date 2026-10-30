-- Agentes de Scouting - esquema de datos
-- Fechas en UTC (datetime2). Códigos: ver comentarios en cada columna.

CREATE SCHEMA scouting;
GO

CREATE TABLE scouting.usuario (
    id              bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_usuario PRIMARY KEY,
    entra_oid       nvarchar(36)  NULL,
    upn             nvarchar(320) NOT NULL,
    nombre          nvarchar(200) NOT NULL,
    email           nvarchar(320) NOT NULL,
    activo          bit           NOT NULL CONSTRAINT df_usuario_activo DEFAULT (1),
    ultimo_acceso   datetime2(0)  NULL,
    creado_en       datetime2(0)  NOT NULL CONSTRAINT df_usuario_creado DEFAULT (SYSUTCDATETIME())
);
CREATE UNIQUE INDEX ux_usuario_upn ON scouting.usuario (upn);
CREATE UNIQUE INDEX ux_usuario_oid ON scouting.usuario (entra_oid) WHERE entra_oid IS NOT NULL;

CREATE TABLE scouting.grupo (
    id                          bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_grupo PRIMARY KEY,
    nombre                      nvarchar(100) NOT NULL,
    descripcion                 nvarchar(500) NULL,
    es_admin                    bit           NOT NULL CONSTRAINT df_grupo_admin DEFAULT (0),
    max_participantes           int           NULL,  -- null = usa el global
    max_exploraciones_activas   int           NULL,  -- null = usa el global
    activo                      bit           NOT NULL CONSTRAINT df_grupo_activo DEFAULT (1),
    creado_en                   datetime2(0)  NOT NULL CONSTRAINT df_grupo_creado DEFAULT (SYSUTCDATETIME()),
    creado_por                  nvarchar(320) NULL,
    modificado_en               datetime2(0)  NULL,
    modificado_por              nvarchar(320) NULL,
    CONSTRAINT ck_grupo_topes CHECK ((max_participantes IS NULL OR max_participantes > 0)
                                 AND (max_exploraciones_activas IS NULL OR max_exploraciones_activas > 0))
);
CREATE UNIQUE INDEX ux_grupo_nombre ON scouting.grupo (nombre);

CREATE TABLE scouting.grupo_miembro (
    grupo_id    bigint NOT NULL CONSTRAINT fk_gm_grupo REFERENCES scouting.grupo (id) ON DELETE CASCADE,
    usuario_id  bigint NOT NULL CONSTRAINT fk_gm_usuario REFERENCES scouting.usuario (id) ON DELETE CASCADE,
    CONSTRAINT pk_grupo_miembro PRIMARY KEY (grupo_id, usuario_id)
);
CREATE INDEX ix_gm_usuario ON scouting.grupo_miembro (usuario_id);

CREATE TABLE scouting.configuracion (
    id                          int           NOT NULL CONSTRAINT pk_configuracion PRIMARY KEY,
    max_participantes           int           NOT NULL,
    max_exploraciones_activas   int           NOT NULL,
    dominios_permitidos         nvarchar(500) NOT NULL,  -- separados por ; o ,
    max_intentos                int           NOT NULL,
    fallas_para_desactivar      int           NOT NULL,
    zona_horaria                nvarchar(64)  NOT NULL,  -- nombre Windows, para AT TIME ZONE
    correo_alertas_ti           nvarchar(320) NULL,
    pie_correo                  nvarchar(2000) NULL,
    modificado_en               datetime2(0)  NULL,
    modificado_por              nvarchar(320) NULL,
    CONSTRAINT ck_configuracion_unica CHECK (id = 1),
    CONSTRAINT ck_configuracion_valores CHECK (max_participantes > 0 AND max_exploraciones_activas > 0
                                           AND max_intentos BETWEEN 1 AND 10 AND fallas_para_desactivar > 0)
);

CREATE TABLE scouting.exploracion (
    id                      bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_exploracion PRIMARY KEY,
    titulo                  nvarchar(200) NOT NULL,
    prompt                  nvarchar(max) NOT NULL,
    frecuencia              tinyint       NOT NULL,  -- 0 Diaria, 1 Semanal, 2 Quincenal, 3 Mensual
    dia_semana              tinyint       NULL,      -- 1 lunes .. 7 domingo
    dia_mes                 tinyint       NULL,      -- 1 .. 28
    hora                    tinyint       NOT NULL,  -- 0 .. 23, hora de Montevideo
    activa                  bit           NOT NULL CONSTRAINT df_exploracion_activa DEFAULT (1),
    propietario_id          bigint        NOT NULL CONSTRAINT fk_exploracion_propietario REFERENCES scouting.usuario (id),
    proxima_ejecucion       datetime2(0)  NULL,
    ultima_ejecucion_ok     datetime2(0)  NULL,
    ultimo_resumen          nvarchar(2000) NULL,
    fallas_consecutivas     int           NOT NULL CONSTRAINT df_exploracion_fallas DEFAULT (0),
    version                 int           NOT NULL CONSTRAINT df_exploracion_version DEFAULT (0),
    creado_en               datetime2(0)  NOT NULL CONSTRAINT df_exploracion_creado DEFAULT (SYSUTCDATETIME()),
    creado_por              nvarchar(320) NULL,
    modificado_en           datetime2(0)  NULL,
    modificado_por          nvarchar(320) NULL,
    CONSTRAINT ck_exploracion_prompt CHECK (LEN(prompt) <= 100000),
    CONSTRAINT ck_exploracion_frecuencia CHECK (frecuencia BETWEEN 0 AND 3),
    CONSTRAINT ck_exploracion_hora CHECK (hora BETWEEN 0 AND 23),
    CONSTRAINT ck_exploracion_dia_semana CHECK (dia_semana IS NULL OR dia_semana BETWEEN 1 AND 7),
    CONSTRAINT ck_exploracion_dia_mes CHECK (dia_mes IS NULL OR dia_mes BETWEEN 1 AND 28),
    CONSTRAINT ck_exploracion_dias CHECK (
        (frecuencia = 0)
     OR (frecuencia IN (1, 2) AND dia_semana IS NOT NULL)
     OR (frecuencia = 3 AND dia_mes IS NOT NULL))
);
CREATE INDEX ix_exploracion_vencidas ON scouting.exploracion (activa, proxima_ejecucion);
CREATE INDEX ix_exploracion_propietario ON scouting.exploracion (propietario_id, activa);

CREATE TABLE scouting.participante (
    id              bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_participante PRIMARY KEY,
    exploracion_id  bigint        NOT NULL CONSTRAINT fk_participante_exploracion
                                  REFERENCES scouting.exploracion (id) ON DELETE CASCADE,
    correo          nvarchar(320) NOT NULL,
    nombre          nvarchar(200) NULL,
    activo          bit           NOT NULL CONSTRAINT df_participante_activo DEFAULT (1),
    creado_en       datetime2(0)  NOT NULL CONSTRAINT df_participante_creado DEFAULT (SYSUTCDATETIME()),
    creado_por      nvarchar(320) NULL
);
CREATE UNIQUE INDEX ux_participante_correo ON scouting.participante (exploracion_id, correo);

CREATE TABLE scouting.ejecucion (
    id                  bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_ejecucion PRIMARY KEY,
    exploracion_id      bigint        NOT NULL CONSTRAINT fk_ejecucion_exploracion
                                      REFERENCES scouting.exploracion (id) ON DELETE CASCADE,
    clave               nvarchar(100) NOT NULL,
    modo                tinyint       NOT NULL,  -- 0 Programada, 1 Prueba, 2 Manual
    estado              tinyint       NOT NULL,  -- 0 Pendiente, 1 En curso, 2 Reintentar, 3 Enviado, 4 Fallido, 5 Omitida
    programada_para     datetime2(0)  NULL,
    intentos            int           NOT NULL CONSTRAINT df_ejecucion_intentos DEFAULT (0),
    proximo_intento     datetime2(0)  NULL,
    bloqueo_hasta       datetime2(0)  NULL,
    iniciada_en         datetime2(0)  NULL,
    finalizada_en       datetime2(0)  NULL,
    creada_en           datetime2(0)  NOT NULL CONSTRAINT df_ejecucion_creada DEFAULT (SYSUTCDATETIME()),
    informe             nvarchar(max) NULL,
    resumen             nvarchar(2000) NULL,
    destinatarios       int           NULL,
    ultimo_error        nvarchar(4000) NULL,
    solicitada_por_id   bigint        NULL CONSTRAINT fk_ejecucion_solicitante REFERENCES scouting.usuario (id),
    run_id_flujo        nvarchar(200) NULL,
    CONSTRAINT ck_ejecucion_modo CHECK (modo BETWEEN 0 AND 2),
    CONSTRAINT ck_ejecucion_estado CHECK (estado BETWEEN 0 AND 5)
);
CREATE UNIQUE INDEX ux_ejecucion_clave ON scouting.ejecucion (clave);
CREATE INDEX ix_ejecucion_cola ON scouting.ejecucion (estado, proximo_intento) INCLUDE (bloqueo_hasta);
CREATE INDEX ix_ejecucion_historial ON scouting.ejecucion (exploracion_id, creada_en DESC);
GO
