package ar.unsam.vv.atalaya.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.unsam.vv.atalaya.dominio.EstadoSolicitud;
import ar.unsam.vv.atalaya.dominio.Umbrales;
import ar.unsam.vv.atalaya.dominio.UmbralesInvalidosException;
import ar.unsam.vv.atalaya.dominio.ValidadorUmbrales;
import ar.unsam.vv.atalaya.persistencia.SolicitudUmbralEntidad;
import ar.unsam.vv.atalaya.persistencia.SolicitudUmbralRepositorio;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RF-12 · ServicioUmbrales aislado de la base y de la seguridad, con Mockito. El repositorio y el
 * validador son mocks: así se prueba solo la orquestación del servicio (qué valida, qué guarda y
 * cuándo corta), sin levantar Spring ni H2. El reloj es fijo (datos sintéticos).
 *
 * <p>Complementa las pruebas de integración de CP-32 y CP-33: acá se verifica además que, cuando
 * la regla corta, el servicio no guarda ni cambia nada.
 */
@ExtendWith(MockitoExtension.class)
class ServicioUmbralesTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private SolicitudUmbralRepositorio solicitudes;

    @Mock
    private ValidadorUmbrales validador;

    private ServicioUmbrales servicio;

    private final Umbrales nuevos = new Umbrales(BigDecimal.valueOf(30), BigDecimal.valueOf(65));

    @BeforeEach
    void crearServicio() {
        servicio = new ServicioUmbrales(solicitudes, validador, Clock.fixed(T0, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("CP-32 · si el validador rechaza los umbrales, la solicitud no se guarda")
    void umbralInvalidoNoSeGuarda() {
        Umbrales invalidos = new Umbrales(new BigDecimal("29.9"), BigDecimal.valueOf(75));
        doThrow(new UmbralesInvalidosException("alto", "fuera de rango")).when(validador).validar(invalidos);

        assertThatThrownBy(() -> servicio.solicitar(invalidos, "supervisor"))
                .isInstanceOf(UmbralesInvalidosException.class);
        verify(solicitudes, never()).save(any());
    }

    @Test
    @DisplayName("CP-32 · una solicitud válida se valida y se guarda PENDIENTE con quien la pidió")
    void solicitudValidaQuedaPendiente() {
        when(solicitudes.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        SolicitudUmbralVista vista = servicio.solicitar(nuevos, "supervisor");

        verify(validador).validar(nuevos);
        assertThat(vista.estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(vista.solicitadaPor()).isEqualTo("supervisor");
        assertThat(vista.aprobadaPor()).isNull();
    }

    @Test
    @DisplayName("CP-33 · si aprueba el mismo usuario que solicitó → ConflictoException y la solicitud sigue PENDIENTE")
    void mismoUsuarioNoAprueba() {
        SolicitudUmbralEntidad pendiente = new SolicitudUmbralEntidad(nuevos, "supervisor", T0);
        when(solicitudes.findById(1L)).thenReturn(Optional.of(pendiente));

        assertThatThrownBy(() -> servicio.aprobar(1L, "supervisor"))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("solicitó");
        assertThat(pendiente.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(pendiente.getAprobadaPor()).isNull();
    }

    @Test
    @DisplayName("CP-33 · otro usuario aprueba → APLICADA con quien aprobó")
    void otroUsuarioAprueba() {
        SolicitudUmbralEntidad pendiente = new SolicitudUmbralEntidad(nuevos, "supervisor", T0);
        when(solicitudes.findById(1L)).thenReturn(Optional.of(pendiente));

        SolicitudUmbralVista vista = servicio.aprobar(1L, "jefe");

        assertThat(vista.estado()).isEqualTo(EstadoSolicitud.APLICADA);
        assertThat(vista.aprobadaPor()).isEqualTo("jefe");
    }

    @Test
    @DisplayName("CP-40 · aprobar una solicitud ya APLICADA → ConflictoException y no cambia quién aprobó")
    void yaAplicadaNoSeVuelveAAprobar() {
        SolicitudUmbralEntidad aplicada = new SolicitudUmbralEntidad(nuevos, "supervisor", T0);
        aplicada.aplicar("jefe", T0);
        when(solicitudes.findById(1L)).thenReturn(Optional.of(aplicada));

        assertThatThrownBy(() -> servicio.aprobar(1L, "otroJefe"))
                .isInstanceOf(ConflictoException.class);
        assertThat(aplicada.getAprobadaPor()).isEqualTo("jefe");
    }

    @Test
    @DisplayName("CP-40 · aprobar una solicitud inexistente → RecursoInexistenteException")
    void solicitudInexistente() {
        when(solicitudes.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.aprobar(99L, "jefe"))
                .isInstanceOf(RecursoInexistenteException.class);
    }
}
