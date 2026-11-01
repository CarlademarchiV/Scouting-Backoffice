package uy.edu.ucu.scouting.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.stereotype.Service;
import uy.edu.ucu.scouting.config.ScoutingProperties;
import uy.edu.ucu.scouting.domain.Usuario;
import uy.edu.ucu.scouting.repository.GrupoRepository;

/** Calcula los roles de la app (grupos propios + admins iniciales). Lo usan el login de Entra y el de desarrollo. */
@Service
@RequiredArgsConstructor
public class RolesService {

    public static final String ROL_USUARIO = "ROLE_USUARIO";
    public static final String ROL_ADMIN = "ROLE_ADMIN";

    private final GrupoRepository grupos;
    private final ScoutingProperties props;

    public ScoutingUser crearPrincipal(Usuario usuario, Collection<? extends GrantedAuthority> base,
                                       OidcIdToken idToken, OidcUserInfo userInfo) {
        boolean admin = usuario.isActivo()
                && (props.admins().contains(usuario.getUpn().toLowerCase(Locale.ROOT)) || grupos.esAdmin(usuario.getId()));
        boolean acceso = usuario.isActivo() && (admin || grupos.tieneGrupoActivo(usuario.getId()));

        Set<GrantedAuthority> roles = new HashSet<>(base);
        if (acceso) {
            roles.add(new SimpleGrantedAuthority(ROL_USUARIO));
        }
        if (admin) {
            roles.add(new SimpleGrantedAuthority(ROL_ADMIN));
        }
        return new ScoutingUser(roles, idToken, userInfo, usuario.getId(), usuario.getNombre(), usuario.getUpn(), admin);
    }
}
