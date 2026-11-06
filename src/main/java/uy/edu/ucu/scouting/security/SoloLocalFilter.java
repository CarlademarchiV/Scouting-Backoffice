package uy.edu.ucu.scouting.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Con el login de desarrollo, las pantallas solo responden a pedidos de la propia PC.
 * Así, si se publica la PC con un túnel para probar la API desde Power Automate, nadie de afuera
 * puede entrar a las pantallas escribiendo un correo. La dirección sale de X-Forwarded-For cuando
 * el pedido llega por un proxy o túnel (server.forward-headers-strategy=framework).
 */
public class SoloLocalFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!esLocal(request.getRemoteAddr())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "El login de desarrollo solo funciona desde la propia PC.");
            return;
        }
        chain.doFilter(request, response);
    }

    static boolean esLocal(String direccion) {
        if (direccion == null || direccion.isBlank()) {
            return false;
        }
        try {
            return InetAddress.getByName(direccion).isLoopbackAddress();
        } catch (IOException e) {
            return false;
        }
    }
}
