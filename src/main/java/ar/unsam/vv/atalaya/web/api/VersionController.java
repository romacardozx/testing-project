package ar.unsam.vv.atalaya.web.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /api/v1/version: versión de software y del modelo de estimación. */
@RestController
@RequestMapping("/api/v1/version")
public class VersionController {

    private final VersionRespuesta version;

    public VersionController(@Value("${atalaya.version.software}") String software,
                             @Value("${atalaya.version.modelo}") String modelo) {
        this.version = new VersionRespuesta(software, modelo);
    }

    @GetMapping
    public VersionRespuesta version() {
        return version;
    }

    public record VersionRespuesta(String software, String modelo) {
    }
}
