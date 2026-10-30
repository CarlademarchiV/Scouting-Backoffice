package uy.edu.ucu.scouting.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.ucu.scouting.domain.Ejecucion;
import uy.edu.ucu.scouting.domain.EstadoEjecucion;
import uy.edu.ucu.scouting.domain.Exploracion;
import uy.edu.ucu.scouting.domain.ModoEjecucion;
import uy.edu.ucu.scouting.domain.Participante;
import uy.edu.ucu.scouting.repository.EjecucionRepository;
import uy.edu.ucu.scouting.repository.ExploracionRepository;
import uy.edu.ucu.scouting.repository.UsuarioRepository;
import uy.edu.ucu.scouting.security.ScoutingUser;
import uy.edu.ucu.scouting.web.form.ExploracionForm;

@Service
@RequiredArgsConstructor
public class ExploracionService {

    private static final DateTimeFormatter CLAVE = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").withZone(ZoneOffset.UTC);

    private final ExploracionRepository exploraciones;
    private final UsuarioRepository usuarios;
    private final EjecucionRepository ejecuciones;
    private final ReglasService reglas;

    @Transactional(readOnly = true)
    public List<Exploracion> listar(ScoutingUser u, boolean todas) {
        return todas && u.isAdmin() ? exploraciones.todas() : exploraciones.delPropietario(u.getUsuarioId());
    }

    /** Exploración con propietario y participantes cargados, verificando que el usuario pueda verla. */
    @Transactional(readOnly = true)
    public Exploracion obtener(Long id, ScoutingUser u) {
        Exploracion e = exploraciones.conDetalle(id).orElseThrow(NoEncontradoException::new);
        verificarAcceso(e, u);
        return e;
    }

    @Transactional
    public Exploracion crear(ExploracionForm f, ScoutingUser u) {
        validar(f);
        Exploracion e = new Exploracion();
        e.setPropietario(usuarios.getReferenceById(u.getUsuarioId()));
        aplicar(f, e);
        if (e.isActiva()) {
            validarTopeActivas(u.getUsuarioId(), null);
            e.setProximaEjecucion(reglas.proximaEjecucion(e));
        }
        e.setCreadoPor(u.getUpn());
        return exploraciones.save(e);
    }

    @Transactional
    public Exploracion actualizar(Long id, ExploracionForm f, ScoutingUser u) {
        validar(f);
        Exploracion e = obtener(id, u);
        if (f.getVersion() != null && !f.getVersion().equals(e.getVersion())) {
            throw new ReglaNegocioException("Otra persona modificó esta exploración mientras la editabas. "
                    + "Recargá la página y volvé a aplicar tus cambios.");
        }
        boolean cambioProgramacion = e.getFrecuencia() != f.getFrecuencia()
                || !Objects.equals(e.getHora(), f.getHora())
                || (f.getFrecuencia().usaDiaSemana() && !Objects.equals(e.getDiaSemana(), f.getDiaSemana()))
                || (f.getFrecuencia().usaDiaMes() && !Objects.equals(e.getDiaMes(), f.getDiaMes()));
        boolean reactivada = !e.isActiva() && f.isActiva();

        aplicar(f, e);
        if (reactivada) {
            validarTopeActivas(e.getPropietario().getId(), e.getId());
            e.setFallasConsecutivas(0);
        }
        if (!e.isActiva()) {
            e.setProximaEjecucion(null);
        } else if (cambioProgramacion || reactivada || e.getProximaEjecucion() == null) {
            e.setProximaEjecucion(reglas.proximaEjecucion(e));
        }
        e.setModificadoEn(Instant.now());
        e.setModificadoPor(u.getUpn());
        return exploraciones.save(e);
    }

    /** Copia inactiva, con sus participantes, a nombre de quien la duplica. */
    @Transactional
    public Exploracion duplicar(Long id, ScoutingUser u) {
        Exploracion origen = obtener(id, u);
        Exploracion copia = new Exploracion();
        String titulo = "Copia de " + origen.getTitulo();
        copia.setTitulo(titulo.length() > 200 ? titulo.substring(0, 200) : titulo);
        copia.setPrompt(origen.getPrompt());
        copia.setFrecuencia(origen.getFrecuencia());
        copia.setDiaSemana(origen.getDiaSemana());
        copia.setDiaMes(origen.getDiaMes());
        copia.setHora(origen.getHora());
        copia.setActiva(false);
        copia.setPropietario(usuarios.getReferenceById(u.getUsuarioId()));
        copia.setCreadoPor(u.getUpn());
        for (Participante p : origen.getParticipantes()) {
            Participante n = new Participante();
            n.setExploracion(copia);
            n.setCorreo(p.getCorreo());
            n.setNombre(p.getNombre());
            n.setActivo(p.isActivo());
            n.setCreadoPor(u.getUpn());
            copia.getParticipantes().add(n);
        }
        return exploraciones.save(copia);
    }

    @Transactional
    public void eliminar(Long id, ScoutingUser u) {
        exploraciones.delete(obtener(id, u));
    }

    /** Encola una corrida manual; el flujo planificador la toma en su próximo ciclo. */
    @Transactional
    public Ejecucion solicitarEjecucion(Long id, ModoEjecucion modo, ScoutingUser u) {
        if (modo == ModoEjecucion.PROGRAMADA) {
            throw new IllegalArgumentException("Las corridas programadas las encola el planificador.");
        }
        Exploracion e = obtener(id, u);
        if (modo == ModoEjecucion.MANUAL && e.participantesActivos() == 0) {
            throw new ReglaNegocioException("Agregá al menos un participante activo antes de enviar.");
        }
        if (ejecuciones.existsByExploracionIdAndModoAndEstadoIn(id, modo,
                EnumSet.of(EstadoEjecucion.PENDIENTE, EstadoEjecucion.EN_CURSO, EstadoEjecucion.REINTENTAR))) {
            throw new ReglaNegocioException("Ya hay una solicitud de este tipo en proceso. "
                    + "Mirá su avance en el historial.");
        }
        Ejecucion x = new Ejecucion();
        x.setExploracion(e);
        x.setModo(modo);
        x.setEstado(EstadoEjecucion.PENDIENTE);
        x.setClave(e.getId() + "-" + (modo == ModoEjecucion.PRUEBA ? "prueba" : "manual") + "-"
                + CLAVE.format(Instant.now()));
        x.setSolicitadaPor(usuarios.getReferenceById(u.getUsuarioId()));
        return ejecuciones.save(x);
    }

    void verificarAcceso(Exploracion e, ScoutingUser u) {
        if (!u.isAdmin() && !e.getPropietario().getId().equals(u.getUsuarioId())) {
            throw new AccessDeniedException("La exploración pertenece a otro usuario.");
        }
    }

    private void validar(ExploracionForm f) {
        if (f.getFrecuencia().usaDiaSemana() && f.getDiaSemana() == null) {
            throw new ReglaNegocioException("Indicá el día de la semana para la frecuencia elegida.");
        }
        if (f.getFrecuencia().usaDiaMes() && f.getDiaMes() == null) {
            throw new ReglaNegocioException("Indicá el día del mes (1 a 28) para la frecuencia mensual.");
        }
    }

    private void validarTopeActivas(Long propietarioId, Long excluirId) {
        int tope = reglas.maxExploracionesActivas(propietarioId);
        if (exploraciones.contarActivas(propietarioId, excluirId) + 1 > tope) {
            throw new ReglaNegocioException("Se alcanzó el máximo de " + tope + " exploraciones activas por usuario. "
                    + "Desactivá otra exploración o pedí más cupo al administrador.");
        }
    }

    private static void aplicar(ExploracionForm f, Exploracion e) {
        e.setTitulo(f.getTitulo().trim());
        e.setPrompt(f.getPrompt());
        e.setFrecuencia(f.getFrecuencia());
        e.setDiaSemana(f.getFrecuencia().usaDiaSemana() ? f.getDiaSemana() : null);
        e.setDiaMes(f.getFrecuencia().usaDiaMes() ? f.getDiaMes() : null);
        e.setHora(f.getHora());
        e.setActiva(f.isActiva());
    }
}
