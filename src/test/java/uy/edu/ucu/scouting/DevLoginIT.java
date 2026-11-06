package uy.edu.ucu.scouting;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Login de desarrollo (scouting.auth.modo=dev): ingreso solo con el correo. */
@SpringBootTest(properties = {"spring.flyway.enabled=true", "scouting.auth.modo=dev",
        "scouting.admins-iniciales=admin@ucu.edu.uy"})
@AutoConfigureMockMvc
@Testcontainers
class DevLoginIT {

    @Container
    @ServiceConnection
    static MSSQLServerContainer<?> sql =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @Autowired
    MockMvc mvc;

    @Test
    void sinSesionVaALaPantallaDeDesarrollo() throws Exception {
        mvc.perform(get("/exploraciones"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login-dev"));
        mvc.perform(get("/login-dev")).andExpect(status().isOk());
    }

    @Test
    void adminInicialIngresaConSuCorreo() throws Exception {
        mvc.perform(post("/login-dev").param("username", "admin@ucu.edu.uy").param("password", "dev").with(csrf()))
                .andExpect(redirectedUrl("/exploraciones"));
    }

    @Test
    void rechazaDominioNoPermitido() throws Exception {
        mvc.perform(post("/login-dev").param("username", "alguien@gmail.com").param("password", "dev").with(csrf()))
                .andExpect(redirectedUrl("/login-dev?error"));
    }

    @Test
    void desdeOtraMaquinaNoSeAccede() throws Exception {
        mvc.perform(get("/login-dev").with(req -> {
                    req.setRemoteAddr("203.0.113.10");
                    return req;
                }))
                .andExpect(status().isForbidden());
    }
}
