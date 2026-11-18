package co.edu.uptc.asistente.dominio.evento;

import java.time.Instant;

public record ConsultaEscalada(String sesionId, String texto, String intent, String categoria, Double confianza,
                               String motivo, String dependencia, Instant fechaHora) implements EventoDominio {
    public String agregadoId() { return sesionId; }
    public String tipoAgregado() { return "SesionChat"; }
}
