package co.edu.uptc.asistente.aplicacion.dto;

import java.util.List;

/** Indicadores del dashboard (UC-15). Los nombres coinciden con lo que pinta el panel. */
public record Metricas(
        String rango,
        String granularidad,
        long total,
        long totalAnterior,
        long resueltas,
        long escaladas,
        double tasaResolucion,
        double tasaResolucionAnterior,
        double tiempoPromedioMs,
        double porcentajeBajo3s,
        long sesiones,
        double disponibilidad,
        double slaDisponibilidad,
        List<PuntoSerie> serie,
        List<ConteoIntencion> topIntenciones) {

    public record PuntoSerie(int clave, long inicio, long resueltas, long escaladas) {
    }

    public record ConteoIntencion(String nombre, long total) {
    }
}
