package uy.edu.ucu.scouting.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Contrato JSON de la API que consume el flujo "Scouting - Origen" de Power Automate. */
public final class FlujoDtos {

    private FlujoDtos() {
    }

    /** Pedido de un lote. Sin cuerpo se toman hasta 10 ejecuciones. */
    public record PedidoLote(
            @Min(1) @Max(50) Integer lote,
            @Size(max = 200) String instancia) {
    }

    /** Una ejecución tomada de la cola, lista para convertirse en un Trabajo. */
    public record EjecucionTomada(
            long ejecucionId,
            String modo,
            int intentos,
            long exploracionId,
            String titulo,
            String prompt,
            String informe,
            String creadorEmail,
            String creadorNombre,
            List<String> participantes,
            String pieCorreo) {
    }

    public record Lote(List<EjecucionTomada> ejecuciones) {
    }

    public record Informe(@NotNull @Size(max = 1_000_000) String informe) {
    }

    public record Resultado(
            @NotNull Boolean ok,
            @PositiveOrZero Integer destinatarios,
            String resumen,
            String error) {
    }

    /** Lo que el origen necesita para decidir si avisa la falla. */
    public record Registro(
            String estado,
            boolean notificar,
            boolean desactivada,
            String titulo,
            String creadorEmail,
            String creadorNombre,
            List<String> correoAlertasTi) {
    }
}
