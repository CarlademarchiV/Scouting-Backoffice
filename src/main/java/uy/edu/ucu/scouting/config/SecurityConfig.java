package uy.edu.ucu.scouting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import uy.edu.ucu.scouting.security.ScoutingOidcUserService;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, ScoutingOidcUserService oidcUserService) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/webjars/**", "/css/**", "/js/**", "/error", "/sin-sesion").permitAll()
                .requestMatchers("/sin-acceso").authenticated()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().hasRole("USUARIO"))
            .oauth2Login(login -> login
                .userInfoEndpoint(info -> info.oidcUserService(oidcUserService)))
            .logout(logout -> logout.logoutSuccessUrl("/sin-sesion"))
            .exceptionHandling(ex -> ex.accessDeniedPage("/sin-acceso"))
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; img-src 'self' data:; object-src 'none'; frame-ancestors 'none'"))
                .referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)));
        return http.build();
    }
}
