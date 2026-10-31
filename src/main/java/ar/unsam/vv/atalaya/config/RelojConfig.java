package ar.unsam.vv.atalaya.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Único origen de tiempo del SISTEMA. Nada llama a now() sin este Clock. */
@Configuration
public class RelojConfig {

    @Bean
    @Profile("qa")
    public RelojMutable relojMutable() {
        return new RelojMutable();
    }

    @Bean
    @Profile("!qa")
    public Clock relojSistema() {
        return Clock.systemUTC();
    }
}
