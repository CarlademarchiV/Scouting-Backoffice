package uy.edu.ucu.scouting.config;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param tenantId            tenant de Entra de UCU; si está, se rechazan tokens de otro tenant
 * @param adminsIniciales     UPNs separados por coma con rol administrador sin pertenecer a un grupo admin
 * @param zonaHoraria         zona IANA para mostrar fechas
 * @param minutosPlanificador cada cuántos minutos corre el flujo planificador (solo para mensajes)
 */
@ConfigurationProperties(prefix = "scouting")
public record ScoutingProperties(String tenantId, String adminsIniciales, String zonaHoraria, int minutosPlanificador) {

    public Set<String> admins() {
        if (adminsIniciales == null || adminsIniciales.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(adminsIniciales.split("[,;\\s]+"))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public ZoneId zona() {
        return ZoneId.of(zonaHoraria == null || zonaHoraria.isBlank() ? "America/Montevideo" : zonaHoraria);
    }
}
