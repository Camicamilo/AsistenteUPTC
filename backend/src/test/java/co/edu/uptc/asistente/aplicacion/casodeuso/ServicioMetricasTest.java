package co.edu.uptc.asistente.aplicacion.casodeuso;

import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.ConsultasEnMemoria;
import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.IntencionesEnMemoria;
import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.MonitorFalso;
import co.edu.uptc.asistente.aplicacion.casodeuso.Fakes.RelojFijo;
import co.edu.uptc.asistente.aplicacion.dto.FiltroHistorial;
import co.edu.uptc.asistente.aplicacion.dto.Metricas;
import co.edu.uptc.asistente.dominio.modelo.RegistroConsulta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicioMetricasTest {
    private ConsultasEnMemoria consultas;
    private ServicioMetricas servicio;

    @BeforeEach
    void preparar() {
        consultas = new ConsultasEnMemoria();
        IntencionesEnMemoria intenciones = new IntencionesEnMemoria();
        intenciones.agregar("saludo", "administrativo", "general", true, "Hola");
        servicio = new ServicioMetricas(consultas, intenciones, new MonitorFalso(), new RelojFijo(),
                ZoneId.of("America/Bogota"));
    }

    private void consulta(String intent, boolean resuelta, long ms, Instant fecha) {
        consultas.registrar(new RegistroConsulta(null, "SES-1", "texto", intent, "académico", 0.9, resuelta,
                resuelta ? null : "Confianza por debajo del umbral", null, ms, "web", fecha));
    }

    @Test
    void calculaIndicadoresDeLaSemana() {
        Instant hoy = Fakes.AHORA.minusSeconds(3600);
        consulta("consulta_matricula", true, 800, hoy);
        consulta("consulta_matricula", true, 1200, hoy);
        consulta("saludo", true, 100, hoy);
        consulta(null, false, 3500, hoy);
        consulta("consulta_matricula", true, 500, Fakes.AHORA.minusSeconds(8L * 24 * 3600)); // semana anterior

        Metricas m = servicio.calcular("7d");
        assertEquals(4, m.total());
        assertEquals(1, m.totalAnterior());
        assertEquals(3, m.resueltas());
        assertEquals(0.75, m.tasaResolucion(), 1e-9);
        assertEquals(0.75, m.porcentajeBajo3s(), 1e-9);
        assertEquals(1400, m.tiempoPromedioMs(), 1e-9);
        assertEquals(7, m.serie().size());
        assertEquals(3, m.serie().get(6).resueltas());
        // Las intenciones sociales (saludo) no cuentan en el top
        assertEquals(1, m.topIntenciones().size());
        assertEquals("consulta_matricula", m.topIntenciones().get(0).nombre());
        assertEquals(0.99, m.disponibilidad(), 1e-9);
    }

    @Test
    void serieDeHoyEsPorHora() {
        Metricas m = servicio.calcular("hoy");
        assertEquals("hora", m.granularidad());
        // 15:00 UTC = 10:00 en Bogotá -> horas 0..10
        assertEquals(11, m.serie().size());
    }

    @Test
    void rangoDesconocidoUsaSieteDias() {
        assertEquals("7d", servicio.calcular("cualquiera").rango());
    }

    @Test
    void historialFiltraPorEstado() {
        consulta("consulta_matricula", true, 800, Fakes.AHORA);
        consulta(null, false, 800, Fakes.AHORA);
        assertEquals(1, servicio.historial(new FiltroHistorial("7d", "escalada", null, 1, 10)).total());
        assertEquals(2, servicio.historial(new FiltroHistorial("7d", "", null, 1, 10)).total());
    }

    @Test
    void elCsvEscapaComillasYFormulas() {
        consultas.registrar(new RegistroConsulta(null, "SES-1", "=HYPERLINK(\"x\")", null, null, null, false,
                "Fuera del dominio", null, 10, "web", Fakes.AHORA));
        String csv = servicio.exportarCsv("7d");
        assertTrue(csv.startsWith("fecha_hora;sesion;"));
        assertTrue(csv.contains("\"'=HYPERLINK(\"\"x\"\")\""), csv);
    }
}
