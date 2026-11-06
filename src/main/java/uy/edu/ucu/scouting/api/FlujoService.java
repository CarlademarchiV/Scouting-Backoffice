package uy.edu.ucu.scouting.api;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import uy.edu.ucu.scouting.api.FlujoDtos.EjecucionTomada;
import uy.edu.ucu.scouting.api.FlujoDtos.Registro;
import uy.edu.ucu.scouting.api.FlujoDtos.Resultado;
import uy.edu.ucu.scouting.service.NoEncontradoException;

/**
 * Cola de ejecuciones para Power Automate. Envuelve los procedimientos de SQL Server, que siguen
 * siendo la única fuente de las reglas (toma con bloqueo, reintentos, fallas y desactivación).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlujoService {

    static final int LOTE_POR_DEFECTO = 10;
    private static final int MAX_RESUMEN = 2000;
    private static final int MAX_ERROR = 4000;
    private static final String[] MODOS = {"programada", "prueba", "manual"};
    private static final String[] ESTADOS = {"pendiente", "en-curso", "reintentar", "enviado", "fallido", "omitida"};

    private final JdbcTemplate jdbc;

    public List<EjecucionTomada> tomarLote(Integer lote, String instancia) {
        int n = lote == null ? LOTE_POR_DEFECTO : lote;
        List<EjecucionTomada> tomadas = jdbc.query("EXEC scouting.sp_tomar_lote @lote = ?, @instancia = ?",
                new Object[] {n, instancia}, new int[] {Types.INTEGER, Types.NVARCHAR}, (rs, i) -> ejecucion(rs));
        if (!tomadas.isEmpty()) {
            log.info("Lote tomado por {}: {} ejecuciones", instancia, tomadas.size());
        }
        return tomadas;
    }

    public void guardarInforme(long ejecucionId, String informe) {
        Integer filas = jdbc.queryForObject("EXEC scouting.sp_guardar_informe @ejecucion_id = ?, @informe = ?",
                new Object[] {ejecucionId, informe}, new int[] {Types.BIGINT, Types.NVARCHAR}, Integer.class);
        if (filas == null || filas == 0) {
            throw new NoEncontradoException();
        }
    }

    public Registro registrarResultado(long ejecucionId, Resultado r) {
        existe(ejecucionId);
        Registro registro = jdbc.queryForObject(
                "EXEC scouting.sp_registrar_resultado @ejecucion_id = ?, @ok = ?, @destinatarios = ?, @resumen = ?, @error = ?",
                new Object[] {ejecucionId, r.ok(), r.destinatarios(), recortar(r.resumen(), MAX_RESUMEN), recortar(r.error(), MAX_ERROR)},
                new int[] {Types.BIGINT, Types.BIT, Types.INTEGER, Types.NVARCHAR, Types.NVARCHAR},
                (rs, i) -> registro(rs));
        if (registro != null && !"enviado".equals(registro.estado())) {
            log.warn("Ejecución {} quedó {}: {}", ejecucionId, registro.estado(), recortar(r.error(), 300));
        }
        return registro;
    }

    private void existe(long ejecucionId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM scouting.ejecucion WHERE id = ?", Integer.class, ejecucionId);
        if (n == null || n == 0) {
            throw new NoEncontradoException();
        }
    }

    private static EjecucionTomada ejecucion(ResultSet rs) throws SQLException {
        return new EjecucionTomada(
                rs.getLong("ejecucion_id"),
                MODOS[rs.getInt("modo")],
                rs.getInt("intentos"),
                rs.getLong("exploracion_id"),
                rs.getString("titulo"),
                rs.getString("prompt"),
                rs.getString("informe"),
                rs.getString("creador_email"),
                rs.getString("creador_nombre"),
                lista(rs.getString("participantes")),
                rs.getString("pie_correo"));
    }

    private static Registro registro(ResultSet rs) throws SQLException {
        return new Registro(
                ESTADOS[rs.getInt("estado")],
                rs.getBoolean("notificar"),
                rs.getBoolean("desactivada"),
                rs.getString("titulo"),
                rs.getString("creador_email"),
                rs.getString("creador_nombre"),
                lista(rs.getString("correo_alertas_ti")));
    }

    static List<String> lista(String valor) {
        if (valor == null || valor.isBlank()) {
            return List.of();
        }
        return Arrays.stream(valor.split("[;,]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    static String recortar(String texto, int max) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
