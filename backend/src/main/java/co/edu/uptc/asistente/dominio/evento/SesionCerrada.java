package co.edu.uptc.asistente.dominio.evento;

import java.time.Instant;

public record SesionCerrada(String sesionId, String causa, int totalConsultas, Instant fechaHora)
        implements EventoDominio {
    public String agregadoId() { return sesionId; }
    public String tipoAgregado() { return "SesionChat"; }
}
