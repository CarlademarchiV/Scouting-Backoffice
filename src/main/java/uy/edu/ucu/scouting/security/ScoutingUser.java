package uy.edu.ucu.scouting.security;

import java.util.Collection;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

/** Usuario autenticado con su id en la tabla scouting.usuario. Los roles se calculan al iniciar sesión. */
@Getter
public class ScoutingUser extends DefaultOidcUser {

    private final Long usuarioId;
    private final String nombre;
    private final String upn;
    private final boolean admin;

    public ScoutingUser(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken, OidcUserInfo userInfo,
                        Long usuarioId, String nombre, String upn, boolean admin) {
        super(authorities, idToken, userInfo, "sub");
        this.usuarioId = usuarioId;
        this.nombre = nombre;
        this.upn = upn;
        this.admin = admin;
    }
}
