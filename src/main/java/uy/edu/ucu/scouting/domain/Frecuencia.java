package uy.edu.ucu.scouting.domain;

/** Se guarda como ordinal (tinyint): el orden no debe cambiar. */
public enum Frecuencia {
    DIARIA("Diaria"),
    SEMANAL("Semanal"),
    QUINCENAL("Quincenal"),
    MENSUAL("Mensual");

    private final String etiqueta;

    Frecuencia(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean usaDiaSemana() {
        return this == SEMANAL || this == QUINCENAL;
    }

    public boolean usaDiaMes() {
        return this == MENSUAL;
    }
}
