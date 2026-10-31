package ar.unsam.vv.atalaya.config;

import ar.unsam.vv.atalaya.dominio.ClasificadorNivel;
import ar.unsam.vv.atalaya.dominio.PoliticaRepeticion;
import ar.unsam.vv.atalaya.dominio.ReglaManiobra;
import ar.unsam.vv.atalaya.dominio.ServicioEscalamiento;
import ar.unsam.vv.atalaya.dominio.ValidadorRango;
import ar.unsam.vv.atalaya.dominio.ValidadorRedundancia;
import ar.unsam.vv.atalaya.dominio.ValidadorUmbrales;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Expone las clases de dominio (sin Spring) como beans. */
@Configuration
public class DominioConfig {

    @Bean
    public ValidadorRango validadorRango() {
        return new ValidadorRango();
    }

    @Bean
    public ValidadorRedundancia validadorRedundancia() {
        return new ValidadorRedundancia();
    }

    @Bean
    public ClasificadorNivel clasificadorNivel() {
        return new ClasificadorNivel();
    }

    @Bean
    public PoliticaRepeticion politicaRepeticion() {
        return new PoliticaRepeticion();
    }

    @Bean
    public ReglaManiobra reglaManiobra() {
        return new ReglaManiobra();
    }

    @Bean
    public ValidadorUmbrales validadorUmbrales() {
        return new ValidadorUmbrales();
    }

    @Bean
    public ServicioEscalamiento servicioEscalamiento() {
        return new ServicioEscalamiento();
    }
}
