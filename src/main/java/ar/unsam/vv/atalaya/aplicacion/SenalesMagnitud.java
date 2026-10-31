package ar.unsam.vv.atalaya.aplicacion;

import ar.unsam.vv.atalaya.dominio.Canal;
import ar.unsam.vv.atalaya.dominio.Magnitud;
import ar.unsam.vv.atalaya.dominio.RecuperacionCanal;
import ar.unsam.vv.atalaya.dominio.SenalCanal;
import ar.unsam.vv.atalaya.persistencia.EstadoCanalEntidad;
import ar.unsam.vv.atalaya.persistencia.EstadoCanalRepositorio;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Lee de la base las señales de los tres canales de una magnitud (los que nunca enviaron, vacíos). */
@Component
public class SenalesMagnitud {

    private final EstadoCanalRepositorio estados;

    public SenalesMagnitud(EstadoCanalRepositorio estados) {
        this.estados = estados;
    }

    public List<SenalCanal> de(Magnitud magnitud) {
        Map<Canal, EstadoCanalEntidad> porCanal = porCanal(magnitud);
        return Arrays.stream(Canal.values())
                .map(c -> porCanal.containsKey(c) ? porCanal.get(c).senal() : SenalCanal.sinDatos(c))
                .toList();
    }

    public Map<Canal, RecuperacionCanal> recuperaciones(Magnitud magnitud) {
        Map<Canal, RecuperacionCanal> resultado = new EnumMap<>(Canal.class);
        porCanal(magnitud).forEach((canal, entidad) -> resultado.put(canal, entidad.recuperacion()));
        return resultado;
    }

    private Map<Canal, EstadoCanalEntidad> porCanal(Magnitud magnitud) {
        return estados.findByMagnitud(magnitud).stream()
                .collect(Collectors.toMap(EstadoCanalEntidad::canal, Function.identity()));
    }
}
