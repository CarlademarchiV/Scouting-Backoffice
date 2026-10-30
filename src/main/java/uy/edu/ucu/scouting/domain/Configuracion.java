package uy.edu.ucu.scouting.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.Getter;
import lombok.Setter;

/** Fila única (id = 1) con los valores globales. */
@Entity
@Table(name = "configuracion")
@Getter
@Setter
public class Configuracion {

    public static final int ID = 1;

    @Id
    private Integer id = ID;

    private int maxParticipantes;
    private int maxExploracionesActivas;
    private String dominiosPermitidos;
    private int maxIntentos;
    private int fallasParaDesactivar;
    private String zonaHoraria;
    private String correoAlertasTi;
    private String pieCorreo;
    private Instant modificadoEn;
    private String modificadoPor;

    /** Dominios normalizados, sin @ y en minúsculas. */
    public List<String> dominios() {
        if (dominiosPermitidos == null || dominiosPermitidos.isBlank()) {
            return List.of();
        }
        return Arrays.stream(dominiosPermitidos.split("[;,\\s]+"))
                .map(d -> d.trim().toLowerCase(Locale.ROOT).replaceFirst("^@", ""))
                .filter(d -> !d.isEmpty())
                .toList();
    }
}
