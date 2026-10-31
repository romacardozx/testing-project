package ar.unsam.vv.atalaya.config;

import ar.unsam.vv.atalaya.dominio.Rol;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Usuarios en memoria cargados desde application-{perfil}.yml (atalaya.usuarios). */
@ConfigurationProperties(prefix = "atalaya")
public record UsuariosProperties(List<Usuario> usuarios) {

    public UsuariosProperties {
        usuarios = usuarios == null ? List.of() : List.copyOf(usuarios);
    }

    public record Usuario(String nombre, String clave, Rol rol) {
    }
}
