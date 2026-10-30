package uy.edu.ucu.scouting.web.admin;

import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uy.edu.ucu.scouting.config.ScoutingProperties;
import uy.edu.ucu.scouting.domain.EstadoEjecucion;
import uy.edu.ucu.scouting.service.EjecucionService;
import uy.edu.ucu.scouting.service.ReglaNegocioException;

@Controller
@RequestMapping("/admin/ejecuciones")
@RequiredArgsConstructor
public class MonitorController {

    private final EjecucionService ejecuciones;
    private final ScoutingProperties props;

    @GetMapping
    public String monitor(@RequestParam(required = false) EstadoEjecucion estado,
                          @RequestParam(required = false) String propietario,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                          Model model) {
        ZoneId zona = props.zona();
        model.addAttribute("ejecuciones", ejecuciones.monitor(estado,
                propietario,
                desde == null ? null : desde.atStartOfDay(zona).toInstant(),
                hasta == null ? null : hasta.plusDays(1).atStartOfDay(zona).toInstant()));
        model.addAttribute("fEstado", estado);
        model.addAttribute("fPropietario", propietario);
        model.addAttribute("fDesde", desde);
        model.addAttribute("fHasta", hasta);
        return "admin/monitor";
    }

    @PostMapping("/{id}/reintentar")
    public String reintentar(@PathVariable Long id, RedirectAttributes flash) {
        try {
            ejecuciones.reintentar(id);
            flash.addFlashAttribute("ok", "La ejecución volvió a la cola; el planificador la toma en su próximo ciclo.");
        } catch (ReglaNegocioException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/ejecuciones";
    }
}
