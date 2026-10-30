package uy.edu.ucu.scouting.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CorreosTest {

    @Test
    void separaPorComaPuntoYComaYSaltos() {
        assertThat(Correos.parsear("A@ucu.edu.uy, b@ucu.edu.uy;\n<c@ucu.edu.uy>  a@ucu.edu.uy"))
                .containsExactly("a@ucu.edu.uy", "b@ucu.edu.uy", "c@ucu.edu.uy");
    }

    @Test
    void textoVacioDevuelveListaVacia() {
        assertThat(Correos.parsear("  ")).isEmpty();
        assertThat(Correos.parsear(null)).isEmpty();
    }

    @Test
    void validaFormato() {
        assertThat(Correos.esValido("nombre.apellido@ucu.edu.uy")).isTrue();
        assertThat(Correos.esValido("sin-arroba.ucu.edu.uy")).isFalse();
        assertThat(Correos.esValido("x@dominio")).isFalse();
    }

    @Test
    void obtieneDominio() {
        assertThat(Correos.dominio("a@correo.ucu.edu.uy")).isEqualTo("correo.ucu.edu.uy");
    }
}
