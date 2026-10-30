package uy.edu.ucu.scouting.web.admin;

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
import uy.edu.ucu.scouting.domain.Grupo;
import uy.edu.ucu.scouting.security.ScoutingUser;
import uy.edu.ucu.scouting.service.GrupoService;
import uy.edu.ucu.scouting.service.ReglaNegocioException;
import uy.edu.ucu.scouting.web.form.GrupoForm;

@Controller
@RequestMapping("/admin/grupos")
@RequiredArgsConstructor
public class GrupoController {

    private final GrupoService grupos;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("grupos", grupos.listar());
        return "admin/grupos";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("form", new GrupoForm());
        return "admin/grupo-form";
    }

    @PostMapping("/nuevo")
    public String crear(@Valid @ModelAttribute("form") GrupoForm form, BindingResult errores,
                        @AuthenticationPrincipal ScoutingUser u, Model model, RedirectAttributes flash) {
        if (errores.hasErrors()) {
            return "admin/grupo-form";
        }
        try {
            Grupo g = grupos.crear(form, u.getUpn());
            flash.addFlashAttribute("ok", "Grupo creado. Agregá sus miembros.");
            return "redirect:/admin/grupos/" + g.getId();
        } catch (ReglaNegocioException ex) {
            model.addAttribute("error", ex.getMessage());
            return "admin/grupo-form";
        }
    }

    @GetMapping("/{id}")
    public String ver(@PathVariable Long id, Model model) {
        Grupo g = grupos.obtener(id);
        model.addAttribute("grupo", g);
        model.addAttribute("form", GrupoForm.desde(g));
        return "admin/grupo-form";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id, @Valid @ModelAttribute("form") GrupoForm form,
                             BindingResult errores, @AuthenticationPrincipal ScoutingUser u,
                             Model model, RedirectAttributes flash) {
        if (!errores.hasErrors()) {
            try {
                grupos.actualizar(id, form, u.getUpn());
                flash.addFlashAttribute("ok", "Grupo guardado. Los cambios de permisos rigen desde el próximo ingreso de cada persona.");
                return "redirect:/admin/grupos/" + id;
            } catch (ReglaNegocioException ex) {
                model.addAttribute("error", ex.getMessage());
            }
        }
        model.addAttribute("grupo", grupos.obtener(id));
        return "admin/grupo-form";
    }

    @PostMapping("/{id}/miembros")
    public String agregarMiembro(@PathVariable Long id, @RequestParam String upn,
                                 @AuthenticationPrincipal ScoutingUser u, RedirectAttributes flash) {
        try {
            grupos.agregarMiembro(id, upn, u.getUpn());
            flash.addFlashAttribute("ok", "Miembro agregado.");
        } catch (ReglaNegocioException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/grupos/" + id + "#miembros";
    }

    @PostMapping("/{id}/miembros/{usuarioId}/eliminar")
    public String quitarMiembro(@PathVariable Long id, @PathVariable Long usuarioId,
                                @AuthenticationPrincipal ScoutingUser u, RedirectAttributes flash) {
        grupos.quitarMiembro(id, usuarioId, u.getUpn());
        flash.addFlashAttribute("ok", "Miembro quitado.");
        return "redirect:/admin/grupos/" + id + "#miembros";
    }
}
