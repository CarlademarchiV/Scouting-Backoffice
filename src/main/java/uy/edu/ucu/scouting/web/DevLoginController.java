package uy.edu.ucu.scouting.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Pantalla de ingreso de desarrollo (solo con scouting.auth.modo=dev). */
@Controller
@ConditionalOnProperty(name = "scouting.auth.modo", havingValue = "dev")
public class DevLoginController {

    @GetMapping("/login-dev")
    public String login() {
        return "login-dev";
    }
}
