package ar.unsam.vv.atalaya.web.panel;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Panel web mínimo para la sala de control y las pruebas de Selenium (UI-01 a UI-03). */
@Controller
public class PanelController {

    @GetMapping("/panel")
    public String panel() {
        return "panel";
    }
}
