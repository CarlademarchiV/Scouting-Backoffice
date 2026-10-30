package uy.edu.ucu.scouting.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String inicio() {
        return "redirect:/exploraciones";
    }

    @GetMapping("/sin-acceso")
    public String sinAcceso() {
        return "sin-acceso";
    }

    @GetMapping("/sin-sesion")
    public String sinSesion() {
        return "sin-sesion";
    }
}
