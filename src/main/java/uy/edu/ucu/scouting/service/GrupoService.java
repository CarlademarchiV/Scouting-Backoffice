package uy.edu.ucu.scouting.service;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uy.edu.ucu.scouting.domain.Grupo;
import uy.edu.ucu.scouting.domain.Usuario;
import uy.edu.ucu.scouting.repository.GrupoRepository;
import uy.edu.ucu.scouting.web.form.GrupoForm;

/** Grupos de seguridad propios de la app (no se gestionan en AD). Los cambios rigen desde el próximo ingreso. */
@Service
@RequiredArgsConstructor
public class GrupoService {

    private final GrupoRepository grupos;
    private final UsuarioService usuarios;

    @Transactional(readOnly = true)
    public List<Grupo> listar() {
        List<Grupo> lista = grupos.findAllByOrderByNombreAsc();
        lista.forEach(g -> g.getMiembros().size());
        return lista;
    }

    @Transactional(readOnly = true)
    public Grupo obtener(Long id) {
        return grupos.conMiembros(id).orElseThrow(NoEncontradoException::new);
    }

    @Transactional
    public Grupo crear(GrupoForm f, String actor) {
        if (grupos.existsByNombreIgnoreCase(f.getNombre().trim())) {
            throw new ReglaNegocioException("Ya existe un grupo con ese nombre.");
        }
        Grupo g = new Grupo();
        aplicar(f, g);
        g.setCreadoPor(actor);
        return grupos.save(g);
    }

    @Transactional
    public Grupo actualizar(Long id, GrupoForm f, String actor) {
        if (grupos.existsByNombreIgnoreCaseAndIdNot(f.getNombre().trim(), id)) {
            throw new ReglaNegocioException("Ya existe un grupo con ese nombre.");
        }
        Grupo g = obtener(id);
        aplicar(f, g);
        g.setModificadoEn(Instant.now());
        g.setModificadoPor(actor);
        return grupos.save(g);
    }

    @Transactional
    public void agregarMiembro(Long grupoId, String upn, String actor) {
        if (!StringUtils.hasText(upn)) {
            throw new ReglaNegocioException("Ingresá el correo institucional de la persona.");
        }
        Grupo g = obtener(grupoId);
        Usuario u = usuarios.obtenerOCrearPorUpn(upn);
        if (!g.getMiembros().add(u)) {
            throw new ReglaNegocioException(u.getUpn() + " ya es miembro del grupo.");
        }
        g.setModificadoEn(Instant.now());
        g.setModificadoPor(actor);
    }

    @Transactional
    public void quitarMiembro(Long grupoId, Long usuarioId, String actor) {
        Grupo g = obtener(grupoId);
        g.getMiembros().removeIf(u -> u.getId().equals(usuarioId));
        g.setModificadoEn(Instant.now());
        g.setModificadoPor(actor);
    }

    private static void aplicar(GrupoForm f, Grupo g) {
        g.setNombre(f.getNombre().trim());
        g.setDescripcion(StringUtils.hasText(f.getDescripcion()) ? f.getDescripcion().trim() : null);
        g.setEsAdmin(f.isEsAdmin());
        g.setMaxParticipantes(f.getMaxParticipantes());
        g.setMaxExploracionesActivas(f.getMaxExploracionesActivas());
        g.setActivo(f.isActivo());
    }
}
