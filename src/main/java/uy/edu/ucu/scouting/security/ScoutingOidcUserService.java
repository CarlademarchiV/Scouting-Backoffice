package uy.edu.ucu.scouting.security;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uy.edu.ucu.scouting.config.ScoutingProperties;
import uy.edu.ucu.scouting.domain.Usuario;
import uy.edu.ucu.scouting.repository.GrupoRepository;
import uy.edu.ucu.scouting.service.UsuarioService;

/** Registra al usuario en la base y le asigna roles según los grupos de la app. */
@Service
@RequiredArgsConstructor
public class ScoutingOidcUserService extends OidcUserService {

    public static final String ROL_USUARIO = "ROLE_USUARIO";
    public static final String ROL_ADMIN = "ROLE_ADMIN";

    private final UsuarioService usuarioService;
    private final GrupoRepository grupos;
    private final ScoutingProperties props;

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcUser oidc = super.loadUser(request);

        String tenant = oidc.getClaimAsString("tid");
        if (StringUtils.hasText(props.tenantId()) && !props.tenantId().equalsIgnoreCase(tenant)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("tenant_invalido", "La cuenta no pertenece a UCU.", null));
        }

        String oid = oidc.getClaimAsString("oid");
        String upn = primero(oidc.getPreferredUsername(), oidc.getEmail(), oidc.getSubject());
        String nombre = primero(oidc.getFullName(), upn);
        String email = primero(oidc.getEmail(), upn);

        Usuario usuario = usuarioService.registrarIngreso(oid, upn, nombre, email);

        boolean admin = usuario.isActivo()
                && (props.admins().contains(upn.toLowerCase(Locale.ROOT)) || grupos.esAdmin(usuario.getId()));
        boolean acceso = usuario.isActivo() && (admin || grupos.tieneGrupoActivo(usuario.getId()));

        Set<GrantedAuthority> roles = new HashSet<>(oidc.getAuthorities());
        if (acceso) {
            roles.add(new SimpleGrantedAuthority(ROL_USUARIO));
        }
        if (admin) {
            roles.add(new SimpleGrantedAuthority(ROL_ADMIN));
        }
        return new ScoutingUser(roles, oidc.getIdToken(), oidc.getUserInfo(),
                usuario.getId(), usuario.getNombre(), usuario.getUpn(), admin);
    }

    private static String primero(String... valores) {
        for (String v : valores) {
            if (StringUtils.hasText(v)) {
                return v.trim();
            }
        }
        return "";
    }
}
