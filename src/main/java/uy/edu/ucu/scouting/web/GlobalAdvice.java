package uy.edu.ucu.scouting.web;

import java.util.stream.IntStream;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;
import uy.edu.ucu.scouting.domain.EstadoEjecucion;
import uy.edu.ucu.scouting.domain.Frecuencia;

@ControllerAdvice
public class GlobalAdvice {

    @ModelAttribute
    public void listas(Model model) {
        model.addAttribute("frecuencias", Frecuencia.values());
        model.addAttribute("estados", EstadoEjecucion.values());
        model.addAttribute("dias", Formatos.DIAS);
        model.addAttribute("horas", IntStream.rangeClosed(0, 23).boxed().toList());
        model.addAttribute("diasMes", IntStream.rangeClosed(1, 28).boxed().toList());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public String concurrencia(Model model) {
        model.addAttribute("mensaje", "Otra persona modificó el mismo dato al mismo tiempo. Volvé atrás, recargá y reintentá.");
        return "error";
    }
}
