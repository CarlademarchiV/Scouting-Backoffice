package uy.edu.ucu.scouting.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Contrato de los procedimientos que usan los flujos de Power Automate. */
class ColaEjecucionesIT extends BaseSqlIT {

    private long exploracionId;

    @BeforeEach
    void datos() throws Exception {
        try (Connection c = conexion(); Statement s = c.createStatement()) {
            s.execute("DELETE FROM scouting.ejecucion; DELETE FROM scouting.exploracion; DELETE FROM scouting.usuario;");
            s.execute("INSERT scouting.usuario (upn, nombre, email) VALUES ('ana@ucu.edu.uy', 'Ana', 'ana@ucu.edu.uy')");
            s.execute("INSERT scouting.exploracion (titulo, prompt, frecuencia, dia_semana, hora, activa, propietario_id, proxima_ejecucion) "
                    + "SELECT 'Tendencias', 'Buscá novedades', 1, 1, 9, 1, id, DATEADD(minute, -5, SYSUTCDATETIME()) FROM scouting.usuario");
            try (ResultSet rs = s.executeQuery("SELECT id FROM scouting.exploracion")) {
                rs.next();
                exploracionId = rs.getLong(1);
            }
            s.execute("INSERT scouting.participante (exploracion_id, correo) VALUES "
                    + "(" + exploracionId + ", 'p1@ucu.edu.uy'), (" + exploracionId + ", 'p2@ucu.edu.uy')");
        }
    }

    @Test
    void encolaLaVencidaYLaEntregaConParticipantes() throws Exception {
        try (Connection c = conexion()) {
            try (ResultSet rs = tomarLote(c)) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt("modo")).isZero();
                assertThat(rs.getInt("intentos")).isEqualTo(1);
                assertThat(rs.getString("participantes")).isEqualTo("p1@ucu.edu.uy;p2@ucu.edu.uy");
                assertThat(rs.getString("creador_email")).isEqualTo("ana@ucu.edu.uy");
                assertThat(rs.next()).isFalse();
            }
            // La próxima ejecución avanzó: un segundo ciclo no encola ni entrega nada.
            try (ResultSet rs = tomarLote(c)) {
                assertThat(rs.next()).isFalse();
            }
            assertThat(escalar(c, "SELECT CASE WHEN proxima_ejecucion > SYSUTCDATETIME() THEN 1 ELSE 0 END FROM scouting.exploracion"))
                    .isEqualTo(1);
        }
    }

    @Test
    void respetaElTopeDeParticipantes() throws Exception {
        try (Connection c = conexion(); Statement s = c.createStatement()) {
            s.execute("UPDATE scouting.configuracion SET max_participantes = 1");
            try (ResultSet rs = tomarLote(c)) {
                rs.next();
                assertThat(rs.getString("participantes")).isEqualTo("p1@ucu.edu.uy");
            } finally {
                s.execute("UPDATE scouting.configuracion SET max_participantes = 50");
            }
        }
    }

    @Test
    void exitoActualizaLaExploracion() throws Exception {
        try (Connection c = conexion()) {
            long id = tomarUna(c);
            try (CallableStatement cs = c.prepareCall("{call scouting.sp_guardar_informe(?, ?)}")) {
                cs.setLong(1, id);
                cs.setString(2, "Informe");
                cs.execute();
            }
            try (ResultSet rs = registrar(c, id, true, null)) {
                rs.next();
                assertThat(rs.getInt("estado")).isEqualTo(3);
                assertThat(rs.getBoolean("notificar")).isFalse();
            }
            assertThat(escalar(c, "SELECT COUNT(*) FROM scouting.exploracion WHERE ultima_ejecucion_ok IS NOT NULL AND ultimo_resumen = 'Resumen'"))
                    .isEqualTo(1);
        }
    }

    @Test
    void reintentaYLuegoFallaYAvisa() throws Exception {
        try (Connection c = conexion(); Statement s = c.createStatement()) {
            long id = tomarUna(c);
            for (int intento = 1; intento <= 3; intento++) {
                try (ResultSet rs = registrar(c, id, false, "Timeout del agente")) {
                    rs.next();
                    if (intento < 3) {
                        assertThat(rs.getInt("estado")).isEqualTo(2);
                        // Se adelanta el reintento para no esperar 15/60 minutos.
                        s.execute("UPDATE scouting.ejecucion SET proximo_intento = DATEADD(minute, -1, SYSUTCDATETIME())");
                        try (ResultSet lote = tomarLote(c)) {
                            assertThat(lote.next()).isTrue();
                            assertThat(lote.getInt("intentos")).isEqualTo(intento + 1);
                        }
                    } else {
                        assertThat(rs.getInt("estado")).isEqualTo(4);
                        assertThat(rs.getBoolean("notificar")).isTrue();
                        assertThat(rs.getString("creador_email")).isEqualTo("ana@ucu.edu.uy");
                    }
                }
            }
            assertThat(escalar(c, "SELECT fallas_consecutivas FROM scouting.exploracion")).isEqualTo(1);
        }
    }

    @Test
    void pruebaManualNoCambiaLaProgramacion() throws Exception {
        try (Connection c = conexion(); Statement s = c.createStatement()) {
            s.execute("UPDATE scouting.exploracion SET proxima_ejecucion = DATEADD(day, 3, SYSUTCDATETIME())");
            s.execute("INSERT scouting.ejecucion (exploracion_id, clave, modo, estado) VALUES (" + exploracionId + ", 'x-prueba-1', 1, 0)");
            long id;
            try (ResultSet rs = tomarLote(c)) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt("modo")).isEqualTo(1);
                id = rs.getLong("ejecucion_id");
            }
            registrar(c, id, true, null).close();
            assertThat(escalar(c, "SELECT COUNT(*) FROM scouting.exploracion WHERE ultima_ejecucion_ok IS NULL")).isEqualTo(1);
        }
    }

    private static ResultSet tomarLote(Connection c) throws Exception {
        CallableStatement cs = c.prepareCall("{call scouting.sp_tomar_lote(?, ?)}");
        cs.setInt(1, 10);
        cs.setString(2, "prueba");
        return cs.executeQuery();
    }

    private static long tomarUna(Connection c) throws Exception {
        try (ResultSet rs = tomarLote(c)) {
            rs.next();
            return rs.getLong("ejecucion_id");
        }
    }

    private static ResultSet registrar(Connection c, long id, boolean ok, String error) throws Exception {
        CallableStatement cs = c.prepareCall("{call scouting.sp_registrar_resultado(?, ?, ?, ?, ?)}");
        cs.setLong(1, id);
        cs.setBoolean(2, ok);
        cs.setInt(3, 2);
        cs.setString(4, ok ? "Resumen" : null);
        cs.setString(5, error);
        return cs.executeQuery();
    }

    private static int escalar(Connection c, String sql) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
