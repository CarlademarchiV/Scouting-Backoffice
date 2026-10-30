INSERT scouting.configuracion
    (id, max_participantes, max_exploraciones_activas, dominios_permitidos, max_intentos,
     fallas_para_desactivar, zona_horaria, correo_alertas_ti, pie_correo)
VALUES
    (1, 50, 10, N'ucu.edu.uy', 3, 3, N'Montevideo Standard Time', NULL,
     N'Recibís este correo porque fuiste agregado como participante de una exploración de Scouting de la UCU. Para dejar de recibirlo, respondé a este correo.');

INSERT scouting.grupo (nombre, descripcion, es_admin, creado_por)
VALUES (N'Administradores Scouting', N'Administran grupos, configuración y monitor', 1, N'sistema'),
       (N'Usuarios Scouting', N'Usuarios habilitados a crear exploraciones', 0, N'sistema');
