package uy.edu.ucu.scouting.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "exploracion")
@Getter
@Setter
public class Exploracion {

    public static final int MAX_PROMPT = 100_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private String prompt;

    @Enumerated(EnumType.ORDINAL)
    private Frecuencia frecuencia = Frecuencia.SEMANAL;

    /** 1 = lunes .. 7 = domingo. */
    private Integer diaSemana;
    /** 1 .. 28. */
    private Integer diaMes;
    /** 0 .. 23, hora de Montevideo. */
    private Integer hora = 9;

    private boolean activa = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "propietario_id")
    private Usuario propietario;

    private Instant proximaEjecucion;
    private Instant ultimaEjecucionOk;
    private String ultimoResumen;
    private int fallasConsecutivas;

    @Version
    private Integer version;

    @OneToMany(mappedBy = "exploracion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Participante> participantes = new ArrayList<>();

    private Instant creadoEn = Instant.now();
    private String creadoPor;
    private Instant modificadoEn;
    private String modificadoPor;

    public long participantesActivos() {
        return participantes.stream().filter(Participante::isActivo).count();
    }
}
