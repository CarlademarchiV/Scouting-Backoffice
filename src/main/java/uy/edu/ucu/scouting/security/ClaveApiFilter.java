package uy.edu.ucu.scouting.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica a Power Automate por la clave compartida del encabezado X-Scouting-Clave.
 * Sin clave configurada, nadie se autentica y la API responde 401.
 */
public class ClaveApiFilter extends OncePerRequestFilter {

    public static final String ENCABEZADO = "X-Scouting-Clave";
    public static final String ROL_FLUJO = "ROLE_FLUJO";

    private final byte[] clave;

    public ClaveApiFilter(String clave) {
        this.clave = clave == null || clave.isBlank() ? null : clave.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recibida = request.getHeader(ENCABEZADO);
        if (clave != null && recibida != null
                && MessageDigest.isEqual(clave, recibida.getBytes(StandardCharsets.UTF_8))) {
            var auth = UsernamePasswordAuthenticationToken.authenticated(
                    "power-automate", null, List.of(new SimpleGrantedAuthority(ROL_FLUJO)));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }
}
