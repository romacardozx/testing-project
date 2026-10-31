package ar.unsam.vv.atalaya.dominio;

import java.util.List;
import java.util.Optional;

/** RF-07: canales, destinatarios y reconocimiento según el nivel. NORMAL no genera alerta. */
public record DefinicionAlerta(List<CanalAlerta> canales, List<Rol> destinatarios, boolean requiereReconocimiento) {

    public DefinicionAlerta {
        canales = List.copyOf(canales);
        destinatarios = List.copyOf(destinatarios);
    }

    public static Optional<DefinicionAlerta> para(NivelRiesgo nivel) {
        return switch (nivel) {
            case NORMAL -> Optional.empty();
            case ATENCION -> Optional.of(new DefinicionAlerta(
                    List.of(CanalAlerta.VISUAL), List.of(), false));
            case ALTO -> Optional.of(new DefinicionAlerta(
                    List.of(CanalAlerta.VISUAL), List.of(Rol.OPERADOR_REACTOR), true));
            case CRITICO -> Optional.of(new DefinicionAlerta(
                    List.of(CanalAlerta.VISUAL, CanalAlerta.SONORA),
                    List.of(Rol.OPERADOR_REACTOR, Rol.JEFE_TURNO), true));
        };
    }
}
