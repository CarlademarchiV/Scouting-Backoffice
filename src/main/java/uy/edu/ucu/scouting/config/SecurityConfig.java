package uy.edu.ucu.scouting.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import uy.edu.ucu.scouting.security.DevAuthenticationProvider;
import uy.edu.ucu.scouting.security.ScoutingOidcUserService;
import uy.edu.ucu.scouting.security.SoloLocalFilter;

/**
 * scouting.auth.modo = entra (por defecto): login con Entra ID.
 * scouting.auth.modo = dev: login de desarrollo solo con el correo (perfil "dev", nunca en servidores);
 *   las pantallas solo responden a pedidos de la propia PC.
 * La API /api/** tiene su propia cadena (ApiSecurityConfig).
 */
@Slf4j
@Configuration
public class SecurityConfig {

    @Bean
    @ConditionalOnProperty(name = "scouting.auth.modo", havingValue = "entra", matchIfMissing = true)
    SecurityFilterChain entraFilterChain(HttpSecurity http, ScoutingOidcUserService oidcUserService) throws Exception {
        comun(http)
            .oauth2Login(login -> login
                .userInfoEndpoint(info -> info.oidcUserService(oidcUserService)));
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "scouting.auth.modo", havingValue = "dev")
    SecurityFilterChain devFilterChain(HttpSecurity http, DevAuthenticationProvider devProvider) throws Exception {
        log.warn("************************************************************************");
        log.warn("*  LOGIN DE DESARROLLO ACTIVO (scouting.auth.modo=dev): sin Entra ID.  *");
        log.warn("*  Usar solo en la PC local. Nunca en servidores.                      *");
        log.warn("************************************************************************");
        comun(http)
            .addFilterBefore(new SoloLocalFilter(), UsernamePasswordAuthenticationFilter.class)
            .authenticationProvider(devProvider)
            .formLogin(form -> form
                .loginPage("/login-dev").permitAll()
                .defaultSuccessUrl("/exploraciones", true)
                .failureUrl("/login-dev?error"));
        return http.build();
    }

    private static HttpSecurity comun(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/webjars/**", "/css/**", "/js/**", "/error", "/sin-sesion").permitAll()
                .requestMatchers("/sin-acceso").authenticated()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().hasRole("USUARIO"))
            .logout(logout -> logout.logoutSuccessUrl("/sin-sesion"))
            .exceptionHandling(ex -> ex.accessDeniedPage("/sin-acceso"))
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; img-src 'self' data:; object-src 'none'; frame-ancestors 'none'"))
                .referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)));
    }
}
