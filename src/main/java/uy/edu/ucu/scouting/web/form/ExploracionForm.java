package uy.edu.ucu.scouting.web.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import uy.edu.ucu.scouting.domain.Exploracion;
import uy.edu.ucu.scouting.domain.Frecuencia;

@Data
public class ExploracionForm {

    @NotBlank(message = "Ingresá un título.")
    @Size(max = 200, message = "El título admite hasta 200 caracteres.")
    private String titulo;

    @NotBlank(message = "Ingresá el prompt de la exploración.")
    @Size(max = Exploracion.MAX_PROMPT, message = "El prompt admite hasta 100.000 caracteres.")
    private String prompt;

    @NotNull(message = "Elegí la frecuencia.")
    private Frecuencia frecuencia = Frecuencia.SEMANAL;

    @Min(1) @Max(7)
    private Integer diaSemana = 1;

    @Min(value = 1, message = "El día del mes va de 1 a 28.")
    @Max(value = 28, message = "El día del mes va de 1 a 28.")
    private Integer diaMes;

    @NotNull(message = "Elegí la hora.")
    @Min(0) @Max(23)
    private Integer hora = 9;

    private boolean activa = true;

    /** Versión leída, para detectar ediciones simultáneas. */
    private Integer version;

    public static ExploracionForm desde(Exploracion e) {
        ExploracionForm f = new ExploracionForm();
        f.setTitulo(e.getTitulo());
        f.setPrompt(e.getPrompt());
        f.setFrecuencia(e.getFrecuencia());
        f.setDiaSemana(e.getDiaSemana());
        f.setDiaMes(e.getDiaMes());
        f.setHora(e.getHora());
        f.setActiva(e.isActiva());
        f.setVersion(e.getVersion());
        return f;
    }
}
