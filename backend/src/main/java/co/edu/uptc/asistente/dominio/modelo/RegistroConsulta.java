package co.edu.uptc.asistente.dominio.modelo;

import java.time.Instant;

/** Una interacción consulta–respuesta registrada (RF-06, UC-10). Sin datos personales. */
public record RegistroConsulta(
        Long id,
        String sesionId,
        String texto,
        String intent,
        String categoria,
        Double confianza,
        boolean resuelta,
        String motivoEscalamiento,
        Long respuestaId,
        long tiempoMs,
        String canal,
        Instant fechaHora) {
}
