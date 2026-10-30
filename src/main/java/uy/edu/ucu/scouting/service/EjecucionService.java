package uy.edu.ucu.scouting.service;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.ucu.scouting.domain.Ejecucion;
import uy.edu.ucu.scouting.domain.EstadoEjecucion;
import uy.edu.ucu.scouting.repository.EjecucionRepository;
import uy.edu.ucu.scouting.security.ScoutingUser;

@Service
@RequiredArgsConstructor
public class EjecucionService {

    private static final int HISTORIAL = 20;
    private static final int MONITOR = 200;

    private final EjecucionRepository ejecuciones;
    private final ExploracionService exploracionService;

    @Transactional(readOnly = true)
    public Ejecucion obtener(Long id, ScoutingUser u) {
        Ejecucion x = ejecuciones.conDetalle(id).orElseThrow(NoEncontradoException::new);
        exploracionService.verificarAcceso(x.getExploracion(), u);
        return x;
    }

    @Transactional(readOnly = true)
    public List<Ejecucion> historial(Long exploracionId) {
        return ejecuciones.findByExploracionIdOrderByCreadaEnDesc(exploracionId, PageRequest.of(0, HISTORIAL));
    }

    @Transactional(readOnly = true)
    public List<Ejecucion> monitor(EstadoEjecucion estado, String propietario, Instant desde, Instant hasta) {
        String filtro = propietario == null || propietario.isBlank() ? null : propietario.trim();
        return ejecuciones.monitor(estado, filtro, desde, hasta, PageRequest.of(0, MONITOR));
    }

    /** Vuelve a encolar una ejecución fallida con los intentos en cero. */
    @Transactional
    public void reintentar(Long id) {
        Ejecucion x = ejecuciones.findById(id).orElseThrow(NoEncontradoException::new);
        if (x.getEstado() != EstadoEjecucion.FALLIDO) {
            throw new ReglaNegocioException("Solo se pueden reintentar ejecuciones fallidas.");
        }
        x.setEstado(EstadoEjecucion.PENDIENTE);
        x.setIntentos(0);
        x.setProximoIntento(null);
        x.setBloqueoHasta(null);
        x.setFinalizadaEn(null);
    }
}
