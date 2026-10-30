package uy.edu.ucu.scouting.domain;

/** Se guarda como ordinal (tinyint): el orden no debe cambiar. */
public enum ModoEjecucion {
    PROGRAMADA("Programada"),
    PRUEBA("Prueba"),
    MANUAL("Enviar ahora");

    private final String etiqueta;

    ModoEjecucion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
