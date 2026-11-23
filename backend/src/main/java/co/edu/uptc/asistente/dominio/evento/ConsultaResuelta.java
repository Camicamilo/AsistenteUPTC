package co.edu.uptc.asistente.dominio.evento;

import java.time.Instant;

public record ConsultaResuelta(String sesionId, String texto, String intent, double confianza, Long respuestaId,
                               long tiempoMs, Instant fechaHora) implements EventoDominio {
    public String agregadoId() { return sesionId; }
    public String tipoAgregado() { return "SesionChat"; }
}
