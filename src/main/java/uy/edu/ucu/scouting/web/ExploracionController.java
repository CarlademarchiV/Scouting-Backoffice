package uy.edu.ucu.scouting.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uy.edu.ucu.scouting.config.ScoutingProperties;
import uy.edu.ucu.scouting.domain.Exploracion;
import uy.edu.ucu.scouting.domain.ModoEjecucion;
import uy.edu.ucu.scouting.security.ScoutingUser;
import uy.edu.ucu.scouting.service.EjecucionService;
import uy.edu.ucu.scouting.service.ExploracionService;
import uy.edu.ucu.scouting.service.ParticipanteService;
import uy.edu.ucu.scouting.service.ReglaNegocioException;
import uy.edu.ucu.scouting.service.ReglasService;
import uy.edu.ucu.scouting.web.form.ExploracionForm;

@Controller
@RequestMapping("/exploraciones")
@RequiredArgsConstructor
public class ExploracionController {

    private final ExploracionService exploraciones;
    private final ParticipanteService participantes;
    private final EjecucionService ejecuciones;
    private final ReglasService reglas;
    private final ScoutingProperties props;

    @GetMapping
    public String listar(@RequestParam(defaultValue = "false") boolean todas,
                         @AuthenticationPrincipal ScoutingUser u, Model model) {
        model.addAttribute("exploraciones", exploraciones.listar(u, todas));
        model.addAttribute("todas", todas && u.isAdmin());
        return "exploraciones/lista";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("form", new ExploracionForm());
        return "exploraciones/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("form") ExploracionForm form, BindingResult errores,
                        @AuthenticationPrincipal ScoutingUser u, Model model, RedirectAttributes flash) {
        if (errores.hasErrors()) {
            return "exploraciones/form";
        }
        try {
            Exploracion e = exploraciones.crear(form, u);
            flash.addFlashAttribute("ok", "Exploración creada. Ahora agregá los participantes.");
            return "redirect:/exploraciones/" + e.getId();
        } catch (ReglaNegocioException ex) {
            model.addAttribute("error", ex.getMessage());
            return "exploraciones/form";
        }
    }

    @GetMapping("/{id}")
    public String ver(@PathVariable Long id, @AuthenticationPrincipal ScoutingUser u, Model model) {
        Exploracion e = exploraciones.obtener(id, u);
        model.addAttribute("form", ExploracionForm.desde(e));
        prepararDetalle(e, model);
        return "exploraciones/form";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id, @Valid @ModelAttribute("form") ExploracionForm form,
                             BindingResult errores, @AuthenticationPrincipal ScoutingUser u,
                             Model model, RedirectAttributes flash) {
        if (!errores.hasErrors()) {
            try {
                exploraciones.actualizar(id, form, u);
                flash.addFlashAttribute("ok", "Cambios guardados.");
                return "redirect:/exploraciones/" + id;
            } catch (ReglaNegocioException ex) {
                model.addAttribute("error", ex.getMessage());
            }
        }
        prepararDetalle(exploraciones.obtener(id, u), model);
        return "exploraciones/form";
    }

    @PostMapping("/{id}/duplicar")
    public String duplicar(@PathVariable Long id, @AuthenticationPrincipal ScoutingUser u, RedirectAttributes flash) {
        Exploracion copia = exploraciones.duplicar(id, u);
        flash.addFlashAttribute("ok", "Se creó una copia inactiva. Revisala y activala cuando esté lista.");
        return "redirect:/exploraciones/" + copia.getId();
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, @AuthenticationPrincipal ScoutingUser u, RedirectAttributes flash) {
        exploraciones.eliminar(id, u);
        flash.addFlashAttribute("ok", "Exploración eliminada.");
        return "redirect:/exploraciones";
    }

    @PostMapping("/{id}/ejecutar")
    public String ejecutar(@PathVariable Long id, @RequestParam String modo,
                           @AuthenticationPrincipal ScoutingUser u, RedirectAttributes flash) {
        ModoEjecucion m = "enviar".equals(modo) ? ModoEjecucion.MANUAL : ModoEjecucion.PRUEBA;
        try {
            exploraciones.solicitarEjecucion(id, m, u);
            String minutos = "unos " + (props.minutosPlanificador() + 5) + " minutos";
            flash.addFlashAttribute("ok", m == ModoEjecucion.MANUAL
                    ? "Ejecución solicitada: el informe se enviará a los participantes en " + minutos + ". El avance queda en el Historial."
                    : "Prueba solicitada: en " + minutos + " te llega el informe solo a vos. El avance queda en el Historial.");
        } catch (ReglaNegocioException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/exploraciones/" + id + "#historial";
    }

    @PostMapping("/{id}/participantes")
    public String agregarParticipantes(@PathVariable Long id, @RequestParam String correos,
                                       @AuthenticationPrincipal ScoutingUser u, RedirectAttributes flash) {
        try {
            ParticipanteService.ResultadoAlta r = participantes.agregar(id, correos, u);
            if (r.agregados() > 0) {
                flash.addFlashAttribute("ok", r.agregados() == 1 ? "Se agregó 1 participante." : "Se agregaron " + r.agregados() + " participantes.");
            }
            if (!r.errores().isEmpty()) {
                flash.addFlashAttribute("errores", r.errores());
            }
        } catch (ReglaNegocioException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/exploraciones/" + id + "#participantes";
    }

    @PostMapping("/{id}/participantes/{pid}/activo")
    public String cambiarActivo(@PathVariable Long id, @PathVariable Long pid, @RequestParam boolean activo,
                                @AuthenticationPrincipal ScoutingUser u, RedirectAttributes flash) {
        try {
            participantes.cambiarActivo(id, pid, activo, u);
        } catch (ReglaNegocioException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/exploraciones/" + id + "#participantes";
    }

    @PostMapping("/{id}/participantes/{pid}/eliminar")
    public String eliminarParticipante(@PathVariable Long id, @PathVariable Long pid,
                                       @AuthenticationPrincipal ScoutingUser u) {
        participantes.eliminar(id, pid, u);
        return "redirect:/exploraciones/" + id + "#participantes";
    }

    private void prepararDetalle(Exploracion e, Model model) {
        model.addAttribute("exploracion", e);
        model.addAttribute("historial", ejecuciones.historial(e.getId()));
        model.addAttribute("topeParticipantes", reglas.maxParticipantes(e.getPropietario().getId()));
    }
}
