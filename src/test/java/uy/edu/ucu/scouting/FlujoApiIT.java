package uy.edu.ucu.scouting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** API que consume el flujo "Scouting - Origen": clave en encabezado y ciclo completo de la cola. */
@SpringBootTest(properties = {"spring.flyway.enabled=true", "scouting.api.clave=" + FlujoApiIT.CLAVE})
@AutoConfigureMockMvc
@Testcontainers
class FlujoApiIT {

    static final String CLAVE = "clave-de-prueba-para-power-automate-0123456789";
    static final String ENCABEZADO = "X-Scouting-Clave";

    @Container
    @ServiceConnection
    static MSSQLServerContainer<?> sql =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void datos() {
        jdbc.execute("DELETE FROM scouting.ejecucion; DELETE FROM scouting.participante; "
                + "DELETE FROM scouting.exploracion; DELETE FROM scouting.grupo_miembro; DELETE FROM scouting.usuario;");
        jdbc.execute("INSERT scouting.usuario (upn, nombre, email) VALUES ('ana@ucu.edu.uy', 'Ana', 'ana@ucu.edu.uy')");
        jdbc.execute("INSERT scouting.exploracion (titulo, prompt, frecuencia, dia_semana, hora, activa, propietario_id, proxima_ejecucion) "
                + "SELECT 'Tendencias', 'Buscá novedades', 1, 1, 9, 1, id, DATEADD(minute, -5, SYSUTCDATETIME()) FROM scouting.usuario");
        jdbc.execute("INSERT scouting.participante (exploracion_id, correo) "
                + "SELECT id, 'p1@ucu.edu.uy' FROM scouting.exploracion UNION ALL SELECT id, 'p2@ucu.edu.uy' FROM scouting.exploracion");
    }

    @Test
    void sinClaveONoValidaResponde401() throws Exception {
        mvc.perform(post("/api/flujo/lote")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/flujo/lote").header(ENCABEZADO, "otra")).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM scouting.ejecucion", Integer.class)).isZero();
    }

    @Test
    void cicloCompletoDeUnaEjecucion() throws Exception {
        String cuerpo = mvc.perform(post("/api/flujo/lote").header(ENCABEZADO, CLAVE)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lote\": 5, \"instancia\": \"prueba\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ejecuciones.length()").value(1))
                .andExpect(jsonPath("$.ejecuciones[0].modo").value("programada"))
                .andExpect(jsonPath("$.ejecuciones[0].intentos").value(1))
                .andExpect(jsonPath("$.ejecuciones[0].titulo").value("Tendencias"))
                .andExpect(jsonPath("$.ejecuciones[0].creadorEmail").value("ana@ucu.edu.uy"))
                .andExpect(jsonPath("$.ejecuciones[0].participantes[0]").value("p1@ucu.edu.uy"))
                .andExpect(jsonPath("$.ejecuciones[0].participantes[1]").value("p2@ucu.edu.uy"))
                .andReturn().getResponse().getContentAsString();
        long id = jdbc.queryForObject("SELECT id FROM scouting.ejecucion", Long.class);
        assertThat(cuerpo).contains("\"ejecucionId\":" + id);

        mvc.perform(put("/api/flujo/ejecuciones/" + id + "/informe").header(ENCABEZADO, CLAVE)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"informe\": \"Informe del agente\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/flujo/ejecuciones/" + id + "/resultado").header(ENCABEZADO, CLAVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ok\": true, \"destinatarios\": 3, \"resumen\": \"Tres novedades\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("enviado"))
                .andExpect(jsonPath("$.notificar").value(false));

        assertThat(jdbc.queryForObject("SELECT informe FROM scouting.ejecucion WHERE id = ?", String.class, id))
                .isEqualTo("Informe del agente");
        assertThat(jdbc.queryForObject("SELECT ultimo_resumen FROM scouting.exploracion", String.class))
                .isEqualTo("Tres novedades");
    }

    @Test
    void fallaPasaAReintentar() throws Exception {
        mvc.perform(post("/api/flujo/lote").header(ENCABEZADO, CLAVE)).andExpect(status().isOk());
        long id = jdbc.queryForObject("SELECT id FROM scouting.ejecucion", Long.class);

        mvc.perform(post("/api/flujo/ejecuciones/" + id + "/resultado").header(ENCABEZADO, CLAVE)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ok\": false, \"error\": \"agente: tiempo agotado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("reintentar"))
                .andExpect(jsonPath("$.notificar").value(false));
    }

    @Test
    void ejecucionInexistenteResponde404() throws Exception {
        mvc.perform(post("/api/flujo/ejecuciones/999999/resultado").header(ENCABEZADO, CLAVE)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ok\": true}"))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/flujo/ejecuciones/999999/informe").header(ENCABEZADO, CLAVE)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"informe\": \"x\"}"))
                .andExpect(status().isNotFound());
    }
}
