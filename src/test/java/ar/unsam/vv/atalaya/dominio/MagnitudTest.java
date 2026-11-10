package ar.unsam.vv.atalaya.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RF-01/02 y RF-03 · Catálogo de magnitudes: búsqueda por nombre en la ruta (CP-37) y rango físico (A2, CP-07). */
class MagnitudTest {

    @Test
    @DisplayName("CP-37 · la magnitud se busca sin distinguir mayúsculas")
    void buscaSinDistinguirMayusculas() {
        assertThat(Magnitud.desde("temperatura_primario")).contains(Magnitud.TEMPERATURA_PRIMARIO);
        assertThat(Magnitud.desde("TASA_DOSIS")).contains(Magnitud.TASA_DOSIS);
    }

    @Test
    @DisplayName("CP-37 · una magnitud inexistente no se encuentra")
    void magnitudInexistente() {
        assertThat(Magnitud.desde("caudal")).isEmpty();
    }

    @Test
    @DisplayName("CP-07 · el rango físico de la temperatura es 330 °C (base del 2 %, A2)")
    void rangoFisico() {
        assertThat(Magnitud.TEMPERATURA_PRIMARIO.rangoFisico()).isEqualByComparingTo("330");
        assertThat(Magnitud.TEMPERATURA_PRIMARIO.unidad()).isEqualTo("°C");
    }
}
