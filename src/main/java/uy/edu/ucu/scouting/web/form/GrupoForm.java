package uy.edu.ucu.scouting.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import uy.edu.ucu.scouting.domain.Grupo;

@Data
public class GrupoForm {

    @NotBlank(message = "Ingresá el nombre del grupo.")
    @Size(max = 100)
    private String nombre;

    @Size(max = 500)
    private String descripcion;

    private boolean esAdmin;

    @Positive(message = "El tope debe ser mayor que 0 o quedar vacío.")
    private Integer maxParticipantes;

    @Positive(message = "El tope debe ser mayor que 0 o quedar vacío.")
    private Integer maxExploracionesActivas;

    private boolean activo = true;

    public static GrupoForm desde(Grupo g) {
        GrupoForm f = new GrupoForm();
        f.setNombre(g.getNombre());
        f.setDescripcion(g.getDescripcion());
        f.setEsAdmin(g.isEsAdmin());
        f.setMaxParticipantes(g.getMaxParticipantes());
        f.setMaxExploracionesActivas(g.getMaxExploracionesActivas());
        f.setActivo(g.isActivo());
        return f;
    }
}
