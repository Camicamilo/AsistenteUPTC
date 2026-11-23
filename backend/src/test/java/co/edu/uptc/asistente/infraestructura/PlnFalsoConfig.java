package co.edu.uptc.asistente.infraestructura;

import co.edu.uptc.asistente.aplicacion.puerto.salida.ClasificadorIntencion;
import co.edu.uptc.asistente.aplicacion.puerto.salida.EntrenadorModelo;
import co.edu.uptc.asistente.dominio.excepcion.PlnNoDisponibleException;
import co.edu.uptc.asistente.dominio.modelo.NivelConfianza;
import co.edu.uptc.asistente.dominio.modelo.ResultadoClasificacion;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

/** Motor PLN falso para las pruebas de integración (no hace falta levantar el servicio Python). */
@TestConfiguration
public class PlnFalsoConfig {

    @Bean
    @Primary
    public ClasificadorIntencion clasificadorFalso() {
        return texto -> {
            String t = texto.toLowerCase();
            if (t.contains("caido")) {
                throw new PlnNoDisponibleException("caído", null);
            }
            if (t.contains("partido")) {
                return new ResultadoClasificacion("nlu_fallback", NivelConfianza.de(0.9), List.of());
            }
            if (t.contains("ambiguo")) {
                return new ResultadoClasificacion("consulta_matricula", NivelConfianza.de(0.41), List.of());
            }
            if (t.contains("matr")) {
                return new ResultadoClasificacion("consulta_matricula", NivelConfianza.de(0.93), List.of());
            }
            return new ResultadoClasificacion("saludo", NivelConfianza.de(0.88), List.of());
        };
    }

    @Bean
    @Primary
    public EntrenadorModelo entrenadorFalso() {
        return datos -> {
        };
    }
}
