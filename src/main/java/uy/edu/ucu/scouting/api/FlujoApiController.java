package uy.edu.ucu.scouting.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uy.edu.ucu.scouting.api.FlujoDtos.Informe;
import uy.edu.ucu.scouting.api.FlujoDtos.Lote;
import uy.edu.ucu.scouting.api.FlujoDtos.PedidoLote;
import uy.edu.ucu.scouting.api.FlujoDtos.Registro;
import uy.edu.ucu.scouting.api.FlujoDtos.Resultado;

/**
 * API que consume el flujo "Scouting - Origen". Reemplaza el acceso directo a SQL Server vía gateway.
 * Requiere el encabezado X-Scouting-Clave (ver ApiSecurityConfig).
 */
@Validated
@RestController
@RequestMapping("/api/flujo")
@RequiredArgsConstructor
public class FlujoApiController {

    private final FlujoService flujo;

    /** Encola las programadas vencidas y toma un lote de la cola (bloqueo de 30 min). */
    @PostMapping("/lote")
    public Lote tomarLote(@Valid @RequestBody(required = false) PedidoLote pedido) {
        PedidoLote p = pedido == null ? new PedidoLote(null, null) : pedido;
        return new Lote(flujo.tomarLote(p.lote(), p.instancia()));
    }

    /** Guarda la respuesta del agente para no volver a consumir créditos en un reintento. */
    @PutMapping("/ejecuciones/{id}/informe")
    public ResponseEntity<Void> guardarInforme(@PathVariable long id, @Valid @RequestBody Informe informe) {
        flujo.guardarInforme(id, informe.informe());
        return ResponseEntity.noContent().build();
    }

    /** Registra el resultado: enviado, reintento diferido o falla definitiva. */
    @PostMapping("/ejecuciones/{id}/resultado")
    public Registro registrarResultado(@PathVariable long id, @Valid @RequestBody Resultado resultado) {
        return flujo.registrarResultado(id, resultado);
    }
}
