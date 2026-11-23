package co.edu.uptc.asistente.dominio.evento;

import java.time.Instant;

public record SesionIniciada(String sesionId, String canal, Instant fechaHora) implements EventoDominio {
    public String agregadoId() { return sesionId; }
    public String tipoAgregado() { return "SesionChat"; }
}
