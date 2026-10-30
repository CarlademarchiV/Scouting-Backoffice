package uy.edu.ucu.scouting.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.ucu.scouting.domain.Exploracion;
import uy.edu.ucu.scouting.domain.Participante;
import uy.edu.ucu.scouting.repository.ExploracionRepository;
import uy.edu.ucu.scouting.security.ScoutingUser;

@Service
@RequiredArgsConstructor
public class ParticipanteService {

    public record ResultadoAlta(int agregados, List<String> errores) {
    }

    private final ExploracionService exploracionService;
    private final ExploracionRepository exploraciones;
    private final ConfiguracionService configuracion;
    private final ReglasService reglas;

    /** Agrega uno o varios correos (separados por coma, punto y coma o salto de línea). */
    @Transactional
    public ResultadoAlta agregar(Long exploracionId, String texto, ScoutingUser u) {
        Exploracion e = exploracionService.obtener(exploracionId, u);
        List<String> correos = Correos.parsear(texto);
        if (correos.isEmpty()) {
            throw new ReglaNegocioException("Ingresá al menos un correo.");
        }
        List<String> dominios = configuracion.obtener().dominios();
        int tope = reglas.maxParticipantes(e.getPropietario().getId());
        long activos = e.participantesActivos();

        List<String> errores = new ArrayList<>();
        int agregados = 0;
        for (String correo : correos) {
            if (!Correos.esValido(correo)) {
                errores.add(correo + ": no es un correo válido.");
            } else if (!dominios.isEmpty() && !dominios.contains(Correos.dominio(correo))) {
                errores.add(correo + ": el dominio no está permitido (" + String.join(", ", dominios) + ").");
            } else if (e.getParticipantes().stream().anyMatch(p -> p.getCorreo().equalsIgnoreCase(correo))) {
                errores.add(correo + ": ya es participante.");
            } else if (activos + 1 > tope) {
                errores.add(correo + ": la exploración ya tiene el máximo de " + tope + " participantes activos.");
            } else {
                Participante p = new Participante();
                p.setExploracion(e);
                p.setCorreo(correo);
                p.setCreadoPor(u.getUpn());
                e.getParticipantes().add(p);
                activos++;
                agregados++;
            }
        }
        exploraciones.save(e);
        return new ResultadoAlta(agregados, errores);
    }

    @Transactional
    public void cambiarActivo(Long exploracionId, Long participanteId, boolean activo, ScoutingUser u) {
        Exploracion e = exploracionService.obtener(exploracionId, u);
        Participante p = buscar(e, participanteId);
        if (activo && !p.isActivo()) {
            int tope = reglas.maxParticipantes(e.getPropietario().getId());
            if (e.participantesActivos() + 1 > tope) {
                throw new ReglaNegocioException("Esta exploración ya tiene el máximo de " + tope + " participantes activos.");
            }
        }
        p.setActivo(activo);
        exploraciones.save(e);
    }

    @Transactional
    public void eliminar(Long exploracionId, Long participanteId, ScoutingUser u) {
        Exploracion e = exploracionService.obtener(exploracionId, u);
        e.getParticipantes().remove(buscar(e, participanteId));
        exploraciones.save(e);
    }

    private static Participante buscar(Exploracion e, Long participanteId) {
        return e.getParticipantes().stream()
                .filter(p -> p.getId().equals(participanteId))
                .findFirst()
                .orElseThrow(NoEncontradoException::new);
    }
}
