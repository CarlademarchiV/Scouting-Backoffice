package uy.edu.ucu.scouting.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import uy.edu.ucu.scouting.security.ScoutingUser;
import uy.edu.ucu.scouting.service.EjecucionService;

@Controller
@RequiredArgsConstructor
public class EjecucionController {

    private final EjecucionService ejecuciones;

    @GetMapping("/ejecuciones/{id}")
    public String ver(@PathVariable Long id, @AuthenticationPrincipal ScoutingUser u, Model model) {
        model.addAttribute("ejecucion", ejecuciones.obtener(id, u));
        return "ejecuciones/detalle";
    }
}
