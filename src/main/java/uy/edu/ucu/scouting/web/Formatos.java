package uy.edu.ucu.scouting.web;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;
import uy.edu.ucu.scouting.config.ScoutingProperties;
import uy.edu.ucu.scouting.domain.Exploracion;
import uy.edu.ucu.scouting.domain.Frecuencia;

/** Formatos para las vistas: ${@fmt.fecha(x)}. Las fechas se guardan en UTC y se muestran en Montevideo. */
@Component("fmt")
public class Formatos {

    public static final List<String> DIAS = List.of("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo");

    private final DateTimeFormatter fecha;

    public Formatos(ScoutingProperties props) {
        this.fecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(props.zona());
    }

    public String fecha(Instant instante) {
        return instante == null ? "—" : fecha.format(instante);
    }

    public String dia(Integer diaSemana) {
        return diaSemana == null || diaSemana < 1 || diaSemana > 7 ? "" : DIAS.get(diaSemana - 1);
    }

    public String hora(Integer hora) {
        return hora == null ? "" : String.format("%02d:00", hora);
    }

    public String programacion(Exploracion e) {
        Frecuencia f = e.getFrecuencia();
        String base = f.getEtiqueta();
        if (f.usaDiaSemana()) {
            base += ", " + dia(e.getDiaSemana());
        } else if (f.usaDiaMes()) {
            base += ", día " + e.getDiaMes();
        }
        return base + " a las " + hora(e.getHora());
    }
}
