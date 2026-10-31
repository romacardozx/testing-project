package ar.unsam.vv.atalaya;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Punto de entrada de ATALAYA 3.1.0 (implementación reducida para el TP de VyV). */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AtalayaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtalayaApplication.class, args);
    }
}
