package uy.edu.ucu.scouting.web.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uy.edu.ucu.scouting.security.ScoutingUser;
import uy.edu.ucu.scouting.service.ConfiguracionService;
import uy.edu.ucu.scouting.web.form.ConfiguracionForm;

@Controller
@RequestMapping("/admin/configuracion")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final ConfiguracionService configuracion;

    @GetMapping
    public String ver(Model model) {
        model.addAttribute("form", ConfiguracionForm.desde(configuracion.obtener()));
        model.addAttribute("config", configuracion.obtener());
        return "admin/configuracion";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("form") ConfiguracionForm form, BindingResult errores,
                          @AuthenticationPrincipal ScoutingUser u, Model model, RedirectAttributes flash) {
        if (errores.hasErrors()) {
            model.addAttribute("config", configuracion.obtener());
            return "admin/configuracion";
        }
        configuracion.guardar(form, u.getUpn());
        flash.addFlashAttribute("ok", "Configuración guardada.");
        return "redirect:/admin/configuracion";
    }
}
