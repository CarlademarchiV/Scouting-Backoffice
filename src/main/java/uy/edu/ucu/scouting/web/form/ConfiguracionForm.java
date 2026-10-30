package uy.edu.ucu.scouting.web.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import uy.edu.ucu.scouting.domain.Configuracion;

@Data
public class ConfiguracionForm {

    @Min(value = 1, message = "Debe ser al menos 1.")
    private int maxParticipantes;

    @Min(value = 1, message = "Debe ser al menos 1.")
    private int maxExploracionesActivas;

    @NotBlank(message = "Indicá al menos un dominio.")
    @Size(max = 500)
    private String dominiosPermitidos;

    @Min(1) @Max(10)
    private int maxIntentos;

    @Min(1) @Max(30)
    private int fallasParaDesactivar;

    @Email(message = "Correo inválido.")
    @Size(max = 320)
    private String correoAlertasTi;

    @Size(max = 2000)
    private String pieCorreo;

    public static ConfiguracionForm desde(Configuracion c) {
        ConfiguracionForm f = new ConfiguracionForm();
        f.setMaxParticipantes(c.getMaxParticipantes());
        f.setMaxExploracionesActivas(c.getMaxExploracionesActivas());
        f.setDominiosPermitidos(c.getDominiosPermitidos());
        f.setMaxIntentos(c.getMaxIntentos());
        f.setFallasParaDesactivar(c.getFallasParaDesactivar());
        f.setCorreoAlertasTi(c.getCorreoAlertasTi());
        f.setPieCorreo(c.getPieCorreo());
        return f;
    }
}
