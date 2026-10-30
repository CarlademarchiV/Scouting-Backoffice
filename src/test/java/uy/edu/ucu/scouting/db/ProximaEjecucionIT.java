package uy.edu.ucu.scouting.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * "Ahora" fijo: jueves 29/10/2026 09:00 en Montevideo (12:00 UTC). Montevideo es UTC-3 sin horario de verano.
 */
class ProximaEjecucionIT extends BaseSqlIT {

    @ParameterizedTest(name = "{0}")
    @CsvSource(nullValues = "null", value = {
            "diaria antes de la hora,      0, null, null, 10, null,                2026-10-29T13:00",
            "diaria hora ya cumplida,      0, null, null,  9, null,                2026-10-30T12:00",
            "semanal lunes,                1,    1, null,  9, null,                2026-11-02T12:00",
            "semanal hoy hora cumplida,    1,    4, null,  9, null,                2026-11-05T12:00",
            "semanal hoy más tarde,        1,    4, null, 18, null,                2026-10-29T21:00",
            "quincenal sin anterior,       2,    5, null,  9, null,                2026-10-30T12:00",
            "quincenal con anterior,       2,    4, null,  9, 2026-10-15T12:00:00, 2026-11-12T12:00",
            "quincenal anterior lejana,    2,    4, null,  9, 2026-01-01T12:00:00, 2026-11-05T12:00",
            "mensual día ya pasado,        3, null,   28,  9, null,                2026-11-28T12:00",
            "mensual día por venir,        3, null,    5,  9, null,                2026-11-05T12:00",
            "mensual noche cruza día UTC,  3, null,   28, 23, null,                2026-11-29T02:00"
    })
    void calculaLaProxima(String caso, int frecuencia, Integer diaSemana, Integer diaMes, int hora,
                          String anterior, String esperada) throws Exception {
        try (Connection c = conexion();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT scouting.fn_proxima_ejecucion(?, ?, ?, ?, '2026-10-29T12:00:00', ?, 'Montevideo Standard Time')")) {
            ps.setInt(1, frecuencia);
            ps.setObject(2, diaSemana);
            ps.setObject(3, diaMes);
            ps.setInt(4, hora);
            ps.setObject(5, anterior == null ? null : LocalDateTime.parse(anterior));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertThat(rs.getObject(1, LocalDateTime.class)).isEqualTo(LocalDateTime.parse(esperada));
            }
        }
    }
}
