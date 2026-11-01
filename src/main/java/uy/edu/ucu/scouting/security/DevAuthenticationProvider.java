package uy.edu.ucu.scouting.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.stereotype.Component;
import uy.edu.ucu.scouting.domain.Usuario;
import uy.edu.ucu.scouting.service.ConfiguracionService;
import uy.edu.ucu.scouting.service.Correos;
import uy.edu.ucu.scouting.service.UsuarioService;

/**
 * SOLO DESARROLLO: entra con el correo, sin contraseña ni Entra ID.
 * Se activa únicamente con scouting.auth.modo=dev (perfil "dev"). Nunca habilitarlo en servidores.
 */
@Component
@ConditionalOnProperty(name = "scouting.auth.modo", havingValue = "dev")
@RequiredArgsConstructor
public class DevAuthenticationProvider implements AuthenticationProvider {

    private final UsuarioService usuarioService;
    private final RolesService rolesService;
    private final ConfiguracionService configuracion;

    @Override
    public Authentication authenticate(Authentication auth) {
        String upn = Correos.normalizar(auth.getName());
        if (!Correos.esValido(upn)) {
            throw new BadCredentialsException("Ingresá un correo válido.");
        }
        List<String> dominios = configuracion.obtener().dominios();
        if (!dominios.isEmpty() && !dominios.contains(Correos.dominio(upn))) {
            throw new BadCredentialsException("El correo debe ser de un dominio permitido: " + String.join(", ", dominios));
        }
        Usuario usuario = usuarioService.registrarIngreso(null, upn, upn, upn);
        Instant ahora = Instant.now();
        OidcIdToken token = new OidcIdToken("dev", ahora, ahora.plusSeconds(8 * 3600),
                Map.of("sub", "dev-" + upn, "preferred_username", upn, "email", upn));
        ScoutingUser principal = rolesService.crearPrincipal(usuario, List.of(new SimpleGrantedAuthority("ROLE_DEV")), token, null);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
