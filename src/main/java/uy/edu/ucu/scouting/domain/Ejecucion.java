package uy.edu.ucu.scouting.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Cola e historial. La escriben el backoffice (pedidos manuales) y los procedimientos que usan los flujos. */
@Entity
@Table(name = "ejecucion")
@Getter
@Setter
public class Ejecucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exploracion_id")
    private Exploracion exploracion;

    private String clave;

    @Enumerated(EnumType.ORDINAL)
    private ModoEjecucion modo;

    @Enumerated(EnumType.ORDINAL)
    private EstadoEjecucion estado = EstadoEjecucion.PENDIENTE;

    private Instant programadaPara;
    private int intentos;
    private Instant proximoIntento;
    private Instant bloqueoHasta;
    private Instant iniciadaEn;
    private Instant finalizadaEn;
    private Instant creadaEn = Instant.now();
    private String informe;
    private String resumen;
    private Integer destinatarios;
    private String ultimoError;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitada_por_id")
    private Usuario solicitadaPor;

    private String runIdFlujo;
}
