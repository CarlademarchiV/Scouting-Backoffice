package uy.edu.ucu.scouting.domain;

/** Se guarda como ordinal (tinyint): el orden no debe cambiar. */
public enum EstadoEjecucion {
    PENDIENTE("Pendiente", "secondary"),
    EN_CURSO("En curso", "info"),
    REINTENTAR("Reintentar", "warning"),
    ENVIADO("Enviado", "success"),
    FALLIDO("Fallido", "danger"),
    OMITIDA("Omitida", "light");

    private final String etiqueta;
    private final String color;

    EstadoEjecucion(String etiqueta, String color) {
        this.etiqueta = etiqueta;
        this.color = color;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Clase de color de Bootstrap para la etiqueta. */
    public String getColor() {
        return color;
    }
}
