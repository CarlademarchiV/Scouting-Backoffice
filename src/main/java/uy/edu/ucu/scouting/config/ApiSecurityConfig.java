package uy.edu.ucu.scouting.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import uy.edu.ucu.scouting.security.ClaveApiFilter;

/**
 * Seguridad de /api/** (la consume Power Automate): sin sesión ni CSRF, autenticación por la clave
 * del encabezado X-Scouting-Clave. Va antes que la cadena de las pantallas (Entra o dev).
 */
@Slf4j
@Configuration
public class ApiSecurityConfig {

    /** Largo mínimo recomendado de la clave. */
    static final int LARGO_MINIMO = 32;

    @Bean
    @Order(1)
    SecurityFilterChain apiFilterChain(HttpSecurity http,
                                       @Value("${scouting.api.clave:${SCOUTING_API_CLAVE:}}") String clave) throws Exception {
        if (clave == null || clave.isBlank()) {
            log.warn("scouting.api.clave no está configurada: la API /api/flujo rechaza todas las llamadas.");
        } else if (clave.length() < LARGO_MINIMO) {
            log.warn("scouting.api.clave tiene menos de {} caracteres: usar una clave más larga.", LARGO_MINIMO);
        }
        http.securityMatcher("/api/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("FLUJO"))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .requestCache(cache -> cache.disable())
            .addFilterBefore(new ClaveApiFilter(clave), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((req, res, e) -> res.sendError(HttpStatus.FORBIDDEN.value())));
        return http.build();
    }
}
