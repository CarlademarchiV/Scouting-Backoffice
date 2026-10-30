package uy.edu.ucu.scouting.service;

import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uy.edu.ucu.scouting.domain.Usuario;
import uy.edu.ucu.scouting.repository.UsuarioRepository;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarios;

    /** Busca por oid, luego por UPN (alta previa de un admin) y si no existe lo crea. */
    @Transactional
    public Usuario registrarIngreso(String oid, String upn, String nombre, String email) {
        Usuario u = (StringUtils.hasText(oid) ? usuarios.findByEntraOid(oid) : java.util.Optional.<Usuario>empty())
                .or(() -> usuarios.findByUpnIgnoreCase(upn))
                .orElseGet(Usuario::new);
        if (StringUtils.hasText(oid)) {
            u.setEntraOid(oid);
        }
        u.setUpn(upn.toLowerCase(Locale.ROOT));
        u.setNombre(nombre);
        u.setEmail(email.toLowerCase(Locale.ROOT));
        u.setUltimoAcceso(Instant.now());
        return usuarios.save(u);
    }

    /** Alta previa por UPN para poder sumarlo a un grupo antes de su primer ingreso. */
    @Transactional
    public Usuario obtenerOCrearPorUpn(String upn) {
        String normalizado = Correos.normalizar(upn);
        if (!Correos.esValido(normalizado)) {
            throw new ReglaNegocioException("\"" + upn + "\" no es un UPN válido (usá el correo institucional).");
        }
        return usuarios.findByUpnIgnoreCase(normalizado).orElseGet(() -> {
            Usuario u = new Usuario();
            u.setUpn(normalizado);
            u.setEmail(normalizado);
            u.setNombre(normalizado);
            return usuarios.save(u);
        });
    }
}
