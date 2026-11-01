package uy.edu.ucu.scouting.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uy.edu.ucu.scouting.config.ScoutingProperties;
import uy.edu.ucu.scouting.domain.Usuario;
import uy.edu.ucu.scouting.service.UsuarioService;

/** Login con Entra ID: registra al usuario en la base y le asigna roles según los grupos de la app. */
@Service
@RequiredArgsConstructor
public class ScoutingOidcUserService extends OidcUserService {

    private final UsuarioService usuarioService;
    private final RolesService rolesService;
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
        return rolesService.crearPrincipal(usuario, oidc.getAuthorities(), oidc.getIdToken(), oidc.getUserInfo());
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
