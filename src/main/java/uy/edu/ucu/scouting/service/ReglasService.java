package uy.edu.ucu.scouting.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import uy.edu.ucu.scouting.domain.Exploracion;

/**
 * Reglas que viven en SQL Server para que la app y los flujos usen exactamente la misma lógica:
 * topes por usuario y cálculo de la próxima ejecución.
 */
@Service
@RequiredArgsConstructor
public class ReglasService {

    private final JdbcTemplate jdbc;

    public int maxParticipantes(Long usuarioId) {
        Integer v = jdbc.queryForObject("SELECT scouting.fn_max_participantes(?)", Integer.class, usuarioId);
        return v == null ? 0 : v;
    }

    public int maxExploracionesActivas(Long usuarioId) {
        Integer v = jdbc.queryForObject("SELECT scouting.fn_max_exploraciones_activas(?)", Integer.class, usuarioId);
        return v == null ? 0 : v;
    }

    /** Próxima ejecución desde ahora, sin corrida anterior. */
    public Instant proximaEjecucion(Exploracion e) {
        LocalDateTime utc = jdbc.queryForObject(
                "SELECT scouting.fn_proxima_ejecucion(?, ?, ?, ?, SYSUTCDATETIME(), NULL, "
                        + "(SELECT zona_horaria FROM scouting.configuracion WHERE id = 1))",
                LocalDateTime.class,
                e.getFrecuencia().ordinal(),
                // La función ignora el día que no corresponde a la frecuencia; se evita enviar nulos.
                e.getDiaSemana() == null ? 1 : e.getDiaSemana(),
                e.getDiaMes() == null ? 1 : e.getDiaMes(),
                e.getHora());
        return utc == null ? null : utc.toInstant(ZoneOffset.UTC);
    }
}
